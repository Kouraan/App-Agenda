package controller;

import java.time.LocalDateTime;
import java.util.Map;
import models.*;
import utils.*;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class Controller {
    private Stage stage;
    private Utilizador utilizador;
    private Map<String, Cliente> clientesMap;
    private Map<LocalDateTime, Marcacao> marcacoesMap;

    public Controller(Stage stage) {
        this.stage = stage;
        utilizador = Persistencia.lerUtilizador();
        clientesMap = Persistencia.lerClientes();
        marcacoesMap = Persistencia.lerMarcacoes();
    }

    public void startApp() throws Exception {
        if (utilizador != null) {
            mostrarLogin();
        } else {
            // Avançar para a app sem login
            System.out.println("Nenhum utilizador encontrado, a app avança sem login.");
        }
    }

    private void mostrarLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
        Parent root = loader.load();
        LoginController loginController = loader.getController();
        loginController.setUtilizador(utilizador);
        loginController.setAppController(this); // Para navegação futura
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }
}
