# Script simples de build (usa PyInstaller se estiver instalado).
# Ajuste conforme necessário para incluir assets (ui, data, etc).

import os
import sys
import subprocess

def main():
    print("Build helper: cria bundle com PyInstaller (se instalado).")
    entry = "main.py"
    if not os.path.exists(entry):
        print("main.py não encontrado no diretório atual.")
        return
    args = [
        "pyinstaller",
        "--noconfirm",
        "--clean",
        "--windowed",
        "--add-data", "ui{}ui".format(os.pathsep),  # inclui pasta ui
        "--add-data", "backend{}backend".format(os.pathsep),
        "--name", "AppAgenda",
        entry
    ]
    try:
        subprocess.check_call(args)
        print("PyInstaller executado. Verifique a pasta dist/AppAgenda.")
    except FileNotFoundError:
        print("PyInstaller não encontrado. Instale com: pip install pyinstaller")
    except subprocess.CalledProcessError as e:
        print("Erro ao executar PyInstaller:", e)

if __name__ == "__main__":
    main()