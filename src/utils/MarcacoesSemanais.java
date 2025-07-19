package utils;

import models.Cliente;
import models.Marcacao;
import java.time.*;
import java.util.*;

public class MarcacoesSemanais {
    public static List<Marcacao> gerarMarcacoesSemanais(Cliente cliente, Map<LocalDateTime, Marcacao> marcacoesMap, LocalDate dataInicio) {
        List<Marcacao> novas = new ArrayList<>();
        if (cliente.getTipoCliente() != Cliente.TipoCliente.SEMANAL) return novas;
        if (cliente.getDiaSemana() == null || cliente.getHoraCorte() == null) return novas;

        DayOfWeek diaSemana = DayOfWeek.valueOf(cliente.getDiaSemana().toUpperCase());
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
}