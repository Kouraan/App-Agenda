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
            
            width = screen_width
            height = screen_height
            
            # Mínimos e máximos
            width = max(1200, min(width, 1920))
            height = max(700, min(height, 1080))
            
            return width, height
            
        except Exception as e:
            print(f"Erro ao obter dimensões do ecrã: {e}")
            # Fallback para dimensões padrão
            return 1200, 700 
    
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
        
        # caminho do ícone
        icon_path = os.path.join(base_dir, "ui", "images", "icon.ico")
        
        # --- tentar definir o ícone apenas no Windows ---
        if os.name == "nt":
            try:
                import ctypes

                def _set_windows_icon(window_title, ico_path, retries=10, delay=0.25):
                    WM_SETICON = 0x0080
                    ICON_SMALL = 0
                    ICON_BIG = 1
                    LR_LOADFROMFILE = 0x00000010
                    IMAGE_ICON = 1

                    for _ in range(retries):
                        # encontra a janela pelo título
                        hwnd = ctypes.windll.user32.FindWindowW(None, window_title)
                        if hwnd:
                            # carrega o .ico a partir de ficheiro
                            hicon = ctypes.windll.user32.LoadImageW(None, ico_path, IMAGE_ICON, 0, 0, LR_LOADFROMFILE)
                            if hicon:
                                # definir tanto o ícone grande como o pequeno
                                ctypes.windll.user32.SendMessageW(hwnd, WM_SETICON, ICON_BIG, hicon)
                                ctypes.windll.user32.SendMessageW(hwnd, WM_SETICON, ICON_SMALL, hicon)
                            return True
                        time.sleep(delay)
                    return False

                # arrancar a tarefa em background (não bloqueia o webview.start)
                threading.Thread(target=_set_windows_icon, args=('App-Agenda', icon_path), daemon=True).start()
            except Exception as e:
                print(f"Não foi possível definir ícone Windows: {e}")
        else:
            # Em Linux/macOS não tentamos usar windll (não aplicável)
            pass
        
        # Log de aplicação iniciada
        from backend.utils import Logger
        Logger.log_app_iniciada()
        
        # Callback para quando a janela fechar
        def on_closing():
            Logger.log_app_terminada()
        
        if window is not None:
            events = getattr(window, "events", None)
            if events is not None:
                events.closing += on_closing
        
        # Iniciar o webview
        webview.start(debug=False)

if __name__ == '__main__':
    app = MainApp()
    app.start_app()