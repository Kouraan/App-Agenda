import sqlite3
import os
from datetime import datetime
from contextlib import contextmanager
from typing import Optional

# Caminho para a base de dados
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB_PATH = os.path.join(BASE_DIR, "data", "agenda.db")

# Sync com Supabase
def _sync(operacao: str, tabela: str, dados: dict):
    try:
        from . import SupabaseSync
        SupabaseSync.sincronizar(operacao, tabela, dados)
    except Exception:
        pass


# Ligação

@contextmanager
def _connect():
    """Context manager que abre e fecha a ligação garantindo commit/rollback."""
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH, detect_types=sqlite3.PARSE_DECLTYPES)
    conn.row_factory = sqlite3.Row          # acesso por nome de coluna
    conn.execute("PRAGMA journal_mode=WAL") # melhor concorrência
    conn.execute("PRAGMA foreign_keys=ON")
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


# Criação do schema

def inicializar_bd():
    """Cria todas as tabelas se ainda não existirem."""
    with _connect() as conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS utilizador (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                nome     TEXT    NOT NULL,
                password TEXT    NOT NULL
            );

            CREATE TABLE IF NOT EXISTS clientes (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                nome             TEXT    NOT NULL UNIQUE,
                numero_telefone  TEXT    NOT NULL,  
                tipo_cliente     TEXT    NOT NULL DEFAULT 'NORMAL',
                faltas           INTEGER NOT NULL DEFAULT 0,
                dia_semana       TEXT,
                hora_corte       TEXT,
                rapido           INTEGER NOT NULL DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS marcacoes (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                data_hora   TEXT    NOT NULL UNIQUE,
                cliente_nome TEXT   NOT NULL,
                duracao     INTEGER NOT NULL DEFAULT 30,
                observacoes TEXT    NOT NULL DEFAULT '',
                falta       INTEGER NOT NULL DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS anotacoes (
                id    INTEGER PRIMARY KEY CHECK (id = 1),
                texto TEXT    NOT NULL DEFAULT ''
            );

            CREATE TABLE IF NOT EXISTS pendentes (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                nome             TEXT    NOT NULL,
                numero_telefone  TEXT    NOT NULL DEFAULT ''
            );

            CREATE TABLE IF NOT EXISTS logs (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                data_hora TEXT    NOT NULL,
                tipo      TEXT    NOT NULL,
                mensagem  TEXT    NOT NULL
            );

            CREATE INDEX IF NOT EXISTS idx_marcacoes_data_hora
                ON marcacoes(data_hora);

            CREATE INDEX IF NOT EXISTS idx_logs_tipo
                ON logs(tipo);
                
            CREATE TABLE IF NOT EXISTS slots_semanais_usados (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                cliente_nome TEXT    NOT NULL,
                data_hora    TEXT    NOT NULL,
                UNIQUE(cliente_nome, data_hora)
            );
            
            CREATE INDEX IF NOT EXISTS idx_slots_semanais_cliente
                ON slots_semanais_usados(cliente_nome);
                
            CREATE INDEX IF NOT EXISTS idx_clientes_nome_lower
                ON clientes(LOWER(nome));

            CREATE INDEX IF NOT EXISTS idx_clientes_telefone
                ON clientes(numero_telefone);
        """)


# Utilizador

def ler_utilizador():
    """Devolve (nome, password) ou None."""
    with _connect() as conn:
        row = conn.execute(
            "SELECT nome, password FROM utilizador LIMIT 1"
        ).fetchone()
        return dict(row) if row else None


def guardar_utilizador(nome: str, password: str) -> bool:
    """Insere ou actualiza o utilizador (há sempre apenas 1)."""
    try:
        with _connect() as conn:
            existe = conn.execute(
                "SELECT id FROM utilizador LIMIT 1"
            ).fetchone()
            if existe:
                conn.execute(
                    "UPDATE utilizador SET nome=?, password=? WHERE id=?",
                    (nome, password, existe["id"])
                )
            else:
                conn.execute(
                    "INSERT INTO utilizador (nome, password) VALUES (?, ?)",
                    (nome, password)
                )
        _sync("upsert", "utilizador", {"nome": nome, "password": password})
        return True
    except Exception as e:
        print(f"[Database] guardar_utilizador: {e}")
        return False


# Clientes

def ler_clientes() -> list[dict]:
    """Devolve lista de dicts com todos os clientes, ordenados por nome."""
    with _connect() as conn:
        rows = conn.execute(
            "SELECT * FROM clientes ORDER BY LOWER(nome)"
        ).fetchall()
        return [dict(r) for r in rows]


def inserir_cliente(nome: str, numero_telefone: str, tipo_cliente: str,
                    faltas: int = 0, dia_semana=None, hora_corte=None,
                    rapido: bool = False) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                """INSERT INTO clientes
                   (nome, numero_telefone, tipo_cliente, faltas,
                    dia_semana, hora_corte, rapido)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (nome, numero_telefone, tipo_cliente, faltas,
                 dia_semana, hora_corte, int(rapido))
            )
        _sync("upsert", "clientes", {
            "nome": nome, "numero_telefone": numero_telefone,
            "tipo_cliente": tipo_cliente, "faltas": faltas,
            "dia_semana": dia_semana, "hora_corte": hora_corte,
            "rapido": int(rapido)
        })
        return True
    except Exception as e:
        print(f"[Database] inserir_cliente: {e}")
        return False


def atualizar_cliente(nome_original: str, nome: str, numero_telefone: str,
                      tipo_cliente: str, faltas: int, dia_semana=None,
                      hora_corte=None, rapido: bool = False) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                """UPDATE clientes
                   SET nome=?, numero_telefone=?, tipo_cliente=?,
                       faltas=?, dia_semana=?, hora_corte=?, rapido=?
                   WHERE nome=?""",
                (nome, numero_telefone, tipo_cliente, faltas,
                 dia_semana, hora_corte, int(rapido), nome_original)
            )
        if nome != nome_original:
            _sync("delete", "clientes", {"_campo": "nome", "_valor": nome_original})
        _sync("upsert", "clientes", {
            "nome": nome, "numero_telefone": numero_telefone,
            "tipo_cliente": tipo_cliente, "faltas": faltas,
            "dia_semana": dia_semana, "hora_corte": hora_corte,
            "rapido": int(rapido)
        })
        return True
    except Exception as e:
        print(f"[Database] atualizar_cliente: {e}")
        return False


