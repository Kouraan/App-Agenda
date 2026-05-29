class RegistoController {
    constructor() {
        this.nomeField = document.getElementById('nomeField');
        this.passwordField = document.getElementById('passwordField');
        this.errorLabel = document.getElementById('errorLabel');
        this.registoForm = document.getElementById('registoForm');
        this.registoBtn = document.getElementById('registoBtn');
        this.loginLinkBtn = document.getElementById('loginLinkBtn');

        this.init();
    }
    
    init() {
        // Event listeners
        this.registoForm.addEventListener('submit', (e) => this.handleRegisto(e));
        
        // Link para login
        this.loginLinkBtn.addEventListener('click', () => this.goToLogin());

        // Enter key support (igual ao JavaFX)
        this.nomeField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleRegisto(e);
        });
        
        this.passwordField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleRegisto(e);
        });
        
        // Limpar erros quando o utilizador começa a escrever
        this.nomeField.addEventListener('input', () => this.clearError());
        this.passwordField.addEventListener('input', () => this.clearError());
        
        // Focus inicial (como no JavaFX com requestFocus)
        setTimeout(() => {
            this.nomeField.focus();
        }, 100);
        
        // Simular comportamento de focus do JavaFX
        document.addEventListener('click', (e) => {
            if (!this.registoForm.contains(e.target)) {
                // Se clicou fora do formulário, remover focus
                document.activeElement.blur();
            }
        });
    }
    
    async handleRegisto(event) {
        if (event) event.preventDefault();
        
        const nome = this.nomeField.value.trim();
        const password = this.passwordField.value;
        
        // Limpar mensagem de erro anterior
        this.clearError();
        
        // Desabilitar botão durante o processamento
        this.registoBtn.disabled = true;
        this.registoBtn.textContent = 'A processar...';
        
        try {
            // Chamar método Python através do pywebview
            const result = await pywebview.api.registar_utilizador(nome, password);
            
            if (result.success) {
                // Sucesso - ir para login (como no JavaFX)
                await pywebview.api.mostrar_login();
            } else {
                // Mostrar erro (igual ao JavaFX)
                this.showError(result.error);
            }
        } catch (error) {
            console.error('Erro no registo:', error);
            this.showError('Erro inesperado. Tente novamente.');
        } finally {
            // Reabilitar botão
            this.registoBtn.disabled = false;
            this.registoBtn.textContent = 'Registar';
        }
    }

    async goToLogin() {
        try {
            await pywebview.api.mostrar_login();
        } catch (error) {
            console.error('Erro ao navegar para login:', error);
        }
    }
    
    showError(message) {
        this.errorLabel.textContent = message;
        this.errorLabel.classList.add('visible');
        
        // Animação de shake (feedback visual)
        this.errorLabel.style.animation = 'shake 0.5s ease-in-out';
        setTimeout(() => {
            this.errorLabel.style.animation = '';
        }, 500);
    }
    
    clearError() {
        this.errorLabel.classList.remove('visible');
        this.errorLabel.textContent = '';
    }
}

// CSS para animação de shake (adicionar ao head dinamicamente)
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
    new RegistoController();
});