package br.com.seunome.todolist.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistência.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo do identificador
 */
public interface Repositorio<T, ID> {

    T salvar(T entidade);

    Optional<T> buscarPorId(ID id);

    List<T> buscarTodos();

    boolean remover(ID id);

    default boolean existe(ID id) {
        return buscarPorId(id).isPresent();
    }
}
