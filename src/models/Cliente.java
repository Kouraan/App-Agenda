package models;

public class Cliente {
    private String nome;
    private String numeroTelefone;
    private String tipoCliente;
    private int faltas;

    // Construtores
    public Cliente() {
        this.nome = "";
        this.numeroTelefone = "";
        this.tipoCliente = "";
        this.faltas = 0;
    }

    public Cliente(String nome, String numeroTelefone, String tipoCLiente, int faltas) {
        this.nome = nome;
        this.numeroTelefone = numeroTelefone;
        this.tipoCliente = tipoCliente;
        this.faltas = faltas;
    }

    public Cliente(Cliente outro) {
        this.nome = outro.nome;
        this.numeroTelefone = outro.numeroTelefone;
        this.tipoCliente = outro.tipoCliente;
        this.faltas = outro.faltas;
    }
    // Construtor para cliente temporário
    public Cliente(String nome) {
        this.nome = nome;
        this.numeroTelefone = "";
        this.tipoCliente = "desconhecido";
        this.faltas = 0;
    }

    // Getters e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(String numeroTelefone) {
        this.numeroTelefone = numeroTelefone;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public int getFaltas() {
        return faltas;
    }

    public void setFaltas(int faltas) {
        this.faltas = faltas;
    }

    public boolean isTemporario() {
        return "desconhecido".equalsIgnoreCase(this.tipoCliente);
    }

    @Override
    public Cliente clone() {
        return new Cliente(this);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "nome='" + nome + '\'' +
                ", numeroTelefone='" + numeroTelefone + '\'' +
                ", tipoCliente='" + tipoCliente + '\'' +
                ", faltas=" + faltas +
                '}';
    }
}
