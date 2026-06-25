import re
from datetime import datetime, time, date
from ..models.Cliente import Cliente, TipoCliente
from ..models.Utilizador import Utilizador
from ..models.Marcacao import Marcacao

class Validation:
    @staticmethod
    def pode_cliente_semanal(clientes_dict, dia_semana, hora_corte):
        """Verifica se pode criar cliente semanal nesse dia/hora"""
        for cliente in clientes_dict.values():
            if cliente.get_tipo_cliente() == TipoCliente.SEMANAL:
                if (dia_semana.upper() == cliente.get_dia_semana().upper() and 
                    hora_corte == cliente.get_hora_corte()):
                    return False
        return True

    @staticmethod
    def hora_valida(hora_corte=None, data_hora=None):
        """Valida hora de corte ou data/hora de marcação"""
        inicio_manha = time(7, 0)
        fim_manha = time(12, 30)
        inicio_tarde = time(14, 0)
        fim_tarde = time(21, 0)

        # Caso para hora_corte (string no formato "HH:MM")
        if hora_corte is not None and data_hora is None:
            try:
                hora_obj = time.fromisoformat(hora_corte)
                quarto_hora = hora_obj.minute % 15 == 0
                manha = inicio_manha <= hora_obj <= fim_manha
                tarde = inicio_tarde <= hora_obj <= fim_tarde
                return quarto_hora and (manha or tarde)
            except ValueError:
                return False

        # Caso para data_hora (datetime)
        if data_hora is not None and hora_corte is None:
            hora = data_hora.time()
            dia_semana = data_hora.weekday() + 1  # Python: 0=segunda, Java: 1=segunda
            
            dia_ok = 1 <= dia_semana <= 6  # Segunda a sábado
            quarto_hora = hora.minute % 15 == 0
            manha = inicio_manha <= hora <= fim_manha
            tarde = inicio_tarde <= hora <= fim_tarde
            
            # Verificar se é hora futura (se for hoje)
            hora_futura = True
            if data_hora.date() == date.today():
                hora_futura = hora > datetime.now().time()
            
            return dia_ok and quarto_hora and (manha or tarde) and hora_futura

        return False

    @staticmethod
    def nome_valido(nome):
        """Valida nome"""
        return nome is not None and len(nome.strip()) >= 2

    @staticmethod
    def password_valida(password):
        """Valida password"""
        return password is not None and len(password.strip()) >= 6

    @staticmethod
    def cliente_duplicado(clientes_dict, nome, numero_telefone):
        """Verifica se cliente já existe (por nome ignorando capitalização, ou telefone)"""
        return any(
            c.get_nome().lower() == nome.lower() or
            c.get_numero_telefone() == numero_telefone
            for c in clientes_dict.values()
        )

    @staticmethod
    def marcacao_duplicada(marcacoes_dict, data_hora):
        """Verifica se marcação já existe nessa data/hora"""
        return data_hora in marcacoes_dict

    @staticmethod
    def numero_telefone_valido(numero):
        """Valida número de telefone"""
        if numero is None:
            return False
        
        # Remove espaços, hífens e parênteses
        limpo = re.sub(r'[\s\-()]', '', numero)
        # Verifica se tem entre 8-15 dígitos, opcionalmente com + no início
        return bool(re.match(r'^(\+)?\d{8,15}$', limpo))

    @staticmethod
    def utilizador_valido(utilizador):
        """Valida utilizador completo"""
        return (utilizador is not None and
                Validation.nome_valido(utilizador.get_nome()) and
                Validation.password_valida(utilizador.get_password()))

    @staticmethod
    def cliente_valido(cliente, clientes_dict):
        """Valida cliente completo"""
        if cliente is None:
            return False
        
        nome_ok = Validation.nome_valido(cliente.get_nome())
        telefone_ok = Validation.numero_telefone_valido(cliente.get_numero_telefone())
        nao_duplicado = not Validation.cliente_duplicado(clientes_dict, cliente.get_nome(), cliente.get_numero_telefone())
        
        # Validação específica para cliente semanal
        tipo_ok = True
        if cliente.get_tipo_cliente() == TipoCliente.SEMANAL:
            tipo_ok = (cliente.get_dia_semana() is not None and 
                      cliente.get_hora_corte() is not None)
        
        return nome_ok and telefone_ok and nao_duplicado and tipo_ok

    @staticmethod
    def marcacao_valida(marcacao, clientes_dict):
        """Valida marcação completa"""
        return (marcacao is not None and
                Validation.cliente_valido(marcacao.get_cliente(), clientes_dict) and
                marcacao.get_data_hora() is not None and
                Validation.hora_valida(data_hora=marcacao.get_data_hora()) and
                marcacao.get_duracao() > 0)