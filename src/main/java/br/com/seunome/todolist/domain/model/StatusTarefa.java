package br.com.seunome.todolist.domain.model;

public enum StatusTarefa {
    PENDENTE, EM_ANDAMENTO, CONCLUIDA, CANCELADA;

    public boolean podeTransicionarPara(StatusTarefa destino) {
        return switch (this) {
            case PENDENTE -> destino == EM_ANDAMENTO || destino == CONCLUIDA || destino == CANCELADA;
            case EM_ANDAMENTO -> destino == PENDENTE || destino == CONCLUIDA || destino == CANCELADA;
            case CONCLUIDA, CANCELADA -> false;
        };
    }

    public boolean estaEncerrada() {
        return this == CONCLUIDA || this == CANCELADA;
    }
}
