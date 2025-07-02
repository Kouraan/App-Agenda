package controller;

import com.sun.jdi.VMCannotBeModifiedException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import models.Utilizador;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.Locale;
import java.util.ResourceBundle;

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
    
    private Utilizador utilizador;
    private Controller appController;
    private LocalDate semanaAtual;
    
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
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        semanaAtual = LocalDate.now().with(DayOfWeek.MONDAY);
        
        ToggleGroup group = new ToggleGroup();
        semanaToggle.setToggleGroup(group);
        mesToggle.setToggleGroup(group);
        diaToggle.setToggleGroup(group);
        semanaToggle.setSelected(true);

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd", Locale.ENGLISH);
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

                if (data.getMonth() != primeiroDiaMes.getMonth()) {
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
        
        for (int dia = 0; dia < 7; dia++) {
            LocalDate dataAtual = semanaAtual.plusDays(dia);
            String textoHeader = diasSemana[dia] + "\n" + dataAtual.format(dayFormatter);
            
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

            vbox.setStyle("-fx-background-color: #3498db; -fx-border-color: white; -fx-border-width: 1; -fx-padding: 8;");

            calendarioGrid.add(vbox, dia + 1, 0);
        }
    }

    private void atualizarCabecalho() {
        DateTimeFormatter semanaFmt = DateTimeFormatter.ofPattern("MMMM dd", Locale.ENGLISH);
        DateTimeFormatter mesFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        DateTimeFormatter diaFmt = DateTimeFormatter.ofPattern("EEEE MMMM dd", Locale.ENGLISH);
    
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
                break;
        }
        semanaLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    }
    
    private void criarGradeHorarios() {
        LocalTime horaAtual = HORA_ABERTURA;
        int linha = 1;
        
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
            }
            
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
}
