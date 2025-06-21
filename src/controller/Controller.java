package controller;

import java.time.LocalDateTime;
import java.util.Map;
import models.*;
import utils.*;

public class Controller {
    Utilizador utilizador = Persistencia.lerUtilizador();
    Map<String, Cliente> clientesMap;
    Map<LocalDateTime, Marcacao> marcacoesMap;

    public Controller() {
        utilizador = Persistencia.lerUtilizador();
        clientesMap = Persistencia.lerClientes();
        marcacoesMap = Persistencia.lerMarcacoes();
    }
}
