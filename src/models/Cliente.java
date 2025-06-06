package models;

public class Cliente {
    private String id;
    private String nome;
    private String numeroTelefone;
    private String tipoCliente;
    private int faltas;

    // Construtores
    public Cliente() {
        this.id = "";
        this.nome = "";
        this.numeroTelefone = "";
        this.tipoCliente = "";
        this.faltas = 0;
    }

    public Cliente(String id, String nome, String numeroTelefone, String tipoCLiente, int faltas) {
        this.id = id;
        this.nome = nome;
        this.numeroTelefone = numeroTelefone;
        this.tipoCliente = tipoCliente;
        this.faltas = faltas;
    }

    public Cliente(Cliente outro) {
        this.id = outro.id;
        this.nome = outro.nome;
        this.numeroTelefone = outro.numeroTelefone;
        this.tipoCliente = outro.tipoCliente;
        this.faltas = outro.faltas;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    @Override
    public Cliente clone() {
        return new Cliente(this);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "id='" + id + '\'' +
                ", nome='" + nome + '\'' +
                ", numeroTelefone='" + numeroTelefone + '\'' +
                ", tipoCliente='" + tipoCliente + '\'' +
                ", faltas=" + faltas +
                '}';
    }
}
