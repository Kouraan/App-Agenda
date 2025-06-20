package queries;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.*;

public class MarcacoesQueries {
    private Map<LocalDateTime, Marcacao> marcacoes;

    // Construtores
    public MarcacoesQueries() {
        this.marcacoes = new HashMap<>();
    }

    public MarcacoesQueries(Map<LocalDateTime, Marcacao> marcacoes) {
        this.marcacoes = new HashMap<>(marcacoes);
    }

    public MarcacoesQueries(MarcacoesQueries outro) {
        this.marcacoes = new HashMap<>(outro.marcacoes);
    }

    // Getters e Setters
    public Map<LocalDateTime, Marcacao> getMarcacoes() {
        return new HashMap<>(marcacoes);
    }

    public void setMarcacoes(Map<LocalDateTime, Marcacao> marcacoes) {
        this.marcacoes = new HashMap<>(marcacoes);
    }

    // Métodos
    public void addMarcacao(Marcacao marcacao) {
        if (marcacao != null && marcacao.getDataHora() != null) {
            marcacoes.put(marcacao.getDataHora(), marcacao.clone());
        }
    }

    public void removeMarcacao(LocalDateTime dataHora) {
        if (dataHora != null && marcacoes.containsKey(dataHora)) {
            marcacoes.remove(dataHora);
        }
    }

    public void alterarMarcacao(LocalDateTime dataHoraMarcacao, LocalDateTime dataHora, Cliente cliente, int duracao, List<String> observacoes) {
        if (dataHoraMarcacao != null && marcacoes.containsKey(dataHoraMarcacao)) {
            Marcacao marcacao = marcacoes.get(dataHoraMarcacao);

            if (dataHora != null) marcacao.setDataHora(dataHora);
            if (cliente != null) marcacao.setCliente(cliente.clone());
            if (duracao > 0) marcacao.setDuracao(duracao);
            if (observacoes != null) marcacao.setObservacoes(observacoes);

            if (!dataHoraMarcacao.equals(marcacao.getDataHora())) {
                marcacoes.remove(dataHoraMarcacao);
                marcacoes.put(dataHora, marcacao.clone());
            } else {
                marcacoes.put(dataHoraMarcacao, marcacao.clone());
            }
        }
    }

    @Override
    public MarcacoesQueries clone() {
        return new MarcacoesQueries(this);
    }
}
