from datetime import datetime
from .Cliente import Cliente

class Marcacao:
    def __init__(self, data_hora=None, cliente=None, duracao=0, observacoes="", outro=None):
        """
        Construtor da classe Marcacao
        Pode ser usado como:
        - Marcacao() - construtor padrão
        - Marcacao(data_hora, cliente, duracao, observacoes) - construtor com parâmetros
        - Marcacao(outro=outra_marcacao) - construtor de cópia
        """
        if outro is not None:
            # Construtor de cópia
            self.data_hora = outro.data_hora
            self.cliente = outro.cliente.clone() if outro.cliente else None
            self.duracao = outro.duracao
            self.observacoes = outro.observacoes
            self.falta = outro.falta
        else:
            # Construtor padrão ou com parâmetros
            self.data_hora = data_hora if data_hora else datetime.now()
            self.cliente = cliente.clone() if cliente else Cliente()
            self.duracao = duracao
            self.observacoes = observacoes
            self.falta = False

    # Getters e Setters
    def get_data_hora(self):
        return self.data_hora

    def set_data_hora(self, data_hora):
        self.data_hora = data_hora

    def get_cliente(self):
        return self.cliente.clone()

    def set_cliente(self, cliente):
        self.cliente = cliente.clone()

    def get_duracao(self):
        return self.duracao

    def set_duracao(self, duracao):
        self.duracao = duracao

    def get_observacoes(self):
        return self.observacoes

    def set_observacoes(self, observacoes):
        self.observacoes = observacoes

    def is_falta(self):
        return self.falta

    def set_falta(self, falta):
        self.falta = falta

    def clone(self):
        return Marcacao(outro=self)