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
            const { data, error } = await supabaseClient.from("clientes").select("*");
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
                sucess: true,
                cliente: {
                    nome: data.nome, numeroTelefone: data.numero_telefone,
                    tipoCliente: data.tipo_cliente, faltas: data.faltas,
                    diaSemana: data.dia_semana, horaCorte: data.hora_corte,
                    rapido: !!data.rapido, temporario: data.tipo_cliente === "DESCONHECIDO",
                },
            };
        },
        // Caso me esqueça é aqui que tenho de continuar se quiser em casa

    };
}

function inicializarSiteApi() {
    window.__siteApi = _construirSiteApi();
}