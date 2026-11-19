package br.ufrrj.sgha.model.enums;

/**
 * Comissão para a qual o discente direciona a solicitação.
 * JPA (Etapa 2): @Enumerated(EnumType.STRING) no atributo da entidade Solicitacao.
 */
public enum ComissaoDestino {

    EXTENSAO("Extensão"),
    ATIVIDADES_COMPLEMENTARES("Atividades Complementares"),
    ESTAGIO("Estágio");

    private final String descricao;

    ComissaoDestino(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
