package utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Feriados {
    // Feriados fixos (dia, mês)
    private static final int[][] FIXOS = {
        {1, 1},    // Ano novo
        {25, 4},   // Dia da liberdade
        {1, 5},    // Dia do trabalhador
        {10, 6},   // Dia de Portugal
        {24, 6},   // Batalha de São Mamede
        {15, 8},   // Assunção de Nossa Senhora
        {5, 10},   // Implantação da República
        {1, 11},   // Todos os Santos
        {1, 12},   // Restauração da Independência
        {8, 12},   // Imaculada Conceição
        {25, 12}   // Natal
    };

    // Calcula a data da Páscoa para um dado ano
    public static LocalDate calcularPascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    // Retorna lista de feriados (LocalDate) para o ano dado
    public static List<LocalDate> getFeriados(int ano) {
        List<LocalDate> feriados = new ArrayList<>();
        for (int[] f : FIXOS) {
            feriados.add(LocalDate.of(ano, f[1], f[0]));
        }
        LocalDate pascoa = calcularPascoa(ano);
        feriados.add(pascoa); // Páscoa
        feriados.add(pascoa.minusDays(2)); // Sexta-feira Santa
        feriados.add(pascoa.minusDays(47)); // Carnaval
        feriados.add(pascoa.plusDays(60)); // Corpo de Deus
        return feriados;
    }

    // Verifica se uma data é feriado
    public static boolean isFeriado(LocalDate data) {
        return getFeriados(data.getYear()).contains(data);
    }
}