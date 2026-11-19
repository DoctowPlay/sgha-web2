package br.ufrrj.sgha.model;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import br.ufrrj.sgha.model.enums.ComissaoDestino;
import br.ufrrj.sgha.model.enums.StatusSolicitacao;
import br.ufrrj.sgha.model.excecao.OperacaoInvalidaException;

/**
 * Aluno de Sistemas de Informação. Auto-cadastrado no primeiro acesso (nome + matrícula).
 * A conta é imutável: não há setters para nome/matrícula e não existe operação de exclusão.
 *
 * JPA (Etapa 2): @Entity; matricula com @Column(unique = true);
 * @OneToMany(mappedBy = "discente") em solicitacoes.
 */
public class Discente {

    private Long id;
    private String matricula;
    private String nomeCompleto;
    private LocalDateTime dataCadastro;
    private List<Solicitacao> solicitacoes = new ArrayList<>();

    protected Discente() {
        // exigido pelo JPA
    }

    public Discente(String nomeCompleto, String matricula) {
        if (nomeCompleto == null || nomeCompleto.isBlank() || matricula == null || matricula.isBlank()) {
            throw new OperacaoInvalidaException("Nome completo e matrícula são obrigatórios.");
        }
        this.nomeCompleto = nomeCompleto.trim();
        this.matricula = matricula.trim();
        this.dataCadastro = LocalDateTime.now();
    }

    /** Cria uma solicitação (status CRIADA) já vinculada a este discente. */
    public Solicitacao criarSolicitacao(String tituloDocumento, String caminhoArquivoComprovante,
            ComissaoDestino comissaoDestino, int horasPretendidas, String observacaoDiscente) {
        return criarSolicitacao(tituloDocumento, caminhoArquivoComprovante, comissaoDestino,
                horasPretendidas, observacaoDiscente, Clock.systemDefaultZone());
    }

    /** Variante com relógio explícito, usada apenas para simular datas passadas nos testes. */
    public Solicitacao criarSolicitacao(String tituloDocumento, String caminhoArquivoComprovante,
            ComissaoDestino comissaoDestino, int horasPretendidas, String observacaoDiscente, Clock relogio) {
        Solicitacao nova = new Solicitacao(this, tituloDocumento, caminhoArquivoComprovante,
                comissaoDestino, horasPretendidas, observacaoDiscente, relogio);
        solicitacoes.add(nova);
        return nova;
    }

    /** Exclusão física: só é permitida se a solicitação estiver CRIADA (validado na Solicitacao). */
    public void excluirSolicitacao(Solicitacao solicitacao) {
        solicitacao.validarExclusao();
        solicitacoes.remove(solicitacao);
    }

    /**
     * Prévia do Alerta de Duplicidade Cruzada (Etapa 3): outras solicitações do MESMO discente,
     * em QUALQUER comissão, com documento semelhante. Ignora CRIADA e CANCELADA.
     */
    public List<Solicitacao> buscarPossiveisDuplicidades(Solicitacao alvo) {
        return solicitacoes.stream()
                .filter(s -> alvo.parecePossivelDuplicidadeDe(s))
                .filter(s -> s.getStatus() != StatusSolicitacao.CRIADA && s.getStatus() != StatusSolicitacao.CANCELADA)
                .collect(Collectors.toList());
    }

    /**
     * Consolidação de carga horária: soma das horas do último parecer das solicitações
     * CONCLUIDAS da comissão informada.
     */
    public int totalHorasHomologadas(ComissaoDestino comissao) {
        return solicitacoes.stream()
                .filter(s -> s.getStatus() == StatusSolicitacao.CONCLUIDO && s.getComissaoDestino() == comissao)
                .map(Solicitacao::getUltimoParecer)
                .filter(Objects::nonNull)
                .mapToInt(Parecer::getHorasAprovadas)
                .sum();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricula() { return matricula; }
    public String getNomeCompleto() { return nomeCompleto; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public List<Solicitacao> getSolicitacoes() { return Collections.unmodifiableList(solicitacoes); }

    /** Identidade de negócio = matrícula. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Discente)) return false;
        return Objects.equals(matricula, ((Discente) o).matricula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matricula);
    }

    @Override
    public String toString() {
        return nomeCompleto + " (mat. " + matricula + ")";
    }
}
