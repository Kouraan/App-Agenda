import sqlite3
import os
from datetime import datetime
from contextlib import contextmanager

# Caminho para a base de dados — fica em Python/data/agenda.db
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB_PATH = os.path.join(BASE_DIR, "data", "agenda.db")


# ---------------------------------------------------------------------------
# Ligação
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------
# Criação do schema
# ---------------------------------------------------------------------------

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
        """)


# ---------------------------------------------------------------------------
# UTILIZADOR
# ---------------------------------------------------------------------------

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
        return True
    except Exception as e:
        print(f"[Database] guardar_utilizador: {e}")
        return False


# ---------------------------------------------------------------------------
# CLIENTES
# ---------------------------------------------------------------------------

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
        return True
    except Exception as e:
        print(f"[Database] atualizar_cliente: {e}")
        return False


def apagar_cliente(nome: str) -> bool:
    try:
        with _connect() as conn:
            conn.execute("DELETE FROM clientes WHERE nome=?", (nome,))
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


# ---------------------------------------------------------------------------
# MARCAÇÕES
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------
# ANOTAÇÕES
# ---------------------------------------------------------------------------

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
        return True
    except Exception as e:
        print(f"[Database] guardar_anotacoes: {e}")
        return False


# ---------------------------------------------------------------------------
# PENDENTES
# ---------------------------------------------------------------------------

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
    """Substitui toda a lista de pendentes (apaga e reinsere)."""
    try:
        with _connect() as conn:
            conn.execute("DELETE FROM pendentes")
            conn.executemany(
                "INSERT INTO pendentes (nome, numero_telefone) VALUES (?, ?)",
                [(p["nome"], p.get("numero_telefone", "")) for p in pendentes]
            )
        return True
    except Exception as e:
        print(f"[Database] guardar_pendentes: {e}")
        return False


# ---------------------------------------------------------------------------
# LOGS
# ---------------------------------------------------------------------------

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
        return True
    except Exception as e:
        print(f"[Database] inserir_log: {e}")
        return False


def ler_logs(tipo: str = None, limite: int = 200) -> list[dict]:
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


# ---------------------------------------------------------------------------
# MIGRAÇÃO — converte JSON antigo → BD (usar uma única vez)
# ---------------------------------------------------------------------------

def migrar_de_json():
    """
    Lê os ficheiros JSON existentes e popula a BD.
    Seguro de correr múltiplas vezes (usa INSERT OR IGNORE / verificações).
    """
    import json

    data_dir = os.path.join(BASE_DIR, "data")

    # --- utilizador ---
    path_u = os.path.join(data_dir, "utilizador.json")
    if os.path.exists(path_u):
        try:
            with open(path_u, encoding="utf-8") as f:
                u = json.load(f)
            if u and ler_utilizador() is None:
                guardar_utilizador(u.get("nome", ""), u.get("password", ""))
                print("[Migração] utilizador importado.")
        except Exception as e:
            print(f"[Migração] utilizador: {e}")

    # --- clientes ---
    path_c = os.path.join(data_dir, "clientes.json")
    if os.path.exists(path_c):
        try:
            with open(path_c, encoding="utf-8") as f:
                clientes = json.load(f) or []
            existentes = {c["nome"] for c in ler_clientes()}
            for c in clientes:
                if c["nome"] not in existentes:
                    inserir_cliente(
                        nome=c["nome"],
                        numero_telefone=c.get("numeroTelefone", ""),
                        tipo_cliente=c.get("tipoCliente", "NORMAL"),
                        faltas=c.get("faltas", 0),
                        dia_semana=c.get("diaSemana"),
                        hora_corte=c.get("horaCorte"),
                        rapido=bool(c.get("rapido", False))
                    )
            print(f"[Migração] {len(clientes)} clientes processados.")
        except Exception as e:
            print(f"[Migração] clientes: {e}")

    # --- anotações ---
    path_a = os.path.join(data_dir, "anotacoes.json")
    if os.path.exists(path_a):
        try:
            with open(path_a, encoding="utf-8") as f:
                texto = json.load(f) or ""
            guardar_anotacoes(texto)
            print("[Migração] anotações importadas.")
        except Exception as e:
            print(f"[Migração] anotações: {e}")

    # --- pendentes ---
    path_p = os.path.join(data_dir, "pendentes.json")
    if os.path.exists(path_p):
        try:
            with open(path_p, encoding="utf-8") as f:
                pendentes = json.load(f) or []
            existentes = {p["nome"] for p in ler_pendentes()}
            for p in pendentes:
                if p["nome"] not in existentes:
                    inserir_pendente(
                        nome=p["nome"],
                        numero_telefone=p.get("numeroTelefone", "")
                    )
            print(f"[Migração] {len(pendentes)} pendentes processados.")
        except Exception as e:
            print(f"[Migração] pendentes: {e}")

    # --- marcações ---
    marcacoes_dir = os.path.join(data_dir, "Marcacoes")
    if os.path.exists(marcacoes_dir):
        total = 0
        existentes_dh = {m["data_hora"] for m in ler_marcacoes()}
        for ano_dir in sorted(os.listdir(marcacoes_dir)):
            ano_path = os.path.join(marcacoes_dir, ano_dir)
            if not os.path.isdir(ano_path):
                continue
            for ficheiro in sorted(os.listdir(ano_path)):
                if not (ficheiro.startswith("marcacoes") and
                        ficheiro.endswith(".json")):
                    continue
                path_m = os.path.join(ano_path, ficheiro)
                try:
                    with open(path_m, encoding="utf-8") as f:
                        marcacoes = json.load(f) or []
                    bulk = []
                    for m in marcacoes:
                        dh = m.get("dataHora", "")
                        if dh and dh not in existentes_dh:
                            cliente_info = m.get("cliente", {})
                            bulk.append({
                                "data_hora": dh,
                                "cliente_nome": cliente_info.get("nome", ""),
                                "duracao": m.get("duracao", 30),
                                "observacoes": m.get("observacoes", ""),
                                "falta": int(m.get("falta", False))
                            })
                            existentes_dh.add(dh)
                    if bulk:
                        inserir_marcacoes_bulk(bulk)
                        total += len(bulk)
                except Exception as e:
                    print(f"[Migração] {path_m}: {e}")
        print(f"[Migração] {total} marcações importadas.")

    print("[Migração] Concluída.")