package queries;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import models.Cliente;

public class ClientesQueries {
    private Map<String, Cliente> clientes;

    // Construtores
    public ClientesQueries() {
        this.clientes = new HashMap<>();
    }

    public ClientesQueries(Map<String, Cliente> clientes) {
        this.clientes = new HashMap<>(clientes);
    }

    public ClientesQueries(ClientesQueries outro) {
        this.clientes = new HashMap<>(outro.clientes);
    }

    // Getters e Setters
    public Map<String, Cliente> getClientes() {
        return new HashMap<>(clientes);
    }

    public void setClientes(Map<String, Cliente> clientes) {
        this.clientes = new HashMap<>(clientes);
    }

    // Métodos
    public void addCliente(Cliente cliente) {
        if (cliente != null && cliente.getNome() != null) {
            if (!utils.Validation.clienteDuplicado(clientes, cliente.getNome(), cliente.getNumeroTelefone())) {
                clientes.put(cliente.getNome(), cliente.clone());
            }
        }
    }

    public void removeCliente(String nome) {
        if (nome != null && clientes.containsKey(nome)) {
            clientes.remove(nome);
        }
    }

    public void addFaltas(String nome) {
        if (nome != null && clientes.containsKey(nome)) {
            Cliente cliente = clientes.get(nome);
            cliente.setFaltas(cliente.getFaltas() + 1);
            clientes.put(nome, cliente.clone());
        }
    }

    public void removeFaltas(String nome) {
        if (nome != null && clientes.containsKey(nome)) {
            Cliente cliente = clientes.get(nome);
            if (cliente.getFaltas() > 0) {
                cliente.setFaltas(cliente.getFaltas() - 1);
                clientes.put(nome, cliente.clone());
            }
        }
    }

    public void alterarCliente(String nomeCliente, String nome, String numeroTelefone, Cliente.TipoCliente tipo, String diaSemana, String horaCorte) {
        if (nomeCliente != null && clientes.containsKey(nomeCliente)) {
            Cliente cliente = clientes.get(nomeCliente);

            if (nome != null) cliente.setNome(nome);
            if (numeroTelefone != null) cliente.setNumeroTelefone(numeroTelefone);
            if (tipo != null && tipo != cliente.getTipoCliente()) {
                if (tipo == Cliente.TipoCliente.SEMANAL) {
                    if (diaSemana != null && horaCorte != null) {
                        try {
                            DayOfWeek dia = DayOfWeek.valueOf(diaSemana.toUpperCase());
                            LocalTime hora = LocalTime.parse(horaCorte);
                            if (utils.Validation.horaValida(hora, null) &&
                                utils.Validation.podeClienteSemanal(clientes, dia, hora)) {
                                cliente.setTipoCliente(tipo);
                                cliente.setDiaSemana(diaSemana);
                                cliente.setHoraCorte(horaCorte);
                            } else {
                                return;
                            }
                        } catch (Exception e) {
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    cliente.setTipoCliente(tipo);
                    cliente.setDiaSemana(null);
                    cliente.setHoraCorte(null);
                }
            } else if (tipo == Cliente.TipoCliente.SEMANAL) {
                if (diaSemana != null) cliente.setDiaSemana(diaSemana);
                if (horaCorte != null) {
                    try {
                        LocalTime hora = LocalTime.parse(horaCorte);
                        DayOfWeek dia = cliente.getDiaSemana() != null
                            ? DayOfWeek.valueOf(cliente.getDiaSemana().toUpperCase())
                            : null;
                        if (dia != null &&
                            utils.Validation.horaValida(hora, null) &&
                            utils.Validation.podeClienteSemanal(clientes, dia, hora)) {
                            cliente.setHoraCorte(horaCorte);
                        }
                    } catch (Exception e) {
                        System.err.println("Erro" + e.getMessage());
                    }
                }
            }
            if (!nomeCliente.equals(cliente.getNome())) {
                clientes.remove(nomeCliente);
                clientes.put(cliente.getNome(), cliente.clone());
            } else {
                clientes.put(nomeCliente, cliente.clone());
            }
        }
    }

    @Override
    public ClientesQueries clone() {
        return new ClientesQueries(this);
    }
}