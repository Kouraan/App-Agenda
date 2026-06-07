/**
 * dateUtils.js
 * Utilitários de data/hora e feriados.
 * Não tem dependências externas.
 */

/** Feriados fixos portugueses (MM-DD) */
const FERIADOS_FIXOS = [
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

/**
 * Calcula a data da Páscoa para um dado ano (algoritmo de Gauss).
 * @param {number} ano
 * @returns {Date}
 */
export function calcularPascoa(ano) {
    const a = ano % 19;
    const b = Math.floor(ano / 100);
    const c = ano % 100;
    const d = Math.floor(b / 4);
    const e = b % 4;
    const f = Math.floor((b + 8) / 25);
    const g = Math.floor((b - f + 1) / 3);
    const h = (19 * a + b - d - g + 15) % 30;
    const i = Math.floor(c / 4);
    const k = c % 4;
    const l = (32 + 2 * e + 2 * i - h - k) % 7;
    const m = Math.floor((a + 11 * h + 22 * l) / 451);
    const mes = Math.floor((h + l - 7 * m + 114) / 31);
    const dia = ((h + l - 7 * m + 114) % 31) + 1;
    return new Date(ano, mes - 1, dia);
}

/**
 * Devolve o conjunto de feriados para o ano indicado (como strings "MM-DD").
 * Inclui feriados fixos e móveis (Páscoa, Sexta-feira Santa, Carnaval, Corpo de Deus).
 * @param {number} ano
 * @returns {Set<string>}
 */
export function getFeriados(ano) {
    const feriados = new Set(FERIADOS_FIXOS);

    const pascoa = calcularPascoa(ano);

    const adicionar = (data) => {
        const mm = String(data.getMonth() + 1).padStart(2, "0");
        const dd = String(data.getDate()).padStart(2, "0");
        feriados.add(`${mm}-${dd}`);
    };

    adicionar(pascoa);

    // Sexta-feira Santa (-2 dias)
    const sextaSanta = new Date(pascoa);
    sextaSanta.setDate(pascoa.getDate() - 2);
    adicionar(sextaSanta);

    // Carnaval (-47 dias)
    const carnaval = new Date(pascoa);
    carnaval.setDate(pascoa.getDate() - 47);
    adicionar(carnaval);

    // Corpo de Deus (+60 dias)
    const corpoDeus = new Date(pascoa);
    corpoDeus.setDate(pascoa.getDate() + 60);
    adicionar(corpoDeus);

    return feriados;
}

/**
 * Verifica se uma data é feriado.
 * @param {Date} date
 * @returns {boolean}
 */
export function isHoliday(date) {
    const feriados = getFeriados(date.getFullYear());
    const mm = String(date.getMonth() + 1).padStart(2, "0");
    const dd = String(date.getDate()).padStart(2, "0");
    return feriados.has(`${mm}-${dd}`);
}

/**
 * Verifica se uma data é hoje.
 * @param {Date} date
 * @returns {boolean}
 */
export function isToday(date) {
    const today = new Date();
    return (
        date.getDate() === today.getDate() &&
        date.getMonth() === today.getMonth() &&
        date.getFullYear() === today.getFullYear()
    );
}

/**
 * Verifica se uma data é domingo.
 * @param {Date} date
 * @returns {boolean}
 */
export function isSunday(date) {
    return date.getDay() === 0;
}

/**
 * Verifica se um Date está no passado (comparado com agora ao minuto).
 * @param {Date} date
 * @returns {boolean}
 */
export function isPast(date) {
    return date < new Date();
}

/**
 * Devolve a segunda-feira da semana de uma data.
 * @param {Date} date
 * @returns {Date}
 */
export function getMonday(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    d.setHours(0, 0, 0, 0);
    return d;
}

/**
 * Formata uma data segundo um formato simples.
 * Formatos suportados: 'dd/MM', 'dd/MM/yyyy', 'MMMM yyyy', 'EEEE MMMM dd'
 * @param {Date} date
 * @param {string} format
 * @returns {string}
 */
export function formatDate(date, format = "dd/MM") {
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();

    switch (format) {
        case "dd/MM":
            return `${day}/${month}`;
        case "dd/MM/yyyy":
            return `${day}/${month}/${year}`;
        case "MMMM yyyy":
            return date.toLocaleDateString("pt-PT", { month: "long", year: "numeric" });
        case "EEEE MMMM dd":
            return date.toLocaleDateString("pt-PT", {
                weekday: "long",
                day: "numeric",
                month: "long",
            });
        default:
            return date.toLocaleDateString("pt-PT");
    }
}

/**
 * Converte "HH:MM" para minutos desde meia-noite.
 * @param {string} hhmm
 * @returns {number|null}
 */
export function timeToMinutes(hhmm) {
    if (!hhmm) return null;
    const parts = String(hhmm).split(":");
    const h = parseInt(parts[0], 10);
    const m = parseInt(parts[1], 10);
    if (isNaN(h) || isNaN(m)) return null;
    return h * 60 + m;
}

/**
 * Converte minutos desde meia-noite para "HH:MM".
 * @param {number} minutes
 * @returns {string}
 */
export function minutesToTime(minutes) {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}

/**
 * Gera lista de horas de trabalho entre 07:00 e 21:00.
 * @param {number} step - intervalo em minutos (15 ou 30)
 * @returns {string[]}
 */
export function gerarHoras(step = 30) {
    const horas = [];
    for (let h = 7; h <= 21; h++) {
        for (let m = 0; m < 60; m += step) {
            if (h === 21 && m > 0) continue;
            horas.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
        }
    }
    return horas;
}

/**
 * Devolve o slot atual (arredondado para :00 ou :30) como Date.
 * @returns {Date}
 */
export function getCurrentSlotDate() {
    const now = new Date();
    const roundedMin = now.getMinutes() < 30 ? 0 : 30;
    return new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours(), roundedMin, 0, 0);
}