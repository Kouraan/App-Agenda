class LoginController {
    constructor() {
        this.usernameField = document.getElementById('usernameField');
        this.passwordField = document.getElementById('passwordField');
        this.errorLabel = document.getElementById('errorLabel');
        this.loginForm = document.getElementById('loginForm');
        this.entrarBtn = document.getElementById('entrarBtn');
        
        this.init();
    }
    
    init() {
        // Event listeners
        this.loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        
        // Enter key support (igual ao JavaFX)
        this.usernameField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleLogin(e);
        });
        
        this.passwordField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleLogin(e);
        });
        
        // Limpar erros quando o utilizador começa a escrever
        this.usernameField.addEventListener('input', () => this.clearError());
        this.passwordField.addEventListener('input', () => this.clearError());
        
        // Focus management (como no JavaFX)
        this.usernameField.addEventListener('focus', () => this.clearFieldError(this.usernameField));
        this.passwordField.addEventListener('focus', () => this.clearFieldError(this.passwordField));
        
        // Click fora remove focus (como no JavaFX)
        document.addEventListener('click', (e) => {
            if (!this.loginForm.contains(e.target)) {
                document.activeElement.blur();
            }
        });
        
        // Focus inicial
        setTimeout(() => {
            this.usernameField.focus();
        }, 100);
    }
    
    async handleLogin(event) {
        if (event) event.preventDefault();
        
        const username = this.usernameField.value.trim();
        const password = this.passwordField.value;
        
        // Limpar erros anteriores
        this.clearError();
        
        // Desabilitar botão durante processamento
        this.entrarBtn.disabled = true;
        this.entrarBtn.textContent = 'A processar...';
        
        try {
            // Chamar método Python através do pywebview
            const result = await pywebview.api.fazer_login(username, password);
            
            if (result.success) {
                // Login bem-sucedido - ir para página principal
                await pywebview.api.mostrar_pagina_principal();
            } else {
                // Mostrar erro (como no JavaFX)
                this.showError();
                this.setFieldError(this.usernameField, true);
                this.setFieldError(this.passwordField, true);
            }
        } catch (error) {
            console.error('Erro no login:', error);
            this.showError();
            this.setFieldError(this.usernameField, true);
            this.setFieldError(this.passwordField, true);
        } finally {
            // Reabilitar botão
            this.entrarBtn.disabled = false;
            this.entrarBtn.textContent = 'Entrar';
        }
    }
    
    showError() {
        this.errorLabel.classList.add('visible');
        
        // Animação de shake
        this.errorLabel.style.animation = 'shake 0.5s ease-in-out';
        setTimeout(() => {
            this.errorLabel.style.animation = '';
        }, 500);
    }
    
    clearError() {
        this.errorLabel.classList.remove('visible');
        this.clearFieldError(this.usernameField);
        this.clearFieldError(this.passwordField);
    }
    
    setFieldError(field, error) {
        if (error) {
            field.classList.add('error');
        } else {
            field.classList.remove('error');
        }
    }
    
    clearFieldError(field) {
        field.classList.remove('error');
    }
}

// CSS para animação de shake
const shakeCSS = `
@keyframes shake {
    0%, 100% { transform: translateX(0); }
    10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
    20%, 40%, 60%, 80% { transform: translateX(5px); }
}
`;

const style = document.createElement('style');
style.textContent = shakeCSS;
document.head.appendChild(style);

// Inicializar quando a página carregar
document.addEventListener('DOMContentLoaded', () => {
    new LoginController();
});