# Script de build (usa PyInstaller).
# Ajuste conforme necessário para incluir assets (ui, data, etc).

import os
import sys
import subprocess
import shutil

def main():
    print("=" * 50)
    print("Build — App Agenda")
    print("=" * 50)

    entry = "main.py"
    if not os.path.exists(entry):
        print("ERRO: main.py não encontrado.")
        return

    # Verifica se o .env existe
    if not os.path.exists(".env"):
        print("AVISO: .env não encontrado -- o sync com Supabase não vai funcionar!")
        if os.environ.get("CI", "").lower() == "true":
            print("A correr em CI -- a continuar automaticamente.")
        else:
            resposta = input("Continuar mesmo assim? (s/n): ")
            if resposta.lower() != "s":
                return

    separador = ";" if sys.platform == "win32" else ":"

    args = [
        "pyinstaller",
        "--noconfirm",
        "--clean",
        "--windowed",
        "--name", "AppAgenda",
        "--add-data", f"ui{separador}ui",
        "--add-data", f"backend{separador}backend",
        "--add-data", f".env{separador}.",
        entry
    ]

    try:
        subprocess.check_call(args)
        print("\n✓ PyInstaller concluído.")

        # Criar pasta data junto ao executável se não existir
        dist_path = os.path.join("dist", "AppAgenda")
        data_path = os.path.join(dist_path, "data")
        os.makedirs(data_path, exist_ok=True)
        print(f"✓ Pasta data criada em: {data_path}")

        # Copiar .env para junto do executável (redundância)
        if os.path.exists(".env"):
            shutil.copy(".env", os.path.join(dist_path, ".env"))
            print(f"✓ .env copiado para: {dist_path}")
            
        if os.path.exists("version.txt"):
            shutil.copy("version.txt", os.path.join(dist_path, "version.txt"))
            print(f"✓ version.txt copiado para: {dist_path}")

        print("\n" + "=" * 50)
        print("BUILD CONCLUÍDO")
        print(f"Executável em: dist/AppAgenda/")
        print("=" * 50)

    except FileNotFoundError:
        print("ERRO: PyInstaller não encontrado.")
        print("Instala com: pip install pyinstaller")
    except subprocess.CalledProcessError as e:
        print(f"ERRO ao executar PyInstaller: {e}")

if __name__ == "__main__":
    main()