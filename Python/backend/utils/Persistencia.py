import json
import os
from datetime import datetime, date, timedelta
from typing import Dict, List, Optional
from ..models.Utilizador import Utilizador
from ..models.Cliente import Cliente, TipoCliente
from ..models.Marcacao import Marcacao
from ..models.Pendente import Pendente

# BASE_DIR = raiz do projecto (AppAgenda/)
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA_DIR = os.path.join(BASE_DIR, 'data')

class Persistencia:
    @staticmethod
    def _data_path(*parts):
        return os.path.join(DATA_DIR, *parts)
    
    @staticmethod
    def ler_utilizador() -> Optional[Utilizador]:
        """Lê utilizador do ficheiro JSON"""
        path = Persistencia._data_path('utilizador.json')
        try:
            with open(path, "r", encoding="utf-8") as file:
                data = json.load(file)
                if data:
                    return Utilizador(data.get("nome", ""), data.get("password", ""))
                return None
        except FileNotFoundError:
            print(f"[Persistencia] ficheiro utilizador não encontrado: {path}")
            return None
        except json.JSONDecodeError as e:
            print(f"[Persistencia] JSON inválido em {path}: {e}")
            return None
        except Exception as e:
            print(f"[Persistencia] erro ler_utilizador: {e}")
            return None

    @staticmethod
    def ler_clientes() -> Dict[str, Cliente]:
        """Lê clientes do ficheiro JSON"""
        path = Persistencia._data_path('clientes.json')
        try:
            with open(path, "r", encoding="utf-8") as file:
                data = json.load(file)
                cliente_map = {}
                if data:
                    for cliente_data in data:
                        # Converter tipoCliente para Enum TipoCliente de forma robusta
                        tipo_raw = cliente_data.get("tipoCliente", "DESCONHECIDO")
                        tipo = TipoCliente.DESCONHECIDO
                        try:
                            if isinstance(tipo_raw, TipoCliente):
                                tipo = tipo_raw
                            elif isinstance(tipo_raw, str):
                                try:
                                    tipo = TipoCliente[tipo_raw.upper()]
                                except KeyError:
                                    try:
                                        tipo = TipoCliente(tipo_raw)
                                    except Exception:
                                        tipo = TipoCliente.DESCONHECIDO
                        except Exception:
                            tipo = TipoCliente.DESCONHECIDO

                        cliente = Cliente(
                            nome=cliente_data.get("nome", ""),
                            numero_telefone=cliente_data.get("numeroTelefone", ""),
                            tipo_cliente=tipo,
                            dia_semana=cliente_data.get("diaSemana"),
                            hora_corte=cliente_data.get("horaCorte"),
                            rapido=cliente_data.get("rapido", False)
                        )
                        cliente.set_faltas(cliente_data.get("faltas", 0))
                        cliente_map[cliente.get_nome()] = cliente
                return cliente_map
        except FileNotFoundError:
            print(f"[Persistencia] ficheiro clientes não encontrado: {path}")
            return {}
        except json.JSONDecodeError as e:
            print(f"[Persistencia] JSON inválido em {path}: {e}")
            return {}
        except Exception as e:
            print(f"[Persistencia] erro ler_clientes: {e}")
            return {}

    @staticmethod
    def ler_marcacoes() -> Dict[datetime, Marcacao]:
        """Lê marcações dos ficheiros JSON (últimos 6 meses + próximos 3 anos)"""
        marcacao_map = {}
        hoje = date.today()
        inicio = hoje.replace(day=1) - timedelta(days=6*30)  # aproximado

        # Ler ficheiros por intervalo (histórico + futuro)
        # primeiro: últimos 6 meses até hoje
        data = inicio
        while data <= hoje:
            ano = str(data.year)
            mes = f"{data.month:02d}"
            file_path = Persistencia._data_path('Marcacoes', ano, f"marcacoes{mes}.json")
            try:
                with open(file_path, "r", encoding="utf-8") as file:
                    marcacoes = json.load(file)
                    if marcacoes:
                        for marcacao_data in marcacoes:
                            try:
                                dt = datetime.fromisoformat(marcacao_data["dataHora"])
                            except Exception:
                                continue
                            # Converter cliente
                            cliente_info = marcacao_data.get("cliente", {})
                            tipo_raw = cliente_info.get("tipoCliente", "DESCONHECIDO")
                            tipo = TipoCliente.DESCONHECIDO
                            try:
                                if isinstance(tipo_raw, TipoCliente):
                                    tipo = tipo_raw
                                elif isinstance(tipo_raw, str):
                                    try:
                                        tipo = TipoCliente[tipo_raw.upper()]
                                    except KeyError:
                                        try:
                                            tipo = TipoCliente(tipo_raw)
                                        except Exception:
                                            tipo = TipoCliente.DESCONHECIDO
                            except Exception:
                                tipo = TipoCliente.DESCONHECIDO

                            cliente = Cliente(
                                nome=cliente_info.get("nome", ""),
                                numero_telefone=cliente_info.get("numeroTelefone", ""),
                                tipo_cliente=tipo
                            )
                            marcacao = Marcacao(
                                data_hora=dt,
                                cliente=cliente,
                                duracao=marcacao_data.get("duracao", 30),
                                observacoes=marcacao_data.get("observacoes", "")
                            )
                            marcacao.set_falta(marcacao_data.get("falta", False))
                            marcacao_map[dt] = marcacao
            except FileNotFoundError:
                # ficheiro pode não existir — ok
                pass
            except json.JSONDecodeError as e:
                print(f"[Persistencia] JSON inválido em {file_path}: {e}")
            except Exception as e:
                print(f"[Persistencia] erro ler marcacoes em {file_path}: {e}")

            # próximo mês
            if data.month == 12:
                data = data.replace(year=data.year + 1, month=1)
            else:
                data = data.replace(month=data.month + 1)

        # Ler futuros (restante ano + próximos 3 anos)
        ano_atual = hoje.year
        for ano in range(ano_atual, ano_atual + 4):
            mes_inicio = hoje.month if ano == ano_atual else 1
            for mes in range(mes_inicio, 13):
                ano_str = str(ano)
                mes_str = f"{mes:02d}"
                file_path = Persistencia._data_path('Marcacoes', ano_str, f"marcacoes{mes_str}.json")
                try:
                    with open(file_path, "r", encoding="utf-8") as file:
                        marcacoes = json.load(file)
                        if marcacoes:
                            for marcacao_data in marcacoes:
                                try:
                                    dt = datetime.fromisoformat(marcacao_data["dataHora"])
                                except Exception:
                                    continue
                                cliente_info = marcacao_data.get("cliente", {})
                                tipo_raw = cliente_info.get("tipoCliente", "DESCONHECIDO")
                                tipo = TipoCliente.DESCONHECIDO
                                try:
                                    if isinstance(tipo_raw, TipoCliente):
                                        tipo = tipo_raw
                                    elif isinstance(tipo_raw, str):
                                        try:
                                            tipo = TipoCliente[tipo_raw.upper()]
                                        except KeyError:
                                            try:
                                                tipo = TipoCliente(tipo_raw)
                                            except Exception:
                                                tipo = TipoCliente.DESCONHECIDO
                                except Exception:
                                    tipo = TipoCliente.DESCONHECIDO

                                cliente = Cliente(
                                    nome=cliente_info.get("nome", ""),
                                    numero_telefone=cliente_info.get("numeroTelefone", ""),
                                    tipo_cliente=tipo
                                )
                                marcacao = Marcacao(
                                    data_hora=dt,
                                    cliente=cliente,
                                    duracao=marcacao_data.get("duracao", 30),
                                    observacoes=marcacao_data.get("observacoes", "")
                                )
                                marcacao.set_falta(marcacao_data.get("falta", False))
                                marcacao_map[dt] = marcacao
                except FileNotFoundError:
                    pass
                except json.JSONDecodeError as e:
                    print(f"[Persistencia] JSON inválido em {file_path}: {e}")
                except Exception as e:
                    print(f"[Persistencia] erro ler marcacoes em {file_path}: {e}")

        return marcacao_map


    @staticmethod
    def ler_anotacoes() -> str:
        """Lê anotações do ficheiro JSON"""
        path = Persistencia._data_path('anotacoes.json')
        try:
            with open(path, "r", encoding="utf-8") as file:
                data = json.load(file)
                return data if data else ""
        except (FileNotFoundError, json.JSONDecodeError, Exception):
            return ""

    @staticmethod
    def ler_pendentes() -> List[Pendente]:
        """Lê pendentes do ficheiro JSON"""
        path = Persistencia._data_path('pendentes.json')
        try:
            with open(path, "r", encoding="utf-8") as file:
                data = json.load(file)
                pendentes = []
                if data:
                    for pendente_data in data:
                        pendente = Pendente(
                            nome=pendente_data.get("nome", ""),
                            numero_telefone=pendente_data.get("numeroTelefone", "")
                        )
                        pendentes.append(pendente)
                return pendentes
        except (FileNotFoundError, json.JSONDecodeError, Exception):
            return []

    @staticmethod
    def guardar_utilizador(utilizador: Utilizador) -> bool:
        """Guarda utilizador no ficheiro JSON"""
        try:
            os.makedirs(Persistencia._data_path(), exist_ok=True)
            path = Persistencia._data_path('utilizador.json')
            data = {
                "nome": utilizador.get_nome(),
                "password": utilizador.get_password()
            }
            with open(path, "w", encoding="utf-8") as file:
                json.dump(data, file, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False

    @staticmethod
    def guardar_clientes(clientes: Dict[str, Cliente]) -> bool:
        """Guarda clientes no ficheiro JSON"""
        try:
            os.makedirs(Persistencia._data_path(), exist_ok=True)
            clientes_ordenados = sorted(clientes.values(), key=lambda c: c.get_nome().lower())
            data = []
            for cliente in clientes_ordenados:
                cliente_data = {
                    "nome": cliente.get_nome(),
                    "numeroTelefone": cliente.get_numero_telefone(),
                    "tipoCliente": cliente.get_tipo_cliente().value,
                    "faltas": cliente.get_faltas(),
                    "diaSemana": cliente.get_dia_semana(),
                    "horaCorte": cliente.get_hora_corte(),
                    "rapido": cliente.is_rapido(),
                    "temporario": cliente.is_temporario() if hasattr(cliente, 'is_temporario') else False
                }
                data.append(cliente_data)

            path = Persistencia._data_path('clientes.json')
            with open(path, "w", encoding="utf-8") as file:
                json.dump(data, file, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False

    @staticmethod
    def guardar_marcacoes(marcacoes: Dict[datetime, Marcacao]) -> bool:
        """Guarda marcações nos ficheiros JSON organizados por ano/mês"""
        try:
            # Agrupar marcações por ano/mês
            marcacoes_por_ano_mes = {}
            for marcacao in marcacoes.values():
                data_hora = marcacao.get_data_hora()
                chave = f"{data_hora.year}-{data_hora.month:02d}"
                if chave not in marcacoes_por_ano_mes:
                    marcacoes_por_ano_mes[chave] = []
                
                marcacao_data = {
                    "dataHora": data_hora.isoformat(),
                    "cliente": {
                        "nome": marcacao.get_cliente().get_nome(),
                        "numeroTelefone": marcacao.get_cliente().get_numero_telefone(),
                        "tipoCliente": marcacao.get_cliente().get_tipo_cliente().value,
                        "faltas": marcacao.get_cliente().get_faltas(),
                        "diaSemana": marcacao.get_cliente().get_dia_semana(),
                        "horaCorte": marcacao.get_cliente().get_hora_corte(),
                        "rapido": marcacao.get_cliente().is_rapido()
                    },
                    "duracao": marcacao.get_duracao(),
                    "observacoes": marcacao.get_observacoes(),
                    "falta": marcacao.is_falta()
                }
                marcacoes_por_ano_mes[chave].append(marcacao_data)

            # Guardar cada grupo no respetivo ficheiro
            for ano_mes, lista in marcacoes_por_ano_mes.items():
                partes = ano_mes.split("-")
                ano = partes[0]
                mes = partes[1]
                dir_path = f"data/Marcacoes/{ano}"
                file_path = f"{dir_path}/marcacoes{mes}.json"

                os.makedirs(dir_path, exist_ok=True)
                
                # Ordenar por data/hora
                lista.sort(key=lambda m: m["dataHora"])
                
                with open(file_path, "w", encoding="utf-8") as file:
                    json.dump(lista, file, indent=2, ensure_ascii=False)

            # Limpar ficheiros vazios no intervalo de leitura
            hoje = date.today()
            inicio = hoje.replace(day=1) - timedelta(days=6*30)
            fim = date(hoje.year + 3, 12, 31)

            marcacoes_root = "data/Marcacoes"
            if os.path.exists(marcacoes_root):
                for ano_dir in os.listdir(marcacoes_root):
                    ano_path = os.path.join(marcacoes_root, ano_dir)
                    if os.path.isdir(ano_path):
                        try:
                            ano_int = int(ano_dir)
                            for mes_file in os.listdir(ano_path):
                                if mes_file.startswith("marcacoes") and mes_file.endswith(".json"):
                                    mes_str = mes_file[9:-5]  # Remove "marcacoes" e ".json"
                                    try:
                                        mes_int = int(mes_str)
                                        data_ficheiro = date(ano_int, mes_int, 1)
                                        
                                        # Só apaga dentro do intervalo de leitura
                                        if inicio <= data_ficheiro <= fim:
                                            chave = f"{ano_dir}-{mes_str}"
                                            if chave not in marcacoes_por_ano_mes:
                                                os.remove(os.path.join(ano_path, mes_file))
                                    except (ValueError, OSError):
                                        continue
                            
                            # Remove diretório vazio
                            if not os.listdir(ano_path):
                                os.rmdir(ano_path)
                        except (ValueError, OSError):
                            continue

            return True
        except Exception:
            return False

    @staticmethod
    def guardar_anotacoes(anotacoes: str) -> bool:
        """Guarda anotações no ficheiro JSON"""
        try:
            os.makedirs("data", exist_ok=True)
            with open("data/anotacoes.json", "w", encoding="utf-8") as file:
                json.dump(anotacoes if anotacoes else "", file, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False

    @staticmethod
    def guardar_pendentes(pendentes: List[Pendente]) -> bool:
        """Guarda pendentes no ficheiro JSON"""
        try:
            os.makedirs("data", exist_ok=True)
            data = []
            for pendente in pendentes:
                pendente_data = {
                    "nome": pendente.get_nome(),
                    "numeroTelefone": pendente.get_numero_telefone()
                }
                data.append(pendente_data)
            
            with open("data/pendentes.json", "w", encoding="utf-8") as file:
                json.dump(data, file, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False