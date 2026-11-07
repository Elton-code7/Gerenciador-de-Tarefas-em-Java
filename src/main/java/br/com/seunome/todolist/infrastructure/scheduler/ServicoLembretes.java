package br.com.seunome.todolist.infrastructure.scheduler;

import br.com.seunome.todolist.application.service.GerenciadorTarefas;
import br.com.seunome.todolist.domain.model.Tarefa;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Verifica periodicamente, em uma thread de background, as tarefas prestes a vencer
 * e avisa cada uma delas uma única vez.
 */
public class ServicoLembretes implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(ServicoLembretes.class.getName());

    private final GerenciadorTarefas gerenciador;
    private final Duration janela;
    private final Duration intervalo;
    private final Consumer<Tarefa> acao;
    private final Set<Long> jaNotificadas = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "servico-lembretes");
        t.setDaemon(true);
        return t;
    });

    public ServicoLembretes(GerenciadorTarefas gerenciador, Duration janela,
                            Duration intervalo, Consumer<Tarefa> acao) {
        this.gerenciador = gerenciador;
        this.janela = janela;
        this.intervalo = intervalo;
        this.acao = acao;
    }

    public void iniciar() {
        executor.scheduleAtFixedRate(this::verificar, 0, intervalo.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void verificar() {
        try {
            gerenciador.listarVencendoEm(janela).stream()
                    .filter(t -> jaNotificadas.add(t.getId()))
                    .forEach(acao);
        } catch (RuntimeException e) {
            // uma exceção não tratada cancelaria silenciosamente as próximas execuções
            LOG.log(System.Logger.Level.ERROR, "Falha ao verificar lembretes", e);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
