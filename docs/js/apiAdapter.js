// Substitui apiUtils.js do bundle.js - mesma "API pública", mas fala com Supabase.

function getApi() {
    return window.__siteApi || null;
}

async function waitForApi(timeout = 3000, interval = 100) {
    return getApi();
}

async function callApi(fn, fallback = null) {
    const api = getApi();
    if (!api) { console.warn("[apiAdapter] API não disponível."); return fallback; }
    try { return await fn(api); }
    catch (e) { console.error("[apiAdapter] Erro:", e); return fallback; }
}

function _nowIsoUtc() {
    return new Date().toISOString();
}

async function _inserirTombstone(tabela, chave) {
    try {
        await supabaseClient.from("sync_tombstones").insert({
            tabela, chave, apagado_em: _nowIsoUtc()
        });
    } catch (e) {
        console.error("[apiAdapter] Erro ao inserir tombstone:", e);
    }
}

async function _limparMarcacoesAntigas(mesesRetencao = 3) {
    try {
        const hoje = new Date();
        const corte = new Date(hoje.getFullYear(), hoje.getMonth() - mesesRetencao, 1);
        const corteISO = toLocalISOString(corte);

        const { data: antigas, error: errSel } = await supabaseClient
            .from("marcacoes").select("data_hora").lt("data_hora", corteISO);
        if (errSel) { console.error("[apiAdapter] Erro ao selecionar marcacoes antigas:", errSel); return; }
        if (!antigas || antigas.length === 0) return;

        const { error: errDel } = await supabaseClient
            .from("marcacoes").delete().lt("data_hora", corteISO);
        if (errDel) { console.error("[apiAdapter] Erro ao apagar marcacoes antigas:", errDel); return; }

        for (const m of antigas) {
            await _inserirTombstone("marcacoes", m.data_hora);
        }
        console.log(`[apiAdapter] ${antigas.length} marcações antigas removidas.`);
    } catch (e) {
        console.error("[apiAdapter] Erro ao limpar marcacoes antigas:", e);
    }
}

