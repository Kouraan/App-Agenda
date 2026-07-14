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
from datetime import datetime, timezone
from dateutil import parser as date_parser
from typing import Any, Dict, List, cast
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

_sessao_valida_ate = 0

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB_PATH  = os.path.join(BASE_DIR, "data", "agenda.db")

_supabase_client = None
_client_lock     = threading.Lock()
_sync_thread     = None
_fila_lock       = threading.Lock()
_iniciado        = False


def _parse_ts(valor: str):
    """Converte um ISO string (com ou sem offset) para datetime aware em UTC."""
    if not valor:
        return None
    dt = date_parser.isoparse(valor)
    if dt.tzinfo is None:
        dt = dt.astimezone()
    return dt.astimezone(timezone.utc)

def _autenticar(client) -> bool:
    """Garante que o cliente tem uma sessão válida. Só reautentica se necessário."""
    global _sessao_valida_ate
    agora = time.time()
    if agora < _sessao_valida_ate - 60:
        return True
    
    email = os.getenv("SUPABASE_SERVICE_EMAIL")
    password = os.getenv("SUPABASE_SERVICE_PASSWORD")
    if not email or not password:
        print("[Sync] SUPABASE_SERVICE_EMAIL/PASSWORD não configurados no .env")
        return False
    
    try:
        res = client.auth.sign_in_with_password({"email": email, "password": password})
        if res and res.session:
            _sessao_valida_ate = agora + res.session.expires_in
            return True
    except Exception as e:
        print(f"[Sync] Falha na autenticação Supabase (provavelmente sem internet): {e}")
    return False

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

def _get_client_autenticado():
    """Devolve o cliente Supabase já autenticado, ou None se não for possível
    (sem internet, credenciais em falta, ou falha de autenticação)."""
    client = _get_client()
    if not client:
        return None
    if not _autenticar(client):
        return None
    return client


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
                client.table("sync_tombstones").insert({"tabela": tabela, "chave": valor}).execute()

        elif operacao == "delete_futuras_cliente":
            nome     = dados.get("cliente_nome")
            a_partir = dados.get("a_partir_de")
            if nome and a_partir:
                res = client.table("marcacoes").select("data_hora")\
                    .eq("cliente_nome", nome).gte("data_hora", a_partir).execute()
                chaves = [r["data_hora"] for r in (res.data or [])]
                client.table("marcacoes").delete()\
                    .eq("cliente_nome", nome)\
                    .gte("data_hora", a_partir)\
                    .execute()
                if chaves:
                    client.table("sync_tombstones").insert(
                        [{"tabela": "marcacoes", "chave": k} for k in chaves]
                    ).execute()
                    
        elif operacao == "delete_marcacoes_antes":
            antes_de = dados.get("antes_de")
            if antes_de:
                res = client.table("marcacoes").select("data_hora")\
                    .lt("data_hora", antes_de).execute()
                chaves = [r["data_hora"] for r in (res.data or [])]
                client.table("marcacoes").delete()\
                    .lt("data_hora", antes_de)\
                    .execute()
                if chaves:
                    client.table("sync_tombstones").insert(
                        [{"tabela": "marcacoes", "chave": k} for k in chaves]
                    ).execute()

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
    client = _get_client_autenticado()
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

def processar_fila_bloqueante():
    """
    Versão síncrona de _processar_fila, para ser chamada explicitamente
    no arranque da app, ANTES de qualquer pull — garante que as
    alterações feitas offline (ou na sessão anterior) são enviadas
    primeiro, evitando que o pull as sobreponha com dados desatualizados.
    """
    _garantir_tabela_fila()
    with _fila_lock:
        _processar_fila()

# Função principal

def sincronizar(operacao: str, tabela: str, dados: dict):
    """
    Grava na fila de forma SÍNCRONA (garante a ordem cronológica real das
    operações) e só delega para background o envio de rede.
    """
    if not SUPABASE_URL or not SUPABASE_KEY:
        return

    _garantir_tabela_fila()
    _adicionar_fila(operacao, tabela, dados)
    
    def _tarefa():
        with _fila_lock:
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
    
