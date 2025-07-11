package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import models.Cliente;
import models.Marcacao;

public class AdicionarMarcacaoController implements Initializable {

    @FXML private Label tituloLabel;
    @FXML private TextField pesquisaClienteField;
    @FXML private ListView<String> sugestoesList;
    @FXML private CheckBox desconhecidoCheck;
    @FXML private TextField nomeDesconhecidoField;
    @FXML private TextField numeroDesconhecidoField;
    @FXML private ComboBox<Integer> duracaoCombo;
    @FXML private Label duracaoErrorLabel;
    @FXML private TextArea observacoesArea;
    @FXML private Label geralErrorLabel;
    @FXML private Button btnSalvar;
    @FXML private Button btnSair;

    private LocalDate data;
    private LocalTime hora;
    private controller.Controller appController;
    private Map<String, Cliente> clientesMap;
    private Cliente clienteSelecionado = null;

    public void setDataHora(LocalDate data, LocalTime hora) {
        this.data = data;
        this.hora = hora;
        atualizarTitulo();
    }

    public void setAppController(controller.Controller appController) {
        this.appController = appController;
        this.clientesMap = appController.getClientesMap();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicializa combos
        duracaoCombo.setItems(FXCollections.observableArrayList(15, 30, 45, 60, 75, 90));

        // Pesquisa dinâmica de clientes
        pesquisaClienteField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (desconhecidoCheck.isSelected() || newVal.trim().isEmpty()) {
                sugestoesList.setVisible(false); sugestoesList.setManaged(false);
                return;
            }
            List<String> nomes = clientesMap == null ? List.of() :
                clientesMap.values().stream()
                    .map(Cliente::getNome)
                    .filter(nome -> nome.toLowerCase().contains(newVal.toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            sugestoesList.setItems(FXCollections.observableArrayList(nomes));
            sugestoesList.setVisible(!nomes.isEmpty());
            sugestoesList.setManaged(!nomes.isEmpty());
        });

        // Seleção de sugestão
        sugestoesList.setOnMouseClicked(e -> {
            String selecionado = sugestoesList.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                pesquisaClienteField.setText(selecionado);
                sugestoesList.setVisible(false); sugestoesList.setManaged(false);
            }
        });

        // Enter na lista de sugestões
        sugestoesList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String selecionado = sugestoesList.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    pesquisaClienteField.setText(selecionado);
                    sugestoesList.setVisible(false); sugestoesList.setManaged(false);
                }
            }
        });

        // CheckBox Desconhecido
        desconhecidoCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            nomeDesconhecidoField.setDisable(!newVal);
            numeroDesconhecidoField.setDisable(!newVal);
            pesquisaClienteField.setDisable(newVal);
            sugestoesList.setVisible(false); sugestoesList.setManaged(false);
            if (!newVal) {
                nomeDesconhecidoField.clear();
                numeroDesconhecidoField.clear();
            }
        });

        // Botão Sair
        btnSair.setOnAction(e -> fechar());
        // Botão Salvar
        btnSalvar.setOnAction(e -> salvarMarcacao());

        // ESC e ENTER handlers
        btnSair.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) fechar();
                    if (event.getCode() == KeyCode.ENTER) salvarMarcacao();
                });
            }
        });

        Platform.runLater(() -> {
            pesquisaClienteField.setFocusTraversable(false);
            nomeDesconhecidoField.setFocusTraversable(false);
            numeroDesconhecidoField.setFocusTraversable(false);
            observacoesArea.setFocusTraversable(false);
            duracaoCombo.setFocusTraversable(false);
            desconhecidoCheck.setFocusTraversable(false);
            btnSalvar.setFocusTraversable(false);
            btnSair.setFocusTraversable(false);
            ((VBox)btnSalvar.getParent().getParent()).requestFocus();
        });

        String focusStyle = "-fx-focus-color: #3498db; -fx-faint-focus-color: transparent;";
        String noFocusStyle = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

        TextInputControl[] campos = {pesquisaClienteField, nomeDesconhecidoField, numeroDesconhecidoField, observacoesArea};
        for (TextInputControl campo : campos) {
            campo.setStyle(noFocusStyle);
            campo.focusedProperty().addListener((obs, oldVal, newVal) -> {
                campo.setStyle(newVal ? focusStyle : noFocusStyle);
            });
        }

        Platform.runLater(() -> {
            btnSalvar.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, evt -> {
                if (!(evt.getTarget() instanceof TextInputControl)) {
                    pesquisaClienteField.getParent().requestFocus();
                }
            });
    });
    }

    private void atualizarTitulo() {
        if (data == null || hora == null) return;
        String diaSemana = data.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("pt", "PT"));
        diaSemana = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1);
        String texto = String.format("%s dia %02d às %s", diaSemana, data.getDayOfMonth(), hora.toString());
        tituloLabel.setText(texto);
    }

    private void salvarMarcacao() {
        geralErrorLabel.setVisible(false); geralErrorLabel.setManaged(false);
        duracaoErrorLabel.setVisible(false); duracaoErrorLabel.setManaged(false);

        Cliente cliente = null;
        String nomeCliente = null;
        String numeroCliente = null;

        if (desconhecidoCheck.isSelected()) {
            nomeCliente = nomeDesconhecidoField.getText().trim();
            numeroCliente = numeroDesconhecidoField.getText().trim();
            if (nomeCliente.isEmpty()) {
                geralErrorLabel.setText("Nome do cliente é obrigatório.");
                geralErrorLabel.setVisible(true); geralErrorLabel.setManaged(true);
                return;
            }
            cliente = new Cliente(nomeCliente, numeroCliente == null ? "" : numeroCliente, Cliente.TipoCliente.DESCONHECIDO);
        } else {
            final String nomeClienteSelecionado = pesquisaClienteField.getText().trim();
            if (nomeClienteSelecionado.isEmpty()) {
                geralErrorLabel.setText("Selecione um cliente.");
                geralErrorLabel.setVisible(true); geralErrorLabel.setManaged(true);
                return;
            }
            cliente = clientesMap.values().stream()
                    .filter(c -> c.getNome().equalsIgnoreCase(nomeClienteSelecionado))
                    .findFirst().orElse(null);
            if (cliente == null) {
                geralErrorLabel.setText("Cliente não encontrado.");
                geralErrorLabel.setVisible(true); geralErrorLabel.setManaged(true);
                return;
            }
        }

        Integer duracao = duracaoCombo.getValue();
        if (duracao == null) {
            duracaoErrorLabel.setText("Selecione a duração.");
            duracaoErrorLabel.setVisible(true); duracaoErrorLabel.setManaged(true);
            return;
        }

        String observacoes = observacoesArea.getText() == null ? "" : observacoesArea.getText().trim();

        java.time.LocalDateTime dataHoraMarcacao = java.time.LocalDateTime.of(data, hora);
        Marcacao novaMarcacao = new Marcacao(dataHoraMarcacao, cliente, duracao, observacoes);

        appController.getMarcacoesMap().put(novaMarcacao.getDataHora(), novaMarcacao);
        utils.Persistencia.guardarMarcacoes(appController.getMarcacoesMap());

        fechar();
    }

    private void fechar() {
        ((Stage) btnSair.getScene().getWindow()).close();
    }
}