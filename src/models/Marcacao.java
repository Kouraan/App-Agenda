package models;

import java.time.LocalDateTime;

public class Marcacao {
    private LocalDateTime dataHora;
    private Cliente cliente;
    private int duracao;
    private String observacoes;
    
    // Construtores
    public Marcacao() {
        this.dataHora = LocalDateTime.now();
        this.cliente = new Cliente();
        this.duracao = 0;
        this.observacoes = "";
    }

    public Marcacao(LocalDateTime dataHora, Cliente cliente, int duracao, String observacoes) {
        this.dataHora = dataHora;
        this.cliente = cliente.clone();
        this.duracao = duracao;
        this.observacoes = observacoes;
    }

    public Marcacao(Marcacao outra) {
        this.dataHora = outra.dataHora;
        this.cliente = outra.cliente.clone();
        this.duracao = outra.duracao;
        this.observacoes = outra.observacoes;
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

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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
                ", observacoes=" + observacoes +
                '}';
    }
}
