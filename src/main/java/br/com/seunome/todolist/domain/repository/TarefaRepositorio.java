package br.com.seunome.todolist.domain.repository;

import br.com.seunome.todolist.domain.model.Tarefa;
import br.com.seunome.todolist.domain.specification.Especificacao;

import java.util.List;

public interface TarefaRepositorio extends Repositorio<Tarefa, Long> {

    /** Gera o próximo identificador de forma thread-safe. */
    long proximoId();

    default List<Tarefa> buscarPorEspecificacao(Especificacao<Tarefa> especificacao) {
        return buscarTodos().stream()
                .filter(especificacao::eSatisfeitaPor)
                .toList();
    }
}
