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

        // Remover o foco visual dos botoes
        todayBtn.setFocusTraversable(false);
        semanaAnteriorBtn.setFocusTraversable(false);
        proximaSemanaBtn.setFocusTraversable(false);
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
            }
        });
        
        atualizarCalendario();

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
                    anotacoesArea.setEditable(false);
                } else {
                    anotacoesArea.setEffect(null);
                    blurToggleBtn.setText("🚫");
                    anotacoesArea.setEditable(true);
                }
            });
        }

        if (caixaClientesPendentes != null) {
            caixaClientesPendentes.setStyle("-fx-cursor: hand;");
            caixaClientesPendentes.setOnMouseEntered(e -> caixaClientesPendentes.setStyle("-fx-cursor: hand;"));
            caixaClientesPendentes.setOnMouseExited(e -> caixaClientesPendentes.setStyle("-fx-cursor: hand;"));
            caixaClientesPendentes.setOnMouseClicked(e -> abrirGestaoPendentes());
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
    }

    @FXML
    public void mostrarClientes() {
        calendarioToggle.setSelected(false);
        clientesToggle.setSelected(true);
        areaCentral.setVisible(false);
        areaClientes.setVisible(true);

        clientesContent.getChildren().clear();

        if (appController != null && (appController.getClientesMap() == null || appController.getClientesMap().isEmpty())) {
            Label msg = new Label("Não tem nenhum cliente salvo, deseja adicionar um?");
            msg.setStyle("-fx-font-size: 22px; -fx-text-fill: #222; -fx-font-weight: bold;");
            Button adicionarBtn = new Button("Adicionar");
            adicionarBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24 8 24;");
            
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

            VBox box = new VBox(18, msg, adicionarBtn);
            box.setAlignment(Pos.CENTER);
            box.setPrefHeight(clientesContent.getHeight());
            box.setPrefWidth(clientesContent.getWidth());
            box.setStyle("-fx-alignment: center; -fx-padding: 0;");

            clientesContent.setAlignment(Pos.CENTER);
            clientesContent.getChildren().add(box);
        } else {
            HBox barraTopo = new HBox(10);
            barraTopo.setAlignment(Pos.TOP_LEFT);
            barraTopo.setPadding(new Insets(10, 10, 10, 10));

            double alturaBarra = 32;
            double larguraBotao = 32;

            TextField pesquisaField = new TextField();
            pesquisaField.setPromptText("Pesquisar cliente...");
            pesquisaField.setPrefHeight(alturaBarra);
            pesquisaField.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
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
            btnAdicionar.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0;");
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
            layout.setOnMouseClicked(event -> {
                if (event.getTarget() != pesquisaField) {
                    layout.requestFocus();
                }
            });
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
        tabela.setVgap(2);
        tabela.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 10; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
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
            th.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #d6eaf8; -fx-padding: 6 12 6 12; -fx-border-color: #bdc3c7; -fx-border-width: 0 1 1 0;");
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
                Label nomeLabel = novoCell(c.getNome());
                nomeLabel.setStyle(nomeLabel.getStyle() + "; -fx-cursor: hand;");
                nomeLabel.setOnMouseClicked(event -> {
                    abrirDetalhesCliente(c);
                });
                tabela.add(nomeLabel, 0, row);

                tabela.add(novoCell(c.getNumeroTelefone()), 1, row);
                tabela.add(novoCell(c.getTipoCliente().toString()), 2, row);
                tabela.add(novoCell(String.valueOf(c.getFaltas())), 3, row);
                if (c.getTipoCliente() == models.Cliente.TipoCliente.SEMANAL) {
                    tabela.add(novoCell(c.getDiaSemana()), 4, row);
                    tabela.add(novoCell(c.getHoraCorte()), 5, row);
                } else {
                    tabela.add(novoCell("—"), 4, row);
                    tabela.add(novoCell("—"), 5, row);
                }
                row++;
            }
        }

        ScrollPane tabelaScroll = new ScrollPane(tabela);
        tabelaScroll.setFitToWidth(true);
        tabelaScroll.setFitToHeight(true);
        tabelaScroll.setFocusTraversable(false);
        tabelaScroll.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;");
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
        semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

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

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            calendarioGrid.getColumnConstraints().add(col);
        }
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            if (i == 0) {
                row.setPrefHeight(28);
                row.setMinHeight(20);
                row.setMaxHeight(32);
                row.setVgrow(Priority.NEVER);
            } else {
                row.setVgrow(Priority.ALWAYS);
            }
            calendarioGrid.getRowConstraints().add(row);
        }

        // Cabeçalho dos dias da semana
        String[] diasSemana = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        for (int col = 0; col < 7; col++) {
            Label label = new Label(diasSemana[col]);
            label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3498db; -fx-background-color: #eaf6fb; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setMaxHeight(28);
            label.setAlignment(Pos.CENTER);
            calendarioGrid.add(label, col, 0);
        }

        // Descobre o primeiro dia do mês e o primeiro dia a mostrar (pode ser do mês anterior)
        LocalDate primeiroDiaMes = semanaAtual.withDayOfMonth(1);
        int diaSemanaPrimeiro = primeiroDiaMes.getDayOfWeek().getValue();
        LocalDate inicioGrid = primeiroDiaMes.minusDays(diaSemanaPrimeiro - 1);

        LocalDate data = inicioGrid;
            for (int row = 1; row <= 5; row++) {
                for (int col = 0; col < 7; col++) {
                    StackPane cell = new StackPane();
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setMaxHeight(Double.MAX_VALUE);

                    Label diaLabel = new Label(String.format("%02d", data.getDayOfMonth()));
                    diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #222; -fx-font-weight: bold;");
                    StackPane.setAlignment(diaLabel, Pos.TOP_LEFT);
                    diaLabel.setPadding(new Insets(4, 0, 0, 6));
                    cell.getChildren().add(diaLabel);

                    if (data.equals(LocalDate.now())) {
                        cell.setStyle("-fx-background-color: #ffb366; -fx-border-color: #e67e22; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: normal;");
                    } else if (utils.Feriados.isFeriado(data)) {
                        cell.setStyle("-fx-background-color: #f78fb3; -fx-border-color: #e67e22; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (col == 6) { // Domingo
                        cell.setStyle("-fx-background-color: #145a32; -fx-border-color: #e67e22; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (data.getMonth() != primeiroDiaMes.getMonth()) {
                        cell.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
                        diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-font-weight: normal;");
                    } else {
                        cell.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-cursor: hand;");
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
        while (!horaAtual.isAfter(HORA_FECHO)) {
            Label horaLabel = new Label(horaAtual.format(DateTimeFormatter.ofPattern("HH:mm")));
            horaLabel.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8;");
            horaLabel.setMaxWidth(Double.MAX_VALUE);
            horaLabel.setAlignment(Pos.CENTER);

            StackPane celula = new StackPane();
            celula.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;");
            celula.setPrefHeight(40);
            celula.setMaxWidth(Double.MAX_VALUE);

            LocalDate dataSelecionada = diaSelecionado;
            LocalTime horaSelecionada = horaAtual;

            boolean isPassado = dataSelecionada.isBefore(LocalDate.now()) ||
                (dataSelecionada.isEqual(LocalDate.now()) && horaSelecionada.isBefore(LocalTime.now().withSecond(0).withNano(0)));

            if (!isPassado) {
                celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                celula.setStyle(celula.getStyle() + "; -fx-cursor: hand;");
            } else {
                celula.setStyle(celula.getStyle() + "; -fx-background-color: #f5f5f5; -fx-cursor: default;");
            }

            celula.setOnMouseEntered(e -> 
                celula.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #3498db; -fx-border-width: 2; -fx-min-height: 40;"));
            celula.setOnMouseExited(e -> 
                celula.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;"));

            calendarioGrid.add(horaLabel, 0, linha);
            calendarioGrid.add(celula, 1, linha);

            celulasDia.put(horaAtual, celula);

            linha++;
            horaAtual = horaAtual.plusMinutes(INTERVALO_MINUTOS);
            
            java.time.LocalDateTime dataHora = dataSelecionada.atTime(horaSelecionada);
            Map<java.time.LocalDateTime, Marcacao> marcacoesMap = 
                (appController != null && appController.getMarcacoesMap() != null)
                ? appController.getMarcacoesMap()
                : java.util.Collections.emptyMap();

            Marcacao marcacao1 = marcacoesMap.get(dataHora);
            Marcacao marcacao2 = marcacoesMap.get(dataHora.plusMinutes(15));

            boolean is15min1 = marcacao1 != null && marcacao1.getDuracao() == 15;
            boolean is15min2 = marcacao2 != null && marcacao2.getDuracao() == 15;

            if (is15min1 || is15min2) {
                HBox hbox = new HBox(2);

                if (is15min1) {
                    Pane box1 = criarBoxMarcacao(marcacao1);
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
                    Pane box2 = criarBoxMarcacao(marcacao2);
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

                celula.getChildren().clear();
                celula.getChildren().add(hbox);
                celula.setOnMouseClicked(null);
                celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
            } else if (marcacao1 != null && marcacao1.getDuracao() >= 30) {
                Pane box = criarBoxMarcacao(marcacao1);
                celula.getChildren().add(box);
                celula.setOnMouseClicked(null);
                celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
            } else if (!isPassado) {
                celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                celula.setStyle(celula.getStyle() + "; -fx-cursor: hand;");
            } else {
                celula.setStyle(celula.getStyle() + "; -fx-background-color: #f5f5f5; -fx-cursor: default;");
            }
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
        // Célula vazia no canto superior esquerdo
        Label horaHeader = new Label("");
        horaHeader.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; " +
                           "-fx-font-weight: bold; -fx-alignment: center; " +
                           "-fx-border-color: white; -fx-border-width: 1; -fx-padding: 10;");
        horaHeader.setMaxWidth(Double.MAX_VALUE);
        horaHeader.setMaxHeight(Double.MAX_VALUE);
        calendarioGrid.add(horaHeader, 0, 0);
        
        // Cabeçalhos dos dias da semana
        String[] diasSemana = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
        LocalDate hoje = LocalDate.now();
        
        for (int dia = 0; dia < 7; dia++) {
            LocalDate dataAtual = semanaAtual.plusDays(dia);
            
            Label diaSemanaLabel = new Label(diasSemana[dia]);
            diaSemanaLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label diaNumeroLabel = new Label(dataAtual.format(dayFormatter));
            diaNumeroLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            VBox vbox = new VBox(2, diaSemanaLabel, diaNumeroLabel);
            vbox.setAlignment(Pos.CENTER);
            vbox.setOnMouseClicked(e -> {
                diaSelecionado = dataAtual;
                semanaAtual = dataAtual.with(DayOfWeek.MONDAY);
                modoAtual = ModoVisualizacao.DIA;
                diaToggle.setSelected(true);
                atualizarCalendario();
            });

            if (dataAtual.equals(hoje)) {
                vbox.setStyle("-fx-background-color: #ffb366; -fx-border-color: white; -fx-border-width: 2; -fx-padding: 8;");
            } else if (utils.Feriados.isFeriado(dataAtual)) {
                vbox.setStyle("-fx-background-color: #f78fb3; -fx-border-color: white; -fx-border-width: 2; -fx-padding: 8;");
            } else if (dia == 6) { // Domingo
                vbox.setStyle("-fx-background-color: #145a32; -fx-border-color: white; -fx-border-width: 2; -fx-padding: 8;");
            } else {
                vbox.setStyle("-fx-background-color: #3498db; -fx-border-color: white; -fx-border-width: 1; -fx-padding: 8;");
            }

            calendarioGrid.add(vbox, dia + 1, 0);
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
                break;
            case MES:
                semanaLabel.setText(semanaAtual.format(mesFmt));
                break;
            case DIA:
                semanaLabel.setText(diaSelecionado.format(diaFmt));
                if (diaSelecionado.equals(LocalDate.now())) {
                    semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ff8800;");
                } else {
                    semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
                }
                break;
        }
        semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    }
    
    private void criarGradeHorarios() {
        LocalTime horaAtual = HORA_ABERTURA;
        int linha = 1;

        celulasSemana.clear();
        
        while (!horaAtual.isAfter(HORA_FECHO)) {
            // Criar label da hora
            Label horaLabel = new Label(horaAtual.format(DateTimeFormatter.ofPattern("HH:mm")));
            horaLabel.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; " +
                              "-fx-font-weight: bold; -fx-alignment: center; " +
                              "-fx-border-color: white; -fx-border-width: 1; -fx-padding: 8;");
            horaLabel.setMaxWidth(Double.MAX_VALUE);
            horaLabel.setMaxHeight(Double.MAX_VALUE);
            horaLabel.setAlignment(Pos.CENTER);
            
            calendarioGrid.add(horaLabel, 0, linha);

            Map<Integer, Pane> linhaSemana = new HashMap<>();
            
            // Criar células para cada dia da semana
            for (int dia = 0; dia < 7; dia++) {
                StackPane celula = new StackPane();
                celula.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; " +
                               "-fx-border-width: 1; -fx-min-height: 40;");
                celula.setPrefHeight(40);
                celula.setMaxWidth(Double.MAX_VALUE);
                
                // Adicionar efeito hover
                celula.setOnMouseEntered(e -> 
                    celula.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #3498db; " +
                                   "-fx-border-width: 2; -fx-min-height: 40;"));
                celula.setOnMouseExited(e -> 
                    celula.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; " +
                                   "-fx-border-width: 1; -fx-min-height: 40;"));
                
                LocalDate dataSelecionada = semanaAtual.plusDays(dia);
                LocalTime horaSelecionada = horaAtual;

                boolean isPassado = dataSelecionada.isBefore(LocalDate.now()) ||
                    (dataSelecionada.isEqual(LocalDate.now()) && horaSelecionada.isBefore(LocalTime.now().withSecond(0).withNano(0)));

                if (!isPassado) {
                    celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: hand;");
                } else {
                    celula.setStyle(celula.getStyle() + "; -fx-background-color: #f5f5f5; -fx-cursor: default;");
                }
                
                calendarioGrid.add(celula, dia + 1, linha);

                linhaSemana.put(dia, celula);

                java.time.LocalDateTime dataHora = dataSelecionada.atTime(horaSelecionada);
                Map<java.time.LocalDateTime, Marcacao> marcacoesMap = 
                    (appController != null && appController.getMarcacoesMap() != null)
                    ? appController.getMarcacoesMap()
                    : java.util.Collections.emptyMap();

                Marcacao marcacao1 = marcacoesMap.get(dataHora); // 1ª metade
                Marcacao marcacao2 = marcacoesMap.get(dataHora.plusMinutes(15)); // 2ª metade

                boolean is15min1 = marcacao1 != null && marcacao1.getDuracao() == 15;
                boolean is15min2 = marcacao2 != null && marcacao2.getDuracao() == 15;

                if (is15min1 || is15min2) {
                    HBox hbox = new HBox(2);

                    if (is15min1) {
                        Pane box1 = criarBoxMarcacao(marcacao1);
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
                        Pane box2 = criarBoxMarcacao(marcacao2);
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

                    celula.getChildren().clear();
                    celula.getChildren().add(hbox);
                    celula.setOnMouseClicked(null);
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
                } else if (marcacao1 != null && marcacao1.getDuracao() >= 30) {
                    Pane box = criarBoxMarcacao(marcacao1);
                    celula.getChildren().add(box);
                    celula.setOnMouseClicked(null);
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: default;");
                } else if (!isPassado) {
                    celula.setOnMouseClicked(e -> abrirCriarMarcacao(dataSelecionada, horaSelecionada));
                    celula.setStyle(celula.getStyle() + "; -fx-cursor: hand;");
                } else {
                    celula.setStyle(celula.getStyle() + "; -fx-background-color: #f5f5f5; -fx-cursor: default;");
                }
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
            celulasDia.values().forEach(p -> p.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;"));
            return;
        }
        LocalTime agora = LocalTime.now();
        if (agora.isBefore(HORA_ABERTURA) || agora.isAfter(HORA_FECHO.plusMinutes(INTERVALO_MINUTOS - 1))) {
            celulasDia.values().forEach(p -> p.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;"));
            return;
        }
        int minuto = agora.getMinute() < 30 ? 0 : 30;
        LocalTime blocoAtual = LocalTime.of(agora.getHour(), minuto);

        celulasDia.forEach((hora, pane) -> {
            if (hora.equals(blocoAtual)) {
                pane.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: #ff9800; -fx-border-width: 2; -fx-min-height: 40;");
            } else {
                pane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;");
            }
        });
    }

    private void destacarBlocoAtualSemana() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemanaVisivel = semanaAtual;
        LocalDate fimSemanaVisivel = semanaAtual.plusDays(6);

        if (hoje.isBefore(inicioSemanaVisivel) || hoje.isAfter(fimSemanaVisivel)) {
            celulasSemana.values().forEach(map -> map.values().forEach(p ->
                p.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;")));
            return;
        }

        LocalTime agora = LocalTime.now();
        if (agora.isBefore(HORA_ABERTURA) || agora.isAfter(HORA_FECHO.plusMinutes(INTERVALO_MINUTOS - 1))) {
            celulasSemana.values().forEach(map -> map.values().forEach(p ->
                p.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;")));
            return;
        }

        int minuto = agora.getMinute() < 30 ? 0 : 30;
        LocalTime blocoAtual = LocalTime.of(agora.getHour(), minuto);
        int diaSemana = hoje.getDayOfWeek().getValue() - 1;

        celulasSemana.forEach((hora, map) -> {
            map.forEach((dia, pane) -> {
                if (hora.equals(blocoAtual) && dia == diaSemana) {
                    pane.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: #ff9800; -fx-border-width: 2; -fx-min-height: 40;");
                } else {
                    pane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;");
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

    private void atualizarBoxClientesPendentes() {
        caixaClientesPendentes.getChildren().clear();

        java.util.List<models.Pendente> pendentes = appController != null ? appController.getPendentes() : null;

        if (pendentes == null || pendentes.isEmpty()) {
            return;
        }

        for (int i = 0; i < pendentes.size(); i++) {
            models.Pendente p = pendentes.get(i);

            Label nome = new Label(p.getNome());
            nome.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 8 0 8 0;");
            nome.setMaxWidth(Double.MAX_VALUE);

            caixaClientesPendentes.getChildren().add(nome);

            if (i < pendentes.size() - 1) {
                Separator sep = new Separator();
                sep.setStyle("-fx-background-color: #e0e0e0;");
                sep.setMaxWidth(Double.MAX_VALUE);
                caixaClientesPendentes.getChildren().add(sep);
            }
        }
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

    private Pane criarBoxMarcacao(Marcacao marcacao) {
        Label nome = new Label(marcacao.getCliente().getNome());
        nome.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        nome.setMaxWidth(Double.MAX_VALUE);
        nome.setPrefWidth(0);
        nome.setAlignment(Pos.CENTER_LEFT);

        StackPane box = new StackPane(nome);
        
        if (marcacao.isFalta()) {
            box.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 12; -fx-border-radius: 12;");
        } else {
            box.setStyle("-fx-background-color: #A020F0; -fx-background-radius: 12; -fx-border-radius: 12;");
        }
        box.setPrefHeight(36);
        box.setMaxWidth(Double.MAX_VALUE);
        nome.setPrefWidth(0);
        StackPane.setAlignment(nome, Pos.CENTER_LEFT);
        StackPane.setMargin(nome, new Insets(0, 10, 0, 10));

        box.setOnMouseClicked(e -> abrirDetalheMarcacao(marcacao));

        return box;
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
}
