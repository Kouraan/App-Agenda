from datetime import time
from ..models.Cliente import Cliente, TipoCliente
from ..utils.Validation import Validation

class ClientesRepository:
    def __init__(self, clientes=None, outro=None):
        """
        Construtor da classe ClientesRepository
        Pode ser usado como:
        - ClientesRepository() - construtor padrão
        - ClientesRepository(clientes=dict_clientes) - construtor com dicionário
        - ClientesRepository(outro=outro_repo) - construtor de cópia
        """
        if outro is not None:
            # Construtor de cópia
            self.clientes = outro.clientes.copy()
        elif clientes is not None:
            # Construtor com dicionário
            self.clientes = clientes.copy()
        else:
            # Construtor padrão
            self.clientes = {}

    # Getters e Setters
    def get_clientes(self):
        return self.clientes.copy()

    def set_clientes(self, clientes):
        self.clientes = clientes.copy()

    # Métodos
    def add_cliente(self, cliente):
        if cliente is not None and cliente.get_nome() is not None:
            # Validação com módulo Validation
            if not Validation.cliente_duplicado(self.clientes, cliente.get_nome(), cliente.get_numero_telefone()):
                self.clientes[cliente.get_nome()] = cliente.clone()

    def remove_cliente(self, nome):
        if nome is not None and nome in self.clientes:
            del self.clientes[nome]

    def add_faltas(self, nome):
        if nome is not None and nome in self.clientes:
            cliente = self.clientes[nome]
            cliente.set_faltas(cliente.get_faltas() + 1)
            self.clientes[nome] = cliente.clone()

    def remove_faltas(self, nome):
        if nome is not None and nome in self.clientes:
            cliente = self.clientes[nome]
            if cliente.get_faltas() > 0:
                cliente.set_faltas(cliente.get_faltas() - 1)
                self.clientes[nome] = cliente.clone()

    def alterar_cliente(self, nome_cliente, nome=None, numero_telefone=None, 
                       tipo=None, dia_semana=None, hora_corte=None):
        if nome_cliente is not None and nome_cliente in self.clientes:
            cliente = self.clientes[nome_cliente]

            if nome is not None:
                cliente.set_nome(nome)
            if numero_telefone is not None:
                cliente.set_numero_telefone(numero_telefone)
            
            if tipo is not None and tipo != cliente.get_tipo_cliente():
                if tipo == TipoCliente.SEMANAL:
                    if dia_semana is not None and hora_corte is not None:
                        try:
                            # Validar com módulo Validation
                            if (Validation.hora_valida(hora_corte=hora_corte) and
                                Validation.pode_cliente_semanal(self.clientes, dia_semana, hora_corte)):
                                cliente.set_tipo_cliente(tipo)
                                cliente.set_dia_semana(dia_semana)
                                cliente.set_hora_corte(hora_corte)
                            else:
                                return
                        except Exception as e:
                            return
                    else:
                        return
                else:
                    cliente.set_tipo_cliente(tipo)
                    cliente.set_dia_semana(None)
                    cliente.set_hora_corte(None)
            elif tipo == TipoCliente.SEMANAL:
                if dia_semana is not None:
                    cliente.set_dia_semana(dia_semana)
                if hora_corte is not None:
                    try:
                        dia_atual = cliente.get_dia_semana() if cliente.get_dia_semana() else dia_semana
                        if (dia_atual and 
                            Validation.hora_valida(hora_corte=hora_corte) and
                            Validation.pode_cliente_semanal(self.clientes, dia_atual, hora_corte)):
                            cliente.set_hora_corte(hora_corte)
                    except Exception as e:
                        print(f"Erro ao converter hora: {e}")

            # Atualizar no dicionário
            if nome_cliente != cliente.get_nome():
                del self.clientes[nome_cliente]
                self.clientes[cliente.get_nome()] = cliente.clone()
            else:
                self.clientes[nome_cliente] = cliente.clone()

    def clone(self):
        return ClientesRepository(outro=self)