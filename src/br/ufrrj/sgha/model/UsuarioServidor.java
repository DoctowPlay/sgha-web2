package br.ufrrj.sgha.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import br.ufrrj.sgha.model.enums.PerfilFuncional;
import br.ufrrj.sgha.model.excecao.OperacaoInvalidaException;

/**
 * Pessoa (docente/servidor) com login e senha pré-cadastrados no banco.
 * Cadastrada UMA vez; o que muda ao longo do tempo são seus Mandatos.
 *
 * Regra de acesso: só consegue logar quem possui mandato vigente na data do login.
 *
 * JPA (Etapa 2): @Entity; @OneToMany(mappedBy = "servidor", cascade = ALL) em mandatos; login com @Column(unique = true).
 * Senha em texto plano é aceita pelo escopo do trabalho.
 */
public class UsuarioServidor {

    private Long id;
    private String nomeCompleto;
    private String login;
    private String senha;
    private List<Mandato> mandatos = new ArrayList<>();

    protected UsuarioServidor() {
        // exigido pelo JPA
    }

    public UsuarioServidor(String nomeCompleto, String login, String senha) {
        if (nomeCompleto == null || nomeCompleto.isBlank() || login == null || login.isBlank()
                || senha == null || senha.isBlank()) {
            throw new OperacaoInvalidaException("Nome, login e senha são obrigatórios.");
        }
        this.nomeCompleto = nomeCompleto;
        this.login = login;
        this.senha = senha;
    }

    /**
     * Concede um novo mandato ao servidor. Um servidor não pode ter dois mandatos
     * ativos com períodos sobrepostos (não exerce dois cargos ao mesmo tempo).
     */
    public Mandato concederMandato(PerfilFuncional perfil, LocalDate dataInicio, LocalDate dataFim) {
        Mandato novo = new Mandato(this, perfil, dataInicio, dataFim);
        for (Mandato existente : mandatos) {
            if (existente.isAtivo() && existente.sobrepoe(dataInicio, dataFim)) {
                throw new OperacaoInvalidaException("Período sobreposto ao mandato existente: " + existente);
            }
        }
        mandatos.add(novo);
        return novo;
    }

    /** Mandato vigente na data informada (no máximo um, graças à regra de não sobreposição). */
    public Optional<Mandato> getMandatoVigenteEm(LocalDate data) {
        return mandatos.stream().filter(m -> m.estaVigenteEm(data)).findFirst();
    }

    public boolean podeLogarEm(LocalDate data) {
        return getMandatoVigenteEm(data).isPresent();
    }

    /** Autenticação: credenciais corretas E mandato vigente na data. */
    public boolean autenticar(String loginInformado, String senhaInformada, LocalDate data) {
        return login.equals(loginInformado) && senha.equals(senhaInformada) && podeLogarEm(data);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public String getLogin() { return login; }
    public List<Mandato> getMandatos() { return Collections.unmodifiableList(mandatos); }

    @Override
    public String toString() {
        return nomeCompleto + " (" + login + ")";
    }
}
