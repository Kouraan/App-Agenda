const Validation = {
    podeClienteSemanal(clientesArray, diaSemana, horaCorte) {
        for (const c of clientesArray) {
            if (c.tipoCliente === "SEMANAL") {
                if ((c.diaSemana || "").toUpperCase() === diaSemana.toUpperCase() &&
                    c.horaCorte === horaCorte) {
                    return false;
                }
            }
        }
        return true;
    },

    horaValida({ horaCorte = null, dataHora = null } = {}) {
        const inicioManha = 7 * 60, fimManha = 12 * 60 + 30;
        const inicioTarde = 14 * 60, fimTarde = 21 * 60;

        if (horaCorte !== null && dataHora === null) {
            const min = timeToMinutes(horaCorte);
            if (min === null) return false;
            const quartoHora = min % 15 === 0;
            const manha = min >= inicioManha && min <= fimManha;
            const tarde = min >= inicioTarde && min <= fimTarde;
            return quartoHora && (manha || tarde);
        }

        if (dataHora !== null && horaCorte === null) {
            const dt = dataHora instanceof Date ? dataHora : new Date(dataHora);
            const min = dt.getHours() * 60 + dt.getMinutes();
            const diaSemana = dt.getDay(); // 0=domingo ... 6=sábado

            const diaOk = diaSemana >= 1 && diaSemana <= 6; // Segunda a sábado
            const quartoHora = dt.getMinutes() % 15 === 0;
            const manha = min >= inicioManha && min <= fimManha;
            const tarde = min >= inicioTarde && min <= fimTarde;

            let horaFutura = true;
            const agora = new Date();
            if (dt.toDateString() === agora.toDateString()) {
                horaFutura = dt.getTime() > agora.getTime();
            }

            return diaOk && quartoHora && (manha || tarde) && horaFutura;
        }

        return false;
    },

    nomeValido(nome) {
        return nome !== null && nome !== undefined && nome.trim().length >= 2;
    },

    passwordValida(password) {
        return password !== null && password !== undefined && password.trim().length >= 6;
    },

    clienteDuplicado(clientesArray, nome, numeroTelefone) {
        return clientesArray.some(c =>
            c.nome.toLowerCase() === nome.toLowerCase() ||
            c.numeroTelefone === numeroTelefone
        );
    },

    marcacaoDuplicada(marcacoesMap, dataHoraISO) {
        return Object.prototype.hasOwnProperty.call(marcacoesMap, dataHoraISO);
    },

    numeroTelefoneValido(numero) {
        if (!numero) return false;
        const limpo = numero.replace(/[\s\-()]/g, "");
        return /^(\+)?\d{8,15}$/.test(limpo);
    },

    clienteValido(cliente, clientesArray) {
        if (!cliente) return false;

        const nomeOk = Validation.nomeValido(cliente.nome);
        const telefoneOk = Validation.numeroTelefoneValido(cliente.numeroTelefone);
        const naoDuplicado = !Validation.clienteDuplicado(clientesArray, cliente.nome, cliente.numeroTelefone);

        let tipoOk = true;
        if (cliente.tipoCliente === "SEMANAL") {
            tipoOk = !!cliente.diaSemana && !!cliente.horaCorte;
        }

        return nomeOk && telefoneOk && naoDuplicado && tipoOk;
    },

    marcacaoValida(marcacao, clientesArray) {
        return !!marcacao &&
            Validation.clienteValido(marcacao.cliente, clientesArray) &&
            !!marcacao.dataHora &&
            Validation.horaValida({ dataHora: marcacao.dataHora }) &&
            marcacao.duracao > 0;
    },
};

function timeToMinutes(hhmm) {
    if (!hhmm) return null;
    const partes = String(hhmm).split(":");
    const h = parseInt(partes[0], 10);
    const m = parseInt(partes[1], 10);
    if (isNaN(h) || isNaN(m)) return null;
    return h * 60 + m;
}