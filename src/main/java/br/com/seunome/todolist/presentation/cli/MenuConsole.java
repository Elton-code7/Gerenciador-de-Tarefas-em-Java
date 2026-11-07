package br.com.seunome.todolist.presentation.cli;

import br.com.seunome.todolist.application.dto.CriarTarefaComando;
import br.com.seunome.todolist.application.service.GerenciadorTarefas;
import br.com.seunome.todolist.domain.exception.TodoListException;
import br.com.seunome.todolist.domain.exception.ValidacaoException;
import br.com.seunome.todolist.domain.model.Prioridade;
import br.com.seunome.todolist.domain.model.StatusTarefa;
import br.com.seunome.todolist.domain.model.Tarefa;
import br.com.seunome.todolist.domain.specification.Especificacao;
import br.com.seunome.todolist.domain.specification.TarefaEspecificacoes;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interface de linha de comando interativa.
 *
 * <p>Recebe entrada e saída por parâmetro (em vez de usar {@code System.in/out} direto),
 * o que permite testá-la sem terminal.
 */
public class MenuConsole {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int TAMANHO_MAXIMO_TITULO_EXIBIDO = 30;

    private static final String MENU = """

            ══════════ GERENCIADOR DE TAREFAS ══════════
              1) Criar tarefa
              2) Listar tarefas
              3) Iniciar tarefa
              4) Concluir tarefa
              5) Cancelar tarefa
              6) Reagendar prazo
              7) Remover tarefa
              8) Filtrar tarefas
              9) Ver tarefas atrasadas
             10) Estatísticas
              0) Sair
            ════════════════════════════════════════════""";

    private final GerenciadorTarefas gerenciador;
    private final Clock clock;
    private final long usuarioId;
    private final Scanner entrada;
    private final PrintStream saida;

    public MenuConsole(GerenciadorTarefas gerenciador, Clock clock, long usuarioId,
                       InputStream entrada, PrintStream saida) {
        this.gerenciador = gerenciador;
        this.clock = clock;
        this.usuarioId = usuarioId;
        this.entrada = new Scanner(entrada, StandardCharsets.UTF_8);
        this.saida = saida;
    }

    public void executar() {
        boolean continuar = true;
        while (continuar) {
            saida.println(MENU);
            String opcao = lerLinha("Escolha uma opção: ");
            if (opcao == null) {
                break; // fim da entrada (Ctrl+D)
            }
            continuar = processar(opcao.strip());
        }
        saida.println("\nAté logo! 👋");
    }

    // ------------------------------------------------------------------ Despacho

    private boolean processar(String opcao) {
        try {
            switch (opcao) {
                case "1" -> criar();
                case "2" -> imprimirTarefas(gerenciador.listar(usuarioId));
                case "3" -> gerenciador.iniciar(lerId());
                case "4" -> gerenciador.concluir(lerId());
                case "5" -> gerenciador.cancelar(lerId());
                case "6" -> reagendar();
                case "7" -> gerenciador.remover(lerId());
                case "8" -> filtrar();
                case "9" -> imprimirTarefas(gerenciador.listarAtrasadas(usuarioId));
                case "10" -> estatisticas();
                case "0" -> { return false; }
                default -> saida.println("⚠️  Opção inválida.");
            }
        } catch (NumberFormatException e) {
            saida.println("⚠️  Informe um número válido.");
        } catch (DateTimeParseException e) {
            saida.println("⚠️  Data inválida. Use dd/MM/yyyy HH:mm (ex.: 25/12/2026 18:30).");
        } catch (TodoListException e) {
            saida.println("⚠️  " + e.getMessage());
        }
        return true;
    }

    // ------------------------------------------------------------------ Ações

    private void criar() {
        String titulo = perguntar("Título: ");
        String descricao = perguntar("Descrição (opcional): ");
        LocalDateTime prazo = lerPrazo();
        Prioridade prioridade = lerEnum("Prioridade", Prioridade.values(), true);

        gerenciador.criar(new CriarTarefaComando(usuarioId, titulo, descricao, prazo, prioridade));
    }

    private void reagendar() {
        long id = lerId();
        LocalDateTime novoPrazo = lerPrazo();
        gerenciador.reagendar(id, novoPrazo);
        saida.println(novoPrazo == null
                ? "📅 Prazo removido da tarefa #" + id
                : "📅 Tarefa #" + id + " reagendada para " + FORMATO_DATA.format(novoPrazo));
    }

