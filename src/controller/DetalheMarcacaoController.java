package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.util.Locale;
import java.util.Map;
import models.Marcacao;

public class DetalheMarcacaoController {
    @FXML private Label tituloLabel;
    @FXML private TextField nomeField;
    @FXML private TextField telefoneField;
    @FXML private TextField duracaoField;
    @FXML private TextArea observacoesArea;
    @FXML private Button btnApagar;
    @FXML private Button btnSalvar;
    @FXML private ComboBox<String> diaCombo;
    @FXML private ComboBox<String> horaCombo;

    private Marcacao marcacao;
    private String observacoesOriginais;
    private controller.Controller appController;
    private static final String[] DIAS_SEMANA = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};


    public void setMarcacao(Marcacao marcacao) {
        this.marcacao = marcacao;
        atualizarDetalhes();
    }

    public void setAppController(controller.Controller appController) {
        this.appController = appController;
    }

    @FXML
    public void initialize() {
        btnSalvar.setDisable(true);

        observacoesArea.textProperty().addListener((obs, oldVal, newVal) -> {
            btnSalvar.setDisable(newVal.equals(observacoesOriginais) && !horaAlterada());
        });

        horaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            btnSalvar.setDisable(!horaAlterada() && observacoesArea.getText().equals(observacoesOriginais));
        });

        diaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            atualizarHorasDisponiveis();
            horaCombo.setValue(null);
            btnSalvar.setDisable(!horaAlterada() && observacoesArea.getText().equals(observacoesOriginais));
        });

        btnSalvar.setOnAction(e -> salvarObservacoesOuHora());
        btnApagar.setOnAction(e -> apagarMarcacao());

        Platform.runLater(() -> {
            btnSalvar.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) fechar();
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !btnSalvar.isDisabled()) salvarObservacoesOuHora();
            });
            btnSalvar.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, evt -> {
                if (!(evt.getTarget() instanceof TextInputControl)) {
                    observacoesArea.getParent().requestFocus();
                }
            });
        });
    }

    private void atualizarDetalhes() {
        if (marcacao == null) return;

        var dataHora = marcacao.getDataHora();
        String diaSemana = DIAS_SEMANA[dataHora.getDayOfWeek().getValue() - 1];
        String hora = dataHora.toLocalTime().toString();
        String titulo = String.format("%s dia %02d às %s", diaSemana, dataHora.getDayOfMonth(), hora);
        tituloLabel.setText(titulo);

        nomeField.setText(marcacao.getCliente().getNome());
        telefoneField.setText(marcacao.getCliente().getNumeroTelefone());
        duracaoField.setText(marcacao.getDuracao() + " minutos");
        observacoesOriginais = marcacao.getObservacoes() == null ? "" : marcacao.getObservacoes();
        observacoesArea.setText(observacoesOriginais);

        diaCombo.getItems().setAll(DIAS_SEMANA);
        diaCombo.setValue(diaSemana);

        Platform.runLater(() -> {
            atualizarHorasDisponiveis();
            horaCombo.setValue(hora);
        });

        btnSalvar.setDisable(true);
    }

    private void atualizarHorasDisponiveis() {
        if (marcacao == null || appController == null) return;
        String diaSelecionado = diaCombo.getValue();
        if (diaSelecionado == null) return;

        int diaSemanaIdx = java.util.Arrays.asList(DIAS_SEMANA).indexOf(diaSelecionado);
        java.time.LocalDate novaData = marcacao.getDataHora().toLocalDate().with(java.time.DayOfWeek.of(diaSemanaIdx + 1));
        int duracao = marcacao.getDuracao();

        Map<java.time.LocalDateTime, Marcacao> marcacoesMap = appController.getMarcacoesMap();

        java.util.List<String> horasDisponiveis = new java.util.ArrayList<>();
        for (int h = 7; h <= 21; h++) {
            for (int m = 0; m < 60; m += 30) {
                java.time.LocalTime hora = java.time.LocalTime.of(h, m);
                java.time.LocalDateTime dt = novaData.atTime(hora);

                // Só adiciona se não houver conflito com outras marcações (exceto a própria)
                boolean livre = true;
                for (int i = 0; i < duracao; i += 15) {
                    Marcacao mMarc = marcacoesMap.get(dt.plusMinutes(i));
                    if (mMarc != null && mMarc != marcacao) {
                        livre = false;
                        break;
                    }
                }
                if (livre) {
                    horasDisponiveis.add(hora.toString());
                }
            }
        }
        horaCombo.getItems().setAll(horasDisponiveis);
    }

    private boolean horaAlterada() {
        if (marcacao == null) return false;
        String horaAtual = marcacao.getDataHora().toLocalTime().toString();
        String horaComboVal = horaCombo.getValue();
        String diaAtual = DIAS_SEMANA[marcacao.getDataHora().getDayOfWeek().getValue() - 1];
        String diaComboVal = diaCombo.getValue();
        return (horaComboVal != null && !horaAtual.equals(horaComboVal)) || (diaComboVal != null && !diaAtual.equals(diaComboVal));
    }

    private void salvarObservacoesOuHora() {
        if (marcacao == null || appController == null) return;
        boolean alterouObs = !observacoesArea.getText().equals(observacoesOriginais);
        boolean alterouHora = horaAlterada();

        Map<java.time.LocalDateTime, Marcacao> marcacoesMap = appController.getMarcacoesMap();

        if (alterouHora) {
            marcacoesMap.remove(marcacao.getDataHora());

            int diaSemanaIdx = java.util.Arrays.asList(DIAS_SEMANA).indexOf(diaCombo.getValue());
            java.time.LocalDate novaData = marcacao.getDataHora().toLocalDate().with(java.time.DayOfWeek.of(diaSemanaIdx + 1));
            java.time.LocalTime novaHora = java.time.LocalTime.parse(horaCombo.getValue());
            java.time.LocalDateTime novaDataHora = novaData.atTime(novaHora);

            marcacao.setDataHora(novaDataHora);
        }

        if (alterouObs) {
            marcacao.setObservacoes(observacoesArea.getText());
            observacoesOriginais = observacoesArea.getText();
        }

        marcacoesMap.put(marcacao.getDataHora(), marcacao);
        utils.Persistencia.guardarMarcacoes(marcacoesMap);

        try {
            appController.mostrarPaginaPrincipal();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        btnSalvar.setDisable(true);
        fechar();
    }

    private void apagarMarcacao() {
        if (marcacao == null || appController == null) return;
        Map<java.time.LocalDateTime, Marcacao> marcacoesMap = appController.getMarcacoesMap();
        marcacoesMap.remove(marcacao.getDataHora());
        utils.Persistencia.guardarMarcacoes(marcacoesMap);

        try {
            appController.mostrarPaginaPrincipal();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        fechar();
    }

    private void fechar() {
        Stage stage = (Stage) btnApagar.getScene().getWindow();
        stage.close();
    }
}