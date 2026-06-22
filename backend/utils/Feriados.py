from datetime import date, timedelta

class Feriados:
    # Feriados fixos (dia, mês)
    FIXOS = [
        (1, 1),    # Ano novo
        (25, 4),   # Dia da liberdade
        (1, 5),    # Dia do trabalhador
        (10, 6),   # Dia de Portugal
        (24, 6),   # Batalha de São Mamede
        (15, 8),   # Assunção de Nossa Senhora
        (5, 10),   # Implantação da República
        (1, 11),   # Todos os Santos
        (1, 12),   # Restauração da Independência
        (8, 12),   # Imaculada Conceição
        (25, 12)   # Natal
    ]

    @staticmethod
    def calcular_pascoa(ano):
        """Calcula a data da Páscoa usando o algoritmo de Gauss"""
        a = ano % 19
        b = ano // 100
        c = ano % 100
        d = b // 4
        e = b % 4
        f = (b + 8) // 25
        g = (b - f + 1) // 3
        h = (19 * a + b - d - g + 15) % 30
        i = c // 4
        k = c % 4
        l = (32 + 2 * e + 2 * i - h - k) % 7
        m = (a + 11 * h + 22 * l) // 451
        mes = (h + l - 7 * m + 114) // 31
        dia = ((h + l - 7 * m + 114) % 31) + 1
        return date(ano, mes, dia)

    @staticmethod
    def get_feriados(ano):
        """Retorna lista de feriados para o ano"""
        feriados = []
        
        # Adicionar feriados fixos
        for dia, mes in Feriados.FIXOS:
            feriados.append(date(ano, mes, dia))
        
        # Calcular feriados móveis baseados na Páscoa
        pascoa = Feriados.calcular_pascoa(ano)
        feriados.append(pascoa)  # Páscoa
        feriados.append(pascoa - timedelta(days=2))  # Sexta-feira Santa
        feriados.append(pascoa - timedelta(days=47)) # Carnaval
        feriados.append(pascoa + timedelta(days=60)) # Corpo de Deus
        
        return feriados

    @staticmethod
    def is_feriado(data):
        """Verifica se uma data é feriado"""
        if isinstance(data, str):
            data = date.fromisoformat(data)
        return data in Feriados.get_feriados(data.year)