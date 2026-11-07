package br.com.seunome.todolist.domain.exception;

public class TodoListException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TodoListException(String mensagem) { super(mensagem); }
}
