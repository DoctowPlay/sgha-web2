package br.ufrrj.sgha.model;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.ufrrj.sgha.model.enums.ComissaoDestino;
import br.ufrrj.sgha.model.enums.StatusSolicitacao;
import br.ufrrj.sgha.model.enums.TipoAutor;
import br.ufrrj.sgha.model.enums.TipoParecer;
import br.ufrrj.sgha.model.excecao.OperacaoInvalidaException;

import static br.ufrrj.sgha.model.enums.StatusSolicitacao.*;

/**
 * Solicitação de homologação de horas. É a classe central: contém a MÁQUINA DE ESTADOS.
 * Todo método que muda o estado valida (1) o status atual, (2) quem está agindo e
 * (3) grava uma linha imutável em HistoricoStatus.
 *
 * JPA (Etapa 2): @Entity; @ManyToOne Discente; @OneToMany(mappedBy="solicitacao", cascade=ALL)
 * em pareceres e historico; o campo "relogio" é transient (não é persistido).
 */
public class Solicitacao {

    private Long id;
    private Discente discente;
    private String tituloDocumento;
    /** Somente o nome/caminho único gerado no upload; o arquivo fica fora do Tomcat (ex.: /uploads/sgha/). */
    private String caminhoArquivoComprovante;
    private ComissaoDestino comissaoDestino;
    private int horasPretendidas;
    private String observacaoDiscente;
    private StatusSolicitacao status;
    /** Guardado ao cancelar, para o Coordenador poder restaurar o status anterior. */
    private StatusSolicitacao statusAnteriorAoCancelamento;
    private LocalDateTime dataCriacao;
    private List<Parecer> pareceres = new ArrayList<>();
    private List<HistoricoStatus> historico = new ArrayList<>();

    /** Fonte de data/hora; transient = ignorado pelo JPA. Em produção é o relógio do sistema. */
    private transient Clock relogio = Clock.systemDefaultZone();

    protected Solicitacao() {
        // exigido pelo JPA
    }

    /** Construtor de pacote: solicitações são criadas via Discente.criarSolicitacao(). */
    Solicitacao(Discente discente, String tituloDocumento, String caminhoArquivoComprovante,
            ComissaoDestino comissaoDestino, int horasPretendidas, String observacaoDiscente, Clock relogio) {
        validarDados(tituloDocumento, comissaoDestino, horasPretendidas);
        this.discente = discente;
        this.tituloDocumento = tituloDocumento.trim();
        this.caminhoArquivoComprovante = caminhoArquivoComprovante;
        this.comissaoDestino = comissaoDestino;
        this.horasPretendidas = horasPretendidas;
        this.observacaoDiscente = observacaoDiscente;
        this.relogio = relogio;
        this.status = CRIADA;
        this.dataCriacao = LocalDateTime.now(relogio);
        registrar(null, CRIADA, "Solicitação criada", TipoAutor.DISCENTE, discente, null);
    }

    // ------------------------------------------------------------------
    // Operações do DISCENTE
    // ------------------------------------------------------------------

    /** Alterar dados: apenas em CRIADA. Se novoCaminhoArquivo for null, mantém o comprovante atual. */
    public void alterar(Discente autor, String novoTitulo, String novoCaminhoArquivo,
            ComissaoDestino novoDestino, int novasHoras, String novaObservacao) {
        exigirDono(autor);
        exigirStatus("alterar", CRIADA);
        validarDados(novoTitulo, novoDestino, novasHoras);
        this.tituloDocumento = novoTitulo.trim();
        if (novoCaminhoArquivo != null) {
            this.caminhoArquivoComprovante = novoCaminhoArquivo;
        }
        this.comissaoDestino = novoDestino;
        this.horasPretendidas = novasHoras;
        this.observacaoDiscente = novaObservacao;
        registrar(CRIADA, CRIADA, "Solicitação alterada pelo discente", TipoAutor.DISCENTE, autor, null);
    }

