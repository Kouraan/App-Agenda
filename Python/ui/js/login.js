class LoginController {
    constructor() {
        this.usernameField = document.getElementById("usernameField");
        this.passwordField = document.getElementById("passwordField");
        this.errorLabel    = document.getElementById("errorLabel");
        this.loginForm     = document.getElementById("loginForm");
        this.entrarBtn     = document.getElementById("entrarBtn");

        this._bind();
        // Foco inicial
        setTimeout(() => this.usernameField.focus(), 100);
    }

    // Bindings

    _bind() {
        this.loginForm.addEventListener("submit", (e) => this._handleLogin(e));

        // Enter em qualquer campo submete
        [this.usernameField, this.passwordField].forEach(field => {
            field.addEventListener("keypress", (e) => {
                if (e.key === "Enter") this._handleLogin(e);
            });
            // Limpar erro ao começar a escrever
            field.addEventListener("input", () => this._clearError());
            // Limpar borda de erro ao focar
            field.addEventListener("focus", () => this._setFieldError(field, false));
        });

        // Clicar fora do formulário remove o foco (comportamento JavaFX)
        document.addEventListener("click", (e) => {
            if (!this.loginForm.contains(e.target)) {
                document.activeElement?.blur();
            }
        });
    }

    // Login

    async _handleLogin(e) {
        if (e) e.preventDefault();

        const username = this.usernameField.value.trim();
        const password = this.passwordField.value;

        this._clearError();
        this._setLoading(true);

        try {
            const api = await this._getApi();
            if (!api) throw new Error("API não disponível");

            const result = await api.fazer_login(username, password);

            if (result?.success) {
                window.location.href = "pagina_principal.html";
            } else {
                this._showError();
                this._setFieldError(this.usernameField, true);
                this._setFieldError(this.passwordField, true);
            }
        } catch (err) {
            console.error("[Login] Erro:", err);
            this._showError();
            this._setFieldError(this.usernameField, true);
            this._setFieldError(this.passwordField, true);
        } finally {
            this._setLoading(false);
        }
    }

    // Estado de loading

    _setLoading(loading) {
        this.entrarBtn.disabled     = loading;
        this.entrarBtn.textContent  = loading ? "A processar..." : "Entrar";
    }

    // Erros

    _showError() {
        this.errorLabel.classList.add("visible");
        this._animateShake(this.errorLabel);
    }

    _clearError() {
        this.errorLabel.classList.remove("visible");
        this._setFieldError(this.usernameField, false);
        this._setFieldError(this.passwordField, false);
    }

    _setFieldError(field, error) {
        if (error) {
            field.classList.add("error");
        } else {
            field.classList.remove("error");
        }
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
    new LoginController();
});