package models;

public class Utilizador {
    private String nome;
    private String password;


    // Construtores
    public Utilizador() {
        this.nome = "";
        this.password = "";
    }

    public Utilizador(String nome, String password) {
        this.nome = nome;
        this.password = password;
    }

    public Utilizador(Utilizador outro) {
        this.nome = outro.nome;
        this.password = outro.password;
    }

    // Getters e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Utilizador clone() {
        return new Utilizador(this);
    }

    @Override
    public String toString() {
        return "Utilizador{" +
                "nome='" + nome + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}