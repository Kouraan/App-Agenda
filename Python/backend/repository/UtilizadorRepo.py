from ..models.Utilizador import Utilizador
from ..utils.Validation import Validation

class UtilizadorRepository:
    @staticmethod
    def autenticar(nome, password, utilizador):
        """Autentica um utilizador"""
        return (utilizador is not None and 
                utilizador.get_nome() == nome and 
                utilizador.get_password() == password)

    @staticmethod
    def alterar_nome(utilizador, nome):
        """Altera o nome do utilizador"""
        if (utilizador is not None and 
            nome is not None and 
            Validation.nome_valido(nome)):
            utilizador.set_nome(nome)

    @staticmethod
    def alterar_password(utilizador, password):
        """Altera a password do utilizador"""
        if (utilizador is not None and 
            password is not None and 
            Validation.password_valida(password)):
            utilizador.set_password(password)