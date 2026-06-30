"""
SupabaseSync.py — sincroniza alterações locais com o Supabase em background.
Funciona offline: guarda numa fila local e envia quando a internet voltar.
"""

import os
import json
import threading
import time
import sqlite3
from datetime import datetime
from contextlib import contextmanager
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")


BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.path.join(BASE_DIR, "data", "agenda.db")

_supabase_client = None
_client_lock     = threading.Lock()
_sync_thread     = None
_online          = False


# Cliente Supabase
def _get_cliente():
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
                    id          INTEGER PRIMARY KET AUTOINCREMENT,
                    operacao    TEXT NOT NULL,
                    tabela      TEXT NOT NULL,
                    dados       TEXT NOT NULL,
                    criado_em   TEXT NOT NULL
                )
            """)
    except Exception as e:
        print(f"[Sync] Erro ao garantir tabela fila: {e}")
        
def _adicionar_fila(operacao: str, tabela: str, dados: dict):
    """Adiciona uma operação à fila offline."""
    try:
        with _sqlite() as conn:
            conn.execute(
                "INSERT INTO sync_pendente (operacao, tabela, dados, criado_em) VALUES (?, ?, ?, ?)",
                (operacao, tabela, json.dumps(dados), datetime.now().isoformat())
            )
    except Exception as e:
        print(f"[Sync] Erro ao adicionar à fila: {e}")
        
def _ler_fila():
    """Lê todas as operações à fila offline."""
    try:
        with _sqlite() as conn:
            rows = conn.execute(
                "SELECT * FROM sync_pendente ORDER BY id ASC"
            ).fetchall()
            return [dict(r) for r in rows]
    except Exception:
        return []
    
def _remover_fila(ids: list):
    """Remove operações já enviadas da fila."""
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
        
# Envio para Supabase

def _enviar_operacao(client, operacao: str, tabela: str, dados: dict) -> bool:
    """Envia uma operação para o Supabase. Retorna True se sucesso."""
    try:
        if operacao == "upsert":
            if tabela == "clientes":
                client.table(tabela).upsert(dados, on_conflict="nome").execute()
            elif tabela == "marcacoes":
                client.table(tabela).upsert(dados, on_conflict="data_hora").execute()
            elif tabela == "anotacoes":
                client.table(tabela).upsert(dados).execute()
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
                    
        return True
    
    except Exception as e:
        print(f"[Sync] Erro ao enviar {operacao} em {tabela}: {e}")
        return False
    
def _processar_fila():
    """Tentar enviar tudo o que está na fila offline."""
    global _online
    client= _get_cliente()
    if not client:
        return
    
    pendentes = _ler_fila()
    if not pendentes:
        return
    
    print(f"[Sync] A processar {len(pendentes)} operação(ões) pendente(s)...")
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
        print(f"[Sync] {len(ids_ok)} operação(ões) sincronizada(s) com sucesso.")
        
    _online = len(ids_ok) == len(pendentes)
    
# Função principal de sync

def sincronizar(operacao: str, tabela: str, dados: dict):
    """
    Ponto de entrada principal.
    Tenta enviar imediatamente; se falhar, guarda na fila.
    """
    if not SUPABASE_URL or not SUPABASE_KEY:
        return
    
    client = _get_cliente()
    if not client:
        _adicionar_fila(operacao, tabela, dados)
        return
    
    ok = _enviar_operacao(client, operacao, tabela, dados)
    if not ok:
        _adicionar_fila(operacao, tabela, dados)
        
# Thread de background

def _loop_background():
    """Corre em background: tenta processar a fila a cada 5 minutos."""
    _garantir_tabela_fila()
    while True:
        try:
            _processar_fila()
        except Exception as e:
            print(f"[Sync] Erro no loop background: {e}")
        time.sleep(300)
        
def iniciar_sync_background():
    """Inicia a thread de sync em background"""
    global _sync_thread
    if _sync_thread is not None and _sync_thread.is_alive():
        return
    _sync_thread = threading.Thread(target=_loop_background, daemon=True)
    _sync_thread.start()
    print("[Sync] Thread de sincronização iniciada.")