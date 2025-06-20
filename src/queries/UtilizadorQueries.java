package queries;

import models.Utilizador;

public class UtilizadorQueries {
    public boolean autenticar(String nome, String password, Utilizador utilizador) {
        return utilizador != null && 
               utilizador.getNome().equals(nome) && 
               utilizador.getPassword().equals(password);
    }

    public void alterarNome(Utilizador utilizador, String nome) {
        if (utilizador != null && nome != null && !nome.isEmpty()) {
            utilizador.setNome(nome);
        }
    }

    public void alterarPassword(Utilizador utilizador, String password) {
        if (utilizador != null && password != null && !password.isEmpty()) {
            utilizador.setPassword(password);
        }
    }
}
