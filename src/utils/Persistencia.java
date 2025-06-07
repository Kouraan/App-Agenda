package utils;

import models.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Persistencia {
    // Parsing e criação de uma lista de Utilizadores proveniente do ficheiro JSON
    public static List<Utilizador> lerUtilizador() {
        Gson gson = new Gson();
        Type utilizadorListType = new TypeToken<List<Utilizador>>(){}.getType();
        try (FileReader reader = new FileReader("../data/utilizadores.json")) {
            List<Utilizador> utilizadores = gson.fromJson(reader, utilizadorListType);
            return utilizadores != null ? utilizadores : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
    // Parsing e criação de um Map de Clientes (chave: nome) de um ficheiro JSON
    public static Map<String, Cliente> lerClientes() throws IOException {
        Gson gson = new Gson();
        Type clienteListType = new TypeToken<List<Cliente>>(){}.getType();
        try (FileReader reader = new FileReader("../data/clientes.json")) {
            List<Cliente> clientes = gson.fromJson(reader, clienteListType);
            Map<String, Cliente> clienteMap = new HashMap<>();
            if (clientes != null) {
                for (Cliente cliente : clientes) {
                    clienteMap.put(cliente.getNome(), cliente.clone());
                }
            }
            return clienteMap;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    // Parsing e criação de um Map de Marcações (chave: dataHora) de um ficherio JSON
    public static Map<LocalDateTime, Marcacao> lerMarcacoes() {
        Gson gson = new Gson();
        Type marcacaoListType = new TypeToken<List<Marcacao>>(){}.getType();
        try (FileReader reader = new FileReader("../data/marcacoes.json")) {
            List<Marcacao> marcacoes = gson.fromJson(reader, marcacaoListType);
            Map<LocalDateTime, Marcacao> marcacaoMap = new HashMap<>();
            if (marcacoes != null) {
                for (Marcacao marcacao : marcacoes) {
                    marcacaoMap.put(marcacao.getDataHora(), marcacao.clone());
                }
            }
            return marcacaoMap;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // Guarda utilizadores no ficheiro JSON
    public static boolean guardarUtilizadores(List<Utilizador> utilizadores) {
        if (utilizadores.size() > 1) return false; // só pode haver 1
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter("../data/utilizadores.json")) {
            gson.toJson(utilizadores, writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    // Guarda clientes no ficheiro JSON
    public static boolean guardarClientes(Map<String, Cliente> clientes) {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter("../data/clientes.json")) {
            gson.toJson(clientes.values(), writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    // Guarda marcaçoes no ficheiro JSON
    public static boolean guardarMarcacoes(Map<LocalDateTime, Marcacao> marcacoes) {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter("../data/marcacoes.json")) {
            gson.toJson(marcacoes.values(), writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
