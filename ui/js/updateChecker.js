(function () {
    async function getApi(timeout = 3000, interval = 100) {
        const start = Date.now();
        while (Date.now() - start < timeout) {
            const api = window.pywebview?.api;
            if (api) return api;
            await new Promise(r => setTimeout(r, interval));
        }
        return null;
    }

    function mostrarBanner(info) {
        const banner = document.createElement("div");
        banner.id = "updateBanner";
        banner.style.cssText =
            "position:fixed;top:0;left:0;right:0;background:rgb(36,43,141);color:white;" +
            "padding:10px 16px;display:flex;align-items:center;justify-content:center;" +
            "gap:16px;z-index:99999;font-size:14px;font-weight:bold;flex-wrap:wrap;text-align:center;";
        banner.innerHTML = `
            <span>Nova versão disponível: ${info.versao_remota} (tens ${info.versao_local})</span>
            <button id="btnBaixarUpdate" style="background:#e5c158;color:#111;border:none;
                padding:6px 14px;border-radius:6px;cursor:pointer;font-weight:bold;">Baixar agora</button>
        `;
        document.body.prepend(banner);

        document.getElementById("btnBaixarUpdate").addEventListener("click", async () => {
            const btn = document.getElementById("btnBaixarUpdate");
            btn.disabled = true;
            btn.textContent = "A atualizar...";
            const api = window.pywebview.api;
            const res = await api.baixar_e_atualizar(info.url_download);
            if (!res || !res.success) {
                btn.disabled = false;
                btn.textContent = "Erro — tentar novamente";
            }
            // se success, a app fecha-se sozinha e reabre atualizada
        });
    }

    async function checarAtualizacao() {
        const api = await getApi();
        if (!api) return;
        try {
            const res = await api.verificar_atualizacao();
            if (res && res.success && res.tem_atualizacao) {
                mostrarBanner(res);
            }
        } catch (e) {
            console.error("[updateChecker]", e);
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        setTimeout(checarAtualizacao, 500);
    });
})();