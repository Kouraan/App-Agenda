package utils;

import models.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class Parsing {
    // Parsing e criação de uma lista de Cliente proveniente dos ficheiro JSON
    public static List<Cliente> lerClientes() throws IOException {
        Gson gson = new Gson();
        Type clienteListType = new TypeToken<List<Cliente>>(){}.getType();
        try (FileReader reader = new FileReader("../data/clientes.json")) {
            return gson.fromJson(reader, clienteListType);
        }
    }
}
