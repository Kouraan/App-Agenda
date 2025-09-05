package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.effect.GaussianBlur;

import models.*;
import utils.Feriados;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Map;

public class PaginaPrincipalController implements Initializable {
    
    @FXML private Label userLabel;
    @FXML private Label semanaLabel;
    @FXML private Button todayBtn;
    @FXML private Button semanaAnteriorBtn;
    @FXML private Button proximaSemanaBtn;
    @FXML private Button logoutBtn;
    @FXML private GridPane calendarioGrid;
    @FXML private ToggleButton semanaToggle;
    @FXML private ToggleButton mesToggle;
    @FXML private ToggleButton diaToggle;
    @FXML private ToggleButton calendarioToggle;
    @FXML private ToggleButton clientesToggle;
    @FXML private Label relogioLabel;
    @FXML private VBox areaCentral;
    @FXML private StackPane areaClientes;
    @FXML private VBox clientesContent;
    @FXML private ScrollPane scrollPaneCalendario;
    @FXML private VBox anotacoesBox;
    @FXML private BorderPane rootPane;
    @FXML private TextArea anotacoesArea;
    @FXML private Button blurToggleBtn;
    @FXML private VBox caixaClientesPendentes;
    
    private Utilizador utilizador;
    private Controller appController;
    private LocalDate semanaAtual;
    private Map<LocalTime, Pane> celulasDia = new HashMap<>();
    private Map<LocalTime, Map<Integer, Pane>> celulasSemana = new HashMap<>();
    
    // Horários de funcionamento
    private static final LocalTime HORA_ABERTURA = LocalTime.of(7, 0);
    private static final LocalTime HORA_FECHO = LocalTime.of(21, 0);
    private static final int INTERVALO_MINUTOS = 30;

