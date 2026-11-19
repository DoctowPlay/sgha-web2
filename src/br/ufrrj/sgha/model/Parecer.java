package br.ufrrj.sgha.model;

import java.time.LocalDateTime;

import br.ufrrj.sgha.model.enums.PerfilFuncional;
import br.ufrrj.sgha.model.enums.TipoParecer;

/**
 * Parecer emitido sobre uma solicitação. Imutável.
 * Guarda quem avaliou E qual função exercia na data (snapshot do perfil).
 *
 * Parecer Duplo:
 *  - observacaoPublica: o discente e todos os perfis internos veem.
 *  - observacaoAdministrativa: somente perfis internos (comissões, coordenador, secretário).
 *
 * JPA (Etapa 2): @Entity; @ManyToOne Solicitacao e Mandato; colunas de texto com @Column(length = 2000).
 */
public class Parecer {

    private Long id;
    private Solicitacao solicitacao;
    private Mandato mandatoAvaliador;
    private PerfilFuncional perfilNoMomento;
    private String nomeAvaliador;
    private TipoParecer tipo;
    private int horasAprovadas;
    private String observacaoPublica;
    private String observacaoAdministrativa;
    private LocalDateTime dataHora;

    protected Parecer() {
        // exigido pelo JPA
    }

    Parecer(Solicitacao solicitacao, Mandato mandatoAvaliador, TipoParecer tipo, int horasAprovadas,
            String observacaoPublica, String observacaoAdministrativa, LocalDateTime dataHora) {
        this.solicitacao = solicitacao;
        this.mandatoAvaliador = mandatoAvaliador;
        this.perfilNoMomento = mandatoAvaliador.getPerfil();
        this.nomeAvaliador = mandatoAvaliador.getServidor().getNomeCompleto();
        this.tipo = tipo;
        this.horasAprovadas = horasAprovadas;
        this.observacaoPublica = observacaoPublica;
        this.observacaoAdministrativa = observacaoAdministrativa;
        this.dataHora = dataHora;
    }

    /** O discente nunca vê a observação administrativa; perfis internos sempre veem. */
    public boolean observacaoAdministrativaVisivelPara(PerfilFuncional perfil) {
        return perfil != null && perfil.veObservacaoAdministrativa();
    }

    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public Mandato getMandatoAvaliador() { return mandatoAvaliador; }
    public PerfilFuncional getPerfilNoMomento() { return perfilNoMomento; }
    public String getNomeAvaliador() { return nomeAvaliador; }
    public TipoParecer getTipo() { return tipo; }
    public int getHorasAprovadas() { return horasAprovadas; }
    public String getObservacaoPublica() { return observacaoPublica; }
    public String getObservacaoAdministrativa() { return observacaoAdministrativa; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public String toString() {
        return tipo + " por " + nomeAvaliador + " como " + perfilNoMomento + " | " + horasAprovadas + "h";
    }
}
