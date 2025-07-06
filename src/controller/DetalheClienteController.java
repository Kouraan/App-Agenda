package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;

import models.Cliente;
import java.time.LocalTime;
import java.util.Map;

public class DetalheClienteController {

    @FXML private Button btnSair, btnApagar;
    @FXML private GridPane gridVisual;

    private Cliente cliente;
    private controller.PaginaPrincipalController paginaPrincipalController;

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        mostrarVisual();
    }

    public void setPaginaPrincipalController(controller.PaginaPrincipalController controller) {
        this.paginaPrincipalController = controller;
    }

    @FXML
    public void initialize() {
        btnSair.setOnAction(e -> fechar());
        btnApagar.setOnAction(e -> mostrarConfirmacaoApagar());
    }

    private void mostrarVisual() {
        gridVisual.setVisible(true); gridVisual.setManaged(true);

        gridVisual.getChildren().clear();
        int row = 0;
        addRow("Nome", cliente.getNome(), row++);
        addRow("Telefone", cliente.getNumeroTelefone(), row++);
        addRow("Tipo", cliente.getTipoCliente().toString(), row++);
        if (cliente.getTipoCliente() == Cliente.TipoCliente.SEMANAL) {
            addRow("Dia da Semana", cliente.getDiaSemana(), row++);
            addRow("Hora Corte", cliente.getHoraCorte(), row++);
        }
        addRow("Faltas", String.valueOf(cliente.getFaltas()), row);
    }

    private void addRow(String titulo, String valor, int row) {
        Label th = new Label(titulo);
        th.setStyle("-fx-background-color: #d6eaf8; -fx-text-fill: #222; -fx-font-size: 15px; -fx-font-weight: bold; "
                + "-fx-padding: 10 18 10 18; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-background-radius: 4;");
        th.setMaxWidth(Double.MAX_VALUE);
        th.setMinWidth(140);

        Label val = new Label(valor == null ? "—" : valor);
        val.setStyle("-fx-background-color: #f8fafd; -fx-font-size: 15px; -fx-padding: 10 18 10 18; "
                + "-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-background-radius: 4;");
        val.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(val, Priority.ALWAYS);
        
        gridVisual.add(th, 0, row);
        gridVisual.add(val, 1, row);
    }

    
    private void mostrarConfirmacaoApagar() {
        Stage parentStage = (Stage) btnApagar.getScene().getWindow();

        VBox box = new VBox(24);
        box.setStyle("-fx-background-color: white; -fx-padding: 32 24 24 24; -fx-border-color: #bdc3c7; -fx-border-width: 2; -fx-background-radius: 8;");
        box.setAlignment(javafx.geometry.Pos.CENTER);

        Label msg = new Label("Deseja apagar o Cliente? Esta ação é irreversível");
        msg.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c0392b; -fx-alignment: center;");
        msg.setWrapText(true);

        HBox botoes = new HBox(24);
        botoes.setAlignment(javafx.geometry.Pos.CENTER);

        Button btnNao = new Button("Não");
        btnNao.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #222; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 32 10 32;");
        Button btnSim = new Button("Sim");
        btnSim.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 32 10 32;");

        botoes.getChildren().addAll(btnNao, btnSim);
        box.getChildren().addAll(msg, botoes);

        Stage dialog = new Stage();
        dialog.initOwner(parentStage);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setResizable(false);

        double largura = parentStage.getWidth() * 0.8;
        double altura = parentStage.getHeight() * 0.25;
        dialog.setWidth(largura);
        dialog.setHeight(altura);

        dialog.setX(parentStage.getX() + (parentStage.getWidth() - largura) / 2);
        dialog.setY(parentStage.getY() + (parentStage.getHeight() - altura) / 2);

        Scene scene = new Scene(box);
        dialog.setScene(scene);

        btnNao.setOnAction(ev -> dialog.close());
        btnSim.setOnAction(ev -> {
            dialog.close();
            apagarCliente();
        });

        dialog.showAndWait();
    }

    private void apagarCliente() {
        Map<String, Cliente> clientes = paginaPrincipalController.getAppController().getClientesMap();
        clientes.remove(cliente.getNome());
        utils.Persistencia.guardarClientes(clientes);

        fechar();

        paginaPrincipalController.mostrarClientes();
    }

    private void fechar() {
        ((Stage) btnSair.getScene().getWindow()).close();
    }
}