    /** CRIADA -> ENVIADA (passa a ser visível para a comissão). */
    public void enviar(Discente autor) {
        exigirDono(autor);
        exigirStatus("enviar", CRIADA);
        transicionar(ENVIADA, "Enviada para a comissão " + comissaoDestino.getDescricao(),
                TipoAutor.DISCENTE, autor, null);
    }

    /** Cancelamento pelo discente: qualquer status exceto CONCLUIDO e CANCELADA. */
    public void cancelar(Discente autor) {
        exigirDono(autor);
        if (status == CONCLUIDO || status == CANCELADA) {
            throw new OperacaoInvalidaException("Não é possível cancelar uma solicitação " + status + ".");
        }
        statusAnteriorAoCancelamento = status;
        transicionar(CANCELADA, "Cancelada pelo discente", TipoAutor.DISCENTE, autor, null);
    }

    /** Valida a exclusão física: só CRIADA pode ser excluída (chamado por Discente.excluirSolicitacao). */
    public void validarExclusao() {
        if (status != CRIADA) {
            throw new OperacaoInvalidaException(
                    "Exclusão não permitida: só solicitações CRIADA podem ser excluídas (status atual: " + status + ").");
        }
    }

    /** AVALIADA -> ACEITA -> CONCLUIDO  |  AVALIADA_APOS_RECURSO -> CONCLUIDO. */
    public void aceitar(Discente autor) {
        exigirDono(autor);
        exigirStatus("aceitar", AVALIADA, AVALIADA_APOS_RECURSO);
        if (status == AVALIADA) {
            transicionar(ACEITA, "Discente aceitou o parecer", TipoAutor.DISCENTE, autor, null);
            transicionar(CONCLUIDO, "Conclusão automática após aceite", TipoAutor.SISTEMA, null, null);
        } else {
            transicionar(CONCLUIDO, "Discente aceitou o parecer do recurso", TipoAutor.DISCENTE, autor, null);
        }
    }

    /** AVALIADA -> RECURSO (volta para a comissão reavaliar). */
    public void abrirRecurso(Discente autor, String justificativa) {
        exigirDono(autor);
        exigirStatus("abrir recurso", AVALIADA);
        transicionar(RECURSO, "Recurso aberto pelo discente: " + justificativa, TipoAutor.DISCENTE, autor, null);
    }

    /** AVALIADA_APOS_RECURSO -> RECURSO_COLEGIADO (decisão final do Coordenador/Colegiado). */
    public void acionarRecursoColegiado(Discente autor, String justificativa) {
        exigirDono(autor);
        exigirStatus("acionar recurso colegiado", AVALIADA_APOS_RECURSO);
        transicionar(RECURSO_COLEGIADO, "Recurso colegiado acionado: " + justificativa,
                TipoAutor.DISCENTE, autor, null);
    }

    // ------------------------------------------------------------------
    // Operações da COMISSÃO / COORDENADOR (sempre através de um Mandato vigente)
    // ------------------------------------------------------------------

