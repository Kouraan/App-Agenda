const MarcacoesSemanais = {
    gerarMarcacoesSemanais(cliente, marcacoesArray, dataInicio, mesesAFrente = 6, slotsUsados = null) {
        const novas = [];

        if (!cliente.diaSemana || !cliente.horaCorte) return novas;

        let diaSemana;
        try {
            diaSemana = MarcacoesSemanais._traduzirDiaSemana(cliente.diaSemana);
        } catch (e) {
            return novas;
        }

        const [hh, mm] = cliente.horaCorte.split(":").map(Number);
        if (isNaN(hh) || isNaN(mm)) return novas;

        const duracao = cliente.rapido ? 15 : 30;

        const marcacoesPorData = new Map();
        for (const m of marcacoesArray) {
            marcacoesPorData.set(m.data_hora || m.dataHora, m);
        }

        const overlapDt = (aStart, aDur, bStart, bDur) => {
            const aEnd = new Date(aStart.getTime() + aDur * 60000);
            const bEnd = new Date(bStart.getTime() + bDur * 60000);
            return !(aEnd <= bStart || bEnd <= aStart);
        };

        // Primeiro dia da semana correto a partir de dataInicio
        let data = new Date(dataInicio);
        data.setHours(0, 0, 0, 0);
        while (data.getDay() !== diaSemana) {
            data = new Date(data.getTime() + 86400000);
        }

        const limite = new Date(dataInicio.getTime() + mesesAFrente * 31 * 86400000);
        limite.setHours(23, 59, 59, 999);

        let candidate = new Date(data.getFullYear(), data.getMonth(), data.getDate(), hh, mm);

        while (candidate <= limite) {

            while (candidate <= limite) {
                const candidateISO = toLocalISOString(candidate);

                if (slotsUsados && slotsUsados.has(candidateISO)) {
                    candidate = new Date(candidate.getTime() + 7 * 86400000);
                    continue;
                }

                let occupied = false;
                for (const [exISO, exM] of marcacoesPorData) {
                    const exStart = new Date(exISO);
                    const exDur = exM.duracao !== undefined ? exM.duracao : 30;
                    if (overlapDt(candidate, duracao, exStart, exDur)) {
                        occupied = true;
                        break;
                    }
                }

                if (!occupied) break;
                candidate = new Date(candidate.getTime() + 7 * 86400000);
            }

            if (candidate > limite) break;

            let already = false;
            const candidateISO = toLocalISOString(candidate);
            for (const [exISO, exM] of marcacoesPorData) {
                const nomeEx = exM.cliente_nome || (exM.cliente ? exM.cliente.nome : null);
                if (exISO === candidateISO && nomeEx === cliente.nome) {
                    already = true;
                    break;
                }
            }

            if (!already) {
                const nova = { dataHora: candidateISO, duracao };
                novas.push(nova);
                marcacoesPorData.set(candidateISO, { data_hora: candidateISO, duracao });
            }

            candidate = new Date(candidate.getTime() + 7 * 86400000);
        }

        return novas;
    },

    _traduzirDiaSemana(diaSemanaPt) {
        const traducao = {
            "segunda": 1, "terça": 2, "terca": 2, "quarta": 3,
            "quinta": 4, "sexta": 5, "sábado": 6, "sabado": 6, "domingo": 0,
        };
        const diaLower = diaSemanaPt.toLowerCase().trim();
        if (diaLower in traducao) return traducao[diaLower];
        throw new Error(`Dia da semana inválido: ${diaSemanaPt}`);
    },
};