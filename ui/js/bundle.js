// ==========================================
// 1. dateUtils.js
// ==========================================

const FERIADOS_FIXOS = [
    "01-01", "04-25", "05-01", "06-10", "06-24", "08-15",
    "10-05", "11-01", "12-01", "12-08", "12-25"
];

function calcularPascoa(ano) {
    const a = ano % 19;
    const b = Math.floor(ano / 100);
    const c = ano % 100;
    const d = Math.floor(b / 4);
    const e = b % 4;
    const f = Math.floor((b + 8) / 25);
    const g = Math.floor((b - f + 1) / 3);
    const h = (19 * a + b - d - g + 15) % 30;
    const i = Math.floor(c / 4);
    const k = c % 4;
    const l = (32 + 2 * e + 2 * i - h - k) % 7;
    const m = Math.floor((a + 11 * h + 22 * l) / 451);
    const mes = Math.floor((h + l - 7 * m + 114) / 31);
    const dia = ((h + l - 7 * m + 114) % 31) + 1;
    return new Date(ano, mes - 1, dia);
}

function getFeriados(ano) {
    const feriados = new Set(FERIADOS_FIXOS);
    const pascoa = calcularPascoa(ano);
    const adicionar = (data) => {
        const mm = String(data.getMonth() + 1).padStart(2, "0");
        const dd = String(data.getDate()).padStart(2, "0");
        feriados.add(`${mm}-${dd}`);
    };
    adicionar(pascoa);
    const sextaSanta = new Date(pascoa); sextaSanta.setDate(pascoa.getDate() - 2); adicionar(sextaSanta);
    const carnaval = new Date(pascoa);   carnaval.setDate(pascoa.getDate() - 47);  adicionar(carnaval);
    const corpoDeus = new Date(pascoa);  corpoDeus.setDate(pascoa.getDate() + 60); adicionar(corpoDeus);
    return feriados;
}

function isHoliday(date) {
    const feriados = getFeriados(date.getFullYear());
    const mm = String(date.getMonth() + 1).padStart(2, "0");
    const dd = String(date.getDate()).padStart(2, "0");
    return feriados.has(`${mm}-${dd}`);
}

function isToday(date) {
    const today = new Date();
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
}

function isSunday(date) { return date.getDay() === 0; }

function isPast(date) { return date < new Date(); }

function getMonday(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    d.setHours(0, 0, 0, 0);
    return d;
}

function timeToMinutes(hhmm) {
    if (!hhmm) return null;
    const parts = String(hhmm).split(":");
    const h = parseInt(parts[0], 10);
    const m = parseInt(parts[1], 10);
    if (isNaN(h) || isNaN(m)) return null;
    return h * 60 + m;
}

function gerarHoras(step = 30) {
    const horas = [];
    for (let h = 7; h <= 21; h++) {
        for (let m = 0; m < 60; m += step) {
            if (h === 21 && m > 0) continue;
            horas.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
        }
    }
    return horas;
}

function getCurrentSlotDate() {
    const now = new Date();
    const roundedMin = now.getMinutes() < 30 ? 0 : 30;
    return new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), roundedMin, 0, 0);
}

// Converte Date local para string ISO local (sem UTC offset)
function toLocalISOString(date) {
    const y  = date.getFullYear();
    const mo = String(date.getMonth() + 1).padStart(2, "0");
    const d  = String(date.getDate()).padStart(2, "0");
    const h  = String(date.getHours()).padStart(2, "0");
    const mi = String(date.getMinutes()).padStart(2, "0");
    const s  = String(date.getSeconds()).padStart(2, "0");
    return `${y}-${mo}-${d}T${h}:${mi}:${s}`;
}

// Converte string ISO (possivelmente UTC) para Date local
function parseISOToLocal(isoStr) {
    if (!isoStr) return null;
    // Se termina em Z ou tem offset, usa new Date normalmente (converte para local)
    // Se não tem info de timezone, trata como local
    if (isoStr.includes("Z") || /[+-]\d{2}:\d{2}$/.test(isoStr)) {
        return new Date(isoStr);
    }
    // sem timezone — tratar como local
    return new Date(isoStr);
}

// Chave de lookup no mapa de marcações (ISO local sem Z)
function marcacaoKey(date) {
    return toLocalISOString(date);
}


// ==========================================
// 2. apiUtils.js
// ==========================================

async function waitForApi(timeout = 3000, interval = 100) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
        const api = (window.pywebview && window.pywebview.api) ||
                    (typeof pywebview !== "undefined" && pywebview && pywebview.api) || null;
        if (api) return api;
        await new Promise((res) => setTimeout(res, interval));
    }
    return null;
}

function getApi() {
    return (window.pywebview && window.pywebview.api) ||
           (typeof pywebview !== "undefined" && pywebview && pywebview.api) || null;
}

async function callApi(fn, fallback = null) {
    const api = getApi();
    if (!api) { console.warn("[apiUtils] API não disponível."); return fallback; }
    try { return await fn(api); }
    catch (e) { console.error("[apiUtils] Erro:", e); return fallback; }
}

async function getUtilizadorInfo()               { return callApi(api => api.get_utilizador_info(), null); }
async function fazerLogout()                      { return callApi(api => api.fazer_logout(), {success:false}); }
async function mostrarLogin()                     { return callApi(api => api.mostrar_login(), {success:false}); }
async function getClientesMap()                   { return callApi(api => api.get_clientes_map(), {}); }
async function getMarcacoesMap()                  { return callApi(api => api.get_marcacoes_map(), {}); }
async function getPendentes()                     { return callApi(api => api.get_pendentes(), []); }
async function lerAnotacoes()                     { return callApi(api => api.ler_anotacoes(), {success:false, texto:""}); }
async function guardarAnotacoes(texto)            { return callApi(api => api.guardar_anotacoes(texto), {success:false}); }
async function getCliente(nome)                   { return callApi(api => api.get_cliente(nome), {success:false}); }
async function adicionarCliente(obj)              { return callApi(api => api.adicionar_cliente(obj), {success:false}); }
async function alterarCliente(payload)            { return callApi(api => api.alterar_cliente(payload), {success:false}); }
async function apagarCliente(nome)                { return callApi(api => api.apagar_cliente(nome), {success:false}); }
async function adicionarPendente(nome, numero)    { return callApi(api => api.adicionar_pendente(nome, numero||""), {success:false}); }
async function removerPendente(nome)              { return callApi(api => api.remover_pendente(nome), {success:false}); }
async function guardarPendentesLista(lista)       { return callApi(api => api.guardar_pendentes_lista(lista), {success:false}); }


// ==========================================
// 3. AnotacoesModule.js
// ==========================================

class AnotacoesModule {
    constructor(anotacoesArea, blurToggleBtn) {
        this.area    = anotacoesArea;
        this.btn     = blurToggleBtn;
        this.blurred = true;
        this._bind();
        this._aplicarBlur(true);
    }

    async carregarAnotacoes() {
        const res = await lerAnotacoes();
        if (res && res.success) this.area.value = res.texto || "";
    }

    toggle() { this.blurred = !this.blurred; this._aplicarBlur(this.blurred); }

    _aplicarBlur(blur) {
        if (blur) {
            this.area.classList.add("blurred");
            this.btn.textContent = "👁";
            this.area.disabled   = true;
        } else {
            this.area.classList.remove("blurred");
            this.btn.textContent = "⛔";
            this.area.disabled   = false;
            this.area.focus();
        }
    }

    async guardar() { await guardarAnotacoes(this.area.value); }

    _bind() {
        this.btn.addEventListener("click", () => this.toggle());
        this.area.addEventListener("blur", () => this.guardar());
    }
}


// ==========================================
// 4. CalendarioModule.js
// ==========================================

const HORA_ABERTURA     = 7;
const HORA_FECHO        = 21;
const INTERVALO_MINUTOS = 30;
const DIAS_SEMANA_CURTO = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"];
const DIAS_SEMANA_LONGO = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];

class CalendarioModule {
    constructor(els, getMarcacoes, onRefresh) {
        this.grid          = els.calendarioGrid;
        this.semanaLabel   = els.semanaLabel;
        this.todayBtn      = els.todayBtn;
        this.prevBtn       = els.semanaAnteriorBtn;
        this.nextBtn       = els.proximaSemanaBtn;
        this.diaToggle     = els.diaToggle;
        this.semanaToggle  = els.semanaToggle;
        this.mesToggle     = els.mesToggle;
        this.getMarcacoes  = getMarcacoes;
        this.onRefresh     = onRefresh;

        this.modoAtual      = "SEMANA";
        this.semanaAtual    = getMonday(new Date());
        this.diaSelecionado = new Date();

        this._bindNav();
    }

