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
        Database.inicializar_bd()

        self.utilizador    = Persistencia.ler_utilizador()
        self.clientes_map  = Persistencia.ler_clientes()
        self.marcacoes_map = Persistencia.ler_marcacoes()
        self.pendentes     = Persistencia.ler_pendentes()

        # Gerar marcações semanais em falta (respeitando slots já processados)
        try:
            slots_a_guardar = []
            for cliente in list(self.clientes_map.values()):
                if cliente.get_tipo_cliente() != TipoCliente.SEMANAL:
                    continue
                try:
                    slots_usados = set(
                        Database.ler_slots_semanais_usados(cliente.get_nome())
                    )
                    novas = MarcacoesSemanais.gerar_marcacoes_semanais(
                        cliente, self.marcacoes_map, date.today(),
                        meses_a_frente=6, slots_usados=slots_usados
                    )
                    for m in novas:
                        slots_a_guardar.append({
                            "cliente_nome": cliente.get_nome(),
                            "data_hora":    m.get_data_hora().isoformat()
                        })
                        try:
                            Logger.log_marcacao_criada(m)
                        except Exception:
                            pass
                except Exception as e:
                    print(f"[AppController] initialize semanais {cliente.get_nome()}: {e}")
                    continue

            if slots_a_guardar:
                Database.inserir_slots_semanais_bulk(slots_a_guardar)
            Persistencia.guardar_marcacoes(self.marcacoes_map)
        except Exception as e:
            print(f"[AppController] initialize semanais: {e}")

    # ── Navegação ──────────────────────────────────────────────────────────────

    def _get_html_path(self, filename: str) -> str:
        current_dir  = os.path.dirname(os.path.abspath(__file__))
        backend_dir  = os.path.dirname(current_dir)
        project_root = os.path.dirname(backend_dir)
        html_path    = os.path.join(project_root, "ui", "html", filename)
        return f"file://{html_path}"

    def mostrar_login(self):
        try:
            webview.windows[0].load_url(self._get_html_path("login.html"))
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def mostrar_registo(self):
        try:
            webview.windows[0].load_url(self._get_html_path("registo.html"))
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def mostrar_pagina_principal(self):
        try:
            webview.windows[0].load_url(self._get_html_path("pagina_principal.html"))
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    # ── Utilizador ─────────────────────────────────────────────────────────────

    def registar_utilizador(self, nome: str, password: str):
        if not Validation.nome_valido(nome):
            return {"success": False, "error": "Nome inválido."}
        if not Validation.password_valida(password):
            return {"success": False, "error": "Password tem de ter mais de 5 caracteres."}

        novo = Utilizador(nome, password)
        if Persistencia.guardar_utilizador(novo):
            self.utilizador = novo
            Logger.log_registo(nome)
            return {"success": True}
        return {"success": False, "error": "Erro ao guardar utilizador."}

    def fazer_login(self, nome: str, password: str):
        if (self.utilizador is not None and
                self.utilizador.get_nome() == nome and
                self.utilizador.get_password() == password):
            Logger.log_login(nome)
            return {"success": True}
        return {"success": False, "error": "Credenciais inválidas."}

    def fazer_logout(self):
        if self.utilizador:
            Logger.log_logout(self.utilizador.get_nome())
            return {"success": True}
        return {"success": False, "error": "Nenhum utilizador logado."}

    def get_utilizador_info(self):
        if not self.utilizador:
            try:
                self.utilizador = Persistencia.ler_utilizador()
            except Exception:
                self.utilizador = None
        if self.utilizador:
            return {"nome": self.utilizador.get_nome(), "authenticated": True}
        return {"authenticated": False}

    # ── Clientes ───────────────────────────────────────────────────────────────

    def get_clientes_map(self):
        return {nome: self._cliente_to_dict(c) for nome, c in self.clientes_map.items()}

    def get_cliente(self, nome: str):
        if not nome or nome not in self.clientes_map:
            return {"success": False, "error": "Cliente não encontrado"}
        return {"success": True, "cliente": self._cliente_to_dict(self.clientes_map[nome])}

    def adicionar_cliente(self, cliente_dict: dict):
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

            nome_normalizado = novo.get_nome().strip()
            # Procurar chave existente com mesmo nome ignorando case
            chave_existente = next(
                (k for k in self.clientes_map if k.lower() == nome_normalizado.lower()),
                None
            )
            if chave_existente and chave_existente != nome_normalizado:
                return {"success": False, "error": "Já existe um cliente com esse nome."}
            self.clientes_map[nome_normalizado] = novo

            if not Persistencia.guardar_clientes(self.clientes_map):
                del self.clientes_map[novo.get_nome()]
                return {"success": False, "error": "Erro ao guardar cliente."}

            Logger.log_cliente_criado(novo.get_nome())

            if novo.get_tipo_cliente() == TipoCliente.SEMANAL:
                self._gerar_e_guardar_semanais(novo)

            return {"success": True, "message": "Cliente adicionado com sucesso."}

        except Exception as e:
            print(f"[AppController] adicionar_cliente: {e}")
            return {"success": False, "error": str(e)}

    def alterar_cliente(self, cliente_dict: dict):
        try:
            if not cliente_dict or "nomeOriginal" not in cliente_dict:
                return {"success": False, "error": "Dados inválidos"}

            nome_original = cliente_dict.get("nomeOriginal")
            if nome_original not in self.clientes_map:
                return {"success": False, "error": "Cliente original não encontrado"}

            cliente_atual = self.clientes_map[nome_original]
            tipo_atual    = cliente_atual.get_tipo_cliente()

            novo_nome = cliente_dict.get("nome", "").strip()
            numero    = cliente_dict.get("numeroTelefone", "").strip()
            tipo_raw  = cliente_dict.get("tipoCliente", "NORMAL")
            dia       = cliente_dict.get("diaSemana")
            hora      = cliente_dict.get("horaCorte")
            faltas    = int(cliente_dict.get("faltas", 0))
            rapido    = bool(cliente_dict.get("rapido", False))

            tipo_novo = self._converter_tipo(tipo_raw)

            # Validar duplicados excluindo o próprio cliente
            outros = {k: v for k, v in self.clientes_map.items() if k != nome_original}
            if novo_nome.lower() != nome_original.lower() and any(
                c.get_nome().lower() == novo_nome.lower() for c in outros.values()
            ):
                return {"success": False, "error": "Já existe um cliente com esse nome."}
            if any(c.get_numero_telefone() == numero for c in outros.values()):
                return {"success": False, "error": "Já existe um cliente com esse número de telefone."}

            # Construir novo objeto cliente
            if tipo_novo == TipoCliente.SEMANAL:
                novo = Cliente(novo_nome, numero, tipo_novo, dia, hora, rapido)
            else:
                novo = Cliente(novo_nome, numero, tipo_novo)
            novo.set_faltas(faltas)

            if not Validation.cliente_valido(novo, outros):
                return {"success": False, "error": "Dados do cliente inválidos ou duplicados."}

        # ── Atualizar marcações existentes com o novo nome ──────────────────
        # Faz isto ANTES de alterar o clientes_map para não perder a referência
            if novo_nome != nome_original:
                for dt, m in self.marcacoes_map.items():
                    c = m.get_cliente()
                    if c and self._get_nome_safe(c) == nome_original:
                        c_atualizado = Cliente(
                            novo_nome,
                            numero,
                            c.get_tipo_cliente(),
                            c.get_dia_semana(),
                            c.get_hora_corte(),
                            c.is_rapido()
                        )
                        c_atualizado.set_faltas(c.get_faltas())
                        m.set_cliente(c_atualizado)
                Logger.log_nome_alterado(nome_original, novo_nome)

        # ── Atualizar mapa de clientes ──────────────────────────────────────
            if novo_nome != nome_original:
                del self.clientes_map[nome_original]
            self.clientes_map[novo_nome] = novo

        # ── Tratar marcações semanais consoante a mudança de tipo ───────────
            tipo_atual_val  = tipo_atual
            tipo_novo_val   = tipo_novo

            horario_semanal_mudou = (
                tipo_novo_val == TipoCliente.SEMANAL and
                tipo_atual_val == TipoCliente.SEMANAL and
                (cliente_atual.get_dia_semana() != dia or
                cliente_atual.get_hora_corte() != hora or
                cliente_atual.is_rapido() != rapido)
            )

            if tipo_atual_val == TipoCliente.SEMANAL and tipo_novo_val != TipoCliente.SEMANAL:
            # SEMANAL → NORMAL: apagar apenas marcações futuras semanais
                self._apagar_marcacoes_futuras_cliente(novo_nome)
                Database.apagar_slots_semanais_cliente(nome_original)
                if novo_nome != nome_original:
                    Database.apagar_slots_semanais_cliente(novo_nome)

            elif tipo_atual_val != TipoCliente.SEMANAL and tipo_novo_val == TipoCliente.SEMANAL:
            # NORMAL → SEMANAL: gerar novas marcações semanais
                self._gerar_e_guardar_semanais(novo)

            elif horario_semanal_mudou:
            # SEMANAL → SEMANAL com horário diferente: apagar futuras e regenerar
                self._apagar_marcacoes_futuras_cliente(novo_nome)
                Database.apagar_slots_semanais_cliente(nome_original)
                if novo_nome != nome_original:
                    Database.apagar_slots_semanais_cliente(novo_nome)
                self._gerar_e_guardar_semanais(novo)

        # ── Persistir tudo ──────────────────────────────────────────────────
            Persistencia.guardar_clientes(self.clientes_map)
            Persistencia.guardar_marcacoes(self.marcacoes_map)

            return {"success": True}

        except Exception as e:
            print(f"[AppController] alterar_cliente: {e}")
            return {"success": False, "error": str(e)}

    def apagar_cliente(self, nome: str):
        try:
            if not nome or nome not in self.clientes_map:
                return {"success": False, "error": "Cliente não encontrado"}

            cliente = self.clientes_map[nome]
            hoje_dt = datetime.combine(date.today(), datetime.min.time())

            to_remove = [
                dt for dt, m in self.marcacoes_map.items()
                if m.get_cliente() and
                    self._get_nome_safe(m.get_cliente()) == nome and
                    dt >= hoje_dt
            ]
            for dt in to_remove:
                del self.marcacoes_map[dt]

            Persistencia.guardar_marcacoes(self.marcacoes_map)
            del self.clientes_map[nome]
            Persistencia.guardar_clientes(self.clientes_map)

            # Limpar slots semanais do cliente apagado
            Database.apagar_slots_semanais_cliente(nome)

            Logger.log_cliente_apagado(cliente.get_nome())
            return {"success": True}

        except Exception as e:
            print(f"[AppController] apagar_cliente: {e}")
            return {"success": False, "error": str(e)}

    # ── Marcações ──────────────────────────────────────────────────────────────

    def get_marcacoes_map(self):
        """Devolve TODAS as marcações serializadas para o JS."""
        return {
            dt.isoformat(): self._marcacao_to_dict(m)
            for dt, m in self.marcacoes_map.items()
        }

    def get_marcacoes_periodo(self, data_inicio: str, data_fim: str):
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
        """
        Cria uma (ou mais) marcações de 15 min para cobrir a duração pedida,
        exactamente como o Java faz no AdicionarMarcacaoController.
        """
        try:
            # suporte a cliente desconhecido (nome arbitrário não registado)
            data_hora_obj = self._parse_dt(data_hora)
            if data_hora_obj is None:
                return {"success": False, "error": "Data/hora inválida."}

            if cliente_nome in self.clientes_map:
                cliente = self.clientes_map[cliente_nome]
            else:
                # cliente desconhecido — cria objecto temporário
                cliente = Cliente(cliente_nome, "", TipoCliente.DESCONHECIDO)

            duracao = int(duracao)
            minutos_restantes = duracao
            bloco_atual = data_hora_obj

            while minutos_restantes > 0:
                m_existente = self.marcacoes_map.get(bloco_atual)
                bloco_dur = 15

                if m_existente is not None and m_existente.get_duracao() == 15:
                    bloco_atual += self._td(15)
                    continue

                proximo = self.marcacoes_map.get(bloco_atual + self._td(15))
                if proximo is not None and proximo.get_duracao() == 15:
                    bloco_dur = 15
                elif minutos_restantes >= 30:
                    bloco_dur = 30
                elif minutos_restantes >= 15:
                    bloco_dur = 15

                ja_existe = self.marcacoes_map.get(bloco_atual)
                if ja_existe is not None and ja_existe.get_duracao() >= 30:
                    bloco_atual += self._td(bloco_dur)
                    continue

                nova = Marcacao(bloco_atual, cliente, bloco_dur, observacoes)
                self.marcacoes_map[bloco_atual] = nova
                Logger.log_marcacao_criada(nova)

                minutos_restantes -= bloco_dur
                bloco_atual += self._td(bloco_dur)

            Persistencia.guardar_marcacoes(self.marcacoes_map)
            return {"success": True, "message": "Marcação criada com sucesso."}

        except Exception as e:
            print(f"[AppController] criar_marcacao: {e}")
            return {"success": False, "error": str(e)}

    def criar_marcacao_desconhecido(self, nome_cliente: str, numero_telefone: str,
                                    data_hora: str, duracao: int, observacoes: str = ""):
        """Cria marcação para cliente desconhecido (não registado)."""
        try:
            data_hora_obj = self._parse_dt(data_hora)
            if data_hora_obj is None:
                return {"success": False, "error": "Data/hora inválida."}

            cliente = Cliente(nome_cliente, numero_telefone or "", TipoCliente.DESCONHECIDO)
            duracao = int(duracao)
            minutos_restantes = duracao
            bloco_atual = data_hora_obj

            while minutos_restantes > 0:
                m_existente = self.marcacoes_map.get(bloco_atual)
                bloco_dur = 15

                if m_existente is not None and m_existente.get_duracao() == 15:
                    bloco_atual += self._td(15)
                    continue

                proximo = self.marcacoes_map.get(bloco_atual + self._td(15))
                if proximo is not None and proximo.get_duracao() == 15:
                    bloco_dur = 15
                elif minutos_restantes >= 30:
                    bloco_dur = 30
                elif minutos_restantes >= 15:
                    bloco_dur = 15

                ja_existe = self.marcacoes_map.get(bloco_atual)
                if ja_existe is not None and ja_existe.get_duracao() >= 30:
                    bloco_atual += self._td(bloco_dur)
                    continue

                nova = Marcacao(bloco_atual, cliente, bloco_dur, observacoes)
                self.marcacoes_map[bloco_atual] = nova
                Logger.log_marcacao_criada(nova)

                minutos_restantes -= bloco_dur
                bloco_atual += self._td(bloco_dur)

            Persistencia.guardar_marcacoes(self.marcacoes_map)
            return {"success": True}

        except Exception as e:
            print(f"[AppController] criar_marcacao_desconhecido: {e}")
            return {"success": False, "error": str(e)}

    def apagar_marcacao(self, data_hora: str):
        try:
            dt = self._parse_dt(data_hora)
            if dt is None or dt not in self.marcacoes_map:
                return {"success": False, "error": "Marcação não encontrada."}

            marcacao = self.marcacoes_map.pop(dt)
            Persistencia.guardar_marcacoes(self.marcacoes_map)
            Logger.log_marcacao_apagada(marcacao)
            return {"success": True}

        except Exception as e:
            print(f"[AppController] apagar_marcacao: {e}")
            return {"success": False, "error": str(e)}

    def alterar_marcacao(self, data_hora_original: str, data_hora_nova: str,
                         observacoes: str):
        """Altera data/hora e/ou observações de uma marcação."""
        try:
            dt_orig = self._parse_dt(data_hora_original)
            if dt_orig is None or dt_orig not in self.marcacoes_map:
                return {"success": False, "error": "Marcação não encontrada."}

            marcacao = self.marcacoes_map[dt_orig]
            dt_nova  = self._parse_dt(data_hora_nova) if data_hora_nova else dt_orig
            if dt_nova is None:
                dt_nova = dt_orig

            if observacoes is not None:
                old_obs = marcacao.get_observacoes()
                marcacao.set_observacoes(observacoes)
                if observacoes != old_obs:
                    Logger.log_marcacao_obs_alterada(marcacao, observacoes)

            if dt_nova != dt_orig:
                if dt_nova in self.marcacoes_map:
                    return {"success": False, "error": "Já existe uma marcação nessa hora."}
                del self.marcacoes_map[dt_orig]
                marcacao.set_data_hora(dt_nova)
                Logger.log_marcacao_data_hora_alterada(
                    marcacao,
                    dt_orig.strftime("%d/%m/%Y %H:%M"),
                    dt_nova.strftime("%d/%m/%Y %H:%M")
                )

            self.marcacoes_map[dt_nova] = marcacao
            Persistencia.guardar_marcacoes(self.marcacoes_map)
            return {"success": True}

        except Exception as e:
            print(f"[AppController] alterar_marcacao: {e}")
            return {"success": False, "error": str(e)}

    def alterar_observacoes_marcacao(self, data_hora: str, observacoes: str):
        """Atalho para alterar só as observações."""
        return self.alterar_marcacao(data_hora, data_hora, observacoes)

    def marcar_falta_marcacao(self, data_hora: str):
        try:
            dt = self._parse_dt(data_hora)
            if dt is None or dt not in self.marcacoes_map:
                return {"success": False, "error": "Marcação não encontrada."}

            marcacao = self.marcacoes_map[dt]
            marcacao.set_falta(True)
            self.marcacoes_map[dt] = marcacao

            # adiciona falta ao cliente registado
            nome_cliente = self._get_nome_safe(marcacao.get_cliente())
            if nome_cliente and nome_cliente in self.clientes_map:
                c = self.clientes_map[nome_cliente]
                c.set_faltas(c.get_faltas() + 1)
                Persistencia.guardar_clientes(self.clientes_map)

            Persistencia.guardar_marcacoes(self.marcacoes_map)
            Logger.log_marcacao_falta(marcacao)
            return {"success": True}

        except Exception as e:
            print(f"[AppController] marcar_falta_marcacao: {e}")
            return {"success": False, "error": str(e)}

    def get_horas_disponiveis_data(self, data_hora_original: str, data_alvo: str, duracao: int):
        """
        Devolve horas disponíveis para remarcar uma marcação para uma data específica.

        data_hora_original : ISO da marcação actual (para excluir ela própria do mapa).
        data_alvo          : ISO da data-alvo (ano-mês-dia T 00:00:00 — hora ignorada).
        duracao            : duração em minutos (15, 30, 45, ...).

        Regras:
        - Não permite data/hora no passado (antes do momento actual).
        - Não permite slots que colidam com marcações existentes
          (excepto a própria marcação original).
        - Respeita o horário de funcionamento: 07:00–21:00 (último slot às 21:00,
          fim às 21:30 para marcação de 30 min).
        """
        from datetime import timedelta

        try:
            dt_orig  = self._parse_dt(data_hora_original)
            dt_alvo  = self._parse_dt(data_alvo)
            duracao  = int(duracao)
            agora    = datetime.now()

            if dt_orig is None or dt_alvo is None:
                return {"success": False, "error": "Data inválida.", "horas": []}

            nova_data = dt_alvo.date()
            hora_fecho_fim = datetime(nova_data.year, nova_data.month, nova_data.day, 21, 30)

            step = 15 if duracao == 15 else 30
            horas = []

            for h in range(7, 22):
                for m in range(0, 60, step):
                    if h == 21 and m > 0:
                        continue

                    candidate    = datetime(nova_data.year, nova_data.month, nova_data.day, h, m)
                    fim_candidate = candidate + timedelta(minutes=duracao)

                    # Fora do horário de funcionamento
                    if fim_candidate > hora_fecho_fim:
                        continue

                    # Não pode ser no passado (dia passado, ou mesmo dia mas hora passada)
                    if candidate <= agora:
                        continue

                    # Verificar disponibilidade bloco a bloco (cada 15 min)
                    livre = True
                    for i in range(0, duracao, 15):
                        bloco       = candidate + timedelta(minutes=i)
                        m_existente = self.marcacoes_map.get(bloco)
                        if m_existente is not None and bloco != dt_orig:
                            livre = False
                            break

                    if livre:
                        horas.append(f"{h:02d}:{m:02d}")

            return {"success": True, "horas": horas}

        except Exception as e:
            print(f"[AppController] get_horas_disponiveis_data: {e}")
            return {"success": False, "error": str(e), "horas": []}

    # ── Anotações ──────────────────────────────────────────────────────────────

    def guardar_anotacoes(self, texto: str):
        try:
            Persistencia.guardar_anotacoes(texto)
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def ler_anotacoes(self):
        try:
            texto = Persistencia.ler_anotacoes()
            return {"success": True, "texto": texto}
        except Exception as e:
            return {"success": False, "error": str(e), "texto": ""}

    # ── Pendentes ──────────────────────────────────────────────────────────────

    def get_pendentes(self):
        return [self._pendente_to_dict(p) for p in self.pendentes]

    def adicionar_pendente(self, nome: str, numero_telefone: str = ""):
        """Adiciona um pendente e persiste imediatamente."""
        try:
            nome = (nome or "").strip()
            if not nome:
                return {"success": False, "error": "Nome é obrigatório."}

            # evitar duplicados
            if any(p.get_nome().lower() == nome.lower() for p in self.pendentes):
                return {"success": False, "error": "Este cliente já está na lista de pendentes."}

            novo = Pendente(nome, numero_telefone or "")
            self.pendentes.append(novo)
            Persistencia.guardar_pendentes(self.pendentes)
            Logger.log_pendente_adicionado(novo)
            return {"success": True}

        except Exception as e:
            print(f"[AppController] adicionar_pendente: {e}")
            return {"success": False, "error": str(e)}

    def remover_pendente(self, nome: str):
        """Remove pendente pelo nome e persiste."""
        try:
            nome = (nome or "").strip()
            antes = len(self.pendentes)
            for p in self.pendentes:
                if p.get_nome().lower() == nome.lower():
                    Logger.log_pendente_removido(p)
                    break
            self.pendentes = [p for p in self.pendentes if p.get_nome().lower() != nome.lower()]
            if len(self.pendentes) == antes:
                return {"success": False, "error": "Pendente não encontrado."}
            Persistencia.guardar_pendentes(self.pendentes)
            return {"success": True}
        except Exception as e:
            print(f"[AppController] remover_pendente: {e}")
            return {"success": False, "error": str(e)}

    def guardar_pendentes_lista(self, lista: list):
        """Recebe lista de dicts {nome, numeroTelefone} e persiste."""
        try:
            self.pendentes = [Pendente(p.get("nome", ""), p.get("numeroTelefone", "")) for p in lista]
            Persistencia.guardar_pendentes(self.pendentes)
            return {"success": True}
        except Exception as e:
            print(f"[AppController] guardar_pendentes_lista: {e}")
            return {"success": False, "error": str(e)}

    # ── Utils ──────────────────────────────────────────────────────────────────

    def get_current_time(self):
        return datetime.now().strftime("%H:%M:%S")

    @staticmethod
    def _converter_tipo(tipo_raw) -> TipoCliente:
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

    @staticmethod
    def _parse_dt(s: str) -> Optional[datetime]:
        """Converte string ISO (com ou sem Z/timezone) para datetime naive."""
        if not s:
            return None
        try:
            s = s.replace("Z", "+00:00")
            dt = datetime.fromisoformat(s)
            if dt.tzinfo is not None:
                dt = dt.replace(tzinfo=None)
            return dt
        except Exception:
            return None

    @staticmethod
    def _td(minutes: int):
        from datetime import timedelta
        return timedelta(minutes=minutes)

    def _gerar_e_guardar_semanais(self, cliente: Cliente):
        try:
            slots_usados = set(
                Database.ler_slots_semanais_usados(cliente.get_nome())
            )
            novas = MarcacoesSemanais.gerar_marcacoes_semanais(
                cliente, self.marcacoes_map, date.today(),
                meses_a_frente=6, slots_usados=slots_usados
            )
            slots_a_guardar = []
            for m in novas:
                slots_a_guardar.append({
                    "cliente_nome": cliente.get_nome(),
                    "data_hora":    m.get_data_hora().isoformat()
                })
                try:
                    Logger.log_marcacao_criada(m)
                except Exception:
                    pass
            if slots_a_guardar:
                Database.inserir_slots_semanais_bulk(slots_a_guardar)
            Persistencia.guardar_marcacoes(self.marcacoes_map)
        except Exception as e:
            print(f"[AppController] _gerar_e_guardar_semanais: {e}")

    def _reprocessar_semanais(self, nome_original: str, novo: Cliente):
        try:
            hoje_dt = datetime.combine(date.today(), datetime.min.time())
            to_remove = [
                dt for dt, m in self.marcacoes_map.items()
                if m.get_cliente() and
                    self._get_nome_safe(m.get_cliente()) == nome_original and
                    dt >= hoje_dt
            ]
            for dt in to_remove:
                del self.marcacoes_map[dt]

            # Limpar slots usados para que sejam regenerados com o novo horário
            Database.apagar_slots_semanais_cliente(nome_original)
            if novo.get_nome() != nome_original:
                Database.apagar_slots_semanais_cliente(novo.get_nome())

            if novo.get_tipo_cliente() == TipoCliente.SEMANAL:
                self._gerar_e_guardar_semanais(novo)
            else:
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
                "nome": "N/A", "numeroTelefone": "",
                "tipoCliente": TipoCliente.DESCONHECIDO.value,
                "faltas": 0, "diaSemana": None, "horaCorte": None,
                "rapido": False, "temporario": True,
            }
        else:
            cliente_dict = self._cliente_to_dict(cliente)

        dh = marcacao.get_data_hora()
        return {
            "dataHora":    dh.isoformat() if dh else "N/A",
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
        
    def _get_nome_safe(self, obj) -> Optional[str]:
        """Retorna o nome de um objeto Cliente/Pendente/Utilizador mesmo que seja None ou dict."""
        if obj is None:
            return None
        # dict-like
        if isinstance(obj, dict):
            for key in ("nome", "name", "get_nome", "getName"):
                if key in obj and obj[key]:
                    return str(obj[key])
            return None
        # objeto com método get_nome ou atributo nome
        try:
            if hasattr(obj, "get_nome") and callable(getattr(obj, "get_nome")):
                val = obj.get_nome()
                return str(val) if val is not None else None
        except Exception:
            pass
        # atributo nome direto
        if hasattr(obj, "nome"):
            try:
                val = getattr(obj, "nome")
                return str(val) if val is not None else None
            except Exception:
                pass
        # fallback to str(obj) if it looks usable
        try:
            s = str(obj)
            return s if s else None
        except Exception:
            return None
    
    def _apagar_marcacoes_futuras_cliente(self, nome: str):
        """Apaga apenas marcações futuras de um cliente (hoje inclusive)."""
        hoje_dt = datetime.combine(date.today(), datetime.min.time())
        to_remove = [
            dt for dt, m in self.marcacoes_map.items()
            if m.get_cliente() and
                self._get_nome_safe(m.get_cliente()) == nome and
                dt >= hoje_dt
        ]
        for dt in to_remove:
            del self.marcacoes_map[dt]