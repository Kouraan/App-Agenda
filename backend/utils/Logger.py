"""
Logger.py — regista eventos em dois destinos em paralelo:
  1. ficheiros .txt em logs/<ano>/logs<Tipo><mes>.txt
  2. tabela `logs` na base de dados SQLite
"""

import os
from datetime import datetime

from ..models.Marcacao import Marcacao
from ..models.Pendente import Pendente
from . import Database


def _escrever_log(tipo_txt: str, tipo_bd: str, mensagem: str):
    """
    tipo_txt : sufixo do ficheiro
    tipo_bd  : valor na coluna tipo da BD
    mensagem : texto do log
    """
    now = datetime.now()
    data = now.strftime("%d/%m/%Y %H:%M:%S")
    ano = now.strftime("%Y")
    mes = now.strftime("%m")
    linha = f"[{data}] {mensagem}\n"

    # ficheiro .txt
    base_dir = os.path.dirname(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    )
    dir_path = os.path.join(base_dir, "logs", ano)
    file_path = os.path.join(dir_path, f"logs{tipo_txt}{mes}.txt")

    try:
        os.makedirs(dir_path, exist_ok=True)

        conteudo_existente = ""
        if os.path.exists(file_path):
            with open(file_path, "r", encoding="utf-8") as f:
                conteudo_existente = f.read()

        with open(file_path, "w", encoding="utf-8") as f:
            f.write(linha + conteudo_existente)
    except OSError as e:
        print(f"[Logger] Erro ao escrever .txt: {e}")

    # BD
    try:
        Database.inserir_log(tipo_bd, mensagem)
    except Exception as e:
        print(f"[Logger] Erro ao inserir log na BD: {e}")


# Logs de Utilizador

def log_utilizador(mensagem: str):
    _escrever_log("Utilizador", "utilizador", mensagem)

def log_registo(nome: str):
    log_utilizador(f"Novo utilizador registado com o nome: '{nome}'.")

def log_login(nome: str):
    log_utilizador(f"Utilizador '{nome}' efetuou login na sua conta.")

def log_logout(nome: str):
    log_utilizador(f"Utilizador '{nome}' deu logout da sua conta.")

def log_app_iniciada():
    log_utilizador("Aplicação iniciada.")

def log_app_terminada():
    log_utilizador("Aplicação terminada.")


# Logs de Cliente

def log_cliente(mensagem: str):
    _escrever_log("Clientes", "cliente", mensagem)

def log_cliente_criado(nome: str):
    log_cliente(f"Cliente '{nome}' criado.")

def log_cliente_apagado(nome: str):
    log_cliente(f"Cliente '{nome}' apagado.")

def log_falta_adicionada(nome: str):
    log_cliente(f"Falta adicionada ao Cliente '{nome}'.")

def log_falta_retirada(nome: str):
    log_cliente(f"Falta retirada ao Cliente '{nome}'.")

def log_nome_alterado(nome_antigo: str, nome_novo: str):
    log_cliente(f"Nome do Cliente '{nome_antigo}' alterado para '{nome_novo}'.")

def log_numero_alterado(nome: str, numero_novo: str):
    log_cliente(f"Numero do Cliente '{nome}' alterado para '{numero_novo}'.")

def log_tipo_semanal_para_normal(nome: str):
    log_cliente(f"Cliente '{nome}' alterado de SEMANAL para NORMAL.")

def log_tipo_normal_para_semanal(nome: str, dia: str, hora: str):
    log_cliente(
        f"Cliente '{nome}' alterado de NORMAL para SEMANAL, "
        f"horário de '{dia}' às '{hora}'."
    )

def log_horario_semanal_alterado(nome: str, dia: str, hora: str):
    log_cliente(
        f"Horas do Cliente SEMANAL '{nome}' alteradas para '{dia}' às '{hora}'."
    )


# Logs de Marcação

def log_marcacao(mensagem: str):
    _escrever_log("Marcacoes", "marcacao", mensagem)

def log_marcacao_criada(marcacao: Marcacao):
    cliente = marcacao.get_cliente()
    nome = cliente.get_nome() if cliente is not None else "N/A"
    data_hora = marcacao.get_data_hora()
    data_hora_str = data_hora.strftime("%d/%m/%Y %H:%M") if data_hora is not None else "N/A"
    log_marcacao(f"Marcação criada para '{nome}' em {data_hora_str}.")

def log_marcacao_apagada(marcacao: Marcacao):
    cliente = marcacao.get_cliente()
    nome = cliente.get_nome() if cliente is not None else "N/A"
    data_hora = marcacao.get_data_hora()
    data_hora_str = data_hora.strftime("%d/%m/%Y %H:%M") if data_hora is not None else "N/A"
    log_marcacao(f"Marcação de '{nome}' em {data_hora_str} apagada.")

def log_marcacao_obs_alterada(marcacao: Marcacao, obs_nova: str):
    cliente = marcacao.get_cliente()
    nome = cliente.get_nome() if cliente is not None else "N/A"
    data_hora = marcacao.get_data_hora()
    data_hora_str = data_hora.strftime("%d/%m/%Y %H:%M") if data_hora is not None else "N/A"
    log_marcacao(
        f"Observações da marcação de '{nome}' em {data_hora_str} "
        f"alteradas para '{obs_nova}'."
    )

def log_marcacao_data_hora_alterada(marcacao: Marcacao,
                                    data_hora_antiga: str,
                                    data_hora_nova: str):
    cliente = marcacao.get_cliente()
    nome = cliente.get_nome() if cliente is not None else "N/A"
    log_marcacao(
        f"Data/hora da marcação de '{nome}' alterada de "
        f"{data_hora_antiga} para {data_hora_nova}."
    )

def log_marcacao_falta(marcacao: Marcacao):
    cliente = marcacao.get_cliente()
    nome = cliente.get_nome() if cliente is not None else "N/A"
    data_hora = marcacao.get_data_hora()
    data_hora_str = data_hora.strftime("%d/%m/%Y %H:%M") if data_hora is not None else "N/A"
    log_marcacao(f"O cliente '{nome}' faltou a marcacao na data '{data_hora_str}'.")


# Logs de Pendentes

def log_pendente(mensagem: str):
    _escrever_log("Pendentes", "pendente", mensagem)

def log_pendente_adicionado(pendente: Pendente):
    log_pendente(f"Cliente Pendente adicionado: '{pendente.get_nome()}'.")

def log_pendente_removido(pendente: Pendente):
    log_pendente(f"Cliente Pendente removido: '{pendente.get_nome()}'.")