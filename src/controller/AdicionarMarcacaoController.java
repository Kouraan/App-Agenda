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
import java.time.LocalDateTime;
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
        Platform.runLater(() -> {
            if (appController != null && data != null && hora != null) {
                Map<java.time.LocalDateTime, Marcacao> marcacoesMap = appController.getMarcacoesMap();
                List<Integer> duracoesPossiveis = calcularDuracoesPossiveis(data, hora, marcacoesMap);
                duracaoCombo.setItems(FXCollections.observableArrayList(duracoesPossiveis));
                if (duracoesPossiveis.contains(30)) {
                    duracaoCombo.setValue(30);
                } else if (duracoesPossiveis.size() == 1 && duracoesPossiveis.contains(15)) {
                    duracaoCombo.setValue(15);
                }
            }
        });

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
        numeroDesconhecidoField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\+?\\d*") ? change : null;
        }));

        // Botão Sair
        btnSair.setOnAction(e -> fechar());
        // Botão Salvar
        btnSalvar.setOnAction(e -> salvarMarcacao());

        // ESC e ENTER handlers
        btnSair.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) fechar();
                });
            }
        });
        btnSalvar.setDefaultButton(true);

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

        String noFocusStyle = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

        TextInputControl[] campos = {pesquisaClienteField, nomeDesconhecidoField, numeroDesconhecidoField, observacoesArea};
        for (TextInputControl campo : campos) {
            campo.setStyle(campo.getStyle() + noFocusStyle);
            campo.focusedProperty().addListener((obs, oldVal, newVal) -> {
                String baseStyle = campo.getStyle().replace(noFocusStyle, "");
                campo.setStyle(baseStyle + noFocusStyle);
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

        Map<java.time.LocalDateTime, Marcacao> marcacoesMap = appController.getMarcacoesMap();
        int minutosRestantes = duracao;
        LocalDateTime blocoAtual = dataHoraMarcacao;

        while (minutosRestantes > 0) {
            Marcacao mExistente = marcacoesMap.get(blocoAtual);
            int blocoDuracao = 15;

            // Se existe marcação de 15min neste bloco, salta para o próximo bloco
            if (mExistente != null && mExistente.getDuracao() == 15) {
                blocoAtual = blocoAtual.plusMinutes(15);
                continue;
            }

            // Se o próximo bloco está ocupado por uma marcação de 15min, só pode fazer 15min neste bloco
            Marcacao proximo = marcacoesMap.get(blocoAtual.plusMinutes(15));
            if (proximo != null && proximo.getDuracao() == 15) {
                blocoDuracao = 15;
            } else if (minutosRestantes >= 30) {
                blocoDuracao = 30;
            } else if (minutosRestantes >= 15) {
                blocoDuracao = 15;
            }

            // Não criar marcação se já existe uma de 30min ou mais neste bloco
            Marcacao jaExiste = marcacoesMap.get(blocoAtual);
            if (jaExiste != null && jaExiste.getDuracao() >= 30) {
                blocoAtual = blocoAtual.plusMinutes(blocoDuracao);
                continue;
            }

            Marcacao novaMarcacao = new Marcacao(blocoAtual, cliente, blocoDuracao, observacoes);
            marcacoesMap.put(novaMarcacao.getDataHora(), novaMarcacao);

            utils.Logger.logMarcacaoCriada(novaMarcacao);

            minutosRestantes -= blocoDuracao;
            blocoAtual = blocoAtual.plusMinutes(blocoDuracao);
        }

        utils.Persistencia.guardarMarcacoes(marcacoesMap);

        fechar();
    }

    private void fechar() {
        ((Stage) btnSair.getScene().getWindow()).close();
    }

    private List<Integer> calcularDuracoesPossiveis(LocalDate data, LocalTime hora, Map<java.time.LocalDateTime, Marcacao> marcacoesMap) {
        List<Integer> opcoes = Arrays.asList(15, 30, 45, 60, 75, 90);
        List<Integer> disponiveis = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(data, hora);

        // Hora máxima
        LocalTime horaMaxima = LocalTime.of(21,30);

        // Se não for múltiplo de 30 minutos, só permite 15 minutos
        if (hora.getMinute() == 45 || hora.getMinute() == 15) {
            Marcacao m = marcacoesMap.get(inicio);
            if (m == null) {
                disponiveis.add(15);
            }
            return disponiveis;
        }

        // Lógica normal: só permite durações que não colidam com marcações existentes
        outer:
        for (int dur : opcoes) {
            LocalTime fim = hora.plusMinutes(dur);
            if (fim.isAfter(horaMaxima)) continue;
            
            LocalDateTime bloco = inicio;
            int blocos = dur / 15;
            for (int i = 0; i < blocos; i++) {
                Marcacao m = marcacoesMap.get(bloco);
                if (m != null) {
                    // Só permite se for de 15min e for exatamente o último bloco do intervalo
                    if (!(m.getDuracao() == 15 && i == blocos - 1)) {
                        continue outer;
                    }
                }
                if (i == 1 && dur >= 30) {
                    Marcacao proximo = marcacoesMap.get(inicio.plusMinutes(15));
                    if (proximo != null && proximo.getDuracao() == 15) {
                        continue outer;
                    }
                }
                bloco = bloco.plusMinutes(15);
            }
            disponiveis.add(dur);
        }
        return disponiveis;
    }
}