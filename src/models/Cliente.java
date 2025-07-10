package models;

public class Cliente {
    public enum TipoCliente {
        NORMAL,
        DESCONHECIDO,
        SEMANAL,
    }

    private String nome;
    private String numeroTelefone;
    private TipoCliente tipoCliente;
    private int faltas;
    private String diaSemana;
    private String horaCorte;
    private boolean rapido;

    // Construtores
    public Cliente() {
        this.nome = "";
        this.numeroTelefone = "";
        this.tipoCliente = TipoCliente.DESCONHECIDO;
        this.faltas = 0;
        this.diaSemana = null;
        this.horaCorte = null;
        this.rapido = false;
    }

    // Construtor para NORMAL e DESCONHECIDO
    public Cliente(String nome, String numeroTelefone, TipoCliente tipoCliente) {
        this.nome = nome;
        this.numeroTelefone = numeroTelefone;
        this.tipoCliente = tipoCliente;
        this.faltas = 0;
        this.diaSemana = null;
        this.horaCorte = null;
        this.rapido = false;
    }

    // Construtor para SEMANAL
    public Cliente(String nome, String numeroTelefone, TipoCliente tipoCliente, String diaSemana, String horaCorte, boolean rapido) {
        this.nome = nome;
        this.numeroTelefone = numeroTelefone;
        this.tipoCliente = tipoCliente;
        this.faltas = 0;
        this.diaSemana = diaSemana;
        this.horaCorte = horaCorte;
        this.rapido = rapido;
    }

    public Cliente(Cliente outro) {
        this.nome = outro.nome;
        this.numeroTelefone = outro.numeroTelefone;
        this.tipoCliente = outro.tipoCliente;
        this.faltas = outro.faltas;
        this.diaSemana = outro.diaSemana;
        this.horaCorte = outro.horaCorte;
        this.rapido = outro.rapido;
    }

    // Getters e Setters
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

    public TipoCliente getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(TipoCliente tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public int getFaltas() {
        return faltas;
    }

    public void setFaltas(int faltas) {
        this.faltas = faltas;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getHoraCorte() {
        return horaCorte;
    }

    public void setHoraCorte(String horaCorte) {
        this.horaCorte = horaCorte;
    }

    public boolean isRapido() {
        return rapido;
    }

    public void setRapido(boolean rapido) {
        this.rapido = rapido;
    }

    public boolean isTemporario() {
        return this.tipoCliente == TipoCliente.DESCONHECIDO;
    }

    @Override
    public Cliente clone() {
        return new Cliente(this);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "nome='" + nome + '\'' +
                ", numeroTelefone='" + numeroTelefone + '\'' +
                ", tipoCliente='" + tipoCliente + '\'' +
                ", faltas=" + faltas +
                (tipoCliente == TipoCliente.SEMANAL
                 ? ", diaSemana='" + diaSemana + '\'' +
                  ", horaCorte='" + horaCorte + '\''
                  : "") +
                ", rapido=" + rapido +
                '}';
    }
}
