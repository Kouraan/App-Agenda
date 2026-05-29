from enum import Enum

class TipoCliente(Enum):
    NORMAL = "NORMAL"
    DESCONHECIDO = "DESCONHECIDO"
    SEMANAL = "SEMANAL"
    
class Cliente:
    def __init__(self, nome="", numero_telefone="", tipo_cliente=TipoCliente.DESCONHECIDO,
                 dia_semana=None, hora_corte=None, rapido=False, outro=None):
        """
        Construtor da classe Cliente
        Pode ser usado como:
        - Cliente() - construtor padrão
        - Cliente(nome, numero_telefone, tipo_cliente) - para NORMAL e DESCONHECIDO
        - Cliente(nome, numero_telefone, TipoCliente.SEMANAL, dia_semana, hora_corte, rapido) - para SEMANAL
        - Cliente(outro=outro_cliente) - construtor de cópia
        """
        if outro is not None:
            # Construtor de cópia
            self.nome = outro.nome
            self.numero_telefone = outro.numero_telefone
            self.tipo_cliente = outro.tipo_cliente
            self.faltas = outro.faltas
            self.dia_semana = outro.dia_semana
            self.hora_corte = outro.hora_corte
            self.rapido = outro.rapido
        else:
            # Construtor padrão ou com parâmetros
            self.nome = nome
            self.numero_telefone = numero_telefone
            self.tipo_cliente = tipo_cliente
            self.faltas = 0
            self.dia_semana = dia_semana
            self.hora_corte = hora_corte
            self.rapido = rapido
            
    # Getters e Setters
    def get_nome(self):
        return self.nome
    
    def set_nome(self, nome):
        self.nome = nome
        
    def get_numero_telefone(self):
        return self.numero_telefone
    
    def set_numero_telefone(self, numero_telefone):
        self.numero_telefone = numero_telefone
        
    def get_tipo_cliente(self):
        return self.tipo_cliente
    
    def set_tipo_cliente(self, tipo_cliente):
        self.tipo_cliente = tipo_cliente
        
    def get_faltas(self):
        return self.faltas
    
    def set_faltas(self, faltas):
        self.faltas = faltas
        
    def get_dia_semana(self):
        return self.dia_semana
    
    def set_dia_semana(self, dia_semana):
        self.dia_semana = dia_semana
        
    def get_hora_corte(self):
        return self.hora_corte
    
    def set_hora_corte(self, hora_corte):
        self.hora_corte = hora_corte
        
    def is_rapido(self):
        return self.rapido
    
    def set_rapido(self, rapido):
        self.rapido = rapido
        
    def is_temporario(self):
        return self.tipo_cliente == TipoCliente.DESCONHECIDO
    
    def clone(self):
        return Cliente(outro=self)
    
    