function _construirSiteApi() {
    return {
        get_utilizador_info: async () => ({ nome: "Acesso Web", authenticated: true}),

        fazer_logout: async () => {
            await supabaseClient.auth.signOut();
            return { success: true };
        },
        mostrar_login: async () => {
            location.reload();
            return { success: true };
        },

        get_clientes_map: async () => {
            const { data, error } = await supabaseClient.from("clientes").select("*").order("nome", { ascending: true });
            if (error) { console.error(error); return {}; }
            const mapa = {};
            for (const row of data) {
                mapa[row.nome] = {
                    nome: row.nome,
                    numeroTelefone: row.numero_telefone,
                    tipoCliente: row.tipo_cliente,
                    faltas: row.faltas,
                    diaSemana: row.dia_semana,
                    horaCorte: row.hora_corte,
                    rapido: !!row.rapido,
                    temporario: row.tipo_cliente === "DESCONHECIDO",
                };
            }
            return mapa;
        },

        get_cliente: async (nome) => {
            const { data, error } = await supabaseClient.from("clientes").select("*").eq("nome", nome).maybeSingle();
            if (error || !data) return { success: false, error: "Cliente não encontrado." };
            return {
                success: true,
                cliente: {
                    nome: data.nome, numeroTelefone: data.numero_telefone,
                    tipoCliente: data.tipo_cliente, faltas: data.faltas,
                    diaSemana: data.dia_semana, horaCorte: data.hora_corte,
                    rapido: !!data.rapido, temporario: data.tipo_cliente === "DESCONHECIDO",
                },
            };
        },
        
        // Clientes
        adicionar_cliente: async (clienteDict) => {
            const nome = (clienteDict.nome || "").trim();
            const numero = (clienteDict.numeroTelefone || "").trim();
            const tipo = clienteDict.tipoCliente || "NORMAL";
            const dia = clienteDict.diaSemana || null;
            const hora = clienteDict.horaCorte || null;
            const rapido = !!clienteDict.rapido;

            if (!Validation.nomeValido(nome)) return { success: false, error: "Nome inválido." };
            if (!Validation.numeroTelefoneValido(numero)) return { success: false, error: "Número de telefone inválido." };

            const { data: existeNome } = await supabaseClient.from("clientes").select("nome").ilike("nome", nome).maybeSingle();
            if (existeNome) return { success: false, error: "Já existe um cliente com esse nome." };
            const { data: existeNumero } = await supabaseClient.from("clientes").select("nome").eq("numero_telefone", numero).maybeSingle();
            if (existeNumero) return { success: false, error: "Já existe um cliente com esse número de telefone." };

            if (tipo === "SEMANAL") {
                if (!dia || !hora) return { success: false, error: "Dia e hora obrigatórios para cliente semanal." };
                const { data: clientesSemanais } = await supabaseClient.from("clientes")
                    .select("dia_semana, hora_corte").eq("tipo_cliente", "SEMANAL");
                const conflito = (clientesSemanais || []).some(c => 
                    (c.dia_semana || "").toUpperCase() === dia.toUpperCase() && c.hora_corte === hora
                );
                if (conflito) return { success: false, error: "Já existe um cliente semanal nesse horário." };
            }

            const { error } = await supabaseClient.from("clientes").insert({
                nome, numero_telefone: numero, tipo_cliente: tipo, faltas: 0,
                dia_semana: tipo === "SEMANAL" ? dia : null,
                hora_corte: tipo === "SEMANAL" ? hora : null,
                rapido: tipo === "SEMANAL" ? rapido : false,
                updated_at: _nowIsoUtc(),
            });
            if (error) return { success: false, error: "Erro ao guardar cliente." };

            if (tipo === "SEMANAL") {
                await _gerarMarcacoesSemanaisEGuardar(nome, dia, hora, rapido);
            }
            return { success: true, message: "Cliente adicionado com sucesso." };
        },

        alterar_cliente: async (clienteDict) => {
            const nomeOriginal = clienteDict.nomeOriginal;
            const { data: atual } = await supabaseClient.from("clientes").select("*").eq("nome", nomeOriginal).maybeSingle();
            if (!atual) return { success: false, error: "Cliente original não encontrado." };

            const novoNome = (clienteDict.nome || "").trim();
            const numero = (clienteDict.numeroTelefone || "").trim();
            const tipoNovo = clienteDict.tipoCliente || "NORMAL";
            const dia = clienteDict.diaSemana || null;
            const hora = clienteDict.horaCorte || null;
            const faltas = parseInt(clienteDict.faltas || 0, 10);
            const rapido = !!clienteDict.rapido;

            if (!Validation.nomeValido(novoNome)) return { success: false, error: "Nome inválido." };
            if (!Validation.numeroTelefoneValido(numero)) return { success: false, error: "Número de telefone inválido." };

            if (novoNome.toLowerCase() !== nomeOriginal.toLowerCase()) {
                const { data: dup } = await supabaseClient.from("clientes").select("nome")
                    .ilike("nome", novoNome).neq("nome", nomeOriginal).maybeSingle();
                if (dup) return { success: false, error: "Já existe um cliente com esse nome." };
            }
            if (numero !== atual.numero_telefone) {
                const { data: dupTel } = await supabaseClient.from("clientes").select("nome")
                    .eq("numero_telefone", numero).neq("nome", nomeOriginal).maybeSingle();
                if (dupTel) return { success: false, error: "Já existe um cliente com esse número de telefone." };
            }

            const { error } = await supabaseClient.from("clientes").update({
                nome: novoNome, numero_telefone: numero, tipo_cliente: tipoNovo,
                faltas, dia_semana: tipoNovo === "SEMANAL" ? dia : null,
                hora_corte: tipoNovo === "SEMANAL" ? hora : null,
                rapido: tipoNovo === "SEMANAL" ? rapido : false,
                updated_at: _nowIsoUtc(),
            }).eq("nome", nomeOriginal);
            if (error) return { success: false, error: "Erro ao guardar alterações." };

            // Atualizar cliente_nome nas marcações se o nome mudou
            if (novoNome !== nomeOriginal) {
                await supabaseClient.from("marcacoes").update({ cliente_nome: novoNome })
                    .eq("cliente_nome", nomeOriginal);
            }

            const horarioSemanalMudou = tipoNovo === "SEMANAL" && atual.tipo_cliente === "SEMANAL" &&
                (atual.dia_semana !== dia || atual.hora_corte !== hora || !!atual.rapido !== rapido);

            if (atual.tipo_cliente === "SEMANAL" && tipoNovo !== "SEMANAL") {
                await _apagarMarcacoesFuturasCliente(novoNome);
            } else if (atual.tipo_cliente !== "SEMANAL" && tipoNovo === "SEMANAL") {
                await _gerarMarcacoesSemanaisEGuardar(novoNome, dia, hora, rapido);
            } else if (horarioSemanalMudou) {
                await _apagarMarcacoesFuturasCliente(novoNome);
                await _gerarMarcacoesSemanaisEGuardar(novoNome, dia, hora, rapido);
            }

            return { success: true };
        },

        apagar_cliente: async (nome) => {
            const hojeISO = new Date(); hojeISO.setHours(0, 0, 0, 0);

            const { data: futurasApagadas } = await supabaseClient.from("marcacoes")
                .select("data_hora")
                .eq("cliente_nome", nome).gte("data_hora", toLocalISOString(hojeISO));

            await supabaseClient.from("marcacoes").delete()
                .eq("cliente_nome", nome).gte("data_hora", toLocalISOString(hojeISO));
            for (const m of futurasApagadas || []) {
                await _inserirTombstone("marcacoes", m.data_hora);
            }

            const { error } = await supabaseClient.from("clientes").delete().eq("nome", nome);
            if (error) return { success: false, error: "Erro ao apagar cliente." };
            await _inserirTombstone("clientes", nome);
            return { success: true };
        },

        get_total_marcacoes_cliente: async (nome) => {
            const agora = new Date().toISOString();
            const { count, error } = await supabaseClient.from("marcacoes")
                .select("*", { count: "exact", head: true })
                .eq("cliente_nome", nome).lt("data_hora", agora).eq("falta", 0);
            if (error) return { success: false, error: error.message, total: 0 };
            return { success: true, total: count || 0 };
        },

        get_estatisticas_cliente: async (nome) => {
            const { data, error } = await supabaseClient.from("marcacoes")
                .select("*").eq("cliente_nome", nome).order("data_hora", { ascending: true });
            if (error) return { success: false, error: error.message };

            if (!data || data.length === 0) {
                return {
                    success: true, totalRealizadas: 0, totalFaltas: 0,
                    taxaFaltas: 0,
                    hoje: null, futuras: [], passadas: []
                };
            }

            const agora = new Date();
            const hojeStr = agora.toDateString();

            const visitas = [];
            let atual = null;
            for (const row of data) {
                const dt = new Date(row.data_hora);
                const dur = row.duracao;
                if (atual && dt.getTime() === atual.fim.getTime()) {
                    atual.duracao += dur;
                    atual.fim = new Date(dt.getTime() + dur * 60000);
                    atual.falta = atual.falta || !!row.falta;
                    atual.observacoes = atual.observacoes || row.observacoes || "";
                } else {
                    if (atual) visitas.push(atual);
                    atual = {
                        dataHora: dt,
                        fim: new Date(dt.getTime() + dur * 60000),
                        duracao: dur,
                        falta: !!row.falta,
                        observacoes: row.observacoes || "",
                    };
                }
            }
            if (atual) visitas.push(atual);

            const hojeLista = visitas.filter(v => v.dataHora.toDateString() === hojeStr);
            const futuras   = visitas.filter(v => v.dataHora.toDateString() !== hojeStr && v.dataHora > agora)
                .sort((a, b) => a.dataHora - b.dataHora);
            const ocorridas = visitas.filter(v => v.dataHora < agora);
            const passadas  = ocorridas.filter(v => v.dataHora.toDateString() !== hojeStr)
                .sort((a, b) => a.dataHora - b.dataHora);

            const totalFaltas     = ocorridas.filter(v => v.falta).length;
            const totalRealizadas = ocorridas.length - totalFaltas;
            const taxaFaltas      = ocorridas.length ? Math.round((totalFaltas / ocorridas.length) * 1000) / 10 : 0;

            const toDict = (v) => ({
                dataHora: toLocalISOString(v.dataHora),
                duracao: v.duracao,
                falta: v.falta,
                observacoes: v.observacoes,
            });

            return {
                success: true, totalRealizadas, totalFaltas, taxaFaltas,
                hoje: hojeLista.length ? toDict(hojeLista[0]) : null,
                futuras: futuras.slice(0, 3).map(toDict),
                passadas: passadas.slice(-3).map(toDict),
            };
        },

        // Marcacoes
        get_marcacoes_map: async () => {
            const [{ data, error }, { data: clientesData }] = await Promise.all([
                supabaseClient.from("marcacoes").select("*"),
                supabaseClient.from("clientes").select("nome, numero_telefone, faltas"),
            ]);
            if (error) { console.error(error); return {}; }
            const clientesMap = {};
            (clientesData || []).forEach(c => { clientesMap[c.nome] = c; });
            const mapa = {};
            for (const row of data) {
                mapa[row.data_hora] = _marcacaoRowParaDict(row, clientesMap);
            }
            return mapa;
        },

        criar_marcacao: async (clienteNome, dataHora, duracao, observacoes = "") => {
            return await _criarMarcacaoInterna(clienteNome, dataHora, parseInt(duracao, 10), observacoes, null);
        },

        criar_marcacao_desconhecido: async (nomeCliente, numeroTelefone, dataHora, duracao, observacoes = "") => {
            return await _criarMarcacaoInterna(nomeCliente, dataHora, parseInt(duracao, 10), observacoes, numeroTelefone || "");
        },

        apagar_marcacao: async (dataHora) => {
            const { error } = await supabaseClient.from("marcacoes").delete().eq("data_hora", dataHora);
            if (error) return { success: false, error: "Erro ao apagar marcação." };
            await _inserirTombstone("marcacoes", dataHora);
            return { success: true };
        },

        alterar_marcacao: async (dataHoraOriginal, dataHoraNova, observacoes) => {
            const { data: existente } = await supabaseClient.from("marcacoes").select("*")
                .eq("data_hora", dataHoraOriginal).maybeSingle();
            if (!existente) return { success: false, error: "Marcação não encontrada." };

            const dtNova = dataHoraNova || dataHoraOriginal;

            if (dtNova !== dataHoraOriginal) {
                const { data: ocupado } = await supabaseClient.from("marcacoes").select("data_hora")
                    .eq("data_hora", dtNova).maybeSingle();
                if (ocupado) return { success: false, error: "Já existe uma marcação nessa hora." };

                await supabaseClient.from("marcacoes").delete().eq("data_hora", dataHoraOriginal);
                await _inserirTombstone("marcacoes", dataHoraOriginal);

                const { error } = await supabaseClient.from("marcacoes").insert({
                    data_hora: dtNova, cliente_nome: existente.cliente_nome,
                    duracao: existente.duracao,
                    observacoes: observacoes !== null && observacoes !== undefined ? observacoes : existente.observacoes,
                    falta: existente.falta,
                    updated_at: _nowIsoUtc(),
                });
                if (error) return { success: false, error: "Erro ao alterar marcação." };
            } else if (observacoes !== null && observacoes !== undefined) {
                const { error } = await supabaseClient.from("marcacoes")
                    .update({ observacoes, updated_at: _nowIsoUtc() }).eq("data_hora", dataHoraOriginal);
                if (error) return { success: false, error: "Erro ao guardar observações." };
            }
            return { success: true };
        },

        marcar_falta_marcacao: async (dataHora) => {
            const { data: marcacao } = await supabaseClient.from("marcacoes").select("*")
                .eq("data_hora", dataHora).maybeSingle();
            if (!marcacao) return { success: false, error: "Marcação não encontrada." };

            const { error } = await supabaseClient.from("marcacoes").update({ falta: 1, updated_at: _nowIsoUtc() }).eq("data_hora", dataHora);
            if (error) return { success: false, error: "Erro ao marcar falta." };

            if (marcacao.cliente_nome) {
                const { data: cliente } = await supabaseClient.from("clientes").select("faltas")
                    .eq("nome", marcacao.cliente_nome).maybeSingle();
                if (cliente) {
                    await supabaseClient.from("clientes").update({ faltas: (cliente.faltas || 0) + 1, updated_at: _nowIsoUtc() })
                        .eq("nome", marcacao.cliente_nome);
                }
            }
            return { success: true };
        },

        trocar_cliente_marcacao: async (dataHora, novoClienteNome) => {
            const nome = (novoClienteNome || "").trim();
            if (!nome) return { success: false, error: "Selecione um cliente." };
            const { data: cliente } = await supabaseClient.from("clientes").select("nome").eq("nome", nome).maybeSingle();
            if (!cliente) return { success: false, error: "Cliente não encontrado na base de dados." };

            const { error } = await supabaseClient.from("marcacoes")
                .update({ cliente_nome: nome, updated_at: _nowIsoUtc() }).eq("data_hora", dataHora);
            if (error) return { success: false, error: "Erro ao trocar cliente." };
            return { success: true };
        },

        alterar_cliente_desconhecido_marcacao: async (dataHora, novoNome, novoNumero) => {
            const nome = (novoNome || "").trim();
            if (!nome) return { success: false, error: "O nome não pode ser vazio." };
            // Nota: cliente "desconhecido" só existe embutido na marcação (cliente_nome),
            // não há linha própria em `clientes`. Apenas atualizamos o nome na marcação.
            const { error } = await supabaseClient.from("marcacoes")
                .update({ cliente_nome: nome, updated_at: _nowIsoUtc() }).eq("data_hora", dataHora);
            if (error) return { success: false, error: "Erro ao atualizar." };
            return { success: true };
        },

        get_marcacoes_trocaveis: async (dataHoraOriginal, dataAlvo) => {
            const dtOrig = new Date(dataHoraOriginal);
            const dtAlvo = new Date(dataAlvo);
            const diaInicio = new Date(dtAlvo.getFullYear(), dtAlvo.getMonth(), dtAlvo.getDate(), 0, 0, 0);
            const diaFim = new Date(dtAlvo.getFullYear(), dtAlvo.getMonth(), dtAlvo.getDate(), 23, 59, 59);

            const { data: orig } = await supabaseClient.from("marcacoes").select("*")
                .eq("data_hora", dataHoraOriginal).maybeSingle();
            if (!orig) return { success: false, error: "Dados inválidos.", opcoes: [] };

            const { data: candidatas } = await supabaseClient.from("marcacoes").select("*")
                .gte("data_hora", toLocalISOString(diaInicio)).lte("data_hora", toLocalISOString(diaFim));

            const agora = new Date();
            const opcoes = [];
            for (const m of (candidatas || [])) {
                const dt = new Date(m.data_hora);
                if (m.data_hora === dataHoraOriginal || dt <= agora) continue;
                if (!_trocasCompativel(dtOrig, orig.duracao, dt, m.duracao, candidatas)) continue;
                opcoes.push({
                    dataHora: m.data_hora,
                    hora: `${String(dt.getHours()).padStart(2, "0")}:${String(dt.getMinutes()).padStart(2, "0")}`,
                    nome: m.cliente_nome || "N/A",
                    duracao: m.duracao,
                });
            }
            opcoes.sort((a, b) => a.dataHora.localeCompare(b.dataHora));
            return { success: true, opcoes };
        },

        trocar_marcacoes: async (dataHoraA, dataHoraB) => {
            if (dataHoraA === dataHoraB) return { success: false, error: "Selecione duas marcações diferentes." };
            const { data: marcA } = await supabaseClient.from("marcacoes").select("*").eq("data_hora", dataHoraA).maybeSingle();
            const { data: marcB } = await supabaseClient.from("marcacoes").select("*").eq("data_hora", dataHoraB).maybeSingle();
            if (!marcA || !marcB) return { success: false, error: "Marcação não encontrada." };

            const dtA = new Date(dataHoraA), dtB = new Date(dataHoraB);
            const { data: todasDoDia } = await supabaseClient.from("marcacoes").select("*");
            if (!_trocasCompativel(dtA, marcA.duracao, dtB, marcB.duracao, todasDoDia)) {
                return { success: false, error: "Estas marcações não são compatíveis para troca." };
            }

            if (marcA.duracao === marcB.duracao) {
                await supabaseClient.from("marcacoes").update({ cliente_nome: marcB.cliente_nome, updated_at: _nowIsoUtc() }).eq("data_hora", dataHoraA);
                await supabaseClient.from("marcacoes").update({ cliente_nome: marcA.cliente_nome, updated_at: _nowIsoUtc() }).eq("data_hora", dataHoraB);
            } else {
                const [m30, m15] = marcA.duracao === 30 ? [marcA, marcB] : [marcB, marcA];
                const [dt30, dt15] = marcA.duracao === 30 ? [dtA, dtB] : [dtB, dtA];
                const dt30Segundo = new Date(dt30.getTime() + 15 * 60000);

                await supabaseClient.from("marcacoes").delete().eq("data_hora", toLocalISOString(dt30));
                await supabaseClient.from("marcacoes").delete().eq("data_hora", toLocalISOString(dt15));
                await supabaseClient.from("marcacoes").delete().eq("data_hora", toLocalISOString(dt30Segundo));

                await _inserirTombstone("marcacoes", toLocalISOString(dt30));
                await _inserirTombstone("marcacoes", toLocalISOString(dt15));
                await _inserirTombstone("marcacoes", toLocalISOString(dt30Segundo));

                await supabaseClient.from("marcacoes").insert({
                    data_hora: toLocalISOString(dt15), cliente_nome: m30.cliente_nome,
                    duracao: 30, observacoes: m30.observacoes, falta: m30.falta,
                    updated_at: _nowIsoUtc()
                });
                await supabaseClient.from("marcacoes").insert({
                    data_hora: toLocalISOString(dt30), cliente_nome: m15.cliente_nome,
                    duracao: 15, observacoes: m15.observacoes, falta: m15.falta,
                    updated_at: _nowIsoUtc()
                });
            }
            return { success: true };
        },

        get_horas_disponiveis_data: async (dataHoraOriginal, dataAlvo, duracao) => {
            duracao = parseInt(duracao, 10);
            const dtAlvo = new Date(dataAlvo);
            const agora = new Date();
            const horaFechoFim = new Date(dtAlvo.getFullYear(), dtAlvo.getMonth(), dtAlvo.getDate(), 21, 30);
            const step = duracao === 15 ? 15 : 30;

            const diaInicio = new Date(dtAlvo.getFullYear(), dtAlvo.getMonth(), dtAlvo.getDate(), 0, 0, 0);
            const diaFim = new Date(dtAlvo.getFullYear(), dtAlvo.getMonth(), dtAlvo.getDate(), 23, 59, 59);
            const { data: marcacoesDia } = await supabaseClient.from("marcacoes").select("data_hora")
                .gte("data_hora", toLocalISOString(diaInicio)).lte("data_hora", toLocalISOString(diaFim));
            const ocupadas = new Set((marcacoesDia || []).map(m => m.data_hora));

            const horas = [];
            for (let h = 7; h <= 21; h++) {
                for (let m = 0; m < 60; m += step) {
                    if (h === 21 && m > 0) continue;
                    const candidate = new Date(dtAlvo.getFullYear(), dtAlvo.getMonth(), dtAlvo.getDate(), h, m);
                    const fimCandidate = new Date(candidate.getTime() + duracao * 60000);
                    if (fimCandidate > horaFechoFim) continue;
                    if (candidate <= agora) continue;

                    let livre = true;
                    for (let i = 0; i < duracao; i += 15) {
                        const bloco = new Date(candidate.getTime() + i * 60000);
                        const blocoISO = toLocalISOString(bloco);
                        if (ocupadas.has(blocoISO) && blocoISO !== dataHoraOriginal) { livre = false; break; }
                    }
                    if (livre) horas.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
                }
            }
            return { success: true, horas };
        },

        // Anotações
        ler_anotacoes: async () => {
            const { data, error } = await supabaseClient.from("anotacoes").select("texto").eq("id", 1).maybeSingle();
            if (error) return { success: false, texto: "" };
            return { success: true, texto: data ? data.texto : "" };
        },

        guardar_anotacoes: async (texto) => {
            const { error } = await supabaseClient.from("anotacoes").upsert({ id: 1, texto: texto || "" });
            if (error) return { success: false, error: "Erro ao guardar anotações." };
            return { success: true };
        },

        // Pendentes
        get_pendentes: async () => {
            const { data, error } = await supabaseClient.from("pendentes").select("*");
            if (error) return [];
            return (data || []).map(p => ({ nome: p.nome, numeroTelefone: p.numero_telefone }));
        },

        adicionar_pendente: async (nome, numeroTelefone = "") => {
            nome = (nome || "").trim();
            if (!nome) return { success: false, error: "Nome é obrigatório." };
            const { data: dup } = await supabaseClient.from("pendentes").select("nome").ilike("nome", nome).maybeSingle();
            if (dup) return { success: false, error: "Este cliente já está na lista de pendentes." };
            const { error } = await supabaseClient.from("pendentes").insert({ nome, numero_telefone: numeroTelefone || "" });
            if (error) return { success: false, error: "Erro ao adicionar pendente." };
            return { success: true };
        },

        remover_pendente: async (nome) => {
            const { error } = await supabaseClient.from("pendentes").delete().ilike("nome", (nome || "").trim());
            if (error) return { success: false, error: "Erro ao remover pendente." };
            return { success: true };
        },

        guardar_pendentes_lista: async (lista) => {
            await supabaseClient.from("pendentes").delete().neq("nome", "__nunca__");
            if (lista && lista.length) {
                await supabaseClient.from("pendentes").insert(
                    lista.map(p => ({ nome: p.nome || "", numero_telefone: p.numeroTelefone || "" }))
                );
            }
            return { success: true };
        },
    };
}

