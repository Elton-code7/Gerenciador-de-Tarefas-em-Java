package br.com.seunome.todolist.domain.event;

import br.com.seunome.todolist.domain.model.StatusTarefa;
import br.com.seunome.todolist.domain.model.Tarefa;

import java.time.LocalDateTime;

public sealed interface EventoTarefa {

    Tarefa tarefa();
    LocalDateTime ocorridoEm();

    record Criada(Tarefa tarefa, LocalDateTime ocorridoEm) implements EventoTarefa {}

    record StatusAlterado(Tarefa tarefa, StatusTarefa anterior, LocalDateTime ocorridoEm)
            implements EventoTarefa {}

    record Removida(Tarefa tarefa, LocalDateTime ocorridoEm) implements EventoTarefa {}
}
