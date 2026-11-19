package br.ufrrj.sgha.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import br.ufrrj.sgha.model.enums.PerfilFuncional;
import br.ufrrj.sgha.model.enums.StatusSolicitacao;
import br.ufrrj.sgha.model.enums.TipoAutor;

/**
 * Trilha de auditoria IMUTÁVEL: cada transição, alteração ou parecer gera uma linha.
 * Não possui setters nem métodos de alteração; só é criada por Solicitacao (construtor de pacote).
 *
 * Guarda o Mandato do autor (para rastreabilidade) e também um "retrato" (snapshot) do nome e
 * do perfil que ele exercia NAQUELE momento, para o histórico nunca mudar, mesmo se o
 * servidor trocar de cargo depois.
 *
 * JPA (Etapa 2): @Entity; @ManyToOne Solicitacao, Discente (opcional) e Mandato (opcional).
 */
public class HistoricoStatus {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long id;
    private Solicitacao solicitacao;
    private LocalDateTime dataHora;
    /** null apenas na criação da solicitação. */
    private StatusSolicitacao statusAnterior;
    private StatusSolicitacao novoStatus;
    private String descricao;
    private TipoAutor tipoAutor;
    private Discente discenteAutor;     // preenchido se tipoAutor == DISCENTE
    private Mandato mandatoAutor;       // preenchido se tipoAutor == SERVIDOR
    private String nomeAutor;           // snapshot
    private PerfilFuncional perfilAutorNoMomento; // snapshot (null para discente/sistema)

    protected HistoricoStatus() {
        // exigido pelo JPA
    }

    HistoricoStatus(Solicitacao solicitacao, LocalDateTime dataHora, StatusSolicitacao statusAnterior,
            StatusSolicitacao novoStatus, String descricao, TipoAutor tipoAutor,
            Discente discenteAutor, Mandato mandatoAutor) {
        this.solicitacao = solicitacao;
        this.dataHora = dataHora;
        this.statusAnterior = statusAnterior;
        this.novoStatus = novoStatus;
        this.descricao = descricao;
        this.tipoAutor = tipoAutor;
        switch (tipoAutor) {
            case DISCENTE:
                this.discenteAutor = discenteAutor;
                this.nomeAutor = discenteAutor.getNomeCompleto();
                break;
            case SERVIDOR:
                this.mandatoAutor = mandatoAutor;
                this.nomeAutor = mandatoAutor.getServidor().getNomeCompleto();
                this.perfilAutorNoMomento = mandatoAutor.getPerfil();
                break;
            default:
                this.nomeAutor = "SISTEMA";
        }
    }

    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public LocalDateTime getDataHora() { return dataHora; }
    public StatusSolicitacao getStatusAnterior() { return statusAnterior; }
    public StatusSolicitacao getNovoStatus() { return novoStatus; }
    public String getDescricao() { return descricao; }
    public TipoAutor getTipoAutor() { return tipoAutor; }
    public Discente getDiscenteAutor() { return discenteAutor; }
    public Mandato getMandatoAutor() { return mandatoAutor; }
    public String getNomeAutor() { return nomeAutor; }
    public PerfilFuncional getPerfilAutorNoMomento() { return perfilAutorNoMomento; }

    @Override
    public String toString() {
        String funcao = (perfilAutorNoMomento != null) ? " como " + perfilAutorNoMomento
                : (tipoAutor == TipoAutor.DISCENTE ? " (discente)" : "");
        return "[" + dataHora.format(FORMATO) + "] "
                + (statusAnterior == null ? "-" : statusAnterior) + " -> " + novoStatus
                + " | " + nomeAutor + funcao + " | " + descricao;
    }
}
