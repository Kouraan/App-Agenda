package utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import models.*;

public class Validation {
    public static boolean podeClienteSemanal(Map<String, Cliente> clientes, DayOfWeek diaSemana, LocalTime horaCorte) {
        for (Cliente c : clientes.values()) {
            if(c.getTipoCliente() == Cliente.TipoCliente.SEMANAL) {
                if (diaSemana.name().equalsIgnoreCase(c.getDiaSemana()) && horaCorte.toString().equals(c.getHoraCorte())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean horaValida(LocalTime horaCorte, LocalDateTime dataHora) {
        LocalTime inicio = LocalTime.of(7, 0);
        LocalTime fim = LocalTime.of(21, 0);

        // Caso para horaCorte
        if (horaCorte != null && dataHora == null) {
            boolean meiaHora = horaCorte.getMinute() == 0 || horaCorte.getMinute() == 30;
            boolean horaOk = !horaCorte.isBefore(inicio) && !horaCorte.isAfter(fim);
            return meiaHora && horaOk;
        }

        // Caso para dataHora
        if (dataHora != null && horaCorte == null) {
            LocalTime hora = dataHora.toLocalTime();
            int diaSemana = dataHora.getDayOfWeek().getValue();
            boolean diaOk = diaSemana >= 1 && diaSemana <= 6;
            boolean meiaHora = hora.getMinute() == 0 || hora.getMinute() == 30;
            boolean horaOk = !hora.isBefore(inicio) && !hora.isAfter(fim);
            boolean horaFutura = true;
            if (dataHora.toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
                horaFutura = hora.isAfter(LocalTime.now());
            }
            return diaOk && meiaHora && horaOk && horaFutura;
        }
            
        return false;
    }

    // Valida nome
    public static boolean nomeValido(String nome) {
        return nome != null && !nome.trim().isEmpty() && nome.length() >= 2;
    }

    // Clientes Duplicados
    public static boolean clienteDuplicado(Map<String, Cliente> clientes, String nome, String numeroTelefone) {
        return clientes.values().stream().anyMatch(
            c -> c.getNome().equalsIgnoreCase(nome) || c.getNumeroTelefone().equals(numeroTelefone)
        );
    }

    // Valida número de telefone
    public static boolean numeroTelefoneValido(String numero) {
        if (numero == null) return false;
        String limpo = numero.replaceAll("[\\s\\-()]", "");
        return limpo.matches("^(\\+)?\\d{8,15}$");
}
}