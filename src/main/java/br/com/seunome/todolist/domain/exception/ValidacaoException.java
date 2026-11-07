package br.com.seunome.todolist.domain.exception;

public class ValidacaoException extends TodoListException {
    private static final long serialVersionUID = 1L;

    public ValidacaoException(String mensagem) { super(mensagem); }
}
