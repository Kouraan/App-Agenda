package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
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
    @FXML private Button semanaAnteriorBtn;
    @FXML private Button proximaSemanaBtn;
    @FXML private GridPane calendarioGrid;
    
    private Utilizador utilizador;
    private Controller appController;
    private LocalDate semanaAtual;
    
    // Horários de funcionamento
    private static final LocalTime HORA_ABERTURA = LocalTime.of(7, 0);
    private static final LocalTime HORA_FECHO = LocalTime.of(21, 0);
    private static final int INTERVALO_MINUTOS = 30;
    
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
        semanaAtual = LocalDate.now();
        // Ajustar para o início da semana (segunda-feira)
        semanaAtual = semanaAtual.with(DayOfWeek.MONDAY);
        
        atualizarCalendario();
    }
    
    @FXML
    private void semanaAnterior() {
        semanaAtual = semanaAtual.minusWeeks(1);
        atualizarCalendario();
    }
    
    @FXML
    private void proximaSemana() {
        semanaAtual = semanaAtual.plusWeeks(1);
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
    
    private void atualizarCalendario() {
        // Limpar grid anterior
        calendarioGrid.getChildren().clear();
        calendarioGrid.getRowConstraints().clear();
        calendarioGrid.getColumnConstraints().clear();
        
        // Atualizar label da semana
        LocalDate fimSemana = semanaAtual.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM", Locale.ENGLISH);
        semanaLabel.setText(semanaAtual.format(formatter) + " - " + fimSemana.format(formatter));
        
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
        
        // Criar cabeçalho dos dias
        criarCabecalhoDias();
        
        // Criar grade de horários
        criarGradeHorarios();
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
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        
        for (int dia = 0; dia < 7; dia++) {
            LocalDate dataAtual = semanaAtual.plusDays(dia);
            String textoHeader = diasSemana[dia] + "\n" + dataAtual.format(dayFormatter);
            
            Label diaLabel = new Label(textoHeader);
            diaLabel.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-alignment: center; " +
                             "-fx-border-color: white; -fx-border-width: 1; -fx-padding: 10;");
            diaLabel.setMaxWidth(Double.MAX_VALUE);
            diaLabel.setMaxHeight(Double.MAX_VALUE);
            diaLabel.setAlignment(Pos.CENTER);
            
            calendarioGrid.add(diaLabel, dia + 1, 0);
        }
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
