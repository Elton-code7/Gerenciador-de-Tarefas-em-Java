package br.com.seunome.todolist.domain.exception;

import br.com.seunome.todolist.domain.model.StatusTarefa;

public class TransicaoInvalidaException extends TodoListException {
    private static final long serialVersionUID = 1L;

    public TransicaoInvalidaException(long id, StatusTarefa de, StatusTarefa para) {
        super("Tarefa #%d não pode ir de %s para %s.".formatted(id, de, para));
    }
}
