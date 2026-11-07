package br.com.seunome.todolist.domain.specification;

import br.com.seunome.todolist.domain.model.Prioridade;
import br.com.seunome.todolist.domain.model.StatusTarefa;
import br.com.seunome.todolist.domain.model.Tarefa;

import java.time.LocalDateTime;

public final class TarefaEspecificacoes {

    private TarefaEspecificacoes() {}

    public static Especificacao<Tarefa> doUsuario(long usuarioId) {
        return t -> t.getUsuarioId() == usuarioId;
    }

    public static Especificacao<Tarefa> comStatus(StatusTarefa status) {
        return t -> t.getStatus() == status;
    }

    public static Especificacao<Tarefa> comPrioridadeMinima(Prioridade minima) {
        return t -> t.getPrioridade().compareTo(minima) >= 0;
    }

    public static Especificacao<Tarefa> tituloContem(String texto) {
        String busca = texto.toLowerCase();
        return t -> t.getTitulo().toLowerCase().contains(busca);
    }

    public static Especificacao<Tarefa> atrasadas(LocalDateTime agora) {
        return t -> t.estaAtrasada(agora);
    }

    /** Tarefas abertas com prazo no intervalo [inicio, fim]. */
    public static Especificacao<Tarefa> vencendoEntre(LocalDateTime inicio, LocalDateTime fim) {
        return t -> t.getPrazo() != null
                && !t.getStatus().estaEncerrada()
                && !t.getPrazo().isBefore(inicio)
                && !t.getPrazo().isAfter(fim);
    }
}
