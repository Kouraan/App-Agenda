from datetime import datetime
from ..models.Marcacao import Marcacao
from ..models.Cliente import Cliente
from ..utils.Validation import Validation

class MarcacoesRepository:
    def __init__(self, marcacoes=None, outro=None):
        """
        Construtor da classe MarcacoesRepository
        Pode ser usado como:
        - MarcacoesRepository() - construtor padrão
        - MarcacoesRepository(marcacoes=dict_marcacoes) - construtor com dicionário
        - MarcacoesRepository(outro=outro_repo) - construtor de cópia
        """
        if outro is not None:
            # Construtor de cópia
            self.marcacoes = outro.marcacoes.copy()
        elif marcacoes is not None:
            # Construtor com dicionário
            self.marcacoes = marcacoes.copy()
        else:
            # Construtor padrão
            self.marcacoes = {}

    # Getters e Setters
    def get_marcacoes(self):
        return self.marcacoes.copy()

    def set_marcacoes(self, marcacoes):
        self.marcacoes = marcacoes.copy()

    # Métodos
    def add_marcacao(self, marcacao):
        if marcacao is not None and marcacao.get_data_hora() is not None:
            # Validação com módulo Validation
            if not Validation.marcacao_duplicada(self.marcacoes, marcacao.get_data_hora()):
                self.marcacoes[marcacao.get_data_hora()] = marcacao.clone()

    def remove_marcacao(self, data_hora):
        if data_hora is not None and data_hora in self.marcacoes:
            del self.marcacoes[data_hora]

    def alterar_marcacao(self, data_hora_marcacao, data_hora=None, cliente=None, 
                        duracao=None, observacoes=None):
        if data_hora_marcacao is not None and data_hora_marcacao in self.marcacoes:
            marcacao = self.marcacoes[data_hora_marcacao]

            # Verificar se nova data_hora já existe
            if (data_hora is not None and 
                data_hora != data_hora_marcacao and 
                data_hora in self.marcacoes):
                return

            if data_hora is not None:
                marcacao.set_data_hora(data_hora)
            if cliente is not None:
                marcacao.set_cliente(cliente.clone())
            if duracao is not None and duracao > 0:
                marcacao.set_duracao(duracao)
            if observacoes is not None:
                marcacao.set_observacoes(observacoes)

            # Atualizar no dicionário
            if (data_hora is not None and 
                data_hora_marcacao != marcacao.get_data_hora()):
                del self.marcacoes[data_hora_marcacao]
                self.marcacoes[data_hora] = marcacao.clone()
            else:
                self.marcacoes[data_hora_marcacao] = marcacao.clone()

    def clone(self):
        return MarcacoesRepository(outro=self)