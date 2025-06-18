package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import models.*;

public class Logger {
    public static void escreverLogUtilizador(Log log) {
        try (FileWriter writer = new FileWriter("../logs/logsUtilizador.json", true)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String linha = String.format(
                "[%s] Utilizador: %s | Detalhes: %s\n",
                log.getDataHora().format(formatter),
                log.getUtilizador() != null ? log.getUtilizador().getNome() : "N/A",
                log.getDetalhes()
            );
            writer.write(linha);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }

    public static void escreverLogCliente(Log log) {
        try (FileWriter writer = new FileWriter("../logs/logsClientes.json", true)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String linha = String.format(
                "[%s] Cliente: %s | Detalhes: %s\n",
                log.getDataHora().format(formatter),
                log.getCliente() != null ? log.getCliente().getNome() : "N/A",
                log.getDetalhes()
            );
            writer.write(linha);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }

    public static void escreverLogMarcacao(Log log) {
        try (FileWriter writer = new FileWriter("../logs/logsMarcacoes.json", true)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String linha = String.format(
                "[%s] Marcacao: %s de %s | Detalhes: %s\n",
                log.getDataHora().format(formatter),
                log.getMarcacao() != null ? log.getMarcacao().getDataHora().toString() : "N/A",
                log.getMarcacao().getCliente() != null ? log.getMarcacao().getCliente().getNome() : "N/A",
                log.getDetalhes()
            );
            writer.write(linha);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no ficheiro de log: " + e.getMessage());
        }
    }
}