    atualizar() {
        // Limpar cabeçalho fixo anterior se existir
        const scrollEl = this.grid.closest(".calendar-scroll");
        
        if (scrollEl) {
            const headerFixo = scrollEl.parentElement.querySelector(".semana-header-fixo");
            if (headerFixo) headerFixo.remove();

            // Se o grid está dentro de um inner, movê-lo de volta para scrollEl
            const inner = scrollEl.querySelector(".calendar-scroll-inner");
            if (inner) {
                if (inner.contains(this.grid)) {
                    scrollEl.insertBefore(this.grid, inner);
                }
                inner.remove();
            }

            // Repor scroll no scrollEl
            scrollEl.style.overflowY = "";
            scrollEl.style.overflow = "";
        }

        this.grid.innerHTML = "";
        this.grid.className = `calendar-grid ${this.modoAtual.toLowerCase()}`;

        if (scrollEl) {
            if (this.modoAtual === "DIA" || this.modoAtual === "SEMANA") {
                scrollEl.style.overflowY = "auto";
                scrollEl.style.scrollbarWidth = "none";
            } else {
                scrollEl.style.overflowY = "hidden";
            }
        }

        switch (this.modoAtual) {
            case "SEMANA": this._criarSemana(); break;
            case "MES":    this._criarMes();    break;
            case "DIA":    this._criarDia();    break;
        }
        this._atualizarLabel();
        this._atualizarEstiloToggles();
    }
    setModo(modo) {
        this.modoAtual = modo;
        if (modo === "MES") {
            this.semanaAtual = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth(), 1);
        }
        this.atualizar();
    }

    iniciarHighlightLoop() {
        this._updateCurrentSlotHighlight();
        const now    = new Date();
        const msTo   = ((now.getMinutes() < 30 ? 30 : 60) - now.getMinutes()) * 60000
                       - now.getSeconds() * 1000 - now.getMilliseconds();
        setTimeout(() => {
            this._updateCurrentSlotHighlight();
            setInterval(() => this._updateCurrentSlotHighlight(), 30 * 60000);
        }, msTo);
    }

    _bindNav() {
        this.todayBtn.addEventListener("click",      () => this._hoje());
        this.prevBtn.addEventListener("click",       () => this._anterior());
        this.nextBtn.addEventListener("click",       () => this._proximo());
        this.diaToggle.addEventListener("click",     () => this.setModo("DIA"));
        this.semanaToggle.addEventListener("click",  () => this.setModo("SEMANA"));
        this.mesToggle.addEventListener("click",     () => this.setModo("MES"));
    }

    _hoje() {
        const hoje = new Date();
        this.semanaAtual    = getMonday(hoje);
        this.diaSelecionado = hoje;
        this.atualizar();
    }

    _anterior() {
        if (this.modoAtual === "SEMANA")       this.semanaAtual = new Date(this.semanaAtual.getTime() - 7 * 86400000);
        else if (this.modoAtual === "MES")     this.semanaAtual = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() - 1, 1);
        else { this.diaSelecionado = new Date(this.diaSelecionado.getTime() - 86400000); this.semanaAtual = getMonday(this.diaSelecionado); }
        this.atualizar();
    }

    _proximo() {
        if (this.modoAtual === "SEMANA")       this.semanaAtual = new Date(this.semanaAtual.getTime() + 7 * 86400000);
        else if (this.modoAtual === "MES")     this.semanaAtual = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() + 1, 1);
        else { this.diaSelecionado = new Date(this.diaSelecionado.getTime() + 86400000); this.semanaAtual = getMonday(this.diaSelecionado); }
        this.atualizar();
    }

    // ── Semana ────────────────────────────────────────────────────────────────

    _criarSemana() {
        const scrollEl = this.grid.closest(".calendar-scroll");

        // Criar cabeçalho fixo fora do scroll
        const headerFixo = document.createElement("div");
        headerFixo.className = "semana-header-fixo";

        // Célula vazia topo-esquerda
        headerFixo.appendChild(this._celula("", "header"));

        // Cabeçalhos dos dias
        for (let i = 0; i < 7; i++) {
            const data  = new Date(this.semanaAtual.getTime() + i * 86400000);
            const texto = `${DIAS_SEMANA_CURTO[i]} ${String(data.getDate()).padStart(2, "0")}`;
            const cel   = this._celula(texto, "header");
            if      (isToday(data))    cel.classList.add("today");
            else if (isSunday(data))   cel.classList.add("sunday");
            else if (isHoliday(data))  cel.classList.add("holiday");
            cel.style.cursor = "pointer";
            cel.addEventListener("click", () => {
                this.diaSelecionado = data;
                this.setModo("DIA");
            });
            headerFixo.appendChild(cel);
        }

        // Inserir header fixo ANTES do calendar-scroll no DOM
        if (scrollEl) {
            scrollEl.parentElement.insertBefore(headerFixo, scrollEl);

            // Envolver o grid num inner scrollável
            const inner = document.createElement("div");
            inner.className = "calendar-scroll-inner";
            inner.appendChild(this.grid);
            scrollEl.appendChild(inner);
        }

        // Linhas de horas (sem os cabeçalhos — ficam no headerFixo)
        for (let h = HORA_ABERTURA; h <= HORA_FECHO; h++) {
            for (let m = 0; m < 60; m += INTERVALO_MINUTOS) {
                const horaStr = `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
                this.grid.appendChild(this._celula(horaStr, "hour"));
                for (let d = 0; d < 7; d++) {
                    const data     = new Date(this.semanaAtual.getTime() + d * 86400000);
                    const dataHora = new Date(data.getFullYear(), data.getMonth(), data.getDate(), h, m);
                    this.grid.appendChild(this._celulaHorario(dataHora));
                }
            }
        }
    }

    // ── Mês ───────────────────────────────────────────────────────────────────

    _criarMes() {
        const header = document.createElement("div");
        header.className = "mes-header";
        DIAS_SEMANA_CURTO.forEach(d => header.appendChild(this._celula(d, "header")));
        this.grid.appendChild(header);

        const weeksWrap = document.createElement("div");
        weeksWrap.className = "mes-weeks";

        const primeiroDia = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth(), 1);
        let offset = primeiroDia.getDay();
        if (offset === 0) offset = 7;
        const inicio = new Date(primeiroDia);
        inicio.setDate(inicio.getDate() - (offset - 1));

        const ultimo    = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() + 1, 0);
        let offsetFim   = ultimo.getDay();
        if (offsetFim === 0) offsetFim = 7;
        const fim = new Date(ultimo);
        fim.setDate(fim.getDate() + (7 - offsetFim));

        const cur = new Date(inicio);
        while (cur <= fim) {
            const semanaRow = document.createElement("div");
            semanaRow.className = "mes-week";
            for (let d = 0; d < 7; d++) {
                const cel    = this._celula(String(cur.getDate()), "");
                const clique = new Date(cur);
                if      (isToday(cur))    cel.classList.add("today");
                else if (isSunday(cur))   cel.classList.add("sunday");
                else if (isHoliday(cur))  cel.classList.add("holiday");
                if (cur.getMonth() !== this.semanaAtual.getMonth()) {
                    cel.style.opacity = "0.4"; cel.style.fontSize = "16px";
                }
                cel.style.cursor = "pointer";
                cel.addEventListener("click", () => {
                    this.diaSelecionado = clique;
                    this.semanaAtual    = getMonday(clique);
                    this.setModo("DIA");
                });
                semanaRow.appendChild(cel);
                cur.setDate(cur.getDate() + 1);
            }
            weeksWrap.appendChild(semanaRow);
        }
        this.grid.appendChild(weeksWrap);
    }

    // ── Dia ───────────────────────────────────────────────────────────────────

    _criarDia() {
        for (let h = HORA_ABERTURA; h <= HORA_FECHO; h++) {
            for (let m = 0; m < 60; m += INTERVALO_MINUTOS) {
                const horaStr = `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
                this.grid.appendChild(this._celula(horaStr, "hour"));
                const dataHora = new Date(
                    this.diaSelecionado.getFullYear(), this.diaSelecionado.getMonth(),
                    this.diaSelecionado.getDate(), h, m);
                this.grid.appendChild(this._celulaHorario(dataHora));
            }
        }
    }

    // ── Célula de horário (bloco principal) ───────────────────────────────────

    _celulaHorario(dataHora) {
        const cel      = document.createElement("div");
        cel.className  = "calendar-cell";
        const isoLocal = toLocalISOString(dataHora);
        cel.setAttribute("data-datetime", isoLocal);

        const passado = isPast(dataHora);
        if (passado) cel.classList.add("past");

        // Highlight do bloco actual
        const slotAtual = getCurrentSlotDate();
        if (toLocalISOString(slotAtual) === isoLocal) cel.classList.add("current-slot");

        // Estilo domingo
        if (dataHora.getDay() === 0) cel.classList.add("sunday-cell");

        const marcacoes = this.getMarcacoes();
        const marc1     = this._getMarcacao(marcacoes, dataHora);
        const marc2     = this._getMarcacao(marcacoes, new Date(dataHora.getTime() + 15 * 60000));
        const is15m1    = marc1 && marc1.duracao === 15;
        const is15m2    = marc2 && marc2.duracao === 15;

        if (is15m1 || is15m2) {
            const hbox = document.createElement("div");
            hbox.className = "marcacao-15-container";

            if (is15m1) {
                hbox.appendChild(this._boxMarcacao(marc1, true));
            } else {
                const r = document.createElement("div");
                r.className = "marcacao-15-slot-vazio";
                if (!passado) r.addEventListener("click", () => this._abrirCriarMarcacao(dataHora));
                r.style.cursor = passado ? "default" : "pointer";
                hbox.appendChild(r);
            }
            if (is15m2) {
                hbox.appendChild(this._boxMarcacao(marc2, true));
            } else {
                const r = document.createElement("div");
                r.className = "marcacao-15-slot-vazio";
                const dt15 = new Date(dataHora.getTime() + 15 * 60000);
                if (!passado) r.addEventListener("click", () => this._abrirCriarMarcacao(dt15));
                r.style.cursor = passado ? "default" : "pointer";
                hbox.appendChild(r);
            }
            cel.appendChild(hbox);
        } else if (marc1 && marc1.duracao >= 30) {
            // Marcação de 30+ min ocupa a célula inteira
            cel.appendChild(this._boxMarcacao(marc1, false));
            cel.style.cursor = "pointer";

        } else {
            // Célula vazia
            if (!passado) {
                cel.style.cursor = "pointer";
                cel.addEventListener("click", () => this._abrirCriarMarcacao(dataHora));
                cel.addEventListener("mouseenter", () => cel.classList.add("hover-cell"));
                cel.addEventListener("mouseleave", () => cel.classList.remove("hover-cell"));
            } else {
                cel.style.cursor = "default";
            }
        }

        return cel;
    }

    /** Procura a marcação cujo dataHora coincide com a data dada (por ISO local). */
    _getMarcacao(marcacoes, date) {
        const key = toLocalISOString(date);
        // Tentar chave exacta
        if (marcacoes[key]) return marcacoes[key];
        // Tentar variantes ISO que o backend possa devolver
        const keyZ = date.toISOString();
        if (marcacoes[keyZ]) return marcacoes[keyZ];
        // Fallback: iterar (lento mas seguro)
        for (const [k, v] of Object.entries(marcacoes)) {
            const dt = parseISOToLocal(k);
            if (dt && Math.abs(dt.getTime() - date.getTime()) < 1000) return v;
        }
        return null;
    }

    // ── Box de marcação (visual) ──────────────────────────────────────────────

    _boxMarcacao(marcacao, meia) {
        const wrap = document.createElement("div");
        wrap.className = meia ? "marcacao-15-slot" : "marcacao-wrap-full";

        const box      = document.createElement("div");
        const isFalta  = marcacao.falta;
        box.className  = `marcacao${isFalta ? " falta" : ""}`;

        const label = document.createElement("div");
        label.textContent = marcacao.cliente ? marcacao.cliente.nome : "—";
        label.className   = "marcacao-label";

        box.appendChild(label);
        box.addEventListener("click", (e) => {
            e.stopPropagation();
            this._abrirDetalheMarcacao(marcacao);
        });
        wrap.appendChild(box);
        return wrap;
    }

    // ── Modal: Criar Marcação ─────────────────────────────────────────────────

    _abrirCriarMarcacao(dataHora) {
        if (document.getElementById("marcacao-overlay")) return;

        const overlay = this._criarOverlay("marcacao-overlay");
        document.body.appendChild(overlay);
        const modal   = this._criarModal("360px");
        overlay.appendChild(modal);

        const titulo = document.createElement("div");
        const dias   = ["Domingo","Segunda","Terça","Quarta","Quinta","Sexta","Sábado"];
        titulo.textContent = `${dias[dataHora.getDay()]} dia ${String(dataHora.getDate()).padStart(2,"0")} às ${String(dataHora.getHours()).padStart(2,"0")}:${String(dataHora.getMinutes()).padStart(2,"0")}`;
        titulo.style.cssText = "color:white;font-size:17px;font-weight:bold;text-align:center;padding-bottom:8px;";
        modal.appendChild(titulo);

        // Pesquisa cliente
        const pesquisa = this._mkInput("Pesquisar cliente...");
        modal.appendChild(pesquisa);

        const sugestoes = document.createElement("div");
        sugestoes.style.cssText = "background:white;border-radius:8px;max-height:90px;overflow-y:auto;display:none;margin-bottom:4px;";
        modal.appendChild(sugestoes);

        // Checkbox desconhecido
        const chkRow  = document.createElement("label");
        chkRow.style.cssText = "display:flex;align-items:center;gap:8px;color:white;font-size:15px;cursor:pointer;";
        const chkDesk = document.createElement("input"); chkDesk.type = "checkbox";
        chkRow.append(chkDesk, document.createTextNode(" Desconhecido"));
        modal.appendChild(chkRow);

        const lblNome = this._mkLabel("Nome:");
        const fldNome = this._mkInput("Nome"); fldNome.disabled = true;
        const lblTel  = this._mkLabel("Telefone:");
        const fldTel  = this._mkInput("Número de telefone"); fldTel.disabled = true;
        fldTel.addEventListener("input", () => { fldTel.value = fldTel.value.replace(/[^\d+]/g, ""); });
        modal.append(lblNome, fldNome, lblTel, fldTel);

        const lblDur = this._mkLabel("Duração:");
        const selDur = document.createElement("select");
        selDur.style.cssText = "width:100%;padding:6px 8px;border-radius:8px;border:none;font-size:14px;background:white;";
        modal.append(lblDur, selDur);

        const lblObs = this._mkLabel("Observações:");
        const txaObs = document.createElement("textarea");
        txaObs.placeholder   = "Observações (opcional)";
        txaObs.rows          = 3;
        txaObs.style.cssText = "width:100%;padding:6px 8px;border-radius:8px;border:none;font-size:14px;box-sizing:border-box;resize:none;";
        modal.append(lblObs, txaObs);

        const errorEl = document.createElement("div");
        errorEl.style.cssText = "color:#ff8080;font-size:13px;min-height:16px;";
        modal.appendChild(errorEl);

        const btnRow    = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:12px;justify-content:center;margin-top:8px;";
        const btnSalvar = this._mkBtn("Salvar", "rgb(36,43,141)");
        const btnSair   = this._mkBtn("Sair",   "rgb(128,26,15)");
        btnRow.append(btnSalvar, btnSair);
        modal.appendChild(btnRow);

        // Carregar clientes
        let clientesSnapshot = {};
        const api = getApi();
        if (api) {
            api.get_clientes_map().then(m => {
                clientesSnapshot = m || {};
                this._popularDuracoes(selDur, dataHora, this.getMarcacoes());
            });
        } else {
            this._popularDuracoes(selDur, dataHora, this.getMarcacoes());
        }

        // Pesquisa dinâmica com sugestões
        pesquisa.addEventListener("input", () => {
            if (chkDesk.checked) { sugestoes.style.display = "none"; return; }
            const val = pesquisa.value.trim().toLowerCase();
            if (!val) { sugestoes.style.display = "none"; return; }
            const matches = Object.keys(clientesSnapshot).filter(n => n.toLowerCase().includes(val));
            sugestoes.innerHTML = "";
            if (!matches.length) { sugestoes.style.display = "none"; return; }
            sugestoes.style.display = "block";
            matches.forEach(n => {
                const item = document.createElement("div");
                item.textContent  = n;
                item.style.cssText = "padding:6px 10px;cursor:pointer;font-size:13px;color:black;border-bottom:1px solid #eee;";
                item.addEventListener("mousedown", (e) => {
                    e.preventDefault(); // impede blur no pesquisa
                    pesquisa.value = n;
                    sugestoes.style.display = "none";
                });
                sugestoes.appendChild(item);
            });
        });

        // Fechar sugestões ao clicar fora
        pesquisa.addEventListener("blur", () => {
            setTimeout(() => { sugestoes.style.display = "none"; }, 150);
        });

        chkDesk.addEventListener("change", () => {
            fldNome.disabled   = !chkDesk.checked;
            fldTel.disabled    = !chkDesk.checked;
            pesquisa.disabled  = chkDesk.checked;
            sugestoes.style.display = "none";
            if (!chkDesk.checked) { fldNome.value = ""; fldTel.value = ""; }
        });

        const closeModal = () => {
            if (document.body.contains(overlay)) document.body.removeChild(overlay);
            window.removeEventListener("keydown", keyH, true);
        };
        const keyH = (e) => {
            if (!document.body.contains(overlay)) return;
            if (e.key === "Escape") { e.preventDefault(); closeModal(); }
            if (e.key === "Enter")  { e.preventDefault(); btnSalvar.click(); }
        };
        window.addEventListener("keydown", keyH, true);
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeModal(); });
        btnSair.addEventListener("click", closeModal);

        btnSalvar.addEventListener("click", async () => {
            errorEl.textContent = "";
            if (!api) { errorEl.textContent = "API não disponível."; return; }

            const duracao = parseInt(selDur.value, 10);
            if (!duracao) { errorEl.textContent = "Selecione a duração."; return; }

            const dhStr = toLocalISOString(dataHora);

            try {
                btnSalvar.disabled = true;
                let res;
                if (chkDesk.checked) {
                    const nome   = fldNome.value.trim();
                    const numero = fldTel.value.trim();
                    if (!nome) { errorEl.textContent = "Nome é obrigatório."; btnSalvar.disabled = false; return; }
                    res = await api.criar_marcacao_desconhecido(nome, numero, dhStr, duracao, txaObs.value.trim());
                } else {
                    const clienteNome = pesquisa.value.trim();
                    if (!clienteNome) { errorEl.textContent = "Selecione um cliente."; btnSalvar.disabled = false; return; }
                    if (!clientesSnapshot[clienteNome]) { errorEl.textContent = "Cliente não encontrado."; btnSalvar.disabled = false; return; }
                    res = await api.criar_marcacao(clienteNome, dhStr, duracao, txaObs.value.trim());
                }

                if (res && res.success) {
                    await this.onRefresh();
                    closeModal();
                    this.atualizar();
                } else {
                    errorEl.textContent = res?.error || "Erro ao criar marcação.";
                }
            } catch (e) {
                errorEl.textContent = "Erro ao comunicar com o backend.";
                console.error(e);
            } finally {
                btnSalvar.disabled = false;
            }
        });
    }

    _popularDuracoes(sel, dataHora, marcacoes) {
        const opcoes   = [15, 30, 45, 60, 75, 90];
        const maxTime  = new Date(dataHora.getFullYear(), dataHora.getMonth(), dataHora.getDate(), 21, 30);
        const is15slot = (dataHora.getMinutes() === 15 || dataHora.getMinutes() === 45);

        sel.innerHTML = "";
        const disponiveis = is15slot ? [15] : opcoes.filter(dur => {
            const fim = new Date(dataHora.getTime() + dur * 60000);
            if (fim > maxTime) return false;
            for (let i = 0; i < dur; i += 15) {
                const candidate = new Date(dataHora.getTime() + i * 60000);
                if (this._getMarcacao(marcacoes, candidate)) return false;
            }
            return true;
        });

        disponiveis.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d; opt.textContent = `${d} min`;
            sel.appendChild(opt);
        });
        if (disponiveis.includes(30)) sel.value = "30";
        else if (disponiveis.length > 0) sel.value = String(disponiveis[0]);
    }

    // ── Modal: Detalhe de Marcação ────────────────────────────────────────────

    _abrirDetalheMarcacao(marcacao) {
        if (document.getElementById("detalhe-marcacao-overlay")) return;

        const overlay = this._criarOverlay("detalhe-marcacao-overlay");
        document.body.appendChild(overlay);
        const modal   = this._criarModal("360px");
        overlay.appendChild(modal);

        const dt     = parseISOToLocal(marcacao.dataHora) || new Date(marcacao.dataHora);
        const passou = dt < new Date();
        const dias   = ["Domingo","Segunda","Terça","Quarta","Quinta","Sexta","Sábado"];

        const titulo = document.createElement("div");
        titulo.textContent = `${dias[dt.getDay()]} dia ${String(dt.getDate()).padStart(2,"0")} às ${String(dt.getHours()).padStart(2,"0")}:${String(dt.getMinutes()).padStart(2,"0")}`;
        titulo.style.cssText = "color:white;font-size:17px;font-weight:bold;text-align:center;padding-bottom:10px;";
        modal.appendChild(titulo);

        const addField = (label, value) => {
            const lbl = document.createElement("div");
            lbl.textContent  = label;
            lbl.style.cssText = "color:white;font-size:14px;margin-top:6px;";
            const inp = document.createElement("input");
            inp.type     = "text";
            inp.value    = value || "";
            inp.readOnly = true;
            inp.style.cssText = "width:100%;padding:6px 10px;border-radius:8px;border:none;background:white;font-size:14px;box-sizing:border-box;";
            modal.append(lbl, inp);
        };

        addField("Nome:",     marcacao.cliente?.nome);
        addField("Telefone:", marcacao.cliente?.numeroTelefone);
        addField("Duração:",  `${marcacao.duracao} minutos`);

        const lblObs = document.createElement("div");
        lblObs.textContent  = "Observações:";
        lblObs.style.cssText = "color:white;font-size:14px;margin-top:6px;";
        const txaObs = document.createElement("textarea");
        txaObs.value        = marcacao.observacoes || "";
        txaObs.rows         = 3;
        txaObs.style.cssText = "width:100%;padding:6px 10px;border-radius:8px;border:none;font-size:14px;box-sizing:border-box;resize:none;";
        modal.append(lblObs, txaObs);

        // Alterar hora (disponível para marcações passadas E futuras — apenas bloqueia
        // combinações data+hora que já passaram ou que têm conflito)
        let selDia = null, selHora = null;
        let semanaAlvo = getMonday(dt);

        if (!passou) {
            // --- Título ---
            const lblHora = document.createElement("div");
            lblHora.textContent  = "Alterar Hora";
            lblHora.style.cssText = "color:white;font-size:15px;font-weight:bold;text-align:center;margin-top:12px;";
            modal.appendChild(lblHora);

            const navSemana = document.createElement("div");
            navSemana.style.cssText = "display:flex;align-items:center;justify-content:center;gap:8px;margin-top:6px;";

            const btnSemAnt  = document.createElement("button");
            btnSemAnt.textContent = "◀";
            btnSemAnt.style.cssText = "background:rgb(43,40,40);color:white;border:none;border-radius:8px;padding:4px 10px;cursor:pointer;font-size:14px;font-weight:bold;";

            const lblSemana  = document.createElement("div");
            lblSemana.style.cssText = "color:white;font-size:13px;min-width:140px;text-align:center;";

            const btnSemProx = document.createElement("button");
            btnSemProx.textContent = "▶";
            btnSemProx.style.cssText = "background:rgb(43,40,40);color:white;border:none;border-radius:8px;padding:4px 10px;cursor:pointer;font-size:14px;font-weight:bold;";

            navSemana.append(btnSemAnt, lblSemana, btnSemProx);
            modal.appendChild(navSemana);

            const hboxCombos = document.createElement("div");
            hboxCombos.style.cssText = "display:flex;gap:16px;justify-content:center;margin-top:8px;";

            const mkCombo = (lbl) => {
                const wrap = document.createElement("div");
                wrap.style.cssText = "display:flex;flex-direction:column;gap:4px;align-items:center;";
                const l   = document.createElement("div"); l.textContent = lbl; l.style.cssText = "color:white;font-size:13px;";
                const sel = document.createElement("select");
                sel.style.cssText = "padding:4px 8px;border-radius:8px;border:none;min-width:110px;background:white;";
                wrap.append(l, sel);
                hboxCombos.appendChild(wrap);
                return sel;
            };

            selDia  = mkCombo("Dia");
            selHora = mkCombo("Hora");
         modal.appendChild(hboxCombos);

            DIAS_SEMANA_LONGO.forEach(d => {
                const opt = document.createElement("option"); opt.value = d; opt.textContent = d;
                selDia.appendChild(opt);
            });

            const diaIdxOriginal = dt.getDay() === 0 ? 6 : dt.getDay() - 1;
            selDia.value = DIAS_SEMANA_LONGO[diaIdxOriginal];

            const horaOriginalStr = `${String(dt.getHours()).padStart(2,"0")}:${String(dt.getMinutes()).padStart(2,"0")}`;

            const formatarSemana = (segunda) => {
                const dom = new Date(segunda.getTime() + 6 * 86400000);
                const fmt = (d) => `${d.getDate()} ${d.toLocaleDateString("pt-PT", { month: "short" })}`;
                return `${fmt(segunda)} – ${fmt(dom)}`;
            };

            const dataDoDiaSelecionado = () => {
                const idx = DIAS_SEMANA_LONGO.indexOf(selDia.value);
                return new Date(semanaAlvo.getTime() + idx * 86400000);
            };

            const popularHoras = async () => {
                selHora.innerHTML = "";
                const dataAlvo = dataDoDiaSelecionado();
                const api = getApi();
                if (api) {
                    try {
                        const res = await api.get_horas_disponiveis_data(
                            toLocalISOString(dt),
                            toLocalISOString(dataAlvo),
                            marcacao.duracao
                        );
                        if (res && res.success && res.horas) {
                            res.horas.forEach(h => {
                                const opt = document.createElement("option"); opt.value = h; opt.textContent = h;
                                selHora.appendChild(opt);
                            });
                        }
                    } catch(e) { console.error(e); }
                }
                if ([...selHora.options].some(o => o.value === horaOriginalStr)) {
                    selHora.value = horaOriginalStr;
                } else if (selHora.options.length > 0) {
                    selHora.selectedIndex = 0;
                }
                verificarMudancas();
            };

            const actualizarSemana = () => {
                lblSemana.textContent = formatarSemana(semanaAlvo);
                popularHoras();
            };

            btnSemAnt.addEventListener("click", () => {
                const semanaAtualLocal = getMonday(new Date());
                const candidata = new Date(semanaAlvo.getTime() - 7 * 86400000);
                if (candidata >= semanaAtualLocal) {
                    semanaAlvo = candidata;
                    actualizarSemana();
                }
                btnSemAnt.disabled = (semanaAlvo.getTime() === semanaAtualLocal.getTime());
                btnSemAnt.style.opacity = btnSemAnt.disabled ? "0.4" : "1";
            });

            btnSemProx.addEventListener("click", () => {
                semanaAlvo = new Date(semanaAlvo.getTime() + 7 * 86400000);
                actualizarSemana();
                const semanaAtualLocal = getMonday(new Date());
                btnSemAnt.disabled = (semanaAlvo.getTime() === semanaAtualLocal.getTime());
                btnSemAnt.style.opacity = btnSemAnt.disabled ? "0.4" : "1";
            });

            selDia.addEventListener("change", popularHoras);

            const semanaAtualLocal = getMonday(new Date());
            btnSemAnt.disabled = (semanaAlvo.getTime() === semanaAtualLocal.getTime());
            btnSemAnt.style.opacity = btnSemAnt.disabled ? "0.4" : "1";
            actualizarSemana();
        }

        const errorEl = document.createElement("div");
        errorEl.style.cssText = "color:#ff8080;font-size:13px;min-height:16px;margin-top:4px;";
        modal.appendChild(errorEl);

        // Botões
        const btnRow    = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:12px;justify-content:center;margin-top:14px;";
        const btnSalvar = this._mkBtn("Salvar", "rgb(36,43,141)");
        const btnSair   = this._mkBtn("Sair",   "rgb(60,60,60)");
        btnRow.append(btnSalvar, btnSair);

        if (passou) {
            const btnFaltou = this._mkBtn("Faltou", "rgb(128,26,15)");
            btnFaltou.disabled = marcacao.falta;
            if (marcacao.falta) btnFaltou.style.opacity = "0.5";
            btnFaltou.addEventListener("click", async () => {
                const api = getApi();
                if (!api) return;
                try {
                    const res = await api.marcar_falta_marcacao(toLocalISOString(dt));
                    if (res && res.success) { await this.onRefresh(); closeModal(); this.atualizar(); }
                    else errorEl.textContent = res?.error || "Erro ao marcar falta.";
                } catch(e) { errorEl.textContent = "Erro de comunicação."; }
            });
            btnRow.insertBefore(btnFaltou, btnSalvar);
        } else {
            const btnApagar = this._mkBtn("Apagar", "rgb(128,26,15)");
            btnApagar.addEventListener("click", async () => {
                const api = getApi();
                if (!api) return;
                try {
                    const res = await api.apagar_marcacao(toLocalISOString(dt));
                    if (res && res.success) { await this.onRefresh(); closeModal(); this.atualizar(); }
                    else errorEl.textContent = res?.error || "Erro ao apagar.";
                } catch(e) { errorEl.textContent = "Erro de comunicação."; }
            });
            btnRow.insertBefore(btnApagar, btnSalvar);
        }

        modal.appendChild(btnRow);

        const obsOriginal = txaObs.value;
        btnSalvar.disabled = true;

        const verificarMudancas = () => {
            const obsAlterada  = txaObs.value !== obsOriginal;
            const horaAlterada = (!passou && selDia && selHora) && (() => {
                if (!selHora.value) return false;
                // Calcular a data concreta que o utilizador escolheu
                const idx      = DIAS_SEMANA_LONGO.indexOf(selDia.value);
                const dataAlvo = new Date(semanaAlvo.getTime() + idx * 86400000);
                const [hh, mm] = selHora.value.split(":").map(Number);
                const dtNova   = new Date(
                    dataAlvo.getFullYear(), dataAlvo.getMonth(), dataAlvo.getDate(), hh, mm
                );
                // Compara com a data/hora original da marcação
                return dtNova.getTime() !== dt.getTime();
            })();
            btnSalvar.disabled = !obsAlterada && !horaAlterada;
        };

        txaObs.addEventListener("input",   verificarMudancas);
        if (selDia)  selDia.addEventListener("change",  verificarMudancas);
        if (selHora) selHora.addEventListener("change",  verificarMudancas);

        const closeModal = () => {
            if (document.body.contains(overlay)) document.body.removeChild(overlay);
            window.removeEventListener("keydown", keyH, true);
        };
        const keyH = (e) => {
            if (!document.body.contains(overlay)) return;
            if (e.key === "Escape") { e.preventDefault(); closeModal(); }
            if (e.key === "Enter" && !btnSalvar.disabled) { e.preventDefault(); btnSalvar.click(); }
        };
        window.addEventListener("keydown", keyH, true);
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeModal(); });
        btnSair.addEventListener("click", closeModal);

        btnSalvar.addEventListener("click", async () => {
            const api = getApi();
            if (!api) { errorEl.textContent = "API não disponível."; return; }
            try {
                btnSalvar.disabled = true;

                let dtNova = dt;
                    if (selDia && selHora && selHora.value) {
                    // Usa a mesma lógica de verificarMudancas: dia escolhido dentro da semanaAlvo navegada
                    const idx      = DIAS_SEMANA_LONGO.indexOf(selDia.value);
                    const dataAlvo = new Date(semanaAlvo.getTime() + idx * 86400000);
                    const [hh, mm] = selHora.value.split(":").map(Number);
                    dtNova = new Date(
                        dataAlvo.getFullYear(), dataAlvo.getMonth(), dataAlvo.getDate(), hh, mm
                    );
                }

                const res = await api.alterar_marcacao(
                    toLocalISOString(dt),
                    toLocalISOString(dtNova),
                    txaObs.value
                );
                if (res && res.success) {
                    await this.onRefresh();
                    closeModal();
                    this.atualizar();
                } else {
                    errorEl.textContent = res?.error || "Erro ao guardar.";
                    btnSalvar.disabled = false;
                }
            } catch(e) {
                errorEl.textContent = "Erro de comunicação.";
                btnSalvar.disabled  = false;
            }
        });
    }

    _updateCurrentSlotHighlight() {
        document.querySelectorAll(".calendar-cell.current-slot")
            .forEach(el => el.classList.remove("current-slot"));
        const iso = toLocalISOString(getCurrentSlotDate());
        const el  = document.querySelector(`.calendar-cell[data-datetime="${iso}"]`);
        if (el) el.classList.add("current-slot");
    }

    _atualizarLabel() {
        let texto = "";
        switch (this.modoAtual) {
            case "SEMANA": {
                const fim    = new Date(this.semanaAtual.getTime() + 6 * 86400000);
                const ini    = this.semanaAtual;
                const mesIni = ini.toLocaleDateString("pt-PT", { month: "long" });
                const mesFim = fim.toLocaleDateString("pt-PT", { month: "long" });
                texto = ini.getMonth() === fim.getMonth()
                    ? `${ini.getDate()} de ${mesIni} - ${fim.getDate()} de ${mesIni}`
                    : `${ini.getDate()} de ${mesIni} - ${fim.getDate()} de ${mesFim}`;
                break;
            }
            case "MES":
                texto = this.semanaAtual.toLocaleDateString("pt-PT", { month: "long", year: "numeric" });
                break;
            case "DIA":
                texto = this.diaSelecionado.toLocaleDateString("pt-PT", { weekday: "long", day: "numeric", month: "long" });
                break;
        }
        this.semanaLabel.textContent = texto;
        this.semanaLabel.style.cssText = "color:white;font-size:20px;font-weight:bold;text-align:center;flex:1;";
    }

    _atualizarEstiloToggles() {
        const ativo   = "background-color:rgb(60,60,60);color:white;font-weight:bold;border:none;border-radius:12px;padding:8px 12px;cursor:pointer;";
        const inativo = "background-color:rgb(43,40,40);color:white;font-weight:bold;border:none;border-radius:12px;padding:8px 12px;cursor:pointer;";
        this.diaToggle.style.cssText    = this.modoAtual === "DIA"    ? ativo : inativo;
        this.semanaToggle.style.cssText = this.modoAtual === "SEMANA" ? ativo : inativo;
        this.mesToggle.style.cssText    = this.modoAtual === "MES"    ? ativo : inativo;
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    _celula(texto, tipo) {
        const el = document.createElement("div");
        el.className  = `calendar-cell${tipo ? " " + tipo : ""}`;
        el.textContent = texto;
        return el;
    }

    _criarOverlay(id) {
        const el = document.createElement("div");
        el.id = id;
        Object.assign(el.style, {
            position: "fixed", left: 0, top: 0, right: 0, bottom: 0,
            background: "rgba(0,0,0,0.6)",
            display: "flex", alignItems: "center", justifyContent: "center",
            zIndex: 9999,
        });
        return el;
    }

    _criarModal(width) {
        const el = document.createElement("div");
        Object.assign(el.style, {
            width, maxWidth: "95%",
            background: "rgb(15,14,14)",
            borderRadius: "12px", padding: "20px",
            boxShadow: "0 8px 30px rgba(0,0,0,0.6)",
            color: "white",
            display: "flex", flexDirection: "column", gap: "6px",
            maxHeight: "90vh", overflowY: "auto",
        });
        return el;
    }

    _mkInput(placeholder) {
        const inp = document.createElement("input");
        inp.type        = "text";
        inp.placeholder = placeholder;
        inp.style.cssText = "width:100%;padding:6px 10px;border-radius:8px;border:none;font-size:14px;box-sizing:border-box;";
        return inp;
    }

    _mkLabel(texto) {
        const lbl = document.createElement("div");
        lbl.textContent  = texto;
        lbl.style.cssText = "color:white;font-size:14px;margin-top:4px;";
        return lbl;
    }

    _mkBtn(texto, bg) {
        const btn = document.createElement("button");
        btn.textContent = texto;
        btn.style.cssText = `background:${bg};color:white;border:none;padding:8px 18px;border-radius:8px;cursor:pointer;font-weight:700;font-size:15px;min-width:80px;`;
        return btn;
    }
}


