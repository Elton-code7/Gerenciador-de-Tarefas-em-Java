package br.com.seunome.todolist.domain.model;

import br.com.seunome.todolist.domain.exception.TransicaoInvalidaException;
import br.com.seunome.todolist.domain.exception.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TarefaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 1, 10, 12, 0);

    private Tarefa novaTarefa(LocalDateTime prazo) {
        return Tarefa.builder().id(1).usuarioId(1).titulo("Teste")
                .prazo(prazo).criadaEm(AGORA).build();
    }

    @Test
    void naoPermiteTituloEmBranco() {
        var builder = Tarefa.builder().id(1).usuarioId(1).titulo("  ").criadaEm(AGORA);
        assertThrows(ValidacaoException.class, builder::build);
    }

    @Test
    void concluirRegistraDataDeConclusao() {
        Tarefa t = novaTarefa(null);
        t.alterarStatus(StatusTarefa.CONCLUIDA, AGORA);
        assertEquals(AGORA, t.getConcluidaEm());
    }

    @Test
    void tarefaConcluidaNaoPodeMudarDeStatus() {
        Tarefa t = novaTarefa(null);
        t.alterarStatus(StatusTarefa.CONCLUIDA, AGORA);
        assertThrows(TransicaoInvalidaException.class,
                () -> t.alterarStatus(StatusTarefa.EM_ANDAMENTO, AGORA));
    }

    @Test
    void detectaAtraso() {
        Tarefa t = novaTarefa(AGORA.minusDays(1));
        assertTrue(t.estaAtrasada(AGORA));
        t.alterarStatus(StatusTarefa.CONCLUIDA, AGORA);
        assertFalse(t.estaAtrasada(AGORA));
    }
}
