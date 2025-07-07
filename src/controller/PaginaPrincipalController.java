package controller;

import com.sun.jdi.VMCannotBeModifiedException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import models.Utilizador;

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
    
    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
        if (userLabel != null) {
            userLabel.setText("Bem-vindo, " + utilizador.getNome());
        }
    }
    
    public void setAppController(Controller appController) {
        this.appController = appController;
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
            LocalDate hoje = LocalDate.now();
            
            if (newToggle == semanaToggle) {
                modoAtual = ModoVisualizacao.SEMANA;
                if (oldToggle == mesToggle) {
                    if (semanaAtual.getMonth() == hoje.getMonth() && semanaAtual.getYear() == hoje.getYear()) {
                        semanaAtual = hoje.with(DayOfWeek.MONDAY);
                        diaSelecionado = hoje;
                    } else {
                        semanaAtual = semanaAtual.withDayOfMonth(1).with(DayOfWeek.MONDAY);
                        diaSelecionado = semanaAtual;
                    }
                }
                else if (oldToggle == diaToggle) {
                    if (diaSelecionado.equals(hoje)) {
                        semanaAtual = hoje.with(DayOfWeek.MONDAY);
                        diaSelecionado = hoje;
                    } else {
                        semanaAtual = diaSelecionado.with(DayOfWeek.MONDAY);
                        diaSelecionado = semanaAtual;
                    }
                }
                atualizarCalendario();
            } else if (newToggle == mesToggle) {
                modoAtual = ModoVisualizacao.MES;
                if (oldToggle == semanaToggle) {
                    LocalDate inicioSemana = semanaAtual;
                    LocalDate fimSemana = semanaAtual.plusDays(6);
                    if (!hoje.isBefore(inicioSemana) && !hoje.isAfter(fimSemana)) {
                        semanaAtual = hoje.withDayOfMonth(1);
                        diaSelecionado = hoje;
                    } else {
                        semanaAtual = semanaAtual.withDayOfMonth(1);
                        diaSelecionado = semanaAtual;
                    }
                }
                else if (oldToggle == diaToggle) {
                    if (diaSelecionado.equals(hoje)) {
                        semanaAtual = hoje.withDayOfMonth(1);
                        diaSelecionado = hoje;
                    } else {
                        semanaAtual = diaSelecionado.withDayOfMonth(1);
                        diaSelecionado = diaSelecionado;
                    }
                }
                atualizarCalendario();
            } else if (newToggle == diaToggle) {
                modoAtual = ModoVisualizacao.DIA;
                if (oldToggle == semanaToggle) {
                    LocalDate inicioSemana = semanaAtual;
                    LocalDate fimSemana = semanaAtual.plusDays(6);
                    if (!hoje.isBefore(inicioSemana) && !hoje.isAfter(fimSemana)) {
                        diaSelecionado = hoje;
                        semanaAtual = hoje.with(DayOfWeek.MONDAY);
                    } else {
                        diaSelecionado = semanaAtual;
                    }
                }
                else if (oldToggle == mesToggle) {
                    LocalDate primeiroDiaMes = semanaAtual.withDayOfMonth(1);
                    LocalDate ultimoDiaMes = primeiroDiaMes.plusMonths(1).minusDays(1);
                    if (!hoje.isBefore(primeiroDiaMes) && !hoje.isAfter(ultimoDiaMes)) {
                        diaSelecionado = hoje;
                        semanaAtual = hoje.with(DayOfWeek.MONDAY);
                } else {
                        diaSelecionado = primeiroDiaMes;
                        semanaAtual = primeiroDiaMes.with(DayOfWeek.MONDAY);
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

    
    private void atualizarCalendario() {
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

        // Configurar colunas (8 colunas: 1 para horas + 7 para dias)
        for (int i = 0; i < 8; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            if (i == 0) {
                colConstraints.setPrefWidth(80); // Coluna das horas
                colConstraints.setMinWidth(80);
            } else {
                colConstraints.setHgrow(Priority.ALWAYS);
                colConstraints.setMinWidth(100);
            }
            calendarioGrid.getColumnConstraints().add(colConstraints);
        }

        // Limpar cabeçalho dos dias
        criarCabecalhoDias();

        // Criar grade de horários
        criarGradeHorarios();

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
                    final LocalDate diaClicado = data;
                    cell.setOnMouseClicked(e -> {
                        diaSelecionado = diaClicado;
                        modoAtual = ModoVisualizacao.DIA;
                        diaToggle.setSelected(true);
                        atualizarCalendario();
                    });
                } else if (data.getMonth() != primeiroDiaMes.getMonth() && data.isBefore(primeiroDiaMes)) {
                    cell.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
                    diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-font-weight: normal;");
                } else if (data.getMonth() != primeiroDiaMes.getMonth()) {
                    cell.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
                    diaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-font-weight: normal;");
                } else {
                    cell.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-cursor: hand;");
                    final LocalDate diaClicado = data;
                    cell.setOnMouseClicked(e -> {
                        diaSelecionado = diaClicado;
                        modoAtual = ModoVisualizacao.DIA;
                        diaToggle.setSelected(true);
                        atualizarCalendario();
                    });
                }
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

            Pane celula = new Pane();
            celula.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-min-height: 40;");
            celula.setPrefHeight(40);
            celula.setMaxWidth(Double.MAX_VALUE);

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
                modoAtual = ModoVisualizacao.DIA;
                diaToggle.setSelected(true);
                atualizarCalendario();
            });

            if (dataAtual.equals(hoje)) {
                vbox.setStyle("-fx-background-color: #ffb366; -fx-border-color: white; -fx-border-width: 2; -fx-padding: 8;");
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
                Pane celula = new Pane();
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
                
                // TODO: Aqui será onde vamos adicionar as marcações
                // Por enquanto, deixamos as células vazias
                
                calendarioGrid.add(celula, dia + 1, linha);

                linhaSemana.put(dia, celula);
            }

            celulasSemana.put(horaAtual, linhaSemana);
            
            linha++;
            horaAtual = horaAtual.plusMinutes(INTERVALO_MINUTOS);
        }
        
        // Configurar constraints das linhas
        for (int i = 0; i < linha; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            if (i == 0) {
                rowConstraints.setPrefHeight(60); // Cabeçalho mais alto
            } else {
                rowConstraints.setPrefHeight(40);
            }
            rowConstraints.setVgrow(Priority.NEVER);
            calendarioGrid.getRowConstraints().add(rowConstraints);
        }
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
}
