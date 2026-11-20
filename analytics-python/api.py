# ============================================================================
# api.py — a "porta de entrada" do Python: transforma o ETL em uma API (FastAPI).
# O Spring chama POST /etl com as linhas; o Python VALIDA, limpa, calcula e GRAVA no Azure.
# Como ligar: uvicorn api:app --port 8000 (documentação automática em http://localhost:8000/docs)
# ============================================================================
import hmac                                                                             # hmac: compara a chave sem vazar informação
import logging                                                                          # logging: escreve mensagens no terminal
import os                                                                               # os: lê variáveis de ambiente (ETL_API_KEY)
from typing import Any                                                                  # Any: "qualquer tipo" (valores das linhas)

import pandas as pd                                                                     # pandas: tabelas de dados
from fastapi import FastAPI, Header, HTTPException                                      # FastAPI: cria a API; Header: lê cabeçalhos; HTTPException: erro HTTP
from pydantic import BaseModel                                                          # BaseModel: descreve o formato do JSON que o Spring envia

import config                                                                           # config.py: carrega o .env (senha do banco, ETL_API_KEY)
from main import SemLinhasValidas, preparar, processar_lote                             # as funções do ETL (main.py)
from validacao import relatorio, validar                                                # a validação (validacao.py)

log = logging.getLogger("analytics")                                                    # logger desta API
app = FastAPI(title="Datum — ETL em Python")                                            # app: a aplicação web
_engine = None                                                                          # conexão com o Azure (criada na primeira vez que for usada)


class Pedido(BaseModel):                                                                # Pedido: o formato do JSON enviado pelo Spring
    arquivoNome: str = "planilha.xlsx"                                                  # nome do arquivo enviado
    recebidas: int | None = None                                                        # total de linhas da planilha (boas + ruins)
    invalidas: int = 0                                                                  # linhas já recusadas antes (front e backend)
    comAviso: int = 0                                                                   # linhas com aviso no backend
    linhas: list[dict[str, Any]]                                                        # só as linhas que passaram no backend


def _engine_azure():                                                                    # devolve a conexão com o Azure (cria só uma vez)
    global _engine                                                                      # usa a variável de fora da função
    if _engine is None:                                                                 # ainda não existe?
        import banco                                                                    # banco.py: único arquivo que fala com o Azure
        _engine = banco.criar_engine()                                                  # abre a conexão
    return _engine                                                                      # devolve


def _conferir_chave(x_etl_key: str | None) -> None:                                     # confere o cabeçalho X-ETL-Key
    chave = os.getenv("ETL_API_KEY", "").strip()                                        # chave combinada com o Spring (vazia = sem chave, só para estudo)
    if chave and not hmac.compare_digest(x_etl_key or "", chave):                       # há chave e a enviada não confere?
        raise HTTPException(status_code=401, detail="Chave do ETL inválida.")           # recusa: 401 não autorizado


@app.get("/saude")                                                                      # GET /saude: confere se a API está de pé
def saude() -> dict:                                                                    # função chamada nesse endereço
    return {"status": "ok"}                                                             # resposta simples


@app.post("/validar")                                                                   # POST /validar: só valida (não calcula, não grava)
def validar_linhas(linhas: list[dict[str, Any]], x_etl_key: str | None = Header(default=None)) -> dict:  # recebe a lista de clientes
    _conferir_chave(x_etl_key)                                                          # confere a chave
    return relatorio(validar(linhas))                                                   # devolve válidas, rejeitadas e avisos


@app.post("/testar")                                                                    # POST /testar: valida e calcula, sem banco
def testar(linhas: list[dict[str, Any]], x_etl_key: str | None = Header(default=None)) -> dict:  # recebe a lista de clientes
    _conferir_chave(x_etl_key)                                                          # confere a chave
    try:                                                                                # tenta preparar
        rel, _, payload = preparar(linhas)                                              # valida, limpa e calcula
    except SemLinhasValidas as e:                                                       # nenhuma linha boa
        raise HTTPException(status_code=422, detail={"mensagem": str(e), "rejeitadas": e.relatorio["rejeitadas"]})  # 422 com os motivos
    return {"validacao": rel, "dashboard": payload}                                     # relatório + números e frases


@app.post("/etl")                                                                       # POST /etl: valida, calcula e grava no Azure
def etl(pedido: Pedido, x_etl_key: str | None = Header(default=None)) -> dict:          # x_etl_key: cabeçalho X-ETL-Key
    _conferir_chave(x_etl_key)                                                          # confere a chave
    if not pedido.linhas:                                                               # veio vazio?
        raise HTTPException(status_code=422, detail={"mensagem": "Nenhuma linha foi enviada.", "rejeitadas": []})  # 422: dados inválidos
    recebidas = pedido.recebidas if pedido.recebidas is not None else len(pedido.linhas)  # total de linhas da planilha
    try:                                                                                # tenta processar
        lote_id, rel = processar_lote(_engine_azure(), pedido.arquivoNome, recebidas,   # valida, calcula e grava no Azure
                                      pedido.invalidas, pedido.comAviso, pedido.linhas)
    except SemLinhasValidas as e:                                                       # o Python recusou todas as linhas
        raise HTTPException(status_code=422, detail={"mensagem": str(e), "rejeitadas": e.relatorio["rejeitadas"]})  # 422 com os motivos
    except Exception:                                                                   # noqa: BLE001 - erro de banco ou de cálculo
        log.exception("Falha no ETL")                                                   # escreve o erro completo no terminal
        raise HTTPException(status_code=500, detail="Falha no ETL (veja o terminal do Python).")  # 500: erro no servidor
    return {                                                                            # 200: deu certo
        "ok": True, "loteId": lote_id,                                                  # id do lote criado
        "linhasValidas": rel["linhasValidas"], "linhasInvalidas": rel["linhasInvalidas"],  # contagens da validação no Python
        "rejeitadas": rel["rejeitadas"], "avisos": rel["avisos"],                       # motivos das rejeições e avisos
    }
