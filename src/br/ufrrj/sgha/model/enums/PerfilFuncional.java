package br.ufrrj.sgha.model.enums;

/**
 * Os 5 perfis funcionais (RBAC) exercidos por servidores/docentes através de um Mandato.
 * As regras de permissão ficam no próprio Enum para que Servlets/Filtros (Etapa 3)
 * e as entidades reutilizem a mesma lógica.
 */
public enum PerfilFuncional {

    COMISSAO_EXTENSAO("Comissão de Extensão", ComissaoDestino.EXTENSAO),
    COMISSAO_COMPLEMENTARES("Comissão de Atividades Complementares", ComissaoDestino.ATIVIDADES_COMPLEMENTARES),
    COMISSAO_ESTAGIO("Comissão de Estágio", ComissaoDestino.ESTAGIO),
    COORDENADOR("Coordenador do Curso", null),
    SECRETARIO("Secretário do Curso", null);

    private final String descricao;
    /** Comissão atendida pelo perfil; null para Coordenador e Secretário. */
    private final ComissaoDestino comissaoAtendida;

    PerfilFuncional(String descricao, ComissaoDestino comissaoAtendida) {
        this.descricao = descricao;
        this.comissaoAtendida = comissaoAtendida;
    }

    public String getDescricao() {
        return descricao;
    }

    public ComissaoDestino getComissaoAtendida() {
        return comissaoAtendida;
    }

    public boolean ehComissao() {
        return comissaoAtendida != null;
    }

    /** Coordenador avalia qualquer solicitação; cada comissão avalia só a sua; Secretário nunca avalia. */
    public boolean podeAvaliar(ComissaoDestino destino) {
        if (this == COORDENADOR) {
            return true;
        }
        return ehComissao() && comissaoAtendida == destino;
    }

    /** Prerrogativa exclusiva do Coordenador: restaurar solicitações CANCELADAS. */
    public boolean podeRestaurarCancelada() {
        return this == COORDENADOR;
    }

    /** Cancelamento administrativo (inclusive de solicitações CONCLUIDAS): Coordenador. */
    public boolean podeCancelarAdministrativamente() {
        return this == COORDENADOR;
    }

    /** Consulta global de discentes/solicitações/pareceres (sem poder de decisão). */
    public boolean podeConsultarTudo() {
        return this == COORDENADOR || this == SECRETARIO;
    }

    /** Observação Administrativa (privada) é visível a todos os perfis internos; nunca ao discente. */
    public boolean veObservacaoAdministrativa() {
        return true;
    }
}
