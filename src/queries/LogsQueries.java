package queries;

import java.time.LocalDateTime;
import models.*;
import utils.Logger;

public class LogsQueries {
    public static void registarLogUtilizador(Utilizador utilizador, String detalhes) {
        Log log = new Log(LocalDateTime.now(), utilizador, detalhes);
        Logger.escreverLogUtilizador(log);
    }

    public static void registarLogCliente(Cliente cliente, String detalhes) {
        Log log = new Log(LocalDateTime.now(), cliente, detalhes);
        Logger.escreverLogCliente(log);
    }

    public static void registarLogMarcacao(Marcacao marcacao, String detalhes) {
        Log log = new Log(LocalDateTime.now(), marcacao, detalhes);
        Logger.escreverLogMarcacao(log);
    }
}
