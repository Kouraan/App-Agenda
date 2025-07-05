package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import models.Utilizador;
import queries.UtilizadorQueries;
import controller.Controller;


public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button entrarBtn;
    @FXML private StackPane stackPaneRoot;

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
        entrarBtn.setFocusTraversable(false);

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });

        usernameField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        passwordField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        
        stackPaneRoot.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (usernameField.isFocused() && !usernameField.localToScene(usernameField.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
                usernameField.getParent().requestFocus();
            }
            if (passwordField.isFocused() && !passwordField.localToScene(passwordField.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
                passwordField.getParent().requestFocus();
            }
        });
        stackPaneRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((obsFocus, wasFocused, isFocused) -> {
                            if (isFocused) {
                                stackPaneRoot.requestFocus();
                            }
                        });
                    }
                });
            }
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