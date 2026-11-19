package br.ufrrj.sgha.model.enums;

/**
 * Estados da máquina de estados da Solicitação.
 * As regras de transição ficam em Solicitacao; aqui ficam apenas consultas sobre o estado.
 */
public enum StatusSolicitacao {

    CRIADA("Criada"),
    ENVIADA("Enviada / Parecer pendente"),
    AVALIADA("Avaliada"),
    ACEITA("Aceita"),
    RECURSO("Recurso"),
    AVALIADA_APOS_RECURSO("Avaliada após recurso"),
    RECURSO_COLEGIADO("Recurso colegiado"),
    CANCELADA("Cancelada"),
    CONCLUIDO("Concluído");

    private final String descricao;

    StatusSolicitacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Estados em que a comissão/coordenador está analisando (parecer pendente). */
    public boolean emAnalise() {
        return this == ENVIADA || this == RECURSO || this == RECURSO_COLEGIADO;
    }

    /** CRIADA é visível só ao discente; CANCELADA sai das filas da comissão. */
    public boolean visivelParaComissao() {
        return this != CRIADA && this != CANCELADA;
    }

    /** CONCLUIDO é estático: não permite alteração nem exclusão. */
    public boolean ehFinal() {
        return this == CONCLUIDO;
    }
}
