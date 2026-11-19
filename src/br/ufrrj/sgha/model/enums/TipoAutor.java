package br.ufrrj.sgha.model.enums;

/** Quem originou uma entrada da trilha de auditoria. */
public enum TipoAutor {
    DISCENTE,
    SERVIDOR,   // ação feita através de um Mandato (Comissão, Coordenador, Secretário)
    SISTEMA     // transições automáticas (ex.: ACEITA -> CONCLUIDO)
}
