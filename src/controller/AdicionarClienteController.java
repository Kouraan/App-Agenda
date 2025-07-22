package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import models.Marcacao;
import utils.MarcacoesSemanais;

public class AdicionarClienteController {
    @FXML public TextField nomeField;
    @FXML public Label nomeErrorLabel;
    @FXML public TextField telefoneField;
    @FXML public Label telefoneErrorLabel;
    @FXML public ComboBox<String> diaSemanaCombo;
    @FXML public ComboBox<String> horaCorteCombo;
    @FXML public Label horaCorteErrorLabel;
    @FXML public Button btnSalvar;
    @FXML public Button btnCancelar;
    @FXML public Label geralErrorLabel;
    @FXML public CheckBox semanalCheck;
    @FXML public CheckBox rapidoCheck;
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
            rapidoCheck.setDisable(!newVal);
            if (!newVal) rapidoCheck.setSelected(false);
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
        rapidoCheck.setFocusTraversable(false);

        rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelar();
                    }
                });
            }
        });

        rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((obsFocus, wasFocused, isFocused) -> {
                            if (isFocused) {
                                rootVBox.requestFocus();
                            }   
                        });
                    }
                });
            }
        });
    }

    @FXML
    private void salvarCliente() {
        nomeErrorLabel.setVisible(false); nomeErrorLabel.setManaged(false);
        telefoneErrorLabel.setVisible(false); telefoneErrorLabel.setManaged(false);
        horaCorteErrorLabel.setVisible(false); horaCorteErrorLabel.setManaged(false);
        geralErrorLabel.setVisible(false); geralErrorLabel.setManaged(false);

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

        boolean semanal = semanalCheck.isSelected();

        // Campos obrigatórios
        boolean camposObrigatoriosFaltando = nome.isEmpty() || telefone.isEmpty() ||
            (semanal && (diaSemana == null || horaCorte == null));
        if (camposObrigatoriosFaltando) {
            geralErrorLabel.setText("Preencha todos os campos obrigatórios");
            geralErrorLabel.setVisible(true); geralErrorLabel.setManaged(true);
            return;
        }

        // Cliente Duplicado
        Map<String, models.Cliente> clientes = appController.getClientesMap();
        
        if (clientes.values().stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nome))) {
            nomeErrorLabel.setText("Já existe um cliente com esse nome.");
            nomeErrorLabel.setVisible(true); nomeErrorLabel.setManaged(true);
            return;
        }
        if (clientes.values().stream().anyMatch(c -> c.getNumeroTelefone().equals(telefone))) {
            telefoneErrorLabel.setText("Já existe um cliente com esse número.");
            telefoneErrorLabel.setVisible(true); telefoneErrorLabel.setManaged(true);
            return;
        }

        // Validação de formato
        if (!utils.Validation.nomeValido(nome)) {
            nomeErrorLabel.setText("Nome inválido.");
            nomeErrorLabel.setVisible(true); nomeErrorLabel.setManaged(true);
            return;
        }
        if (!utils.Validation.numeroTelefoneValido(telefone)) {
            telefoneErrorLabel.setText("Telefone inválido.");
            telefoneErrorLabel.setVisible(true); telefoneErrorLabel.setManaged(true);
            return;
        }

        // Cliente semanal
        if (semanal) {
            // Verifica se a hora já está ocupada
            boolean ocupado = clientes.values().stream().anyMatch(c -> 
                c.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL &&
                diaSemana.equalsIgnoreCase(c.getDiaSemana()) &&
                horaCorte.equals(c.getHoraCorte())
            );
            if (ocupado) {
                horaCorteErrorLabel.setText("Já existe um cliente semanal nesse horário.");
                horaCorteErrorLabel.setVisible(true); horaCorteErrorLabel.setManaged(true);
                return;
            }
        }

        // Validação final
        models.Cliente novoCliente;
        if (semanal) {
            boolean rapido = rapidoCheck.isSelected();
            novoCliente = new models.Cliente(nome, telefone, models.Cliente.TipoCliente.SEMANAL, diaSemana, horaCorte, rapido);
        } else {
            novoCliente = new models.Cliente(nome, telefone, models.Cliente.TipoCliente.NORMAL);
            novoCliente.setRapido(false);
        }
        if (!utils.Validation.clienteValido(novoCliente, clientes)) {
            geralErrorLabel.setText("Dados do cliente inválidos.");
            geralErrorLabel.setVisible(true); geralErrorLabel.setManaged(true);
            return;
        }

        // Adiciona e salva novo Cliente
        clientes.put(novoCliente.getNome(), novoCliente);
        utils.Persistencia.guardarClientes(clientes);

        if (novoCliente.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL) {
            Map<LocalDateTime, models.Marcacao> marcacoes = appController.getMarcacoesMap();
        
            List<models.Marcacao> novasMarcacoes = MarcacoesSemanais.gerarMarcacoesSemanais(
                novoCliente,
                marcacoes,
                LocalDate.now()
            );
        
            for (models.Marcacao m : novasMarcacoes) {
                marcacoes.put(m.getDataHora(), m);
            }
        
            utils.Persistencia.guardarMarcacoes(marcacoes);
        }

        utils.Logger.logClienteCriado(novoCliente.getNome());

        ((Stage) btnSalvar.getScene().getWindow()).close();
    }

    @FXML
    private void cancelar() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}