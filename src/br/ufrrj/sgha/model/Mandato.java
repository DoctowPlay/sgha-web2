package br.ufrrj.sgha.model;

import java.time.LocalDate;

import br.ufrrj.sgha.model.enums.PerfilFuncional;
import br.ufrrj.sgha.model.excecao.OperacaoInvalidaException;

/**
 * Vínculo (atuação) de um servidor com um perfil funcional durante um período.
 *
 * É ESTA classe que resolve a "troca de cargos": o servidor continua sendo o mesmo
 * (UsuarioServidor), mas pode ter vários mandatos ao longo do tempo
 * (Comissão -> Coordenador -> Comissão ...), cada um com perfil, início e fim.
 *
 * JPA (Etapa 2): @Entity; @ManyToOne para UsuarioServidor; @Enumerated(STRING) em perfil.
 */
public class Mandato {

    private Long id;
    private UsuarioServidor servidor;
    private PerfilFuncional perfil;
    private LocalDate dataInicio;
    /** null = mandato em aberto (sem data de término). */
    private LocalDate dataFim;
    /** Chave administrativa: false = mandato revogado/desativado, independente das datas. */
    private boolean ativo = true;

    protected Mandato() {
        // exigido pelo JPA
    }

    /** Construtor de visibilidade de pacote: mandatos são criados via UsuarioServidor.concederMandato(). */
    Mandato(UsuarioServidor servidor, PerfilFuncional perfil, LocalDate dataInicio, LocalDate dataFim) {
        if (servidor == null || perfil == null || dataInicio == null) {
            throw new OperacaoInvalidaException("Servidor, perfil e data de início são obrigatórios.");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new OperacaoInvalidaException("A data de fim não pode ser anterior à data de início.");
        }
        this.servidor = servidor;
        this.perfil = perfil;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    /** Vigente = ativo E data dentro de [início, fim] (fim nulo = sem término). */
    public boolean estaVigenteEm(LocalDate data) {
        if (!ativo) {
            return false;
        }
        boolean aposInicio = !data.isBefore(dataInicio);
        boolean antesDoFim = (dataFim == null) || !data.isAfter(dataFim);
        return aposInicio && antesDoFim;
    }

    public boolean estaVigenteHoje() {
        return estaVigenteEm(LocalDate.now());
    }

    /** Encerra o mandato na data informada (o servidor deixa o cargo, mas o histórico permanece). */
    public void encerrar(LocalDate dataFim) {
        if (dataFim.isBefore(dataInicio)) {
            throw new OperacaoInvalidaException("A data de fim não pode ser anterior à data de início.");
        }
        this.dataFim = dataFim;
    }

    /** Revoga o mandato por completo (nunca mais vigente). */
    public void desativar() {
        this.ativo = false;
    }

    /** Dois períodos se sobrepõem? (fim nulo é tratado como "infinito"). */
    boolean sobrepoe(LocalDate outroInicio, LocalDate outroFim) {
        LocalDate fimEfetivo = (dataFim == null) ? LocalDate.MAX : dataFim;
        LocalDate outroFimEfetivo = (outroFim == null) ? LocalDate.MAX : outroFim;
        return !dataInicio.isAfter(outroFimEfetivo) && !outroInicio.isAfter(fimEfetivo);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UsuarioServidor getServidor() { return servidor; }
    public PerfilFuncional getPerfil() { return perfil; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public boolean isAtivo() { return ativo; }

    @Override
    public String toString() {
        return perfil + " [" + dataInicio + " -> " + (dataFim == null ? "atual" : dataFim) + "]"
                + (ativo ? "" : " (revogado)");
    }
}