// ==========================================
// 5. ClientesModule.js
// ==========================================

class ClientesModule {
    constructor(contentEl, getClientes, onRefresh) {
        this.content     = contentEl;
        this.getClientes = getClientes;
        this.onRefresh   = onRefresh;
    }

    renderizar() {
        this.content.innerHTML = "";
        const clientes = Object.values(this.getClientes());
        if (clientes.length === 0) this._renderVazio();
        else                       this._renderTabela(clientes);
    }

    _renderVazio() {
        const wrap = document.createElement("div");
        wrap.className = "clientes-empty";
        const msg = document.createElement("div");
        msg.className = "clientes-empty-message";
        msg.textContent = "Não tem nenhum cliente salvo, deseja adicionar um?";
        const btn = document.createElement("button");
        btn.className = "clientes-add-btn";
        btn.textContent = "Adicionar";
        btn.addEventListener("click", () => this._abrirAdicionarCliente());
        wrap.append(msg, btn);
        this.content.appendChild(wrap);
    }

    _renderTabela(clientesArray, filtro = "") {
        this.content.innerHTML = "";

        const toolbar   = document.createElement("div");
        toolbar.className = "clientes-toolbar";

        const search = document.createElement("input");
        search.className    = "search-field";
        search.type         = "text";
        search.placeholder  = "Pesquisar cliente...";
        search.value        = filtro;
        search.addEventListener("input", () => {
            this._atualizarTabelaFiltro(table, clientesArray, search.value);
            // Actualizar contador com resultados filtrados
            const filtroLower = search.value.toLowerCase();
            const visiveis = clientesArray.filter(c =>
                c.nome.toLowerCase().includes(filtroLower) ||
                c.numeroTelefone.includes(filtroLower)
            ).length;
            if (search.value.trim()) {
                contadorEl.textContent = `${visiveis} de ${totalClientes} cliente${totalClientes !== 1 ? "s" : ""}`;
            } else {
                contadorEl.textContent = `${totalClientes} cliente${totalClientes !== 1 ? "s" : ""} na base de dados`;
            }
        });

        const addBtn = document.createElement("button");
        addBtn.className   = "add-client-btn";
        addBtn.textContent = "+";
        addBtn.addEventListener("click", () => this._abrirAdicionarCliente());

        toolbar.append(search, addBtn);
        this.content.appendChild(toolbar);

        const contadorEl = document.createElement("div");
        contadorEl.style.cssText = "color:rgba(255,255,255,0.6);font-size:13px;padding:0 4px 6px 4px;";
        const totalClientes = clientesArray.length;
        contadorEl.textContent = `${totalClientes} cliente${totalClientes !== 1 ? "s" : ""} na base de dados`;
        this.content.appendChild(contadorEl);

        const container   = document.createElement("div");
        container.className = "clientes-table-container";

        const table = document.createElement("div");
        table.className = "clientes-table";

        const headers = ["Nome","Telefone","Tipo","Faltas","Dia Semana","Hora Corte"];
        headers.forEach(h => {
            const th = document.createElement("div");
            th.className   = "table-header";
            th.textContent = h;
            table.appendChild(th);
        });

        this._atualizarTabelaFiltro(table, clientesArray, filtro);

        container.appendChild(table);
        this.content.appendChild(container);

        // Focar automaticamente o campo de pesquisa após render
        // sem roubar foco se o utilizador estiver a interagir com outra coisa
        setTimeout(() => {
            if (document.activeElement === document.body ||
                document.activeElement === this.content) {
                search.focus();
            }
        }, 50);
    }

