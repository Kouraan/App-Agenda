class Pendente:
    def __init__(self, nome="", numero_telefone="", outro=None):
        """
        Construtor da classe Pendente
        Pode ser usado como:
        - Pendente() - construtor padrão
        - Pendente(nome, numero_telefone) - construtor com parâmetros
        - Pendente(outro=outro_pendente) - construtor de cópia
        """
        if outro is not None:
            # Construtor de cópia
            self.nome = outro.nome
            self.numero_telefone = outro.numero_telefone
        else:
            # Construtor padrão ou com parâmetros
            self.nome = nome
            self.numero_telefone = numero_telefone

    # Getters e Setters
    def get_nome(self):
        return self.nome

    def set_nome(self, nome):
        self.nome = nome

    def get_numero_telefone(self):
        return self.numero_telefone

    def set_numero_telefone(self, numero_telefone):
        self.numero_telefone = numero_telefone

    def clone(self):
        return Pendente(outro=self)
