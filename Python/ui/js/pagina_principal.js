import { waitForApi, getApi, getClientesMap, getMarcacoesMap, getPendentes, fazerLogout, mostrarLogin, getUtilizadorInfo } from "./utils/apiUtils.js";
import { CalendarioModule }  from "./modules/CalendarioModule.js";
import { ClientesModule }    from "./modules/ClientesModule.js";
import { PendentesModule }   from "./modules/PendentesModule.js";
import { AnotacoesModule }   from "./modules/AnotacoesModule.js";

class PaginaPrincipalController {
    constructor() {
        // Elementos DOM
        this.els = {
            userLabel:              document.getElementById("userLabel"),
            logoutBtn:              document.getElementById("logoutBtn"),
            calendarioToggle:       document.getElementById("calendarioToggle"),
            clientesToggle:         document.getElementById("clientesToggle"),
            areaCentral:            document.getElementById("areaCentral"),
            areaClientes:           document.getElementById("areaClientes"),
            clientesContent:        document.getElementById("clientesContent"),
            calendarioGrid:         document.getElementById("calendarioGrid"),
            semanaLabel:            document.getElementById("semanaLabel"),
            relogioLabel:           document.getElementById("relogioLabel"),
            anotacoesArea:          document.getElementById("anotacoesArea"),
            blurToggleBtn:          document.getElementById("blurToggleBtn"),
            caixaClientesPendentes: document.getElementById("caixaClientesPendentes"),
            todayBtn:               document.getElementById("todayBtn"),
            semanaAnteriorBtn:      document.getElementById("semanaAnteriorBtn"),
            proximaSemanaBtn:       document.getElementById("proximaSemanaBtn"),
            diaToggle:              document.getElementById("diaToggle"),
            semanaToggle:           document.getElementById("semanaToggle"),
            mesToggle:              document.getElementById("mesToggle"),
        };

        // Estado global de dados
        this.clientes  = {};
        this.marcacoes = {};
        this.pendentes = [];

        // Módulos (inicializados em init())
        this.calendarioModule = null;
        this.clientesModule   = null;
        this.pendentesModule  = null;
        this.anotacoesModule  = null;
    }

    // Arranque

    async init() {
        // Aguardar API
        const api = await waitForApi(3000, 100);

        // Carregar nome do utilizador
        if (api) {
            try {
                const info = await getUtilizadorInfo();
                const nome = info?.nome || info?.name || info?.username || null;
                this.els.userLabel.textContent = nome ? `Bem-vindo, ${nome}` : "Bem-vindo, Utilizador";
            } catch { /* não crítico */ }
        }

        // Carregar dados
        await this._carregarDados();

        // Instanciar módulos
        this._inicializarModulos();

        // Anotações
        if (api) await this.anotacoesModule.carregarAnotacoes();

        // Relógio
        this._iniciarRelogio();

        // Bindings globais (logout, toggles de navegação lateral)
        this._bindGlobal();

        // Render inicial — calendário visível por defeito
        this._mostrarCalendario();

        // Loop de highlight do slot atual
        this.calendarioModule.iniciarHighlightLoop();
    }

    // Carregamento de dados

    async _carregarDados() {
        try {
            const [clientes, marcacoes, pendentes] = await Promise.all([
                getClientesMap(),
                getMarcacoesMap(),
                getPendentes(),
            ]);
            this.clientes  = clientes  || {};
            this.marcacoes = marcacoes || {};
            this.pendentes = pendentes || [];
        } catch (e) {
            console.error("[PaginaPrincipal] Erro ao carregar dados:", e);
            this.clientes  = {};
            this.marcacoes = {};
            this.pendentes = [];
        }
    }

    /**
     * Recarrega todos os dados e propaga para os módulos que dependem deles.
     * Usado como callback pelos módulos após operações de escrita.
     */
    async _refresh() {
        await this._carregarDados();
        // Pendentes precisam de ser actualizados no módulo
        this.pendentesModule.setPendentes(this.pendentes);
    }

    // Inicialização dos módulos

    _inicializarModulos() {
        // Calendário
        this.calendarioModule = new CalendarioModule(
            this.els,
            () => this.marcacoes,           // getter de marcações
            () => this._refresh()           // callback após modificação
        );

        // Clientes
        this.clientesModule = new ClientesModule(
            this.els.clientesContent,
            () => this.clientes,            // getter de clientes
            () => this._refresh()           // callback após modificação
        );

        // Pendentes
        this.pendentesModule = new PendentesModule(
            this.els.caixaClientesPendentes,
            () => this._refresh()           // callback após modificação
        );
        this.pendentesModule.setPendentes(this.pendentes);

        // Anotações
        this.anotacoesModule = new AnotacoesModule(
            this.els.anotacoesArea,
            this.els.blurToggleBtn
        );
    }

    // Bindings globais

    _bindGlobal() {
        // Logout
        this.els.logoutBtn.addEventListener("click", () => this._handleLogout());

        // Navegação lateral
        this.els.calendarioToggle.addEventListener("click", () => this._mostrarCalendario());
        this.els.clientesToggle.addEventListener("click",   () => this._mostrarClientes());
    }

    // Navegação entre vistas

    _mostrarCalendario() {
        this.els.calendarioToggle.classList.add("active");
        this.els.clientesToggle.classList.remove("active");
        this._estiloTogglesLaterais();

        this.els.areaCentral.style.display = "flex";
        this.els.areaClientes.classList.add("hidden");

        this.calendarioModule.atualizar();
    }

    _mostrarClientes() {
        this.els.clientesToggle.classList.add("active");
        this.els.calendarioToggle.classList.remove("active");
        this._estiloTogglesLaterais();

        this.els.areaCentral.style.display = "none";
        this.els.areaClientes.classList.remove("hidden");

        this.clientesModule.renderizar();
    }

    _estiloTogglesLaterais() {
        const base   = "font-size:15px;font-weight:bold;border-radius:12px;border:none;padding:12px;min-height:40px;max-width:100%;width:100%;cursor:pointer;";
        const ativo  = base + "background-color:rgb(60,60,60);color:white;";
        const inativo = base + "background-color:rgb(43,40,40);color:white;";
        const calAtivo = this.els.calendarioToggle.classList.contains("active");
        this.els.calendarioToggle.style.cssText = calAtivo ? ativo : inativo;
        this.els.clientesToggle.style.cssText   = calAtivo ? inativo : ativo;
    }

    // Logout

    async _handleLogout() {
        await this.anotacoesModule.guardar();
        const res = await fazerLogout();
        if (res && res.success) {
            await mostrarLogin();
        } else {
            console.error("[PaginaPrincipal] Erro no logout:", res?.error);
        }
    }

    // Relógio

    _iniciarRelogio() {
        const tick = () => {
            this.els.relogioLabel.textContent = new Date().toLocaleTimeString("pt-PT", {
                hour12: false, hour: "2-digit", minute: "2-digit", second: "2-digit",
            });
        };
        tick();
        setInterval(tick, 1000);
    }
}

// Arranque

document.addEventListener("DOMContentLoaded", () => {
    window.paginaController = new PaginaPrincipalController();
    window.paginaController.init();
});