    _atualizarTabelaFiltro(table, clientesArray, filtro) {
        // Remover linhas antigas (manter apenas os headers: primeiros 6 filhos)
        const headers = Array.from(table.children).slice(0, 6);
        table.innerHTML = "";
        headers.forEach(h => table.appendChild(h));

        const filtroLower = (filtro || "").toLowerCase();
        clientesArray
            .filter(c => c.nome.toLowerCase().includes(filtroLower) ||
                         c.numeroTelefone.includes(filtroLower))
            .forEach(c => {
                const dados = [
                    c.nome, c.numeroTelefone, c.tipoCliente,
                    String(c.faltas), c.diaSemana || "—", c.horaCorte || "—",
                ];
                dados.forEach((d, i) => {
                    const cell = document.createElement("div");
                    cell.className   = "table-cell";
                    cell.textContent = d;
                    if (i === 0) {
                        cell.style.cursor = "pointer";
                        cell.addEventListener("click", () => this._abrirDetalheCliente(c));
                    }
                    table.appendChild(cell);
                });
            });
    }

    async _abrirDetalheCliente(clienteLocal) {
        if (document.getElementById("detalhe-overlay")) return;

        let clienteObj = clienteLocal;
        const resp = await getCliente(clienteLocal.nome);
        if (resp && resp.success && resp.cliente) clienteObj = resp.cliente;

        const overlay = this._overlay("detalhe-overlay");
        document.body.appendChild(overlay);
        const modal   = this._modal("760px");
        overlay.appendChild(modal);

        const closeModal = () => {
            if (document.body.contains(overlay)) document.body.removeChild(overlay);
            window.removeEventListener("keydown", keyH, true);
        };
        const keyH = (e) => {
            if (!document.body.contains(overlay)) return;
            if (e.key === "Escape") { e.preventDefault(); closeModal(); }
        };
        window.addEventListener("keydown", keyH, true);
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeModal(); });

        const topRow    = document.createElement("div");
        topRow.style.cssText = "display:flex;align-items:center;gap:8px;";
        const btnEditar = this._btn("Editar", "rgb(60,60,60)");
        const spacer    = document.createElement("div"); spacer.style.flex = "1";
        topRow.append(btnEditar, spacer);
        modal.appendChild(topRow);

        const visualBox = document.createElement("div");
        Object.assign(visualBox.style, {
            background: "rgb(43,40,40)", borderRadius: "12px", padding: "16px",
            display: "grid", gridTemplateColumns: "160px 1fr", gap: "12px",
        });
        modal.appendChild(visualBox);

        const addRow = (title, val) => {
            const th = document.createElement("div");
            th.style.cssText = "background:rgba(197,130,63,0.86);color:white;padding:12px;border-radius:12px;font-weight:700;";
            th.textContent = title;
            const vv = document.createElement("div");
            vv.style.cssText = "background:rgb(60,60,60);padding:12px;border-radius:12px;color:white;";
            vv.textContent = val || "—";
            visualBox.append(th, vv);
        };

        const populateVisual = (c) => {
            visualBox.innerHTML = "";
            addRow("Nome",     c.nome);
            addRow("Telefone", c.numeroTelefone);
            addRow("Tipo",     c.tipoCliente);
            if (c.tipoCliente === "SEMANAL") {
                addRow("Dia da Semana", c.diaSemana || "—");
                addRow("Hora Corte",    c.horaCorte || "—");
            }
            addRow("Rápido", (c.rapido === true || c.rapido === "true") ? "Sim" : "Não");
            addRow("Faltas",  String(c.faltas || 0));
        };
        populateVisual(clienteObj);

        const editBox = document.createElement("div");
        editBox.style.cssText = "display:none;flex-direction:column;gap:10px;align-items:center;width:100%;";
        modal.appendChild(editBox);

        const mkInput = (label, val) => {
            const wrap  = document.createElement("div");
            wrap.style.cssText = "display:flex;flex-direction:column;gap:4px;align-items:center;width:100%;";
            const lbl   = document.createElement("label"); lbl.textContent = label; lbl.style.cssText = "color:white;font-size:15px;font-weight:700;text-align:center;";
            const input = document.createElement("input"); input.type = "text"; input.value = val || "";
            input.style.cssText = "width:320px;max-width:80%;padding:6px 12px;border:none;border-radius:12px;background:white;color:black;font-size:15px;height:34px;box-sizing:border-box;";
            wrap.append(lbl, input);
            editBox.appendChild(wrap);
            return input;
        };

        const mkSelect = (label, opcoes, valor) => {
            const wrap  = document.createElement("div");
            wrap.style.cssText = "display:flex;flex-direction:column;gap:4px;align-items:center;width:100%;";
            const lbl   = document.createElement("label"); lbl.textContent = label; lbl.style.cssText = "color:white;font-size:15px;font-weight:700;text-align:center;";
            const sel   = document.createElement("select");
            sel.style.cssText = "width:320px;max-width:80%;padding:6px 10px;border-radius:12px;border:none;height:34px;background:white;";
            opcoes.forEach(o => {
                const opt = document.createElement("option"); opt.value = o.v; opt.textContent = o.l;
                sel.appendChild(opt);
            });
            sel.value = valor || "";
            wrap.append(lbl, sel);
            editBox.appendChild(wrap);
            return sel;
        };

        const mkCheck = (label, checked) => {
            const row  = document.createElement("label");
            row.style.cssText = "display:flex;align-items:center;gap:10px;justify-content:center;width:100%;color:white;font-weight:800;font-size:15px;cursor:pointer;";
            const chk  = document.createElement("input"); chk.type = "checkbox"; chk.checked = checked; chk.style.transform = "scale(1.5)";
            row.append(chk, document.createTextNode(` ${label}`));
            editBox.appendChild(row);
            return chk;
        };

        const semanalChk = mkCheck("Cliente Semanal", clienteObj.tipoCliente === "SEMANAL");
        const nomeInp    = mkInput("Nome:", clienteObj.nome);
        const telInp     = mkInput("Telefone:", clienteObj.numeroTelefone);
        telInp.addEventListener("input", () => { telInp.value = telInp.value.replace(/[^\d+]/g, ""); });

        const diaOps = [{v:"",l:"--"},
            {v:"Segunda",l:"Segunda"},{v:"Terça",l:"Terça"},{v:"Quarta",l:"Quarta"},
            {v:"Quinta",l:"Quinta"},{v:"Sexta",l:"Sexta"},{v:"Sábado",l:"Sábado"},{v:"Domingo",l:"Domingo"}];
        const diaSel  = mkSelect("Dia da Semana:", diaOps, clienteObj.diaSemana || "");
        const horaSel = mkSelect("Hora do Corte:", [], clienteObj.horaCorte || "");

        const faltasRow = document.createElement("div");
        faltasRow.style.cssText = "display:flex;align-items:center;gap:10px;justify-content:center;color:white;font-size:16px;font-weight:700;";
        let faltasVal   = clienteObj.faltas || 0;
        const faltasLbl = document.createElement("div"); faltasLbl.textContent = String(faltasVal); faltasLbl.style.cssText = "min-width:36px;text-align:center;color:white;font-size:15px;";
        const btnMenos  = this._btn("-","rgb(60,60,60)"); btnMenos.style.padding = "4px 10px";
        const btnMais   = this._btn("+","rgb(60,60,60)"); btnMais.style.padding  = "4px 10px";
        const faltasTxt = document.createElement("div"); faltasTxt.textContent = "Faltas:";
        faltasRow.append(faltasTxt, btnMenos, faltasLbl, btnMais);
        editBox.appendChild(faltasRow);
        btnMenos.addEventListener("click", () => { faltasVal = Math.max(0, faltasVal - 1); faltasLbl.textContent = String(faltasVal); });
        btnMais.addEventListener("click",  () => { faltasVal++;  faltasLbl.textContent = String(faltasVal); });

        const rapidoChk = mkCheck("Corte Rápido", clienteObj.rapido === true || clienteObj.rapido === "true");

        const popularHoras = () => {
            const step     = rapidoChk.checked ? 15 : 30;
            const diaEsc   = diaSel.value || null;
            const ocupados = new Set();
            Object.values(this.getClientes()).forEach(c => {
                if (String(c.tipoCliente).toUpperCase() !== "SEMANAL") return;
                if (c.nome === clienteObj.nome) return;
                if (!diaEsc || String(c.diaSemana).toLowerCase() !== String(diaEsc).toLowerCase()) return;
                const start = timeToMinutes(c.horaCorte); if (start === null) return;
                const dur = (c.rapido === true || c.rapido === "true") ? 15 : 30;
                for (let t = start; t < start + dur; t += 15) ocupados.add(t);
            });
            horaSel.innerHTML = "";
            const ph = document.createElement("option"); ph.value = ""; ph.textContent = "--"; horaSel.appendChild(ph);
            gerarHoras(step).forEach(h => {
                const min  = timeToMinutes(h);
                const next = min + 15;
                const ok   = step === 15 ? !ocupados.has(min) : (next <= 21*60 && !ocupados.has(min) && !ocupados.has(next));
                if (ok) { const o = document.createElement("option"); o.value = h; o.textContent = h; horaSel.appendChild(o); }
            });
            if (clienteObj.horaCorte && [...horaSel.options].some(o => o.value === clienteObj.horaCorte)) horaSel.value = clienteObj.horaCorte;
        };

        const updateSemanal = () => {
            const s = semanalChk.checked;
            diaSel.disabled  = !s;
            horaSel.disabled = !s;
            if (s) popularHoras(); else horaSel.innerHTML = "";
        };

        semanalChk.addEventListener("change", updateSemanal);
        diaSel.addEventListener("change", popularHoras);
        rapidoChk.addEventListener("change", popularHoras);
        updateSemanal();

        const bottomRow = document.createElement("div");
        bottomRow.style.cssText = "display:flex;justify-content:flex-end;gap:8px;margin-top:auto;";
        const btnApagar = this._btn("Apagar", "rgb(128,26,15)");
        const btnSalvar = this._btn("Salvar", "rgb(36,43,141)"); btnSalvar.style.display = "none";
        const btnSair   = this._btn("Sair",   "rgb(60,60,60)");
        bottomRow.append(btnApagar, btnSalvar, btnSair);
        modal.appendChild(bottomRow);

        btnEditar.addEventListener("click", () => {
            const isEditing = editBox.style.display !== "none";
            if (isEditing) {
                editBox.style.display   = "none";
                visualBox.style.display = "grid";
                btnSalvar.style.display = "none";
                btnApagar.style.display = "inline-block";
                btnEditar.textContent   = "Editar";
            } else {
                visualBox.style.display = "none";
                editBox.style.display   = "flex";
                btnSalvar.style.display = "inline-block";
                btnApagar.style.display = "none";
                btnEditar.textContent   = "Editar";
                setTimeout(() => nomeInp.focus(), 50);
            }
        });

        btnSair.addEventListener("click", closeModal);

        btnApagar.addEventListener("click", async () => {
            const ok = await this._confirmar("Deseja apagar o Cliente? Esta ação é irreversível", overlay);
            if (!ok) return;
            const res = await apagarCliente(clienteObj.nome);
            if (res && res.success) { await this.onRefresh(); closeModal(); this.renderizar(); }
            else alert(res?.error || "Erro ao apagar cliente.");
        });

        btnSalvar.addEventListener("click", async () => {
            const novoNome = nomeInp.value.trim();
            const novoTel  = telInp.value.trim();
            const tipo     = semanalChk.checked ? "SEMANAL" : "NORMAL";
            const dia      = semanalChk.checked ? (diaSel.value || null)  : null;
            const hora     = semanalChk.checked ? (horaSel.value || null) : null;
            const rapido   = rapidoChk.checked;

            if (!novoNome) { alert("Nome inválido"); return; }
            if (!/^\+?\d[\d\-\s()]{6,}$/.test(novoTel)) { alert("Telefone inválido"); return; }
            if (tipo === "SEMANAL" && (!dia || !hora)) { alert("Selecione dia e hora para cliente semanal"); return; }

            // Verificar duplicado de nome case-insensitive no frontend antes de enviar
            const clientesAtuais = Object.values(this.getClientes());
            const nomeDuplicado = clientesAtuais.some(
                c => c.nome.toLowerCase() === novoNome.toLowerCase() &&
                    c.nome.toLowerCase() !== clienteObj.nome.toLowerCase()
            );
            if (nomeDuplicado) { alert("Já existe um cliente com esse nome."); return; }

            const payload = {
                nomeOriginal: clienteObj.nome, nome: novoNome, numeroTelefone: novoTel,
                tipoCliente: tipo, diaSemana: dia, horaCorte: hora, faltas: faltasVal, rapido
            };
            const res = await alterarCliente(payload);
            if (res && res.success) { await this.onRefresh(); closeModal(); this.renderizar(); }
            else alert(res?.error || "Erro ao guardar cliente.");
        });
    }

    _abrirAdicionarCliente() {
        if (document.getElementById("add-cliente-overlay")) return;

        const overlay = this._overlay("add-cliente-overlay");
        document.body.appendChild(overlay);
        const modal   = this._modal("360px");
        overlay.appendChild(modal);

        const closeModal = () => {
            if (document.body.contains(overlay)) document.body.removeChild(overlay);
            window.removeEventListener("keydown", keyH, true);
        };
        const keyH = (e) => {
            if (!document.body.contains(overlay)) return;
            if (e.key === "Escape") { e.preventDefault(); closeModal(); }
            if (e.key === "Enter")  { e.preventDefault(); btnSalvar.click(); }
        };
        window.addEventListener("keydown", keyH, true);
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeModal(); });

        const titulo = document.createElement("h3");
        titulo.textContent = "Adicionar Cliente";
        titulo.style.cssText = "color:white;text-align:center;margin:0 0 8px 0;font-size:18px;";
        modal.appendChild(titulo);

        const semanalRow = document.createElement("label");
        semanalRow.style.cssText = "display:flex;align-items:center;gap:10px;color:white;font-size:14px;cursor:pointer;";
        const semanalChk = document.createElement("input"); semanalChk.type = "checkbox"; semanalChk.style.transform = "scale(1.4)";
        semanalRow.append(semanalChk, document.createTextNode(" Cliente Semanal"));
        modal.appendChild(semanalRow);

        const mkF = (lbl, ph) => {
            const l = document.createElement("div"); l.textContent = lbl; l.style.cssText = "color:white;font-size:13px;margin-top:4px;";
            const inp = document.createElement("input"); inp.type = "text"; inp.placeholder = ph;
            inp.style.cssText = "width:100%;padding:6px 8px;height:32px;border-radius:8px;border:none;background:white;color:black;font-size:13px;box-sizing:border-box;";
            modal.append(l, inp);
            return inp;
        };
        const mkSel = (lbl) => {
            const l = document.createElement("div"); l.textContent = lbl; l.style.cssText = "color:white;font-size:13px;margin-top:4px;";
            const sel = document.createElement("select");
            sel.style.cssText = "width:100%;padding:6px 8px;height:34px;border-radius:8px;border:none;background:white;color:black;font-size:13px;";
            modal.append(l, sel);
            return sel;
        };

        const nomeInp  = mkF("Nome",    "Nome");
        const telInp   = mkF("Telefone","Número de telefone");
        telInp.addEventListener("input", () => { telInp.value = telInp.value.replace(/[^\d+]/g, ""); });

        const diaSelEl  = mkSel("Dia da Semana");
        const horaSelEl = mkSel("Hora do Corte");
        const horaPlaceholder = document.createElement("option"); horaPlaceholder.value = ""; horaPlaceholder.textContent = "Hora do Corte"; horaSelEl.appendChild(horaPlaceholder);

        ["","Segunda","Terça","Quarta","Quinta","Sexta","Sábado","Domingo"].forEach(d => {
            const o = document.createElement("option"); o.value = d; o.textContent = d || "--"; diaSelEl.appendChild(o);
        });

        const rapidoRow = document.createElement("label");
        rapidoRow.style.cssText = "display:flex;align-items:center;gap:10px;color:white;font-size:14px;cursor:pointer;margin-top:4px;";
        const rapidoChk = document.createElement("input"); rapidoChk.type = "checkbox"; rapidoChk.style.transform = "scale(1.4)"; rapidoChk.disabled = true;
        rapidoRow.append(rapidoChk, document.createTextNode(" Corte Rápido"));
        modal.appendChild(rapidoRow);

        const errorEl = document.createElement("div");
        errorEl.style.cssText = "color:#ff8080;font-size:13px;min-height:16px;text-align:center;margin-top:4px;";
        modal.appendChild(errorEl);

        const btnRow    = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:12px;justify-content:flex-end;margin-top:8px;";
        const btnSalvar = this._btn("Salvar", "rgb(36,43,141)");
        const btnSair   = this._btn("Sair",   "rgb(60,60,60)");
        btnRow.append(btnSalvar, btnSair);
        modal.appendChild(btnRow);

        const setDisabled = (el, dis) => { el.disabled = dis; el.style.opacity = dis ? "0.5" : "1"; };
        setDisabled(diaSelEl, true); setDisabled(horaSelEl, true);

        semanalChk.addEventListener("change", () => {
            const s = semanalChk.checked;
            setDisabled(diaSelEl, !s);
            rapidoChk.disabled = !s;
            if (!s) { setDisabled(horaSelEl, true); rapidoChk.checked = false; horaSelEl.innerHTML = ""; horaSelEl.appendChild(horaPlaceholder); }
        });

        const popularHorasAdd = () => {
            horaSelEl.innerHTML = ""; horaSelEl.appendChild(horaPlaceholder);
            const diaEsc = diaSelEl.value;
            if (!diaEsc) { setDisabled(horaSelEl, true); return; }
            const step     = rapidoChk.checked ? 15 : 30;
            const ocupados = new Set();
            Object.values(this.getClientes()).forEach(c => {
                if (c.tipoCliente !== "SEMANAL" || !c.diaSemana) return;
                if (String(c.diaSemana).toLowerCase() !== diaEsc.toLowerCase()) return;
                const start = timeToMinutes(c.horaCorte); if (start === null) return;
                const dur = (c.rapido === true || c.rapido === "true") ? 15 : 30;
                for (let t = start; t < start + dur; t += 15) ocupados.add(t);
            });
            gerarHoras(step).forEach(h => {
                const min  = timeToMinutes(h);
                const next = min + 15;
                const ok   = step === 15 ? !ocupados.has(min) : (next <= 21*60 && !ocupados.has(min) && !ocupados.has(next));
                if (ok) { const o = document.createElement("option"); o.value = h; o.textContent = h; horaSelEl.appendChild(o); }
            });
            setDisabled(horaSelEl, false);
            errorEl.textContent = horaSelEl.options.length <= 1 ? "Nenhuma hora disponível neste dia." : "";
        };

        diaSelEl.addEventListener("change",  popularHorasAdd);
        rapidoChk.addEventListener("change", popularHorasAdd);
        btnSair.addEventListener("click", closeModal);

        btnSalvar.addEventListener("click", async () => {
            errorEl.textContent = "";
            const nome     = nomeInp.value.trim();
            const telefone = telInp.value.trim();
            const tipo     = semanalChk.checked ? "SEMANAL" : "NORMAL";
            const dia      = semanalChk.checked ? (diaSelEl.value  || null) : null;
            const hora     = semanalChk.checked ? (horaSelEl.value || null) : null;
            const rapido   = rapidoChk.checked;

            if (!nome)     { errorEl.textContent = "Nome é obrigatório."; return; }
            if (!telefone) { errorEl.textContent = "Telefone é obrigatório."; return; }
            const cs = Object.values(this.getClientes());
            if (cs.some(c => c.nome.toLowerCase() === nome.toLowerCase())) { errorEl.textContent = "Já existe um cliente com esse nome."; return; }
            if (cs.some(c => c.numeroTelefone === telefone)) { errorEl.textContent = "Já existe um cliente com esse número."; return; }
            if (tipo === "SEMANAL" && (!dia || !hora)) { errorEl.textContent = "Dia e hora são obrigatórios para cliente semanal."; return; }

            try {
                btnSalvar.disabled = true;
                const res = await adicionarCliente({ nome, numeroTelefone: telefone, tipoCliente: tipo, faltas: 0, diaSemana: dia, horaCorte: hora, rapido, temporario: false });
                if (res && res.success) { await this.onRefresh(); closeModal(); this.renderizar(); }
                else errorEl.textContent = res?.error || "Erro ao adicionar cliente.";
            } catch { errorEl.textContent = "Erro ao comunicar com o backend."; }
            finally { btnSalvar.disabled = false; }
        });
    }

    _confirmar(mensagem, parentOverlay) {
        return new Promise(resolve => {
            const wrap = document.createElement("div");
            Object.assign(wrap.style, {
                position: "absolute", left: 0, top: 0, right: 0, bottom: 0,
                display: "flex", alignItems: "center", justifyContent: "center", zIndex: 99999,
            });
            const dialog = document.createElement("div");
            Object.assign(dialog.style, {
                minWidth: "420px", maxWidth: "90%",
                background: "rgb(20,19,19)", borderRadius: "8px", padding: "18px",
                boxShadow: "0 8px 30px rgba(0,0,0,0.6)", color: "white", textAlign: "center",
            });
            const msg = document.createElement("div"); msg.textContent = mensagem; msg.style.cssText = "margin-bottom:16px;font-size:16px;color:#ddd;";
            const row = document.createElement("div"); row.style.cssText = "display:flex;justify-content:center;gap:12px;";
            const nao = this._btn("Não", "rgb(96,96,96)");
            const sim = this._btn("Sim", "rgb(128,26,15)");
            row.append(nao, sim);
            dialog.append(msg, row);
            wrap.appendChild(dialog);
            parentOverlay.appendChild(wrap);
            const cleanup = () => { if (parentOverlay.contains(wrap)) parentOverlay.removeChild(wrap); };
            nao.addEventListener("click", () => { cleanup(); resolve(false); });
            sim.addEventListener("click", () => { cleanup(); resolve(true);  });
            wrap.addEventListener("click", (e) => { if (e.target === wrap) { cleanup(); resolve(false); } });
        });
    }

    _overlay(id) {
        const el = document.createElement("div");
        el.id = id;
        Object.assign(el.style, {
            position: "fixed", left: 0, top: 0, right: 0, bottom: 0,
            background: "rgba(0,0,0,0.6)",
            display: "flex", alignItems: "center", justifyContent: "center",
            zIndex: 9999,
        });
        return el;
    }

    _modal(width) {
        const el = document.createElement("div");
        Object.assign(el.style, {
            width, maxWidth: "95%",
            background: "rgb(15,14,14)", borderRadius: "12px", padding: "16px",
            boxShadow: "0 8px 30px rgba(0,0,0,0.6)", color: "white",
            display: "flex", flexDirection: "column", gap: "10px",
            maxHeight: "90vh", overflowY: "auto",
        });
        return el;
    }

    _btn(texto, bg) {
        const btn = document.createElement("button");
        btn.textContent = texto;
        btn.style.cssText = `background:${bg};color:white;border:none;padding:10px 18px;border-radius:8px;cursor:pointer;font-weight:700;font-size:15px;`;
        return btn;
    }
}


