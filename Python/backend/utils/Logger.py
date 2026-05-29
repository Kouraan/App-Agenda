import os
from datetime import datetime
from ..models.Utilizador import Utilizador
from ..models.Cliente import Cliente
from ..models.Marcacao import Marcacao
from ..models.Pendente import Pendente

class Logger:
    @staticmethod
    def _data_atual() -> str:
        """Retorna data/hora atual formatada"""
        return datetime.now().strftime("%d/%m/%Y %H:%M:%S")

    @staticmethod
    def _escrever_log(tipo_log: str, mensagem: str):
        """Método genérico para escrever logs"""
        now = datetime.now()
        data = Logger._data_atual()
        ano = now.strftime("%Y")
        mes = now.strftime("%m")
        linha = f"[{data}] {mensagem}\n"
        dir_path = f"logs/{ano}"
        file_path = f"{dir_path}/logs{tipo_log}{mes}.txt"

        try:
            # Garantir que o diretório existe
            os.makedirs(dir_path, exist_ok=True)

            # Ler ficheiro se existir
            conteudo_existente = ""
            if os.path.exists(file_path):
                with open(file_path, "r", encoding="utf-8") as file:
                    conteudo_existente = file.read()

            # Escrever log (nova linha primeiro, depois conteúdo existente)
            with open(file_path, "w", encoding="utf-8") as file:
                file.write(linha + conteudo_existente)

        except OSError as e:
            print(f"Erro ao escrever no ficheiro de log: {e}")

    # LOGS DE UTILIZADOR
    @staticmethod
    def log_utilizador(mensagem: str):
        """Log genérico de utilizador"""
        Logger._escrever_log("Utilizador", mensagem)

    @staticmethod
    def log_registo(nome: str):
        """Log de novo utilizador registado"""
        Logger.log_utilizador(f"Novo utilizador registado com o nome: '{nome}'.")

    @staticmethod
    def log_login(nome: str):
        """Log de login"""
        Logger.log_utilizador(f"Utilizador '{nome}' efetuou login na sua conta.")

    @staticmethod
    def log_logout(nome: str):
        """Log de logout"""
        Logger.log_utilizador(f"Utilizador '{nome}' deu logout da sua conta.")

    @staticmethod
    def log_app_iniciada():
        """Log de aplicação iniciada"""
        Logger.log_utilizador("Aplicação iniciada.")

    @staticmethod
    def log_app_terminada():
        """Log de aplicação terminada"""
        Logger.log_utilizador("Aplicação terminada.")

    # LOGS DE CLIENTE
    @staticmethod
    def log_cliente(mensagem: str):
        """Log genérico de cliente"""
        Logger._escrever_log("Clientes", mensagem)

    @staticmethod
    def log_cliente_criado(nome: str):
        """Log de cliente criado"""
        Logger.log_cliente(f"Cliente '{nome}' criado.")

    @staticmethod
    def log_cliente_apagado(nome: str):
        """Log de cliente apagado"""
        Logger.log_cliente(f"Cliente '{nome}' apagado.")

    @staticmethod
    def log_falta_adicionada(nome: str):
        """Log de falta adicionada"""
        Logger.log_cliente(f"Falta adicionada ao Cliente '{nome}'.")

    @staticmethod
    def log_falta_retirada(nome: str):
        """Log de falta retirada"""
        Logger.log_cliente(f"Falta retirada ao Cliente '{nome}'.")

    @staticmethod
    def log_nome_alterado(nome_antigo: str, nome_novo: str):
        """Log de nome alterado"""
        Logger.log_cliente(f"Nome do Cliente '{nome_antigo}' alterado para '{nome_novo}'.")

    @staticmethod
    def log_numero_alterado(nome: str, numero_novo: str):
        """Log de número alterado"""
        Logger.log_cliente(f"Numero do Cliente '{nome}' alterado para '{numero_novo}'.")

    @staticmethod
    def log_tipo_semanal_para_normal(nome: str):
        """Log de tipo alterado de SEMANAL para NORMAL"""
        Logger.log_cliente(f"Cliente '{nome}' alterado de SEMANAL para NORMAL.")

    @staticmethod
    def log_tipo_normal_para_semanal(nome: str, dia: str, hora: str):
        """Log de tipo alterado de NORMAL para SEMANAL"""
        Logger.log_cliente(f"Cliente '{nome}' alterado de NORMAL para SEMANAL, horário de '{dia}' às '{hora}'.")

    @staticmethod
    def log_horario_semanal_alterado(nome: str, dia: str, hora: str):
        """Log de horário semanal alterado"""
        Logger.log_cliente(f"Horas do Cliente SEMANAL '{nome}' alteradas para '{dia}' às '{hora}'.")

    # LOGS DE MARCAÇÃO
    @staticmethod
    def log_marcacao(mensagem: str):
        """Log genérico de marcação"""
        Logger._escrever_log("Marcacoes", mensagem)

    @staticmethod
    def log_marcacao_criada(marcacao: Marcacao):
        """Log de marcação criada"""
        nome = marcacao.get_cliente().get_nome() if marcacao.get_cliente() else "N/A"
        data_hora = marcacao.get_data_hora().strftime("%d/%m/%Y %H:%M")
        Logger.log_marcacao(f"Marcação criada para '{nome}' em {data_hora}.")

    @staticmethod
    def log_marcacao_apagada(marcacao: Marcacao):
        """Log de marcação apagada"""
        nome = marcacao.get_cliente().get_nome() if marcacao.get_cliente() else "N/A"
        data_hora = marcacao.get_data_hora().strftime("%d/%m/%Y %H:%M")
        Logger.log_marcacao(f"Marcação de '{nome}' em {data_hora} apagada.")

    @staticmethod
    def log_marcacao_obs_alterada(marcacao: Marcacao, obs_nova: str):
        """Log de observações alteradas"""
        nome = marcacao.get_cliente().get_nome() if marcacao.get_cliente() else "N/A"
        data_hora = marcacao.get_data_hora().strftime("%d/%m/%Y %H:%M")
        Logger.log_marcacao(f"Observações da marcação de '{nome}' em {data_hora} alteradas para '{obs_nova}'.")

    @staticmethod
    def log_marcacao_data_hora_alterada(marcacao: Marcacao, data_hora_antiga: str, data_hora_nova: str):
        """Log de data/hora alterada"""
        nome = marcacao.get_cliente().get_nome() if marcacao.get_cliente() else "N/A"
        Logger.log_marcacao(f"Data/hora da marcação de '{nome}' alterada de {data_hora_antiga} para {data_hora_nova}.")

    @staticmethod
    def log_marcacao_falta(marcacao: Marcacao):
        """Log de falta à marcação"""
        nome = marcacao.get_cliente().get_nome() if marcacao.get_cliente() else "N/A"
        data_hora = marcacao.get_data_hora().strftime("%d/%m/%Y %H:%M")
        Logger.log_marcacao(f"O cliente '{nome}' faltou a marcacao na data '{data_hora}'.")

    # LOGS DE PENDENTE
    @staticmethod
    def log_pendente(mensagem: str):
        """Log genérico de pendente"""
        Logger._escrever_log("Pendentes", mensagem)

    @staticmethod
    def log_pendente_adicionado(pendente: Pendente):
        """Log de pendente adicionado"""
        Logger.log_pendente(f"Cliente Pendente adicionado: '{pendente.get_nome()}'.")

    @staticmethod
    def log_pendente_removido(pendente: Pendente):
        """Log de pendente removido"""
        Logger.log_pendente(f"Cliente Pendente removido: '{pendente.get_nome()}'.")