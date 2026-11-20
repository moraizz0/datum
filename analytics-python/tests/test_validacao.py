"""Testes da validação e do fluxo de preparação (sem banco). Rodar: python -m pytest tests -v"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))   # deixa importar os .py da pasta analytics-python

import pytest

from main import SemLinhasValidas, preparar
from validacao import relatorio, validar

AMOSTRAS = Path(__file__).resolve().parent.parent / "amostras"


def linha(**kw):
    base = {"codigoCliente": "CTI001", "nomeCliente": "Metalúrgica Horizonte", "consultor": "Ana Souza",
            "segmento": "Indústria", "nivelCliente": "A", "faturamentoAnual": 1850000,
            "servicosContratados": "Internet Dedicada;Firewall", "dataContratacao": "2025-01-15",
            "cidade": "Campinas", "uf": "SP"}
    base.update(kw)
    return base


def uma(**kw):
    return validar([linha(**kw)])[0]


def test_linha_valida_sem_avisos():
    r = uma()
    assert r.valida and not r.avisos
    assert r.valores["data_contratacao"] == "2025-01-15"


@pytest.mark.parametrize("campo", ["codigoCliente", "nomeCliente", "consultor", "segmento", "nivelCliente",
                                   "faturamentoAnual", "servicosContratados", "dataContratacao"])
def test_obrigatorios(campo):
    r = uma(**{campo: ""})
    assert not r.valida and any("obrigatório" in e for e in r.erros)


def test_cidade_e_uf_sao_opcionais():
    assert uma(cidade="", uf="").valida


def test_segmento_padronizado_vira_aviso():
    r = uma(segmento="IND.")
    assert r.valida and r.valores["segmento"] == "Indústria" and any("padronizado" in a for a in r.avisos)


def test_segmento_desconhecido_e_erro():
    r = uma(segmento="Agro")
    assert not r.valida and any("não reconhecido" in e for e in r.erros)


def test_consultor_vira_titulo():
    assert uma(consultor="ANA da SILVA").valores["consultor"] == "Ana da Silva"


@pytest.mark.parametrize("nivel,valida", [("A", True), ("c", True), ("D", False), ("AB", False)])
def test_nivel(nivel, valida):
    assert uma(nivelCliente=nivel).valida is valida


@pytest.mark.parametrize("valor,esperado", [
    (1850000, 1850000.0), ("1850000", 1850000.0), ("1.850.000,50", 1850000.5),
    ("R$ 1.850,00", 1850.0), ("1850.5", 1850.5), (0, 0.0)])
def test_faturamento_aceita_varios_formatos(valor, esperado):
    r = uma(faturamentoAnual=valor)
    assert r.valida and r.valores["faturamento_anual"] == esperado


@pytest.mark.parametrize("valor", ["abc", -1, "-50", 10**13, "1e5"])
def test_faturamento_invalido(valor):
    assert not uma(faturamentoAnual=valor).valida


def test_faturamento_zero_gera_aviso():
    assert any("zero" in a for a in uma(faturamentoAnual=0).avisos)


@pytest.mark.parametrize("data,esperado", [
    ("2025-01-15", "2025-01-15"), ("15/01/2025", "2025-01-15"), ("5/3/2024", "2024-03-05"),
    (45356, "2024-03-05"), ("2024-03-05T00:00:00Z", "2024-03-05")])
def test_datas_aceitas(data, esperado):
    r = uma(dataContratacao=data)
    assert r.valida and r.valores["data_contratacao"] == esperado


@pytest.mark.parametrize("data", ["31/02/2025", "ontem", "1980-01-01", "2025-13-01"])
def test_datas_invalidas(data):
    assert not uma(dataContratacao=data).valida


def test_data_futura_gera_aviso():
    r = uma(dataContratacao="2999-01-01")
    assert r.valida and any("futuro" in a for a in r.avisos)


def test_servicos_viram_lista_sem_repetir():
    r = uma(servicosContratados="mpls ; Cloud ;; MPLS")
    assert r.valida and r.valores["servicos_contratados"] == "MPLS;Cloud"


def test_servicos_so_separadores_e_erro():
    assert not uma(servicosContratados=" ; ; ").valida


@pytest.mark.parametrize("uf,valida", [("SP", True), ("sp", True), ("S", False), ("S1", False), ("SPP", False)])
def test_uf(uf, valida):
    assert uma(uf=uf).valida is valida


def test_codigo_duplicado_ignora_maiusculas():
    r = validar([linha(codigoCliente="cti001"), linha(codigoCliente="CTI001", nomeCliente="Outra")])
    assert r[0].valida and not r[1].valida and "linha 2" in r[1].erros[0]


def test_codigo_com_caractere_invalido():
    assert not uma(codigoCliente="CTI 1!").valida


def test_duplicidade_provavel_e_so_aviso():
    r = validar([linha(codigoCliente="A1"), linha(codigoCliente="A2")])
    assert r[1].valida and any("possível duplicidade" in a for a in r[1].avisos)


def test_numero_da_linha_vem_do_backend():
    r = validar([linha(numeroLinha=17)])
    assert r[0].numero == 17


def test_relatorio_conta_certo():
    linhas = json.loads((AMOSTRAS / "clientes_com_erros.json").read_text(encoding="utf-8"))
    rel = relatorio(validar(linhas))
    assert (rel["totalLinhas"], rel["linhasValidas"], rel["linhasInvalidas"]) == (10, 5, 5)
    assert len(rel["rejeitadas"]) == 5 and rel["rejeitadas"][0].startswith("Linha 4:")


def test_preparar_calcula_so_com_as_validas():
    linhas = json.loads((AMOSTRAS / "clientes_com_erros.json").read_text(encoding="utf-8"))
    rel, res, payload = preparar(linhas, recebidas=12)
    assert len(res.tratado) == 5
    assert payload["resumo"]["totalClientes"] == 5
    assert payload["qualidade"]["linhasRecebidas"] == 12 and payload["qualidade"]["linhasInvalidas"] == 7
    assert payload["insights"]
    json.dumps(payload)   # precisa ser serializável em JSON


def test_amostra_da_aula():
    linhas = json.loads((AMOSTRAS / "clientes.json").read_text(encoding="utf-8"))
    _, res, payload = preparar(linhas)
    assert list(res.tratado["consultor"]) == ["Ana Souza", "Carlos Lima"]
    assert list(res.tratado["segmento"]) == ["Indústria", "Comércio"]
    assert payload["resumo"]["media"] == 1135000.0


def test_sem_nenhuma_linha_valida():
    with pytest.raises(SemLinhasValidas) as e:
        preparar([linha(nomeCliente="")])
    assert e.value.relatorio["linhasInvalidas"] == 1
