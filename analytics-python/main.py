"""O trabalho do Python, na ordem: VALIDAR -> LIMPAR -> CALCULAR -> GRAVAR.

`preparar` não usa banco (por isso dá para testar sem Azure).
`processar_lote` chama `preparar` e grava o resultado no banco (banco.py).
Quem chama estas funções é a API (api.py).
"""
from __future__ import annotations                                                      # recurso do Python para tipos mais flexíveis

import logging                                                                          # logging: escreve mensagens no terminal

import pandas as pd                                                                     # pandas: tabelas de dados

from analise import calcular                                                            # calcular: gera os indicadores e as frases (analise.py)
from limpeza import ResultadoTransformacao, tratar                                      # tratar: limpa e padroniza os dados (limpeza.py)
from validacao import ResultadoLinha, relatorio, validar                                # validar: confere cada linha (validacao.py)

log = logging.getLogger("analytics")                                                    # logger do programa


class SemLinhasValidas(Exception):                                                      # erro: depois de validar, não sobrou nenhuma linha
    def __init__(self, rel: dict):                                                      # recebe o relatório da validação
        super().__init__("Nenhuma linha válida depois da validação.")                   # mensagem do erro
        self.relatorio = rel                                                            # guarda o relatório (as rejeitadas)


def preparar(linhas: list[dict], recebidas: int | None = None) -> tuple[dict, ResultadoTransformacao, dict]:  # preparar: valida, limpa e calcula (sem banco)
    """Devolve (relatório da validação, tabela limpa, JSON do dashboard)."""
    resultados: list[ResultadoLinha] = validar(linhas)                                  # 1) VALIDAR cada linha
    rel = relatorio(resultados)                                                         # resumo: válidas, rejeitadas e avisos
    validas = [r for r in resultados if r.valida]                                       # só as linhas sem erro
    if not validas:                                                                     # nada sobrou
        raise SemLinhasValidas(rel)                                                     # avisa quem chamou
    bruto = pd.DataFrame([{**r.valores, "linha": r.numero} for r in validas]).astype(object)  # lista de dicionários vira tabela do pandas
    res = tratar(bruto)                                                                 # 2) LIMPAR e padronizar (limpeza.py)
    payload = calcular(res, recebidas if recebidas is not None else len(linhas))        # 3) CALCULAR média, mediana, agrupamentos e frases
    payload["qualidade"]["linhasRejeitadasNoPython"] = rel["linhasInvalidas"]           # quantas o Python recusou na validação
    payload["qualidade"]["avisos"] = rel["avisos"][:50] + res.avisos                    # avisos da validação (os 50 primeiros)
    return rel, res, payload                                                            # devolve tudo


def processar_lote(engine, arquivo_nome: str, recebidas: int, invalidas_antes: int,     # processar_lote: valida, calcula e GRAVA no Azure
                   com_aviso_antes: int, linhas: list[dict]) -> tuple[int, dict]:       # e devolve (id do lote, relatório)
    import banco                                                                        # banco.py: funções que gravam no banco Azure
    rel, res, payload = preparar(linhas, recebidas)                                     # valida, limpa e calcula (se falhar, nada foi gravado)
    lote_id = banco.criar_lote(engine, arquivo_nome, recebidas)                         # 4) GRAVAR: cria o lote (PROCESSANDO)
    try:                                                                                # tenta gravar
        invalidas = invalidas_antes + rel["linhasInvalidas"] + len(res.rejeitadas)      # recusadas no backend + no Python
        banco.gravar_resultado(engine, lote_id, res.tratado, payload,                   # GRAVA clientes, contratos e o JSON no Azure
                               recebidas, invalidas, max(com_aviso_antes, rel["linhasComAviso"]))
        log.info("Lote %s processado.", lote_id)                                        # registra no terminal
        return lote_id, rel                                                             # deu certo
    except Exception as e:                                                              # noqa: BLE001 - qualquer falha deve aparecer na tela do usuário
        log.exception("Falha no lote %s", lote_id)                                      # escreve o erro completo no terminal
        banco.marcar_erro(engine, lote_id, f"Falha no ETL: {e}")                        # grava ERRO no lote
        raise                                                                           # repassa o erro para a API responder 500
