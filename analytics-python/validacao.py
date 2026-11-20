"""Validação das linhas da planilha, feita no Python.

Recebe as linhas (como o backend Spring envia), confere cada campo e devolve, para cada linha,
a lista de ERROS (a linha é recusada) e de AVISOS (a linha segue, mas algo foi ajustado).
Usa as mesmas funções de limpeza.py (parse_numero, parse_data, padronizar_segmento...),
então existe uma única regra para segmento, serviço, número e data.
"""
from __future__ import annotations                                                      # recurso do Python para tipos mais flexíveis

import re                                                                               # re: expressões regulares (padrões de texto)
from dataclasses import dataclass, field                                                # dataclass: classe só para carregar dados
from datetime import date                                                               # date: datas

from limpeza import (                                                                   # funções que já existem na limpeza
    NIVEIS, OBRIGATORIAS, SEGMENTOS,                                                    # níveis aceitos, colunas obrigatórias e dicionário de segmentos
    limpar_texto, padronizar_segmento, padronizar_servicos, parse_data, parse_numero, titulo,  # limpeza e conversão
    _simples,                                                                           # _simples: tira acento e pontuação (usada na checagem de duplicidade)
)

TAMANHO_MAXIMO = {                                                                      # tamanho máximo de cada coluna de texto (igual ao banco)
    "codigo_cliente": 60, "nome_cliente": 255, "consultor": 120, "segmento": 80,        # código, nome, consultor, segmento
    "nivel_cliente": 20, "servicos_contratados": 400, "cidade": 120,                    # nível, serviços e cidade
}
RE_CODIGO = re.compile(r"^[A-Za-z0-9_-]+$")                                             # código: só letras, números, - e _
RE_UF = re.compile(r"^[A-Za-z]{2}$")                                                    # UF: exatamente 2 letras
LIMITE_FATURAMENTO = 1_000_000_000_000                                                  # 1 trilhão: acima disso é erro de digitação
ANO_MINIMO = 1990                                                                       # datas antes de 1990 são recusadas
SEGMENTOS_VALIDOS = sorted(set(SEGMENTOS.values()))                                     # nomes oficiais (para a mensagem de erro)


@dataclass                                                                              # dataclass: classe só para carregar o resultado
class ResultadoLinha:                                                                   # resultado da validação de UMA linha
    numero: int                                                                         # número da linha na planilha (cabeçalho = linha 1)
    valores: dict                                                                       # valores já limpos e padronizados
    erros: list[str] = field(default_factory=list)                                      # erros: bloqueiam a linha
    avisos: list[str] = field(default_factory=list)                                     # avisos: só informam o que foi ajustado

    @property                                                                           # property: usa como atributo (r.valida)
    def valida(self) -> bool:                                                           # valida: true quando não há nenhum erro
        return not self.erros                                                           # válida = sem erros


def _chave(nome: str) -> str:                                                           # _chave: codigoCliente (JSON) vira codigo_cliente
    return re.sub(r"(?<!^)(?=[A-Z])", "_", str(nome)).lower()                           # camelCase vira snake_case


def _numero_da_linha(reg: dict, posicao: int) -> int:                                   # _numero_da_linha: usa o número enviado ou a posição
    for c in ("numero_linha", "linha"):                                                 # nomes possíveis do campo
        try:                                                                            # tenta converter
            if reg.get(c) is not None:                                                  # veio algum valor?
                return int(reg[c])                                                      # devolve como inteiro
        except (TypeError, ValueError):                                                 # não era número
            pass                                                                        # tenta o próximo
    return posicao                                                                      # sem número: usa a posição (começa em 2)