    private void filtrar() {
        saida.println("""
                Filtrar por:
                  1) Status
                  2) Prioridade mínima
                  3) Texto no título""");
        Especificacao<Tarefa> filtro = switch (perguntar("Escolha: ")) {
            case "1" -> TarefaEspecificacoes.comStatus(lerEnum("Status", StatusTarefa.values(), false));
            case "2" -> TarefaEspecificacoes.comPrioridadeMinima(lerEnum("Prioridade mínima", Prioridade.values(), false));
            case "3" -> TarefaEspecificacoes.tituloContem(perguntar("Texto: "));
            default -> throw new ValidacaoException("Opção de filtro inválida.");
        };
        imprimirTarefas(gerenciador.listar(usuarioId, filtro, GerenciadorTarefas.ORDEM_PADRAO));
    }

    private void estatisticas() {
        Map<StatusTarefa, Long> contagem = gerenciador.contarPorStatus(usuarioId);
        saida.println("\n📊 Estatísticas:");
        long total = 0;
        for (StatusTarefa status : StatusTarefa.values()) {
            long qtd = contagem.getOrDefault(status, 0L);
            total += qtd;
            saida.printf("  %-13s %d%n", status, qtd);
        }
        saida.println("  Total         " + total);
    }

    // ------------------------------------------------------------------ Exibição

    private void imprimirTarefas(List<Tarefa> tarefas) {
        if (tarefas.isEmpty()) {
            saida.println("\n(nenhuma tarefa encontrada)");
            return;
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        saida.printf("%n%-4s %-" + TAMANHO_MAXIMO_TITULO_EXIBIDO + "s %-8s %-13s %s%n",
                "ID", "TÍTULO", "PRIOR.", "STATUS", "PRAZO");
        for (Tarefa t : tarefas) {
            String prazo = t.getPrazo() == null ? "-" : FORMATO_DATA.format(t.getPrazo());
            if (t.estaAtrasada(agora)) {
                prazo += "  ⚠️ ATRASADA";
            }
            saida.printf("%-4d %-" + TAMANHO_MAXIMO_TITULO_EXIBIDO + "s %-8s %-13s %s%n",
                    t.getId(), truncar(t.getTitulo()), t.getPrioridade(), t.getStatus(), prazo);
        }
    }

    private static String truncar(String texto) {
        return texto.length() <= TAMANHO_MAXIMO_TITULO_EXIBIDO
                ? texto
                : texto.substring(0, TAMANHO_MAXIMO_TITULO_EXIBIDO - 1) + "…";
    }

    // ------------------------------------------------------------------ Leitura

    private long lerId() {
        return Long.parseLong(perguntar("ID da tarefa: "));
    }

    /** Linha vazia significa "sem prazo". */
    private LocalDateTime lerPrazo() {
        String texto = perguntar("Prazo [dd/MM/yyyy HH:mm] (Enter = sem prazo): ");
        return texto.isEmpty() ? null : LocalDateTime.parse(texto, FORMATO_DATA);
    }

    /** Mostra as constantes do enum numeradas e devolve a escolhida. */
    private <E extends Enum<E>> E lerEnum(String rotulo, E[] valores, boolean opcional) {
        StringBuilder opcoes = new StringBuilder(rotulo).append(":");
        for (int i = 0; i < valores.length; i++) {
            opcoes.append("  ").append(i + 1).append("-").append(valores[i]);
        }
        saida.println(opcoes);

        String resposta = perguntar(opcional ? "Escolha (Enter = padrão): " : "Escolha: ");
        if (resposta.isEmpty() && opcional) {
            return null;
        }
        int indice = Integer.parseInt(resposta);
        if (indice < 1 || indice > valores.length) {
            throw new ValidacaoException("Opção inválida para " + rotulo.toLowerCase() + ".");
        }
        return valores[indice - 1];
    }

    private String perguntar(String texto) {
        String linha = lerLinha(texto);
        return linha == null ? "" : linha.strip();
    }

    /** Devolve {@code null} quando a entrada acabou. */
    private String lerLinha(String prompt) {
        saida.print(prompt);
        saida.flush();
        return entrada.hasNextLine() ? entrada.nextLine() : null;
    }
}
