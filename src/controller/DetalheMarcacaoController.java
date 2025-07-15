package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import models.Marcacao;

public class DetalheMarcacaoController {
    @FXML private Label tituloLabel;
    @FXML private Label nomeLabel;
    @FXML private Label telefoneLabel;
    @FXML private Label duracaoLabel;
    @FXML private Label observacoesLabel;

    private Marcacao marcacao;

    public void setMarcacao(Marcacao marcacao) {
        this.marcacao = marcacao;
        atualizarDetalhes();
    }

    @FXML
    public void initialize() {

    }

    private void atualizarDetalhes() {
        if (marcacao == null) return;

        var dataHora = marcacao.getDataHora();
        String diaSemana = dataHora.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("pt", "PT"));
        diaSemana = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1);
        String hora = dataHora.toLocalTime().toString();
        String titulo = String.format("%s dia %02d às %s", diaSemana, dataHora.getDayOfMonth(), hora);
        tituloLabel.setText(titulo);
        
        nomeLabel.setText("Nome: " + marcacao.getCliente().getNome());
        telefoneLabel.setText("Telefone: " + marcacao.getCliente().getNumeroTelefone());
        duracaoLabel.setText("Duração: " + marcacao.getDuracao() + " minutos");
        observacoesLabel.setText("Observações: " + (marcacao.getObservacoes() == null || marcacao.getObservacoes().isEmpty() ? "—" : marcacao.getObservacoes()));
    }
}