// ==========================================
// 6. PendentesModule.js
// ==========================================

class PendentesModule {
    constructor(caixaEl, onRefreshData) {
        this.caixa         = caixaEl;
        this.onRefreshData = onRefreshData;
        this.pendentes     = [];

        this.caixa.addEventListener("click", () => this._abrirGestao());
    }

    setPendentes(pendentes) {
        this.pendentes = pendentes || [];
        this._render();
    }

    _render() {
        this.caixa.innerHTML = "";

        if (this.pendentes.length === 0) {
            const item = document.createElement("div");
            item.className  = "pendente-item placeholder";
            item.textContent = "Clique para adicionar pendente";
            item.style.fontStyle = "italic";
            item.style.color     = "#bbb";
            this.caixa.appendChild(item);
            return;
        }

        this.pendentes.forEach((p, i) => {
            const item = document.createElement("div");
            item.className  = "pendente-item";
            item.textContent = p.nome || p.get_nome?.() || "—";
            this.caixa.appendChild(item);

            if (i < this.pendentes.length - 1) {
                const sep = document.createElement("div");
                sep.className = "pendente-separator";
                this.caixa.appendChild(sep);
            }
        });
    }

    _abrirGestao() {
        if (document.getElementById("pendentes-overlay")) return;
        const overlay = this._criarOverlay();
        document.body.appendChild(overlay);
        this._renderGestao(overlay);
    }

