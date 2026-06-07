from datetime import date, time, datetime, timedelta
from typing import List, Dict
from ..models.Cliente import Cliente, TipoCliente
from ..models.Marcacao import Marcacao
from .Feriados import Feriados

class MarcacoesSemanais:
    @staticmethod
    def gerar_marcacoes_semanais(cliente: Cliente, marcacoes_map: Dict[datetime, Marcacao],
                                data_inicio: date, meses_a_frente: int = 6) -> List[Marcacao]:
        """
        Gera marcações semanais para um cliente SEMANAL:
         - tenta agendar a cada semana,
         - se o slot estiver ocupado (overlap por duração), "passa à frente" para a seguinte semana
           até encontrar um slot livre — depois a sequência semanal continua a partir dessa marcação.
         - actualiza o marcacoes_map (adiciona as novas marcações) para evitar conflitos entre novas marcações.
        """
        novas = []

        if cliente.get_tipo_cliente() != TipoCliente.SEMANAL:
            return novas

        dia_semana_pt = cliente.get_dia_semana()
        hora_str = cliente.get_hora_corte()
        if not dia_semana_pt or not hora_str:
            return novas

        # traduz dia e hora
        try:
            dia_semana = MarcacoesSemanais._traduzir_dia_semana(dia_semana_pt)
            hora = time.fromisoformat(hora_str)
        except Exception:
            return novas

        duracao = 15 if cliente.is_rapido() else 30

        # helper overlap (baseado em datetimes)
        def overlap_dt(a_start: datetime, a_dur: int, b_start: datetime, b_dur: int) -> bool:
            a_end = a_start + timedelta(minutes=a_dur)
            b_end = b_start + timedelta(minutes=b_dur)
            return not (a_end <= b_start or b_end <= a_start)

        # encontra próxima ocorrência (não hoje)
        data = data_inicio
        while data.weekday() != dia_semana:
            data += timedelta(days=1)

        limite = data_inicio + timedelta(days=meses_a_frente * 31)
        limite_dt = datetime.combine(limite, time.max)

        candidate = datetime.combine(data, hora)

        while candidate <= limite_dt:
            # procura a primeira semana nessa sequência onde o slot esteja livre
            while True:
                occupied = False
                # verificar todas as marcações existentes nesse dia (e também novas geradas)
                for ex_dt, ex_m in list(marcacoes_map.items()):
                    try:
                        ex_start = ex_dt  # chave do map é datetime
                        if ex_start.tzinfo is not None:
                            ex_start = ex_start.replace(tzinfo=None)
                        ex_dur = ex_m.get_duracao() if hasattr(ex_m, 'get_duracao') else getattr(ex_m, 'duracao', 30)
                    except Exception:
                        continue
                    if overlap_dt(candidate, duracao, ex_start, ex_dur):
                        occupied = True
                        break
                if not occupied:
                    break
                # se ocupada, adiar uma semana e tentar novamente
                candidate = candidate + timedelta(days=7)
                if candidate > limite_dt:
                    break

            if candidate > limite_dt:
                break

            # não criar duplicados para o mesmo cliente no mesmo datetime
            already = False
            for ex_dt, ex_m in list(marcacoes_map.items()):
                try:
                    c = ex_m.get_cliente()
                    if ex_dt == candidate and c is not None and c.get_nome() == cliente.get_nome():
                        already = True
                        break
                except Exception:
                    continue

            if not already:
                # criar Marcacao (usar mesma ordem de construtor que AppController emprega)
                try:
                    nova = Marcacao(candidate, cliente, duracao, "")
                except TypeError:
                    # fallback caso a assinatura seja diferente
                    nova = Marcacao(candidate, cliente, duracao, "")
                # adicionar ao resultado e ao mapa (para checagens subsequentes)
                novas.append(nova)
                marcacoes_map[candidate] = nova

            # sequência segue 7 dias após a marcação efectivamente criada
            candidate = candidate + timedelta(days=7)

        return novas

    @staticmethod
    def _traduzir_dia_semana(dia_semana_pt: str) -> int:
        traducao = {
            "segunda": 0,
            "terça": 1,
            "terca": 1,
            "quarta": 2,
            "quinta": 3,
            "sexta": 4,
            "sábado": 5,
            "sabado": 5,
            "domingo": 6
        }

        dia_lower = dia_semana_pt.lower().strip()
        if dia_lower in traducao:
            return traducao[dia_lower]
        else:
            raise ValueError(f"Dia da semana inválido: {dia_semana_pt}")