package utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.*;

public class Persistencia {
    // Gson com suporte a LocalDateTime
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                    return LocalDateTime.parse(json.getAsString());
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.toString());
                }
            })
            .setPrettyPrinting()
            .create();

    // Parsing e criação de um Utilizador proveniente do ficheiro JSON
    public static Utilizador lerUtilizador() {
        try (FileReader reader = new FileReader("data/utilizador.json")) {
            Utilizador utilizador = gson.fromJson(reader, Utilizador.class);
            return utilizador != null ? utilizador.clone() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Parsing e criação de um Map de Clientes (chave: nome) de um ficheiro JSON
    public static Map<String, Cliente> lerClientes() {
        Type clienteListType = new TypeToken<List<Cliente>>() {
        }.getType();
        try (FileReader reader = new FileReader("data/clientes.json")) {
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

    // Parsing e criação de um Map de Marcações (chave: dataHora)
    public static Map<LocalDateTime, Marcacao> lerMarcacoes() {
        Map<LocalDateTime, Marcacao> marcacaoMap = new HashMap<>();
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusMonths(6).withDayOfMonth(1);

        // Ler marcações dos últimos 6 meses até ao mês atual
        LocalDate data = inicio;
        while (!data.isAfter(hoje)) {
            String ano = String.valueOf(data.getYear());
            String mes = String.format("%02d", data.getMonthValue());
            String filePath = "data/Marcacoes/" + ano + "/marcacoes" + mes + ".json";
            try (FileReader reader = new FileReader(filePath)) {
                Type marcacaoListType = new TypeToken<List<Marcacao>>() {
                }.getType();
                List<Marcacao> marcacoes = gson.fromJson(reader, marcacaoListType);
                if (marcacoes != null) {
                    for (Marcacao m : marcacoes) {
                        LocalDateTime dt = m.getDataHora();
                        if (!dt.toLocalDate().isBefore(inicio) && !dt.toLocalDate().isAfter(hoje)) {
                            marcacaoMap.put(dt, m.clone());
                        }
                    }
                }
            } catch (Exception e) {
            }
            data = data.plusMonths(1);
        }

        int anoAtual = hoje.getYear();
        int mesAtual = hoje.getMonthValue();

        for (int mes = mesAtual; mes <= 12; mes++) {
            String ano = String.valueOf(anoAtual);
            String mesStr = String.format("%02d", mes);
            String filePath = "data/Marcacoes/" + ano + "/marcacoes" + mesStr + ".json";
            try (FileReader reader = new FileReader(filePath)) {
                Type marcacaoListType = new TypeToken<List<Marcacao>>() {
                }.getType();
                List<Marcacao> marcacoes = gson.fromJson(reader, marcacaoListType);
                if (marcacoes != null) {
                    for (Marcacao m : marcacoes) {
                        LocalDateTime dt = m.getDataHora();
                        if (dt.toLocalDate().isAfter(hoje)) {
                            marcacaoMap.put(dt, m.clone());
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        for (int ano = anoAtual + 1; ano <= anoAtual + 3; ano++) {
            for (int mes = 1; mes <= 12; mes++) {
                String anoStr = String.valueOf(ano);
                String mesStr = String.format("%02d", mes);
                String filePath = "data/Marcacoes/" + anoStr + "/marcacoes" + mesStr + ".json";
                try (FileReader reader = new FileReader(filePath)) {
                    Type marcacaoListType = new TypeToken<List<Marcacao>>() {
                    }.getType();
                    List<Marcacao> marcacoes = gson.fromJson(reader, marcacaoListType);
                    if (marcacoes != null) {
                        for (Marcacao m : marcacoes) {
                            LocalDateTime dt = m.getDataHora();
                            if (dt.toLocalDate().isAfter(hoje)) {
                                marcacaoMap.put(dt, m.clone());
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

        return marcacaoMap;
    }

    // Ler anotações do ficheiro JSON
    public static String lerAnotacoes() {
        try (FileReader reader = new FileReader("data/anotacoes.json")) {
            String anotacoes = gson.fromJson(reader, String.class);
            return anotacoes != null ? anotacoes : "";
        } catch (Exception e) {
            return "";
        }
    }

    // Parsing e criação de uma List de Pendentes de um ficheiro JSON
    public static List<Pendente> lerPendentes() {
        Type pendenteListType = new TypeToken<List<Pendente>>() {
        }.getType();
        try (FileReader reader = new FileReader("data/pendentes.json")) {
            List<Pendente> pendentes = gson.fromJson(reader, pendenteListType);
            if (pendentes != null) {
                return new ArrayList<>(pendentes);
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Guarda utilizador no ficheiro JSON
    public static boolean guardarUtilizador(Utilizador utilizador) {
        try (FileWriter writer = new FileWriter("data/utilizador.json")) {
            gson.toJson(utilizador, writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Guarda clientes no ficheiro JSON
    public static boolean guardarClientes(Map<String, Cliente> clientes) {
        List<Cliente> clientesOrdenados = clientes.values().stream()
                .sorted((c1, c2) -> c1.getNome().compareToIgnoreCase(c2.getNome()))
                .toList();
        try (FileWriter writer = new FileWriter("data/clientes.json")) {
            gson.toJson(clientesOrdenados, writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Guarda marcaçoes no ficheiro JSON
    public static boolean guardarMarcacoes(Map<LocalDateTime, Marcacao> marcacoes) {
        try {
            // Agrupar marcações por ano/mês
            Map<String, List<Marcacao>> marcacoesPorAnoMes = new HashMap<>();
            for (Marcacao marcacao : marcacoes.values()) {
                LocalDateTime dataHora = marcacao.getDataHora();
                String ano = String.valueOf(dataHora.getYear());
                String mes = String.format("%02d", dataHora.getMonthValue());
                String chave = ano + "-" + mes;
                marcacoesPorAnoMes.computeIfAbsent(chave, k -> new ArrayList<>()).add(marcacao);
            }

            // Guarda cada grupo no respectivo ficheiro
            for (String anoMes : marcacoesPorAnoMes.keySet()) {
                String[] partes = anoMes.split("-");
                String ano = partes[0];
                String mes = partes[1];
                String dirPath = "data/Marcacoes/" + ano;
                String filePath = dirPath + "/marcacoes" + mes + ".json";

                java.io.File dir = new java.io.File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                List<Marcacao> lista = marcacoesPorAnoMes.get(anoMes);
                lista.sort((m1, m2) -> m1.getDataHora().compareTo(m2.getDataHora()));

                try (FileWriter writer = new FileWriter(filePath)) {
                    gson.toJson(lista, writer);
                }
            }

            // Calcula o intervalo de meses a manter limpo
            LocalDate hoje = LocalDate.now();
            LocalDate inicio = hoje.minusMonths(6).withDayOfMonth(1);
            LocalDate fim = hoje.plusYears(3).withMonth(12).withDayOfMonth(31);

            String marcacoesRootPath = "data/Marcacoes";
            java.io.File marcacoesRoot = new java.io.File(marcacoesRootPath);
            if (marcacoesRoot.exists()) {
                for (java.io.File anoDir : marcacoesRoot.listFiles()) {
                    if (anoDir.isDirectory()) {
                        int anoInt;
                        try {
                            anoInt = Integer.parseInt(anoDir.getName());
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        for (java.io.File mesFile : anoDir.listFiles()) {
                            String nomeFicheiro = mesFile.getName();
                            if (nomeFicheiro.startsWith("marcacoes") && nomeFicheiro.endsWith(".json")) {
                                String mesStr = nomeFicheiro.substring("marcacoes".length(), nomeFicheiro.length() - 5);
                                int mesInt;
                                try {
                                    mesInt = Integer.parseInt(mesStr);
                                } catch (NumberFormatException e) {
                                    continue;
                                }
                                LocalDate dataFicheiro = LocalDate.of(anoInt, mesInt, 1);
                                // Só apaga ficheiros dentro do intervalo de leitura
                                if (!dataFicheiro.isBefore(inicio) && !dataFicheiro.isAfter(fim)) {
                                    String chave = anoDir.getName() + "-" + mesStr;
                                    if (!marcacoesPorAnoMes.containsKey(chave)) {
                                        mesFile.delete();
                                    }
                                }
                            }
                        }
                        // Se o diretório do ano ficou vazio, apaga-o
                        if (anoDir.list().length == 0) {
                            anoDir.delete();
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Guardar anotações no ficheiro JSON
    public static boolean guardarAnotacoes(String anotacoes) {
        try (FileWriter writer = new FileWriter("data/anotacoes.json")) {
            gson.toJson(anotacoes != null ? anotacoes : "", writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Guarda pendentes no ficheiro JSON
    public static boolean guardarPendentes(List<Pendente> pendentes) {
        try (FileWriter writer = new FileWriter("data/pendentes.json")) {
            gson.toJson(pendentes, writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
