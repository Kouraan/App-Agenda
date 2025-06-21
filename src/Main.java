import java.time.LocalDateTime;
import java.util.Map;
import models.*;
import queries.*;
import utils.*;

public class Main {
    public static void main(String[] args) {
        Utilizador utilizador = Persistencia.lerUtilizador();
        Map<String, Cliente> clientesMap;
        Map<LocalDateTime, Marcacao> marcacoesMap;
        try {
            clientesMap = Persistencia.lerClientes();
        } catch (Exception e) {
            clientesMap = null;
        }
        marcacoesMap = Persistencia.lerMarcacoes();

        ClientesQueries clientes = new ClientesQueries();
        MarcacoesQueries marcacoes = new MarcacoesQueries();

        if ((utilizador == null)
                && (clientesMap == null || clientesMap.isEmpty())
                && (marcacoesMap == null || marcacoesMap.isEmpty())) {

        System.out.println();
        Utilizador u1 = new Utilizador("admin", "1234");
        Log log1 = new Log(LocalDateTime.now(), u1, "Novo utilizador +" + u1.getNome() + " criado");

        System.out.println("Utilizador valido: " + Validation.utilizadorValido(u1));
        System.out.println(u1.toString());
        Cliente c1 = new Cliente("Marco", "96513183", Cliente.TipoCliente.NORMAL);
        Log log2 = new Log(LocalDateTime.now(), c1, "Novo cliente +" + c1.getNome() + " criado");

        Cliente c2 = new Cliente("Nuno", "913332902", Cliente.TipoCliente.SEMANAL, "saturday", "10:00");
        Log log3 = new Log(LocalDateTime.now(), c2, "Novo cliente +" + c2.getNome() + " criado");
        
        System.out.println(c1.toString());
        System.out.println(c2.toString());
        Marcacao m1 = new Marcacao(LocalDateTime.of(2025, 6, 5, 12, 0), c2, 30, "Corte Simples");
        Log log4 = new Log(LocalDateTime.now(), m1, "Nova marcação +" + m1.getDataHora() + " criada");
        
        Marcacao m2 = new Marcacao(LocalDateTime.of(2025, 6, 6, 12, 0), c1, 30, "Corte+Barba");
        Log log5 = new Log(LocalDateTime.now(), m2, "Nova marcação +" + m2.getDataHora() + " criada");
        
        System.out.println(m1.toString());
        System.out.println(m2.toString());
        UtilizadorQueries.alterarNome(u1, "novoAdmin");
        Log log6 = new Log(LocalDateTime.now(), u1, "Utilizador teve o nome alterado para +" + u1.getNome());

        System.out.println("Nome alterado: " + u1);
        System.out.println("Nome válido: " + Validation.nomeValido(u1.getNome()));
        System.out.println("Numero de telefone válido: " + Validation.nomeValido(c1.getNumeroTelefone()));
        System.out.println("Atutenticação(certa e errada): " + 
            UtilizadorQueries.autenticar(u1.getNome(), u1.getPassword(), u1) +
            " | " + UtilizadorQueries.autenticar("admin", "1234", u1));

        System.out.println("Cliente válida: " + Validation.clienteValido(c1, clientes.getClientes()));
        System.out.println("Marcacao válida: " + Validation.marcacaoValida(m1, clientes.getClientes()));
        clientes.addCliente(c1.clone());
        clientes.addCliente(c2.clone());
        clientes.addCliente(c1.clone());
        System.out.println("Cliente duplicado: " + Validation.clienteDuplicado(clientes.getClientes(), c1.getNome(), c1.getNumeroTelefone()));
        System.out.println("Clientes: ");
        for (Cliente c : clientes.getClientes().values()) {
            System.out.println(c.toString());
        }
        clientes.removeCliente(c2.getNome());
        Log log7 = new Log(LocalDateTime.now(), c2, "Cliente +" + c2.getNome() + " removido");

        clientes.addFaltas(c1.getNome());
        Log log8 = new Log(LocalDateTime.now(), c1, "Cliente +" + c1.getNome() + " falta adicionada");

        System.out.println("Clientes após remoção e falta: ");
        for (Cliente c : clientes.getClientes().values()) {
            System.out.println(c.toString());
        }
        clientes.removeFaltas(c1.getNome());
        Log log9 = new Log(LocalDateTime.now(), c1, "Falta do cliente +" + c1.getNome() + " removida");

        clientes.addCliente(c2.clone());
        System.out.println("Clientes após remoção de falta e adição: ");
        for (Cliente c : clientes.getClientes().values()) {
            System.out.println(c.toString());
        }
        clientes.alterarCliente(c1.getNome(), "Marco Silva", null, Cliente.TipoCliente.SEMANAL, "saturday", "11:00");
        Log log10 = new Log(LocalDateTime.now(), c1, "Cliente alterou o nome para +" + c1.getNome() + " e o seu tipo para " + c1.getTipoCliente() + " com dia " + c1.getDiaSemana() + " e hora " + c1.getHoraCorte());

        clientes.alterarCliente(c2.getNome(), null, "987654321", null, null, null);
        Log log11 = new Log(LocalDateTime.now(), c2, "Cliente +" + c2.getNome() + " teve o telefone alterado para " + c2.getNumeroTelefone());

        System.out.println("Clientes após alteração de telefone: ");
        for (Cliente c : clientes.getClientes().values()) {
            System.out.println(c.toString());
        }
        marcacoes.addMarcacao(m1.clone());
        marcacoes.addMarcacao(m2.clone());
        marcacoes.addMarcacao(m1.clone());
        System.out.println("Marcacao duplicada: " + Validation.marcacaoDuplicada(marcacoes.getMarcacoes(), m1.getDataHora()));
        System.out.println("Marcacoes: ");
        for (Marcacao m : marcacoes.getMarcacoes().values()) {
            System.out.println(m.toString());
        }
        marcacoes.removeMarcacao(m2.getDataHora());
        Log log12 = new Log(LocalDateTime.now(), m2, "Marcacao +" + m2.getDataHora() + " removida");
        
        System.out.println("Marcacoes após remoção: ");
        for (Marcacao m : marcacoes.getMarcacoes().values()) {
            System.out.println(m.toString());
        }
        marcacoes.addMarcacao(m2.clone());
        System.out.println("Marcacoes após adição: ");
        for (Marcacao m : marcacoes.getMarcacoes().values()) {
            System.out.println(m.toString());
        }
        marcacoes.alterarMarcacao(m1.getDataHora(), LocalDateTime.of(2025, 6, 6, 12, 0), null, 0, null);
        System.out.println("Marcacoes após alteração: ");
        for (Marcacao m : marcacoes.getMarcacoes().values()) {
            System.out.println(m.toString());
        }
        marcacoes.alterarMarcacao(m2.getDataHora(), LocalDateTime.of(2025, 6, 7, 12, 0), null, 30, "Corte+Barba+Sobrancelhas");
        Log log13 = new Log(LocalDateTime.now(), m2, "Marcacao +" + m2.getDataHora() + " alterada para " + m2.getDataHora() + " com duração de " + m2.getDuracao() + " minutos e observações: " + m2.getObservacoes());
        
        marcacoes.alterarMarcacao(m1.getDataHora(), null, null, 0, "nada agora");
        Log log14 = new Log(LocalDateTime.now(), m1, "Marcacao +" + m1.getDataHora() + " alterada para " + m1.getDataHora() + " e observações: " + m1.getObservacoes());
        
        System.out.println("Marcacoes após alteração de data e observações: ");
        for (Marcacao m : marcacoes.getMarcacoes().values()) {
            System.out.println(m.toString());
        }

        // Registar logs
        Logger.escreverLogUtilizador(log1);
        Logger.escreverLogCliente(log2);
        Logger.escreverLogCliente(log3);
        Logger.escreverLogMarcacao(log4);
        Logger.escreverLogMarcacao(log5);
        Logger.escreverLogUtilizador(log6);
        Logger.escreverLogCliente(log7);
        Logger.escreverLogCliente(log8);
        Logger.escreverLogCliente(log9);
        Logger.escreverLogCliente(log10);
        Logger.escreverLogCliente(log11);
        Logger.escreverLogMarcacao(log12);
        Logger.escreverLogMarcacao(log13);
        Logger.escreverLogMarcacao(log14);

        // Guardar dados
        Persistencia.guardarUtilizador(u1);
        Persistencia.guardarClientes(clientes.getClientes());
        Persistencia.guardarMarcacoes(marcacoes.getMarcacoes());
        }
    }
}