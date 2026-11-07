package br.com.seunome.todolist.domain.event;

@FunctionalInterface
public interface OuvinteTarefa {
    void aoOcorrer(EventoTarefa evento);
}
