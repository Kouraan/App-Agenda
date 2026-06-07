import webview
import os
from datetime import datetime, date
from typing import Dict, List, Optional

from ..models.Utilizador import Utilizador
from ..models.Cliente import Cliente, TipoCliente
from ..models.Marcacao import Marcacao
from ..models.Pendente import Pendente
from ..utils import Persistencia
from ..utils import Logger
from ..utils import Database
from ..utils.Validation import Validation
from ..utils.MarcacoesSemanais import MarcacoesSemanais


class AppController:
    def __init__(self):
        self.utilizador: Optional[Utilizador] = None
        self.clientes_map: Dict[str, Cliente] = {}
        self.marcacoes_map: Dict[datetime, Marcacao] = {}
        self.pendentes: List[Pendente] = []

    def initialize(self):
        """Carrega dados iniciais a partir da base de dados SQLite."""

        # Garante que as tabelas existem
        Database.inicializar_bd()

        # Carregar dados da BD
        self.utilizador    = Persistencia.ler_utilizador()
        self.clientes_map  = Persistencia.ler_clientes()
        self.marcacoes_map = Persistencia.ler_marcacoes()
        self.pendentes     = Persistencia.ler_pendentes()

        # Gerar marcações semanais em falta (até 6 meses à frente)
        try:
            for cliente in list(self.clientes_map.values()):
                try:
                    novas = MarcacoesSemanais.gerar_marcacoes_semanais(
                        cliente, self.marcacoes_map, date.today(), meses_a_frente=6
                    )
                    for m in novas:
                        try:
                            Logger.log_marcacao_criada(m)
                        except Exception:
                            pass
                except Exception:
                    continue
            Persistencia.guardar_marcacoes(self.marcacoes_map)
        except Exception:
            pass

    # Navegar entre paginas

    def _get_html_path(self, filename: str) -> str:
        """Devolve o URL file:// para um ficheiro HTML da pasta ui/html/."""
        current_dir  = os.path.dirname(os.path.abspath(__file__))  # controllers/
        backend_dir  = os.path.dirname(current_dir)                # backend/
        project_root = os.path.dirname(backend_dir)                # Python/
        html_path    = os.path.join(project_root, "ui", "html", filename)
        if not os.path.exists(html_path):
            print(f"[AppController] Aviso: ficheiro HTML não encontrado em {html_path}")
        return f"file://{html_path}"

    def mostrar_login(self):
        try:
            webview.windows[0].load_url(self._get_html_path("login.html"))
            return {"success": True}
        except Exception as e:
            print(f"[AppController] mostrar_login: {e}")
            return {"success": False, "error": str(e)}

    def mostrar_registo(self):
        try:
            webview.windows[0].load_url(self._get_html_path("registo.html"))
            return {"success": True}
        except Exception as e:
            print(f"[AppController] mostrar_registo: {e}")
            return {"success": False, "error": str(e)}

    def mostrar_pagina_principal(self):
        try:
            webview.windows[0].load_url(self._get_html_path("pagina_principal.html"))
            return {"success": True}
        except Exception as e:
            print(f"[AppController] mostrar_pagina_principal: {e}")
            return {"success": False, "error": str(e)}

    # Registo e Login

    def registar_utilizador(self, nome: str, password: str):
        """Regista um novo utilizador (usado na página de registo)."""
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
        """Autentica o utilizador. Chamado pelo JavaScript da página de login."""
        if (self.utilizador is not None and
                self.utilizador.get_nome() == nome and
                self.utilizador.get_password() == password):
            Logger.log_login(nome)
            return {"success": True, "message": "Login efetuado com sucesso!"}
        else:
            return {"success": False, "error": "Credenciais inválidas."}

    def fazer_logout(self):
        """Faz logout do utilizador atual."""
        if self.utilizador:
            Logger.log_logout(self.utilizador.get_nome())
            return {"success": True, "message": "Logout efetuado com sucesso!"}
        return {"success": False, "error": "Nenhum utilizador logado."}

    def get_utilizador_info(self):
        """Retorna informação do utilizador para o JavaScript."""
        # Se por alguma razão não está em memória, tenta recarregar da BD
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

    # Clientes

    def get_clientes_map(self):
        """Devolve o mapa de clientes serializado para JavaScript."""
        return {
            nome: self._cliente_to_dict(cliente)
            for nome, cliente in self.clientes_map.items()
        }

    def get_cliente(self, nome: str):
        """Devolve dados de um cliente específico."""
        if not nome or nome not in self.clientes_map:
            return {"success": False, "error": "Cliente não encontrado"}
        return {"success": True, "cliente": self._cliente_to_dict(self.clientes_map[nome])}

    def adicionar_cliente(self, cliente_dict: dict):
        """Adiciona um novo cliente recebido do JavaScript."""
        try:
            nome     = cliente_dict.get("nome", "").strip()
            numero   = cliente_dict.get("numeroTelefone", "").strip()
            tipo_raw = cliente_dict.get("tipoCliente", "NORMAL")
            dia      = cliente_dict.get("diaSemana")
            hora     = cliente_dict.get("horaCorte")
            rapido   = bool(cliente_dict.get("rapido", False))

            if not nome:
                return {"success": False, "error": "Campo 'nome' obrigatório."}

            tipo = self._converter_tipo(tipo_raw)

            if tipo == TipoCliente.SEMANAL:
                novo = Cliente(nome, numero, tipo, dia, hora, rapido)
            else:
                novo = Cliente(nome, numero, tipo)

            if not Validation.cliente_valido(novo, self.clientes_map):
                return {"success": False, "error": "Dados do cliente inválidos ou duplicados."}

            self.clientes_map[novo.get_nome()] = novo

            if not Persistencia.guardar_clientes(self.clientes_map):
                # reverter mapa em memória se a BD falhou
                del self.clientes_map[novo.get_nome()]
                return {"success": False, "error": "Erro ao guardar cliente."}

            Logger.log_cliente_criado(novo.get_nome())

            # Gerar marcações semanais se aplicável
            if novo.get_tipo_cliente() == TipoCliente.SEMANAL:
                self._gerar_e_guardar_semanais(novo)

            return {"success": True, "message": "Cliente adicionado com sucesso."}

        except Exception as e:
            print(f"[AppController] adicionar_cliente: {e}")
            return {"success": False, "error": str(e)}

    def alterar_cliente(self, cliente_dict: dict):
        """Altera um cliente existente com os dados recebidos do JavaScript."""
        try:
            if not cliente_dict or "nomeOriginal" not in cliente_dict:
                return {"success": False, "error": "Dados inválidos"}

            nome_original = cliente_dict.get("nomeOriginal")
            if nome_original not in self.clientes_map:
                return {"success": False, "error": "Cliente original não encontrado"}

            novo_nome = cliente_dict.get("nome", "").strip()
            numero    = cliente_dict.get("numeroTelefone", "").strip()
            tipo_raw  = cliente_dict.get("tipoCliente", "NORMAL")
            dia       = cliente_dict.get("diaSemana")
            hora      = cliente_dict.get("horaCorte")
            faltas    = int(cliente_dict.get("faltas", 0))
            rapido    = bool(cliente_dict.get("rapido", False))

            tipo = self._converter_tipo(tipo_raw)

            # Verificar duplicados excluindo o próprio cliente
            outros = {k: v for k, v in self.clientes_map.items() if k != nome_original}
            if novo_nome != nome_original and any(
                c.get_nome().lower() == novo_nome.lower() for c in outros.values()
            ):
                return {"success": False, "error": "Já existe um cliente com esse nome."}
            if any(
                c.get_numero_telefone() == numero for c in outros.values()
            ):
                return {"success": False, "error": "Já existe um cliente com esse número de telefone."}

            if tipo == TipoCliente.SEMANAL:
                novo = Cliente(novo_nome, numero, tipo, dia, hora, rapido)
            else:
                novo = Cliente(novo_nome, numero, tipo)
            novo.set_faltas(faltas)

            if not Validation.cliente_valido(novo, outros):
                return {"success": False, "error": "Dados do cliente inválidos ou duplicados."}

            # Atualizar mapa em memória
            if novo_nome != nome_original:
                del self.clientes_map[nome_original]
            self.clientes_map[novo.get_nome()] = novo

            Persistencia.guardar_clientes(self.clientes_map)

            # Logs de alteração
            if nome_original != novo.get_nome():
                Logger.log_nome_alterado(nome_original, novo.get_nome())

            # Gerir marcações futuras
            self._reprocessar_semanais(nome_original, novo)

            return {"success": True}

        except Exception as e:
            print(f"[AppController] alterar_cliente: {e}")
            return {"success": False, "error": str(e)}

    def apagar_cliente(self, nome: str):
        """Apaga um cliente e as suas marcações futuras."""
        try:
            if not nome or nome not in self.clientes_map:
                return {"success": False, "error": "Cliente não encontrado"}

            cliente = self.clientes_map[nome]

            # Remover marcações futuras do mapa em memória
            hoje_dt = datetime.combine(date.today(), datetime.min.time())
            to_remove = []
            for dt, m in list(self.marcacoes_map.items()):
                c = m.get_cliente()
                if c is None:
                    continue
                if c.get_nome() == nome and dt >= hoje_dt:
                    to_remove.append(dt)
            for dt in to_remove:
                del self.marcacoes_map[dt]

            Persistencia.guardar_marcacoes(self.marcacoes_map)

            del self.clientes_map[nome]
            Persistencia.guardar_clientes(self.clientes_map)

            Logger.log_cliente_apagado(cliente.get_nome())

            return {"success": True}

        except Exception as e:
            print(f"[AppController] apagar_cliente: {e}")
            return {"success": False, "error": str(e)}

    # Marcações

    def get_marcacoes_map(self):
        """Devolve o mapa de marcações serializado para JavaScript."""
        return {
            dt.isoformat(): self._marcacao_to_dict(marcacao)
            for dt, marcacao in self.marcacoes_map.items()
        }

    def get_marcacoes_periodo(self, data_inicio: str, data_fim: str):
        """Devolve marcações dentro de um período (datas ISO)."""
        try:
            inicio = datetime.fromisoformat(data_inicio)
            fim    = datetime.fromisoformat(data_fim)
            return {
                dt.isoformat(): self._marcacao_to_dict(m)
                for dt, m in self.marcacoes_map.items()
                if inicio <= dt <= fim
            }
        except Exception as e:
            print(f"[AppController] get_marcacoes_periodo: {e}")
            return {}

    def criar_marcacao(self, cliente_nome: str, data_hora: str,
                       duracao: int, observacoes: str = ""):
        """Cria uma nova marcação (chamada pelo JavaScript)."""
        try:
            if cliente_nome not in self.clientes_map:
                return {"success": False, "error": "Cliente não encontrado."}

            data_hora_obj = datetime.fromisoformat(data_hora)
            cliente = self.clientes_map[cliente_nome]

            nova = Marcacao(data_hora_obj, cliente, duracao, observacoes)
            self.marcacoes_map[data_hora_obj] = nova

            Persistencia.guardar_marcacoes(self.marcacoes_map)
            Logger.log_marcacao_criada(nova)

            return {"success": True, "message": "Marcação criada com sucesso."}

        except Exception as e:
            print(f"[AppController] criar_marcacao: {e}")
            return {"success": False, "error": str(e)}

    # Anotação

    def guardar_anotacoes(self, texto: str):
        try:
            Persistencia.guardar_anotacoes(texto)
            return {"success": True, "message": "Anotações guardadas."}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def ler_anotacoes(self):
        try:
            texto = Persistencia.ler_anotacoes()
            return {"success": True, "texto": texto}
        except Exception as e:
            return {"success": False, "error": str(e), "texto": ""}

    # Pendentes

    def get_pendentes(self):
        """Devolve a lista de pendentes serializada para JavaScript."""
        return [self._pendente_to_dict(p) for p in self.pendentes]

    # Utils

    def get_current_time(self):
        return datetime.now().strftime("%H:%M:%S")

    @staticmethod
    def _converter_tipo(tipo_raw) -> TipoCliente:
        """Converte string ou enum para TipoCliente de forma robusta."""
        if isinstance(tipo_raw, TipoCliente):
            return tipo_raw
        if isinstance(tipo_raw, str):
            try:
                return TipoCliente[tipo_raw.upper()]
            except KeyError:
                try:
                    return TipoCliente(tipo_raw)
                except Exception:
                    pass
        return TipoCliente.NORMAL

    def _gerar_e_guardar_semanais(self, cliente: Cliente):
        """Gera marcações semanais para um cliente SEMANAL e persiste."""
        try:
            novas = MarcacoesSemanais.gerar_marcacoes_semanais(
                cliente, self.marcacoes_map, date.today(), meses_a_frente=6
            )
            for m in novas:
                try:
                    Logger.log_marcacao_criada(m)
                except Exception:
                    pass
            Persistencia.guardar_marcacoes(self.marcacoes_map)
        except Exception as e:
            print(f"[AppController] _gerar_e_guardar_semanais: {e}")

    def _reprocessar_semanais(self, nome_original: str, novo: Cliente):
        """
        Após alterar um cliente, remove as marcações futuras do nome antigo
        e, se o cliente continua/passa a ser SEMANAL, regenera-as.
        """
        try:
            hoje_dt = datetime.combine(date.today(), datetime.min.time())

            # Remover futuras do cliente original
            to_remove = []
            for dt, m in list(self.marcacoes_map.items()):
                c = m.get_cliente()
                if c is None:
                    continue
                if c.get_nome() == nome_original and dt >= hoje_dt:
                    to_remove.append(dt)
            for dt in to_remove:
                del self.marcacoes_map[dt]

            # Regenerar se for SEMANAL
            if novo.get_tipo_cliente() == TipoCliente.SEMANAL:
                self._gerar_e_guardar_semanais(novo)
            else:
                # Só persiste a remoção
                Persistencia.guardar_marcacoes(self.marcacoes_map)

        except Exception as e:
            print(f"[AppController] _reprocessar_semanais: {e}")

    def _cliente_to_dict(self, cliente: Cliente) -> dict:
        return {
            "nome":           cliente.get_nome(),
            "numeroTelefone": cliente.get_numero_telefone(),
            "tipoCliente":    cliente.get_tipo_cliente().value,
            "faltas":         cliente.get_faltas(),
            "diaSemana":      cliente.get_dia_semana(),
            "horaCorte":      cliente.get_hora_corte(),
            "rapido":         cliente.is_rapido(),
            "temporario":     cliente.is_temporario(),
        }

    def _marcacao_to_dict(self, marcacao: Marcacao) -> dict:
        cliente = marcacao.get_cliente()
        if cliente is None:
            cliente_dict = {
                "nome": "N/A",
                "numeroTelefone": "",
                "tipoCliente": TipoCliente.DESCONHECIDO.value,
                "faltas": 0,
                "diaSemana": None,
                "horaCorte": None,
                "rapido": False,
                "temporario": True,
            }
        else:
            cliente_dict = self._cliente_to_dict(cliente)
            
        data_hora = marcacao.get_data_hora()
        data_hora_iso = data_hora.isoformat() if data_hora is not None else "N/A"
        
        return {
            "dataHora":    data_hora_iso,
            "cliente":     cliente_dict,
            "duracao":     marcacao.get_duracao(),
            "observacoes": marcacao.get_observacoes(),
            "falta":       marcacao.is_falta(),
        }

    def _pendente_to_dict(self, pendente: Pendente) -> dict:
        return {
            "nome":           pendente.get_nome(),
            "numeroTelefone": pendente.get_numero_telefone(),
        }