def validar(linhas: list[dict]) -> list[ResultadoLinha]:                                # validar: função principal; uma resposta por linha
    resultados: list[ResultadoLinha] = []                                               # lista de resultados
    codigos: dict[str, int] = {}                                                        # código -> primeira linha que o usou
    assinaturas: dict[str, str] = {}                                                    # nome+faturamento+data -> código (duplicidade provável)

    for posicao, bruto in enumerate(linhas, start=2):                                   # para cada linha (a 1ª linha de dados é a 2)
        reg = {_chave(k): v for k, v in bruto.items()}                                  # chaves em snake_case
        r = ResultadoLinha(_numero_da_linha(reg, posicao), {})                          # resultado desta linha
        v = r.valores                                                                   # atalho para os valores limpos

        # 1) texto limpo (espaços extras) e cortado no tamanho da coluna
        for campo in ("codigo_cliente", "nome_cliente", "consultor", "segmento",        # para cada coluna de texto
                      "nivel_cliente", "servicos_contratados", "cidade", "uf"):
            original = reg.get(campo)                                                   # valor original
            limpo = limpar_texto(original)                                              # sem espaços sobrando (vazio vira None)
            if isinstance(original, str) and limpo is not None and limpo != original:   # mudou?
                r.avisos.append(f"{campo}: espaços extras removidos")                   # avisa
            maximo = TAMANHO_MAXIMO.get(campo)                                          # tamanho máximo (se houver)
            if limpo is not None and maximo and len(limpo) > maximo:                    # passou do tamanho?
                limpo = limpo[:maximo].strip()                                          # corta
                r.avisos.append(f"{campo}: texto cortado em {maximo} caracteres")       # avisa
            v[campo] = limpo                                                            # guarda o valor limpo

        # 2) campos obrigatórios (faturamento e data são conferidos adiante)
        for campo in OBRIGATORIAS:                                                      # para cada obrigatório
            if campo in ("faturamento_anual", "data_contratacao"):                      # esses dois têm checagem própria
                continue                                                                # pula
            if v.get(campo) is None:                                                    # vazio?
                r.erros.append(f"{campo}: campo obrigatório vazio")                     # erro

        # 3) código: formato e duplicidade dentro da planilha
        codigo = v["codigo_cliente"]                                                    # código limpo
        if codigo is not None:                                                          # veio código
            if not RE_CODIGO.match(codigo):                                             # tem caractere inválido?
                r.erros.append("codigo_cliente: use só letras, números, - e _")         # erro
            v["codigo_cliente"] = codigo = codigo.upper()                               # código em maiúsculas
            primeira = codigos.setdefault(codigo, r.numero)                             # guarda a 1ª linha que usou o código
            if primeira != r.numero:                                                    # já foi usado antes
                r.erros.append(f"codigo_cliente: código duplicado (já usado na linha {primeira})")  # erro

        # 4) segmento: precisa ser um dos conhecidos (IND. vira Indústria)
        if v["segmento"] is not None:                                                   # veio segmento
            nome, reconhecido = padronizar_segmento(v["segmento"])                      # padroniza
            if not reconhecido:                                                         # desconhecido
                r.erros.append(f"segmento: '{v['segmento']}' não reconhecido (use: {', '.join(SEGMENTOS_VALIDOS)})")  # erro
            elif nome != v["segmento"]:                                                 # foi padronizado
                r.avisos.append(f"segmento: padronizado para {nome}")                   # aviso
                v["segmento"] = nome                                                    # guarda o nome oficial

        # 5) consultor em formato Título (ana souza -> Ana Souza)
        if v["consultor"] is not None:                                                  # veio consultor
            ajustado = titulo(v["consultor"])                                           # formata
            if ajustado != v["consultor"]:                                              # mudou?
                r.avisos.append("consultor: nome ajustado")                             # aviso
                v["consultor"] = ajustado                                               # guarda

        # 6) nível: A, B ou C (minúscula vira aviso)
        if v["nivel_cliente"] is not None:                                              # veio nível
            nivel = v["nivel_cliente"].upper()                                          # maiúscula
            if nivel not in NIVEIS:                                                     # fora de A/B/C
                r.erros.append(f"nivel_cliente: '{v['nivel_cliente']}' inválido (use A, B ou C)")  # erro
            elif nivel != v["nivel_cliente"]:                                           # estava minúsculo
                r.avisos.append("nivel_cliente: convertido para maiúscula")             # aviso
            v["nivel_cliente"] = nivel                                                  # guarda

        # 7) faturamento: número, não negativo, abaixo de 1 trilhão (zero gera aviso)
        bruto_fat = reg.get("faturamento_anual")                                        # valor original
        fat = None                                                                      # número convertido
        if limpar_texto(bruto_fat) is None:                                             # vazio
            r.erros.append("faturamento_anual: campo obrigatório vazio")                # erro
        else:                                                                           # veio algo
            fat = parse_numero(bruto_fat)                                               # tenta converter
            if fat is None:                                                             # não é número
                r.erros.append(f"faturamento_anual: '{bruto_fat}' não é um número")     # erro
            elif fat < 0:                                                               # negativo
                r.erros.append("faturamento_anual: não pode ser negativo")              # erro
            elif fat >= LIMITE_FATURAMENTO:                                             # absurdo
                r.erros.append("faturamento_anual: valor alto demais")                  # erro
            elif fat == 0:                                                              # zero
                r.avisos.append("faturamento_anual: valor zero")                        # aviso
        v["faturamento_anual"] = None if fat is None or fat < 0 or fat >= LIMITE_FATURAMENTO else round(fat, 2)  # valor guardado

        # 8) serviços separados por ";": pelo menos um
        if v["servicos_contratados"] is not None:                                       # veio algo
            servicos = padronizar_servicos(v["servicos_contratados"])                   # lista de nomes oficiais
            if not servicos:                                                            # só separadores
                r.erros.append("servicos_contratados: informe pelo menos um serviço")   # erro
            else:                                                                       # ok
                v["servicos_contratados"] = ";".join(servicos)                          # guarda padronizado

        # 9) data: dd/mm/aaaa, aaaa-mm-dd ou número do Excel; a partir de 1990; futuro gera aviso
        bruto_data = reg.get("data_contratacao")                                        # valor original
        data = None                                                                     # data convertida
        if limpar_texto(bruto_data) is None:                                            # vazio
            r.erros.append("data_contratacao: campo obrigatório vazio")                 # erro
        else:                                                                           # veio algo
            data = parse_data(bruto_data)                                               # tenta converter
            if data is None or data.year < ANO_MINIMO:                                  # inválida ou antiga
                r.erros.append(f"data_contratacao: '{bruto_data}' não é uma data válida")  # erro
                data = None                                                             # descarta
            elif data > date.today():                                                   # no futuro
                r.avisos.append("data_contratacao: data no futuro")                     # aviso
        v["data_contratacao"] = data.isoformat() if data else None                      # guarda como aaaa-mm-dd

        # 10) UF opcional: 2 letras
        if v["uf"] is not None:                                                         # veio UF
            if not RE_UF.match(v["uf"]):                                                # formato errado
                r.erros.append(f"uf: '{v['uf']}' inválida (use 2 letras)")              # erro
            else:                                                                       # ok
                if v["uf"] != v["uf"].upper():                                          # estava minúscula
                    r.avisos.append("uf: convertida para maiúscula")                    # aviso
                v["uf"] = v["uf"].upper()                                               # guarda

        # 11) possível duplicidade: mesmo nome + faturamento + data com outro código
        if r.valida and v["nome_cliente"] and fat is not None and data is not None:     # só em linhas boas
            chave = f"{_simples(v['nome_cliente'])}|{fat}|{data}"                       # assinatura da linha
            outro = assinaturas.setdefault(chave, codigo)                               # guarda ou devolve o código que já tinha
            if outro != codigo:                                                         # mesmo cliente com código diferente
                r.avisos.append(f"possível duplicidade com o cliente {outro}")          # aviso

        resultados.append(r)                                                            # guarda o resultado
    return resultados                                                                   # devolve todos


def relatorio(resultados: list[ResultadoLinha], limite_avisos: int = 100) -> dict:      # relatorio: resumo para devolver ao backend e ao front
    rejeitadas = [f"Linha {r.numero}: " + " | ".join(r.erros) for r in resultados if not r.valida]  # só as com erro
    avisos = [f"Linha {r.numero}: " + " | ".join(r.avisos) for r in resultados if r.valida and r.avisos]  # linhas boas com aviso
    return {                                                                            # dicionário que vira JSON
        "totalLinhas": len(resultados),                                                 # total conferido
        "linhasValidas": sum(1 for r in resultados if r.valida),                        # válidas
        "linhasInvalidas": len(rejeitadas),                                             # com erro
        "linhasComAviso": len(avisos),                                                  # válidas com aviso
        "rejeitadas": rejeitadas,                                                       # texto de cada rejeição
        "avisos": avisos[:limite_avisos],                                               # avisos (limitado)
    }
