"""
migrar_para_supabase.py
Corre UMA VEZ para enviar tudo o que está no SQLite para o Supabase.
"""

import sqlite3
import os
from dotenv import load_dotenv
from supabase import create_client

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")
DB_PATH      = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "agenda.db")

def conectar_sqlite():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def migrar():
    print("=" * 50)
    print("MIGRAÇÃO SQLite -> Supabase")
    print("=" * 50)
    
    if not SUPABASE_URL or not SUPABASE_KEY:
        print("ERRO: .env não encontrado ou incompleto.")
        return
    
    if not os.path.exists(DB_PATH):
        print(f"ERRO: Base de dados não encontrada em {DB_PATH}")
        return
    
    supabase = create_client(SUPABASE_URL, SUPABASE_KEY)
    conn     = conectar_sqlite()
    
    # Utilizador
    print("\n[1/6] A migrar utilizador...")
    try:
        rows = conn.execute("SELECT nome, password FROM utilizador").fetchall()
        if rows:
            dados = [{"nome": r["nome"], "password": r["password"]} for r in rows]
            supabase.table("utilizador").upsert(dados).execute()
            print(f"    ✓ {len(dados)} utilizador(es) migrado(s)")
        else:
            print("    ✗ Nenhum utilizador encontrado.")
    except Exception as e:
        print(f"    ✗ Erro: {e}")
        
    # Clientes
    print("\n[2/6] A migrar clientes...")
    try:
        rows = conn.execute("SELECT * FROM clientes").fetchall()
        if rows:
            dados = [dict(r) for r in rows]
            for d in dados:
                d.pop("id", None)
            total = 0
            for i in range(0, len(dados), 100):
                bloco = dados[i:i+100]
                supabase.table("clientes").upsert(bloco, on_conflict="nome").execute()
                total += len(bloco)
                print(f"    ... {total}/{len(dados)}")
            print(f"    ✓ {len(dados)} cliente(s) migrado(s)")
        else:
            print("    ✗ Nenhum cliente encontrado.")
    except Exception as e:
        print(f"    ✗ Erro: {e}")
        
    # Marcações
    print("\n[3/6] A migrar marcações...")
    try:
        rows = conn.execute("SELECT * FROM marcacoes").fetchall()
        if rows:
            dados = [dict(r) for r in rows]
            for d in dados:
                d.pop("id", None)
            total = 0
            for i in range(0, len(dados), 100):
                bloco = dados[i:i+100]
                supabase.table("marcacoes").upsert(bloco, on_conflict="data_hora").execute()
                total += len(bloco)
                print(f"    ... {total}/{len(dados)}")
            print(f"    ✓ {len(dados)} marcação(ões) migrada(s)")
        else:
            print("    ✗ Nenhuma marcação encontrada.")
    except Exception as e:
        print(f"    ✗ Erro: {e}")
        
    # Anotações
    print("\n[4/6] A migrar anotações...")
    try:
        row = conn.execute("SELECT texto FROM anotacoes WHERE id=1").fetchone()
        if row:
            supabase.table("anotacoes").upsert({"id": 1, "texto": row["texto"]}).execute()
            print("      ✓ Anotações migradas")
        else:
            print("      ℹ Nenhuma anotação encontrada")
    except Exception as e:
        print(f"      ✗ Erro: {e}")

    # Pendentes
    print("\n[5/6] A migrar pendentes...")
    try:
        rows = conn.execute("SELECT nome, numero_telefone FROM pendentes").fetchall()
        if rows:
            dados = [dict(r) for r in rows]
            supabase.table("pendentes").upsert(dados).execute()
            print(f"    ✓ {len(dados)} pendente(s) migrado(s)")
        else:
            print("    ✗ Nenhum pendente encontrado.")
    except Exception as e:
        print(f"    ✗ Erro: {e}")
        
    # Logs
    print("\n[6/6] A migrar logs...")
    try:
        rows = conn.execute("SELECT data_hora, tipo, mensagem FROM logs").fetchall()
        if rows:
            dados = [dict(r) for r in rows]
            total = 0
            for i in range(0, len(dados), 100):
                bloco = dados[i:i+100]
                supabase.table("logs").upsert(bloco).execute()
                total += len(bloco)
            print(f"    ✓ {len(dados)} log(s) migrado(s)")
        else:
            print("    ✗ Nenhum log encontrado.")
    except Exception as e:
        print(f"    ✗ Erro: {e}")


    conn.close()
    
    print("\n" + "=" * 50)
    print("MIGRAÇÃO CONCLUIDA")
    print("=" * 50)
    print("\nPodes verificar os dados no Supabase em:")
    print("Table Editor -> cada tabela")

if __name__ == "__main__":
    migrar()