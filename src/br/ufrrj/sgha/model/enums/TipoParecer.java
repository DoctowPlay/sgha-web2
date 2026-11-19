package br.ufrrj.sgha.model.enums;

/** Em que etapa do fluxo o parecer foi emitido. */
public enum TipoParecer {
    INICIAL,    // ENVIADA -> AVALIADA
    RECURSO,    // RECURSO -> AVALIADA_APOS_RECURSO
    COLEGIADO   // RECURSO_COLEGIADO -> CONCLUIDO
}
