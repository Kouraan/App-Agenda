/**
 * ClientesModule.js
 * Gere a área de clientes: tabela, pesquisa, modal de detalhe/edição e modal de adição.
 */

import { timeToMinutes, gerarHoras } from "../utils/dateUtils.js";
import { getApi, adicionarCliente, alterarCliente, apagarCliente, getCliente } from "../utils/apiUtils.js";

export class ClientesModule {
    /**
     * @param {HTMLElement} contentEl    - contentor onde se renderiza a área
     * @param {Function}    getClientes  - callback que devolve o mapa de clientes atual
     * @param {Function}    onRefresh    - callback chamado após modificar um cliente
     */
    constructor(contentEl, getClientes, onRefresh) {
        this.content    = contentEl;
        this.getClientes = getClientes;
        this.onRefresh  = onRefresh;
    }

    // API pública

    renderizar() {
        this.content.innerHTML = "";
        const clientes = Object.values(this.getClientes());

        if (clientes.length === 0) {
            this._renderVazio();
        } else {
            this._renderTabela(clientes);
        }
    }

    // Renderização vazia

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

    // Renderização tabela

    _renderTabela(clientesArray, filtro = "") {
        this.content.innerHTML = "";

        // Toolbar
        const toolbar = document.createElement("div");
        toolbar.className = "clientes-toolbar";

        const search = document.createElement("input");
        search.className = "search-field";
        search.type        = "text";
        search.placeholder = "Pesquisar cliente...";
        search.value       = filtro;

        const addBtn = document.createElement("button");
        addBtn.className = "add-client-btn";
        addBtn.textContent = "+";
        addBtn.addEventListener("click", () => this._abrirAdicionarCliente());

        search.addEventListener("input", () => this._renderTabela(clientesArray, search.value));
        toolbar.append(search, addBtn);
        this.content.appendChild(toolbar);

        // Tabela
        const container = document.createElement("div");
        container.className = "clientes-table-container";

        const table = document.createElement("div");
        table.className = "clientes-table";

        const headers = ["Nome", "Telefone", "Tipo", "Faltas", "Dia Semana", "Hora Corte"];
        headers.forEach(h => {
            const th = document.createElement("div");
            th.className   = "table-header";
            th.textContent = h;
            table.appendChild(th);
        });

        const filtroLower = filtro.toLowerCase();
        clientesArray
            .filter(c => c.nome.toLowerCase().includes(filtroLower) || c.numeroTelefone.includes(filtroLower))
            .forEach(c => {
                const dados = [
                    c.nome,
                    c.numeroTelefone,
                    c.tipoCliente,
                    String(c.faltas),
                    c.diaSemana || "—",
                    c.horaCorte || "—",
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

        container.appendChild(table);
        this.content.appendChild(container);
    }

    // Modal de detalhe/edição

    async _abrirDetalheCliente(clienteLocal) {
        if (document.getElementById("detalhe-overlay")) return;

        // Tentar obter dados actualizados da API
        let clienteObj = clienteLocal;
        const resp = await getCliente(clienteLocal.nome);
        if (resp && resp.success && resp.cliente) clienteObj = resp.cliente;

        const overlay = this._overlay("detalhe-overlay");
        document.body.appendChild(overlay);

        const modal = this._modal("760px");
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

        // Barra topo
        const topRow = document.createElement("div");
        topRow.style.cssText = "display:flex;align-items:center;gap:8px;";
        const btnEditar = this._btn("Editar", "rgb(60,60,60)");
        const spacer    = document.createElement("div"); spacer.style.flex = "1";
        topRow.append(btnEditar, spacer);
        modal.appendChild(topRow);

        // Vista visual
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

        // Vista edição
        const editBox = document.createElement("div");
        editBox.style.cssText = "display:none;flex-direction:column;gap:10px;align-items:center;width:100%;";
        modal.appendChild(editBox);

        // helpers de campo
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
            const chk  = document.createElement("input"); chk.type = "checkbox"; chk.checked = checked;
            chk.style.transform = "scale(1.5)";
            row.append(chk, document.createTextNode(` ${label}`));
            editBox.appendChild(row);
            return chk;
        };

        const semanalChk = mkCheck("Cliente Semanal", clienteObj.tipoCliente === "SEMANAL");
        const nomeInp    = mkInput("Nome:", clienteObj.nome);
        const telInp     = mkInput("Telefone:", clienteObj.numeroTelefone);
        const diaOps     = [{v:"",l:"--"},
            {v:"Segunda",l:"Segunda"},{v:"Terça",l:"Terça"},{v:"Quarta",l:"Quarta"},
            {v:"Quinta",l:"Quinta"},{v:"Sexta",l:"Sexta"},{v:"Sábado",l:"Sábado"},{v:"Domingo",l:"Domingo"}];
        const diaSel     = mkSelect("Dia da Semana:", diaOps, clienteObj.diaSemana || "");
        const horaSel    = mkSelect("Hora do Corte:", [], clienteObj.horaCorte || "");

        // faltas
        const faltasRow  = document.createElement("div");
        faltasRow.style.cssText = "display:flex;align-items:center;gap:10px;justify-content:center;color:white;font-size:16px;font-weight:700;";
        let faltasVal    = clienteObj.faltas || 0;
        const faltasLbl  = document.createElement("div"); faltasLbl.textContent = String(faltasVal);
        faltasLbl.style.cssText = "min-width:36px;text-align:center;color:white;font-size:15px;";
        const btnMenos   = this._btn("-", "rgb(60,60,60)"); btnMenos.style.padding = "4px 10px";
        const btnMais    = this._btn("+", "rgb(60,60,60)"); btnMais.style.padding  = "4px 10px";
        const faltasTxt  = document.createElement("div"); faltasTxt.textContent = "Faltas:";
        faltasRow.append(faltasTxt, btnMenos, faltasLbl, btnMais);
        editBox.appendChild(faltasRow);

        const rapidoChk  = mkCheck("Corte Rápido", clienteObj.rapido === true || clienteObj.rapido === "true");

        btnMenos.addEventListener("click", () => { faltasVal = Math.max(0, faltasVal - 1); faltasLbl.textContent = String(faltasVal); });
        btnMais.addEventListener("click",  () => { faltasVal++;  faltasLbl.textContent = String(faltasVal); });

        // popular horas
        const popularHoras = () => {
            const step     = rapidoChk.checked ? 15 : 30;
            const diaEsc   = diaSel.value || null;
            const ocupados = new Set();
            Object.values(this.getClientes()).forEach(c => {
                if (String(c.tipoCliente).toUpperCase() !== "SEMANAL") return;
                if (c.nome === clienteObj.nome) return;
                if (!diaEsc || String(c.diaSemana).toLowerCase() !== String(diaEsc).toLowerCase()) return;
                const start = timeToMinutes(c.horaCorte);
                if (start === null) return;
                const dur = (c.rapido === true || c.rapido === "true") ? 15 : 30;
                for (let t = start; t < start + dur; t += 15) ocupados.add(t);
            });

            horaSel.innerHTML = "";
            const ph = document.createElement("option"); ph.value = ""; ph.textContent = "--"; horaSel.appendChild(ph);
            gerarHoras(step).forEach(h => {
                const min = timeToMinutes(h);
                let ok = false;
                if (step === 15) {
                    ok = !ocupados.has(min);
                } else {
                    const next = min + 15;
                    ok = next <= 21 * 60 && !ocupados.has(min) && !ocupados.has(next);
                }
                if (ok) { const o = document.createElement("option"); o.value = h; o.textContent = h; horaSel.appendChild(o); }
            });

            if (clienteObj.horaCorte && [...horaSel.options].some(o => o.value === clienteObj.horaCorte)) {
                horaSel.value = clienteObj.horaCorte;
            }
        };

        const updateSemanal = () => {
            const s = semanalChk.checked;
            diaSel.disabled  = !s;
            horaSel.disabled = !s;
            if (s) popularHoras(); else { horaSel.innerHTML = ""; }
        };

        semanalChk.addEventListener("change", updateSemanal);
        diaSel.addEventListener("change", popularHoras);
        rapidoChk.addEventListener("change", popularHoras);
        updateSemanal();

        // Botões rodapé
        const bottomRow  = document.createElement("div");
        bottomRow.style.cssText = "display:flex;justify-content:flex-end;gap:8px;margin-top:auto;";
        const btnApagar  = this._btn("Apagar", "rgb(128,26,15)");
        const btnSalvar  = this._btn("Salvar", "rgb(36,43,141)"); btnSalvar.style.display = "none";
        const btnSair    = this._btn("Sair",   "rgb(60,60,60)");
        bottomRow.append(btnApagar, btnSalvar, btnSair);
        modal.appendChild(bottomRow);

        // Toggle editar/visual
        btnEditar.addEventListener("click", () => {
            const isEditing = editBox.style.display !== "none";
            if (isEditing) {
                editBox.style.display  = "none";
                visualBox.style.display = "grid";
                btnSalvar.style.display = "none";
                btnApagar.style.display = "inline-block";
                btnEditar.textContent  = "Editar";
            } else {
                visualBox.style.display = "none";
                editBox.style.display  = "flex";
                btnSalvar.style.display = "inline-block";
                btnApagar.style.display = "none";
                btnEditar.textContent  = "Editar";
                setTimeout(() => nomeInp.focus(), 50);
            }
        });

        btnSair.addEventListener("click", closeModal);

        // Apagar
        btnApagar.addEventListener("click", async () => {
            const ok = await this._confirmar("Deseja apagar o Cliente? Esta ação é irreversível", overlay);
            if (!ok) return;
            const res = await apagarCliente(clienteObj.nome);
            if (res && res.success) { await this.onRefresh(); closeModal(); this.renderizar(); }
            else alert(res?.error || "Erro ao apagar cliente.");
        });

        // Salvar
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

            const payload = {
                nomeOriginal: clienteObj.nome,
                nome: novoNome, numeroTelefone: novoTel,
                tipoCliente: tipo, diaSemana: dia, horaCorte: hora,
                faltas: faltasVal, rapido
            };

            const res = await alterarCliente(payload);
            if (res && res.success) {
                await this.onRefresh();
                closeModal();
                this.renderizar();
            } else {
                alert(res?.error || "Erro ao guardar cliente.");
            }
        });
    }

