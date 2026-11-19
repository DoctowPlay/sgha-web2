package br.ufrrj.sgha.teste;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufrrj.sgha.model.Discente;
import br.ufrrj.sgha.model.HistoricoStatus;
import br.ufrrj.sgha.model.Mandato;
import br.ufrrj.sgha.model.Parecer;
import br.ufrrj.sgha.model.Solicitacao;
import br.ufrrj.sgha.model.UsuarioServidor;
import br.ufrrj.sgha.model.enums.ComissaoDestino;
import br.ufrrj.sgha.model.enums.PerfilFuncional;
import br.ufrrj.sgha.model.excecao.OperacaoInvalidaException;

/**
 * Simulação da camada model (Etapa 1): sem banco, sem Servlets, apenas objetos em memória.
 * Para simular o passar do tempo (mandatos de 2023 a 2026) cada Solicitacao recebe um
 * Clock "fixo" com a data/hora do evento simulado.
 */
public class MainTesteModelo {

    /** "Banco" em memória: matrícula -> discente (simula a checagem de cadastro prévio). */
    private static final Map<String, Discente> BANCO_DISCENTES = new LinkedHashMap<>();

    public static void main(String[] args) {

        // ============================================================
        titulo("1. IDENTIFICAÇÃO / AUTO-CADASTRO DO DISCENTE");
        Discente lucas = identificarOuCadastrar("Lucas Almeida", "20210001");
        Discente beatriz = identificarOuCadastrar("Beatriz Souza", "20220002");
        Discente lucasDeNovo = identificarOuCadastrar("Lucas Almeida", "20210001");
        System.out.println("  Mesmo objeto no 2º acesso do Lucas? " + (lucas == lucasDeNovo));
        System.out.println("  Total de discentes cadastrados: " + BANCO_DISCENTES.size());

        // ============================================================
        titulo("2. SERVIDORES COM MÚLTIPLOS MANDATOS AO LONGO DO TEMPO");
        UsuarioServidor carlos = new UsuarioServidor("Prof. Carlos Mendes", "carlos", "123");
        Mandato carlosExtensao1 = carlos.concederMandato(PerfilFuncional.COMISSAO_EXTENSAO, d("2023-02-01"), d("2024-07-31"));
        Mandato carlosCoord = carlos.concederMandato(PerfilFuncional.COORDENADOR, d("2024-08-01"), d("2025-12-31"));
        Mandato carlosExtensao2 = carlos.concederMandato(PerfilFuncional.COMISSAO_EXTENSAO, d("2026-02-01"), null);

        UsuarioServidor helena = new UsuarioServidor("Profa. Helena Rocha", "helena", "123");
        Mandato helenaCoord = helena.concederMandato(PerfilFuncional.COORDENADOR, d("2026-01-01"), null);

        UsuarioServidor marina = new UsuarioServidor("Profa. Marina Lopes", "marina", "123");
        Mandato marinaComplementares = marina.concederMandato(PerfilFuncional.COMISSAO_COMPLEMENTARES, d("2025-01-01"), null);

        UsuarioServidor paulo = new UsuarioServidor("Prof. Paulo Ribeiro", "paulo", "123");
        Mandato pauloEstagio = paulo.concederMandato(PerfilFuncional.COMISSAO_ESTAGIO, d("2024-03-01"), null);

        UsuarioServidor joana = new UsuarioServidor("Joana Teixeira", "joana", "123");
        Mandato joanaSecretaria = joana.concederMandato(PerfilFuncional.SECRETARIO, d("2025-01-01"), null);

        UsuarioServidor roberto = new UsuarioServidor("Roberto Dias (ex-secretário)", "roberto", "123");
        Mandato robertoSecretaria = roberto.concederMandato(PerfilFuncional.SECRETARIO, d("2021-01-01"), d("2024-12-31"));

        System.out.println("  Mandatos do Prof. Carlos:");
        for (Mandato m : carlos.getMandatos()) {
            System.out.println("    - " + m);
        }
        System.out.println("  Perfil do Carlos em cada data (vigência):");
        for (String data : new String[] { "2023-06-01", "2024-09-15", "2025-12-31", "2026-01-15", "2026-03-01" }) {
            String perfil = carlos.getMandatoVigenteEm(d(data)).map(m -> m.getPerfil().toString()).orElse("SEM MANDATO (não loga)");
            System.out.println("    " + data + " -> " + perfil);
        }
        System.out.println("  Login do Carlos em 2026-01-15 (entre mandatos)? " + carlos.autenticar("carlos", "123", d("2026-01-15")));
        System.out.println("  Login do Carlos em 2026-03-01 (comissão de volta)? " + carlos.autenticar("carlos", "123", d("2026-03-01")));
        System.out.println("  Login do Carlos com senha errada? " + carlos.autenticar("carlos", "xxx", d("2026-03-01")));
        System.out.println("  Login do ex-secretário Roberto em 2026-03-01? " + roberto.autenticar("roberto", "123", d("2026-03-01")));
        esperarErro("Mandato sobreposto ao mandato atual do Carlos",
                () -> carlos.concederMandato(PerfilFuncional.COORDENADOR, d("2026-06-01"), null));

        // ============================================================
        titulo("3. FLUXO COMPLETO: CRIADA -> ... -> RECURSO COLEGIADO -> CONCLUIDO (Lucas, Estágio)");
        Solicitacao a = lucas.criarSolicitacao("Certificado de Estagio - Empresa XPTO", "a1b2-estagio.pdf",
                ComissaoDestino.ESTAGIO, 120, "Estágio de 6 meses", em("2025-02-10T09:00"));
        a.setRelogio(em("2025-02-10T09:30"));
        a.alterar(lucas, "Declaração de Estágio - Empresa XPTO", null, ComissaoDestino.ESTAGIO, 120,
                "Estágio de 6 meses (anexei a declaração)");
        a.setRelogio(em("2025-02-10T10:00"));
        a.enviar(lucas);
        // Em 2025 o Carlos era COORDENADOR: avalia qualquer comissão e fica registrado como Coordenador.
        a.setRelogio(em("2025-03-05T14:00"));
        a.emitirParecer(carlosCoord, 60, "Carga horária comprovada insuficiente.",
                "Conferir datas com a declaração da empresa.");
        a.setRelogio(em("2025-03-06T08:00"));
        a.abrirRecurso(lucas, "Anexei contrato com a carga horária completa.");
        a.setRelogio(em("2025-04-02T11:00"));
        a.emitirParecer(pauloEstagio, 100, "Aprovadas 100h após reanálise.", "Contrato confere, falta 1 mês de comprovação.");
        a.setRelogio(em("2025-04-03T09:00"));
        a.acionarRecursoColegiado(lucas, "Discordo das 100h; o estágio durou 6 meses.");
        a.setRelogio(em("2025-04-20T16:00"));
        a.emitirParecer(carlosCoord, 110, "Colegiado aprova 110h.", "Decisão final do Colegiado.");
        imprimirSolicitacao(a);

        // ============================================================
        titulo("4. ACEITE DIRETO (Beatriz, Extensão) - passa por ACEITA e vai a CONCLUIDO");
        Solicitacao b = beatriz.criarSolicitacao("Certificado Semana de Extensão Rural", "c3d4-extensao.pdf",
                ComissaoDestino.EXTENSAO, 40, "", em("2026-03-10T10:00"));
        b.setRelogio(em("2026-03-10T10:05"));
        b.enviar(beatriz);
        b.setRelogio(em("2026-03-20T15:00"));
        // Em 2026 o Carlos voltou à COMISSÃO DE EXTENSÃO: o perfil registrado é outro.
        b.emitirParecer(carlosExtensao2, 40, "Aprovado integralmente.", "");
        b.setRelogio(em("2026-03-21T09:00"));
        b.aceitar(beatriz);
        imprimirSolicitacao(b);

        // ============================================================
        titulo("5. RBAC + VIGÊNCIA: tentativas bloqueadas, cancelamento e restauração (Lucas, Extensão)");
        Solicitacao c = lucas.criarSolicitacao("Certificado Oficina Python (Projeto de Extensão)", "e5f6-oficina.pdf",
                ComissaoDestino.EXTENSAO, 20, "Oficina de 20h", em("2026-04-01T09:00"));
        c.setRelogio(em("2026-04-01T09:10"));
        c.enviar(lucas);
        c.setRelogio(em("2026-04-02T10:00"));
        esperarErro("Comissão de Complementares avaliando solicitação de Extensão",
                () -> c.emitirParecer(marinaComplementares, 20, "x", "x"));
        esperarErro("Secretário tentando emitir parecer",
                () -> c.emitirParecer(joanaSecretaria, 20, "x", "x"));
        esperarErro("Ex-secretário (mandato encerrado em 2024) tentando atuar",
                () -> c.emitirParecer(robertoSecretaria, 20, "x", "x"));
        esperarErro("Carlos usando mandato antigo de Coordenador (já encerrado) em 2026",
                () -> c.emitirParecer(carlosCoord, 20, "x", "x"));
        c.setRelogio(em("2026-04-03T08:00"));
        c.cancelar(lucas);
        System.out.println("  Status após cancelar: " + c.getStatus() + " (anterior guardado: "
                + c.getStatusAnteriorAoCancelamento() + "; visível para comissão? "
                + c.getStatus().visivelParaComissao() + ")");
        c.setRelogio(em("2026-04-04T08:00"));
        esperarErro("Comissão de Extensão tentando restaurar solicitação cancelada",
                () -> c.restaurar(carlosExtensao2));
        c.restaurar(helenaCoord);
        System.out.println("  Status após restauração pelo Coordenador: " + c.getStatus());

        // ============================================================
        titulo("6. AJUSTE DO NOME + ALERTA DE DUPLICIDADE CRUZADA + PARECER DUPLO");
        c.setRelogio(em("2026-04-05T09:00"));
        c.corrigirNomeDocumento(carlosExtensao2, "Certificado Oficina Python");
        // Mesma ação enviada em OUTRA comissão (tentativa de contagem dupla).
        Solicitacao dup = lucas.criarSolicitacao("Certificado Oficina Python", "g7h8-oficina-copia.pdf",
                ComissaoDestino.ATIVIDADES_COMPLEMENTARES, 20, "Mesma oficina", em("2026-04-06T09:00"));
        dup.setRelogio(em("2026-04-06T09:05"));
        dup.enviar(lucas);
        c.setRelogio(em("2026-04-10T15:00"));
        Parecer parecerC = c.emitirParecer(carlosExtensao2, 15, "Aprovadas 15h (carga comprovada).",
                "Aluno também enviou este certificado à comissão de Complementares.");
        c.setRelogio(em("2026-04-11T09:00"));
        c.aceitar(lucas);

        dup.setRelogio(em("2026-04-12T10:00"));
        System.out.println("  Marina (Complementares) vai analisar: " + dup);
        List<Solicitacao> semelhantes = lucas.buscarPossiveisDuplicidades(dup);
        if (!semelhantes.isEmpty()) {
            System.out.println("  *** ALERTA DE DUPLICIDADE: documentos semelhantes do mesmo discente ***");
            for (Solicitacao s : semelhantes) {
                System.out.println("      - " + s);
                for (Parecer p : s.getPareceres()) {
                    System.out.println("        parecer anterior: " + p + " | público: " + p.getObservacaoPublica());
                }
            }
        }
        Parecer parecerDup = dup.emitirParecer(marinaComplementares, 0,
                "Documento já contabilizado na comissão de Extensão.", "Duplicidade confirmada com a sol. de Extensão.");
        dup.setRelogio(em("2026-04-13T09:00"));
        dup.aceitar(lucas);

        System.out.println("\n  Visibilidade do parecer da Marina:");
        exibirParecerPara("DISCENTE", null, parecerDup);
        exibirParecerPara("SECRETARIO", PerfilFuncional.SECRETARIO, parecerDup);
        exibirParecerPara("COMISSAO_EXTENSAO", PerfilFuncional.COMISSAO_EXTENSAO, parecerC);

        // ============================================================
        titulo("7. EXCLUSÃO FÍSICA: só em CRIADA");
        Solicitacao rascunho = beatriz.criarSolicitacao("Rascunho", "x.pdf", ComissaoDestino.ESTAGIO, 10, "", em("2026-05-01T10:00"));
        Solicitacao enviada = beatriz.criarSolicitacao("Certificado Monitoria", "y.pdf",
                ComissaoDestino.ATIVIDADES_COMPLEMENTARES, 30, "", em("2026-05-01T10:10"));
        enviada.setRelogio(em("2026-05-01T10:15"));
        enviada.enviar(beatriz);
        System.out.println("  Solicitações da Beatriz antes: " + beatriz.getSolicitacoes().size());
        beatriz.excluirSolicitacao(rascunho);
        System.out.println("  Excluída a CRIADA. Depois: " + beatriz.getSolicitacoes().size());
        esperarErro("Excluir solicitação já ENVIADA", () -> beatriz.excluirSolicitacao(enviada));
        enviada.cancelar(beatriz);
        System.out.println("  ENVIADA só pode ser cancelada -> " + enviada.getStatus());

        // ============================================================
        titulo("8. CANCELAMENTO ADMINISTRATIVO de solicitação CONCLUIDA e restauração");
        b.setRelogio(em("2026-05-10T10:00"));
        esperarErro("Discente cancelando solicitação CONCLUIDA", () -> b.cancelar(beatriz));
        esperarErro("Alterar solicitação que não está CRIADA",
                () -> b.alterar(beatriz, "Novo título", null, ComissaoDestino.EXTENSAO, 10, ""));
        b.cancelarAdministrativamente(helenaCoord, "Documento invalidado pela secretaria.");
        System.out.println("  Status: " + b.getStatus() + " (anterior: " + b.getStatusAnteriorAoCancelamento() + ")");
        b.setRelogio(em("2026-05-11T10:00"));
        b.restaurar(helenaCoord);
        System.out.println("  Status após restauração: " + b.getStatus());

        // ============================================================
        titulo("9. CONSOLIDAÇÃO DE HORAS HOMOLOGADAS (Lucas)");
        for (ComissaoDestino comissao : ComissaoDestino.values()) {
            System.out.println("  " + comissao.getDescricao() + ": " + lucas.totalHorasHomologadas(comissao) + "h");
        }

        // ============================================================
        titulo("10. TRILHA DE AUDITORIA COMPLETA - Solicitação do Lucas (Extensão)");
        imprimirSolicitacao(c);
    }

