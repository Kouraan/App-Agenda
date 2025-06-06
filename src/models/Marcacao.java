package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Marcacao {
    private LocalDateTime dataHora;
    private Cliente cliente;
    private int duracao;
    private String servico;
    private List<String> observacoes;
    
    // Construtores
    public Marcacao() {
        this.dataHora = LocalDateTime.now();
        this.cliente = new Cliente();
        this.duracao = 0;
        this.servico = "";
        this.observacoes = new ArrayList<>();
    }

    public Marcacao(LocalDateTime dataHora, Cliente cliente, int duracao, String servico, List<String> observacoes) {
        this.dataHora = dataHora;
        this.cliente = cliente.clone();
        this.duracao = duracao;
        this.servico = servico;
        this.observacoes = new ArrayList<>(observacoes);
    }

    public Marcacao(Marcacao outra) {
        this.dataHora = outra.dataHora;
        this.cliente = outra.cliente.clone();
        this.duracao = outra.duracao;
        this.servico = outra.servico;
        this.observacoes = new ArrayList<>(outra.observacoes);
    }

    // Getters e Setters
    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Cliente getCliente() {
        return cliente.clone();
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente.clone();
    }

    public int getDuracao() {
        return duracao;
    }

    public void setDuracao(int duracao) {
        this.duracao = duracao;
    }

    public String getServico() {
        return servico;
    }

    public void setServico(String servico) {
        this.servico = servico;
    }

    public List<String> getObservacoes() {
        return new ArrayList<>(observacoes);
    }

    public void setObservacoes(List<String> observacoes) {
        this.observacoes = new ArrayList<>(observacoes);
    }

    @Override
    public Marcacao clone() {
        return new Marcacao(this);
    }

    @Override
    public String toString() {
        return "Marcacao{" +
                "dataHora=" + dataHora +
                ", cliente=" + cliente +
                ", duracao=" + duracao +
                ", servico='" + servico +
                ", observacoes=" + observacoes +
                '}';
    }
}
