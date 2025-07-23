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
    @FXML private HBox botoesBox;
    @FXML private Label alterarHoraLabel;
    @FXML private Label diaLabel;
    @FXML private Label horaLabel;

    private Marcacao marcacao;
    private String observacoesOriginais;
    private Button btnFaltou;
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
    
        boolean passou = dataHora.isBefore(java.time.LocalDateTime.now());
    
        diaCombo.setVisible(!passou);
        horaCombo.setVisible(!passou);
        alterarHoraLabel.setVisible(!passou);
        diaLabel.setVisible(!passou);
        horaLabel.setVisible(!passou);
    
        btnApagar.setVisible(!passou);
    
        if (passou) {
            if (btnFaltou == null) {
                btnFaltou = new Button("Faltou");
                btnFaltou.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-pref-width: 90px; -fx-pref-height: 36px;");
                btnFaltou.setOnAction(e -> marcarFalta());
            }
            if (marcacao.isFalta()) {
                btnFaltou.setDisable(true);
            } else {
                btnFaltou.setDisable(false);
                btnFaltou.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-pref-width: 90px; -fx-pref-height: 36px;");
            }
            if (!botoesBox.getChildren().contains(btnFaltou)) {
                int idx = botoesBox.getChildren().indexOf(btnApagar);
                if (idx >= 0) {
                    botoesBox.getChildren().remove(btnApagar);
                    botoesBox.getChildren().add(idx, btnFaltou);
                } else {
                    botoesBox.getChildren().add(btnFaltou);
                }
            }
        }
    
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
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalTime agora = java.time.LocalTime.now();

        if (novaData.isBefore(hoje)) {
            horaCombo.getItems().clear();
            return;
        }

        if (duracao == 15) {
            // Blocos de 15 em 15 minutos
            for (int h = 7; h <= 21; h++) {
                for (int m = 0; m < 60; m += 15) {
                    java.time.LocalTime hora = java.time.LocalTime.of(h, m);

                    if (hora.plusMinutes(15).isAfter(java.time.LocalTime.of(21, 30))) continue;

                    java.time.LocalDateTime dt = novaData.atTime(hora);
    
                    boolean livre = true;
    
                    Marcacao mMarc = marcacoesMap.get(dt);
                    if (mMarc != null && mMarc != marcacao) {
                        // Se já existe marcação de 30min ou 15min, não pode
                        livre = false;
                    } else if (m == 15 || m == 45) {
                        // Só permite xx:15 ou xx:45 se o bloco anterior for ocupado por 15min e este estiver livre
                        java.time.LocalDateTime blocoAnterior = dt.minusMinutes(15);
                        Marcacao mMarcAnterior = marcacoesMap.get(blocoAnterior);
                        if (mMarcAnterior == null || mMarcAnterior == marcacao || mMarcAnterior.getDuracao() != 15) {
                            livre = false;
                        }
                    }

                    // Não permite horas passadas no dia de hoje
                    boolean horaJaPassou = novaData.isBefore(hoje) ||
                        (novaData.isEqual(hoje) && hora.isBefore(agora));

                    if (livre && !horaJaPassou) {
                        horasDisponiveis.add(hora.toString());
                    }
                }
            }
        } else {
            // Para marcações de 30 minutos, lógica normal
            for (int h = 7; h <= 21; h++) {
                for (int m = 0; m < 60; m += 30) {
                    java.time.LocalTime hora = java.time.LocalTime.of(h, m);

                    if (hora.plusMinutes(30).isAfter(java.time.LocalTime.of(21, 30))) continue;

                    java.time.LocalDateTime dt = novaData.atTime(hora);
    
                    boolean livre = true;
                    for (int i = 0; i < duracao; i += 15) {
                        Marcacao mMarcAux = marcacoesMap.get(dt.plusMinutes(i));
                        if (mMarcAux != null && mMarcAux != marcacao) {
                            livre = false;
                            break;
                        }
                    }

                    // Não permite horas passadas no dia de hoje
                    boolean horaJaPassou = novaData.isEqual(hoje) && hora.isBefore(agora);

                    if (livre && !horaJaPassou) {
                        horasDisponiveis.add(hora.toString());
                    }
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

        String dataHoraAntiga = marcacao.getDataHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        if (alterouHora) {
            marcacoesMap.remove(marcacao.getDataHora());

            int diaSemanaIdx = java.util.Arrays.asList(DIAS_SEMANA).indexOf(diaCombo.getValue());
            java.time.LocalDate novaData = marcacao.getDataHora().toLocalDate().with(java.time.DayOfWeek.of(diaSemanaIdx + 1));
            java.time.LocalTime novaHora = java.time.LocalTime.parse(horaCombo.getValue());
            java.time.LocalDateTime novaDataHora = novaData.atTime(novaHora);

            marcacao.setDataHora(novaDataHora);

            // LOG de alteração de data/hora
            String dataHoraNova = novaDataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            utils.Logger.logMarcacaoDataHoraAlterada(marcacao, dataHoraAntiga, dataHoraNova);
        }

        if (alterouObs) {
            marcacao.setObservacoes(observacoesArea.getText());

            // LOG de alteração de observações
            utils.Logger.logMarcacaoObsAlterada(marcacao, observacoesArea.getText());

            observacoesOriginais = observacoesArea.getText();
        }

        marcacoesMap.put(marcacao.getDataHora(), marcacao);
        utils.Persistencia.guardarMarcacoes(marcacoesMap);

        try {
            if (appController.getPaginaPrincipalController() != null) {
                appController.getPaginaPrincipalController().atualizarCalendario();
            }
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

        utils.Logger.logMarcacaoApagada(marcacao);

        try {
            if (appController.getPaginaPrincipalController() != null) {
                appController.getPaginaPrincipalController().atualizarCalendario();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        fechar();
    }

    private void marcarFalta() {
        if (marcacao == null) return;
        marcacao.setFalta(true);

        // LOG de falta à marcação
        utils.Logger.logMarcacaoFalta(marcacao);

        Map<java.time.LocalDateTime, Marcacao> marcacoesMap = appController.getMarcacoesMap();
        marcacoesMap.put(marcacao.getDataHora(), marcacao);

        if (marcacao.getCliente().isTemporario()) {
            appController.getClientesQueries().addFaltas(marcacao.getCliente().getNome());
            utils.Persistencia.guardarMarcacoes(marcacoesMap);
            utils.Persistencia.guardarClientes(appController.getClientesMap());
        } else {
            utils.Persistencia.guardarMarcacoes(marcacoesMap);
        }

        if (appController.getPaginaPrincipalController() != null) {
            appController.getPaginaPrincipalController().atualizarCalendario();
        }
        fechar();
    }

    private void fechar() {
        Stage stage = (Stage) tituloLabel.getScene().getWindow();
        stage.close();
    }
}