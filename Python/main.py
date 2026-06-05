import webview
import threading
import time
import os
from backend.controllers.AppController import AppController

class MainApp:
    def __init__(self):
        self.app_controller = AppController()
        
    def get_screen_dimensions(self):
        """Obtém dimensões do ecrã"""
        try:
            # Tentar obter dimensões do ecrã
            import tkinter as tk
            root = tk.Tk()
            root.withdraw()  # Esconder janela tkinter
            screen_width = root.winfo_screenwidth()
            screen_height = root.winfo_screenheight()
            root.destroy()
            
            # Usar 90% da largura e 85% da altura para deixar espaço
            width = int(screen_width * 0.9)
            height = int(screen_height * 0.85)
            
            # Mínimos e máximos
            width = max(1200, min(width, 1920))
            height = max(700, min(height, 1080))
            
            return width, height
            
        except Exception as e:
            print(f"Erro ao obter dimensões do ecrã: {e}")
            # Fallback para dimensões padrão
            return 1366, 768  
    
    def start_app(self):
        """Inicia a aplicação"""
        # Inicializar dados da aplicação
        self.app_controller.initialize()
        
        # Determinar página inicial
        if self.app_controller.utilizador:
            initial_file = 'login.html'
        else:
            initial_file = 'registo.html'
        
        # Caminho absoluto para o ficheiro HTML
        base_dir = os.path.dirname(os.path.abspath(__file__))
        html_path = os.path.join(base_dir, 'ui', 'html', initial_file)
        
        # Verificar se o ficheiro existe
        if not os.path.exists(html_path):
            print(f"Erro: Ficheiro não encontrado em {html_path}")
            print(f"Diretório atual: {base_dir}")
            print(f"Conteúdo da pasta ui: {os.listdir(os.path.join(base_dir, 'ui')) if os.path.exists(os.path.join(base_dir, 'ui')) else 'Pasta ui não existe'}")
            return
        
        # Obter dimensões do ecrã
        width, height = self.get_screen_dimensions()
        
        # Criar janela do webview
        window = webview.create_window(
            title='App-Agenda',
            url=f'file://{html_path}',
            width=width,           # Dinâmico baseado no ecrã
            height=height,         # Dinâmico baseado no ecrã
            resizable=True,        # Permitir redimensionar
            fullscreen=False,      # NÃO fullscreen automático
            maximized=True,        # Maximizada por defeito
            js_api=self.app_controller  # Expor o controller para JavaScript
        )
        
        # Log de aplicação iniciada
        from backend.utils import Logger
        Logger.log_app_iniciada()
        
        # Callback para quando a janela fechar
        def on_closing():
            Logger.log_app_terminada()
        
        window.events.closing += on_closing
        
        # Iniciar o webview
        webview.start(debug=True)

if __name__ == '__main__':
    app = MainApp()
    app.start_app()