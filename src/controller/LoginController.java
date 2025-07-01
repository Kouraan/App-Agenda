package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import models.Utilizador;
import queries.UtilizadorQueries;
import controller.Controller;


public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private Utilizador utilizador;
    private Controller appController;

    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }
    public void setAppController(Controller appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        // ENTER ativa login
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
    }

    @FXML
    private void handleLogin() {
        boolean valido = utilizador != null &&
            queries.UtilizadorQueries.autenticar(usernameField.getText(), passwordField.getText(), utilizador);
            
        if (valido) {
            // Login bem-sucedido, abrir página principal
            try {
                appController.mostrarPaginaPrincipal();
            } catch (Exception e) {
                errorLabel.setText("Erro ao abrir página principal.");
                errorLabel.setVisible(true);
                e.printStackTrace();
            }
        } else {
            errorLabel.setVisible(true);
            setFieldError(usernameField, true);
            setFieldError(passwordField, true);
        }
    }

    private void setFieldError(TextField field, boolean error) {
        if (error) {
            field.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        } else {
            field.setStyle("");
        }
    }
}