    _criarOverlay() {
        const overlay = document.createElement("div");
        overlay.id = "pendentes-overlay";
        Object.assign(overlay.style, {
            position: "fixed", left: 0, top: 0, right: 0, bottom: 0,
            background: "rgba(0,0,0,0.6)",
            display: "flex", alignItems: "center", justifyContent: "center",
            zIndex: 9999,
        });
        return overlay;
    }

    _renderGestao(overlay) {
        overlay.innerHTML = "";

        const modal = document.createElement("div");
        Object.assign(modal.style, {
            width: "560px", maxWidth: "95%",
            background: "rgb(15,14,14)", borderRadius: "12px", padding: "20px",
            boxShadow: "0 8px 30px rgba(0,0,0,0.6)", color: "white",
            display: "flex", flexDirection: "column", gap: "12px",
        });
        overlay.appendChild(modal);

        const closeModal = () => {
            if (document.body.contains(overlay)) document.body.removeChild(overlay);
            window.removeEventListener("keydown", keyHandler, true);
        };
        const keyHandler = (e) => {
            if (!document.body.contains(overlay)) return;
            if (e.key === "Escape") { e.preventDefault(); closeModal(); }
        };
        window.addEventListener("keydown", keyHandler, true);
        overlay.addEventListener("click", (e) => { if (e.target === overlay) closeModal(); });

        if (this.pendentes.length === 0) {
            this._renderGestaoVazia(modal, closeModal, overlay);
        } else {
            this._renderGestaoComDados(modal, closeModal, overlay);
        }
    }