    /**
     * Emite o parecer conforme o estado atual:
     *   ENVIADA -> AVALIADA | RECURSO -> AVALIADA_APOS_RECURSO | RECURSO_COLEGIADO -> CONCLUIDO (só Coordenador).
     */
    public Parecer emitirParecer(Mandato avaliador, int horasAprovadas,
            String observacaoPublica, String observacaoAdministrativa) {
        exigirMandatoAvaliador(avaliador);
        if (horasAprovadas < 0 || horasAprovadas > horasPretendidas) {
            throw new OperacaoInvalidaException(
                    "Horas aprovadas devem estar entre 0 e as horas pretendidas (" + horasPretendidas + ").");
        }
        TipoParecer tipo;
        StatusSolicitacao novoStatus;
        String descricao;
        switch (status) {
            case ENVIADA:
                tipo = TipoParecer.INICIAL;
                novoStatus = AVALIADA;
                descricao = "Parecer emitido";
                break;
            case RECURSO:
                tipo = TipoParecer.RECURSO;
                novoStatus = AVALIADA_APOS_RECURSO;
                descricao = "Parecer do recurso emitido";
                break;
            case RECURSO_COLEGIADO:
                if (avaliador.getPerfil() != br.ufrrj.sgha.model.enums.PerfilFuncional.COORDENADOR) {
                    throw new OperacaoInvalidaException("O recurso colegiado só pode ser decidido pelo Coordenador.");
                }
                tipo = TipoParecer.COLEGIADO;
                novoStatus = CONCLUIDO;
                descricao = "Parecer do Colegiado emitido (conclusão automática)";
                break;
            default:
                throw new OperacaoInvalidaException("Não há parecer pendente: status atual " + status + ".");
        }
        Parecer parecer = new Parecer(this, avaliador, tipo, horasAprovadas,
                observacaoPublica, observacaoAdministrativa, LocalDateTime.now(relogio()));
        pareceres.add(parecer);
        transicionar(novoStatus, descricao + " (" + horasAprovadas + "h)", TipoAutor.SERVIDOR, null, avaliador);
        return parecer;
    }

    /** A comissão pode corrigir o nome do documento se divergir do comprovante enviado. */
    public void corrigirNomeDocumento(Mandato avaliador, String novoNome) {
        exigirMandatoAvaliador(avaliador);
        if (!status.emAnalise()) {
            throw new OperacaoInvalidaException("O nome só pode ser corrigido durante a análise (status atual: " + status + ").");
        }
        if (novoNome == null || novoNome.isBlank()) {
            throw new OperacaoInvalidaException("O novo nome do documento não pode ser vazio.");
        }
        String anterior = tituloDocumento;
        tituloDocumento = novoNome.trim();
        registrar(status, status, "Nome do documento corrigido de '" + anterior + "' para '" + tituloDocumento + "'",
                TipoAutor.SERVIDOR, null, avaliador);
    }

    /** Cancelamento administrativo: Coordenador, em qualquer status (inclusive CONCLUIDO). */
    public void cancelarAdministrativamente(Mandato coordenador, String motivo) {
        exigirMandatoVigente(coordenador);
        if (!coordenador.getPerfil().podeCancelarAdministrativamente()) {
            throw new OperacaoInvalidaException("Apenas o Coordenador pode cancelar administrativamente.");
        }
        if (status == CANCELADA) {
            throw new OperacaoInvalidaException("A solicitação já está cancelada.");
        }
        statusAnteriorAoCancelamento = status;
        transicionar(CANCELADA, "Cancelamento administrativo: " + motivo, TipoAutor.SERVIDOR, null, coordenador);
    }

    /** CANCELADA -> status anterior ao cancelamento. Prerrogativa exclusiva do Coordenador. */
    public void restaurar(Mandato coordenador) {
        exigirMandatoVigente(coordenador);
        if (!coordenador.getPerfil().podeRestaurarCancelada()) {
            throw new OperacaoInvalidaException(
                    "Apenas o Coordenador pode restaurar solicitações canceladas (perfil atual: "
                            + coordenador.getPerfil() + ").");
        }
        exigirStatus("restaurar", CANCELADA);
        StatusSolicitacao destino = statusAnteriorAoCancelamento;
        statusAnteriorAoCancelamento = null;
        transicionar(destino, "Restaurada pelo Coordenador ao status " + destino, TipoAutor.SERVIDOR, null, coordenador);
    }

    // ------------------------------------------------------------------
    // Duplicidade (prévia da regra completa da Etapa 3)
    // ------------------------------------------------------------------