function _marcacaoRowParaDict(row, clientesMap = {}) {
    const nomeCliente = row.cliente_nome || "N/A";
    const registado = clientesMap[nomeCliente];
    return {
        dataHora: row.data_hora,
        cliente: registado ? {
            nome: registado.nome,
            numeroTelefone: registado.numero_telefone || "",
            tipoCliente: "NORMAL",
            duaSemana: null, horaCorte: null, rapido: false, temporario: false,
            faltas: registado.faltas || 0,
        } : {
            nome: nomeCliente,
            numeroTelefone: "",
            tipoCliente: "DESCONHECIDO",
            diaSemana: null, horaCorte: null, rapido: false, temporario: true,
        },
        duracao: row.duracao,
        observacoes: row.observacoes || "",
        falta: !!row.falta,
    };
}

function _trocasCompativel(dtA, durA, dtB, durB, todasMarcacoes) {
    if (durA === durB) return true;
    let dt15, dt30;
    if (durA === 15 && durB === 30) { dt15 = dtA; dt30 = dtB; }
    else if (durB === 15 && durA === 30) { dt15 = dtB; dt30 = dtA; }
    else return false;

    if (dt15.getMinutes() % 30 !== 0) return false;
    const mapa = new Map((todasMarcacoes || []).map(m => [m.data_hora, m]));

    const dt15Segundo = new Date(dt15.getTime() + 15 * 60000);
    const ocupante = mapa.get(toLocalISOString(dt15Segundo));
    if (ocupante && toLocalISOString(dt15Segundo) !== toLocalISOString(dt30)) return false;

    const dt30Segundo = new Date(dt30.getTime() + 15 * 60000);
    const ocupante30 = mapa.get(toLocalISOString(dt30Segundo));
    if (ocupante30 && toLocalISOString(dt30Segundo) !== toLocalISOString(dt15)) return false;

    return true;
}

