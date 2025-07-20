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

    @FXML private VBox editBox;
    @FXML private CheckBox semanalCheck;
    @FXML private TextField nomeField, telefoneField;
    @FXML private ComboBox<String> diaSemanaCombo, horaCorteCombo;
    @FXML private Button btnEditar, btnVoltarVisual, btnSalvar, btnMaisFalta, btnMenosFalta, btnSair, btnApagar;
    @FXML private Label faltasLabel, nomeErrorLabel, telefoneErrorLabel, horaCorteErrorLabel, geralErrorLabel;
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
        btnSalvar.setOnAction(e -> confirmarSalvar());
        btnEditar.setOnAction(e -> alternarModoEdicao(true));
        btnVoltarVisual.setOnAction(e -> {
            alternarModoEdicao(false);
        });

        semanalCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            diaSemanaCombo.setDisable(!newVal);
            horaCorteCombo.setDisable(true);
            horaCorteCombo.getItems().clear();
        });

        diaSemanaCombo.getItems().addAll("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo");
        diaSemanaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            horaCorteCombo.getItems().clear();
            if (newVal != null) {
                Map<String, Cliente> clientes = paginaPrincipalController.getAppController().getClientesMap();
                LocalTime hora = LocalTime.of(7, 0);
                while (!hora.isAfter(LocalTime.of(21, 0))) {
                    final String horaAtual = hora.toString();
                    boolean ocupado = clientes.values().stream().anyMatch(c ->
                        c != cliente &&
                        c.getTipoCliente() == Cliente.TipoCliente.SEMANAL &&
                        newVal.equalsIgnoreCase(c.getDiaSemana()) &&
                        horaAtual.equals(c.getHoraCorte())
                    );
                    if (!ocupado) {
                        horaCorteCombo.getItems().add(horaAtual);
                    }
                    hora = hora.plusMinutes(30);
                }
                horaCorteCombo.setDisable(false);
            } else {
                horaCorteCombo.setDisable(true);
            }
        });

        btnMaisFalta.setOnAction(e -> alterarFaltas(1));
        btnMenosFalta.setOnAction(e -> alterarFaltas(-1));

        editBox.setOnMouseClicked(event -> {
            if (event.getTarget() != nomeField && event.getTarget() != telefoneField) {
                editBox.requestFocus();
            }
        });

        nomeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) nomeField.deselect();
        });
        telefoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) telefoneField.deselect();
        });

        diaSemanaCombo.setFocusTraversable(false);
        horaCorteCombo.setFocusTraversable(false);
        semanalCheck.setFocusTraversable(false);
        btnMaisFalta.setFocusTraversable(false);
        btnMenosFalta.setFocusTraversable(false);

        editBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case ESCAPE:
                            if (btnSair.isVisible() && btnSair.isManaged()) fechar();
                            break;
                        case ENTER:
                            if (btnSalvar.isVisible() && btnSalvar.isManaged()) confirmarSalvar();
                            break;
                    }
                });
            }
        });
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

        botoes.getChildren().addAll(btnSim, btnNao);
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

        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    btnNao.fire();
                    break;
                case ENTER:
                    btnSim.fire();
                    break;
            }
        });

        dialog.setScene(scene);
        btnSim.requestFocus();

        btnNao.setOnAction(ev -> dialog.close());
        btnSim.setOnAction(ev -> {
            dialog.close();
            apagarCliente();
        });

        dialog.showAndWait();
    }

    private void apagarCliente() {
        Map<String, Cliente> clientes = paginaPrincipalController.getAppController().getClientesMap();
        
        // Remover marcações futuras
        Map<java.time.LocalDateTime, models.Marcacao> marcacoes = paginaPrincipalController.getAppController().getMarcacoesMap();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        marcacoes.entrySet().removeIf(entry ->
            entry.getValue().getCliente().equals(cliente) &&
            !entry.getKey().toLocalDate().isBefore(hoje)
        );
        utils.Persistencia.guardarMarcacoes(marcacoes);

        clientes.remove(cliente.getNome());
        utils.Persistencia.guardarClientes(clientes);

        fechar();

        paginaPrincipalController.mostrarClientes();
    }

    private void fechar() {
        ((Stage) btnSair.getScene().getWindow()).close();
    }

    private void alternarModoEdicao(boolean editar) {
        gridVisual.setVisible(!editar); gridVisual.setManaged(!editar);
        editBox.setVisible(editar); editBox.setManaged(editar);

        btnApagar.setVisible(!editar); btnApagar.setManaged(!editar);
        btnSalvar.setVisible(editar); btnSalvar.setManaged(editar);

        btnEditar.setVisible(!editar); btnEditar.setManaged(!editar);
        btnVoltarVisual.setVisible(editar); btnVoltarVisual.setManaged(editar);

        if (editar) {
            preencherCamposEdicao();
        } else {
            mostrarVisual();
        }
    }

    private void preencherCamposEdicao() {
        nomeField.setText(cliente.getNome());
        telefoneField.setText(cliente.getNumeroTelefone());
        faltasLabel.setText(String.valueOf(cliente.getFaltas()));

        boolean semanal = cliente.getTipoCliente() == Cliente.TipoCliente.SEMANAL;
        semanalCheck.setSelected(semanal);
        diaSemanaCombo.setDisable(!semanal);
        horaCorteCombo.setDisable(!semanal);

        if (semanal) {
            diaSemanaCombo.setValue(cliente.getDiaSemana());
            horaCorteCombo.getItems().clear();
            if (cliente.getDiaSemana() != null) {
                Map<String, Cliente> clientes = paginaPrincipalController.getAppController().getClientesMap();
                LocalTime hora = LocalTime.of(7, 0);
                while (!hora.isAfter(LocalTime.of(21, 0))) {
                    final String horaAtual = hora.toString();
                    boolean ocupado = clientes.values().stream().anyMatch(c ->
                        c != cliente &&
                        c.getTipoCliente() == Cliente.TipoCliente.SEMANAL &&
                        cliente.getDiaSemana().equalsIgnoreCase(c.getDiaSemana()) &&
                        horaAtual.equals(c.getHoraCorte())
                    );
                    if (!ocupado || horaAtual.equals(cliente.getHoraCorte())) {
                        horaCorteCombo.getItems().add(horaAtual);
                    }
                    hora = hora.plusMinutes(30);
                }
                horaCorteCombo.setDisable(false);
            }
            horaCorteCombo.setValue(cliente.getHoraCorte());
        } else {
            diaSemanaCombo.setValue(null);
            horaCorteCombo.getItems().clear();
            horaCorteCombo.setValue(null);
        }
    }

    private void alterarFaltas(int delta) {
        int faltas = Integer.parseInt(faltasLabel.getText());
        faltas = Math.max(0, faltas + delta);
        faltasLabel.setText(String.valueOf(faltas));
    }

    private void confirmarSalvar() {
        limparErros();
    
        String nome = nomeField.getText().trim();
        String telefone = telefoneField.getText().trim();
        String diaSemana = diaSemanaCombo.getValue();
        String horaCorte = horaCorteCombo.getValue();
        boolean semanal = semanalCheck.isSelected();
        int faltas = Integer.parseInt(faltasLabel.getText());
    
        Map<String, Cliente> clientes = paginaPrincipalController.getAppController().getClientesMap();
    
        // Campos obrigatórios
        if (nome.isEmpty() || telefone.isEmpty() || (semanal && (diaSemana == null || horaCorte == null))) {
            geralErrorLabel.setText("Preencha todos os campos obrigatórios");
            geralErrorLabel.setVisible(true); geralErrorLabel.setManaged(true);
            return;
        }
    
        // Cliente Duplicado (nome ou telefone, exceto o próprio)
        if (clientes.values().stream().anyMatch(c -> c != cliente && c.getNome().equalsIgnoreCase(nome))) {
            nomeErrorLabel.setText("Já existe um cliente com esse nome.");
            nomeErrorLabel.setVisible(true); nomeErrorLabel.setManaged(true);
            return;
        }
        if (clientes.values().stream().anyMatch(c -> c != cliente && c.getNumeroTelefone().equals(telefone))) {
            telefoneErrorLabel.setText("Já existe um cliente com esse número.");
            telefoneErrorLabel.setVisible(true); telefoneErrorLabel.setManaged(true);
            return;
        }
    
        // Validação de formato
        if (!utils.Validation.nomeValido(nome)) {
            nomeErrorLabel.setText("Nome inválido.");
            nomeErrorLabel.setVisible(true); nomeErrorLabel.setManaged(true);
            return;
        }
        if (!utils.Validation.numeroTelefoneValido(telefone)) {
            telefoneErrorLabel.setText("Telefone inválido.");
            telefoneErrorLabel.setVisible(true); telefoneErrorLabel.setManaged(true);
            return;
        }
    
        // Cliente semanal: horário ocupado
        if (semanal) {
            boolean ocupado = clientes.values().stream().anyMatch(c ->
                c != cliente &&
                c.getTipoCliente() == Cliente.TipoCliente.SEMANAL &&
                diaSemana.equalsIgnoreCase(c.getDiaSemana()) &&
                horaCorte.equals(c.getHoraCorte())
            );
            if (ocupado) {
                horaCorteErrorLabel.setText("Já existe um cliente semanal nesse horário.");
                horaCorteErrorLabel.setVisible(true); horaCorteErrorLabel.setManaged(true);
                return;
            }
        }

        // Guarda estado antigo para comparação
        Cliente.TipoCliente tipoAntigo = cliente.getTipoCliente();
        String diaSemanaAntigo = cliente.getDiaSemana();
        String horaCorteAntigo = cliente.getHoraCorte();
    
        // Se nada mudou, não faz nada
        boolean igual = nome.equals(cliente.getNome()) &&
                        telefone.equals(cliente.getNumeroTelefone()) &&
                        faltas == cliente.getFaltas() &&
                        ((tipoAntigo == Cliente.TipoCliente.SEMANAL && semanal &&
                          diaSemana.equals(diaSemanaAntigo) && horaCorte.equals(horaCorteAntigo)) ||
                         (tipoAntigo != Cliente.TipoCliente.SEMANAL && !semanal));
        if (igual) {
            alternarModoEdicao(false);
            return;
        }
    
        // Confirmação
        mostrarConfirmacaoSalvar(() -> {
            // Atualiza cliente
            cliente.setNome(nome);
            cliente.setNumeroTelefone(telefone);
            cliente.setFaltas(faltas);
            if (semanal) {
                cliente.setTipoCliente(Cliente.TipoCliente.SEMANAL);
                cliente.setDiaSemana(diaSemana);
                cliente.setHoraCorte(horaCorte);
            } else {
                cliente.setTipoCliente(Cliente.TipoCliente.NORMAL);
                cliente.setDiaSemana(null);
                cliente.setHoraCorte(null);
            }
            // Atualiza mapa e persiste
            utils.Persistencia.guardarClientes(clientes);

            // Marcacoes semanais (caso seja preciso)
            Map<java.time.LocalDateTime, models.Marcacao> marcacoes = paginaPrincipalController.getAppController().getMarcacoesMap();
            java.time.LocalDate hoje = java.time.LocalDate.now();

            // Se passou a ser semanal (antes não era)
            if (tipoAntigo != Cliente.TipoCliente.SEMANAL && semanal) {
                java.util.List<models.Marcacao> novasMarcacoes = utils.MarcacoesSemanais.gerarMarcacoesSemanais(
                    cliente, marcacoes, hoje
                );
                for (models.Marcacao m : novasMarcacoes) {
                    marcacoes.put(m.getDataHora(), m);
                }
                utils.Persistencia.guardarMarcacoes(marcacoes);
            }

            // Se já era semanal e mudou o dia ou hora
            if (tipoAntigo == Cliente.TipoCliente.SEMANAL && semanal &&
                (!diaSemana.equals(diaSemanaAntigo) || !horaCorte.equals(horaCorteAntigo))) {
                // Remover marcações futuras do cliente
                marcacoes.entrySet().removeIf(entry ->
                    entry.getValue().getCliente().equals(cliente) &&
                    !entry.getKey().toLocalDate().isBefore(hoje)
                );
                // Gerar novas marcações para o novo horário
                java.util.List<models.Marcacao> novasMarcacoes = utils.MarcacoesSemanais.gerarMarcacoesSemanais(
                    cliente, marcacoes, hoje
                );
                for (models.Marcacao m : novasMarcacoes) {
                    marcacoes.put(m.getDataHora(), m);
                }
                utils.Persistencia.guardarMarcacoes(marcacoes);
            }

            // Se deixou de ser semanal, remove marcações futuras
            if (tipoAntigo == Cliente.TipoCliente.SEMANAL && !semanal) {
                marcacoes.entrySet().removeIf(entry ->
                    entry.getValue().getCliente().equals(cliente) &&
                    !entry.getKey().toLocalDate().isBefore(hoje)
                );
                utils.Persistencia.guardarMarcacoes(marcacoes);
            }
    
            // Atualiza área de clientes
            paginaPrincipalController.mostrarClientes();
    
            // Sai do modo edição
            alternarModoEdicao(false);
            mostrarVisual();
        });
    }

    private void mostrarConfirmacaoSalvar(Runnable onConfirmar) {
        Stage parentStage = (Stage) btnSalvar.getScene().getWindow();
    
        VBox box = new VBox(24);
        box.setStyle("-fx-background-color: white; -fx-padding: 32 24 24 24; -fx-border-color: #bdc3c7; -fx-border-width: 2; -fx-background-radius: 8;");
        box.setAlignment(javafx.geometry.Pos.CENTER);
    
        Label msg = new Label("Deseja salvar as alterações?");
        msg.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-alignment: center;");
        msg.setWrapText(true);
    
        HBox botoes = new HBox(24);
        botoes.setAlignment(javafx.geometry.Pos.CENTER);
    
        Button btnNao = new Button("Não");
        btnNao.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #222; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 32 10 32;");
        Button btnSim = new Button("Sim");
        btnSim.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 32 10 32;");
    
        botoes.getChildren().addAll(btnSim, btnNao);
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
        
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    btnNao.fire();
                    break;
                case ENTER:
                    btnSim.fire();
                    break;
            }
        });

        dialog.setScene(scene);
    
        btnNao.setOnAction(ev -> dialog.close());
        btnSim.setOnAction(ev -> {
            dialog.close();
            onConfirmar.run();
        });
    
        dialog.showAndWait();
    }

    private void limparErros() {
        nomeErrorLabel.setVisible(false); nomeErrorLabel.setManaged(false);
        telefoneErrorLabel.setVisible(false); telefoneErrorLabel.setManaged(false);
        horaCorteErrorLabel.setVisible(false); horaCorteErrorLabel.setManaged(false);
        geralErrorLabel.setVisible(false); geralErrorLabel.setManaged(false);
    }
}