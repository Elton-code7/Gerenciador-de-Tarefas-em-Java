package br.com.seunome.todolist.application.service;

import br.com.seunome.todolist.application.dto.CriarTarefaComando;
import br.com.seunome.todolist.domain.event.EventoTarefa;
import br.com.seunome.todolist.domain.event.OuvinteTarefa;
import br.com.seunome.todolist.domain.exception.TarefaNaoEncontradaException;
import br.com.seunome.todolist.domain.exception.ValidacaoException;
import br.com.seunome.todolist.domain.model.Prioridade;
import br.com.seunome.todolist.domain.model.StatusTarefa;
import br.com.seunome.todolist.domain.model.Tarefa;
import br.com.seunome.todolist.domain.repository.TarefaRepositorio;
import br.com.seunome.todolist.domain.specification.Especificacao;
import br.com.seunome.todolist.domain.specification.TarefaEspecificacoes;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Orquestra os casos de uso de tarefas.
 *
 * <p>Depende apenas de abstrações ({@link TarefaRepositorio}, {@link Clock}),
 * o que o torna totalmente testável sem I/O e sem relógio real.
 */
public class GerenciadorTarefas {

    private static final System.Logger LOG = System.getLogger(GerenciadorTarefas.class.getName());

    /** Ordem padrão: prioridade (maior primeiro), depois prazo mais próximo, depois id. */
    public static final Comparator<Tarefa> ORDEM_PADRAO = Comparator
            .comparing(Tarefa::getPrioridade, Comparator.reverseOrder())
            .thenComparing(Tarefa::getPrazo, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Tarefa::getId);

    private final TarefaRepositorio repositorio;
    private final Clock clock;
    private final List<OuvinteTarefa> ouvintes = new CopyOnWriteArrayList<>();

    public GerenciadorTarefas(TarefaRepositorio repositorio, Clock clock) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // ------------------------------------------------------------------ Observer

    public void registrarOuvinte(OuvinteTarefa ouvinte) {
        ouvintes.add(Objects.requireNonNull(ouvinte, "ouvinte"));
    }

    // ------------------------------------------------------------------ Comandos

    public synchronized Tarefa criar(CriarTarefaComando comando) {
        LocalDateTime agora = agora();

        if (comando.prazo() != null && comando.prazo().isBefore(agora)) {
            throw new ValidacaoException("O prazo não pode estar no passado.");
        }

        Tarefa tarefa = Tarefa.builder()
                .id(repositorio.proximoId())
                .usuarioId(comando.usuarioId())
                .titulo(comando.titulo())
                .descricao(comando.descricao())
                .prazo(comando.prazo())
                .prioridade(comando.prioridade() != null ? comando.prioridade() : Prioridade.MEDIA)
                .criadaEm(agora)
                .build();

        repositorio.salvar(tarefa);
        notificar(new EventoTarefa.Criada(tarefa, agora));
        return tarefa;
    }

    public synchronized Tarefa iniciar(long id) {
        return transicionar(id, StatusTarefa.EM_ANDAMENTO);
    }

    public synchronized Tarefa concluir(long id) {
        return transicionar(id, StatusTarefa.CONCLUIDA);
    }

    public synchronized Tarefa cancelar(long id) {
        return transicionar(id, StatusTarefa.CANCELADA);
    }

    public synchronized Tarefa reagendar(long id, LocalDateTime novoPrazo) {
        Tarefa tarefa = obterOuFalhar(id);
        tarefa.reagendar(novoPrazo, agora());
        return repositorio.salvar(tarefa);
    }

    public synchronized void remover(long id) {
        Tarefa tarefa = obterOuFalhar(id);
        repositorio.remover(id);
        notificar(new EventoTarefa.Removida(tarefa, agora()));
    }

    // ------------------------------------------------------------------ Consultas

    public Tarefa buscarPorId(long id) {
        return obterOuFalhar(id);
    }

    public List<Tarefa> listar(long usuarioId, Especificacao<Tarefa> filtro, Comparator<Tarefa> ordem) {
        Especificacao<Tarefa> especificacao = TarefaEspecificacoes.doUsuario(usuarioId).e(filtro);
        return repositorio.buscarPorEspecificacao(especificacao).stream()
                .sorted(ordem)
                .toList();
    }

    public List<Tarefa> listar(long usuarioId) {
        return listar(usuarioId, Especificacao.todas(), ORDEM_PADRAO);
    }

    public List<Tarefa> listarAtrasadas(long usuarioId) {
        return listar(usuarioId, TarefaEspecificacoes.atrasadas(agora()), ORDEM_PADRAO);
    }

    /** Usado pelo serviço de lembretes (tarefas abertas que vencem dentro da janela). */
    public List<Tarefa> listarVencendoEm(Duration janela) {
        LocalDateTime agora = agora();
        return repositorio.buscarPorEspecificacao(
                TarefaEspecificacoes.vencendoEntre(agora, agora.plus(janela)));
    }

    public Map<StatusTarefa, Long> contarPorStatus(long usuarioId) {
        return repositorio.buscarPorEspecificacao(TarefaEspecificacoes.doUsuario(usuarioId)).stream()
                .collect(Collectors.groupingBy(
                        Tarefa::getStatus,
                        () -> new EnumMap<>(StatusTarefa.class),
                        Collectors.counting()));
    }

    // ------------------------------------------------------------------ Internos

    private Tarefa transicionar(long id, StatusTarefa destino) {
        Tarefa tarefa = obterOuFalhar(id);
        StatusTarefa anterior = tarefa.getStatus();
        LocalDateTime agora = agora();

        tarefa.alterarStatus(destino, agora);
        repositorio.salvar(tarefa);

        notificar(new EventoTarefa.StatusAlterado(tarefa, anterior, agora));
        return tarefa;
    }

    private Tarefa obterOuFalhar(long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    /** Um ouvinte defeituoso nunca deve quebrar o fluxo de negócio. */
    private void notificar(EventoTarefa evento) {
        for (OuvinteTarefa ouvinte : ouvintes) {
            try {
                ouvinte.aoOcorrer(evento);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Falha em ouvinte de evento: " + evento, e);
            }
        }
    }
}