    _renderGestaoVazia(modal, closeModal, overlay) {
        const msg = document.createElement("div");
        msg.textContent = "Não existem clientes pendentes, deseja adicionar um?";
        msg.style.cssText = "font-size:20px;font-weight:bold;text-align:center;color:white;";

        const btnRow = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:32px;justify-content:center;margin-top:16px;";
        const btnAdicionar = this._criarBtn("Adicionar", "rgb(36,43,141)");
        const btnSair      = this._criarBtn("Sair", "rgb(128,26,15)");
        btnAdicionar.addEventListener("click", () => this._mostrarFormAdicionar(modal, closeModal, overlay));
        btnSair.addEventListener("click", closeModal);
        btnRow.append(btnAdicionar, btnSair);
        modal.append(msg, btnRow);
    }

    _renderGestaoComDados(modal, closeModal, overlay) {
        let linhaSelecionada = -1;

        const barra = document.createElement("div");
        barra.style.cssText = "display:flex;gap:8px;align-items:center;";

        const btnAdicionar = this._criarBtnSmall("+", "rgb(43,40,40)");
        const btnRemover   = this._criarBtnSmall("-", "rgb(43,40,40)");
        const spacer       = document.createElement("div"); spacer.style.flex = "1";
        const btnSair      = this._criarBtn("Sair", "rgb(128,26,15)", "80px");

        btnAdicionar.addEventListener("click", () => this._mostrarFormAdicionar(modal, closeModal, overlay));

        btnRemover.addEventListener("click", async () => {
            if (linhaSelecionada < 0 || linhaSelecionada >= this.pendentes.length) return;
            const pendente = this.pendentes[linhaSelecionada];
            const nome     = pendente.nome || pendente.get_nome?.() || "";
            const res      = await removerPendente(nome);
            if (res && res.success) {
                this.pendentes.splice(linhaSelecionada, 1);
                linhaSelecionada = -1;
                this._render();
                this._renderGestao(overlay);
            }
        });

        btnSair.addEventListener("click", closeModal);
        barra.append(btnAdicionar, btnRemover, spacer, btnSair);
        modal.appendChild(barra);

        // Tabela
        const tabelaWrap = document.createElement("div");
        Object.assign(tabelaWrap.style, {
            background: "rgb(43,40,40)", borderRadius: "12px", padding: "10px",
            display: "flex", flexDirection: "column", gap: "8px",
        });

        const cabecalho = document.createElement("div");
        cabecalho.style.cssText = "display:grid;grid-template-columns:1fr 1fr;gap:8px;";
        ["Nome", "Telefone"].forEach(t => {
            const th = document.createElement("div");
            th.textContent = t;
            th.style.cssText = "background:rgba(197,130,63,0.86);color:white;font-size:15px;font-weight:bold;padding:10px;border-radius:12px;text-align:center;";
            cabecalho.appendChild(th);
        });
        tabelaWrap.appendChild(cabecalho);

        const linhasWrap = document.createElement("div");
        linhasWrap.style.cssText = "display:flex;flex-direction:column;gap:6px;max-height:300px;overflow-y:auto;";

        const renderLinhas = () => {
            linhasWrap.innerHTML = "";
            this.pendentes.forEach((p, i) => {
                const nome   = p.nome || p.get_nome?.() || "—";
                const numero = p.numeroTelefone || p.get_numero_telefone?.() || "—";

                const linha = document.createElement("div");
                linha.style.cssText = "display:grid;grid-template-columns:1fr 1fr;gap:8px;cursor:pointer;";

                const baseStyle = "font-size:14px;background:rgb(60,60,60);color:white;border-radius:12px;border:1px solid rgba(197,130,63,0.86);padding:8px;text-align:center;";
                const selStyle  = "font-size:14px;background:rgb(36,43,141);color:white;border-radius:12px;border:1px solid rgba(197,130,63,0.86);padding:8px;text-align:center;";

                const cellNome = document.createElement("div");
                cellNome.textContent  = nome;
                cellNome.style.cssText = i === linhaSelecionada ? selStyle : baseStyle;

                const cellTel = document.createElement("div");
                cellTel.textContent  = numero === "" ? "—" : numero;
                cellTel.style.cssText = i === linhaSelecionada ? selStyle : baseStyle;

                linha.append(cellNome, cellTel);
                linha.addEventListener("click", () => {
                    linhaSelecionada = i;
                    renderLinhas();
                });
                linhasWrap.appendChild(linha);
            });
        };
        renderLinhas();
        tabelaWrap.appendChild(linhasWrap);
        modal.appendChild(tabelaWrap);
    }