    private enum ModoVisualizacao { SEMANA, MES, DIA }
    private ModoVisualizacao modoAtual = ModoVisualizacao.SEMANA;
    private LocalDate diaSelecionado = LocalDate.now();
    private boolean anotacoesBlurred = false;
    
    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
        if (userLabel != null) {
            userLabel.setText("Bem-vindo, " + utilizador.getNome());
        }
    }
    
    public void setAppController(Controller appController) {
        this.appController = appController;
        atualizarMarcacoesSemanaisSeNecessario();
        atualizarBoxClientesPendentes();
        atualizarCalendario();
    }

    public Controller getAppController() {
        return appController;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        semanaAtual = LocalDate.now().with(DayOfWeek.MONDAY);
        
        ToggleGroup group = new ToggleGroup();
        semanaToggle.setToggleGroup(group);
        mesToggle.setToggleGroup(group);
        diaToggle.setToggleGroup(group);
        semanaToggle.setSelected(true);

        ToggleGroup grupoLateral = new ToggleGroup();
        calendarioToggle.setToggleGroup(grupoLateral);
        clientesToggle.setToggleGroup(grupoLateral);
        calendarioToggle.setSelected(true);

        // Remover o foco visual dos botoes
        todayBtn.setFocusTraversable(false);
        semanaAnteriorBtn.setFocusTraversable(false);
        proximaSemanaBtn.setFocusTraversable(false);
        logoutBtn.setFocusTraversable(false);
        semanaToggle.setFocusTraversable(false);
        mesToggle.setFocusTraversable(false);
        diaToggle.setFocusTraversable(false);
        calendarioToggle.setFocusTraversable(false);
        clientesToggle.setFocusTraversable(false);
        calendarioGrid.setFocusTraversable(false);
        scrollPaneCalendario.setFocusTraversable(false);

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == semanaToggle) {
                modoAtual = ModoVisualizacao.SEMANA;
                if (oldToggle == mesToggle) {
                    // Se não está no mês atual, mostrar a primeira semana do mês visível
                    LocalDate primeiroDiaMes = semanaAtual.withDayOfMonth(1);
                    semanaAtual = primeiroDiaMes.with(DayOfWeek.MONDAY);
                    diaSelecionado = primeiroDiaMes;
                } else if (oldToggle == diaToggle) {
                    // Se o dia selecionado não pertence à semana atual, mostrar a semana correspondente ao dia selecionado
                    semanaAtual = diaSelecionado.with(DayOfWeek.MONDAY);
                }
                atualizarCalendario();
                atualizarEstiloTogglesModo();
            } else if (newToggle == mesToggle) {
            modoAtual = ModoVisualizacao.MES;
            if (oldToggle == semanaToggle) {
                // Mostra o mês do primeiro dia da semana visível
                LocalDate primeiroDiaSemana = semanaAtual;
                LocalDate primeiroDiaMes = primeiroDiaSemana.withDayOfMonth(1);
                // Se a semana começa num mês diferente, mostra esse mês
                if (primeiroDiaSemana.getMonth() != semanaAtual.getMonth()) {
                    primeiroDiaMes = primeiroDiaSemana.withDayOfMonth(1);
                }
                semanaAtual = primeiroDiaSemana.withDayOfMonth(1);
                diaSelecionado = primeiroDiaSemana;
            } else if (oldToggle == diaToggle) {
                // Mostra o mês do dia selecionado
                LocalDate primeiroDiaMes = diaSelecionado.withDayOfMonth(1);
                semanaAtual = primeiroDiaMes;
            }
            atualizarCalendario();
            atualizarEstiloTogglesModo();
            } else if (newToggle == diaToggle) {
                modoAtual = ModoVisualizacao.DIA;
                if (oldToggle == mesToggle) {
                    semanaAtual = diaSelecionado.with(DayOfWeek.MONDAY);
                } else if (oldToggle == semanaToggle) {
                    // Se o dia selecionado não pertence à semana atual, mostrar o primeiro dia da semana
                    if (!diaSelecionado.with(DayOfWeek.MONDAY).equals(semanaAtual)) {
                        diaSelecionado = semanaAtual;
                    }
                }
                atualizarCalendario();
                atualizarEstiloTogglesModo();
            }
        });

        grupoLateral.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            atualizarEstiloTogglesLaterais();
        });
        
        atualizarCalendario();
        atualizarEstiloTogglesModo();
        atualizarEstiloTogglesLaterais();

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0), e ->{
                atualizarRelogio();
                if (modoAtual == ModoVisualizacao.DIA) {
                    destacarBlocoAtual();
                } else if (modoAtual == ModoVisualizacao.SEMANA) {
                    destacarBlocoAtualSemana();
                }
            }),
            new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        if (anotacoesArea != null) {
            anotacoesArea.setText(utils.Persistencia.lerAnotacoes());
            anotacoesArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    utils.Persistencia.guardarAnotacoes(anotacoesArea.getText());
                }
            });
        }

        // Remover foco do TextArea ao clicar em qualquer lado do ecrã
        if (rootPane != null && anotacoesArea != null) {
            rootPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getTarget() != anotacoesArea && anotacoesArea.isFocused()) {
                    rootPane.requestFocus(); // tira o foco do TextArea
                }
            });
        }

        if (blurToggleBtn != null && anotacoesArea != null) {
            blurToggleBtn.setOnAction(e -> {
                String texto = anotacoesArea.getText();
                if (texto == null || texto.trim().isEmpty()) {
                    return;
                }
                anotacoesBlurred = !anotacoesBlurred;
                if (anotacoesBlurred) {
                    anotacoesArea.setEffect(new GaussianBlur(10));
                    blurToggleBtn.setText("👁");
                    blurToggleBtn.setStyle("-fx-background-radius: 12; -fx-background-color: rgb(60, 60, 60); -fx-font-size: 15px; -fx-padding: 0; -fx-text-fill: white;");
                    anotacoesArea.setEditable(false);
                } else {
                    anotacoesArea.setEffect(null);
                    blurToggleBtn.setText("⛔");
                    blurToggleBtn.setStyle("-fx-background-radius: 12; -fx-background-color: rgb(60, 60, 60); -fx-font-size: 15px; -fx-padding: 0; -fx-text-fill: white;");
                    anotacoesArea.setEditable(true);
                }
            });
        }

        atualizarBoxClientesPendentes();
    }
    
    @FXML
    private void semanaAnterior() {
        switch (modoAtual) {
            case SEMANA:
                semanaAtual = semanaAtual.minusWeeks(1);
                break;
            case MES:
                semanaAtual = semanaAtual.minusMonths(1);
                break;
            case DIA:
                diaSelecionado = diaSelecionado.minusDays(1);
                semanaAtual = diaSelecionado.with(DayOfWeek.MONDAY);
                break;
        }
        atualizarCalendario();
    }
    
    @FXML
    private void proximaSemana() {
        switch (modoAtual) {
            case SEMANA:
                semanaAtual = semanaAtual.plusWeeks(1);
                break;
            case MES:
                semanaAtual = semanaAtual.plusMonths(1);
                break;
            case DIA:
                diaSelecionado = diaSelecionado.plusDays(1);
                semanaAtual = diaSelecionado.with(DayOfWeek.MONDAY);
                break;
        }
        atualizarCalendario();
    }
    
    @FXML
    private void handleLogout() {
        // Guardar anotações antes de sair
        if (anotacoesArea != null) {
            utils.Persistencia.guardarAnotacoes(anotacoesArea.getText());
        }
        try {
            if (appController != null) {
                appController.mostrarLogin();
                utils.Logger.logLogout(utilizador.getNome());
            }
        } catch (Exception e) {
            System.err.println("Erro ao fazer logout: " + e.getMessage());
        }
    }

    @FXML
    private void handleToday() {
        LocalDate hoje = LocalDate.now();
        switch (modoAtual) {
            case SEMANA:
                semanaAtual = hoje.with(DayOfWeek.MONDAY);
                diaSelecionado = hoje;
                break;
            case MES:
                semanaAtual = hoje.withDayOfMonth(1);
                diaSelecionado = hoje;
                break;
            case DIA:
                diaSelecionado = hoje;
                semanaAtual = hoje.with(DayOfWeek.MONDAY);
                break;
        }
        atualizarCalendario();
    }

    @FXML
    private void mostrarCalendario() {
        calendarioToggle.setSelected(true);
        clientesToggle.setSelected(false);
        areaCentral.setVisible(true);
        areaClientes.setVisible(false);
        atualizarCalendario();
        atualizarEstiloTogglesLaterais();
    }

    @FXML
    public void mostrarClientes() {
        calendarioToggle.setSelected(false);
        clientesToggle.setSelected(true);
        areaCentral.setVisible(false);
        areaClientes.setVisible(true);
        atualizarEstiloTogglesLaterais();

        clientesContent.getChildren().clear();

        if (appController != null && (appController.getClientesMap() == null || appController.getClientesMap().isEmpty())) {
            Label msg = new Label("Não tem nenhum cliente salvo, deseja adicionar um?");
            msg.setStyle(
                "-fx-font-size: 22px; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: transparent;"
            );
            msg.setAlignment(Pos.CENTER);

            Button adicionarBtn = new Button("Adicionar");
            adicionarBtn.setStyle(
                "-fx-font-size: 18px; " +
                "-fx-background-color: rgb(43, 40, 40); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 0; " +
                "-fx-padding: 10 32 10 32;"
            );
            
            adicionarBtn.setOnAction(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdicionarCliente.fxml"));
                    Parent root = loader.load();
                    AdicionarClienteController adicionarClienteController = loader.getController();
                    adicionarClienteController.setAppController(appController);

                    Stage stage = new Stage();
                    stage.setTitle("Adicionar Cliente");
                    stage.setScene(new Scene(root));
                    stage.initOwner(adicionarBtn.getScene().getWindow());
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.setResizable(false);

                    double largura = areaCentral.getWidth() * 0.25;
                    double altura = areaCentral.getHeight();
                    if (largura < 320) largura = 320;
                    stage.setWidth(largura);
                    stage.setHeight(altura);
                    stage.setX(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinX() + (areaCentral.getWidth() - largura) / 2);
                    stage.setY(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinY());

                    stage.showAndWait();

                    mostrarClientes();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            VBox box = new VBox(32, msg, adicionarBtn);
            box.setAlignment(Pos.CENTER);
            box.setStyle(
                "-fx-background-color: rgb(15, 14, 14); " +
                "-fx-background-radius: 24; " +
                "-fx-border-radius: 24; " +
                "-fx-padding: 32;"
            );

            clientesContent.setAlignment(Pos.CENTER);
            clientesContent.getChildren().add(box);
        } else {
            HBox barraTopo = new HBox(10);
            barraTopo.setAlignment(Pos.TOP_LEFT);
            barraTopo.setPadding(new Insets(10, 0, 15, 0));

            double alturaBarra = 36;
            double larguraBotao = 36;

            TextField pesquisaField = new TextField();
            pesquisaField.setPromptText("Pesquisar cliente...");
            pesquisaField.setPrefHeight(alturaBarra);
            pesquisaField.setStyle(
                "-fx-background-color: rgb(43, 40, 40); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 0; " +
                "-fx-padding: 0 16 0 16;"
                );
            pesquisaField.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ENTER:
                        pesquisaField.getParent().requestFocus();
                        break;
                }
            });

            HBox.setHgrow(pesquisaField, Priority.ALWAYS);

            Button btnAdicionar = new Button("+");
            btnAdicionar.setPrefWidth(larguraBotao);
            btnAdicionar.setPrefHeight(alturaBarra);
            btnAdicionar.setStyle(
                "-fx-background-color: rgb(43, 40, 40); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 18px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 0;"
            );
            btnAdicionar.setOnAction(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdicionarCliente.fxml"));
                    Parent root = loader.load();
                    AdicionarClienteController adicionarClienteController = loader.getController();
                    adicionarClienteController.setAppController(appController);

                    Stage stage = new Stage();
                    stage.setTitle("Adicionar Cliente");
                    stage.setScene(new Scene(root));
                    stage.initOwner(btnAdicionar.getScene().getWindow());
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.setResizable(false);

                    double largura = areaCentral.getWidth() * 0.25;
                    double altura = areaCentral.getHeight();
                    if (largura < 320) largura = 320;
                    stage.setWidth(largura);
                    stage.setHeight(altura);
                    stage.setX(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinX() + (areaCentral.getWidth() - largura) / 2);
                    stage.setY(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinY());

                    stage.showAndWait();

                    mostrarClientes();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            pesquisaField.setFocusTraversable(false);
            btnAdicionar.setFocusTraversable(false);

            barraTopo.getChildren().addAll(pesquisaField, btnAdicionar);
           
            HBox.setHgrow(barraTopo, Priority.ALWAYS);

            VBox layout = new VBox(0, barraTopo);
            layout.setStyle("-fx-background-color: rgb(15, 14, 14); -fx-background-radius: 24; -fx-padding: 10;");
            layout.setOnMouseClicked(event -> {
                if (event.getTarget() != pesquisaField) {
                    layout.requestFocus();
                }
            });
            layout.setMaxWidth(Double.MAX_VALUE);
            layout.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(layout, Priority.ALWAYS);

            clientesContent.setAlignment(Pos.TOP_LEFT);
            clientesContent.getChildren().add(layout);

            pesquisaField.textProperty().addListener((obs, oldText, newText) -> {
                atualizarTabelaClientes(layout, newText);
            });

            atualizarTabelaClientes(layout, "");
        }
    }

    private void atualizarTabelaClientes(VBox layout, String filtro) {
        if (layout.getChildren().size() > 1) {
            layout.getChildren().remove(1);
        }

        GridPane tabela = new GridPane();
        tabela.setHgap(8);
        tabela.setVgap(8);
        tabela.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-padding: 10; -fx-background-radius: 12;");
        tabela.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < 6; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setMinWidth(100.0 / 6);
            tabela.getColumnConstraints().add(col);
        }

        String[] cabecalho = {"Nome", "Telefone", "Tipo", "Faltas", "Dia Semana", "Hora Corte"};
        for (int i = 0; i < cabecalho.length; i++) {
            Label th = new Label(cabecalho[i]);
            th.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: rgba(197, 130, 63, 0.86); -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 0 8 0; -fx-border-width: 0;");
            th.setMaxWidth(Double.MAX_VALUE);
            th.setAlignment(Pos.CENTER);
            tabela.add(th, i, 0);
        }

        java.util.List<models.Cliente> lista = new java.util.ArrayList<>(appController.getClientesMap().values());
        lista.sort(java.util.Comparator.comparing(models.Cliente::getNome, String.CASE_INSENSITIVE_ORDER));

        String filtroLower = filtro.toLowerCase();
        int row = 1;
        for (models.Cliente c : lista) {
            if (c.getNome().toLowerCase().contains(filtroLower) || c.getNumeroTelefone().contains(filtroLower)) {
                for (int col = 0; col < cabecalho.length; col++) {
                    String texto;
                    switch(col) {
                        case 0: texto = c.getNome(); break;
                        case 1: texto = c.getNumeroTelefone(); break;
                        case 2: texto = c.getTipoCliente().toString(); break;
                        case 3: texto = String.valueOf(c.getFaltas()); break;
                        case 4: texto = c.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL ? c.getDiaSemana() : "—"; break;
                        case 5: texto = c.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL ? c.getHoraCorte() : "—"; break;
                        default: texto = "—";
                    }
                    Label cell = new Label(texto == null ? "—" : texto);
                    cell.setStyle(
                        "-fx-font-size: 14px; " +
                        "-fx-background-color: rgb(60, 60, 60); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-border-color: rgba(197, 130, 63, 0.86); " +
                        "-fx-border-width: 1; " +
                        "-fx-padding: 8 0 8 0;"
                    );
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setAlignment(Pos.CENTER);
                    if (col == 0) {
                        cell.setStyle(cell.getStyle() + "; -fx-cursor: hand;");
                        cell.setOnMouseClicked(event -> abrirDetalhesCliente(c));
                    }
                    tabela.add(cell, col, row);
                }
                row++;
            }
        }

        StackPane tabelaFundo = new StackPane(tabela);
        tabelaFundo.setStyle("-fx-background-color: rgb(15, 14, 14); -fx-background-radius: 0;");

        ScrollPane tabelaScroll = new ScrollPane(tabelaFundo);
        tabelaScroll.setFitToWidth(true);
        tabelaScroll.setFitToHeight(true);
        tabelaScroll.setFocusTraversable(false);
        tabelaScroll.setStyle(
            "-fx-background-color: rgb(15, 14, 14);" +
            "-fx-background-insets: 0;" +
            "-fx-background-radius: 12;" +
            "-fx-border-width: 0;" +
            "-fx-padding: 0;" +
            "-fx-hbar-policy: never;" +
            "-fx-vbar-policy: never;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );
        VBox.setVgrow(tabelaScroll, Priority.ALWAYS);

        layout.getChildren().add(tabelaScroll);
    }

    
    public void atualizarCalendario() {
        // Limpar grid anterior
        calendarioGrid.getChildren().clear();
        calendarioGrid.getRowConstraints().clear();
        calendarioGrid.getColumnConstraints().clear();
        
        switch (modoAtual) {
            case SEMANA:
                atualizarSemana();
                break;
            case MES:
                atualizarMes();
                break;
            case DIA:
                atualizarDia();
                break;
        }
    }

    private void atualizarSemana() {
        LocalDate fimSemana = semanaAtual.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd", new Locale("pt", "PT"));
        String inicio = semanaAtual.format(formatter);
        String fim = fimSemana.format(formatter);
        semanaLabel.setText(inicio + " - " + fim);

        // Limpa colunas e linhas antigas
        calendarioGrid.getColumnConstraints().clear();
        calendarioGrid.getRowConstraints().clear();

        // Configurar colunas (8 colunas: 1 para horas + 7 para dias)
        for (int i = 0; i < 8; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            if (i == 0) {
                colConstraints.setPrefWidth(80); // Coluna das horas
                colConstraints.setMinWidth(80);
                colConstraints.setMaxWidth(80);
                colConstraints.setHgrow(Priority.NEVER);
            } else {
                colConstraints.setHgrow(Priority.ALWAYS); // Permite crescer
                colConstraints.setPrefWidth(110);
                colConstraints.setMinWidth(110);
                colConstraints.setMaxWidth(Double.MAX_VALUE);
            }
            calendarioGrid.getColumnConstraints().add(colConstraints);
        }

        // Limpar cabeçalho dos dias
        criarCabecalhoDias();

        // Criar grade de horários
        criarGradeHorarios();

        // Força o GridPane a ocupar todo o espaço disponível
        calendarioGrid.setMaxWidth(Double.MAX_VALUE);
        calendarioGrid.setMaxHeight(Double.MAX_VALUE);
        GridPane.setHgrow(calendarioGrid, Priority.ALWAYS);
        GridPane.setVgrow(calendarioGrid, Priority.ALWAYS);

        atualizarCabecalho();
    }

    private void atualizarMes() {
        calendarioGrid.getColumnConstraints().clear();
        calendarioGrid.getRowConstraints().clear();
        calendarioGrid.getChildren().clear();

        calendarioGrid.setHgap(4);
        calendarioGrid.setVgap(4);
        calendarioGrid.setPadding(new Insets(12, 0, 0, 0));

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setPrefWidth(110);
            calendarioGrid.getColumnConstraints().add(col);
        }
        
        RowConstraints rowCabecalho = new RowConstraints();
        rowCabecalho.setPrefHeight(32);
        rowCabecalho.setMinHeight(32);
        rowCabecalho.setMaxHeight(32);
        rowCabecalho.setVgrow(Priority.NEVER);
        calendarioGrid.getRowConstraints().add(rowCabecalho);

        RowConstraints rowGap = new RowConstraints();
        rowGap.setPrefHeight(4);
        rowGap.setMinHeight(4);
        rowGap.setMaxHeight(4);
        rowGap.setVgrow(Priority.NEVER);
        calendarioGrid.getRowConstraints().add(rowGap);

        for (int i = 0; i < 5; i++) {
            RowConstraints row = new RowConstraints();
            row.setPrefHeight(90);
            row.setMinHeight(60);
            row.setVgrow(Priority.ALWAYS);
            calendarioGrid.getRowConstraints().add(row);
        }

        // Cabeçalho dos dias da semana
        String[] diasSemana = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"};
        for (int col = 0; col < 7; col++) {
            Label label = new Label(diasSemana[col]);
            label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: rgba(197, 130, 63, 0.86); -fx-background-radius: 12; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setMaxHeight(28);
            label.setAlignment(Pos.CENTER);

            StackPane cabecalhoPane = new StackPane(label);
            cabecalhoPane.setAlignment(Pos.CENTER);
            cabecalhoPane.setStyle("-fx-background-radius: 12; -fx-border-radius: 12;");
            
            calendarioGrid.add(cabecalhoPane, col, 0);
        }

        // Descobre o primeiro dia do mês e o primeiro dia a mostrar (pode ser do mês anterior)
        LocalDate primeiroDiaMes = semanaAtual.withDayOfMonth(1);
        int diaSemanaPrimeiro = primeiroDiaMes.getDayOfWeek().getValue();
        LocalDate inicioGrid = primeiroDiaMes.minusDays(diaSemanaPrimeiro - 1);

        LocalDate data = inicioGrid;
            for (int row = 2; row <= 6; row++) {
                for (int col = 0; col < 7; col++) {
                    StackPane cell = new StackPane();
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setMaxHeight(Double.MAX_VALUE);

                    Label diaLabel = new Label(String.format("%02d", data.getDayOfMonth()));
                    diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;");
                    StackPane.setAlignment(diaLabel, Pos.TOP_LEFT);
                    diaLabel.setPadding(new Insets(4, 0, 0, 6));
                    cell.getChildren().add(diaLabel);

                    if (data.equals(LocalDate.now())) {
                        cell.setStyle("-fx-background-color: rgb(255, 215, 0); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-cursor: hand;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: normal;");
                    } else if (utils.Feriados.isFeriado(data)) {
                        cell.setStyle("-fx-background-color: rgb(36, 43, 141); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-cursor: hand;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (data.getMonth() != primeiroDiaMes.getMonth()) {
                        cell.setStyle("-fx-background-color: rgba(43, 40, 40, 0.53); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-cursor: hand;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-font-weight: normal;");
                    } else {
                        cell.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-cursor: hand;");
                    }

                    final LocalDate diaClicado = data;
                    cell.setOnMouseClicked(e -> {
                        diaSelecionado = diaClicado;
                        semanaAtual = diaClicado.with(DayOfWeek.MONDAY);
                        modoAtual = ModoVisualizacao.DIA;
                        diaToggle.setSelected(true);
                        atualizarCalendario();
                    });

                    calendarioGrid.add(cell, col, row);
                    data = data.plusDays(1);
                }
            }
        atualizarCabecalho();
    }

    private void atualizarDia() {
        // Configurar colunas
        calendarioGrid.getColumnConstraints().clear();
        ColumnConstraints colHora = new ColumnConstraints();
        colHora.setPrefWidth(80);
        colHora.setMinWidth(80);
        ColumnConstraints colConteudo = new ColumnConstraints();
        colConteudo.setHgrow(Priority.ALWAYS);
        calendarioGrid.getColumnConstraints().addAll(colHora, colConteudo);
        
        celulasDia.clear();

        // Preencher linhas com horas e células vazias para conteúdos futuros
        LocalTime horaAtual = HORA_ABERTURA;
        int linha = 0;
        boolean isDomingo = diaSelecionado.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isHoje = diaSelecionado.equals(LocalDate.now());

        LocalTime blocoAtual = null;
        if (isHoje) {
            int minuto = LocalTime.now().getMinute() < 30 ? 0 : 30;
            blocoAtual = LocalTime.of(LocalTime.now().getHour(), minuto);
        }

        Map<java.time.LocalDateTime, Marcacao> marcacoesMap =
            (appController != null && appController.getMarcacoesMap() != null)
            ? appController.getMarcacoesMap()
            : java.util.Collections.emptyMap();

        while (!horaAtual.isAfter(HORA_FECHO)) {
            Label horaLabel = new Label(horaAtual.format(DateTimeFormatter.ofPattern("HH:mm")));
            horaLabel.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center; -fx-border-width: 0; -fx-padding: 8; -fx-font-size: 15px; -fx-background-radius: 12;");
            horaLabel.setMaxWidth(Double.MAX_VALUE);
            horaLabel.setAlignment(Pos.CENTER);

            StackPane celula = new StackPane();
            celula.setPrefHeight(40);
            celula.setMaxWidth(Double.MAX_VALUE);

            LocalDate dataSelecionada = diaSelecionado;
            LocalTime horaSelecionada = horaAtual;

            boolean isPassado = dataSelecionada.isBefore(LocalDate.now()) ||
                (dataSelecionada.isEqual(LocalDate.now()) && horaSelecionada.isBefore(LocalTime.now().withSecond(0).withNano(0)));

            String baseStyle;
            if (isDomingo) {
                baseStyle = "-fx-background-color: rgba(197, 130, 63, 0.86);";
            } else {
                baseStyle = "-fx-background-color: rgb(43, 40, 40);";
            }
            baseStyle += " -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;";
            
            if (isHoje && blocoAtual != null && horaAtual.equals(blocoAtual)) {
                baseStyle += " -fx-border-color: rgb(255, 215, 0); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;";
            }

            final String estiloFinal = baseStyle;
            celula.setStyle(estiloFinal + "; -fx-cursor: " + (isPassado ? "default" : "hand") + ";");
            
            // Marcacoes
            java.time.LocalDateTime dataHora = dataSelecionada.atTime(horaSelecionada);
            Marcacao marcacao1 = marcacoesMap.get(dataHora);
            Marcacao marcacao2 = marcacoesMap.get(dataHora.plusMinutes(15));

            boolean is15min1 = marcacao1 != null && marcacao1.getDuracao() == 15;
            boolean is15min2 = marcacao2 != null && marcacao2.getDuracao() == 15;

            celula.getChildren().clear();

            if (is15min1 || is15min2) {
                HBox hbox = new HBox(2);

                if (is15min1) {
                    Pane box1 = criarBoxMarcacao(marcacao1, true, true, is15min2);
                    HBox.setHgrow(box1, Priority.ALWAYS);
                    box1.setMaxWidth(Double.MAX_VALUE);
                    hbox.getChildren().add(box1);
                } else {
                    Region espaco1 = new Region();
                    HBox.setHgrow(espaco1, Priority.ALWAYS);
                    espaco1.setMaxWidth(Double.MAX_VALUE);
                    espaco1.setPrefWidth(0);
                    espaco1.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                    hbox.getChildren().add(espaco1);
                }

                if (is15min2) {
                    Pane box2 = criarBoxMarcacao(marcacao2, true, false, is15min1);
                    HBox.setHgrow(box2, Priority.ALWAYS);
                    box2.setMaxWidth(Double.MAX_VALUE);
                    hbox.getChildren().add(box2);
                } else {
                    Region espaco2 = new Region();
                    HBox.setHgrow(espaco2, Priority.ALWAYS);
                    espaco2.setPrefWidth(0);
                    espaco2.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada.plusMinutes(15)));
                    hbox.getChildren().add(espaco2);
                }

                celula.getChildren().add(hbox);
                celula.setOnMouseClicked(null);
            } else if (marcacao1 != null && marcacao1.getDuracao() >= 30) {
                Pane box = criarBoxMarcacao(marcacao1, false, false, false);
                celula.getChildren().add(box);
                celula.setOnMouseClicked(null);
            } else {
                if (!isPassado) {
                    celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                } else {
                    celula.setOnMouseClicked(null);
                }
            }

            // Hover: só para blocos não passados
            if (!isPassado || marcacao1 != null) {
                celula.setOnMouseEntered(ev -> celula.setStyle("-fx-background-color: white; -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40; -fx-cursor: hand;"));
                celula.setOnMouseExited(ev -> celula.setStyle(estiloFinal + "; -fx-cursor: hand;"));
                if (!isPassado) {
                    celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                } else {
                    celula.setOnMouseClicked(null);
                }
            } else {
                celula.setOnMouseEntered(null);
                celula.setOnMouseExited(null);
                celula.setOnMouseClicked(null);
            }

            calendarioGrid.add(horaLabel, 0, linha);
            calendarioGrid.add(celula, 1, linha);

            celulasDia.put(horaAtual, celula);

            linha++;
            horaAtual = horaAtual.plusMinutes(INTERVALO_MINUTOS);
        }

        calendarioGrid.getRowConstraints().clear();
        for (int i = 0; i < linha; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPrefHeight(40);
            rowConstraints.setVgrow(Priority.NEVER);
            calendarioGrid.getRowConstraints().add(rowConstraints);
        }

        atualizarCabecalho();
    }
    
    private void criarCabecalhoDias() {        
        // Cabeçalhos dos dias da semana
        String[] diasSemana = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"};
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
        LocalDate hoje = LocalDate.now();
        
        for (int dia = 0; dia < 7; dia++) {
            LocalDate dataAtual = semanaAtual.plusDays(dia);
            String textoCabecalho = diasSemana[dia] + " " + dataAtual.format(dayFormatter);

            Label cabecalhoLabel = new Label(textoCabecalho);
            cabecalhoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            cabecalhoLabel.setAlignment(Pos.CENTER);
            cabecalhoLabel.setMaxWidth(Double.MAX_VALUE);

            StackPane cabecalhoPane = new StackPane(cabecalhoLabel);
            cabecalhoPane.setAlignment(Pos.CENTER);
            cabecalhoPane.setOnMouseClicked(e -> {
                diaSelecionado = dataAtual;
                semanaAtual = dataAtual.with(DayOfWeek.MONDAY);
                modoAtual = ModoVisualizacao.DIA;
                diaToggle.setSelected(true);
                atualizarCalendario();
            });

            if (dataAtual.equals(hoje)) {
                cabecalhoPane.setStyle("-fx-background-color:rgb(255, 215, 0); -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 0; -fx-padding: 8;");
            } else if (utils.Feriados.isFeriado(dataAtual)) {
                cabecalhoPane.setStyle("-fx-background-color:rgb(36, 43, 141); -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 0; -fx-padding: 8;");
            } else {
                cabecalhoPane.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-background-radius: 12; -fx-border-width: 0; -fx-padding: 8;");
            }

            calendarioGrid.add(cabecalhoPane, dia + 1, 0);
        }
    }

    private void atualizarCabecalho() {
        DateTimeFormatter semanaFmt = DateTimeFormatter.ofPattern("MMMM dd", new Locale("pt", "PT"));
        DateTimeFormatter mesFmt = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("pt", "PT"));
        DateTimeFormatter diaFmt = DateTimeFormatter.ofPattern("EEEE MMMM dd", new Locale("pt", "PT"));
    
        switch (modoAtual) {
            case SEMANA:
                LocalDate fimSemana = semanaAtual.plusDays(6);
                String inicio = semanaAtual.format(semanaFmt);
                String fim = fimSemana.format(semanaFmt);
                semanaLabel.setText(inicio + " - " + fim);
                semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
                break;
            case MES:
                semanaLabel.setText(semanaAtual.format(mesFmt));
                semanaLabel.setStyle("-fx-font-size: 20px;-fx-font-weight: bold; -fx-text-fill: white;");
                break;
            case DIA:
                semanaLabel.setText(diaSelecionado.format(diaFmt));
                if (diaSelecionado.equals(LocalDate.now())) {
                    semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: rgb(255, 215, 0);");
                } else if (Feriados.isFeriado(diaSelecionado)) {
                    semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: rgb(36, 43, 141);");
                } else {
                    semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
                }
                break;
        }
    }
    
    private void criarGradeHorarios() {
        LocalTime horaAtual = HORA_ABERTURA;
        int linha = 1;

        celulasSemana.clear();

        while (!horaAtual.isAfter(HORA_FECHO)) {
            // Criar label da hora
            Label horaLabel = new Label(horaAtual.format(DateTimeFormatter.ofPattern("HH:mm")));
            horaLabel.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-text-fill: white; " +
                              "-fx-font-weight: bold; -fx-alignment: center; " +
                              "-fx-border-width: 0; -fx-padding: 8; " +
                              "-fx-font-size: 15px; -fx-background-radius:12;");
            horaLabel.setMaxWidth(Double.MAX_VALUE);
            horaLabel.setMaxHeight(Double.MAX_VALUE);
            horaLabel.setAlignment(Pos.CENTER);

            calendarioGrid.add(horaLabel, 0, linha);

            Map<Integer, Pane> linhaSemana = new HashMap<>();

            // Criar células para cada dia da semana
            for (int dia = 0; dia < 7; dia++) {
                StackPane celula = new StackPane();
                celula.setPrefHeight(40);
                celula.setMaxWidth(Double.MAX_VALUE);

                LocalDate dataSelecionada = semanaAtual.plusDays(dia);
                LocalTime horaSelecionada = horaAtual;

                boolean isPassado = dataSelecionada.isBefore(LocalDate.now()) ||
                    (dataSelecionada.isEqual(LocalDate.now()) && horaSelecionada.isBefore(LocalTime.now().withSecond(0).withNano(0)));

                // Estilo base para domingo ou outros dias
                if (dia == 6) { // Domingo
                    celula.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86);"
                        + "-fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; "
                        + "-fx-background-radius:12; -fx-border-radius: 12;"
                        + "-fx-min-height: 40;");
                } else {
                    celula.setStyle("-fx-background-color: rgb(43, 40, 40);"
                        + "-fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; "
                        + "-fx-background-radius:12; -fx-border-radius: 12;"
                        + "-fx-min-height: 40;");
                }

                java.time.LocalDateTime dataHora = dataSelecionada.atTime(horaSelecionada);
                Map<java.time.LocalDateTime, Marcacao> marcacoesMap =
                    (appController != null && appController.getMarcacoesMap() != null)
                    ? appController.getMarcacoesMap()
                    : java.util.Collections.emptyMap();

                Marcacao marcacao1 = marcacoesMap.get(dataHora); // 1ª metade
                Marcacao marcacao2 = marcacoesMap.get(dataHora.plusMinutes(15)); // 2ª metade

                // Só adiciona hover e cursor se não for passado ou entao se for marcacao
                if (!isPassado || marcacao1 != null) {
                    if (dia == 6) {
                        celula.setOnMouseEntered(e ->
                            celula.setStyle("-fx-background-color:rgb(221, 233, 236); -fx-background-radius: 12; -fx-min-height: 40;"));
                        celula.setOnMouseExited(e ->
                            celula.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); "
                                + "-fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;"));
                    } else {
                        celula.setOnMouseEntered(e ->
                            celula.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-min-height: 40;"));
                        celula.setOnMouseExited(e ->
                            celula.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); "
                                + "-fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;"));
                    }
                    if (!isPassado) {
                        celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                    } else {
                        celula.setOnMouseClicked(null);
                    }
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: hand;");
                } else {
                    celula.setOnMouseEntered(null);
                    celula.setOnMouseExited(null);
                    celula.setOnMouseClicked(null);
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
                }

                boolean is15min1 = marcacao1 != null && marcacao1.getDuracao() == 15;
                boolean is15min2 = marcacao2 != null && marcacao2.getDuracao() == 15;

                if (is15min1 || is15min2) {
                    HBox hbox = new HBox(2);

                    Region box1 = is15min1 ? criarBoxMarcacao(marcacao1, true, true, is15min2) : new Region();
                    Region box2 = is15min2 ? criarBoxMarcacao(marcacao2, true, false, is15min1) : new Region();

                    hbox.getChildren().addAll(box1, box2);

                    celula.widthProperty().addListener((obs, oldVal, newVal) -> {
                        double largura = newVal.doubleValue();
                        box1.setPrefWidth(largura / 2);
                        box2.setPrefWidth(largura / 2);
                    });

                    double larguraCelula = celula.getWidth() > 0 ? celula.getWidth() : 110;
                    box1.setPrefWidth(larguraCelula / 2);
                    box2.setPrefWidth(larguraCelula / 2);

                    if (!is15min1) {
                        box1.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                    }
                    if (!is15min2) {
                        box2.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada.plusMinutes(15)));
                    }

                    celula.getChildren().clear();
                    celula.getChildren().add(hbox);
                    celula.setOnMouseClicked(null);
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
                } else if (marcacao1 != null && marcacao1.getDuracao() >= 30) {
                    Pane box = criarBoxMarcacao(marcacao1, false, false, false);
                    celula.getChildren().add(box);
                    celula.setOnMouseClicked(null);
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
                }

                calendarioGrid.add(celula, dia + 1, linha);

                linhaSemana.put(dia, celula);
            }

            celulasSemana.put(horaAtual, linhaSemana);

            linha++;
            horaAtual = horaAtual.plusMinutes(INTERVALO_MINUTOS);
        }

        calendarioGrid.getRowConstraints().clear();
        for (int i = 0; i < linha; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPrefHeight(40);
            rowConstraints.setVgrow(Priority.ALWAYS);
            calendarioGrid.getRowConstraints().add(rowConstraints);
        }

        atualizarCabecalho();
    }

    private void atualizarRelogio() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        relogioLabel.setText(java.time.LocalTime.now().format(formatter));
    }

    private void destacarBlocoAtual() {
        if (!diaSelecionado.equals(LocalDate.now())) {
            celulasDia.forEach((hora, pane) -> {
                if (diaSelecionado.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    pane.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                } else {
                    pane.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                }
            });
            return;
        }
        LocalTime agora = LocalTime.now();
        if (agora.isBefore(HORA_ABERTURA) || agora.isAfter(HORA_FECHO.plusMinutes(INTERVALO_MINUTOS - 1))) {
            celulasDia.forEach((hora, pane) -> {
                if (diaSelecionado.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    pane.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                } else {
                    pane.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                }
            });
            return;
        }
        int minuto = agora.getMinute() < 30 ? 0 : 30;
        LocalTime blocoAtual = LocalTime.of(agora.getHour(), minuto);

        celulasDia.forEach((hora, pane) -> {
            if (hora.equals(blocoAtual)) {
                pane.setStyle("-fx-background-color: rgb(255, 215, 0); -fx-border-color:rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
            } else if (diaSelecionado.getDayOfWeek() == DayOfWeek.SUNDAY) {
                pane.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
            } else {
                pane.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
            }
        });
    }

    private void destacarBlocoAtualSemana() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemanaVisivel = semanaAtual;
        LocalDate fimSemanaVisivel = semanaAtual.plusDays(6);

        if (hoje.isBefore(inicioSemanaVisivel) || hoje.isAfter(fimSemanaVisivel)) {
            celulasSemana.values().forEach(map -> map.forEach((dia, p) -> {
                if (dia == 6) {
                    p.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                } else {
                    p.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                }
            }));
            return;
        }

        LocalTime agora = LocalTime.now();
        if (agora.isBefore(HORA_ABERTURA) || agora.isAfter(HORA_FECHO.plusMinutes(INTERVALO_MINUTOS - 1))) {
            celulasSemana.values().forEach(map -> map.forEach((dia, p) -> {
                if (dia == 6) {
                    p.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                } else {
                    p.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                }
            }));
            return;
        }

        int minuto = agora.getMinute() < 30 ? 0 : 30;
        LocalTime blocoAtual = LocalTime.of(agora.getHour(), minuto);
        int diaSemana = hoje.getDayOfWeek().getValue() - 1;

        celulasSemana.forEach((hora, map) -> {
            map.forEach((dia, pane) -> {
                if (hora.equals(blocoAtual) && dia == diaSemana) {
                    pane.setStyle("-fx-background-color: rgb(255, 215, 0); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                } else if (dia == 6) { // Domingo
                    pane.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                } else {
                    pane.setStyle("-fx-background-color: rgb(43, 40, 40); -fx-border-color: rgba(197, 130, 63, 0.86); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-min-height: 40;");
                }
            });
        });
    }

    private Label novoCell(String texto) {
        Label l = new Label(texto == null ? "—" : texto);
        l.setStyle("-fx-font-size: 14px; -fx-padding: 4 8 4 8; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 1 0;");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private void abrirDetalhesCliente(models.Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DetalheCliente.fxml"));
            Parent root = loader.load();
            DetalheClienteController controller = loader.getController();
            controller.setCliente(cliente);
            controller.setPaginaPrincipalController(this);

            Stage stage = new Stage();
            stage.setTitle("Detalhes do Cliente");
            stage.setScene(new Scene(root));
            stage.initOwner(clientesContent.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);

            double largura = areaCentral.getWidth() * 0.75;
            double altura = areaCentral.getHeight();
            if (largura < 320) largura = 320;
            stage.setWidth(largura);
            stage.setHeight(altura);
            stage.setX(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinX() + (areaCentral.getWidth() - largura) / 2);
            stage.setY(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinY());

            stage.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void abrirGestaoPendentes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Pendentes.fxml"));
            Parent root = loader.load();
            PendentesController controller = loader.getController();
            controller.setPendentes(appController.getPendentes());
            controller.setAppController(appController);
            controller.setPaginaPrincipalController(this);

            Stage stage = new Stage();
            stage.setTitle("Clientes Pendentes");
            stage.setScene(new Scene(root));
            stage.initOwner(rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);

            double largura = areaCentral.getWidth() * 0.75;
            double altura = areaCentral.getHeight();
            if (largura < 400) largura = 400;
            stage.setWidth(largura);
            stage.setHeight(altura);
            stage.setX(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinX() + (areaCentral.getWidth() - largura) / 2);
            stage.setY(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinY());

            stage.showAndWait();
            atualizarBoxClientesPendentes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void atualizarBoxClientesPendentes() {
        caixaClientesPendentes.getChildren().clear();

        java.util.List<models.Pendente> pendentes = appController != null ? appController.getPendentes() : null;

        if (pendentes == null || pendentes.isEmpty()) {
            Label vazio = new Label("Clique para adicionar pendente");
            vazio.setStyle("-fx-font-size: 15px; -fx-text-fill: #bbb; -fx-font-style: italic; -fx-alignment: center;");
            vazio.setMaxWidth(Double.MAX_VALUE);
            vazio.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(vazio, Priority.ALWAYS);
            vazio.setOnMouseClicked(e -> {
                abrirGestaoPendentes();
                e.consume();
            });
            caixaClientesPendentes.getChildren().add(vazio);
            vazio.setStyle(vazio.getStyle() + "; -fx-cursor: hand;");
            return;
        }

        for (int i = 0; i < pendentes.size(); i++) {
            models.Pendente p = pendentes.get(i);

            Label nome = new Label(p.getNome());
            nome.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 2 4 2 4; -fx-cursor: hand;");
            nome.setMaxWidth(Double.MAX_VALUE);
            nome.setOnMouseClicked(e -> {
                abrirGestaoPendentes();
                e.consume();
            });

            caixaClientesPendentes.getChildren().add(nome);

            if (i < pendentes.size() - 1) {
                Separator sep = new Separator();
                sep.setPrefHeight(2);
                sep.setStyle("-fx-background-color: rgba(197, 130, 63, 0.86);");
                sep.setMaxWidth(Double.MAX_VALUE);
                VBox.setMargin(sep, new Insets(0, 4, 0, 4));
                caixaClientesPendentes.getChildren().add(sep);
            }
        }

        caixaClientesPendentes.setOnMouseClicked(e -> abrirGestaoPendentes());
        caixaClientesPendentes.setStyle("-fx-cursor: hand;");
    }

    private void abrirCriarMarcacao(LocalDate data, LocalTime hora) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdicionarMarcacao.fxml"));
            Parent root = loader.load();

            AdicionarMarcacaoController controller = loader.getController();
            controller.setDataHora(data, hora);
            controller.setAppController(appController);

            Stage stage = new Stage();
            stage.setTitle("Nova Marcação");
            stage.setScene(new Scene(root));

            double largura = areaCentral.getWidth() * 0.25;
            double altura = areaCentral.getHeight();
            if (largura < 320) largura = 320;
            stage.setWidth(largura);
            stage.setHeight(altura);
            stage.setX(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinX() + (areaCentral.getWidth() - largura) / 2);
            stage.setY(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinY());

            stage.initOwner(areaCentral.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);

            stage.showAndWait();
            atualizarCalendario();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Pane criarBoxMarcacao(Marcacao marcacao, boolean meiaMarcacao, boolean ladoEsquerdo, boolean existeOutraMarcacao) {
        Label nome = new Label(marcacao.getCliente().getNome());
        nome.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        nome.setMaxWidth(Double.MAX_VALUE);
        nome.setAlignment(Pos.CENTER_LEFT);

        StackPane box = new StackPane(nome);
        
        if (marcacao.isFalta()) {
            box.setStyle("-fx-background-color:rgb(128, 26, 15); -fx-background-radius: 12; -fx-border-radius: 12;");
        } else {
            box.setStyle("-fx-background-color:rgb(14, 126, 79); -fx-background-radius: 12; -fx-border-radius: 12;");
        }
        box.setPrefHeight(32);

        StackPane.setAlignment(nome, Pos.CENTER_LEFT);
        StackPane.setMargin(nome, new Insets(0, 10, 0, 10));

        StackPane container = new StackPane(box);   

        if (meiaMarcacao) {
            if (ladoEsquerdo) {
                container.setPadding(new Insets(4, existeOutraMarcacao ? 1 : 0, 4, 4));
            } else {
                container.setPadding(new Insets(4, 4, 4, existeOutraMarcacao ? 1 : 0));
            }
        } else {
            container.setPadding(new Insets(4, 4, 4, 4));
        }

        container.setMaxWidth(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);

        container.setOnMouseClicked(e -> abrirDetalheMarcacao(marcacao));

        return container;
    }

    private void abrirDetalheMarcacao(Marcacao marcacao) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DetalheMarcacao.fxml"));
            Parent root = loader.load();
            DetalheMarcacaoController controller = loader.getController();
            controller.setMarcacao(marcacao);
            controller.setAppController(appController);

            Stage stage = new Stage();
            stage.setTitle("Detalhe da Marcação");
            stage.setScene(new Scene(root));
            stage.initOwner(areaCentral.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);

            double largura = areaCentral.getWidth() * 0.25;
            double altura = areaCentral.getHeight();
            if (largura < 320) largura = 320;
            stage.setWidth(largura);
            stage.setHeight(altura);
            stage.setX(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinX() + (areaCentral.getWidth() - largura) / 2);
            stage.setY(areaCentral.localToScreen(areaCentral.getBoundsInLocal()).getMinY());

            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void atualizarMarcacoesSemanaisSeNecessario() {
        Map<String, Cliente> clientes = appController.getClientesMap();
        Map<java.time.LocalDateTime, Marcacao> marcacoes = appController.getMarcacoesMap();
        java.time.LocalDate hoje = java.time.LocalDate.now();
    
        for (Cliente cliente : clientes.values()) {
            if (cliente.getTipoCliente() == Cliente.TipoCliente.SEMANAL) {
                // Encontrar a última marcação futura deste cliente
                java.time.LocalDateTime ultima = marcacoes.values().stream()
                    .filter(m -> m.getCliente().equals(cliente) && !m.getDataHora().toLocalDate().isBefore(hoje))
                    .map(Marcacao::getDataHora)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null);
    
                if (ultima == null) {
                    // Não tem marcações futuras, gera a partir de hoje
                    var novas = utils.MarcacoesSemanais.gerarMarcacoesSemanais(cliente, marcacoes, hoje);
                    for (Marcacao m : novas) marcacoes.put(m.getDataHora(), m);
                } else {
                    java.time.LocalDate dataUltima = ultima.toLocalDate();
                    java.time.LocalDate limite = dataUltima.minusMonths(3);
                    if (!hoje.isBefore(limite)) {
                        // Faltam menos de 3 meses, gera mais 6 meses a partir da última marcação
                        var novas = utils.MarcacoesSemanais.gerarMarcacoesSemanais(cliente, marcacoes, dataUltima.plusWeeks(1));
                        for (Marcacao m : novas) marcacoes.put(m.getDataHora(), m);
                    }
                }
            }
        }
        utils.Persistencia.guardarMarcacoes(marcacoes);
    }

    private void atualizarEstiloTogglesModo() {
        String ativo = "-fx-background-color: rgb(60, 60, 60); -fx-border-color: rgb(43, 40, 40); -fx-text-fill: white; -fx-font-weight: bold; -fx-border-width: 0; -fx-background-radius: 12; -fx-border-radius: 12;";
        String inativo = "-fx-background-color: rgb(43, 40, 40); -fx-border-colour: rgb(43, 40, 40); -fx-text-fill: white; -fx-font-weight: bold; -fx-border-width: 0; -fx-background-radius: 12; -fx-border-radius: 12;";
        diaToggle.setStyle(modoAtual == ModoVisualizacao.DIA ? ativo : inativo);
        semanaToggle.setStyle(modoAtual == ModoVisualizacao.SEMANA ? ativo : inativo);
        mesToggle.setStyle(modoAtual == ModoVisualizacao.MES ? ativo : inativo);
    }

    private void atualizarEstiloTogglesLaterais() {
        String base = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-border-radius: 12;";
        String ativo = base + " -fx-background-color: rgb(60,60,60); -fx-text-fill: white;";
        String inativo = base + " -fx-background-color: rgb(43,40,40); -fx-text-fill: white;";
        calendarioToggle.setStyle(calendarioToggle.isSelected() ? ativo : inativo);
        clientesToggle.setStyle(clientesToggle.isSelected() ? ativo : inativo);
    }
}
