/**
 * apiUtils.js
 * Wrapper centralizado para todas as chamadas à API pywebview.
 * Resolve o problema de a API poder demorar a ser injectada.
 */

/**
 * Aguarda que a API pywebview esteja disponível.
 * @param {number} timeout - ms máximos de espera
 * @param {number} interval - ms entre tentativas
 * @returns {Promise<object|null>}
 */
export async function waitForApi(timeout = 3000, interval = 100) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
        const api =
            (window.pywebview && window.pywebview.api) ||
            (typeof pywebview !== "undefined" && pywebview && pywebview.api) ||
            null;
        if (api) return api;
        await new Promise((res) => setTimeout(res, interval));
    }
    return null;
}

/**
 * Devolve a API se já estiver disponível, ou null.
 * @returns {object|null}
 */
export function getApi() {
    return (
        (window.pywebview && window.pywebview.api) ||
        (typeof pywebview !== "undefined" && pywebview && pywebview.api) ||
        null
    );
}

/**
 * Executa uma função da API com tratamento de erros centralizado.
 * @param {Function} fn - função async que recebe a api como argumento
 * @param {*} fallback - valor devolvido em caso de erro
 * @returns {Promise<*>}
 */
export async function callApi(fn, fallback = null) {
    const api = getApi();
    if (!api) {
        console.warn("[apiUtils] API não disponível.");
        return fallback;
    }
    try {
        return await fn(api);
    } catch (e) {
        console.error("[apiUtils] Erro na chamada à API:", e);
        return fallback;
    }
}

// Utilizador

export async function getUtilizadorInfo() {
    return callApi((api) => api.get_utilizador_info(), null);
}

export async function fazerLogout() {
    return callApi((api) => api.fazer_logout(), { success: false });
}

export async function mostrarLogin() {
    return callApi((api) => api.mostrar_login(), { success: false });
}

// Dados

export async function getClientesMap() {
    return callApi((api) => api.get_clientes_map(), {});
}

export async function getMarcacoesMap() {
    return callApi((api) => api.get_marcacoes_map(), {});
}

export async function getPendentes() {
    return callApi((api) => api.get_pendentes(), []);
}

// Anotações

export async function lerAnotacoes() {
    return callApi((api) => api.ler_anotacoes(), { success: false, texto: "" });
}

export async function guardarAnotacoes(texto) {
    return callApi((api) => api.guardar_anotacoes(texto), { success: false });
}

// Clientes

export async function getCliente(nome) {
    return callApi((api) => api.get_cliente(nome), { success: false });
}

export async function adicionarCliente(clienteObj) {
    return callApi((api) => api.adicionar_cliente(clienteObj), { success: false });
}

export async function alterarCliente(payload) {
    return callApi((api) => api.alterar_cliente(payload), { success: false });
}

export async function apagarCliente(nome) {
    return callApi((api) => api.apagar_cliente(nome), { success: false });
}