def apagar_cliente(nome: str) -> bool:
    try:
        with _connect() as conn:
            conn.execute("DELETE FROM clientes WHERE nome=?", (nome,))
        _sync("delete", "clientes", {"_campo": "nome", "_valor": nome})
        return True
    except Exception as e:
        print(f"[Database] apagar_cliente: {e}")
        return False


def atualizar_faltas_cliente(nome: str, faltas: int) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                "UPDATE clientes SET faltas=? WHERE nome=?",
                (faltas, nome)
            )
        return True
    except Exception as e:
        print(f"[Database] atualizar_faltas_cliente: {e}")
        return False


# Marcações

def ler_marcacoes() -> list[dict]:
    """Devolve todas as marcações ordenadas por data_hora."""
    with _connect() as conn:
        rows = conn.execute(
            "SELECT * FROM marcacoes ORDER BY data_hora"
        ).fetchall()
        return [dict(r) for r in rows]


def ler_marcacoes_periodo(data_inicio: str, data_fim: str) -> list[dict]:
    """Devolve marcações entre duas datas ISO (inclusive)."""
    with _connect() as conn:
        rows = conn.execute(
            """SELECT * FROM marcacoes
               WHERE data_hora >= ? AND data_hora <= ?
               ORDER BY data_hora""",
            (data_inicio, data_fim)
        ).fetchall()
        return [dict(r) for r in rows]


def inserir_marcacao(data_hora: str, cliente_nome: str, duracao: int,
                     observacoes: str = "", falta: bool = False) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                """INSERT INTO marcacoes
                   (data_hora, cliente_nome, duracao, observacoes, falta)
                   VALUES (?, ?, ?, ?, ?)""",
                (data_hora, cliente_nome, duracao, observacoes, int(falta))
            )
        return True
    except Exception as e:
        print(f"[Database] inserir_marcacao: {e}")
        return False


def inserir_marcacoes_bulk(marcacoes: list[dict]) -> bool:
    """Insere múltiplas marcações de uma vez (ignora duplicados)."""
    try:
        with _connect() as conn:
            conn.executemany(
                """INSERT OR IGNORE INTO marcacoes
                   (data_hora, cliente_nome, duracao, observacoes, falta)
                   VALUES (:data_hora, :cliente_nome, :duracao,
                           :observacoes, :falta)""",
                marcacoes
            )
        for m in marcacoes:
            _sync("upsert", "marcacoes", m)
        return True
    except Exception as e:
        print(f"[Database] inserir_marcacoes_bulk: {e}")
        return False


