package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import models.Cliente;
import java.time.LocalTime;
import java.util.Map;

public class DetalheClienteController {

    @FXML private ToggleButton btnEditar;
    @FXML private Button btnSalvar, btnSair, btnRemover,editNomeBtn, editTelefoneBtn, btnMaisFalta, btnMenosFalta;
    @FXML private TextField nomeField, telefoneField;
    @FXML private CheckBox semanalCheck;
    @FXML private ComboBox<String> diaSemanaCombo, horaCorteCombo;
    @FXML private Label faltasLabel;
    @FXML private VBox editBox;
    @FXML private GridPane gridVisual;
    @FXML private HBox semanaBox;

    private Cliente cliente;
    private Runnable onClienteAlterado;
    private boolean clienteAlterado = false;
    private boolean editandoNome = false, editandoTelefone = false;

    public void setOnClienteAlterado(Runnable callback) { this.onClienteAlterado = callback; }
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        mostrarVisual();
    }

    public boolean isClienteAlterado() {
        return clienteAlterado;
    }

    @FXML
    public void initialize() {
        btnEditar.setOnAction(e -> alternarModoEdicao(btnEditar.isSelected()));
        btnSair.setOnAction(e -> fechar());
        btnSalvar.setOnAction(e -> confirmarSalvar());
        btnRemover.setOnAction(e -> removerCliente());
        editNomeBtn.setOnAction(e -> liberarEdicaoNome());
        editTelefoneBtn.setOnAction(e -> liberarEdicaoTelefone());
        btnMaisFalta.setOnAction(e -> alterarFaltas(1));
        btnMenosFalta.setOnAction(e -> alterarFaltas(-1));
        semanalCheck.selectedProperty().addListener((obs, oldVal, newVal) -> atualizarCamposSemanal(newVal));
        diaSemanaCombo.valueProperty().addListener((obs, oldVal, newVal) -> atualizarHorasDisponiveis());

        editBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) fechar();
                });
            }
        });
    }

    private void mostrarVisual() {
        editBox.setVisible(false); editBox.setManaged(false);
        gridVisual.setVisible(true); gridVisual.setManaged(true);
        btnSalvar.setVisible(false); btnSalvar.setManaged(false);
        btnEditar.setSelected(false);

        gridVisual.getChildren().clear();
        int row = 0;
        addRow("Nome", cliente.getNome(), row++);
        addRow("Telefone", cliente.getNumeroTelefone(), row++);
        addRow("Tipo", cliente.getTipoCliente().toString(), row++);
        if (cliente.getTipoCliente() == Cliente.TipoCliente.SEMANAL) {
            addRow("Dia da Semana", cliente.getDiaSemana(), row++);
            addRow("Hora Corte", cliente.getHoraCorte(), row++);
        }
        addRow("Faltas", String.valueOf(cliente.getFaltas()), row);
    }

    private void addRow(String titulo, String valor, int row) {
        Label th = new Label(titulo);
        th.setStyle("-fx-background-color: #d6eaf8; -fx-text-fill: #222; -fx-font-size: 15px; -fx-font-weight: bold; "
                + "-fx-padding: 10 18 10 18; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-background-radius: 4;");
        th.setMaxWidth(Double.MAX_VALUE);
        th.setMinWidth(140);

        Label val = new Label(valor == null ? "—" : valor);
        val.setStyle("-fx-background-color: #f8fafd; -fx-font-size: 15px; -fx-padding: 10 18 10 18; "
                + "-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-background-radius: 4;");
        val.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(val, Priority.ALWAYS);
        
        gridVisual.add(th, 0, row);
        gridVisual.add(val, 1, row);
    }

    private void alternarModoEdicao(boolean editar) {
        if (editar) {
            gridVisual.setVisible(false); gridVisual.setManaged(false);
            editBox.setVisible(true); editBox.setManaged(true);
            btnSalvar.setVisible(true); btnSalvar.setManaged(true);

            semanalCheck.setSelected(cliente.getTipoCliente() == Cliente.TipoCliente.SEMANAL);
            nomeField.setText(cliente.getNome());
            nomeField.setEditable(false);
            telefoneField.setText(cliente.getNumeroTelefone());
            telefoneField.setEditable(false);
            faltasLabel.setText(String.valueOf(cliente.getFaltas()));

            diaSemanaCombo.getItems().setAll("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo");
            if (cliente.getTipoCliente() == Cliente.TipoCliente.SEMANAL) {
                semanaBox.setVisible(true); semanaBox.setManaged(true);
                diaSemanaCombo.setValue(cliente.getDiaSemana());
                horaCorteCombo.setValue(cliente.getHoraCorte());
            } else {
                semanaBox.setVisible(false); semanaBox.setManaged(false);
            }
        } else {
            mostrarVisual();
        }
    }

    private void liberarEdicaoNome() {
        nomeField.setEditable(true);
        nomeField.requestFocus();
        nomeField.selectAll();
        nomeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) nomeField.setEditable(false);
        });
    }

    private void liberarEdicaoTelefone() {
        telefoneField.setEditable(true);
        telefoneField.requestFocus();
        telefoneField.selectAll();
        telefoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) telefoneField.setEditable(false);
        });
    }

    private void atualizarCamposSemanal(boolean semanal) {
        semanaBox.setVisible(semanal); semanaBox.setManaged(semanal);
        if (!semanal) {
            diaSemanaCombo.setValue(null);
            horaCorteCombo.setValue(null);
        }
    }

    private void atualizarHorasDisponiveis() {
        horaCorteCombo.getItems().clear();
        String dia = diaSemanaCombo.getValue();
        if (dia != null) {
            LocalTime hora = LocalTime.of(7, 0);
            while (!hora.isAfter(LocalTime.of(21, 0))) {
                horaCorteCombo.getItems().add(hora.toString());
                hora = hora.plusMinutes(30);
            }
        }
    }

    private void alterarFaltas(int delta) {
        int faltas = Integer.parseInt(faltasLabel.getText());
        faltas = Math.max(0, faltas + delta);
        faltasLabel.setText(String.valueOf(faltas));
    }

    private void confirmarSalvar() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Deseja realmente alterar os dados do cliente?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirmar alteração");
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) salvar();
        });
    }

    private void salvar() {
        cliente.setNome(nomeField.getText());
        cliente.setNumeroTelefone(telefoneField.getText());
        cliente.setFaltas(Integer.parseInt(faltasLabel.getText()));
        if (semanalCheck.isSelected()) {
            cliente.setTipoCliente(Cliente.TipoCliente.SEMANAL);
            cliente.setDiaSemana(diaSemanaCombo.getValue());
            cliente.setHoraCorte(horaCorteCombo.getValue());
        } else {
            cliente.setTipoCliente(Cliente.TipoCliente.NORMAL);
            cliente.setDiaSemana(null);
            cliente.setHoraCorte(null);
        }
        if (onClienteAlterado != null) onClienteAlterado.run();
        clienteAlterado = true;
        mostrarVisual();
    }

    private void removerCliente() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Remover este cliente? Esta ação é irreversível.",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(120);
        dialogPane.setPrefHeight(120);
        dialogPane.setPrefWidth(450);
        dialogPane.setMinWidth(450);
        dialogPane.setStyle("-fx-font-size: 14px; -fx-padding: 8 24 8 24;");

        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                Map<String, models.Cliente> clientes = utils.Persistencia.lerClientes();
                clientes.remove(cliente.getNome());
                utils.Persistencia.guardarClientes(clientes);
                if (onClienteAlterado != null) {
                    onClienteAlterado.run();
                }
                clienteAlterado = true;
                fechar();
            }
        });
    }

    private void fechar() {
        ((Stage) btnSair.getScene().getWindow()).close();
    }
}