def puxar_alteracoes() -> dict:
    """
    Puxa alterações da Supabase desde o último pull e aplica-as ao SQLite local.
    Silenciosamente não faz nada se não houver internet/credenciais — não afeta
    o funcionamento offline da app.
    """
    from . import Database

    resumo = {"sucesso": False, "clientes": 0, "marcacoes": 0,
              "pendentes": 0, "anotacoes": False, "conflitos": []}

    client = _get_client_autenticado()
    if not client:
        return resumo

    ultimo_pull = Database.ler_meta("last_pull_timestamp") or "1970-01-01T00:00:00+00:00"
    agora_iso = datetime.utcnow().isoformat() + "+00:00"

    try:
        res = client.table("clientes").select("*").gt("updated_at", ultimo_pull).execute()
        for row in cast(List[Dict[str, Any]], res.data or []):
            _merge_cliente(row, ultimo_pull, resumo)

        res = client.table("marcacoes").select("*").gt("updated_at", ultimo_pull).execute()
        for row in cast(List[Dict[str, Any]], res.data or []):
            _merge_marcacao(row, ultimo_pull, resumo)

        res = client.table("sync_tombstones").select("*").gt("apagado_em", ultimo_pull).execute()
        for row in cast(List[Dict[str, Any]], res.data or []):
            _aplicar_tombstone(row)

        if not Database.ha_fila_pendente_para("pendentes"):
            res = client.table("pendentes").select("*").execute()
            pendentes_data = cast(List[Dict[str, Any]], res.data or [])
            Database.guardar_pendentes_sem_sync(pendentes_data)
            resumo["pendentes"] = len(pendentes_data)

        if not Database.ha_fila_pendente_para("anotacoes"):
            res = client.table("anotacoes").select("texto").eq("id", 1).execute()
            anotacoes_data = cast(List[Dict[str, Any]], res.data or [])
            if anotacoes_data:
                Database.guardar_anotacoes_sem_sync(anotacoes_data[0]["texto"])
                resumo["anotacoes"] = True

        Database.guardar_meta("last_pull_timestamp", agora_iso)
        resumo["sucesso"] = True

    except Exception as e:
        print(f"[Sync] Erro ao puxar alterações: {e}")

    return resumo


def _merge_cliente(row: dict, ultimo_pull: str, resumo: dict):
    from . import Database
    local = Database.ler_cliente_bruto(row["nome"])
    if local and local.get("updated_at"):
        dt_local  = _parse_ts(local["updated_at"])
        dt_pull   = _parse_ts(ultimo_pull)
        if dt_local and dt_pull and dt_local > dt_pull:
            dt_remoto = _parse_ts(row.get("updated_at", ""))
            remoto_ganha = bool(dt_remoto) and dt_remoto > dt_local
            resumo["conflitos"].append({
                "tabela": "clientes", "chave": row["nome"],
                "resolucao": "remoto" if remoto_ganha else "local"
            })
            if not remoto_ganha:
                return
    Database.upsert_cliente_sem_sync(row)
    resumo["clientes"] += 1


def _merge_marcacao(row: dict, ultimo_pull: str, resumo: dict):
    from . import Database
    local = Database.ler_marcacao_bruta(row["data_hora"])
    if local and local.get("updated_at"):
        dt_local  = _parse_ts(local["updated_at"])
        dt_pull   = _parse_ts(ultimo_pull)
        if dt_local and dt_pull and dt_local > dt_pull:
            dt_remoto = _parse_ts(row.get("updated_at", ""))
            remoto_ganha = bool(dt_remoto) and dt_remoto > dt_local
            resumo["conflitos"].append({
                "tabela": "marcacoes", "chave": row["data_hora"],
                "resolucao": "remoto" if remoto_ganha else "local"
            })
            if not remoto_ganha:
                return
    Database.upsert_marcacao_sem_sync(row)
    resumo["marcacoes"] += 1
    
def _aplicar_tombstone(row: dict):
    """Só apaga localmente se não houver uma versão MAIS RECENTE nessa
       mesma 'chave' - evita apagar um registo que reutilizou a mesma
       data_hora/nome depois de um delete anterior."""
    from . import Database
    
    tabela = row["tabela"]
    chave = row["chave"]
    apagado_em = _parse_ts(row.get("apagado_em", ""))
    
    if tabela == "clientes":
        local = Database.ler_cliente_bruto(chave)
        if local and local.get("updated_at"):
            dt_local = _parse_ts(local["updated_at"])
            if dt_local and apagado_em and dt_local > apagado_em:
                return
        Database.apagar_cliente_sem_sync(chave)
        
    elif tabela == "marcacoes":
        local = Database.ler_marcacao_bruta(chave)
        if local and local.get("updated_at"):
            dt_local = _parse_ts(local["updated_at"])
            if dt_local and apagado_em and dt_local > apagado_em:
                return
        Database.apagar_marcacao_sem_sync(chave)