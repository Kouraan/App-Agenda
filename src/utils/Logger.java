package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import models.*;

public class Logger {
    private static String dataAtual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    // Log generico Utilizador
    public static void logUtilizador(String mensagem) {
        LocalDateTime now = LocalDateTime.now();
        String data = dataAtual();
        String ano = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mes = now.format(DateTimeFormatter.ofPattern("MM"));
        String linha = "[" + data + "]" + mensagem + "\n";
        String dirPath = "logs/" + ano;
        String filePath = dirPath + "/logsUtilizador" + mes + ".json";

        try {
            // Garante que o diretório existe
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Ler o ficheiro se existir
            StringBuilder sb = new StringBuilder();
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String l;
                    while ((l = reader.readLine()) != null) {
                        sb.append(l).append("\n");
                    }
                }
            }

            // Escrever o log
            try (FileWriter writer = new FileWriter(filePath, false)) {
                writer.write(linha);
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }

    // Log Registo
    public static void logRegisto(String nome) {
        logUtilizador(" Novo utilizador registado com o nome: '" + nome + "'.");
    }

    // Log Login
    public static void logLogin(String nome) {
        logUtilizador(" Utilizador '" + nome + "' efetuou login na sua conta.");
    }

    // Log Logout
    public static void logLogout(String nome) {
        logUtilizador(" Utilizador '" + nome + "' deu logout da sua conta.");
    }

    // Log App Iniciada
    public static void logAppIniciada() {
        logUtilizador(" Aplicação iniciada.");
    }

    // Log App Terminada
    public static void logAppTerminada() {
        logUtilizador(" Aplicação terminada.");
    }

    // Log generico Cliete
    public static void logCliente(String mensagem) {
        LocalDateTime now = LocalDateTime.now();
        String data = dataAtual();
        String ano = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mes = now.format(DateTimeFormatter.ofPattern("MM"));
        String linha = "[" + data + "]" + mensagem + "\n";
        String dirPath = "logs/" + ano;
        String filePath = dirPath + "/logsClientes" + mes + ".json";

        try {
            // Garante que o diretório existe
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Ler o ficheiro se existir
            StringBuilder sb = new StringBuilder();
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String l;
                    while ((l = reader.readLine()) != null) {
                        sb.append(l).append("\n");
                    }
                }
            }

            // Escrever o log
            try (FileWriter writer = new FileWriter(filePath, false)) {
                writer.write(linha);
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }

    // Cliente criado
    public static void logClienteCriado(String nome) {
        logCliente("Cliente '" + nome + "' criado.");
    }

    // Cliente apagado
    public static void logClienteApagado(String nome) {
        logCliente("Cliente '" + nome + "' apagado.");
    }

    // Falta adicionada
    public static void logFaltaAdicionada(String nome) {
        logCliente("Falta adicionada ao Cliente '" + nome + "'.");
    }

    // Falta retirada
    public static void logFaltaRetirada(String nome) {
        logCliente("Falta retirada ao Cliente '" + nome + "'.");
    }

    // Nome alterado
    public static void logNomeAlterado(String nomeAntigo, String nomeNovo) {
        logCliente("Nome do Cliente '" + nomeAntigo + "' alterado para '" + nomeNovo + "'.");
    }

    // Número alterado
    public static void logNumeroAlterado(String nome, String numeroNovo) {
        logCliente("Numero do Cliente '" + nome + "' alterado para '" + numeroNovo + "'.");
    }

    // Tipo alterado de SEMANAL para NORMAL
    public static void logTipoSemanalParaNormal(String nome) {
        logCliente("Cliente '" + nome + "' alterado de SEMANAL para NORMAL.");
    }

    // Tipo alterado de NORMAL para SEMANAL
    public static void logTipoNormalParaSemanal(String nome, String dia, String hora) {
        logCliente(
                "Cliente '" + nome + "' alterado de NORMAL para SEMANAL, horário de '" + dia + "' às '" + hora + "'.");
    }

