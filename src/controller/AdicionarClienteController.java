package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.time.LocalTime;
import java.util.Map;

public class AdicionarClienteController {
    @FXML public TextField nomeField;
    @FXML public TextField telefoneField;
    @FXML public ComboBox<String> diaSemanaCombo;
    @FXML public ComboBox<String> horaCorteCombo;
    @FXML public Button btnSalvar;
    @FXML public Button btnCancelar;
    @FXML public CheckBox semanalCheck;
    @FXML public VBox rootVBox;

    private controller.Controller appController;

    public void setAppController(controller.Controller appController) {
        this.appController = appController;
    }

    @FXML
    public void initialize() {
        diaSemanaCombo.getItems().addAll("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo");

        diaSemanaCombo.setDisable(true);
        horaCorteCombo.setDisable(true);

        semanalCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            diaSemanaCombo.setDisable(!newVal);
            horaCorteCombo.setDisable(true);
            horaCorteCombo.getItems().clear();
        });

        diaSemanaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            horaCorteCombo.getItems().clear();
            if (newVal != null) {
                Map<String, models.Cliente> clientes = appController.getClientesMap();
                LocalTime hora = LocalTime.of(7, 0);
                while (!hora.isAfter(LocalTime.of(21, 0))) {
                    final String horaAtual = hora.toString();
                    boolean ocupado = clientes.values().stream().anyMatch(c ->
                        c.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL &&
                        newVal.equalsIgnoreCase(c.getDiaSemana()) &&
                        horaAtual.equals(c.getHoraCorte())
                    );
                    if (!ocupado) {
                        horaCorteCombo.getItems().add(horaAtual);
                    }
                    hora = hora.plusMinutes(30);
                }
                horaCorteCombo.setDisable(false);
            } else {
                horaCorteCombo.setDisable(true);
            }
        });

        semanalCheck.setSelected(false);

        nomeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            nomeField.setStyle("");
        });
        telefoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            telefoneField.setStyle("");
        });
        diaSemanaCombo.setOnAction(e -> diaSemanaCombo.setStyle(""));
        horaCorteCombo.setOnAction(e -> horaCorteCombo.setStyle(""));

        rootVBox.setOnMousePressed(e -> rootVBox.requestFocus());

        nomeField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        telefoneField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        semanalCheck.setFocusTraversable(false);
        diaSemanaCombo.setFocusTraversable(false);
        horaCorteCombo.setFocusTraversable(false);
        btnSalvar.setFocusTraversable(false);
        btnCancelar.setFocusTraversable(false);

        rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelar();
                    }
                });
            }
        });
    }

    @FXML
    private void salvarCliente() {
        boolean erro = false;
        String nome = nomeField.getText().trim();
        String telefone = telefoneField.getText().trim();
        String diaSemana = diaSemanaCombo.getValue();
        String horaCorte = horaCorteCombo.getValue();

        // Limpar estilos de erro
        nomeField.setStyle("");
        telefoneField.setStyle("");
        diaSemanaCombo.setStyle("");
        horaCorteCombo.setStyle("");

        // Campos obrigatórios
        if (nome.isEmpty()) {
            nomeField.setStyle("-fx-border-color: red;");
            erro = true;
        }
        if (telefone.isEmpty()) {
            telefoneField.setStyle("-fx-border-color: red;");
            erro = true;
        }
        if (erro) {
            mostrarAlerta("Preencha todos os campos obrigatórios.");
            return;
        }

        // Cliente Duplicado
        Map<String, models.Cliente> clientes = appController.getClientesMap();
        if (clientes.values().stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nome))) {
            nomeField.setStyle("-fx-border-color: red;");
            mostrarAlerta("Já existe um cliente com esse nome.");
            return;
        }
        if (clientes.values().stream().anyMatch(c -> c.getNumeroTelefone().equals(telefone))) {
            telefoneField.setStyle("-fx-border-color: red;");
            mostrarAlerta("Já existe um cliente com esse número de telefone.");
            return;
        }

        // Validação de formato
        if (!utils.Validation.nomeValido(nome)) {
            nomeField.setStyle("-fx-border-color: red;");
            mostrarAlerta("Nome inválido.");
            return;
        }
        if (!utils.Validation.numeroTelefoneValido(telefone)) {
            telefoneField.setStyle("-fx-border-color: red;");
            mostrarAlerta("Telefone inválido.");
            return;
        }

        // Cliente semanal
        boolean semanal = semanalCheck.isSelected();
        if (semanal) {
            if (diaSemana == null || diaSemana.isEmpty()) {
                diaSemanaCombo.setStyle("-fx-border-color: red;");
                mostrarAlerta("Selecione um dia da semana.");
                return;
            }
            if (horaCorte == null || horaCorte.isEmpty()) {
                horaCorteCombo.setStyle("-fx-border-color: red;");
                mostrarAlerta("Selecione uma hora de corte.");
                return;
            }
            // Verifica se a hora já está ocupada
            boolean ocupado = clientes.values().stream().anyMatch(c -> 
                c.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL &&
                diaSemana.equalsIgnoreCase(c.getDiaSemana()) &&
                horaCorte.equals(c.getHoraCorte())
            );
            if (ocupado) {
                horaCorteCombo.setStyle("-fx-border-color: red;");
                mostrarAlerta("Já existe um cliente semanal nesse horário.");
                return;
            }
        }

        // Validação final
        models.Cliente novoCliente;
        if (semanal) {
            novoCliente = new models.Cliente(nome, telefone, models.Cliente.TipoCliente.SEMANAL, diaSemana, horaCorte);
        } else {
            novoCliente = new models.Cliente(nome, telefone, models.Cliente.TipoCliente.NORMAL);
        }
        if (!utils.Validation.clienteValido(novoCliente, clientes)) {
            mostrarAlerta("Dados do cliente inválidos.");
            return;
        }

        // Adiciona e salva novo Cliente
        clientes.put(novoCliente.getNome(), novoCliente);
        utils.Persistencia.guardarClientes(clientes);

        ((Stage) btnSalvar.getScene().getWindow()).close();
    }

    @FXML
    private void cancelar() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.showAndWait();
    }
}