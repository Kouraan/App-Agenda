package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.util.List;
import models.Pendente;

public class PendentesController {
    @FXML private VBox rootVBox;

    private List<Pendente> pendentes;
    private controller.Controller appController;
    private int linhaSelecionada = -1;

    public void setPendentes(List<Pendente> pendentes) {
        this.pendentes = pendentes;
        atualizarConteudo();
    }

    public void setAppController(controller.Controller appController) {
        this.appController = appController;
    }

    private void atualizarConteudo() {
        rootVBox.getChildren().clear();
        if (pendentes == null || pendentes.isEmpty()) {
            Label msg = new Label("Não existe clientes pendentes, deseja adicionar um?");
            msg.setStyle("-fx-font-size: 20px; -fx-text-fill: #222; -fx-font-weight: bold;");
            
            Button adicionarBtn = new Button("Adicionar");
            adicionarBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24 8 24;");
            adicionarBtn.setOnAction(e -> mostrarCriarPendente());
            
            Button btnSair = new Button("Sair");
            btnSair.setStyle("-fx-font-size: 16px; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24 8 24;");
            btnSair.setOnAction(e -> closeWindow());

            HBox botoes = new HBox(16, adicionarBtn, btnSair);
            botoes.setAlignment(Pos.CENTER);

            rootVBox.setAlignment(Pos.CENTER);
            rootVBox.getChildren().addAll(msg, botoes);

        } else {
            Button btnAdicionar = new Button("+");
            btnAdicionar.setFocusTraversable(false);
            btnAdicionar.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            btnAdicionar.setOnAction(e -> mostrarCriarPendente());

            Button btnRemover = new Button("-");
            btnRemover.setFocusTraversable(false);
            btnRemover.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            btnRemover.setOnAction(e -> {
                if (linhaSelecionada >= 0 && linhaSelecionada < pendentes.size()) {
                    pendentes.remove(linhaSelecionada);
                    utils.Persistencia.guardarPendentes(pendentes);
                    linhaSelecionada = -1;
                    atualizarConteudo();
                }
            });

            HBox botoes = new HBox(8, btnAdicionar, btnRemover);
            botoes.setAlignment(Pos.TOP_LEFT);

            GridPane tabela = new GridPane();
            tabela.setHgap(8);
            tabela.setVgap(2);
            tabela.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 10; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
            tabela.setMaxWidth(Double.MAX_VALUE);

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(50);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(50);
            tabela.getColumnConstraints().addAll(col1, col2);

            Label thNome = new Label("Nome");
            thNome.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #d6eaf8; -fx-padding: 6 32 6 32; -fx-border-color: #bdc3c7; -fx-border-width: 0 1 1 0;");
            thNome.setMaxWidth(Double.MAX_VALUE);
            thNome.setAlignment(Pos.CENTER);

            Label thTel = new Label("Telefone");
            thTel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #d6eaf8; -fx-padding: 6 32 6 32; -fx-border-color: #bdc3c7; -fx-border-width: 0 1 1 0;");
            thTel.setMaxWidth(Double.MAX_VALUE);
            thTel.setAlignment(Pos.CENTER);

            tabela.add(thNome, 0, 0);
            tabela.add(thTel, 1, 0);

            int row = 1;
            for (int i = 0; i < pendentes.size(); i++) {
                Pendente p = pendentes.get(i);

                Label nome = new Label(p.getNome());
                nome.setStyle("-fx-font-size: 14px; -fx-padding: 4 32 4 32; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 1 0;");
                nome.setMaxWidth(Double.MAX_VALUE);
                nome.setAlignment(Pos.CENTER);

                String telefone = (p.getNumeroTelefone() == null || p.getNumeroTelefone().isEmpty()) ? "-" : p.getNumeroTelefone();
                Label tel = new Label(telefone);
                tel.setStyle("-fx-font-size: 14px; -fx-padding: 4 32 4 32; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 1 0;");
                tel.setMaxWidth(Double.MAX_VALUE);
                tel.setAlignment(Pos.CENTER);

                if (i == linhaSelecionada) {
                    nome.setStyle(nome.getStyle() + "; -fx-background-color: #1565c0; -fx-text-fill: white;");
                    tel.setStyle(tel.getStyle() + "; -fx-background-color: #1565c0; -fx-text-fill: white;");
                }

                int idx = i;
                nome.setOnMouseClicked(e -> {
                    linhaSelecionada = idx;
                    atualizarConteudo();
                });
                tel.setOnMouseClicked(e -> {
                    linhaSelecionada = idx;
                    atualizarConteudo();
                });

                tabela.add(nome, 0, row);
                tabela.add(tel, 1, row);
                row++;
            }

            Button btnSair = new Button("Sair");
            btnSair.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
            btnSair.setOnAction(e -> closeWindow());
            HBox sairBox = new HBox(btnSair);
            sairBox.setAlignment(Pos.BOTTOM_RIGHT);
            sairBox.setPadding(new Insets(20, 20, 0, 0));

            rootVBox.setAlignment(Pos.TOP_CENTER);
            rootVBox.getChildren().addAll(botoes, tabela, sairBox);

            rootVBox.setOnMouseClicked(event -> {
                if (!(event.getTarget() instanceof Label)) {
                    linhaSelecionada = -1;
                    atualizarConteudo();
                }
            });
        }
        if (rootVBox.getScene() != null) {
            rootVBox.getScene().setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ESCAPE:
                        closeWindow();
                        break;
                }
            });
        } else {
            rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.setOnKeyPressed(event -> {
                        switch (event.getCode()) {
                            case ESCAPE:
                                closeWindow();
                                break;
                        }
                    });
                }
            });
        }
    }

    private void mostrarCriarPendente() {
        rootVBox.getChildren().clear();

        VBox box = new VBox(14);
        box.setAlignment(Pos.TOP_CENTER);
        box.setStyle("-fx-background-color: white; -fx-padding: 24 0 0 0;");

        TextField pesquisa = new TextField();
        pesquisa.setPromptText("Pesquisar cliente...");
        pesquisa.setMaxWidth(320);
        pesquisa.setStyle("-fx-font-size: 15px;");

        ListView<String> sugestoes = new ListView<>();
        sugestoes.setMaxHeight(100);
        sugestoes.setMaxWidth(320);
        sugestoes.setVisible(false);

        CheckBox desconhecido = new CheckBox("Desconhecido");
        desconhecido.setStyle("-fx-font-size: 15px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;");
        desconhecido.setFocusTraversable(false);

        TextField nomeField = new TextField();
        nomeField.setPromptText("Nome");
        nomeField.setMaxWidth(320);

        TextField telField = new TextField();
        telField.setPromptText("Número de telefone");
        telField.setMaxWidth(320);

        Label erroTelefone = new Label();
        erroTelefone.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");

        nomeField.setDisable(true);
        telField.setDisable(true);

        pesquisa.setDisable(false);

        pesquisa.textProperty().addListener((obs, oldVal, newVal) -> {
            erroTelefone.setText("");
            if (desconhecido.isSelected()) {
                sugestoes.setVisible(false);
                return;
            }
            if (newVal.trim().isEmpty()) {
                sugestoes.getItems().clear();
                sugestoes.setVisible(false);
                return;
            }
            List<String> nomes = appController.getClientesMap().values().stream()
                .map(models.Cliente::getNome)
                .filter(nome -> nome.toLowerCase().contains(newVal.toLowerCase()))
                .sorted()
                .toList();
            sugestoes.getItems().setAll(nomes);
            sugestoes.setVisible(!nomes.isEmpty());
        });

        sugestoes.setOnMouseClicked(e -> {
            String selecionado = sugestoes.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                pesquisa.setText(selecionado);
                sugestoes.setVisible(false);
            }
        });

        desconhecido.selectedProperty().addListener((obs, oldVal, newVal) -> {
            nomeField.setDisable(!newVal);
            telField.setDisable(!newVal);
            pesquisa.setDisable(newVal);
            sugestoes.setVisible(false);
            erroTelefone.setText("");
        });

        Button btnSalvar = new Button("Salvar");
        btnSalvar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;-fx-font-size: 15px; -fx-font-weight: bold;");

        Button btnSair = new Button("Sair");
        btnSair.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        btnSair.setOnAction(e -> atualizarConteudo());

        btnSalvar.setOnAction(e -> {
        erroTelefone.setText("");
        if (desconhecido.isSelected()) {
            String nome = nomeField.getText().trim();
            String numero = telField.getText().trim();
            if (nome.isEmpty()) {
                erroTelefone.setText("O nome não pode ser vazio.");
                return;
            }
            String numeroParaVerificar = numero;
            boolean existe = appController.getClientesMap().values().stream()
                .anyMatch(c -> c.getNumeroTelefone().equals(numeroParaVerificar));
            if (existe) {
                erroTelefone.setText("Já existe um cliente com esse numero.");
                return;
            }
            if (numero.isEmpty()) numero = "-";
            Pendente novo = new Pendente(nome, numero);
            pendentes.add(novo);
            utils.Persistencia.guardarPendentes(pendentes);
            atualizarConteudo();
        } else {
            String pesquisaNome = pesquisa.getText().trim();
            if (pesquisaNome.isEmpty()) {
                erroTelefone.setText("Nenhum cliente selecionado.");
                return;
            }
            models.Cliente encontrado = appController.getClientesMap().values().stream()
                .filter(c -> c.getNome().equalsIgnoreCase(pesquisaNome))
                .findFirst().orElse(null);
            if (encontrado == null) {
                erroTelefone.setText("Nenhum cliente selecionado.");
                return;
            }
            Pendente novo = new Pendente(encontrado.getNome(), encontrado.getNumeroTelefone());
            pendentes.add(novo);
            utils.Persistencia.guardarPendentes(pendentes);
            atualizarConteudo();
        }
    });

    VBox campos = new VBox(6, new Label("Nome:"), nomeField, new Label("Número de telefone:"), telField, erroTelefone);
    campos.setAlignment(Pos.CENTER);

    HBox botoes = new HBox(16, btnSalvar, btnSair);
    botoes.setAlignment(Pos.CENTER);

    box.setPadding(new Insets(10, 0, 0, 0));
    box.getChildren().addAll(pesquisa, sugestoes, desconhecido, campos, botoes);

    rootVBox.setAlignment(Pos.TOP_CENTER);
    rootVBox.getChildren().add(box);

    rootVBox.requestFocus();
    rootVBox.setOnMouseClicked(event -> rootVBox.requestFocus());

    // ESC e ENTER handlers (como já tens)
    if (rootVBox.getScene() != null) {
        rootVBox.getScene().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    atualizarConteudo();
                    break;
                case ENTER:
                    btnSalvar.fire();
                    break;
            }
        });
    } else {
        rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case ESCAPE:
                            atualizarConteudo();
                            break;
                        case ENTER:
                            btnSalvar.fire();
                            break;
                    }
                });
            }
        });
    }

        
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) rootVBox.getScene().getWindow();
        stage.close();
    }
}