async function _apagarMarcacoesFuturasCliente(nome) {
    const hoje = new Date(); hoje.setHours(0, 0, 0, 0);
    await supabaseClient.from("marcacoes").delete()
        .eq("cliente_nome", nome).gte("data_hora", toLocalISOString(hoje));
}

async function _gerarMarcacoesSemanaisEGuardar(nomeCliente, diaSemanaPt, horaStr, rapido) {
    const { data: marcacoesExistentes } = await supabaseClient.from("marcacoes").select("*");
    const clienteObj = { nome: nomeCliente, diaSemana: diaSemanaPt, horaCorte: horaStr, rapido };
    const novas = MarcacoesSemanais.gerarMarcacoesSemanais(clienteObj, marcacoesExistentes || [], new Date(), 6);
    for (const nova of novas) {
        await supabaseClient.from("marcacoes").insert({
            data_hora: nova.dataHora, cliente_nome: nomeCliente,
            duracao: nova.duracao, observacoes: "", falta: 0,
            updated_at: _nowIsoUtc(),
        });
    }
}

async function _criarMarcacaoInterna(clienteNome, dataHora, duracao, observacoes, numeroDesconhecido) {
    const dataHoraObj = new Date(dataHora);
    if (isNaN(dataHoraObj.getTime())) return { success: false, error: "Data/hora inválida." };

    const { data: todasMarcacoes } = await supabaseClient.from("marcacoes").select("*");
    const mapa = new Map((todasMarcacoes || []).map(m => [m.data_hora, m]));

    let minutosRestantes = duracao;
    let blocoAtual = new Date(dataHoraObj);
    const inserts = [];

    while (minutosRestantes > 0) {
        const blocoISO = toLocalISOString(blocoAtual);
        const existente = mapa.get(blocoISO);

        if (existente && existente.duracao === 15) {
            blocoAtual = new Date(blocoAtual.getTime() + 15 * 60000);
            continue;
        }

        const proximoISO = toLocalISOString(new Date(blocoAtual.getTime() + 15 * 60000));
        const proximo = mapa.get(proximoISO);
        let blocoDur = 15;
        if (proximo && proximo.duracao === 15) blocoDur = 15;
        else if (minutosRestantes >= 30) blocoDur = 30;
        else if (minutosRestantes >= 15) blocoDur = 15;

        if (existente && existente.duracao >= 30) {
            blocoAtual = new Date(blocoAtual.getTime() + blocoDur * 60000);
            continue;
        }

        inserts.push({
            data_hora: blocoISO, cliente_nome: clienteNome,
            duracao: blocoDur, observacoes: observacoes || "", falta: 0,
            updated_at: _nowIsoUtc(),
        });
        mapa.set(blocoISO, { data_hora: blocoISO, duracao: blocoDur });

        minutosRestantes -= blocoDur;
        blocoAtual = new Date(blocoAtual.getTime() + blocoDur * 60000);
    }

    for (const ins of inserts) {
        const { error } = await supabaseClient.from("marcacoes").insert(ins);
        if (error) return { success: false, error: "Erro ao criar marcação." };
    }
    return { success: true, message: "Marcação criada com sucesso." };
}

function inicializarSiteApi() {
    window.__siteApi = _construirSiteApi();
}