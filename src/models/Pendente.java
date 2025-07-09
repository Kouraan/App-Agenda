package models;

public class Pendente {
    private String nome;
    private String numeroTelefone;

    public Pendente() {
        this.nome = "";
        this.numeroTelefone = "";
    }

    public Pendente(String nome, String numeroTelefone) {
        this.nome = nome;
        this.numeroTelefone = numeroTelefone;
    }

    public Pendente(Pendente outro) {
        this.nome = outro.nome;
        this.numeroTelefone = outro.numeroTelefone;
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

    @Override
    public Pendente clone() {
        return new Pendente(this);
    }

    @Override
    public String toString() {
        return "Pendente{" +
                "nome='" + nome + '\'' +
                ", numeroTelefone='" + numeroTelefone + '\'' +
                '}';
    }

    
}
