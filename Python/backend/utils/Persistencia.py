"""
Persistencia.py — camada de acesso a dados.

Antes usava JSON; agora delega tudo na base de dados SQLite via Database.py.
A interface pública (nomes dos métodos, tipos de retorno) mantém-se igual
para não quebrar o AppController nem nenhum outro código existente.
"""

from datetime import datetime, date, timedelta
from typing import Dict, List, Optional

from ..models.Utilizador import Utilizador
from ..models.Cliente import Cliente, TipoCliente
from ..models.Marcacao import Marcacao
from ..models.Pendente import Pendente
from . import Database


# Helpers de conversão

def _row_para_cliente(row: dict) -> Cliente:
    tipo_raw = row.get("tipo_cliente", "DESCONHECIDO")
    try:
        tipo = TipoCliente[tipo_raw.upper()]
    except KeyError:
        try:
            tipo = TipoCliente(tipo_raw)
        except Exception:
            tipo = TipoCliente.DESCONHECIDO

    cliente = Cliente(
        nome=row["nome"],
        numero_telefone=row["numero_telefone"],
        tipo_cliente=tipo,
        dia_semana=row.get("dia_semana"),
        hora_corte=row.get("hora_corte"),
        rapido=bool(row.get("rapido", 0))
    )
    cliente.set_faltas(row.get("faltas", 0))
    return cliente


def _row_para_marcacao(row: dict, clientes_map: dict) -> Optional[Marcacao]:
    try:
        dt = datetime.fromisoformat(row["data_hora"])
    except Exception:
        return None

    nome_cliente = row.get("cliente_nome", "")
    # tenta usar o cliente completo do mapa; se não existir cria um temporário
    if nome_cliente in clientes_map:
        cliente = clientes_map[nome_cliente]
    else:
        cliente = Cliente(
            nome=nome_cliente,
            numero_telefone="",
            tipo_cliente=TipoCliente.DESCONHECIDO
        )

    marcacao = Marcacao(
        data_hora=dt,
        cliente=cliente,
        duracao=row.get("duracao", 30),
        observacoes=row.get("observacoes", "")
    )
    marcacao.set_falta(bool(row.get("falta", 0)))
    return marcacao


# Leitura

def ler_utilizador() -> Optional[Utilizador]:
    row = Database.ler_utilizador()
    if row:
        return Utilizador(row["nome"], row["password"])
    return None


def ler_clientes() -> Dict[str, Cliente]:
    rows = Database.ler_clientes()
    resultado = {}
    for row in rows:
        c = _row_para_cliente(row)
        resultado[c.get_nome()] = c
    return resultado


def ler_marcacoes() -> Dict[datetime, Marcacao]:
    # precisamos do mapa de clientes para enriquecer as marcações
    clientes_map = ler_clientes()

    rows = Database.ler_marcacoes()
    resultado = {}
    for row in rows:
        m = _row_para_marcacao(row, clientes_map)
        if m:
            resultado[m.get_data_hora()] = m
    return resultado


def ler_anotacoes() -> str:
    return Database.ler_anotacoes()


def ler_pendentes() -> List[Pendente]:
    rows = Database.ler_pendentes()
    return [Pendente(r["nome"], r.get("numero_telefone", "")) for r in rows]


# Escrita

def guardar_utilizador(utilizador: Utilizador) -> bool:
    return Database.guardar_utilizador(
        utilizador.get_nome(),
        utilizador.get_password()
    )

