/**
 * AnotacoesModule.js
 * Gere a área de anotações: blur/unblur e persistência.
 */

import { lerAnotacoes, guardarAnotacoes } from "../utils/apiUtils.js";

export class AnotacoesModule {
    /**
     * @param {HTMLTextAreaElement} anotacoesArea
     * @param {HTMLButtonElement}   blurToggleBtn
     */
    constructor(anotacoesArea, blurToggleBtn) {
        this.area = anotacoesArea;
        this.btn  = blurToggleBtn;
        this.blurred = true;

        this._bind();
        this._aplicarBlur(true); // começa sempre desfocado
    }

    // Inicialização

    async carregarAnotacoes() {
        const res = await lerAnotacoes();
        if (res && res.success) {
            this.area.value = res.texto || "";
        }
    }

    // Blur

    toggle() {
        this.blurred = !this.blurred;
        this._aplicarBlur(this.blurred);
    }

    _aplicarBlur(blur) {
        if (blur) {
            this.area.classList.add("blurred");
            this.btn.textContent = "👁";
            this.area.disabled = true;
        } else {
            this.area.classList.remove("blurred");
            this.btn.textContent = "⛔";
            this.area.disabled = false;
            this.area.focus();
        }
    }

    // Persistência

    async guardar() {
        await guardarAnotacoes(this.area.value);
    }

    // Bindings

    _bind() {
        this.btn.addEventListener("click", () => this.toggle());
        this.area.addEventListener("blur", () => this.guardar());
    }
}