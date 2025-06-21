import models.*;
import queries.*;
import utils.*;

public class Main {
    public static void main(String[] args) {
        Utilizador u1 = new Utilizador("admin", "1234");
        System.out.println("Utilizador criado: " + u1);
        UtilizadorQueries.alterarNome(u1, "novoAdmin");
        System.out.println("Nome alterado: " + u1);
        Validation.nomeValido(u1.getNome());
        System.out.println("Nome válido: " + Validation.nomeValido(u1.getNome()));
    }
}