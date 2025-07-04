package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyCode;
import models.Utilizador;
import utils.Persistencia;
import utils.Validation;

public class RegistoController {
    @FXML private TextField nomeField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button registoBtn;
    @FXML private StackPane stackPaneRoot;


    private Controller appController;

    public void setAppController(Controller appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        registoBtn.setFocusTraversable(false);

        nomeField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleRegisto();
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleRegisto();
        });

        nomeField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        passwordField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        
        stackPaneRoot.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (nomeField.isFocused() && !nomeField.localToScene(nomeField.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
                nomeField.getParent().requestFocus();
            }
            if (passwordField.isFocused() && !passwordField.localToScene(passwordField.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
                passwordField.getParent().requestFocus();
            }
        });
    }

    @FXML
    private void handleRegisto() {
        String nome = nomeField.getText();
        String password = passwordField.getText();

        if (!Validation.nomeValido(nome)) {
            errorLabel.setText("Nome inválido.");
            errorLabel.setVisible(true);
            return;
        }
        if (!Validation.passwordValida(password)) {
            errorLabel.setText("Password tem de ter mais de 5 caracteres.");
            errorLabel.setVisible(true);
            return;
        }

        Utilizador novo = new Utilizador(nome, password);
        if (Persistencia.guardarUtilizador(novo)) {
            errorLabel.setVisible(false);
            appController.setUtilizador(novo);
            // Voltar ao login
            try {
                appController.mostrarLogin();
            } catch (Exception e) {
                errorLabel.setText("Erro ao abrir login.");
                errorLabel.setVisible(true);
            }
        } else {
            errorLabel.setText("Erro ao guardar utilizador.");
            errorLabel.setVisible(true);
        }
    }
}