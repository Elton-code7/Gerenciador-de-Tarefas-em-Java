package br.com.seunome.todolist.domain.exception;

public class TarefaNaoEncontradaException extends TodoListException {
    private static final long serialVersionUID = 1L;

    public TarefaNaoEncontradaException(long id) {
        super("Tarefa não encontrada: id=" + id);
    }
}