    // Horário alterado em SEMANAL
    public static void logHorarioSemanalAlterado(String nome, String dia, String hora) {
        logCliente("Horas do Cliente SEMANAL '" + nome + "' alteradas para '" + dia + "' às '" + hora + "'.");
    }

    // Log generico Marcacao
    public static void logMarcacao(String mensagem) {
        LocalDateTime now = LocalDateTime.now();
        String data = dataAtual();
        String ano = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mes = now.format(DateTimeFormatter.ofPattern("MM"));
        String linha = "[" + data + "] " + mensagem + "\n";
        String dirPath = "logs/" + ano;
        String filePath = dirPath + "/logsMarcacoes" + mes + ".json";

        try {
            // Garante que o diretório existe
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Ler o ficheiro se existir
            StringBuilder sb = new StringBuilder();
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String l;
                    while ((l = reader.readLine()) != null) {
                        sb.append(l).append("\n");
                    }
                }
            }

            // Escrever o log
            try (FileWriter writer = new FileWriter(filePath, false)) {
                writer.write(linha);
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }

    // Log de marcação criada
    public static void logMarcacaoCriada(Marcacao marcacao) {
        String nome = marcacao.getCliente() != null ? marcacao.getCliente().getNome() : "N/A";
        String dataHora = marcacao.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        logMarcacao("Marcação criada para '" + nome + "' em " + dataHora + ".");
    }

    // Log de marcação apagada
    public static void logMarcacaoApagada(Marcacao marcacao) {
        String nome = marcacao.getCliente() != null ? marcacao.getCliente().getNome() : "N/A";
        String dataHora = marcacao.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        logMarcacao("Marcação de '" + nome + "' em " + dataHora + " apagada.");
    }

    // Log de alteração de observações
    public static void logMarcacaoObsAlterada(Marcacao marcacao, String obsNova) {
        String nome = marcacao.getCliente() != null ? marcacao.getCliente().getNome() : "N/A";
        String dataHora = marcacao.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        logMarcacao(
                "Observações da marcação de '" + nome + "' em " + dataHora + " alteradas" + " para '" + obsNova + "'.");
    }

    // Log de alteração de data/hora
    public static void logMarcacaoDataHoraAlterada(Marcacao marcacao, String dataHoraAntiga, String dataHoraNova) {
        String nome = marcacao.getCliente() != null ? marcacao.getCliente().getNome() : "N/A";
        logMarcacao("Data/hora da marcação de '" + nome + "' alterada de " + dataHoraAntiga + " para " + dataHoraNova
                + ".");
    }

    // Log de Falta à Marcacao
    public static void logMarcacaoFalta(Marcacao marcacao) {
        String nome = marcacao.getCliente() != null ? marcacao.getCliente().getNome() : "N/A";
        String dataHora = marcacao.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        logMarcacao("O cliente '" + nome + "' faltou a marcacao na data '" + dataHora + "'.");
    }

    // Log genérico Pendente
    public static void logPendente(String mensagem) {
        LocalDateTime now = LocalDateTime.now();
        String data = dataAtual();
        String ano = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mes = now.format(DateTimeFormatter.ofPattern("MM"));
        String linha = "[" + data + "]" + mensagem + "\n";
        String dirPath = "logs/" + ano;
        String filePath = dirPath + "/logsPendentes" + mes + ".json";

        try {
            // Garante que o diretório existe
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Ler o ficheiro se existir
            StringBuilder sb = new StringBuilder();
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String l;
                    while ((l = reader.readLine()) != null) {
                        sb.append(l).append("\n");
                    }
                }
            }

            // Escrever o log
            try (FileWriter writer = new FileWriter(filePath, false)) {
                writer.write(linha);
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }

    // Log pendente adicionado
    public static void logPendenteAdicionado(Pendente pendente) {
        logPendente("Cliente Pendente adicionado: '" + pendente.getNome() + "'.");
    }

    // Log pendente removido
    public static void logPendenteRemovido(Pendente pendente) {
        logPendente("Cliente Pendente removido: '" + pendente.getNome() + "'.");
    }
}
