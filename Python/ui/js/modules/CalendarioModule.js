/**
 * CalendarioModule.js
 * Gere toda a lógica do calendário: modos semana/mês/dia,
 * navegação, criação e detalhe de marcações.
 */

import {
    getMonday, isToday, isSunday, isHoliday, isPast,
    getCurrentSlotDate, formatDate
} from "../utils/dateUtils.js";
import { getApi } from "../utils/apiUtils.js";

const HORA_ABERTURA      = 7;
const HORA_FECHO         = 21;
const INTERVALO_MINUTOS  = 30;
const DIAS_SEMANA_CURTO  = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"];
const DIAS_SEMANA_LONGO  = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];

export class CalendarioModule {
    /**
     * @param {object} els           - referências aos elementos DOM
     * @param {Function} getMarcacoes - callback que devolve o mapa de marcações atual
     * @param {Function} onRefresh    - callback chamado após salvar/apagar marcação
     */
    constructor(els, getMarcacoes, onRefresh) {
        this.grid         = els.calendarioGrid;
        this.semanaLabel  = els.semanaLabel;
        this.todayBtn     = els.todayBtn;
        this.prevBtn      = els.semanaAnteriorBtn;
        this.nextBtn      = els.proximaSemanaBtn;
        this.diaToggle    = els.diaToggle;
        this.semanaToggle = els.semanaToggle;
        this.mesToggle    = els.mesToggle;

        this.getMarcacoes = getMarcacoes;
        this.onRefresh    = onRefresh;

        this.modoAtual      = "SEMANA";
        this.semanaAtual    = getMonday(new Date());
        this.diaSelecionado = new Date();

        this._currentSlotInterval = null;

        this._bindNav();
    }

    // API pública

    atualizar() {
        this.grid.innerHTML = "";
        this.grid.className = `calendar-grid ${this.modoAtual.toLowerCase()}`;

        switch (this.modoAtual) {
            case "SEMANA": this._criarSemana();  break;
            case "MES":    this._criarMes();     break;
            case "DIA":    this._criarDia();     break;
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
        const now       = new Date();
        const minutesTo = (now.getMinutes() < 30 ? 30 : 60) - now.getMinutes();
        const msTo      = minutesTo * 60000 - now.getSeconds() * 1000 - now.getMilliseconds();
        setTimeout(() => {
            this._updateCurrentSlotHighlight();
            this._currentSlotInterval = setInterval(() => this._updateCurrentSlotHighlight(), 30 * 60000);
        }, msTo);
    }

    // Navegação

    _bindNav() {
        this.todayBtn.addEventListener("click", () => this._hoje());
        this.prevBtn.addEventListener("click",  () => this._anterior());
        this.nextBtn.addEventListener("click",  () => this._proximo());

        this.diaToggle.addEventListener("click",    () => this.setModo("DIA"));
        this.semanaToggle.addEventListener("click", () => this.setModo("SEMANA"));
        this.mesToggle.addEventListener("click",    () => this.setModo("MES"));
    }

    _hoje() {
        const hoje = new Date();
        this.semanaAtual    = getMonday(hoje);
        this.diaSelecionado = hoje;
        this.atualizar();
    }

    _anterior() {
        if (this.modoAtual === "SEMANA") {
            this.semanaAtual = new Date(this.semanaAtual.getTime() - 7 * 86400000);
        } else if (this.modoAtual === "MES") {
            this.semanaAtual = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() - 1, 1);
        } else {
            this.diaSelecionado = new Date(this.diaSelecionado.getTime() - 86400000);
            this.semanaAtual    = getMonday(this.diaSelecionado);
        }
        this.atualizar();
    }

