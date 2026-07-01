"""
SupabaseSync.py — sincroniza alterações locais com o Supabase em background.
Funciona offline: guarda numa fila local e envia quando a internet voltar.
TODAS as operações são feitas em background para não bloquear a UI.
"""

import os
import json
import threading
import time
import sqlite3
from datetime import datetime
from contextlib import contextmanager

def _carregar_env():
    """
    Carrega as variáveis de ambiente.
    Procura o .env em múltiplos locais para funcionar
    tanto em desenvolvimento como no executável.
    """
    # Locais onde procurar o .env
    possiveis = []

    import sys
    if getattr(sys, 'frozen', False):
        possiveis.append(os.path.join(os.path.dirname(sys.executable), '.env'))
        possiveis.append(os.path.join(os.path.dirname(sys.executable), 'config.env'))
    
    base = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    possiveis.append(os.path.join(base, '.env'))
    possiveis.append(os.path.join(base, 'config.env'))
    
    possiveis.append(os.path.join(os.getcwd(), '.env'))
    possiveis.append(os.path.join(os.getcwd(), 'config.env'))

    for caminho in possiveis:
        if os.path.exists(caminho):
            try:
                from dotenv import load_dotenv
                load_dotenv(caminho)
                print(f"[Sync] Configuração carregada de: {caminho}")
                return True
            except Exception as e:
                print(f"[Sync] Erro ao carregar {caminho}: {e}")
    
    print("[Sync] Ficheiro .env não encontrado — sync desactivado")
    return False

_carregar_env()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB_PATH  = os.path.join(BASE_DIR, "data", "agenda.db")

_supabase_client = None
_client_lock     = threading.Lock()
_sync_thread     = None
_fila_lock       = threading.Lock()
_iniciado        = False


# Cliente Supabase

def _get_client():
    global _supabase_client
    if _supabase_client is None:
        with _client_lock:
            if _supabase_client is None:
                try:
                    from supabase import create_client
                    if SUPABASE_URL and SUPABASE_KEY:
                        _supabase_client = create_client(SUPABASE_URL, SUPABASE_KEY)
                except Exception as e:
                    print(f"[Sync] Erro ao criar cliente Supabase: {e}")
    return _supabase_client


# Fila offline

@contextmanager
def _sqlite():
    """Abre a BD local apenas se a pasta data/ já existir."""
    if not os.path.exists(os.path.dirname(DB_PATH)):
        raise RuntimeError("Pasta data/ ainda não existe")
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def _garantir_tabela_fila():
    """Garante que a tabela sync_pendente existe no SQLite local."""
    try:
        with _sqlite() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS sync_pendente (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    operacao  TEXT NOT NULL,
                    tabela    TEXT NOT NULL,
                    dados     TEXT NOT NULL,
                    criado_em TEXT NOT NULL
                )
            """)
    except Exception as e:
        print(f"[Sync] Erro ao garantir tabela fila: {e}")


def _adicionar_fila(operacao: str, tabela: str, dados: dict):
    """Adiciona uma operação à fila offline."""
    try:
        with _sqlite() as conn:
            conn.execute(
                """INSERT INTO sync_pendente 
                   (operacao, tabela, dados, criado_em) 
                   VALUES (?, ?, ?, ?)""",
                (operacao, tabela, json.dumps(dados), datetime.now().isoformat())
            )
    except Exception as e:
        print(f"[Sync] Erro ao adicionar à fila: {e}")


def _ler_fila():
    try:
        with _sqlite() as conn:
            rows = conn.execute(
                "SELECT * FROM sync_pendente ORDER BY id ASC LIMIT 50"
            ).fetchall()
            return [dict(r) for r in rows]
    except Exception:
        return []


def _remover_fila(ids: list):
    if not ids:
        return
    try:
        with _sqlite() as conn:
            placeholders = ",".join("?" * len(ids))
            conn.execute(
                f"DELETE FROM sync_pendente WHERE id IN ({placeholders})", ids
            )
    except Exception as e:
        print(f"[Sync] Erro ao limpar fila: {e}")


# Envio para o Supabase

def _enviar_operacao(client, operacao: str, tabela: str, dados: dict) -> bool:
    try:
        if operacao == "upsert":
            if tabela == "clientes":
                client.table(tabela).upsert(dados, on_conflict="nome").execute()
            elif tabela == "marcacoes":
                client.table(tabela).upsert(dados, on_conflict="data_hora").execute()
            else:
                client.table(tabela).upsert(dados).execute()

        elif operacao == "delete":
            campo = dados.get("_campo")
            valor = dados.get("_valor")
            if campo and valor:
                client.table(tabela).delete().eq(campo, valor).execute()

        elif operacao == "delete_futuras_cliente":
            nome     = dados.get("cliente_nome")
            a_partir = dados.get("a_partir_de")
            if nome and a_partir:
                client.table("marcacoes").delete()\
                    .eq("cliente_nome", nome)\
                    .gte("data_hora", a_partir)\
                    .execute()

        elif operacao == "delete_pendentes_todos":
            client.table("pendentes").delete().neq("id", 0).execute()

        elif operacao == "insert_pendentes":
            if dados.get("lista"):
                client.table("pendentes").insert(dados["lista"]).execute()

        return True

    except Exception as e:
        print(f"[Sync] Erro ao enviar {operacao} em {tabela}: {e}")
        return False


def _processar_fila():
    """Tenta enviar tudo o que está na fila offline."""
    client = _get_client()
    if not client:
        return

    pendentes = _ler_fila()
    if not pendentes:
        return

    ids_ok = []
    for item in pendentes:
        try:
            dados = json.loads(item["dados"])
            ok    = _enviar_operacao(client, item["operacao"], item["tabela"], dados)
            if ok:
                ids_ok.append(item["id"])
        except Exception as e:
            print(f"[Sync] Erro ao processar item {item['id']}: {e}")

    if ids_ok:
        _remover_fila(ids_ok)
        print(f"[Sync] {len(ids_ok)} operação(ões) sincronizada(s).")


# Função principal

def sincronizar(operacao: str, tabela: str, dados: dict):
    """
    Adiciona à fila e processa em background.
    NUNCA bloqueia a thread principal.
    """
    if not SUPABASE_URL or not SUPABASE_KEY:
        return

    def _tarefa():
        with _fila_lock:
            _adicionar_fila(operacao, tabela, dados)
            _processar_fila()

    threading.Thread(target=_tarefa, daemon=True).start()


# Loop de background

def _loop_background():
    """
    Corre em background continuamente.
    Aguarda a BD estar disponível antes de começar.
    """
    # Espera até a pasta data/ existir (pode demorar no arranque)
    for _ in range(30):
        if os.path.exists(DB_PATH):
            break
        time.sleep(1)

    _garantir_tabela_fila()

    while True:
        try:
            with _fila_lock:
                _processar_fila()
        except Exception as e:
            print(f"[Sync] Erro no loop background: {e}")
        time.sleep(120)


def iniciar_sync_background():
    """Inicia a thread de sync em background. Chamar uma vez no arranque."""
    global _sync_thread, _iniciado
    if _iniciado:
        return
    _iniciado = True
    _sync_thread = threading.Thread(target=_loop_background, daemon=True)
    _sync_thread.start()
    print("[Sync] Thread de sincronização iniciada.")