    // ------------------------------------------------------------------
    // Métodos utilitários da simulação
    // ------------------------------------------------------------------

    /** Simula a lógica do Servlet de identificação: busca por matrícula; se não existir, cadastra. */
    private static Discente identificarOuCadastrar(String nome, String matricula) {
        Discente existente = BANCO_DISCENTES.get(matricula);
        if (existente != null) {
            System.out.println("  Matrícula " + matricula + " já existe -> carregando solicitações de " + existente.getNomeCompleto());
            return existente;
        }
        Discente novo = new Discente(nome, matricula);
        BANCO_DISCENTES.put(matricula, novo);
        System.out.println("  Matrícula " + matricula + " nova -> cadastrado automaticamente: " + novo);
        return novo;
    }

    private static void imprimirSolicitacao(Solicitacao s) {
        System.out.println("  " + s + "  discente: " + s.getDiscente());
        System.out.println("  Pareceres:");
        for (Parecer p : s.getPareceres()) {
            System.out.println("    * " + p);
        }
        System.out.println("  Histórico (auditoria):");
        for (HistoricoStatus h : s.getHistorico()) {
            System.out.println("    " + h);
        }
    }

    private static void exibirParecerPara(String quemVe, PerfilFuncional perfil, Parecer p) {
        String adm = p.observacaoAdministrativaVisivelPara(perfil)
                ? p.getObservacaoAdministrativa() : "(oculta)";
        System.out.println("    " + quemVe + " vê -> pública: \"" + p.getObservacaoPublica() + "\" | administrativa: " + adm);
    }

    /** Executa uma ação que DEVE ser bloqueada pelas regras de negócio. */
    private static void esperarErro(String descricao, Runnable acao) {
        try {
            acao.run();
            System.out.println("  [FALHA NO TESTE] deveria ter sido bloqueado: " + descricao);
        } catch (OperacaoInvalidaException e) {
            System.out.println("  [BLOQUEADO OK] " + descricao + "\n                 -> " + e.getMessage());
        }
    }

    private static void titulo(String texto) {
        System.out.println("\n==== " + texto + " ====");
    }

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    /** Relógio fixo em uma data/hora, usado para simular o passar do tempo. */
    private static Clock em(String isoDataHora) {
        ZoneId zona = ZoneId.systemDefault();
        return Clock.fixed(LocalDateTime.parse(isoDataHora).atZone(zona).toInstant(), zona);
    }
}
