package br.ufrrj.sgha.model.excecao;

/**
 * Lançada quando uma regra de negócio é violada (transição inválida, perfil sem permissão,
 * mandato não vigente...). Os Servlets (Etapa 3) vão capturá-la e exibir a mensagem na JSP.
 */
public class OperacaoInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
