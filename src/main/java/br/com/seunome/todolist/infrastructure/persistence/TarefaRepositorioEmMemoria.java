package br.com.seunome.todolist.infrastructure.persistence;

import br.com.seunome.todolist.domain.model.Tarefa;
import br.com.seunome.todolist.domain.repository.TarefaRepositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TarefaRepositorioEmMemoria implements TarefaRepositorio {

    private final Map<Long, Tarefa> armazenamento = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    @Override public long proximoId() { return sequencia.incrementAndGet(); }

    @Override public Tarefa salvar(Tarefa tarefa) {
        armazenamento.put(tarefa.getId(), tarefa);
        return tarefa;
    }

    @Override public Optional<Tarefa> buscarPorId(Long id) {
        return Optional.ofNullable(armazenamento.get(id));
    }

    @Override public List<Tarefa> buscarTodos() {
        return List.copyOf(armazenamento.values());
    }

    @Override public boolean remover(Long id) {
        return armazenamento.remove(id) != null;
    }
}
