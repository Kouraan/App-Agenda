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
    private PaginaPrincipalController paginaPrincipalController;
    private int linhaSelecionada = -1;

    public void setPendentes(List<Pendente> pendentes) {
        this.pendentes = pendentes;
        atualizarConteudo();
    }

    public void setAppController(controller.Controller appController) {
        this.appController = appController;
    }

    public void setPaginaPrincipalController(PaginaPrincipalController controller) {
        this.paginaPrincipalController = controller;
    }

    private void atualizarConteudo() {
        rootVBox.getChildren().clear();
        if (pendentes == null || pendentes.isEmpty()) {
            Label msg = new Label("Não existe clientes pendentes, deseja adicionar um?");
            msg.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
            VBox.setMargin(msg, new Insets(0, 0, 10, 0));

            Button adicionarBtn = new Button("Adicionar");
            adicionarBtn.setPrefWidth(120);
            adicionarBtn.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-background-color: rgb(36, 43, 141); " +
                "-fx-border-color: white; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 2; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 0 6 0; " +
                "-fx-cursor: hand;"
                );
            adicionarBtn.setOnAction(e -> mostrarCriarPendente());
            
            Button btnSair = new Button("Sair");
            btnSair.setPrefWidth(120);
            btnSair.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-background-color: rgb(128, 26, 15); " +
                "-fx-border-color: white; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 2; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 0 6 0; " +
                "-fx-cursor: hand;"
                );
            btnSair.setOnAction(e -> closeWindow());

            HBox botoes = new HBox(32, adicionarBtn, btnSair);
            botoes.setAlignment(Pos.CENTER);

            rootVBox.setAlignment(Pos.CENTER);
            rootVBox.getChildren().addAll(msg, botoes);

        } else {
            Button btnAdicionar = new Button("+");
            btnAdicionar.setFocusTraversable(false);
            btnAdicionar.setStyle(
                "-fx-background-color: rgb(43, 40, 40); -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-width: 0; -fx-cursor: hand;"
            );
            btnAdicionar.setPrefWidth(36);
            btnAdicionar.setPrefHeight(36);
            btnAdicionar.setOnAction(e -> mostrarCriarPendente());

            Button btnRemover = new Button("-");
            btnRemover.setFocusTraversable(false);
            btnRemover.setStyle(
                "-fx-background-color: rgb(43,40,40); -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 0; -fx-cursor: hand;"
            );
            btnRemover.setPrefWidth(36);
            btnRemover.setPrefHeight(36);
            btnRemover.setOnAction(e -> {
                if (linhaSelecionada >= 0 && linhaSelecionada < pendentes.size()) {
                    Pendente removido = pendentes.get(linhaSelecionada);
                    utils.Logger.logPendenteRemovido(removido);
                    pendentes.remove(linhaSelecionada);
                    utils.Persistencia.guardarPendentes(pendentes);
                    linhaSelecionada = -1;
                    atualizarConteudo();
                    if (paginaPrincipalController != null) {
                        paginaPrincipalController.atualizarBoxClientesPendentes();
                    }
                }
            });

            Button btnSair = new Button("Sair");
            btnSair.setStyle(
                "-fx-background-color: rgb(128,26,15); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;" +
                "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 0; -fx-cursor: hand;"
            );
            btnSair.setPrefWidth(80);
            btnSair.setPrefHeight(36);
            btnSair.setOnAction(e -> closeWindow());

            Region espaco = new Region();
            HBox.setHgrow(espaco, Priority.ALWAYS);

            HBox barraBotoes = new HBox(12, btnAdicionar, btnRemover, espaco, btnSair);
            barraBotoes.setAlignment(Pos.CENTER_LEFT);
            barraBotoes.setPadding(new Insets(16, 0, 8, 0));

            VBox areaTabela = new VBox(10);
            areaTabela.setStyle(
                "-fx-background-color: rgb(43,40,40); -fx-background-radius: 18;"
            );
            areaTabela.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(areaTabela, Priority.ALWAYS);

            HBox cabecalho = new HBox(12);
            cabecalho.setPadding(new Insets(8, 0, 4, 0));
            cabecalho.setAlignment(Pos.CENTER);
            Label thNome = new Label("Nome");
            thNome.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(197,130,63,0.86); " +
                "-fx-text-fill: white; -fx-background-radius: 12; " +
                "-fx-padding: 10 0 10 0;"
            );
            Label thTel = new Label("Telefone");
            thTel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(197,130,63,0.86); " +
                "-fx-text-fill: white; -fx-background-radius: 12; " +
                "-fx-padding: 10 0 10 0;"
            );
            thNome.setMinWidth(200);
            thTel.setMinWidth(200);
            cabecalho.getChildren().addAll(thNome, thTel);

            VBox linhas = new VBox(8);
            for (int i = 0; i < pendentes.size(); i++) {
                Pendente p = pendentes.get(i);

                HBox linha = new HBox(12);
                linha.setAlignment(Pos.CENTER);

                Label nome = new Label(p.getNome());
                nome.setStyle(
                    "-fx-font-size: 14px; -fx-background-color: rgb(60,60,60); -fx-text-fill: white;" +
                    "-fx-background-radius: 12; -fx-border-color: rgba(197,130,63,0.86); -fx-border-radius: 12; -fx-border-width: 2; -fx-padding: 8 32 8 32;"
                );
                nome.setMinWidth(200);

                String telefone = (p.getNumeroTelefone() == null || p.getNumeroTelefone().isEmpty()) ? "-" : p.getNumeroTelefone();
                Label tel = new Label(telefone);
                tel.setStyle(
                    "-fx-font-size: 14px; -fx-background-color: rgb(60,60,60); -fx-text-fill: white;" +
                    "-fx-background-radius: 12; -fx-border-color: rgba(197,130,63,0.86); -fx-border-radius: 12; -fx-border-width: 2; -fx-padding: 8 32 8 32;"
                );
                tel.setMinWidth(200);

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

                linha.getChildren().addAll(nome, tel);
                linhas.getChildren().add(linha);
            }

            areaTabela.getChildren().addAll(cabecalho, linhas);
            VBox.setVgrow(areaTabela, Priority.ALWAYS);

            rootVBox.setAlignment(Pos.TOP_CENTER);
            rootVBox.getChildren().setAll(barraBotoes, areaTabela);
            VBox.setVgrow(areaTabela, Priority.ALWAYS);

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

        VBox box = new VBox(18);
        box.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMaxHeight(Double.MAX_VALUE);

        TextField pesquisa = new TextField();
        pesquisa.setPromptText("Pesquisar cliente...");
        pesquisa.setMaxWidth(320);
        pesquisa.setStyle(
            "-fx-background-color: rgb(43, 40, 40); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 17px; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 2; " +
            "-fx-border-color: #222; " +
            "-fx-padding: 4 16 4 16; " +
            "-fx-prompt-text-fill: #bbb; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );
        pesquisa.setFocusTraversable(false);

        ListView<String> sugestoes = new ListView<>();
        sugestoes.setMaxHeight(100);
        sugestoes.setMaxWidth(320);
        sugestoes.setVisible(false);
        sugestoes.setStyle(
            "-fx-background-color: rgb(43, 40, 40); " +
            "-fx-control-inner-background: rgb(43, 40, 40); " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 2; " +
            "-fx-border-color: #222; " +
            "-fx-padding: 0; " +
            "-fx-text-fill: white;"
        );
        sugestoes.setCellFactory(lv -> { 
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                    setStyle("-fx-background-color: rgb(43, 40, 40); -fx-text-fill: white; -fx-font-size: 15px;");
                }
            };
            cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 15px;"));
            cell.setOnMouseExited(e -> cell.setStyle("-fx-background-color: rgb(43,40,40); -fx-text-fill: white; -fx-font-size: 15px;"));
            return cell;
        });

        CheckBox desconhecido = new CheckBox("Desconhecido");
        desconhecido.setPrefHeight(24);
        desconhecido.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-text-fill: white;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;" +
            "-fx-background-insets: 0;"
        );
        desconhecido.setFocusTraversable(false);

        Label nomeLabel = new Label("Nome:");
        nomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
        Label telLabel = new Label("Número de telefone:");
        telLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        TextField nomeField = new TextField();
        nomeField.setPromptText("Nome");
        nomeField.setMaxWidth(320);
        nomeField.setStyle(
            "-fx-background-color: rgb(43,40,40);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;" +
            "-fx-border-color: #222;" +
            "-fx-prompt-text-fill: #bbb;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );

        TextField telField = new TextField();
        telField.setPromptText("Número de telefone");
        telField.setMaxWidth(320);
        telField.setStyle(
            "-fx-background-color: rgb(43,40,40);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;" +
            "-fx-border-color: #222;" +
            "-fx-prompt-text-fill: #bbb;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );

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

            List<String> nomesPendentes = pendentes.stream()
                .map(Pendente::getNome)
                .map(String::toLowerCase)
                .toList();

            List<String> nomes = appController.getClientesMap().values().stream()
                .map(models.Cliente::getNome)
                .filter(nome -> nome.toLowerCase().contains(newVal.toLowerCase()))
                .filter(nome -> !nomesPendentes.contains(nome.toLowerCase()))
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
        btnSalvar.setPrefWidth(120);
        btnSalvar.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-background-color: rgb(36, 43, 141); " +
            "-fx-background-radius: 12px; " +
            "-fx-border-width: 0; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 0 6 0; " +
            "-fx-cursor: hand;"
        );

        Button btnSair = new Button("Sair");
        btnSair.setPrefWidth(120);
        btnSair.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-background-color: rgb(128, 26, 15); " +
            "-fx-background-radius: 12px; " +
            "-fx-border-width: 0; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6 0 6 0; " +
            "-fx-cursor: hand;"
        );
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
                utils.Logger.logPendenteAdicionado(novo);
                atualizarConteudo();
                if (paginaPrincipalController != null) {
                    paginaPrincipalController.atualizarBoxClientesPendentes();
                }
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
                utils.Logger.logPendenteAdicionado(novo);
                atualizarConteudo();
                if (paginaPrincipalController != null) {
                    paginaPrincipalController.atualizarBoxClientesPendentes();
                }
            }
        });

        VBox campos = new VBox(8, nomeLabel, nomeField, telLabel, telField, erroTelefone);
        campos.setAlignment(Pos.CENTER);

        HBox botoes = new HBox(32, btnSalvar, btnSair);
        botoes.setAlignment(Pos.CENTER);

        box.setPadding(new Insets(0, 0, 0, 0));
        box.getChildren().addAll(pesquisa, sugestoes, desconhecido, campos, botoes);

        rootVBox.setAlignment(Pos.TOP_CENTER);
        rootVBox.getChildren().add(box);

        VBox.setVgrow(box, Priority.ALWAYS);

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