    _mostrarFormAdicionar(modal, closeModal, overlay) {
        modal.innerHTML = "";

        const title = document.createElement("h3");
        title.textContent  = "Adicionar Pendente";
        title.style.cssText = "color:white;text-align:center;margin:0 0 8px 0;font-size:18px;";
        modal.appendChild(title);

        const pesquisa = this._criarInput("Pesquisar cliente...");
        modal.appendChild(pesquisa);

        const sugestoes = document.createElement("div");
        sugestoes.style.cssText = "background:white;border-radius:8px;max-height:100px;overflow-y:auto;display:none;";
        modal.appendChild(sugestoes);

        const checkRow = document.createElement("label");
        checkRow.style.cssText = "display:flex;align-items:center;gap:10px;color:white;font-size:15px;cursor:pointer;";
        const chk = document.createElement("input"); chk.type = "checkbox"; chk.style.transform = "scale(1.4)";
        checkRow.append(chk, document.createTextNode(" Desconhecido"));
        modal.appendChild(checkRow);

        const nomeLabel = this._criarLabel("Nome:");
        const nomeField = this._criarInput("Nome"); nomeField.disabled = true;
        const telLabel  = this._criarLabel("Número de telefone:");
        const telField  = this._criarInput("Número de telefone"); telField.disabled = true;
        telField.addEventListener("input", () => { telField.value = telField.value.replace(/[^\d+]/g, ""); });

        const errorEl = document.createElement("div");
        errorEl.style.cssText = "color:#ff8080;font-size:13px;min-height:18px;text-align:center;";

        modal.append(nomeLabel, nomeField, telLabel, telField, errorEl);

        const btnRow    = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:16px;justify-content:flex-end;margin-top:8px;";
        const btnSalvar = this._criarBtn("Salvar", "rgb(36,43,141)");
        const btnSair   = this._criarBtn("Sair", "rgb(60,60,60)");
        btnRow.append(btnSalvar, btnSair);
        modal.appendChild(btnRow);

        let clientesSnapshot = {};
        const api = getApi();
        if (api) api.get_clientes_map().then(m => { clientesSnapshot = m || {}; });

        pesquisa.addEventListener("input", () => {
            if (chk.checked) { sugestoes.style.display = "none"; return; }
            const val = pesquisa.value.trim().toLowerCase();
            if (!val) { sugestoes.style.display = "none"; return; }
            const nomesPendentes = this.pendentes.map(p => (p.nome || "").toLowerCase());
            const matches = Object.keys(clientesSnapshot).filter(
                n => n.toLowerCase().includes(val) && !nomesPendentes.includes(n.toLowerCase())
            );
            sugestoes.innerHTML = "";
            if (!matches.length) { sugestoes.style.display = "none"; return; }
            sugestoes.style.display = "block";
            matches.forEach(n => {
                const item = document.createElement("div");
                item.textContent  = n;
                item.style.cssText = "padding:8px 12px;color:black;cursor:pointer;font-size:14px;border-bottom:1px solid #eee;";
                item.addEventListener("mousedown", (e) => {
                    e.preventDefault();
                    pesquisa.value = n;
                    sugestoes.style.display = "none";
                });
                sugestoes.appendChild(item);
            });
        });
        pesquisa.addEventListener("blur", () => {
            setTimeout(() => { sugestoes.style.display = "none"; }, 150);
        });

        chk.addEventListener("change", () => {
            nomeField.disabled = !chk.checked;
            telField.disabled  = !chk.checked;
            pesquisa.disabled  = chk.checked;
            sugestoes.style.display = "none";
        });

        btnSair.addEventListener("click", () => this._renderGestao(overlay));

        btnSalvar.addEventListener("click", async () => {
            errorEl.textContent = "";

            let nome, numero;

            if (chk.checked) {
                nome   = nomeField.value.trim();
                numero = telField.value.trim();
                if (!nome) { errorEl.textContent = "O nome não pode ser vazio."; return; }
            } else {
                nome = pesquisa.value.trim();
                if (!nome || !clientesSnapshot[nome]) {
                    errorEl.textContent = "Nenhum cliente selecionado."; return;
                }
                numero = clientesSnapshot[nome]?.numeroTelefone || "";
            }

            if (this.pendentes.some(p => (p.nome||"").toLowerCase() === nome.toLowerCase())) {
                errorEl.textContent = "Este cliente já está na lista de pendentes."; return;
            }

            try {
                btnSalvar.disabled = true;
                const res = await adicionarPendente(nome, numero);
                if (res && res.success) {
                    this.pendentes.push({ nome, numeroTelefone: numero });
                    this._render();
                    this._renderGestao(overlay);
                } else {
                    errorEl.textContent = res?.error || "Erro ao adicionar pendente.";
                }
            } catch(e) {
                errorEl.textContent = "Erro ao comunicar com o backend.";
            } finally {
                btnSalvar.disabled = false;
            }
        });

        const keyH = (e) => {
            if (!document.body.contains(overlay)) return;
            if (e.key === "Escape") { e.preventDefault(); this._renderGestao(overlay); window.removeEventListener("keydown", keyH, true); }
            if (e.key === "Enter")  { e.preventDefault(); btnSalvar.click(); }
        };
        window.addEventListener("keydown", keyH, true);
    }

    _criarBtn(texto, bg, width = "auto") {
        const btn = document.createElement("button");
        btn.textContent = texto;
        btn.style.cssText = `background:${bg};color:white;border:none;padding:10px 18px;border-radius:8px;cursor:pointer;font-weight:700;font-size:15px;min-width:${width};`;
        return btn;
    }

    _criarBtnSmall(texto, bg) {
        const btn = document.createElement("button");
        btn.textContent = texto;
        btn.style.cssText = `background:${bg};color:white;border:none;width:30px;height:30px;border-radius:8px;cursor:pointer;font-weight:700;font-size:18px;`;
        return btn;
    }

    _criarInput(placeholder) {
        const input = document.createElement("input");
        input.type        = "text";
        input.placeholder = placeholder;
        input.style.cssText = "width:100%;padding:8px 12px;border:none;border-radius:8px;background:rgb(43,40,40);color:white;font-size:14px;box-sizing:border-box;";
        return input;
    }

    _criarLabel(texto) {
        const label = document.createElement("div");
        label.textContent  = texto;
        label.style.cssText = "color:white;font-size:14px;margin-top:6px;";
        return label;
    }
}


// ==========================================
// 7. pagina_principal.js
// ==========================================

class PaginaPrincipalController {
    constructor() {
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

        this.clientes  = {};
        this.marcacoes = {};
        this.pendentes = [];

        this.calendarioModule = null;
        this.clientesModule   = null;
        this.pendentesModule  = null;
        this.anotacoesModule  = null;
    }

    async init() {
        const api = await waitForApi(5000, 100);

        if (api) {
            try {
                const info = await getUtilizadorInfo();
                const nome = info?.nome || info?.name || info?.username || null;
                this.els.userLabel.textContent = nome ? `Bem-vindo, ${nome}` : "Bem-vindo, Utilizador";
            } catch { /* não crítico */ }
        }

        await this._carregarDados();
        this._inicializarModulos();

        if (api) await this.anotacoesModule.carregarAnotacoes();

        this._iniciarRelogio();
        this._bindGlobal();
        this._mostrarCalendario();
        this.calendarioModule.iniciarHighlightLoop();
    }

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

    async _refresh() {
        await this._carregarDados();
        this.pendentesModule.setPendentes(this.pendentes);
    }

    _inicializarModulos() {
        this.calendarioModule = new CalendarioModule(
            this.els,
            () => this.marcacoes,
            () => this._refresh()
        );

        this.clientesModule = new ClientesModule(
            this.els.clientesContent,
            () => this.clientes,
            () => this._refresh()
        );

        this.pendentesModule = new PendentesModule(
            this.els.caixaClientesPendentes,
            () => this._refresh()
        );
        this.pendentesModule.setPendentes(this.pendentes);

        this.anotacoesModule = new AnotacoesModule(
            this.els.anotacoesArea,
            this.els.blurToggleBtn
        );
    }

    _bindGlobal() {
        this.els.logoutBtn.addEventListener("click",       () => this._handleLogout());
        this.els.calendarioToggle.addEventListener("click",() => this._mostrarCalendario());
        this.els.clientesToggle.addEventListener("click",  () => this._mostrarClientes());
    }

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
        const base    = "font-size:15px;font-weight:bold;border-radius:12px;border:none;padding:12px;min-height:40px;max-width:100%;width:100%;cursor:pointer;";
        const ativo   = base + "background-color:rgb(60,60,60);color:white;";
        const inativo = base + "background-color:rgb(43,40,40);color:white;";
        const calAtivo = this.els.calendarioToggle.classList.contains("active");
        this.els.calendarioToggle.style.cssText = calAtivo ? ativo  : inativo;
        this.els.clientesToggle.style.cssText   = calAtivo ? inativo : ativo;
    }

    async _handleLogout() {
        await this.anotacoesModule.guardar();
        const res = await fazerLogout();
        if (res && res.success) await mostrarLogin();
    }

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

document.addEventListener("DOMContentLoaded", () => {
    window.paginaController = new PaginaPrincipalController();
    window.paginaController.init();
});