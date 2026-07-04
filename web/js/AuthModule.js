class AuthModule {
    constructor(onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        this.overlay = document.getElementById("authOverlay");
        this.form = document.getElementById("authForm");
        this.emailField = document.getElementById("authEmail");
        this.passwordField = document.getElementById("authPassword");
        this.errorLabel = document.getElementById("authError");
        this.entrarBtn = document.getElementById("authEntrarBtn");

        this.from.addEventListener("submit", (e) => this._handleLogin(e));
    }

    async _handleLogin(e) {
        e.preventDefault();
        this._clearError();
        this._setLoading(true);

        const email = this.emailField.value.trim();
        const password = this.passwordField.value;

        try {
            const { data, error } = await supabaseClient.auth.signInWithPassword({
                email, password,
            });
            if (error || !data.session) {
                this._showError("Credenciais inválidas.");
                return;
            }
            this.overlay.style.display = "none";
            this.onLoginSuccess();
        } catch (err) {
            console.error("[Auht] Erro:", err);
            this._showError("Erro ao autenticar. Verificara ligação à internet.");
        } finally {
            this._setLoading(false);
        }
    }

    _setLoading(loading) {
        this.entrarBtn.disabled = loading;
        this.entrarBtn.textContent = loading ? "A processar..." : "Entrar";
    }

    _showError(msg) {
        this.errorLabel.textContent = msg;
        this.errorLabel.classList.add("visible");
    }

    _clearError() {
        this.errorLabel.textContent = "";
        this.errorLabel.classList.remove("visible");
    }
}