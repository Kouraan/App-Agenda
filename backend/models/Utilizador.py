class Utilizador:
    def __init__(self, nome="", password="", outro=None):
        """
        Construtor da classe Utilizador
        Pode ser usado como:
        - Utilizador() - construtor padrão
        - Utilizador(nome, password) - construtor com parâmetros
        - Utilizador(outro=outro_utilizador) - construtor de cópia
        """
        if outro is not None:
            # Contrutor de cópia
            self.nome = outro.nome
            self.password = outro.password
        else:
            # Construtor padrão ou com parâmetros
            self.nome = nome
            self.password = password
            
    # Getters e Setters
    def get_nome(self):
        return self.nome
    
    def set_nome(self, nome):
        self.nome = nome
        
    def get_password(self):
        return self.password
    
    def set_password(self, password):
        self.password = password
        
    def clone(self):
        return Utilizador(outro=self)
    