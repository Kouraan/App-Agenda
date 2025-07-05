package controller;

import java.time.LocalDateTime;
import java.util.Map;
import models.*;
import utils.*;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

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

    public Map<String, Cliente> getClientesMap() {
        return clientesMap;
    }

    public void startApp() throws Exception {
        if (utilizador != null) {
            mostrarLogin();
        } else {
            mostrarRegisto();
        }
    }

    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public void mostrarLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
        Parent root = loader.load();
        LoginController loginController = loader.getController();
        loginController.setUtilizador(utilizador);
        loginController.setAppController(this); // Para navegação futura
        Scene scene = new Scene(root);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.setResizable(false);
        stage.show();
    }

    public void mostrarPaginaPrincipal() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PaginaPrincipal.fxml"));
        Parent root = loader.load();
        PaginaPrincipalController paginaPrincipalController = loader.getController();
        paginaPrincipalController.setUtilizador(utilizador);
        paginaPrincipalController.setAppController(this);
        Scene scene = new Scene(root);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.setScene(scene);
        stage.setTitle("App Agenda - Principal");
        stage.setResizable(false);
        stage.show();
    }

    private void mostrarRegisto() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Registo.fxml"));
        Parent root = loader.load();
        RegistoController registoController = loader.getController();
        registoController.setAppController(this);
        Scene scene = new Scene(root);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.setScene(scene);
        stage.setTitle("Registo");
        stage.setResizable(false);
        stage.show();
    }
}
