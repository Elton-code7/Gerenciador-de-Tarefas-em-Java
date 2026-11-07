package br.com.seunome.todolist;

import br.com.seunome.todolist.application.service.GerenciadorTarefas;
import br.com.seunome.todolist.domain.event.EventoTarefa;
import br.com.seunome.todolist.infrastructure.persistence.TarefaRepositorioEmMemoria;
import br.com.seunome.todolist.infrastructure.scheduler.ServicoLembretes;
import br.com.seunome.todolist.presentation.cli.MenuConsole;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

/** Composition Root: único lugar que conhece as implementações concretas. */
public final class Main {

    private static final long USUARIO_PADRAO = 1L;

    private Main() {}

    public static void main(String[] args) {
        PrintStream saida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Clock relogio = Clock.systemDefaultZone();

        var gerenciador = new GerenciadorTarefas(new TarefaRepositorioEmMemoria(), relogio);

        // Observer: feedback imediato de cada mudança (switch exaustivo sobre a sealed interface)
        gerenciador.registrarOuvinte(evento -> {
            String mensagem = switch (evento) {
                case EventoTarefa.Criada e         -> "✅ Tarefa #%d criada: %s".formatted(
                        e.tarefa().getId(), e.tarefa().getTitulo());
                case EventoTarefa.StatusAlterado e -> "🔄 Tarefa #%d: %s → %s".formatted(
                        e.tarefa().getId(), e.anterior(), e.tarefa().getStatus());
                case EventoTarefa.Removida e       -> "🗑️  Tarefa #%d removida.".formatted(e.tarefa().getId());
            };
            saida.println(mensagem);
        });

        // Lembretes em background: avisa (uma vez) tarefas que vencem na próxima hora
        try (var lembretes = new ServicoLembretes(gerenciador, Duration.ofHours(1), Duration.ofSeconds(30),
                t -> saida.println("\n⏰ LEMBRETE: '%s' vence em %s".formatted(t.getTitulo(), t.getPrazo())))) {
            lembretes.iniciar();
            new MenuConsole(gerenciador, relogio, USUARIO_PADRAO, System.in, saida).executar();
        }
    }
}