    // Modal: adicionar cliente

    _abrirAdicionarCliente() {
        if (document.getElementById("add-cliente-overlay")) return;

        const overlay = this._overlay("add-cliente-overlay");
        document.body.appendChild(overlay);

        const modal = this._modal("360px");
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

        // Título
        const titulo = document.createElement("h3");
        titulo.textContent = "Adicionar Cliente";
        titulo.style.cssText = "color:white;text-align:center;margin:0 0 8px 0;font-size:18px;";
        modal.appendChild(titulo);

        // Semanal checkbox
        const semanalRow = document.createElement("label");
        semanalRow.style.cssText = "display:flex;align-items:center;gap:10px;color:white;font-size:14px;cursor:pointer;";
        const semanalChk = document.createElement("input"); semanalChk.type = "checkbox"; semanalChk.style.transform = "scale(1.4)";
        semanalRow.append(semanalChk, document.createTextNode(" Cliente Semanal"));
        modal.appendChild(semanalRow);

        // Campos
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

        const nomeInp   = mkF("Nome",     "Nome");
        const telInp    = mkF("Telefone", "Número de telefone");
        telInp.addEventListener("input", () => { telInp.value = telInp.value.replace(/[^\d+]/g, ""); });

        const diaSelEl  = mkSel("Dia da Semana");
        const horaSelEl = mkSel("Hora do Corte");

        ["", "Segunda","Terça","Quarta","Quinta","Sexta","Sábado","Domingo"].forEach(d => {
            const o = document.createElement("option"); o.value = d; o.textContent = d || "--"; diaSelEl.appendChild(o);
        });
        const horaPlaceholder = document.createElement("option"); horaPlaceholder.value = ""; horaPlaceholder.textContent = "Hora do Corte"; horaSelEl.appendChild(horaPlaceholder);

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

        const setDisabled = (el, dis) => {
            el.disabled = dis;
            el.style.opacity = dis ? "0.5" : "1";
            el.style.pointerEvents = dis ? "none" : "auto";
        };
        setDisabled(diaSelEl,  true);
        setDisabled(horaSelEl, true);

        semanalChk.addEventListener("change", () => {
            const s = semanalChk.checked;
            setDisabled(diaSelEl,  !s);
            rapidoChk.disabled = !s;
            if (!s) { setDisabled(horaSelEl, true); rapidoChk.checked = false; horaSelEl.innerHTML = ""; horaSelEl.appendChild(horaPlaceholder); }
        });

        const popularHorasAdd = () => {
            horaSelEl.innerHTML = ""; horaSelEl.appendChild(horaPlaceholder);
            const diaEsc   = diaSelEl.value;
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
                let ok = false;
                if (step === 15) { ok = !ocupados.has(min); }
                else { const n = min + 15; ok = n <= 21 * 60 && !ocupados.has(min) && !ocupados.has(n); }
                if (ok) { const o = document.createElement("option"); o.value = h; o.textContent = h; horaSelEl.appendChild(o); }
            });
            setDisabled(horaSelEl, false);
            errorEl.textContent = horaSelEl.options.length <= 1 ? "Nenhuma hora disponível neste dia." : "";
        };

        diaSelEl.addEventListener("change", popularHorasAdd);
        rapidoChk.addEventListener("change", popularHorasAdd);
        btnSair.addEventListener("click", closeModal);

        btnSalvar.addEventListener("click", async () => {
            errorEl.textContent = "";
            const nome    = nomeInp.value.trim();
            const telefone = telInp.value.trim();
            const tipo    = semanalChk.checked ? "SEMANAL" : "NORMAL";
            const dia     = semanalChk.checked ? (diaSelEl.value  || null) : null;
            const hora    = semanalChk.checked ? (horaSelEl.value || null) : null;
            const rapido  = rapidoChk.checked;

            if (!nome)    { errorEl.textContent = "Nome é obrigatório."; return; }
            if (!telefone){ errorEl.textContent = "Telefone é obrigatório."; return; }

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

    // Diálogo de confirmação

    _confirmar(mensagem, parentOverlay) {
        return new Promise(resolve => {
            const wrap = document.createElement("div");
            Object.assign(wrap.style, {
                position: "absolute", left: 0, top: 0, right: 0, bottom: 0,
                display: "flex", alignItems: "center", justifyContent: "center",
                zIndex: 99999,
            });

            const dialog = document.createElement("div");
            Object.assign(dialog.style, {
                minWidth: "420px", maxWidth: "90%",
                background: "rgb(20,19,19)", borderRadius: "8px", padding: "18px",
                boxShadow: "0 8px 30px rgba(0,0,0,0.6)", color: "white", textAlign: "center",
            });

            const msg  = document.createElement("div"); msg.textContent = mensagem; msg.style.cssText = "margin-bottom:16px;font-size:16px;color:#ddd;";
            const row  = document.createElement("div"); row.style.cssText = "display:flex;justify-content:center;gap:12px;";
            const nao  = this._btn("Não", "rgb(96,96,96)");
            const sim  = this._btn("Sim", "rgb(128,26,15)");
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

    // Helpers DOM

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