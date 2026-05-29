import webview
import os
from datetime import datetime
from typing import Dict, List, Optional
from ..models.Utilizador import Utilizador
from ..models.Cliente import Cliente, TipoCliente
from ..models.Marcacao import Marcacao
from ..models.Pendente import Pendente
from ..utils.Persistencia import Persistencia
from ..utils.Logger import Logger
from ..repository.ClienteRepo import ClientesRepository
from ..utils.MarcacoesSemanais import MarcacoesSemanais

class AppController:
    def __init__(self):
        self.utilizador: Optional[Utilizador] = None
        self.clientes_map: Dict[str, Cliente] = {}
        self.marcacoes_map: Dict[datetime, Marcacao] = {}
        self.pendentes: List[Pendente] = []
        
    def initialize(self):
        """Carrega dados iniciais"""
        self.utilizador = Persistencia.ler_utilizador()
        self.clientes_map = Persistencia.ler_clientes()
        self.marcacoes_map = Persistencia.ler_marcacoes()
        self.pendentes = Persistencia.ler_pendentes()
        
        # DEBUG: informar o que foi carregado
        try:
            nome = self.utilizador.get_nome() if self.utilizador else None
        except Exception:
            nome = None
            
        # Garantir marcações semanais para clientes SEMANAIS (gera até 6 meses à frente)
        try:
            from datetime import date
            for cliente in list(self.clientes_map.values()):
                try:
                    novas = MarcacoesSemanais.gerar_marcacoes_semanais(cliente, self.marcacoes_map, date.today(), meses_a_frente=6)
                    for m in novas:
                        try:
                            Logger.log_marcacao_criada(m)
                        except Exception:
                            pass
                except Exception:
                    continue
            # persistir se gerou algo novo
            Persistencia.guardar_marcacoes(self.marcacoes_map)
        except Exception:
            pass
        
    # Getters para JavaScript
    def get_clientes_map(self):
        """Retorna mapa de clientes para JavaScript"""
        return {nome: self._cliente_to_dict(cliente) 
                for nome, cliente in self.clientes_map.items()}
    
    def get_marcacoes_map(self):
        """Retorna marcações para JavaScript"""
        return {dt.isoformat(): self._marcacao_to_dict(marcacao)
                for dt, marcacao in self.marcacoes_map.items()}
    
    def get_pendentes(self):
        """Retorna pendentes para JavaScript"""
        return [self._pendente_to_dict(p) for p in self.pendentes]
    
    def _get_html_path(self, filename):
        """Retorna caminho absoluto para ficheiro HTML"""
        # Ir até à raiz do projeto (onde está main.py)
        current_dir = os.path.dirname(os.path.abspath(__file__))  # backend/controllers/
        backend_dir = os.path.dirname(current_dir)                # backend/
        project_root = os.path.dirname(backend_dir)               # AppAgenda/
        
        html_path = os.path.join(project_root, 'ui', 'html', filename)
        
        # Debug: verificar se o ficheiro existe
        if not os.path.exists(html_path):
            print(f"Aviso: Ficheiro HTML não encontrado em {html_path}")
        
        return f"file://{html_path}"
        
    
    # Navegação entre páginas
    def mostrar_login(self):
        """Navegar para página de login"""
        try:
            webview.windows[0].load_url(self._get_html_path('login.html'))
            return {"success": True}
        except Exception as e:
            print(f"Erro ao navegar para login: {e}")
            return {"success": False, "error": str(e)}
    
    def mostrar_registo(self):
        """Navegar para página de registo"""
        try:
            webview.windows[0].load_url(self._get_html_path('registo.html'))
            return {"success": True}
        except Exception as e:
            print(f"Erro ao navegar para registo: {e}")
            return {"success": False, "error": str(e)}
    
    def mostrar_pagina_principal(self):
        """Navegar para página principal"""
        try:
            webview.windows[0].load_url(self._get_html_path('pagina_principal.html'))
            return {"success": True}
        except Exception as e:
            print(f"Erro ao navegar para página principal: {e}")
            return {"success": False, "error": str(e)}
        
    def mostrar_detalhe_cliente(self, nome: str):
        """Abre a página de detalhe do cliente (passa nome via query string)"""
        try:
            if not nome:
                return {"success": False, "error": "Nome não fornecido"}
            from urllib.parse import quote
            url = self._get_html_path('detalhe_cliente.html') + f'?nome={quote(nome)}'
            webview.windows[0].load_url(url)
            return {"success": True}
        except Exception as e:
            print(f"Erro ao abrir detalhe cliente: {e}")
            return {"success": False, "error": str(e)}
            
    def get_cliente(self, nome: str):
        """Retorna dados do cliente por nome"""
        try:
            if not nome or nome not in self.clientes_map:
                return {"success": False, "error": "Cliente não encontrado"}
            cliente = self.clientes_map[nome]
            return {"success": True, "cliente": self._cliente_to_dict(cliente)}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def apagar_cliente(self, nome: str):
        """Apaga cliente e marcações futuras associadas"""
        try:
            if not nome or nome not in self.clientes_map:
                return {"success": False, "error": "Cliente não encontrado"}

            cliente = self.clientes_map[nome]
            # Remover marcações futuras desse cliente
            from datetime import date
            hoje = date.today()
            to_remove = [dt for dt, m in self.marcacoes_map.items()
                         if m.get_cliente().get_nome() == cliente.get_nome() and not dt.date() < hoje]
            for dt in to_remove:
                del self.marcacoes_map[dt]

            # Persistir mudanças
            Persistencia.guardar_marcacoes(self.marcacoes_map)
            del self.clientes_map[nome]
            Persistencia.guardar_clientes(self.clientes_map)
            try:
                Logger.log_cliente_apagado(cliente.get_nome())
            except Exception:
                pass
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def alterar_cliente(self, cliente_dict):
        """Altera um cliente (recebe dicionário vindo do JS).
           cliente_dict deve conter nomeOriginal, nome, numeroTelefone, tipoCliente, diaSemana, horaCorte, faltas, rapido
        """
        try:
            if not cliente_dict or "nomeOriginal" not in cliente_dict:
                return {"success": False, "error": "Dados inválidos"}

            nome_original = cliente_dict.get("nomeOriginal")
            if nome_original not in self.clientes_map:
                return {"success": False, "error": "Cliente original não encontrado"}

            # Converter tipo
            tipo_raw = cliente_dict.get("tipoCliente", "NORMAL")
            tipo = TipoCliente.NORMAL
            try:
                if isinstance(tipo_raw, str):
                    try:
                        tipo = TipoCliente[tipo_raw.upper()]
                    except KeyError:
                        try:
                            tipo = TipoCliente(tipo_raw)
                        except Exception:
                            tipo = TipoCliente.NORMAL
            except Exception:
                tipo = TipoCliente.NORMAL

            # criar/atualizar cliente
            novo_nome = cliente_dict.get("nome", "").strip()
            numero = cliente_dict.get("numeroTelefone", "").strip()
            dia_semana = cliente_dict.get("diaSemana")
            hora_corte = cliente_dict.get("horaCorte")
            faltas = int(cliente_dict.get("faltas", 0))
            rapido = bool(cliente_dict.get("rapido", False))

            # Validações usando Validation
            from ..utils.Validation import Validation

            # Se o nome mudou para outro que já existe -> erro
            if novo_nome != nome_original and any(c.get_nome().lower() == novo_nome.lower() for c in self.clientes_map.values()):
                return {"success": False, "error": "Já existe um cliente com esse nome."}
            if any(c.get_numero_telefone() == numero and c.get_nome() != nome_original for c in self.clientes_map.values()):
                return {"success": False, "error": "Já existe um cliente com esse número de telefone."}

            # Montar objeto Cliente
            if tipo == TipoCliente.SEMANAL:
                novo = Cliente(novo_nome, numero, tipo, dia_semana, hora_corte, rapido)
            else:
                novo = Cliente(novo_nome, numero, tipo)
            novo.set_faltas(faltas)

            if not Validation.cliente_valido(novo, {k:v for k,v in self.clientes_map.items() if k != nome_original}):
                return {"success": False, "error": "Dados do cliente inválidos ou duplicados."}

            # Substituir no mapa (mover chave se nome alterado)
            if novo_nome != nome_original:
                del self.clientes_map[nome_original]
            self.clientes_map[novo.get_nome()] = novo
            # Persistir
            Persistencia.guardar_clientes(self.clientes_map)
            # Logs
            try:
                Logger.log_nome_alterado(nome_original, novo.get_nome()) if nome_original != novo.get_nome() else None
            except Exception:
                pass
            
            # Remover marcações futuras do cliente antigo e regenerar semanais (se aplicável)
            try:
                from datetime import date
                hoje = date.today()
                # remover do mapa quaisquer marcações futuras deste cliente
                to_remove = [dt for dt, m in self.marcacoes_map.items()
                             if m.get_cliente().get_nome() == nome_original and not dt.date() < hoje]
                for dt in to_remove:
                    del self.marcacoes_map[dt]
                # se novo for semanal, gerar e persistir
                if novo.get_tipo_cliente() == TipoCliente.SEMANAL:
                    novas = MarcacoesSemanais.gerar_marcacoes_semanais(novo, self.marcacoes_map, hoje, meses_a_frente=6)
                    for m in novas:
                        try:
                            Logger.log_marcacao_criada(m)
                        except Exception:
                            pass
                # persistir marcacoes atualizadas
                Persistencia.guardar_marcacoes(self.marcacoes_map)
            except Exception:
                pass
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    # Métodos de autenticação (para JavaScript)
    def registar_utilizador(self, nome: str, password: str):
        """Regista novo utilizador"""
        from ..utils.Validation import Validation
        
        if not Validation.nome_valido(nome):
            return {"success": False, "error": "Nome inválido."}
        
        if not Validation.password_valida(password):
            return {"success": False, "error": "Password tem de ter mais de 5 caracteres."}
        
        novo_utilizador = Utilizador(nome, password)
        
        if Persistencia.guardar_utilizador(novo_utilizador):
            self.utilizador = novo_utilizador
            Logger.log_registo(nome)
            return {"success": True, "message": "Utilizador registado com sucesso!"}
        else:
            return {"success": False, "error": "Erro ao guardar utilizador."}
    
    def fazer_login(self, nome: str, password: str):
        """Faz login do utilizador"""
        from ..repository.UtilizadorRepo import UtilizadorRepository
        
        if UtilizadorRepository.autenticar(nome, password, self.utilizador):
            Logger.log_login(nome)
            return {"success": True, "message": "Login efetuado com sucesso!"}
        else:
            return {"success": False, "error": "Credenciais inválidas."}
    
    def get_utilizador_info(self):
        """Retorna informações do utilizador atual"""
        # garantir que temos o utilizador carregado (tenta ler da persistência se necessário)
        if not self.utilizador:
            try:
                self.utilizador = Persistencia.ler_utilizador()
            except Exception:
                self.utilizador = None

        if self.utilizador:
            return {
                "nome": self.utilizador.get_nome(),
                "authenticated": True
            }
        return {"authenticated": False}
    
    def fazer_login(self, nome: str, password: str):
        """Faz login do utilizador"""
        from ..repository.UtilizadorRepo import UtilizadorRepository
        
        if UtilizadorRepository.autenticar(nome, password, self.utilizador):
            Logger.log_login(nome)
            return {"success": True, "message": "Login efetuado com sucesso!"}
        else:
            return {"success": False, "error": "Credenciais inválidas."}
    
    def fazer_logout(self):
        """Faz logout do utilizador"""
        if self.utilizador:
            Logger.log_logout(self.utilizador.get_nome())
            return {"success": True, "message": "Logout efetuado com sucesso!"}
        return {"success": False, "error": "Nenhum utilizador logado."}
    
    def get_current_time(self):
        """Retorna hora atual formatada"""
        from datetime import datetime
        return datetime.now().strftime("%H:%M:%S")
    
    def get_marcacoes_periodo(self, data_inicio: str, data_fim: str):
        """Retorna marcações de um período"""
        from datetime import datetime
        
        inicio = datetime.fromisoformat(data_inicio)
        fim = datetime.fromisoformat(data_fim)
        
        marcacoes_periodo = {}
        for data_hora, marcacao in self.marcacoes_map.items():
            if inicio <= data_hora <= fim:
                marcacoes_periodo[data_hora.isoformat()] = self._marcacao_to_dict(marcacao)
        
        return marcacoes_periodo
    
    def guardar_anotacoes(self, texto: str):
        """Guarda anotações do utilizador"""
        try:
            from ..utils.Persistencia import Persistencia
            Persistencia.guardar_anotacoes(texto)
            return {"success": True, "message": "Anotações guardadas."}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    def ler_anotacoes(self):
        """Lê anotações do utilizador"""
        try:
            from ..utils.Persistencia import Persistencia
            texto = Persistencia.ler_anotacoes()
            return {"success": True, "texto": texto}
        except Exception as e:
            return {"success": False, "error": str(e), "texto": ""}
    
    def criar_marcacao(self, cliente_nome: str, data_hora: str, duracao: int, observacoes: str = ""):
        """Cria nova marcação"""
        from datetime import datetime
        
        if cliente_nome not in self.clientes_map:
            return {"success": False, "error": "Cliente não encontrado."}
        
        try:
            data_hora_obj = datetime.fromisoformat(data_hora)
            cliente = self.clientes_map[cliente_nome]
            
            nova_marcacao = Marcacao(cliente, data_hora_obj, duracao, observacoes)
            self.marcacoes_map[data_hora_obj] = nova_marcacao
            
            Persistencia.guardar_marcacoes(self.marcacoes_map)
            
            return {"success": True, "message": "Marcação criada com sucesso."}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    def adicionar_cliente(self, cliente_dict):
        """
        Recebe um dicionário com os campos do cliente vindos do JS, adiciona ao mapa
        e persiste. Retorna dict com success True/False.
        """
        try:
            from ..utils.Validation import Validation

            nome = cliente_dict.get("nome", "").strip()
            numero = cliente_dict.get("numeroTelefone", "").strip()
            tipo_raw = cliente_dict.get("tipoCliente", "NORMAL")
            dia_semana = cliente_dict.get("diaSemana")
            hora_corte = cliente_dict.get("horaCorte")
            rapido = bool(cliente_dict.get("rapido", False))

            if not nome:
                return {"success": False, "error": "Campo 'nome' obrigatório."}

            # Converter tipo para Enum TipoCliente de forma robusta
            tipo = TipoCliente.NORMAL
            try:
                if isinstance(tipo_raw, TipoCliente):
                    tipo = tipo_raw
                elif isinstance(tipo_raw, str):
                    # tenta pelo nome do Enum (NORMAL, SEMANAL, DESCONHECIDO)
                    try:
                        tipo = TipoCliente[tipo_raw.upper()]
                    except KeyError:
                        # tenta por value
                        try:
                            tipo = TipoCliente(tipo_raw)
                        except Exception:
                            tipo = TipoCliente.NORMAL
            except Exception:
                tipo = TipoCliente.NORMAL

            # Criar objeto Cliente com os campos apropriados
            if tipo == TipoCliente.SEMANAL:
                novo = Cliente(nome, numero, tipo, dia_semana, hora_corte, rapido)
            else:
                novo = Cliente(nome, numero, tipo)

            # Validação
            if not Validation.cliente_valido(novo, self.clientes_map):
                return {"success": False, "error": "Dados do cliente inválidos ou duplicados."}

            # Armazenar no mapa (usar nome como key)
            chave = novo.get_nome()
            self.clientes_map[chave] = novo

            # Persistir (usa Persistencia.guardar_clientes que existe)
            try:
                saved = False
                if hasattr(Persistencia, "guardar_clientes"):
                    saved = Persistencia.guardar_clientes(self.clientes_map)
                elif hasattr(Persistencia, "guardarClientes"):
                    saved = Persistencia.guardarClientes(self.clientes_map)
                else:
                    saved = False

                if not saved:
                    print("Aviso: Persistencia.guardar_clientes retornou False.")
                    return {"success": False, "error": "Erro ao guardar cliente."}
                else:
                    try:
                        Logger.log_cliente_criado(novo.get_nome())
                    except Exception:
                        pass
                    
                    # Se for cliente SEMANAL, gerar marcações semanais e persistir marcacoes
                    try:
                        from datetime import date
                        if novo.get_tipo_cliente() == TipoCliente.SEMANAL:
                            novas = MarcacoesSemanais.gerar_marcacoes_semanais(novo, self.marcacoes_map, date.today(), meses_a_frente=6)
                            for m in novas:
                                try:
                                    Logger.log_marcacao_criada(m)
                                except Exception:
                                    pass
                            Persistencia.guardar_marcacoes(self.marcacoes_map)
                    except Exception:
                        pass

            except Exception as e:
                print(f"Aviso: erro ao persistir clientes: {e}")
                return {"success": False, "error": "Erro ao guardar cliente."}

            return {"success": True, "message": "Cliente adicionado com sucesso."}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    # Métodos auxiliares para conversão
    def _cliente_to_dict(self, cliente: Cliente):
        """Converte cliente para dicionário"""
        return {
            "nome": cliente.get_nome(),
            "numeroTelefone": cliente.get_numero_telefone(),
            "tipoCliente": cliente.get_tipo_cliente().value,
            "faltas": cliente.get_faltas(),
            "diaSemana": cliente.get_dia_semana(),
            "horaCorte": cliente.get_hora_corte(),
            "rapido": cliente.is_rapido(),
            "temporario": cliente.is_temporario()
        }
    
    def _marcacao_to_dict(self, marcacao: Marcacao):
        """Converte marcação para dicionário"""
        return {
            "dataHora": marcacao.get_data_hora().isoformat(),
            "cliente": self._cliente_to_dict(marcacao.get_cliente()),
            "duracao": marcacao.get_duracao(),
            "observacoes": marcacao.get_observacoes(),
            "falta": marcacao.is_falta()
        }
    
    def _pendente_to_dict(self, pendente: Pendente):
        """Converte pendente para dicionário"""
        return {
            "nome": pendente.get_nome(),
            "numeroTelefone": pendente.get_numero_telefone()
        }