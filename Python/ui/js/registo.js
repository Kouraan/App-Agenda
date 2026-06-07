class RegistoController {
    constructor() {
        this.nomeField     = document.getElementById("nomeField");
        this.passwordField = document.getElementById("passwordField");
        this.errorLabel    = document.getElementById("errorLabel");
        this.registoForm   = document.getElementById("registoForm");
        this.registoBtn    = document.getElementById("registoBtn");

        this._bind();
        // Foco inicial
        setTimeout(() => this.nomeField.focus(), 100);
    }

    // Bindings

    _bind() {
        this.registoForm.addEventListener("submit", (e) => this._handleRegisto(e));

        // Enter em qualquer campo submete
        [this.nomeField, this.passwordField].forEach(field => {
            field.addEventListener("keypress", (e) => {
                if (e.key === "Enter") this._handleRegisto(e);
            });
            // Limpar erro ao começar a escrever
            field.addEventListener("input", () => this._clearError());
        });

        // Clicar fora do formulário remove o foco (comportamento JavaFX)
        document.addEventListener("click", (e) => {
            if (!this.registoForm.contains(e.target)) {
                document.activeElement?.blur();
            }
        });
    }

    // Registo

    async _handleRegisto(e) {
        if (e) e.preventDefault();

        const nome     = this.nomeField.value.trim();
        const password = this.passwordField.value;

        this._clearError();

        // Validação básica no cliente (duplica a validação do backend para UX imediata)
        if (!nome || nome.length < 2) {
            this._showError("Nome inválido.");
            return;
        }
        if (!password || password.length < 6) {
            this._showError("Password tem de ter mais de 5 caracteres.");
            return;
        }

        this._setLoading(true);

        try {
            const api = await this._getApi();
            if (!api) throw new Error("API não disponível");

            const result = await api.registar_utilizador(nome, password);

            if (result?.success) {
                // Registo bem-sucedido — ir para login
                window.location.href = "login.html";
            } else {
                // Erro devolvido pelo backend
                this._showError(result?.error || "Erro ao registar utilizador.");
            }
        } catch (err) {
            console.error("[Registo] Erro:", err);
            this._showError("Erro inesperado. Tente novamente.");
        } finally {
            this._setLoading(false);
        }
    }

    // Estado de loading

    _setLoading(loading) {
        this.registoBtn.disabled    = loading;
        this.registoBtn.textContent = loading ? "A processar..." : "Registar";
    }

    // Erros

    _showError(message) {
        this.errorLabel.textContent = message || "Erro desconhecido.";
        this.errorLabel.classList.add("visible");
        this._animateShake(this.errorLabel);
    }

    _clearError() {
        this.errorLabel.classList.remove("visible");
        this.errorLabel.textContent = "";
    }

    _animateShake(el) {
        el.style.animation = "";
        // Forçar reflow para reiniciar animação
        void el.offsetWidth;
        el.style.animation = "shake 0.5s ease-in-out";
        el.addEventListener("animationend", () => { el.style.animation = ""; }, { once: true });
    }

    // API

    async _getApi(timeout = 3000, interval = 100) {
        const start = Date.now();
        while (Date.now() - start < timeout) {
            const api =
                (window.pywebview?.api) ||
                (typeof pywebview !== "undefined" && pywebview?.api) ||
                null;
            if (api) return api;
            await new Promise((r) => setTimeout(r, interval));
        }
        return null;
    }
}

// CSS da animação de shake

const style = document.createElement("style");
style.textContent = `
@keyframes shake {
    0%, 100% { transform: translateX(0); }
    10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
    20%, 40%, 60%, 80%      { transform: translateX(5px); }
}`;
document.head.appendChild(style);

// Arranque

document.addEventListener("DOMContentLoaded", () => {
    new RegistoController();
});