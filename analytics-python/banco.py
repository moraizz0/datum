"""Banco: GRAVA no PostgreSQL (Azure) o lote, os clientes, os contratos e o JSON do dashboard.

As linhas chegam do backend. Este é o ÚNICO arquivo do Python que fala com o banco.
As tabelas são criadas pelo backend (schema.sql), então rode o backend uma vez antes do primeiro ETL.
"""
from __future__ import annotations                                                      # recurso do Python para tipos mais flexíveis

import json                                                                             # json: transforma dicionário em texto JSON
import logging                                                                          # logging: escreve mensagens no terminal
from datetime import datetime, timezone                                                 # datetime e timezone: data e hora em UTC

import pandas as pd                                                                     # pandas: tabelas de dados (DataFrame)
from sqlalchemy import create_engine, text                                              # SQLAlchemy: create_engine abre a conexão; text escreve SQL
from sqlalchemy.engine import Engine                                                    # tipo Engine (só para indicar o tipo)

from config import database_url                                                         # config: pega o endereço do banco do .env

log = logging.getLogger("analytics")                                                    # logger do programa


def criar_engine() -> Engine:                                                           # criar_engine: abre a "porta" de conexão com o Azure
    return create_engine(database_url(), pool_pre_ping=True)                            # pool_pre_ping testa a conexão antes de usar


def agora_utc() -> datetime:                                                            # agora_utc: hora atual em UTC
    """O Spring Boot grava TIMESTAMP em UTC (sem fuso); fazemos o mesmo."""
    return datetime.now(timezone.utc).replace(tzinfo=None)                              # UTC sem fuso, igual ao backend


# ------------------------------------------------------------------- lote
def criar_lote(engine: Engine, arquivo_nome: str, total_linhas: int) -> int:            # criar_lote: registra o envio e devolve o id
    """Cria o lote com status PROCESSANDO. Quem cria o lote é o Python (o Java só grava usuários)."""
    with engine.begin() as conn:                                                        # abre uma transação (confirma ao sair do bloco)
        return conn.execute(text(                                                       # executa o INSERT
            "INSERT INTO upload_lote (arquivo_nome, status, mensagem, total_linhas) "   # colunas do lote
            "VALUES (:nome, 'PROCESSANDO', 'ETL em andamento...', :total) RETURNING id"  # RETURNING id devolve o id criado
        ), {"nome": arquivo_nome[:200], "total": total_linhas}).scalar_one()            # devolve o id do lote


def marcar_erro(engine: Engine, lote_id: int, mensagem: str) -> None:                   # marcar_erro: grava ERRO no lote
    with engine.begin() as conn:                                                        # abre uma transação
        conn.execute(text("UPDATE upload_lote SET status = 'ERRO', mensagem = :m WHERE id = :id"),  # SQL: status ERRO e a mensagem
                     {"id": lote_id, "m": mensagem[:480]})                              # mensagem limitada a 480 caracteres


# ------------------------------------------------------------------------ carga
def gravar_resultado(engine: Engine, lote_id: int, tratado: pd.DataFrame, payload: dict,  # gravar_resultado: grava clientes, contratos e o JSON
                     total_linhas: int, invalidas: int, com_aviso: int) -> None:        # e as contagens do lote
    """Grava tudo em uma única transação: ou o lote inteiro é processado, ou nada muda."""
    with engine.begin() as conn:                                                        # uma transação só: ou grava tudo ou nada
        consultores: dict[str, int] = {}                                                # cache: nome do consultor -> id
        servicos: dict[str, int] = {}                                                   # cache: nome do serviço -> id

        def id_de(tabela: str, nome: str, cache: dict[str, int]) -> int:                # id_de: acha ou cria um consultor/serviço e devolve o id
            if nome not in cache:                                                       # se ainda não está no cache
                conn.execute(text(f"INSERT INTO {tabela} (nome) VALUES (:n) ON CONFLICT (nome) DO NOTHING"), {"n": nome})  # insere (ignora se já existe)
                cache[nome] = conn.execute(text(f"SELECT id FROM {tabela} WHERE nome = :n"), {"n": nome}).scalar_one()      # busca o id e guarda no cache
            return cache[nome]                                                          # devolve o id

        for r in tratado.itertuples(index=False):                                       # para cada cliente tratado
            cliente_id = conn.execute(text("""
                INSERT INTO cliente (lote_id, codigo_cti, nome, consultor_id, segmento, nivel, faturamento_anual, cidade, uf)
                VALUES (:lote, :codigo, :nome, :consultor, :segmento, :nivel, :fat, :cidade, :uf)
                RETURNING id
            """), {
                "lote": lote_id, "codigo": r.codigo_cliente[:60], "nome": r.nome_cliente[:255],  # código e nome (cortados no tamanho da coluna)
                "consultor": id_de("consultor", r.consultor[:120], consultores),        # consultor vira id
                "segmento": r.segmento[:40], "nivel": r.nivel_cliente, "fat": round(float(r.faturamento_anual), 2),  # segmento, nível e faturamento com 2 casas
                "cidade": r.cidade[:120] if isinstance(r.cidade, str) else None,        # cidade (ou nulo)
                "uf": r.uf if isinstance(r.uf, str) else None,                          # UF (ou nula)
            }).scalar_one()                                                             # fim do INSERT; devolve o id do cliente
            for nome_servico in r.servicos:                                             # para cada serviço do cliente
                conn.execute(text("""
                    INSERT INTO contrato (cliente_id, servico_id, data_contratacao) VALUES (:c, :s, :d)
                """), {"c": cliente_id, "s": id_de("servico", nome_servico[:120], servicos),
                       "d": r.data_contratacao.date()})                                 # data da contratação

        conn.execute(text("INSERT INTO estatistica_lote (lote_id, gerado_em, resultado) VALUES (:l, :g, :r)"),  # grava o JSON com média, mediana e gráficos
                     {"l": lote_id, "g": agora_utc(), "r": json.dumps(payload, ensure_ascii=False)})            # o JSON vai como texto

        mensagem = f"ETL concluído: {len(tratado)} clientes tratados"                   # monta a mensagem final
        if invalidas:                                                                   # se alguma linha foi recusada
            mensagem += f", {invalidas} linhas recusadas"                               # acrescenta quantas
        conn.execute(text("""
            UPDATE upload_lote
               SET status = 'PROCESSADO', mensagem = :m, total_linhas = :t,
                   linhas_validas = :v, linhas_invalidas = :i, linhas_com_aviso = :a
             WHERE id = :id
        """), {"id": lote_id, "m": mensagem + ".", "t": total_linhas, "v": len(tratado), "i": invalidas, "a": com_aviso})  # marca o lote como PROCESSADO
        log.info("Lote %s gravado: %s clientes, %s consultores, %s serviços",           # escreve no terminal o resumo
                 lote_id, len(tratado), len(consultores), len(servicos))                # contagens