def atualizar_marcacao(data_hora_original: str, data_hora: str,
                       cliente_nome: str, duracao: int,
                       observacoes: str, falta: bool) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                """UPDATE marcacoes
                   SET data_hora=?, cliente_nome=?, duracao=?,
                       observacoes=?, falta=?
                   WHERE data_hora=?""",
                (data_hora, cliente_nome, duracao,
                 observacoes, int(falta), data_hora_original)
            )
        if data_hora != data_hora_original:
            _sync("delete", "marcacoes", {"_campo": "data_hora", "_valor": data_hora_original})
        _sync("upsert", "marcacoes", {
            "data_hora": data_hora, "cliente_nome": cliente_nome,
            "duracao": duracao, "observacoes": observacoes, "falta": int(falta)
        })
        return True
    except Exception as e:
        print(f"[Database] atualizar_marcacao: {e}")
        return False


def apagar_marcacao(data_hora: str) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                "DELETE FROM marcacoes WHERE data_hora=?", (data_hora,)
            )
        _sync("delete", "marcacoes", {"_campo": "data_hora", "_valor": data_hora})
        return True
    except Exception as e:
        print(f"[Database] apagar_marcacao: {e}")
        return False


def apagar_marcacoes_futuras_cliente(cliente_nome: str,
                                     a_partir_de: str) -> bool:
    """Remove marcações futuras (>= a_partir_de) de um cliente."""
    try:
        with _connect() as conn:
            conn.execute(
                """DELETE FROM marcacoes
                   WHERE cliente_nome=? AND data_hora >= ?""",
                (cliente_nome, a_partir_de)
            )
        _sync("delete_futuras_cliente", "marcacoes", {
            "cliente_nome": cliente_nome,
            "a_partir_de": a_partir_de
        })
        return True
    except Exception as e:
        print(f"[Database] apagar_marcacoes_futuras_cliente: {e}")
        return False


def marcar_falta_marcacao(data_hora: str) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                "UPDATE marcacoes SET falta=1 WHERE data_hora=?",
                (data_hora,)
            )
        return True
    except Exception as e:
        print(f"[Database] marcar_falta_marcacao: {e}")
        return False


# Anotações

def ler_anotacoes() -> str:
    with _connect() as conn:
        row = conn.execute(
            "SELECT texto FROM anotacoes WHERE id=1"
        ).fetchone()
        return row["texto"] if row else ""


def guardar_anotacoes(texto: str) -> bool:
    try:
        with _connect() as conn:
            existe = conn.execute(
                "SELECT id FROM anotacoes WHERE id=1"
            ).fetchone()
            if existe:
                conn.execute(
                    "UPDATE anotacoes SET texto=? WHERE id=1", (texto,)
                )
            else:
                conn.execute(
                    "INSERT INTO anotacoes (id, texto) VALUES (1, ?)",
                    (texto,)
                )
        _sync("upsert", "anotacoes", {"id": 1, "texto": texto})
        return True
    except Exception as e:
        print(f"[Database] guardar_anotacoes: {e}")
        return False


# Pendentes

def ler_pendentes() -> list[dict]:
    with _connect() as conn:
        rows = conn.execute(
            "SELECT * FROM pendentes ORDER BY id"
        ).fetchall()
        return [dict(r) for r in rows]


def inserir_pendente(nome: str, numero_telefone: str = "") -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                "INSERT INTO pendentes (nome, numero_telefone) VALUES (?, ?)",
                (nome, numero_telefone)
            )
        return True
    except Exception as e:
        print(f"[Database] inserir_pendente: {e}")
        return False


def apagar_pendente_por_nome(nome: str) -> bool:
    try:
        with _connect() as conn:
            conn.execute(
                "DELETE FROM pendentes WHERE nome=?", (nome,)
            )
        return True
    except Exception as e:
        print(f"[Database] apagar_pendente_por_nome: {e}")
        return False


def guardar_pendentes(pendentes: list[dict]) -> bool:
    try:
        with _connect() as conn:
            conn.execute("DELETE FROM pendentes")
            conn.executemany(
                "INSERT INTO pendentes (nome, numero_telefone) VALUES (?, ?)",
                [(p["nome"], p.get("numero_telefone", "")) for p in pendentes]
            )
        try:
            from . import SupabaseSync
            client = SupabaseSync._get_client()
            if client:
                client.table("pendentes").delete().neq("id", 0).execute()
                if pendentes:
                    client.table("pendentes").insert(pendentes).execute()
        except Exception:
            pass
        return True
    except Exception as e:
        print(f"[Database] guardar_pendentes: {e}")
        return False


