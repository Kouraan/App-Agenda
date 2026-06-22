from datetime import datetime
from .Cliente import Cliente
from .Utilizador import Utilizador
from .Marcacao import Marcacao
from .Pendente import Pendente

class Log:
    def __init__(self, data_hora=None, cliente=None, utilizador=None, marcacao=None, 
                 pendente=None, detalhes="", outro=None):
        """
        Construtor da classe Log
        Pode ser usado como:
        - Log() - construtor padrão
        - Log(data_hora, utilizador=utilizador, detalhes=detalhes) - apenas para utilizador
        - Log(data_hora, cliente=cliente, detalhes=detalhes) - apenas para cliente
        - Log(data_hora, marcacao=marcacao, detalhes=detalhes) - apenas para marcacao
        - Log(data_hora, pendente=pendente, detalhes=detalhes) - apenas para pendente
        - Log(outro=outro_log) - construtor de cópia
        """
        if outro is not None:
            # Construtor de cópia
            self.data_hora = outro.data_hora
            self.cliente = outro.cliente
            self.utilizador = outro.utilizador
            self.marcacao = outro.marcacao
            self.pendente = outro.pendente
            self.detalhes = outro.detalhes
        else:
            # Construtor padrão ou com parâmetros
            self.data_hora = data_hora if data_hora else datetime.now()
            self.cliente = cliente
            self.utilizador = utilizador
            self.marcacao = marcacao
            self.pendente = pendente
            self.detalhes = detalhes
            
            # Se marcacao foi fornecida, extrair cliente
            if marcacao is not None:
                self.cliente = marcacao.get_cliente()

    # Getters e Setters
    def get_data_hora(self):
        return self.data_hora

    def set_data_hora(self, data_hora):
        self.data_hora = data_hora

    def get_cliente(self):
        return self.cliente

    def set_cliente(self, cliente):
        self.cliente = cliente

    def get_utilizador(self):
        return self.utilizador

    def set_utilizador(self, utilizador):
        self.utilizador = utilizador

    def get_marcacao(self):
        return self.marcacao

    def set_marcacao(self, marcacao):
        self.marcacao = marcacao

    def get_pendente(self):
        return self.pendente

    def set_pendente(self, pendente):
        self.pendente = pendente

    def get_detalhes(self):
        return self.detalhes

    def set_detalhes(self, detalhes):
        self.detalhes = detalhes

    def clone(self):
        return Log(outro=self)