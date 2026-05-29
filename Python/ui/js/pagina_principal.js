class PaginaPrincipalController {
    constructor() {
        // Elementos DOM
        this.userLabel = document.getElementById('userLabel');
        this.logoutBtn = document.getElementById('logoutBtn');
        this.calendarioToggle = document.getElementById('calendarioToggle');
        this.clientesToggle = document.getElementById('clientesToggle');
        this.areaCentral = document.getElementById('areaCentral');
        this.areaClientes = document.getElementById('areaClientes');
        this.clientesContent = document.getElementById('clientesContent');
        this.calendarioGrid = document.getElementById('calendarioGrid');
        this.semanaLabel = document.getElementById('semanaLabel');
        this.relogioLabel = document.getElementById('relogioLabel');
        this.anotacoesArea = document.getElementById('anotacoesArea');
        this.blurToggleBtn = document.getElementById('blurToggleBtn');
        this.caixaClientesPendentes = document.getElementById('caixaClientesPendentes');
        
        // Botões de controlo
        this.todayBtn = document.getElementById('todayBtn');
        this.semanaAnteriorBtn = document.getElementById('semanaAnteriorBtn');
        this.proximaSemanaBtn = document.getElementById('proximaSemanaBtn');
        
        // Botões de visualização
        this.diaToggle = document.getElementById('diaToggle');
        this.semanaToggle = document.getElementById('semanaToggle');
        this.mesToggle = document.getElementById('mesToggle');
        
        // Estado da aplicação
        this.modoAtual = 'SEMANA';
        this.semanaAtual = this.getMonday(new Date());
        this.diaSelecionado = new Date();
        this.anotacoesBlurred = true;
        
        // Dados
        this.clientes = {};
        this.marcacoes = {};
        this.pendentes = [];
        
        // Horários de funcionamento
        this.HORA_ABERTURA = 7;
        this.HORA_FECHO = 21;
        this.INTERVALO_MINUTOS = 30;

        this.api = null;
        
        this.init();
    }

    async _waitForApi(timeout = 3000, interval = 100) {
        const start = Date.now();
        while (Date.now() - start < timeout) {
            const api = (window.pywebview && window.pywebview.api) ||
                        (typeof pywebview !== 'undefined' && pywebview && pywebview.api) ||
                        null;
            if (api) return api;
            // espera
            await new Promise(res => setTimeout(res, interval));
        }
        return null;
    }
    
    async init() {
        try {
            // esperar pela API (caso webview demore a injectar)
            const api = await this._waitForApi(3000, 100);
            this.api = api; // guarda para uso posterior

            if (this.api) {
                try {
                    const userInfo = await this.api.get_utilizador_info();

                    // tentar extrair nome com vários fallbacks
                    let nomeUsuario = null;
                    if (userInfo) {
                        if (typeof userInfo === 'object') {
                            nomeUsuario = userInfo.nome || userInfo.name || userInfo.username || userInfo.user || null;
                            if (!nomeUsuario && userInfo.utilizador && typeof userInfo.utilizador === 'object') {
                                nomeUsuario = userInfo.utilizador.nome || userInfo.utilizador.name || null;
                            }
                        } else if (typeof userInfo === 'string') {
                            nomeUsuario = userInfo;
                        }
                    }

                    if (nomeUsuario) {
                        this.userLabel.textContent = `Bem-vindo, ${nomeUsuario}`;
                    } else {
                        this.userLabel.textContent = 'Bem-vindo, Utilizador';
                    }
                } catch (e) {
                    console.warn('Erro ao obter utilizador:', e);
                }

                // Carregar clientes/marcações/pendentes (carregarDados já trata erros)
                try {
                    await this.carregarDados();
                } catch (e) {
                    console.warn('Erro ao carregar dados:', e);
                }

                // Ler anotações (opcional)
                try {
                    const anotacoes = await this.api.ler_anotacoes();
                    if (anotacoes && anotacoes.success) {
                        this.anotacoesArea.value = anotacoes.texto || '';
                    }
                } catch (e) {
                    // não crítico
                }
            } else {
                // API não apareceu — apenas fallback sem spam de warnings
                console.info('pywebview API não disponível após espera; a app corre em fallback (fora do webview).');
                await this.carregarDados(); // fallback (carregarDados faz tentativas)
            }
        } catch (error) {
            console.error('Erro ao inicializar:', error);
        }

        this.setupEventListeners();
        this.startClock();
        this.atualizarCalendario();
        this.atualizarPendentes();

        if (typeof this.scheduleCurrentSlotUpdater === 'function') {
            this.scheduleCurrentSlotUpdater();
        }
    }
    
    async carregarDados() {
        try {
            const api = this.api || ((window.pywebview && window.pywebview.api) || (typeof pywebview !== 'undefined' && pywebview && pywebview.api) || null);
            if (!api) {
                // sem API: manter dados vazios (fallback)
                this.clientes = {};
                this.marcacoes = {};
                this.pendentes = [];
                console.warn('carregarDados: pywebview API não disponível (fallback vazio).');
                return;
            }

            // Carregar clientes
            this.clientes = await api.get_clientes_map();

            // Carregar marcações
            this.marcacoes = await api.get_marcacoes_map();

            // Carregar pendentes
            this.pendentes = await api.get_pendentes();

        } catch (error) {
            console.error('Erro ao carregar dados:', error);
            // Dados vazios como fallback
            this.clientes = {};
            this.marcacoes = {};
            this.pendentes = [];
        }
    }
    
    setupEventListeners() {
        // Logout
        this.logoutBtn.addEventListener('click', () => this.handleLogout());
        
        // Navegação lateral
        this.calendarioToggle.addEventListener('click', () => this.mostrarCalendario());
        this.clientesToggle.addEventListener('click', () => this.mostrarClientes());
        
        // Controles de navegação
        this.todayBtn.addEventListener('click', () => this.handleToday());
        this.semanaAnteriorBtn.addEventListener('click', () => this.semanaAnterior());
        this.proximaSemanaBtn.addEventListener('click', () => this.proximaSemana());
        
        // Modos de visualização
        this.diaToggle.addEventListener('click', () => this.setModo('DIA'));
        this.semanaToggle.addEventListener('click', () => this.setModo('SEMANA'));
        this.mesToggle.addEventListener('click', () => this.setModo('MES'));
        
        // Anotações
        this.blurToggleBtn.addEventListener('click', () => this.toggleBlur());
        this.anotacoesArea.addEventListener('blur', () => this.guardarAnotacoes());
        
        // Pendentes
        this.caixaClientesPendentes.addEventListener('click', () => this.abrirGestaoPendentes());
    }
    
    // Relógio
    startClock() {
        this.updateClock();
        setInterval(() => {
            this.updateClock();
        }, 1000);
    }
    
    updateClock() {
        const now = new Date();
        const timeString = now.toLocaleTimeString('pt-PT', {
            hour12: false,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
        this.relogioLabel.textContent = timeString;
    }
    
    // Logout
    async handleLogout() {
        try {
            // Guardar anotações antes de sair
            await this.guardarAnotacoes();

            const api = this.api || ((window.pywebview && window.pywebview.api) || (typeof pywebview !== 'undefined' && pywebview && pywebview.api) || null);
            if (!api) {
                console.error('API não disponível para logout.');
                return;
            }

            const result = await api.fazer_logout();
            if (result.success) {
                await api.mostrar_login();
            } else {
                console.error('Erro no logout:', result.error);
            }
        } catch (error) {
            console.error('Erro no logout:', error);
        }
    }
    
    // Anotações
    toggleBlur() {
        this.anotacoesBlurred = !this.anotacoesBlurred;
        
        if (this.anotacoesBlurred) {
            this.anotacoesArea.classList.add('blurred');
            this.blurToggleBtn.textContent = '👁';
            this.anotacoesArea.disabled = true;
        } else {
            this.anotacoesArea.classList.remove('blurred');
            this.blurToggleBtn.textContent = '⛔';
            this.anotacoesArea.disabled = false;
            this.anotacoesArea.focus();
        }
    }
    
    async guardarAnotacoes() {
        try {
            const texto = this.anotacoesArea.value;
            const api = this.api || ((window.pywebview && window.pywebview.api) || (typeof pywebview !== 'undefined' && pywebview && pywebview.api) || null);
            if (!api) return;
            await api.guardar_anotacoes(texto);
        } catch (error) {
            console.error('Erro ao guardar anotações:', error);
        }
    }
    
    // Navegação
    mostrarCalendario() {
        this.calendarioToggle.classList.add('active');
        this.clientesToggle.classList.remove('active');
        this.areaCentral.style.display = 'flex';
        this.areaClientes.classList.add('hidden');
        this.atualizarCalendario();
    }
    
    mostrarClientes() {
        this.calendarioToggle.classList.remove('active');
        this.clientesToggle.classList.add('active');
        this.areaCentral.style.display = 'none';
        this.areaClientes.classList.remove('hidden');
        this.atualizarAreaClientes();
    }
    
    // Controles de navegação
    handleToday() {
        const hoje = new Date();
        this.semanaAtual = this.getMonday(hoje);
        this.diaSelecionado = hoje;
        this.atualizarCalendario();
    }
    
    semanaAnterior() {
        switch (this.modoAtual) {
            case 'SEMANA':
                this.semanaAtual = new Date(this.semanaAtual.getTime() - 7 * 24 * 60 * 60 * 1000);
                break;
            case 'MES':
                const mesAnterior = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() - 1, 1);
                this.semanaAtual = mesAnterior;
                break;
            case 'DIA':
                this.diaSelecionado = new Date(this.diaSelecionado.getTime() - 24 * 60 * 60 * 1000);
                this.semanaAtual = this.getMonday(this.diaSelecionado);
                break;
        }
        this.atualizarCalendario();
    }
    
    proximaSemana() {
        switch (this.modoAtual) {
            case 'SEMANA':
                this.semanaAtual = new Date(this.semanaAtual.getTime() + 7 * 24 * 60 * 60 * 1000);
                break;
            case 'MES':
                const proximoMes = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() + 1, 1);
                this.semanaAtual = proximoMes;
                break;
            case 'DIA':
                this.diaSelecionado = new Date(this.diaSelecionado.getTime() + 24 * 60 * 60 * 1000);
                this.semanaAtual = this.getMonday(this.diaSelecionado);
                break;
        }
        this.atualizarCalendario();
    }
    
    setModo(modo) {
        this.modoAtual = modo;
        
        // Atualizar botões
        this.diaToggle.classList.remove('active');
        this.semanaToggle.classList.remove('active');
        this.mesToggle.classList.remove('active');
        
        switch (modo) {
            case 'DIA':
                this.diaToggle.classList.add('active');
                break;
            case 'SEMANA':
                this.semanaToggle.classList.add('active');
                break;
            case 'MES':
                this.mesToggle.classList.add('active');
                this.semanaAtual = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth(), 1);
                break;
        }
        
        this.atualizarCalendario();
    }
    
    // Calendário
    atualizarCalendario() {
        this.calendarioGrid.innerHTML = '';
        this.calendarioGrid.className = `calendar-grid ${this.modoAtual.toLowerCase()}`;
        
        switch (this.modoAtual) {
            case 'SEMANA':
                this.criarCalendarioSemana();
                break;
            case 'MES':
                this.criarCalendarioMes();
                break;
            case 'DIA':
                this.criarCalendarioDia();
                break;
        }
        
        this.atualizarLabel();
    }
    
    criarCalendarioSemana() {
        // Cabeçalho vazio para coluna das horas
        this.calendarioGrid.appendChild(this.criarCelula('', 'header'));

        // Cabeçalhos dos dias
        const diasSemana = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab', 'Dom'];
        for (let i = 0; i < 7; i++) {
            const data = new Date(this.semanaAtual.getTime() + i * 24 * 60 * 60 * 1000);
            const texto = `${diasSemana[i]} ${data.getDate().toString().padStart(2, '0')}`;
            const celula = this.criarCelula(texto, 'header');

            if (this.isToday(data)) {
                celula.classList.add('today');
            } else if (this.isSunday(data)) {
                celula.classList.add('sunday');
            } else if (this.isHoliday(data)) {
                celula.classList.add('holiday');
            }

            celula.addEventListener('click', () => {
                this.diaSelecionado = data;
                this.setModo('DIA');
            });

            this.calendarioGrid.appendChild(celula);
        }

        for (let hora = this.HORA_ABERTURA; hora <= this.HORA_FECHO; hora++) {
            for (let minuto = 0; minuto < 60; minuto += this.INTERVALO_MINUTOS) {
                // Coluna da hora
                const horaTexto = `${hora.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}`;
                const celulaHora = this.criarCelula(horaTexto, 'hour');
                this.calendarioGrid.appendChild(celulaHora);

                // Células para cada dia
                for (let dia = 0; dia < 7; dia++) {
                    const data = new Date(this.semanaAtual.getTime() + dia * 24 * 60 * 60 * 1000);
                    const dataHora = new Date(data.getFullYear(), data.getMonth(), data.getDate(), hora, minuto);

                    const celula = this.criarCelulaHorario(dataHora);
                    this.calendarioGrid.appendChild(celula);
                }
            }
        }
    }
    
    // CORREÇÃO: Calendário mensal com layout correto
    criarCalendarioMes() {
        // Container principal do calendário mensal
        const mesContainer = document.createElement('div');
        mesContainer.className = 'mes-container';
        
        // Cabeçalho dos dias da semana
        const headerContainer = document.createElement('div');
        headerContainer.className = 'mes-header';
        
        const diasSemana = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab', 'Dom'];
        diasSemana.forEach(dia => {
            const celula = this.criarCelula(dia, 'header');
            headerContainer.appendChild(celula);
        });
        
        mesContainer.appendChild(headerContainer);
        
        // Container das semanas
        const weeksContainer = document.createElement('div');
        weeksContainer.className = 'mes-weeks';
        
        // Calcular primeiro e último dia do mês
        const primeiroDia = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth(), 1);
        const ultimoDia = new Date(this.semanaAtual.getFullYear(), this.semanaAtual.getMonth() + 1, 0);
        
        // Encontrar o primeiro dia a mostrar (segunda-feira da primeira semana)
        let diaSemanaInicio = primeiroDia.getDay();
        if (diaSemanaInicio === 0) diaSemanaInicio = 7; // Domingo = 7
        
        const inicioCalendario = new Date(primeiroDia);
        inicioCalendario.setDate(inicioCalendario.getDate() - (diaSemanaInicio - 1));
        
        // Encontrar o último dia a mostrar (domingo da última semana)
        let diaSemanaFim = ultimoDia.getDay();
        if (diaSemanaFim === 0) diaSemanaFim = 7; // Domingo = 7
        
        const fimCalendario = new Date(ultimoDia);
        fimCalendario.setDate(fimCalendario.getDate() + (7 - diaSemanaFim));
        
        // Criar semanas
        let dataAtual = new Date(inicioCalendario);
        
        while (dataAtual <= fimCalendario) {
            // Criar linha da semana
            const semanaRow = document.createElement('div');
            semanaRow.className = 'mes-week';
            
            // Criar 7 dias da semana
            for (let dia = 0; dia < 7; dia++) {
                const celula = this.criarCelula(dataAtual.getDate().toString(), '');
                
                // Aplicar estilos baseados no dia
                if (this.isToday(dataAtual)) {
                    celula.classList.add('today');
                } else if (this.isSunday(dataAtual)) {
                    celula.classList.add('sunday');
                } else if (this.isHoliday(dataAtual)) {
                    celula.classList.add('holiday');
                }
                
                // Dias de outros meses ficam mais transparentes
                if (dataAtual.getMonth() !== this.semanaAtual.getMonth()) {
                    celula.style.opacity = '0.4';
                    celula.style.fontSize = '16px';
                }
                
                // Capturar a data para o evento de clique
                const dataClique = new Date(dataAtual);
                celula.addEventListener('click', () => {
                    this.diaSelecionado = new Date(dataClique);
                    this.semanaAtual = this.getMonday(dataClique);
                    this.setModo('DIA');
                });
                
                semanaRow.appendChild(celula);
                
                // Avançar para o próximo dia
                dataAtual.setDate(dataAtual.getDate() + 1);
            }
            
            weeksContainer.appendChild(semanaRow);
        }
        
        mesContainer.appendChild(weeksContainer);
        this.calendarioGrid.appendChild(mesContainer);
    }
    
    criarCalendarioDia() {
        for (let hora = this.HORA_ABERTURA; hora <= this.HORA_FECHO; hora++) {
            for (let minuto = 0; minuto < 60; minuto += this.INTERVALO_MINUTOS) {
                // Coluna da hora
                const horaTexto = `${hora.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}`;
                this.calendarioGrid.appendChild(this.criarCelula(horaTexto, 'hour'));
                
                // Célula do conteúdo
                const dataHora = new Date(
                    this.diaSelecionado.getFullYear(),
                    this.diaSelecionado.getMonth(),
                    this.diaSelecionado.getDate(),
                    hora,
                    minuto
                );
                
                const celula = this.criarCelulaHorario(dataHora);
                this.calendarioGrid.appendChild(celula);
            }
        }
    }
    
    criarCelula(texto, tipo = '') {
        const celula = document.createElement('div');
        celula.className = `calendar-cell ${tipo}`;
        celula.textContent = texto;
        return celula;
    }
    
    criarCelulaHorario(dataHora) {
        const celula = document.createElement('div');
        celula.className = 'calendar-cell';

        // guardar data/hora ISO para referência
        const iso = dataHora.toISOString();
        celula.setAttribute('data-datetime', iso);
        
        // Verificar se é passado
        if (this.isPast(dataHora)) {
            celula.classList.add('past');
        }

        // destacar slot actual se coincidir (será também atualizado periodicamente)
        const currentSlotIso = this._getCurrentSlotIso();
        if (iso === currentSlotIso) {
            celula.classList.add('current-slot');
        } else {
            celula.classList.remove('current-slot');
        }
        
        // Verificar se há marcação
        const marcacao = this.getMarcacao(dataHora);
        if (marcacao) {
            const marcacaoEl = document.createElement('div');
            marcacaoEl.className = `marcacao ${marcacao.falta ? 'falta' : ''}`;
            marcacaoEl.textContent = marcacao.cliente.nome;
            marcacaoEl.addEventListener('click', (e) => {
                e.stopPropagation();
                this.abrirDetalheMarcacao(marcacao);
            });
            celula.appendChild(marcacaoEl);
        } else if (!this.isPast(dataHora)) {
            celula.addEventListener('click', () => {
                this.criarMarcacao(dataHora);
            });
        }
        
        return celula;
    }

    // Retorna Date do slot actual (arredondado para baixo 00 ou 30)
    _getCurrentSlotDate() {
        const now = new Date();
        const minutes = now.getMinutes();
        const roundedMin = minutes < 30 ? 0 : 30;
        return new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), roundedMin, 0, 0);
    }

    // Retorna ISO do slot actual (para comparação com data-datetime)
    _getCurrentSlotIso() {
        return this._getCurrentSlotDate().toISOString();
    }

    // Actualiza realce do bloco horário actual (remove de anteriores e aplica ao actual)
    // Actualiza realce do bloco horário actual (remove de anteriores e aplica ao actual)
    updateCurrentSlotHighlight() {
        // remover realces antigos
        const prev = document.querySelectorAll('.calendar-cell.current-slot');
        prev.forEach(el => el.classList.remove('current-slot'));

        const currentSlotDate = this._getCurrentSlotDate();
        const iso = currentSlotDate.toISOString();
        // procurar célula com esse data-datetime
        const el = document.querySelector(`.calendar-cell[data-datetime="${iso}"]`);
        if (el) {
            el.classList.add('current-slot');
        }

        // Tornar células seleccionáveis / não seleccionáveis de acordo com estado dinâmico:
        // - células com marcação (marcacao) continuam clicáveis para abrir detalhe
        // - células cujo horário é <= slot actual (ou já passado) ficam não seleccionáveis
        const cells = document.querySelectorAll('.calendar-cell[data-datetime]');
        cells.forEach(cell => {
            const dtIso = cell.getAttribute('data-datetime');
            if (!dtIso) return;
            const cellDate = new Date(dtIso);
            const hasMarcacao = !!this.getMarcacao(cellDate);

            if (hasMarcacao) {
                // permitir clique para abrir detalhe da marcação
                cell.style.pointerEvents = 'auto';
                cell.style.cursor = 'pointer';
            } else {
                // bloquear selecção para slots no passado ou até ao slot actual (inclui o current-slot)
                if (cellDate.getTime() <= currentSlotDate.getTime()) {
                    cell.style.pointerEvents = 'none';
                    cell.style.cursor = 'not-allowed';
                    cell.classList.add('unselectable');
                } else {
                    // futuro: permitir seleccionar
                    cell.style.pointerEvents = 'auto';
                    cell.style.cursor = 'default';
                    cell.classList.remove('unselectable');
                }
            }
        });
    }

    // Agendador: sincroniza para trocar exactamente na meia hora e depois a cada 30min
    scheduleCurrentSlotUpdater() {
        // executar já uma vez
        this.updateCurrentSlotHighlight();

        const now = new Date();
        const minutes = now.getMinutes();
        const seconds = now.getSeconds();
        const millis = now.getMilliseconds();
        const nextHalfMinute = (minutes < 30) ? 30 : 60;
        const minutesTo = nextHalfMinute - minutes;
        const msToNextBoundary = minutesTo * 60 * 1000 - seconds * 1000 - millis;

        // primeira actualização no próximo boundary exato
        setTimeout(() => {
            this.updateCurrentSlotHighlight();

            // depois correr a cada 30 minutos
            this._currentSlotInterval = setInterval(() => {
                this.updateCurrentSlotHighlight();
            }, 30 * 60 * 1000);
        }, msToNextBoundary);
    }
    
    // Clientes
    atualizarAreaClientes() {
        this.clientesContent.innerHTML = '';
        
        const clientesArray = Object.values(this.clientes);
        
        if (clientesArray.length === 0) {
            // Área vazia
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'clientes-empty';
            
            const message = document.createElement('div');
            message.className = 'clientes-empty-message';
            message.textContent = 'Não tem nenhum cliente salvo, deseja adicionar um?';
            
            const button = document.createElement('button');
            button.className = 'clientes-add-btn';
            button.textContent = 'Adicionar';
            button.addEventListener('click', () => this.adicionarCliente());
            
            emptyDiv.appendChild(message);
            emptyDiv.appendChild(button);
            this.clientesContent.appendChild(emptyDiv);
        } else {
            // Toolbar
            const toolbar = this.criarToolbarClientes();
            this.clientesContent.appendChild(toolbar);
            
            // Tabela de clientes
            const container = this.criarTabelaClientes(clientesArray);
            this.clientesContent.appendChild(container);
        }
    }
    
    criarToolbarClientes() {
        const toolbar = document.createElement('div');
        toolbar.className = 'clientes-toolbar';
        
        const searchField = document.createElement('input');
        searchField.className = 'search-field';
        searchField.type = 'text';
        searchField.placeholder = 'Pesquisar cliente...';
        
        const addBtn = document.createElement('button');
        addBtn.className = 'add-client-btn';
        addBtn.textContent = '+';
        addBtn.addEventListener('click', () => this.adicionarCliente());
        
        searchField.addEventListener('input', (e) => {
            this.filtrarClientes(e.target.value);
        });
        
        toolbar.appendChild(searchField);
        toolbar.appendChild(addBtn);
        
        return toolbar;
    }
    
    criarTabelaClientes(clientes, filtro = '') {
        const container = document.createElement('div');
        container.className = 'clientes-table-container';
        
        const table = document.createElement('div');
        table.className = 'clientes-table';
        
        // Cabeçalho
        const headers = ['Nome', 'Telefone', 'Tipo', 'Faltas', 'Dia Semana', 'Hora Corte'];
        headers.forEach(header => {
            const th = document.createElement('div');
            th.className = 'table-header';
            th.textContent = header;
            table.appendChild(th);
        });
        
        // Filtrar clientes
        const clientesFiltrados = clientes.filter(cliente => 
            cliente.nome.toLowerCase().includes(filtro.toLowerCase()) ||
            cliente.numeroTelefone.includes(filtro)
        );
        
        // Dados dos clientes
        clientesFiltrados.forEach(cliente => {
            const dados = [
                cliente.nome,
                cliente.numeroTelefone,
                cliente.tipoCliente,
                cliente.faltas.toString(),
                cliente.diaSemana || '—',
                cliente.horaCorte || '—'
            ];
            
            dados.forEach((dado, index) => {
                const cell = document.createElement('div');
                cell.className = 'table-cell';
                cell.textContent = dado;
                
                if (index === 0) { // Nome - clicável
                    cell.style.cursor = 'pointer';
                    cell.addEventListener('click', () => {
                        this.abrirDetalheCliente(cliente);
                    });
                }
                
                table.appendChild(cell);
            });
        });
        
        container.appendChild(table);
        return container;
    }
    
    filtrarClientes(filtro) {
        const clientesArray = Object.values(this.clientes);
        const container = this.clientesContent.querySelector('.clientes-table-container');
        if (container) {
            const newContainer = this.criarTabelaClientes(clientesArray, filtro);
            this.clientesContent.replaceChild(newContainer, container);
        }
    }
    
    atualizarPendentes() {
        this.caixaClientesPendentes.innerHTML = '';
        
        if (this.pendentes.length === 0) {
            const item = document.createElement('div');
            item.className = 'pendente-item placeholder';
            item.textContent = 'Clique para adicionar pendente';
            item.style.fontStyle = 'italic';
            item.style.color = '#bbb';
            this.caixaClientesPendentes.appendChild(item);
        } else {
            this.pendentes.forEach((pendente, index) => {
                const item = document.createElement('div');
                item.className = 'pendente-item';
                item.textContent = pendente.nome;
                this.caixaClientesPendentes.appendChild(item);
                
                if (index < this.pendentes.length - 1) {
                    const separator = document.createElement('div');
                    separator.className = 'pendente-separator';
                    this.caixaClientesPendentes.appendChild(separator);
                }
            });
        }
    }
    
    // Utilitários de data
    getMonday(date) {
        const d = new Date(date);
        const day = d.getDay();
        const diff = d.getDate() - day + (day === 0 ? -6 : 1);
        return new Date(d.setDate(diff));
    }
    
    formatDate(date, format = 'dd/MM') {
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const year = date.getFullYear();
        
        switch (format) {
            case 'dd/MM':
                return `${day}/${month}`;
            case 'dd/MM/yyyy':
                return `${day}/${month}/${year}`;
            case 'MMMM dd':
                return date.toLocaleDateString('pt-PT', { month: 'long', day: 'numeric' });
            case 'MMMM yyyy':
                return date.toLocaleDateString('pt-PT', { month: 'long', year: 'numeric' });
            default:
                return date.toLocaleDateString('pt-PT');
        }
    }
    
    isToday(date) {
        const today = new Date();
        return date.getDate() === today.getDate() &&
               date.getMonth() === today.getMonth() &&
               date.getFullYear() === today.getFullYear();
    }
    
    isSunday(date) {
        return date.getDay() === 0;
    }
    
    isHoliday(date) {
        const feriadosFixos = [
            "01-01", // Ano Novo
            "04-25", // Dia da Liberdade
            "05-01", // Dia do Trabalhador
            "06-10", // Dia de Portugal
            "08-15", // Assunção de Nossa Senhora
            "10-05", // Implantação da República
            "11-01", // Todos os Santos
            "12-01", // Restauração da Independência
            "12-08", // Imaculada Conceição
            "12-25"  // Natal
        ];
        
        const dateStr = `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
        
        return feriadosFixos.includes(dateStr);
    }
    
    isPast(date) {
        return date < new Date();
    }
    
    atualizarLabel() {
        let texto = '';

        switch (this.modoAtual) {
            case 'SEMANA':
                const inicio = this.semanaAtual.getDate();
                const fimSemana = new Date(this.semanaAtual.getTime() + 6 * 24 * 60 * 60 * 1000);
                const fim = fimSemana.getDate();
            
                if (this.semanaAtual.getMonth() === fimSemana.getMonth()) {
                    const mes = this.semanaAtual.toLocaleDateString('pt-PT', { month: 'long' });
                    texto = `${inicio} de ${mes} - ${fim} de ${mes}`;
                } else {
                    const mesInicio = this.semanaAtual.toLocaleDateString('pt-PT', { month: 'long' });
                    const mesFim = fimSemana.toLocaleDateString('pt-PT', { month: 'long' });
                    texto = `${inicio} de ${mesInicio} - ${fim} de ${mesFim}`;
                }
                break;
            
            case 'MES':
                texto = this.semanaAtual.toLocaleDateString('pt-PT', { 
                    month: 'long', 
                    year: 'numeric' 
                });
                break;
            
            case 'DIA':
                texto = this.diaSelecionado.toLocaleDateString('pt-PT', {
                    weekday: 'long',
                    day: 'numeric',
                    month: 'long'
                });
                break;
        }

        this.semanaLabel.textContent = texto;
    }
    
    // Helpers para marcações
    getMarcacao(dataHora) {
        const key = dataHora.toISOString();
        return this.marcacoes[key] || null;
    }
    
    // Ações de placeholder (implementar conforme necessário)
    criarMarcacao(dataHora) {
        console.log('Criar marcação para:', dataHora);
        // TODO: Implementar criação de marcação
    }
    
    abrirDetalheMarcacao(marcacao) {
        console.log('Abrir detalhe da marcação:', marcacao);
        // TODO: Implementar detalhe da marcação
    }
    
    abrirDetalheCliente(cliente) {
        // aceita cliente (objeto) ou nome (string)
        const nome = (cliente && cliente.nome) ? cliente.nome : cliente;

        // obter dados atualizados do backend se possível
        const api = this.api || ((window.pywebview && window.pywebview.api) || (typeof pywebview !== 'undefined' && pywebview && pywebview.api) || null);

        const abrirModalComDados = (clienteObj) => {
            // cria overlay/modal idempotente
            if (document.getElementById('detalhe-overlay')) return;
            const overlay = document.createElement('div');
            overlay.id = 'detalhe-overlay';
            overlay.style.position = 'fixed';
            overlay.style.left = 0;
            overlay.style.top = 0;
            overlay.style.right = 0;
            overlay.style.bottom = 0;
            overlay.style.background = 'rgba(0,0,0,0.6)';
            overlay.style.display = 'flex';
            overlay.style.alignItems = 'center';
            overlay.style.justifyContent = 'center';
            overlay.style.zIndex = 9999;

            const modal = document.createElement('div');
            modal.style.width = '760px';
            modal.style.maxWidth = '95%';
            modal.style.background = 'rgb(15,14,14)';
            modal.style.borderRadius = '12px';
            modal.style.padding = '16px';
            modal.style.boxShadow = '0 8px 30px rgba(0,0,0,0.6)';
            modal.style.color = 'white';
            modal.style.display = 'flex';
            modal.style.flexDirection = 'column';
            modal.style.gap = '12px';
            overlay.appendChild(modal);

            // Top row (apenas Editar à esquerda)
            const topRow = document.createElement('div');
            topRow.style.display = 'flex';
            topRow.style.alignItems = 'center';
            topRow.style.gap = '8px';

            const btnEditar = document.createElement('button');
            btnEditar.textContent = 'Editar';
            btnEditar.className = 'btn small';
            btnEditar.style.cursor = 'pointer';
            btnEditar.style.background = 'rgb(60,60,60)';
            btnEditar.style.border = 'none';
            btnEditar.style.color = 'white';
            btnEditar.style.padding = '10px 16px';
            btnEditar.style.borderRadius = '8px';
            btnEditar.style.fontWeight = '700';

            const spacer = document.createElement('div');
            spacer.style.flex = '1';

            topRow.appendChild(btnEditar);
            topRow.appendChild(spacer);
            modal.appendChild(topRow);

            // Visual box
            const visualBox = document.createElement('div');
            visualBox.style.background = 'rgb(43,40,40)';
            visualBox.style.borderRadius = '12px';
            visualBox.style.padding = '16px';
            visualBox.style.display = 'grid';
            visualBox.style.gridTemplateColumns = '160px 1fr';
            visualBox.style.gap = '12px';
            modal.appendChild(visualBox);

            const addRow = (title, val) => {
                const th = document.createElement('div');
                th.style.background = 'rgba(197,130,63,0.86)';
                th.style.color = 'white';
                th.style.padding = '12px';
                th.style.borderRadius = '12px';
                th.style.fontWeight = '700';
                th.textContent = title;
                const vv = document.createElement('div');
                vv.style.background = 'rgb(60,60,60)';
                vv.style.padding = '12px';
                vv.style.borderRadius = '12px';
                vv.style.color = 'white';
                vv.textContent = val || '—';
                visualBox.appendChild(th);
                visualBox.appendChild(vv);
            };

            addRow('Nome', clienteObj.nome);
            addRow('Telefone', clienteObj.numeroTelefone);
            addRow('Tipo', clienteObj.tipoCliente);
            if (clienteObj.tipoCliente === 'SEMANAL') {
                addRow('Dia da Semana', clienteObj.diaSemana || '—');
                addRow('Hora Corte', clienteObj.horaCorte || '—');
            }
            addRow('Rápido', (clienteObj.rapido === true || clienteObj.rapido === 'true') ? 'Sim' : 'Não');
            addRow('Faltas', (clienteObj.faltas||0).toString());

            // Edit box (hidden by default)
            const editBox = document.createElement('div');
            editBox.style.display = 'none';
            editBox.style.flexDirection = 'column';
            editBox.style.gap = '12px';
            editBox.style.marginTop = '8px';
            editBox.style.alignItems = 'center';
            editBox.style.width = '100%';

            // semanal checkbox
            const semanalRow = document.createElement('label');
            semanalRow.style.display = 'flex';
            semanalRow.style.alignItems = 'center';
            semanalRow.style.gap = '10px';
            semanalRow.style.justifyContent = 'center';
            semanalRow.style.width = '100%';
            const semanalCheck = document.createElement('input');
            semanalCheck.type = 'checkbox';
            semanalCheck.style.transform = 'scale(1.5)';
            semanalCheck.style.margin = '0';
            const semanalLabel = document.createElement('span');
            semanalLabel.textContent = 'Cliente Semanal';
            semanalLabel.style.color = 'white';
            semanalLabel.style.fontWeight = '800';
            semanalRow.appendChild(semanalCheck);
            semanalRow.appendChild(semanalLabel);
            editBox.appendChild(semanalRow);

            // helper to make field         
            const mkField = (labelText) => {
                const wrapper = document.createElement('div');
                wrapper.style.display = 'flex';
                wrapper.style.flexDirection = 'column';
                wrapper.style.gap = '6px';
                wrapper.style.alignItems = 'center';
                wrapper.style.width = '100%';

                const label = document.createElement('label');
                label.textContent = labelText;
                label.style.color = 'white';
                label.style.fontSize = '16px'; // igual aos outros labels
                label.style.textAlign = 'center';
                label.style.fontWeight = '700';

                const input = document.createElement('input');
                input.type = 'text';
                input.style.padding = '6px 12px';
                input.style.border = 'none';
                input.style.borderRadius = '12px'; // igual aos selects
                input.style.width = '320px';
                input.style.maxWidth = '80%';
                input.style.height = '34px'; // igual às selects
                input.style.boxSizing = 'border-box';
                input.style.background = 'white';
                input.style.color = 'black';
                input.style.fontSize = '16px'; // combina visualmente com selects

                wrapper.appendChild(label);
                wrapper.appendChild(input);
                return { wrapper, input };
            };

            const nomeF = mkField('Nome:');
            nomeF.wrapper.querySelector('label').style.fontWeight = '700';
            nomeF.input.style.fontSize = '16px';
            const telF = mkField('Telefone:');
            telF.wrapper.querySelector('label').style.fontWeight = '700';
            telF.input.style.fontSize = '16px';

            // dia select
            const diaWrapper = document.createElement('div');
            diaWrapper.style.display = 'flex';
            diaWrapper.style.flexDirection = 'column';
            diaWrapper.style.alignItems = 'center';
            diaWrapper.style.width = '100%';
            const diaLabel = document.createElement('label');
            diaLabel.textContent = 'Dia da Semana:';
            diaLabel.style.color = 'white';
            diaLabel.style.fontWeight = '700';
            diaLabel.style.fontSize = '16px';
            diaLabel.style.textAlign = 'center';
            const diaSelect = document.createElement('select');
            ['','Segunda','Terça','Quarta','Quinta','Sexta','Sábado','Domingo'].forEach(v => {
                const o = document.createElement('option'); o.value = v; o.textContent = v || '--'; diaSelect.appendChild(o);
            });
            diaSelect.style.padding = '6px 10px';
            diaSelect.style.borderRadius = '12px';
            diaSelect.style.width = '320px';
            diaSelect.style.maxWidth = '80%';
            diaSelect.style.height = '34px';
            diaWrapper.appendChild(diaLabel);
            diaWrapper.appendChild(diaSelect);

            // hora select
            const horaWrapper = document.createElement('div');
            horaWrapper.style.display = 'flex';
            horaWrapper.style.flexDirection = 'column';
            horaWrapper.style.alignItems = 'center';
            horaWrapper.style.width = '100%';
            const horaLabel = document.createElement('label');
            horaLabel.textContent = 'Hora do Corte:';
            horaLabel.style.color = 'white';
            horaLabel.style.fontWeight = '700';
            horaLabel.style.fontSize = '16px';
            horaLabel.style.textAlign = 'center';
            const horaSelect = document.createElement('select');
            horaSelect.style.padding = '6px 10px';
            horaSelect.style.borderRadius = '12px';
            horaSelect.style.width = '320px';
            horaSelect.style.maxWidth = '80%';
            horaSelect.style.height = '34px';
            horaWrapper.appendChild(horaLabel);
            horaWrapper.appendChild(horaSelect);

            // faltas controls
            const faltasRow = document.createElement('div');
            faltasRow.style.display = 'flex';
            faltasRow.style.flexDirection = 'column';
            faltasRow.style.alignItems = 'center';
            faltasRow.style.gap = '8px';
            faltasRow.style.width = '100%';

            const faltasLabelRow = document.createElement('div');
            faltasLabelRow.style.display = 'flex';
            faltasLabelRow.style.alignItems = 'center';
            faltasLabelRow.style.gap = '8px';
            faltasLabelRow.style.justifyContent = 'center';

            const faltasText = document.createElement('div');
            faltasText.textContent = 'Faltas:';
            faltasText.style.color = 'white';
            faltasText.style.fontWeight = '700';
            faltasText.style.fontSize = '16px';

            const menosBtn = document.createElement('button');
            menosBtn.textContent = '-';
            menosBtn.className = 'btn small neutral';
            menosBtn.style.padding = '6px 10px';
            menosBtn.style.minWidth = '30px';
            menosBtn.style.height = '30px';
            menosBtn.style.fontWeight = '700';

            const faltasLabel = document.createElement('div');
            faltasLabel.textContent = (clienteObj.faltas||0).toString();
            faltasLabel.style.minWidth = '36px';
            faltasLabel.style.textAlign = 'center';
            faltasLabel.style.color = 'white';
            faltasLabel.style.fontSize = '14px';

            const maisBtn = document.createElement('button');
            maisBtn.textContent = '+';
            maisBtn.className = 'btn small neutral';
            maisBtn.style.padding = '6px 10px';
            maisBtn.style.minWidth = '30px';
            maisBtn.style.height = '30px';
            maisBtn.style.fontWeight = '700';

            faltasLabelRow.appendChild(faltasText);
            faltasLabelRow.appendChild(menosBtn);
            faltasLabelRow.appendChild(faltasLabel);
            faltasLabelRow.appendChild(maisBtn);

            const sep = document.createElement('div');
            sep.style.height = '0';
            faltasRow.appendChild(sep);
            faltasRow.appendChild(faltasLabelRow);

            // Corte rápido (abaixo das faltas)
            const rapidoRow = document.createElement('label');
            rapidoRow.style.display = 'flex';
            rapidoRow.style.alignItems = 'center';
            rapidoRow.style.gap = '10px';
            rapidoRow.style.justifyContent = 'center';
            rapidoRow.style.width = '100%';
            const rapidoCheck = document.createElement('input');
            rapidoCheck.type = 'checkbox';
            rapidoCheck.style.transform = 'scale(1.5)';
            rapidoCheck.style.margin = '0';
            const rapidoLabel = document.createElement('span');
            rapidoLabel.textContent = 'Corte Rápido';
            rapidoLabel.style.color = 'white';
            rapidoLabel.style.fontWeight = '800';
            rapidoRow.appendChild(rapidoCheck);
            rapidoRow.appendChild(rapidoLabel);

            // montar editBox
            editBox.appendChild(nomeF.wrapper);
            editBox.appendChild(telF.wrapper);
            editBox.appendChild(diaWrapper);
            editBox.appendChild(horaWrapper);
            editBox.appendChild(faltasRow);
            editBox.appendChild(rapidoRow);

            modal.appendChild(editBox);

            // estado inicial popula visual e campos
            nomeF.input.value = clienteObj.nome || '';
            telF.input.value = clienteObj.numeroTelefone || '';
            semanalCheck.checked = (clienteObj.tipoCliente === 'SEMANAL');
            diaSelect.value = clienteObj.diaSemana || '';
            horaSelect.value = clienteObj.horaCorte || '';
            rapidoCheck.checked = (clienteObj.rapido === true || clienteObj.rapido === 'true');
            faltasLabel.textContent = (clienteObj.faltas||0).toString();

            // popular horas função
            const timeToMinutes = (hhmm) => {
                if (!hhmm) return null;
                const parts = String(hhmm).split(':');
                const h = parseInt(parts[0], 10);
                const m = parseInt(parts[1], 10);
                if (isNaN(h) || isNaN(m)) return null;
                return h * 60 + m;
            };
            const minutesToTime = (min) => {
                const h = Math.floor(min / 60);
                const m = min % 60;
                return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
            };

            const gerarHoras = (step = 30) => {
                const list = [];
                for (let h = 7; h <= 21; h++) {
                    for (let m = 0; m < 60; m += step) {
                        if (h === 21 && m > 0) continue; // não permitir depois das 21:00
                        list.push(`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`);
                    }
                }
                return list;
            };

            // popula horas com filtro de conflitos para o dia seleccionado
            const populateHorasBasedOnState = (stepOverride = null) => {
                const step = stepOverride || (rapidoCheck.checked ? 15 : 30);
                horaSelect.innerHTML = '';
                const placeholder = document.createElement('option'); placeholder.value=''; placeholder.textContent='--'; horaSelect.appendChild(placeholder);

                // dia seleccionado (normalizar)
                const diaEscolhido = diaSelect.value || null;

                // construir set de segmentos ocupados (em minutos) por outros clientes semanais no mesmo dia
                const ocupados = new Set();
                Object.values(this.clientes || {}).forEach(c => {
                    try {
                        if (!c) return;
                        if (String(c.tipoCliente).toUpperCase() !== 'SEMANAL') return;
                        if (!c.diaSemana || !c.horaCorte) return;
                        // ignorar o próprio cliente em edição
                        if (c.nome === clienteObj.nome) return;
                        if (!diaEscolhido) return; // sem dia escolhido, não marcar ocupados
                        if (String(c.diaSemana).toLowerCase() !== String(diaEscolhido).toLowerCase()) return;
                        const start = timeToMinutes(c.horaCorte);
                        if (start === null) return;
                        const dur = (c.rapido === true || c.rapido === 'true') ? 15 : 30;
                        for (let t = start; t < start + dur; t += 15) ocupados.add(t);
                    } catch (err) { /* ignore malformed cliente */ }
                });

                gerarHoras(step).forEach(h => {
                    const minutos = timeToMinutes(h);
                    let permitido = false;
                    if (!minutos && minutos !== 0) return;

                    if (step === 15) {
                        // candidato rápido: apenas verificar o segmento de 15min
                        permitido = !ocupados.has(minutos);
                    } else {
                        // candidato 30min: precisa de dois segmentos livres (minutos e minutos+15)
                        const nextSeg = minutos + 15;
                        if (nextSeg > 21 * 60) {
                            permitido = false;
                        } else {
                            permitido = !ocupados.has(minutos) && !ocupados.has(nextSeg);
                        }
                    }

                    if (permitido) {
                        const o = document.createElement('option');
                        o.value = h; o.textContent = h;
                        horaSelect.appendChild(o);
                    }
                });

                // manter valor actual se ainda existir, senão limpar
                if (clienteObj.horaCorte && Array.from(horaSelect.options).some(opt => opt.value === clienteObj.horaCorte)) {
                    horaSelect.value = clienteObj.horaCorte;
                } else {
                    horaSelect.value = '';
                }
            };

            // estado semanal habilita/desabilita selects e popula horas conforme rapido
            const updateSemanalState = () => {
                const s = semanalCheck.checked;
                diaSelect.disabled = !s;
                horaSelect.disabled = !s;
                if (s) {
                    // populamos com o step actual (rapido ou não)
                    populateHorasBasedOnState();
                } else {
                    // limpar horas quando não semanal
                    horaSelect.innerHTML = '';
                    const placeholder = document.createElement('option'); placeholder.value=''; placeholder.textContent='--'; horaSelect.appendChild(placeholder);
                }
            };

            // listeners para repopular quando dia ou rapido mudam
            diaSelect.addEventListener('change', () => populateHorasBasedOnState());
            rapidoCheck.addEventListener('change', () => populateHorasBasedOnState());
            semanalCheck.addEventListener('change', () => updateSemanalState());

            // inicializa
            updateSemanalState();

            // bottom action buttons (único botão "Sair" aqui)
            const bottomActions = document.createElement('div');
            bottomActions.style.display = 'flex';
            bottomActions.style.justifyContent = 'flex-end';
            bottomActions.style.gap = '8px';
            bottomActions.style.marginTop = 'auto';

            const apagarBottom = document.createElement('button');
            apagarBottom.type = 'button';
            apagarBottom.textContent = 'Apagar';
            apagarBottom.className = 'btn danger';
            apagarBottom.style.background = 'rgb(128,26,15)';
            apagarBottom.style.color = 'white';
            apagarBottom.style.border = 'none';
            apagarBottom.style.padding = '12px 18px';
            apagarBottom.style.borderRadius = '8px';
            apagarBottom.style.cursor = 'pointer';
            apagarBottom.style.fontWeight = '700';
            apagarBottom.style.display = 'inline-block';

            const salvarBottom = document.createElement('button');
            salvarBottom.type = 'button';
            salvarBottom.textContent = 'Salvar';
            salvarBottom.className = 'btn primary';
            salvarBottom.style.background = 'rgb(36,43,141)';
            salvarBottom.style.color = 'white';
            salvarBottom.style.border = 'none';
            salvarBottom.style.padding = '12px 18px';
            salvarBottom.style.borderRadius = '8px';
            salvarBottom.style.cursor = 'pointer';
            salvarBottom.style.fontWeight = '700';
            salvarBottom.style.display = 'none';

            const sairBottom = document.createElement('button');
            sairBottom.type = 'button';
            sairBottom.textContent = 'Sair';
            sairBottom.className = 'btn neutral';
            sairBottom.style.background = 'rgb(60,60,60)';
            sairBottom.style.color = 'white';
            sairBottom.style.border = 'none';
            sairBottom.style.padding = '12px 18px';
            sairBottom.style.borderRadius = '8px';
            sairBottom.style.cursor = 'pointer';
            sairBottom.style.fontWeight = '700';

            bottomActions.appendChild(apagarBottom);
            bottomActions.appendChild(salvarBottom);
            bottomActions.appendChild(sairBottom);
            modal.appendChild(bottomActions);
            modal.style.minHeight = modal.style.minHeight || '260px';

            // Função de fechar modal (declarada antes dos listeners)
            const closeModal = () => {
                try {
                    if (document.body.contains(overlay)) document.body.removeChild(overlay);
                } catch (err) { /* ignore */ }
                window.removeEventListener('keydown', keyHandler, true);
            };

            // listeners básicos
            sairBottom.addEventListener('click', (ev) => { ev.preventDefault(); ev.stopPropagation(); closeModal(); });

            // btnEditar agora faz toggle entre modo visual e modo edição
            btnEditar.addEventListener('click', () => {
                const isEditing = editBox.style.display !== 'none';
                if (isEditing) {
                    // passar para vista
                    editBox.style.display = 'none';
                    visualBox.style.display = 'grid';
                    salvarBottom.style.display = 'none';
                    apagarBottom.style.display = 'inline-block';
                    btnEditar.textContent = 'Editar';
                } else {
                    // passar para edição
                    visualBox.style.display = 'none';
                    editBox.style.display = 'flex';
                    salvarBottom.style.display = 'inline-block';
                    apagarBottom.style.display = 'none';
                    btnEditar.textContent = 'Editar';
                    setTimeout(() => nomeF.input.focus(), 50);
                }
            });

            menosBtn.addEventListener('click', () => {
                let v = parseInt(faltasLabel.textContent||'0',10); v = Math.max(0, v-1); faltasLabel.textContent = String(v);
            });
            maisBtn.addEventListener('click', () => {
                let v = parseInt(faltasLabel.textContent||'0',10); v++; faltasLabel.textContent = String(v);
            });

            semanalCheck.addEventListener('change', () => {
                updateSemanalState();
            });

            // helper: confirmação customizada (resolve true = Sim, false = Não)
            const showConfirm = (mensagem) => {
                return new Promise(resolve => {
                    const confirmOverlay = document.createElement('div');
                    confirmOverlay.style.position = 'absolute';
                    confirmOverlay.style.left = 0;
                    confirmOverlay.style.top = 0;
                    confirmOverlay.style.right = 0;
                    confirmOverlay.style.bottom = 0;
                    confirmOverlay.style.display = 'flex';
                    confirmOverlay.style.alignItems = 'center';
                    confirmOverlay.style.justifyContent = 'center';
                    confirmOverlay.style.zIndex = 99999;

                    const dialog = document.createElement('div');
                    dialog.style.minWidth = '420px';
                    dialog.style.maxWidth = '90%';
                    dialog.style.background = 'rgb(20,19,19)';
                    dialog.style.borderRadius = '8px';
                    dialog.style.padding = '18px';
                    dialog.style.boxShadow = '0 8px 30px rgba(0,0,0,0.6)';
                    dialog.style.color = 'white';
                    dialog.style.textAlign = 'center';

                    const msg = document.createElement('div');
                    msg.textContent = mensagem;
                    msg.style.marginBottom = '16px';
                    msg.style.fontSize = '16px';
                    msg.style.color = '#ddd';
                    dialog.appendChild(msg);

                    const btnRow = document.createElement('div');
                    btnRow.style.display = 'flex';
                    btnRow.style.justifyContent = 'center';
                    btnRow.style.gap = '12px';

                    const naoBtn = document.createElement('button');
                    naoBtn.type = 'button';
                    naoBtn.textContent = 'Não';
                    naoBtn.style.padding = '8px 16px';
                    naoBtn.style.borderRadius = '8px';
                    naoBtn.style.border = 'none';
                    naoBtn.style.background = 'rgb(96,96,96)';
                    naoBtn.style.color = 'white';
                    naoBtn.style.cursor = 'pointer';
                    naoBtn.style.fontWeight = '700';

                    const simBtn = document.createElement('button');
                    simBtn.type = 'button';
                    simBtn.textContent = 'Sim';
                    simBtn.style.padding = '8px 16px';
                    simBtn.style.borderRadius = '8px';
                    simBtn.style.border = 'none';
                    simBtn.style.background = 'rgb(128,26,15)';
                    simBtn.style.color = 'white';
                    simBtn.style.cursor = 'pointer';
                    simBtn.style.fontWeight = '700';

                    btnRow.appendChild(naoBtn);
                    btnRow.appendChild(simBtn);
                    dialog.appendChild(btnRow);
                    confirmOverlay.appendChild(dialog);

                    // inserir dentro do overlay principal para manter hierarquia e captura
                    overlay.appendChild(confirmOverlay);

                    const cleanup = () => {
                        try { if (overlay.contains(confirmOverlay)) overlay.removeChild(confirmOverlay); } catch(_) {}
                    };

                    naoBtn.addEventListener('click', () => { cleanup(); resolve(false); });
                    simBtn.addEventListener('click', () => { cleanup(); resolve(true); });

                    // clicar fora do dialog = Não
                    confirmOverlay.addEventListener('click', (e) => {
                        if (e.target === confirmOverlay) { cleanup(); resolve(false); }
                    });
                });
            };

            // apagar handler (bottom visual)
            apagarBottom.addEventListener('click', async (ev) => {
                ev.preventDefault();
                ev.stopPropagation();
                const ok = await showConfirm('Deseja apagar o Cliente? Esta ação é irreversível');
                if (!ok) return;
                try {
                    if (!api) throw new Error('API indisponível');
                    const res = await api.apagar_cliente(clienteObj.nome);
                    if (res && res.success) {
                        await this.carregarDados();
                        this.atualizarAreaClientes();
                        closeModal();
                    } else {
                        alert(res && res.error ? res.error : 'Erro ao apagar cliente.');
                    }
                } catch (err) {
                    console.error(err);
                    alert('Erro ao apagar cliente.');
                }
            });

            // salvar usando o botão do rodapé (com verificação de conflitos para cliente SEMANAL)
            salvarBottom.addEventListener('click', async () => {
                // validações básicas
                const novoNome = nomeF.input.value.trim();
                const novoTel = telF.input.value.trim();
                const tipo = semanalCheck.checked ? 'SEMANAL' : 'NORMAL';
                const dia = semanalCheck.checked ? (diaSelect.value || null) : null;
                const hora = semanalCheck.checked ? (horaSelect.value || null) : null;
                const faltas = parseInt(faltasLabel.textContent||'0',10);
                const rapido = rapidoCheck.checked;

                if (!novoNome) { alert('Nome inválido'); return; }
                if (!/^\+?\d[\d\-\s()]{6,}$/.test(novoTel)) { alert('Telefone inválido'); return; }
                if (tipo === 'SEMANAL' && (!dia || !hora)) { alert('Selecione dia e hora para cliente semanal'); return; }

                // se for cliente semanal, verificar conflitos com outros clientes semanais
                if (tipo === 'SEMANAL') {
                    const timeToMinutes = (hhmm) => {
                        if (!hhmm) return null;
                        const parts = String(hhmm).split(':');
                        const h = parseInt(parts[0], 10);
                        const m = parseInt(parts[1], 10);
                        if (isNaN(h) || isNaN(m)) return null;
                        return h * 60 + m;
                    };

                    const ocupados = new Set();
                    Object.values(this.clientes).forEach(c => {
                        try {
                            if (!c) return;
                            // só considerar clientes semanais
                            if (String(c.tipoCliente).toUpperCase() !== 'SEMANAL') return;
                            if (!c.diaSemana) return;
                            // ignorar o próprio cliente que estamos a editar
                            if (c.nome === clienteObj.nome) return;
                            if (String(c.diaSemana).toLowerCase() !== String(dia).toLowerCase()) return;
                            const start = timeToMinutes(c.horaCorte);
                            if (start === null || isNaN(start)) return;
                            const dur = (c.rapido === true || c.rapido === 'true') ? 15 : 30;
                            for (let t = start; t < start + dur; t += 15) ocupados.add(t);
                        } catch (err) { /* ignore malformed cliente */ }
                    });

                    const candidatoStart = timeToMinutes(hora);
                    if (candidatoStart === null) { alert('Hora inválida'); return; }
                    const candidatoDur = rapido ? 15 : 30;

                    if (candidatoDur === 15) {
                        if (ocupados.has(candidatoStart)) {
                            alert('Conflito: já existe um cliente semanal nesse segmento rápido.');
                            return;
                        }
                    } else {
                        // 30min precisa de dois segmentos de 15min livres
                        const nextSeg = candidatoStart + 15;
                        if (nextSeg > 21 * 60) {
                            alert('Hora inválida (ultrapassa 21:00).'); return;
                        }
                        if (ocupados.has(candidatoStart) || ocupados.has(nextSeg)) {
                            alert('Conflito: já existe um cliente semanal nesse horário.');
                            return;
                        }
                    }
                }

                const payload = {
                    nomeOriginal: clienteObj.nome,
                    nome: novoNome,
                    numeroTelefone: novoTel,
                    tipoCliente: tipo,
                    diaSemana: dia,
                    horaCorte: hora,
                    faltas: faltas,
                    rapido: rapido
                };

                try {
                    if (!api) throw new Error('API indisponível');
                    const res = await api.alterar_cliente(payload);
                    if (res && res.success) {
                        await this.carregarDados();
                        this.atualizarAreaClientes();
                        closeModal();
                    } else {
                        alert(res && res.error ? res.error : 'Erro ao guardar cliente.');
                    }
                } catch (err) {
                    console.error(err);
                    alert('Erro ao guardar cliente.');
                }
            });
            
            // fechar ao clicar fora
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) {
                    e.preventDefault();
                    closeModal();
                }
            });

            // Handlers de teclado: Esc = Sair, Enter = Editar (quando em visual) / Salvar (quando em edição)
            const keyHandler = (e) => {
                // proteger: só actua se o overlay ainda existir
                if (!document.body.contains(overlay)) return;
                if (e.key === 'Escape') {
                    e.preventDefault();
                    e.stopImmediatePropagation();
                    closeModal();
                } else if (e.key === 'Enter') {
                    // quando em edição, salvar; quando em vista, alternar para edição
                    e.preventDefault();
                    e.stopImmediatePropagation();
                    e.stopPropagation();
                    try {
                        btnEditar.click();
                    } catch (err) {
                        // fallback: nada
                    }
                }
            };
            // usar window com capture para garantir que apanha ESC mesmo com inputs focados
            window.addEventListener('keydown', keyHandler, true);

            document.body.appendChild(overlay);
        };

        (async () => {
            try {
                if (api && typeof api.get_cliente === 'function') {
                    const resp = await api.get_cliente(nome);
                    if (resp && resp.success && resp.cliente) {
                        abrirModalComDados(resp.cliente);
                        return;
                    }
                }
            } catch (e) {
                // ignora e usa dados locais
                console.warn('Erro a obter cliente via API, a usar dados locais:', e);
            }
            // fallback para usar objeto local se disponível
            const local = (this.clientes && this.clientes[nome]) ? this.clientes[nome] : (typeof cliente === 'object' ? cliente : { nome: nome, numeroTelefone: '', tipoCliente: 'DESCONHECIDO', faltas:0, rapido:false });
            abrirModalComDados(local);
        })();
    }
    
    abrirGestaoPendentes() {
        console.log('Abrir gestão de pendentes');
        // TODO: Implementar gestão de pendentes
    }

    adicionarCliente() {
        const overlay = document.createElement('div');
        overlay.style.position = 'fixed';
        overlay.style.left = 0;
        overlay.style.top = 0;
        overlay.style.right = 0;
        overlay.style.bottom = 0;
        overlay.style.background = 'rgba(0,0,0,0.6)';
        overlay.style.display = 'flex';
        overlay.style.alignItems = 'center';
        overlay.style.justifyContent = 'center';
        overlay.style.zIndex = 9999;

        const modal = document.createElement('div');
        modal.style.width = '360px';
        modal.style.maxWidth = '90%';
        modal.style.background = 'rgb(15,14,14)';
        modal.style.borderRadius = '12px';
        modal.style.padding = '18px';
        modal.style.boxShadow = '0 6px 24px rgba(0,0,0,0.5)';
        modal.style.color = 'white';
        modal.style.display = 'flex';
        modal.style.flexDirection = 'column';
        modal.style.gap = '10px';
        overlay.appendChild(modal);

        // Header
        const headerRow = document.createElement('div');
        headerRow.style.display = 'flex';
        headerRow.style.justifyContent = 'center';
        headerRow.style.alignItems = 'center';
        headerRow.style.position = 'relative';
        headerRow.style.marginBottom = '4px';

        const title = document.createElement('h3');
        title.textContent = 'Adicionar Cliente';
        title.style.margin = '0';
        title.style.fontSize = '18px';
        title.style.textAlign = 'center';
        title.style.width = '100%';
        headerRow.appendChild(title);

        const closeX = document.createElement('button');
        closeX.textContent = '✕';
        closeX.style.position = 'absolute';
        closeX.style.right = '8px';
        closeX.style.top = '6px';
        closeX.style.background = 'transparent';
        closeX.style.border = 'none';
        closeX.style.color = 'white';
        closeX.style.cursor = 'pointer';
        closeX.style.fontSize = '16px';
        closeX.style.width = '28px';
        closeX.style.height = '28px';
        closeX.style.display = 'flex';
        closeX.style.alignItems = 'center';
        closeX.style.justifyContent = 'center';
        headerRow.appendChild(closeX);

        modal.appendChild(headerRow);

        // Semanal checkbox (logo abaixo do header)
        const semanalWrapper = document.createElement('div');
        semanalWrapper.style.display = 'flex';
        semanalWrapper.style.alignItems = 'center';
        semanalWrapper.style.gap = '10px';
        semanalWrapper.style.justifyContent = 'flex-start';
        const semanalCheck = document.createElement('input');
        semanalCheck.type = 'checkbox';
        semanalCheck.id = 'modal-semanal';
        semanalCheck.style.transform = 'scale(1.4)';
        semanalCheck.style.margin = '0';
        semanalCheck.style.width = '18px';
        semanalCheck.style.height = '18px';
        semanalCheck.style.accentColor = '#242b8d';
        const semanalLabel = document.createElement('label');
        semanalLabel.textContent = 'Cliente Semanal';
        semanalLabel.htmlFor = 'modal-semanal';
        semanalLabel.style.userSelect = 'none';
        semanalLabel.style.fontSize = '14px';
        semanalWrapper.appendChild(semanalCheck);
        semanalWrapper.appendChild(semanalLabel);
        modal.appendChild(semanalWrapper);

        // Helper para criar campos
        const makeField = (labelText, tag='input', type='text') => {
            const wrapper = document.createElement('div');
            wrapper.style.display = 'flex';
            wrapper.style.flexDirection = 'column';
            wrapper.style.gap = '6px';
            wrapper.style.alignItems = 'center';

            const label = document.createElement('label');
            label.textContent = labelText;
            label.style.fontSize = '13px';
            label.style.color = 'white';
            label.style.alignItems = 'flex-start';
            wrapper.appendChild(label);

            let field;
            if (tag === 'input') {
                field = document.createElement('input');
                field.type = type;
                field.style.padding = '6px 8px';
                field.style.height = '32px';
                field.style.width = '280px';
                field.style.borderRadius = '8px';
                field.style.border = 'none';
                field.style.boxSizing = 'border-box';
                field.style.background = 'white';
                field.style.color = 'black';
                field.style.fontSize = '13px';
            } else {
                field = document.createElement('select');
                field.style.padding = '6px 8px';
                field.style.height = '34px';
                field.style.width = '280px';
                field.style.borderRadius = '8px';
                field.style.border = 'none';
                field.style.background = 'white';
                field.style.color = 'black';
                field.style.fontSize = '13px';
                field.style.setProperty('color', 'black', 'important');
            }
            wrapper.appendChild(field);
            return { wrapper, field };
        };

        const nomeF = makeField('Nome', 'input', 'text');
        const telefoneF = makeField('Telefone', 'input', 'text');
        const diaF = makeField('Dia da Semana', 'select');
        const horaF = makeField('Hora do Corte', 'select');

        // preencher dia da semana (pt)
        const dias = ['Segunda','Terça','Quarta','Quinta','Sexta','Sábado','Domingo'];
        dias.forEach(d => {
            const opt = document.createElement('option');
            opt.value = d;
            opt.textContent = d;
            opt.style.color = 'black';
            diaF.field.appendChild(opt);
        });
        // placeholder option para dia/hora
        const diaPlaceholder = document.createElement('option');
        diaPlaceholder.value = '';
        diaPlaceholder.textContent = 'Dia da Semana';
        diaPlaceholder.selected = true;
        diaPlaceholder.disabled = true;
        diaPlaceholder.style.color = '#888';
        diaF.field.insertBefore(diaPlaceholder, diaF.field.firstChild);

        const horaPlaceholder = document.createElement('option');
        horaPlaceholder.value = '';
        horaPlaceholder.textContent = 'Hora do Corte';
        horaPlaceholder.selected = true;
        horaPlaceholder.disabled = true;
        horaPlaceholder.style.color = '#888';
        horaF.field.appendChild(horaPlaceholder);

        // Corte rápido
        const rapidoWrapper = document.createElement('div');
        rapidoWrapper.style.display = 'flex';
        rapidoWrapper.style.alignItems = 'center';
        rapidoWrapper.style.gap = '10px';
        rapidoWrapper.style.justifyContent = 'flex-start';
        const rapidoCheck = document.createElement('input');
        rapidoCheck.type = 'checkbox';
        rapidoCheck.style.transform = 'scale(1.4)';
        rapidoCheck.style.margin = '0';
        rapidoCheck.style.width = '18px';
        rapidoCheck.style.height = '18px';
        rapidoCheck.style.accentColor = '#242b8d';
        const rapidoLabel = document.createElement('label');
        rapidoLabel.textContent = 'Corte Rápido';
        rapidoLabel.style.userSelect = 'none';
        rapidoLabel.style.fontSize = '14px';
        rapidoWrapper.appendChild(rapidoCheck);
        rapidoWrapper.appendChild(rapidoLabel);

        // Erro
        const errorEl = document.createElement('div');
        errorEl.style.color = '#ff8080';
        errorEl.style.minHeight = '18px';
        errorEl.style.fontSize = '13px';
        errorEl.style.textAlign = 'center';

        const buttons = document.createElement('div');
        buttons.style.display = 'flex';
        buttons.style.justifyContent = 'flex-end';
        buttons.style.gap = '12px';
        buttons.style.marginTop = '6px';

        const salvarBtn = document.createElement('button');
        salvarBtn.textContent = 'Salvar';
        salvarBtn.style.padding = '10px 16px';
        salvarBtn.style.borderRadius = '8px';
        salvarBtn.style.background = 'rgb(36,43,141)';
        salvarBtn.style.color = 'white';
        salvarBtn.style.border = 'none';
        salvarBtn.style.cursor = 'pointer';
        salvarBtn.style.fontWeight = '700';

        const sairBtn = document.createElement('button');
        sairBtn.textContent = 'Sair';
        sairBtn.style.padding = '10px 16px';
        sairBtn.style.borderRadius = '8px';
        sairBtn.style.background = 'rgb(60,60,60)';
        sairBtn.style.color = 'white';
        sairBtn.style.border = 'none';
        sairBtn.style.cursor = 'pointer';
        sairBtn.style.fontWeight = '700';

        buttons.appendChild(salvarBtn);
        buttons.appendChild(sairBtn);

        // Montar modal
        modal.appendChild(nomeF.wrapper);
        modal.appendChild(telefoneF.wrapper);
        modal.appendChild(diaF.wrapper);
        modal.appendChild(horaF.wrapper);
        modal.appendChild(rapidoWrapper);
        modal.appendChild(errorEl);
        modal.appendChild(buttons);

        // Estado inicial: dia/hora/rapido desabilitados
        const setDisabledStyle = (el, disabled) => {
            el.disabled = disabled;
            if (disabled) {
                el.style.opacity = '0.5';
                el.style.pointerEvents = 'none';
            } else {
                el.style.opacity = '1';
                el.style.pointerEvents = 'auto';
            }
        };
        setDisabledStyle(diaF.field, true);
        setDisabledStyle(horaF.field, true);
        rapidoCheck.disabled = true;

        // Função de fechar modal (remove listener de teclas registado no window)
        const closeModal = () => {
            if (document.body.contains(overlay)) document.body.removeChild(overlay);
            window.removeEventListener('keydown', keyHandler, true);
        };

        // Handlers de teclado: Esc = Sair, Enter = Editar (quando em visual) / Salvar (quando em edição)
        const keyHandler = (e) => {
            if (e.key === 'Escape') {
                e.preventDefault();
                closeModal();
            } else if (e.key === 'Enter') {
                // em adicionarCliente Enter sempre salva
                e.preventDefault();
                salvarBtn.click();
            }
        };
        window.addEventListener('keydown', keyHandler, true);

        // Habilitar/desabilitar quando checkbox semanal mudar
        semanalCheck.addEventListener('change', () => {
            const ativo = semanalCheck.checked;
            setDisabledStyle(diaF.field, !ativo);
            // hora fica desativada até escolher dia
            if (!ativo) {
                setDisabledStyle(horaF.field, true);
                rapidoCheck.disabled = true;
                rapidoCheck.checked = false;
            } else {
                rapidoCheck.disabled = false;
            }
            // limpar select de horas
            horaF.field.innerHTML = '';
            horaF.field.appendChild(horaPlaceholder);
        });

        // Atualizar opções de hora ao mudar o estado do "rápido"
        rapidoCheck.addEventListener('change', () => {
            // se já foi escolhido um dia, re-popular as horas com o novo step
            if (!diaF.field.disabled && diaF.field.value) {
                diaF.field.dispatchEvent(new Event('change'));
            }
        });

        // gerar horas de 07:00 a 21:00 step 30min
        const timeToMinutes = (hhmm) => {
            if (!hhmm) return null;
            const parts = hhmm.split(':');
            return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10);
        };

        const minutesToTime = (min) => {
            const h = Math.floor(min / 60);
            const m = min % 60;
            return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        };

        const gerarHoras = (step = 30) => {
            const horas = [];
            for (let h = 7; h <= 21; h++) {
                for (let m = 0; m < 60; m += step) {
                    // não permitir 21:30, 21:15, etc. só até 21:00
                    if (h === 21 && m > 0) continue;
                    horas.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
                }
            }
            return horas;
        };

        // Ao escolher dia, popular horas disponíveis (exclui horários já ocupados por clientes semanais)
        diaF.field.addEventListener('change', () => {
            const diaEscolhido = diaF.field.value;
            // calcular ocupados por segmentos de 15 minutos (minutos desde meia-noite)
            const ocupados = new Set();
            Object.values(this.clientes).forEach(c => {
                try {
                    if (!c) return;
                    // só considerar clientes semanais com dia definido
                    if (c.tipoCliente !== 'SEMANAL' && c.tipoCliente !== 'SEMANAL') return;
                    if (!c.diaSemana) return;
                    // comparar dia (normalizar)
                    if (String(c.diaSemana).toLowerCase() !== String(diaEscolhido).toLowerCase()) return;

                    const start = timeToMinutes(c.horaCorte);
                    if (start === null || isNaN(start)) return;
                    const dur = (c.rapido === true || c.rapido === 'true') ? 15 : 30;
                    for (let t = start; t < start + dur; t += 15) {
                        ocupados.add(t);
                    }
                } catch (err) {
                    // ignorar cliente inválido
                }
            });

            // popular opções de hora com base no estado do checkbox rapido
            horaF.field.innerHTML = '';
            horaF.field.appendChild(horaPlaceholder);

            const step = (semanalCheck.checked && rapidoCheck.checked) ? 15 : 30;
            gerarHoras(step).forEach(h => {
                const minutos = timeToMinutes(h);
                let permitido = false;

                if (step === 15) {
                    // candidato rápido: basta o segmento de 15min estar livre
                    permitido = !ocupados.has(minutos);
                } else {
                    // candidato normal (30min): só mostrar :00/:30 e ambos os 15min segments têm de estar livres
                    const nextSegment = minutos + 15;
                    // não mostrar se ultrapassa 21:00
                    if (nextSegment > 21 * 60) {
                        permitido = false;
                    } else {
                        permitido = !ocupados.has(minutos) && !ocupados.has(nextSegment);
                    }
                    // garantir que só aparecem em :00/:30 (já garantido pelo step=30)
                }

                if (permitido) {
                    const opt = document.createElement('option');
                    opt.value = h;
                    opt.textContent = h;
                    horaF.field.appendChild(opt);
                }
            });

            // forçar cor do select (alguns engines mantêm cor branca)
            horaF.field.style.setProperty('color', 'black', 'important');

            setDisabledStyle(horaF.field, false);
            // se nenhuma hora disponível, mostrar erro visual
            if (horaF.field.options.length <= 1) {
                // apenas placeholder
                errorEl.textContent = 'Nenhuma hora disponível neste dia.';
            } else {
                errorEl.textContent = '';
            }
        });

        // Sair e fechar
        sairBtn.addEventListener('click', () => closeModal());
        closeX.addEventListener('click', () => closeModal());
        overlay.addEventListener('click', (e) => { if (e.target === overlay) closeModal(); });

        // Salvar
        salvarBtn.addEventListener('click', async () => {
            errorEl.textContent = '';
            const nome = nomeF.field.value.trim();
            const telefone = telefoneF.field.value.trim();
            const tipo = semanalCheck.checked ? 'SEMANAL' : 'NORMAL';
            const diaSemana = semanalCheck.checked ? (diaF.field.value || null) : null;
            const horaCorte = semanalCheck.checked ? (horaF.field.value || null) : null;
            const rapido = rapidoCheck.checked;

            // validações
            if (!nome) { errorEl.textContent = 'Nome é obrigatório.'; return; }
            if (!telefone) { errorEl.textContent = 'Telefone é obrigatório.'; return; }

            // Verificar duplicados (nome / telefone)
            const clientesArray = Object.values(this.clientes);
            if (clientesArray.some(c => c && c.nome && c.nome.toString().toLowerCase() === nome.toLowerCase())) {
                errorEl.textContent = 'Já existe um cliente com esse nome.'; return;
            }
            if (clientesArray.some(c => c && c.numeroTelefone && c.numeroTelefone.toString() === telefone)) {
                errorEl.textContent = 'Já existe um cliente com esse número.'; return;
            }

            if (tipo === 'SEMANAL' && (!diaSemana || !horaCorte)) {
                errorEl.textContent = 'Dia e hora são obrigatórios para cliente semanal.'; return;
            }

            // Para semanal, garantir não conflita com outro semanal existente
            if (tipo === 'SEMANAL') {
                const conflito = clientesArray.some(c => 
                    c && c.tipoCliente === 'SEMANAL' &&
                    c.diaSemana && c.horaCorte &&
                    c.diaSemana.toString().toLowerCase() === diaSemana.toString().toLowerCase() &&
                    c.horaCorte === horaCorte
                );
                if (conflito) {
                    errorEl.textContent = 'Já existe um cliente semanal nesse horário.'; return;
                }
            }

            const clienteObj = {
                nome,
                numeroTelefone: telefone,
                tipoCliente: tipo,
                faltas: 0,
                diaSemana,
                horaCorte,
                rapido,
                temporario: false
            };

            try {
                salvarBtn.disabled = true;
                
                const api = this.api || ((window.pywebview && window.pywebview.api) || (typeof pywebview !== 'undefined' && pywebview && pywebview.api) || null);
                if (!api) {
                    throw new Error('API do backend não disponível.');
                }

                const res = await api.adicionar_cliente(clienteObj);
                
                if (res && res.success) {
                    // Recarrega dados e actualiza UI
                    await this.carregarDados();
                    this.atualizarAreaClientes();
                    closeModal();
                } else {
                    errorEl.textContent = res && res.error ? res.error : 'Erro ao adicionar cliente.';
                }
            } catch (err) {
                console.error(err);
                errorEl.textContent = 'Erro ao comunicar com o backend.';
            } finally {
                salvarBtn.disabled = false;
            }
        });

        document.body.appendChild(overlay);
    }
}

// Inicializar quando a página carregar
document.addEventListener('DOMContentLoaded', () => {
window.paginaController = new PaginaPrincipalController();
});