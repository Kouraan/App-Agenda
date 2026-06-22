from datetime import date, time, datetime, timedelta
from typing import List, Dict, Optional, Set
from ..models.Cliente import Cliente, TipoCliente
from ..models.Marcacao import Marcacao
from .Feriados import Feriados


class MarcacoesSemanais:
    @staticmethod
    def gerar_marcacoes_semanais(
        cliente: Cliente,
        marcacoes_map: Dict[datetime, Marcacao],
        data_inicio: date,
        meses_a_frente: int = 6,
        slots_usados: Optional[Set[str]] = None
    ) -> List[Marcacao]:
        """
        Gera marcações semanais para um cliente SEMANAL.

        slots_usados: conjunto de strings ISO de slots já gerados para este cliente.
                      Se um slot estiver aqui, não é regenerado mesmo que a marcação
                      tenha sido apagada ou alterada pelo utilizador.
        """
        novas = []

        if cliente.get_tipo_cliente() != TipoCliente.SEMANAL:
            return novas

        dia_semana_pt = cliente.get_dia_semana()
        hora_str = cliente.get_hora_corte()
        if not dia_semana_pt or not hora_str:
            return novas

        try:
            dia_semana = MarcacoesSemanais._traduzir_dia_semana(dia_semana_pt)
            hora = time.fromisoformat(hora_str)
        except Exception:
            return novas

        duracao = 15 if cliente.is_rapido() else 30

        def overlap_dt(a_start: datetime, a_dur: int,
                       b_start: datetime, b_dur: int) -> bool:
            a_end = a_start + timedelta(minutes=a_dur)
            b_end = b_start + timedelta(minutes=b_dur)
            return not (a_end <= b_start or b_end <= a_start)

        # Primeiro dia da semana correto a partir de data_inicio
        data = data_inicio
        while data.weekday() != dia_semana:
            data += timedelta(days=1)

        limite = data_inicio + timedelta(days=meses_a_frente * 31)
        limite_dt = datetime.combine(limite, time.max)

        candidate = datetime.combine(data, hora)

        while candidate <= limite_dt:

            # Encontrar o próximo slot válido: não processado E não ocupado
            while candidate <= limite_dt:
                candidate_iso = candidate.isoformat()

                # Slot já gerado anteriormente (mesmo que movido/apagado pelo user)
                if slots_usados is not None and candidate_iso in slots_usados:
                    candidate += timedelta(days=7)
                    continue

                # Verificar se está ocupado por outra marcação
                occupied = False
                for ex_dt, ex_m in list(marcacoes_map.items()):
                    try:
                        ex_start = ex_dt
                        if ex_start.tzinfo is not None:
                            ex_start = ex_start.replace(tzinfo=None)
                        ex_dur = (ex_m.get_duracao()
                                  if hasattr(ex_m, 'get_duracao')
                                  else getattr(ex_m, 'duracao', 30))
                    except Exception:
                        continue
                    if overlap_dt(candidate, duracao, ex_start, ex_dur):
                        occupied = True
                        break

                if not occupied:
                    break  # slot válido encontrado

                candidate += timedelta(days=7)

            if candidate > limite_dt:
                break

            # Não criar duplicado para o mesmo cliente no mesmo datetime
            already = False
            for ex_dt, ex_m in list(marcacoes_map.items()):
                try:
                    c = ex_m.get_cliente()
                    if (ex_dt == candidate and
                            c is not None and
                            c.get_nome() == cliente.get_nome()):
                        already = True
                        break
                except Exception:
                    continue

            if not already:
                nova = Marcacao(candidate, cliente, duracao, "")
                novas.append(nova)
                marcacoes_map[candidate] = nova

            candidate += timedelta(days=7)

        return novas

    @staticmethod
    def _traduzir_dia_semana(dia_semana_pt: str) -> int:
        traducao = {
            "segunda": 0,
            "terça":   1,
            "terca":   1,
            "quarta":  2,
            "quinta":  3,
            "sexta":   4,
            "sábado":  5,
            "sabado":  5,
            "domingo": 6,
        }
        dia_lower = dia_semana_pt.lower().strip()
        if dia_lower in traducao:
            return traducao[dia_lower]
        raise ValueError(f"Dia da semana inválido: {dia_semana_pt}")