    /** Mesmo discente e título equivalente (ignora acentos, caixa e pontuação; aceita "contém"). */
    public boolean parecePossivelDuplicidadeDe(Solicitacao outra) {
        if (outra == this || !discente.equals(outra.discente)) {
            return false;
        }
        String a = normalizar(tituloDocumento);
        String b = normalizar(outra.tituloDocumento);
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares privados
    // ------------------------------------------------------------------

    private void transicionar(StatusSolicitacao novo, String descricao, TipoAutor tipoAutor,
            Discente discenteAutor, Mandato mandatoAutor) {
        StatusSolicitacao anterior = this.status;
        this.status = novo;
        registrar(anterior, novo, descricao, tipoAutor, discenteAutor, mandatoAutor);
    }

    private void registrar(StatusSolicitacao anterior, StatusSolicitacao novo, String descricao,
            TipoAutor tipoAutor, Discente discenteAutor, Mandato mandatoAutor) {
        historico.add(new HistoricoStatus(this, LocalDateTime.now(relogio()), anterior, novo,
                descricao, tipoAutor, discenteAutor, mandatoAutor));
    }

    private void exigirStatus(String acao, StatusSolicitacao... permitidos) {
        for (StatusSolicitacao p : permitidos) {
            if (status == p) {
                return;
            }
        }
        throw new OperacaoInvalidaException("Não é possível " + acao + " com a solicitação em " + status + ".");
    }

    private void exigirDono(Discente autor) {
        if (autor == null || !autor.equals(discente)) {
            throw new OperacaoInvalidaException("Apenas o discente dono da solicitação pode executar esta operação.");
        }
    }

    private void exigirMandatoVigente(Mandato mandato) {
        if (mandato == null || !mandato.estaVigenteEm(hoje())) {
            throw new OperacaoInvalidaException("Mandato não vigente: o servidor não pode atuar nesta data.");
        }
    }

    private void exigirMandatoAvaliador(Mandato mandato) {
        exigirMandatoVigente(mandato);
        if (!mandato.getPerfil().podeAvaliar(comissaoDestino)) {
            throw new OperacaoInvalidaException("O perfil " + mandato.getPerfil()
                    + " não pode avaliar solicitações da comissão " + comissaoDestino + ".");
        }
    }

    private void validarDados(String titulo, ComissaoDestino destino, int horas) {
        if (titulo == null || titulo.isBlank()) {
            throw new OperacaoInvalidaException("O título do documento é obrigatório.");
        }
        if (destino == null) {
            throw new OperacaoInvalidaException("A comissão destino é obrigatória.");
        }
        if (horas <= 0) {
            throw new OperacaoInvalidaException("As horas pretendidas devem ser maiores que zero.");
        }
    }

    private Clock relogio() {
        return (relogio != null) ? relogio : Clock.systemDefaultZone();
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio());
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    /** Somente para simulação/testes (permite "viajar no tempo"). Não usar em produção. */
    public void setRelogio(Clock relogio) { this.relogio = relogio; }

    public Parecer getUltimoParecer() {
        return pareceres.isEmpty() ? null : pareceres.get(pareceres.size() - 1);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Discente getDiscente() { return discente; }
    public String getTituloDocumento() { return tituloDocumento; }
    public String getCaminhoArquivoComprovante() { return caminhoArquivoComprovante; }
    public ComissaoDestino getComissaoDestino() { return comissaoDestino; }
    public int getHorasPretendidas() { return horasPretendidas; }
    public String getObservacaoDiscente() { return observacaoDiscente; }
    public StatusSolicitacao getStatus() { return status; }
    public StatusSolicitacao getStatusAnteriorAoCancelamento() { return statusAnteriorAoCancelamento; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public List<Parecer> getPareceres() { return Collections.unmodifiableList(pareceres); }
    public List<HistoricoStatus> getHistorico() { return Collections.unmodifiableList(historico); }

    @Override
    public String toString() {
        return "'" + tituloDocumento + "' [" + comissaoDestino + ", " + horasPretendidas + "h, " + status + "]";
    }
}
