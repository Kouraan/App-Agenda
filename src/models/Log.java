package models;

import java.time.LocalDateTime;

public class Log {
    private LocalDateTime dataHora;
    private Cliente cliente;
    private Utilizador utilizador;
    private Marcacao marcacao;
    private String detalhes;

    // Construtores
    public Log() {
        this.dataHora = LocalDateTime.now();
        this.cliente = null;
        this.utilizador = null;
        this.marcacao = null;
        this.detalhes = "";
    }

    // apenas para utilizador
    public Log(LocalDateTime dataHora, Utilizador utilizador, String detalhes) {
        this.dataHora = dataHora;
        this.cliente = null;
        this.utilizador = utilizador;
        this.marcacao = null;
        this.detalhes = detalhes;
    }

    // apenas para cliente
    public Log(LocalDateTime dataHora, Cliente cliente, String detalhes) {
        this.dataHora = dataHora;
        this.cliente = cliente;
        this.utilizador = null;
        this.marcacao = null;
        this.detalhes = detalhes;
    }

    // apenas para marcacao
    public Log(LocalDateTime dataHora, Marcacao marcacao, String detalhes) {
        this.dataHora = dataHora;
        this.cliente = marcacao.getCliente();
        this.utilizador = null;
        this.marcacao = marcacao;
        this.detalhes = detalhes;
    }

    public Log(Log outro) {
        this.dataHora = outro.dataHora;
        this.cliente = outro.cliente;
        this.utilizador = outro.utilizador;
        this.marcacao = outro.marcacao;
        this.detalhes = outro.detalhes;
    }

    // Getters e Setters
    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public Marcacao getMarcacao() {
        return marcacao;
    }

    public void setMarcacao(Marcacao marcacao) {
        this.marcacao = marcacao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    @Override
    public Log clone() {
        return new Log(this);
    }

    @Override
    public String toString() {
        return "Log{" +
                "dataHora=" + dataHora +
                ", cliente='" + cliente + '\'' +
                ", utilizador='" + utilizador + '\'' +
                ", marcacao='" + marcacao + '\'' +
                ", detalhes='" + detalhes + '\'' +
                '}';
    }
}