    _proximo() {
        if (this.modoAtual === "SEMANA") {
            this.semanaAtual = new Date(this.semanaAtual.getTime() + 7 * 86400000);
        } else if (this.modoAtual === "MES") {
            this.semanaAtual = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() + 1, 1);
        } else {
            this.diaSelecionado = new Date(this.diaSelecionado.getTime() + 86400000);
            this.semanaAtual    = getMonday(this.diaSelecionado);
        }
        this.atualizar();
    }

    // Calendário semanal

    _criarSemana() {
        // célula vazia de canto
        this.grid.appendChild(this._celula("", "header"));

        // cabeçalhos dos dias
        for (let i = 0; i < 7; i++) {
            const data  = new Date(this.semanaAtual.getTime() + i * 86400000);
            const texto = `${DIAS_SEMANA_CURTO[i]} ${String(data.getDate()).padStart(2, "0")}`;
            const cel   = this._celula(texto, "header");

            if      (isToday(data))   cel.classList.add("today");
            else if (isSunday(data))  cel.classList.add("sunday");
            else if (isHoliday(data)) cel.classList.add("holiday");

            cel.style.cursor = "pointer";
            cel.addEventListener("click", () => {
                this.diaSelecionado = data;
                this.setModo("DIA");
            });
            this.grid.appendChild(cel);
        }

        // linhas de horas
        for (let h = HORA_ABERTURA; h <= HORA_FECHO; h++) {
            for (let m = 0; m < 60; m += INTERVALO_MINUTOS) {
                const horaStr = `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
                this.grid.appendChild(this._celula(horaStr, "hour"));

                for (let d = 0; d < 7; d++) {
                    const data    = new Date(this.semanaAtual.getTime() + d * 86400000);
                    const dataHora = new Date(data.getFullYear(), data.getMonth(), data.getDate(), h, m);
                    this.grid.appendChild(this._celulaHorario(dataHora));
                }
            }
        }
    }

    // Calendário mensal

    _criarMes() {
        const container = document.createElement("div");
        container.className = "mes-container";

        // cabeçalho
        const header = document.createElement("div");
        header.className = "mes-header";
        DIAS_SEMANA_CURTO.forEach(d => header.appendChild(this._celula(d, "header")));
        container.appendChild(header);

        // semanas
        const weeksWrap = document.createElement("div");
        weeksWrap.className = "mes-weeks";

        const primeiroDia = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth(), 1);
        let   offset      = primeiroDia.getDay();
        if (offset === 0) offset = 7;
        const inicio = new Date(primeiroDia);
        inicio.setDate(inicio.getDate() - (offset - 1));

        const ultimo       = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() + 1, 0);
        let   offsetFim    = ultimo.getDay();
        if (offsetFim === 0) offsetFim = 7;
        const fim = new Date(ultimo);
        fim.setDate(fim.getDate() + (7 - offsetFim));

        const cur = new Date(inicio);
        while (cur <= fim) {
            const semanaRow = document.createElement("div");
            semanaRow.className = "mes-week";

            for (let d = 0; d < 7; d++) {
                const cel   = this._celula(String(cur.getDate()), "");
                const clique = new Date(cur);

                if      (isToday(cur))   cel.classList.add("today");
                else if (isSunday(cur))  cel.classList.add("sunday");
                else if (isHoliday(cur)) cel.classList.add("holiday");

                if (cur.getMonth() !== this.semanaAtual.getMonth()) {
                    cel.style.opacity  = "0.4";
                    cel.style.fontSize = "16px";
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

        container.appendChild(weeksWrap);
        this.grid.appendChild(container);
    }

    // Calendário diário

    _criarDia() {
        for (let h = HORA_ABERTURA; h <= HORA_FECHO; h++) {
            for (let m = 0; m < 60; m += INTERVALO_MINUTOS) {
                const horaStr = `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
                this.grid.appendChild(this._celula(horaStr, "hour"));

                const dataHora = new Date(
                    this.diaSelecionado.getFullYear(),
                    this.diaSelecionado.getMonth(),
                    this.diaSelecionado.getDate(), h, m
                );
                this.grid.appendChild(this._celulaHorario(dataHora));
            }
        }
    }

    // Célula de horário

    _celulaHorario(dataHora) {
        const cel = document.createElement("div");
        cel.className = "calendar-cell";
        const iso = dataHora.toISOString();
        cel.setAttribute("data-datetime", iso);

        if (isPast(dataHora)) cel.classList.add("past");
        if (iso === getCurrentSlotDate().toISOString()) cel.classList.add("current-slot");

        const marcacoes = this.getMarcacoes();
        const key       = dataHora.toISOString();
        const key15     = new Date(dataHora.getTime() + 15 * 60000).toISOString();
        const marc1     = marcacoes[key];
        const marc2     = marcacoes[key15];

        const is15m1 = marc1 && marc1.duracao === 15;
        const is15m2 = marc2 && marc2.duracao === 15;

        if (is15m1 || is15m2) {
            const hbox = document.createElement("div");
            hbox.style.cssText = "display:flex;gap:2px;width:100%;height:100%;";

            if (is15m1) {
                hbox.appendChild(this._boxMarcacao(marc1, true));
            } else {
                const r = document.createElement("div"); r.style.flex = "1";
                if (!isPast(dataHora)) r.addEventListener("click", () => this._abrirCriarMarcacao(dataHora));
                hbox.appendChild(r);
            }
            if (is15m2) {
                hbox.appendChild(this._boxMarcacao(marc2, true));
            } else {
                const r = document.createElement("div"); r.style.flex = "1";
                const dt15 = new Date(dataHora.getTime() + 15 * 60000);
                if (!isPast(dt15)) r.addEventListener("click", () => this._abrirCriarMarcacao(dt15));
                hbox.appendChild(r);
            }
            cel.appendChild(hbox);
        } else if (marc1 && marc1.duracao >= 30) {
            cel.appendChild(this._boxMarcacao(marc1, false));
        } else if (!isPast(dataHora)) {
            cel.addEventListener("click", () => this._abrirCriarMarcacao(dataHora));
        }

        return cel;
    }

    _boxMarcacao(marcacao, meia) {
        const wrap = document.createElement("div");
        wrap.style.cssText = `flex:${meia ? "1" : "1 1 100%"};padding:4px;`;

        const box = document.createElement("div");
        const isFalta = marcacao.falta;
        box.style.cssText = `
            border-radius:12px;
            background:${isFalta ? "rgb(128,26,15)" : "rgb(247,221,151)"};
            height:32px;
            display:flex;align-items:center;padding:0 8px;
            cursor:pointer;
        `;

        const label = document.createElement("div");
        label.textContent = marcacao.cliente ? marcacao.cliente.nome : "—";
        label.style.cssText = `
            font-size:13px;font-weight:bold;
            color:${isFalta ? "white" : "rgb(43,40,40)"};
            overflow:hidden;white-space:nowrap;text-overflow:ellipsis;
        `;
        box.appendChild(label);
        box.addEventListener("click", (e) => {
            e.stopPropagation();
            this._abrirDetalheMarcacao(marcacao);
        });
        wrap.appendChild(box);
        return wrap;
    }

    // Modal: criar marcação

    _abrirCriarMarcacao(dataHora) {
        if (document.getElementById("marcacao-overlay")) return;

        const overlay = this._criarOverlay("marcacao-overlay");
        document.body.appendChild(overlay);

        const modal = this._criarModal("360px");
        overlay.appendChild(modal);

        // Título
        const titulo = document.createElement("div");
        const dias   = ["Domingo","Segunda","Terça","Quarta","Quinta","Sexta","Sábado"];
        const dStr   = dias[dataHora.getDay()];
        titulo.textContent = `${dStr} dia ${String(dataHora.getDate()).padStart(2,"0")} às ${String(dataHora.getHours()).padStart(2,"0")}:${String(dataHora.getMinutes()).padStart(2,"0")}`;
        titulo.style.cssText = "color:white;font-size:17px;font-weight:bold;text-align:center;padding-bottom:8px;";
        modal.appendChild(titulo);

        // Pesquisa cliente
        const pesquisa = this._criarInputStyle("Pesquisar cliente...");
        modal.appendChild(pesquisa);

        const sugestoes = document.createElement("div");
        sugestoes.style.cssText = "background:white;border-radius:8px;max-height:80px;overflow-y:auto;display:none;";
        modal.appendChild(sugestoes);

        // Checkbox desconhecido
        const chkRow = document.createElement("label");
        chkRow.style.cssText = "display:flex;align-items:center;gap:8px;color:white;font-size:15px;cursor:pointer;";
        const chkDesk = document.createElement("input"); chkDesk.type = "checkbox";
        chkRow.append(chkDesk, document.createTextNode(" Desconhecido"));
        modal.appendChild(chkRow);

        // Campos desconhecido
        const lblNome = this._criarLabelStyle("Nome:");
        const fldNome = this._criarInputStyle("Nome"); fldNome.disabled = true;
        const lblTel  = this._criarLabelStyle("Telefone:");
        const fldTel  = this._criarInputStyle("Número de telefone"); fldTel.disabled = true;
        fldTel.addEventListener("input", () => { fldTel.value = fldTel.value.replace(/[^\d+]/g, ""); });
        modal.append(lblNome, fldNome, lblTel, fldTel);

        // Duração
        const lblDur = this._criarLabelStyle("Duração:");
        const selDur = document.createElement("select");
        selDur.style.cssText = "width:100%;padding:6px 8px;border-radius:8px;border:none;font-size:14px;background:white;";
        modal.append(lblDur, selDur);

        // Observações
        const lblObs = this._criarLabelStyle("Observações:");
        const txaObs = document.createElement("textarea");
        txaObs.placeholder  = "Observações (opcional)";
        txaObs.rows         = 3;
        txaObs.style.cssText = "width:100%;padding:6px 8px;border-radius:8px;border:none;font-size:14px;box-sizing:border-box;resize:none;";
        modal.append(lblObs, txaObs);

        const errorEl = document.createElement("div");
        errorEl.style.cssText = "color:#ff8080;font-size:13px;min-height:16px;";
        modal.appendChild(errorEl);

        // Botões
        const btnRow  = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:12px;justify-content:center;margin-top:8px;";
        const btnSalvar = this._criarBtnModal("Salvar",  "rgb(36,43,141)");
        const btnSair   = this._criarBtnModal("Sair",    "rgb(128,26,15)");
        btnRow.append(btnSalvar, btnSair);
        modal.appendChild(btnRow);

        // Lógica
        const api = getApi();
        let clientesSnapshot = {};
        if (api) api.get_clientes_map().then(m => {
            clientesSnapshot = m || {};
            this._popularDuracoes(selDur, dataHora, this.getMarcacoes());
        });
        else this._popularDuracoes(selDur, dataHora, this.getMarcacoes());

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
                item.textContent = n;
                item.style.cssText = "padding:6px 10px;cursor:pointer;font-size:13px;color:black;";
                item.addEventListener("click", () => { pesquisa.value = n; sugestoes.style.display = "none"; });
                sugestoes.appendChild(item);
            });
        });

        chkDesk.addEventListener("change", () => {
            fldNome.disabled = !chkDesk.checked;
            fldTel.disabled  = !chkDesk.checked;
            pesquisa.disabled = chkDesk.checked;
            sugestoes.style.display = "none";
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

            let clienteNome, clienteNumero, tipoCliente;

            if (chkDesk.checked) {
                clienteNome   = fldNome.value.trim();
                clienteNumero = fldTel.value.trim();
                tipoCliente   = "DESCONHECIDO";
                if (!clienteNome) { errorEl.textContent = "Nome é obrigatório."; return; }
            } else {
                clienteNome = pesquisa.value.trim();
                if (!clienteNome) { errorEl.textContent = "Selecione um cliente."; return; }
                if (!clientesSnapshot[clienteNome]) { errorEl.textContent = "Cliente não encontrado."; return; }
                clienteNumero = clientesSnapshot[clienteNome].numeroTelefone || "";
                tipoCliente   = clientesSnapshot[clienteNome].tipoCliente   || "NORMAL";
            }

            const duracao = parseInt(selDur.value, 10);
            if (!duracao) { errorEl.textContent = "Selecione a duração."; return; }

            try {
                btnSalvar.disabled = true;
                const res = await api.criar_marcacao(
                    clienteNome,
                    dataHora.toISOString(),
                    duracao,
                    txaObs.value.trim()
                );
                if (res && res.success) {
                    await this.onRefresh();
                    closeModal();
                    this.atualizar();
                } else {
                    errorEl.textContent = res?.error || "Erro ao criar marcação.";
                }
            } catch (e) {
                errorEl.textContent = "Erro ao comunicar com o backend.";
            } finally {
                btnSalvar.disabled = false;
            }
        });
    }

    _popularDuracoes(sel, dataHora, marcacoes) {
        const opcoes  = [15, 30, 45, 60, 75, 90];
        const maxTime = new Date(dataHora.getFullYear(), dataHora.getMonth(), dataHora.getDate(), 21, 30);
        const minutos = dataHora.getHours() * 60 + dataHora.getMinutes();
        const is15slot = (dataHora.getMinutes() === 15 || dataHora.getMinutes() === 45);

        sel.innerHTML = "";
        const disponíveis = is15slot ? [15] : opcoes.filter(dur => {
            const fim = new Date(dataHora.getTime() + dur * 60000);
            if (fim > maxTime) return false;
            for (let i = 0; i < dur; i += 15) {
                const k = new Date(dataHora.getTime() + i * 60000).toISOString();
                if (marcacoes[k]) return false;
            }
            return true;
        });

        disponíveis.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d; opt.textContent = `${d} min`;
            sel.appendChild(opt);
        });
        if (disponíveis.includes(30)) sel.value = "30";
    }

    // Modal: detalhe marcação

    _abrirDetalheMarcacao(marcacao) {
        if (document.getElementById("detalhe-marcacao-overlay")) return;

        const overlay = this._criarOverlay("detalhe-marcacao-overlay");
        document.body.appendChild(overlay);

        const modal = this._criarModal("340px");
        overlay.appendChild(modal);

        const passou = new Date(marcacao.dataHora) < new Date();

        // Título
        const dt    = new Date(marcacao.dataHora);
        const dias  = ["Domingo","Segunda","Terça","Quarta","Quinta","Sexta","Sábado"];
        const titulo = document.createElement("div");
        titulo.textContent = `${dias[dt.getDay()]} dia ${String(dt.getDate()).padStart(2,"0")} às ${String(dt.getHours()).padStart(2,"0")}:${String(dt.getMinutes()).padStart(2,"0")}`;
        titulo.style.cssText = "color:white;font-size:17px;font-weight:bold;text-align:center;padding-bottom:10px;";
        modal.appendChild(titulo);

        // Campos info
        const addField = (label, value) => {
            const lbl = document.createElement("div");
            lbl.textContent = label;
            lbl.style.cssText = "color:white;font-size:14px;margin-top:6px;";
            const inp = document.createElement("input");
            inp.type = "text"; inp.value = value || ""; inp.readOnly = true;
            inp.style.cssText = "width:100%;padding:6px 10px;border-radius:8px;border:none;background:white;font-size:14px;box-sizing:border-box;";
            modal.append(lbl, inp);
        };

        addField("Nome:", marcacao.cliente?.nome);
        addField("Telefone:", marcacao.cliente?.numeroTelefone);
        addField("Duração:", `${marcacao.duracao} minutos`);

        const lblObs = document.createElement("div");
        lblObs.textContent = "Observações:";
        lblObs.style.cssText = "color:white;font-size:14px;margin-top:6px;";
        const txaObs = document.createElement("textarea");
        txaObs.value = marcacao.observacoes || "";
        txaObs.rows  = 3;
        txaObs.style.cssText = "width:100%;padding:6px 10px;border-radius:8px;border:none;font-size:14px;box-sizing:border-box;resize:none;";
        modal.append(lblObs, txaObs);

        // Alterar hora (apenas futuro)
        if (!passou) {
            const lblHora = document.createElement("div");
            lblHora.textContent = "Alterar Hora";
            lblHora.style.cssText = "color:white;font-size:15px;font-weight:bold;text-align:center;margin-top:12px;";
            modal.appendChild(lblHora);

            const hboxCombos = document.createElement("div");
            hboxCombos.style.cssText = "display:flex;gap:16px;justify-content:center;margin-top:6px;";

            const mkCombo = (lbl) => {
                const wrap = document.createElement("div");
                wrap.style.cssText = "display:flex;flex-direction:column;gap:4px;align-items:center;";
                const l = document.createElement("div"); l.textContent = lbl; l.style.cssText = "color:white;font-size:13px;";
                const sel = document.createElement("select");
                sel.style.cssText = "padding:4px 8px;border-radius:8px;border:none;min-width:110px;background:white;";
                wrap.append(l, sel);
                hboxCombos.appendChild(wrap);
                return sel;
            };

            const selDia  = mkCombo("Dia");
            const selHora = mkCombo("Hora");
            modal.appendChild(hboxCombos);

            DIAS_SEMANA_LONGO.forEach(d => {
                const opt = document.createElement("option"); opt.value = d; opt.textContent = d; selDia.appendChild(opt);
            });
            const diaAtual = DIAS_SEMANA_LONGO[dt.getDay() === 0 ? 6 : dt.getDay() - 1];
            selDia.value   = diaAtual;

            const popularHoras = () => {
                selHora.innerHTML = "";
                const diaIdx  = DIAS_SEMANA_LONGO.indexOf(selDia.value);
                const diasJS  = [1,2,3,4,5,6,0]; // seg=1 ... dom=0
                const diasJSv = diasJS[diaIdx];
                const novaData = new Date(dt);
                novaData.setDate(novaData.getDate() + ((diasJSv - novaData.getDay() + 7) % 7));

                const marcacoes = this.getMarcacoes();
                for (let h = HORA_ABERTURA; h <= HORA_FECHO; h++) {
                    for (let m = 0; m < 60; m += marcacao.duracao < 30 ? 15 : 30) {
                        const candidate = new Date(novaData.getFullYear(), novaData.getMonth(), novaData.getDate(), h, m);
                        if (candidate <= new Date()) continue;
                        const key = candidate.toISOString();
                        if (marcacoes[key] && marcacoes[key] !== marcacao) continue;
                        const opt = document.createElement("option");
                        opt.value = `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
                        opt.textContent = opt.value;
                        selHora.appendChild(opt);
                    }
                }
                const horaAtualStr = `${String(dt.getHours()).padStart(2,"0")}:${String(dt.getMinutes()).padStart(2,"0")}`;
                if ([...selHora.options].some(o => o.value === horaAtualStr)) selHora.value = horaAtualStr;
            };
            selDia.addEventListener("change", popularHoras);
            popularHoras();
        }

        // Botões
        const btnRow    = document.createElement("div");
        btnRow.style.cssText = "display:flex;gap:12px;justify-content:center;margin-top:14px;";
        const btnSalvar = this._criarBtnModal("Salvar",  "rgb(36,43,141)");
        const btnSair   = this._criarBtnModal("Sair",    "rgb(60,60,60)");
        btnRow.append(btnSalvar, btnSair);

        if (passou) {
            const btnFaltou = this._criarBtnModal("Faltou", "rgb(128,26,15)");
            btnFaltou.disabled = marcacao.falta;
            btnFaltou.addEventListener("click", async () => {
                const api = getApi();
                if (!api) return;
                try {
                    const res = await api.marcar_falta_marcacao(marcacao.dataHora);
                    if (res && res.success) { await this.onRefresh(); closeModal(); this.atualizar(); }
                } catch {}
            });
            btnRow.insertBefore(btnFaltou, btnSalvar);
        } else {
            const btnApagar = this._criarBtnModal("Apagar", "rgb(128,26,15)");
            btnApagar.addEventListener("click", async () => {
                const api = getApi();
                if (!api) return;
                try {
                    const res = await api.apagar_marcacao(marcacao.dataHora);
                    if (res && res.success) { await this.onRefresh(); closeModal(); this.atualizar(); }
                } catch {}
            });
            btnRow.insertBefore(btnApagar, btnSalvar);
        }

        modal.appendChild(btnRow);

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

        const obsOriginal = txaObs.value;
        btnSalvar.disabled = true;
        txaObs.addEventListener("input", () => { btnSalvar.disabled = txaObs.value === obsOriginal; });

        btnSalvar.addEventListener("click", async () => {
            const api = getApi();
            if (!api) return;
            try {
                btnSalvar.disabled = true;
                const res = await api.alterar_observacoes_marcacao(marcacao.dataHora, txaObs.value);
                if (res && res.success) { await this.onRefresh(); closeModal(); this.atualizar(); }
            } catch { btnSalvar.disabled = false; }
        });
    }

    // Highlight do slot atual

    _updateCurrentSlotHighlight() {
        document.querySelectorAll(".calendar-cell.current-slot")
            .forEach(el => el.classList.remove("current-slot"));
        const iso = getCurrentSlotDate().toISOString();
        const el  = document.querySelector(`.calendar-cell[data-datetime="${iso}"]`);
        if (el) el.classList.add("current-slot");
    }

    // Label do período

    _atualizarLabel() {
        let texto = "";
        switch (this.modoAtual) {
            case "SEMANA": {
                const fim     = new Date(this.semanaAtual.getTime() + 6 * 86400000);
                const ini     = this.semanaAtual;
                const mesIni  = ini.toLocaleDateString("pt-PT", { month: "long" });
                const mesFim  = fim.toLocaleDateString("pt-PT", { month: "long" });
                if (ini.getMonth() === fim.getMonth()) {
                    texto = `${ini.getDate()} de ${mesIni} - ${fim.getDate()} de ${mesIni}`;
                } else {
                    texto = `${ini.getDate()} de ${mesIni} - ${fim.getDate()} de ${mesFim}`;
                }
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
        const ativo  = "background-color:rgb(60,60,60);color:white;font-weight:bold;border:none;background-radius:12px;";
        const inativo = "background-color:rgb(43,40,40);color:white;font-weight:bold;border:none;border-radius:12px;";
        this.diaToggle.style.cssText    = (this.modoAtual === "DIA"    ? ativo : inativo);
        this.semanaToggle.style.cssText = (this.modoAtual === "SEMANA" ? ativo : inativo);
        this.mesToggle.style.cssText    = (this.modoAtual === "MES"    ? ativo : inativo);
    }

    // Helpers DOM

    _celula(texto, tipo) {
        const el = document.createElement("div");
        el.className = `calendar-cell${tipo ? " " + tipo : ""}`;
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

    _criarInputStyle(placeholder) {
        const inp = document.createElement("input");
        inp.type        = "text";
        inp.placeholder = placeholder;
        inp.style.cssText = "width:100%;padding:6px 10px;border-radius:8px;border:none;font-size:14px;box-sizing:border-box;";
        return inp;
    }

    _criarLabelStyle(texto) {
        const lbl = document.createElement("div");
        lbl.textContent = texto;
        lbl.style.cssText = "color:white;font-size:14px;margin-top:4px;";
        return lbl;
    }

    _criarBtnModal(texto, bg) {
        const btn = document.createElement("button");
        btn.textContent = texto;
        btn.style.cssText = `background:${bg};color:white;border:none;padding:8px 18px;border-radius:8px;cursor:pointer;font-weight:700;font-size:15px;min-width:80px;`;
        return btn;
    }
}