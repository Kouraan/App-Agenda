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
        LocalTime inicioManha = LocalTime.of(7, 0);
        LocalTime fimManha = LocalTime.of(12, 30);
        LocalTime inicioTarde = LocalTime.of(14, 0);
        LocalTime fimTarde = LocalTime.of(21, 0);

        // Caso para horaCorte
        if (horaCorte != null && dataHora == null) {
            boolean quartoHora = horaCorte.getMinute() % 15 == 0;
            boolean manha = !horaCorte.isBefore(inicioManha) && !horaCorte.isAfter(fimManha);
            boolean tarde = !horaCorte.isBefore(inicioTarde) && !horaCorte.isAfter(fimTarde);
            return quartoHora && (manha || tarde);
        }

        // Caso para dataHora
        if (dataHora != null && horaCorte == null) {
            LocalTime hora = dataHora.toLocalTime();
            int diaSemana = dataHora.getDayOfWeek().getValue();
            boolean diaOk = diaSemana >= 1 && diaSemana <= 6;
            boolean quartoHora = hora.getMinute() % 15 == 0;
            boolean manha = !hora.isBefore(inicioManha) && !hora.isAfter(fimManha);
            boolean tarde = !hora.isBefore(inicioTarde) && !hora.isAfter(fimTarde);
            boolean horaFutura = true;
            if (dataHora.toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
                horaFutura = hora.isAfter(LocalTime.now());
            }
            return diaOk && quartoHora && (manha || tarde) && horaFutura;
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

    // Marcacao Duplicada
    public static boolean marcacaoDuplicada(Map<LocalDateTime, Marcacao> marcacoes, LocalDateTime dataHora) {
        return marcacoes.containsKey(dataHora);
    }

    // Valida número de telefone
    public static boolean numeroTelefoneValido(String numero) {
        if (numero == null) return false;
        String limpo = numero.replaceAll("[\\s\\-()]", "");
        return limpo.matches("^(\\+)?\\d{8,15}$");
    }

    // Validacao de Utilizador, Cliente, Marcacao e Log
    public static boolean utilizadorValido(Utilizador u) {
        return u != null &&
               nomeValido(u.getNome()) &&
               u.getPassword() != null && !u.getPassword().trim().isEmpty();
    }

    public static boolean clienteValido(Cliente c, Map<String, Cliente> clientes) {
        return c != null &&
               nomeValido(c.getNome()) &&
               numeroTelefoneValido(c.getNumeroTelefone()) &&
               !clienteDuplicado(clientes, c.getNome(), c.getNumeroTelefone()) &&
               (c.getTipoCliente() == Cliente.TipoCliente.NORMAL || c.getTipoCliente() == Cliente.TipoCliente.DESCONHECIDO ||
                (c.getTipoCliente() == Cliente.TipoCliente.SEMANAL && c.getDiaSemana() != null && c.getHoraCorte() != null));
    }

    public static boolean marcacaoValida(Marcacao m, Map<String, Cliente> clientes) {
        return m != null &&
               clienteValido(m.getCliente(), clientes) &&
               m.getDataHora() != null &&
               horaValida(null, m.getDataHora()) &&
               m.getDuracao() > 0;
    }
}