package utils;

import java.time.*;
import java.util.*;
import models.Cliente;
import models.Marcacao;

public class MarcacoesSemanais {
    public static List<Marcacao> gerarMarcacoesSemanais(Cliente cliente, Map<LocalDateTime, Marcacao> marcacoesMap, LocalDate dataInicio) {
        List<Marcacao> novas = new ArrayList<>();
        if (cliente.getTipoCliente() != Cliente.TipoCliente.SEMANAL) return novas;
        if (cliente.getDiaSemana() == null || cliente.getHoraCorte() == null) return novas;

        DayOfWeek diaSemana = traduzirDiaSemana(cliente.getDiaSemana());
        LocalTime hora = LocalTime.parse(cliente.getHoraCorte());
        LocalDate data = dataInicio;

        // Encontrar o próximo dia da semana correto
        while (data.getDayOfWeek() != diaSemana) {
            data = data.plusDays(1);
        }

        LocalDate dataLimite = dataInicio.plusMonths(6);

        while (!data.isAfter(dataLimite)) {
            LocalDateTime dataHora = data.atTime(hora);

            // Só marca se não for feriado e não houver marcação já existente
            if (!utils.Feriados.isFeriado(data) && !marcacoesMap.containsKey(dataHora)) {
                int duracao = cliente.isRapido() ? 15 : 30;
                Marcacao nova = new Marcacao(dataHora, cliente, duracao, "");
                novas.add(nova);
            }
            data = data.plusWeeks(1);
        }
        return novas;
    }

    private static DayOfWeek traduzirDiaSemana(String diaSemanaPt) {
        switch (diaSemanaPt.toLowerCase()) {
            case "segunda": return DayOfWeek.MONDAY;
            case "terça":   return DayOfWeek.TUESDAY;
            case "quarta":  return DayOfWeek.WEDNESDAY;
            case "quinta":  return DayOfWeek.THURSDAY;
            case "sexta":   return DayOfWeek.FRIDAY;
            case "sábado":  return DayOfWeek.SATURDAY;
            case "domingo": return DayOfWeek.SUNDAY;
            default: throw new IllegalArgumentException("Dia da semana inválido: " + diaSemanaPt);
        }
    }
}