def guardar_clientes(clientes: Dict[str, Cliente]) -> bool:
    """
    Sincroniza o dicionário de clientes com a BD:
    - insere os novos
    - actualiza os existentes
    - apaga os que já não estão no dicionário
    """
    try:
        existentes = {r["nome"]: r for r in Database.ler_clientes()}
        nomes_novos = set(clientes.keys())
        nomes_antigos = set(existentes.keys())

        # apagar os removidos
        for nome in nomes_antigos - nomes_novos:
            Database.apagar_cliente(nome)

        # inserir ou actualizar
        for nome, c in clientes.items():
            tipo_str = c.get_tipo_cliente().value \
                if hasattr(c.get_tipo_cliente(), "value") \
                else str(c.get_tipo_cliente())
            rapido = bool(c.is_rapido())
            faltas = c.get_faltas()
            dia = c.get_dia_semana()
            hora = c.get_hora_corte()
            numero = c.get_numero_telefone()

            if nome in existentes:
                Database.atualizar_cliente(
                    nome_original=nome,
                    nome=nome,
                    numero_telefone=numero,
                    tipo_cliente=tipo_str,
                    faltas=faltas,
                    dia_semana=dia,
                    hora_corte=hora,
                    rapido=rapido
                )
            else:
                Database.inserir_cliente(
                    nome=nome,
                    numero_telefone=numero,
                    tipo_cliente=tipo_str,
                    faltas=faltas,
                    dia_semana=dia,
                    hora_corte=hora,
                    rapido=rapido
                )
        return True
    except Exception as e:
        print(f"[Persistencia] guardar_clientes: {e}")
        return False

def guardar_marcacoes(marcacoes: Dict[datetime, Marcacao]) -> bool:
    """
    Sincroniza o dicionário de marcações com a BD:
    - insere as novas (INSERT OR IGNORE para semanais já existentes)
    - actualiza as modificadas
    - apaga as removidas (dentro da janela de leitura)
    """
    try:
        hoje = date.today()
        inicio_janela = datetime(hoje.year - 1, hoje.month, 1)  # ~12 meses atrás
        fim_janela = datetime(hoje.year + 3, 12, 31, 23, 59, 59)

        # existentes na BD dentro da janela
        existentes = {
            r["data_hora"]: r
            for r in Database.ler_marcacoes_periodo(
                inicio_janela.isoformat(),
                fim_janela.isoformat()
            )
        }

        nomes_novos = {m.get_data_hora().isoformat()
                       for m in marcacoes.values()}
        nomes_antigos = set(existentes.keys())

        # apagar removidas (dentro da janela)
        for dh_str in nomes_antigos - nomes_novos:
            Database.apagar_marcacao(dh_str)

        # inserir ou actualizar
        bulk_insert = []
        for dt, m in marcacoes.items():
            dh_str = dt.isoformat()
            c = m.get_cliente()
            cliente_nome = c.get_nome() if c is not None else ""
            duracao = m.get_duracao()
            observacoes = m.get_observacoes() or ""
            falta = int(m.is_falta())

            if dh_str not in existentes:
                bulk_insert.append({
                    "data_hora": dh_str,
                    "cliente_nome": cliente_nome,
                    "duracao": duracao,
                    "observacoes": observacoes,
                    "falta": falta
                })
            else:
                r = existentes[dh_str]
                # só actualiza se algo mudou
                if (r["cliente_nome"] != cliente_nome or
                        r["duracao"] != duracao or
                        r["observacoes"] != observacoes or
                        bool(r["falta"]) != bool(falta)):
                    Database.atualizar_marcacao(
                        data_hora_original=dh_str,
                        data_hora=dh_str,
                        cliente_nome=cliente_nome,
                        duracao=duracao,
                        observacoes=observacoes,
                        falta=bool(falta)
                    )

        if bulk_insert:
            Database.inserir_marcacoes_bulk(bulk_insert)

        return True
    except Exception as e:
        print(f"[Persistencia] guardar_marcacoes: {e}")
        return False

def guardar_anotacoes(anotacoes: str) -> bool:
    return Database.guardar_anotacoes(anotacoes if anotacoes else "")


def guardar_pendentes(pendentes: List[Pendente]) -> bool:
    dados = [
        {"nome": p.get_nome(), "numero_telefone": p.get_numero_telefone()}
        for p in pendentes
    ]
    return Database.guardar_pendentes(dados)