# Logs

def inserir_log(tipo: str, mensagem: str) -> bool:
    """
    tipo: 'utilizador' | 'cliente' | 'marcacao' | 'pendente'
    """
    try:
        data_hora = datetime.now().strftime("%d/%m/%Y %H:%M:%S")
        with _connect() as conn:
            conn.execute(
                "INSERT INTO logs (data_hora, tipo, mensagem) VALUES (?, ?, ?)",
                (data_hora, tipo, mensagem)
            )
        _sync("upsert", "logs", {
            "data_hora": data_hora, "tipo": tipo, "mensagem": mensagem
        })
        return True
    except Exception as e:
        print(f"[Database] inserir_log: {e}")
        return False


def ler_logs(tipo: Optional[str] = None, limite: int = 200) -> list[dict]:
    """Devolve logs mais recentes, opcionalmente filtrados por tipo."""
    with _connect() as conn:
        if tipo:
            rows = conn.execute(
                """SELECT * FROM logs WHERE tipo=?
                   ORDER BY id DESC LIMIT ?""",
                (tipo, limite)
            ).fetchall()
        else:
            rows = conn.execute(
                "SELECT * FROM logs ORDER BY id DESC LIMIT ?",
                (limite,)
            ).fetchall()
        return [dict(r) for r in rows]
   
# slots semanais usados (para evitar duplicados ao gerar marcações recorrentes)
 
def inserir_slots_semanais_bulk(slots: list[dict]) -> bool:
    """Regista slots semanais já gerados (ignora duplicados)."""
    try:
        with _connect() as conn:
            conn.executemany(
                "INSERT OR IGNORE INTO slots_semanais_usados (cliente_nome, data_hora) VALUES (?, ?)",
                [(s["cliente_nome"], s["data_hora"]) for s in slots]
            )
        return True
    except Exception as e:
        print(f"[Database] inserir_slots_semanais_bulk: {e}")
        return False


def ler_slots_semanais_usados(cliente_nome: str) -> list[str]:
    """Devolve lista de data_hora ISO dos slots já gerados para este cliente."""
    with _connect() as conn:
        rows = conn.execute(
            "SELECT data_hora FROM slots_semanais_usados WHERE cliente_nome = ?",
            (cliente_nome,)
        ).fetchall()
        return [r["data_hora"] for r in rows]


def apagar_slots_semanais_cliente(cliente_nome: str) -> bool:
    """Remove todos os slots usados de um cliente (ex: ao mudar horário semanal)."""
    try:
        with _connect() as conn:
            conn.execute(
                "DELETE FROM slots_semanais_usados WHERE cliente_nome = ?",
                (cliente_nome,)
            )
        return True
    except Exception as e:
        print(f"[Database] apagar_slots_semanais_cliente: {e}")
        return False
    
def cliente_existe_por_nome(nome: str, excluir_nome: str = None) -> bool:
    """Verifica se existe cliente com este nome (ignora capitalização)."""
    with _connect() as conn:
        if excluir_nome:
            row = conn.execute(
                """SELECT 1 FROM clientes 
                   WHERE LOWER(nome) = LOWER(?) 
                   AND LOWER(nome) != LOWER(?)
                   LIMIT 1""",
                (nome, excluir_nome)
            ).fetchone()
        else:
            row = conn.execute(
                """SELECT 1 FROM clientes 
                   WHERE LOWER(nome) = LOWER(?) 
                   LIMIT 1""",
                (nome,)
            ).fetchone()
        return row is not None


def cliente_existe_por_telefone(numero: str, excluir_nome: str = None) -> bool:
    """Verifica se existe cliente com este número de telefone."""
    with _connect() as conn:
        if excluir_nome:
            row = conn.execute(
                """SELECT 1 FROM clientes 
                   WHERE numero_telefone = ? 
                   AND LOWER(nome) != LOWER(?)
                   LIMIT 1""",
                (numero, excluir_nome)
            ).fetchone()
        else:
            row = conn.execute(
                """SELECT 1 FROM clientes 
                   WHERE numero_telefone = ? 
                   LIMIT 1""",
                (numero,)
            ).fetchone()
        return row is not None