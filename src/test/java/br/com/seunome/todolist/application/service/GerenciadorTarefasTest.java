package br.com.seunome.todolist.application.service;

import br.com.seunome.todolist.application.dto.CriarTarefaComando;
import br.com.seunome.todolist.domain.exception.TarefaNaoEncontradaException;
import br.com.seunome.todolist.domain.exception.ValidacaoException;
import br.com.seunome.todolist.domain.model.Prioridade;
import br.com.seunome.todolist.domain.model.StatusTarefa;
import br.com.seunome.todolist.domain.model.Tarefa;
import br.com.seunome.todolist.infrastructure.persistence.TarefaRepositorioEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GerenciadorTarefasTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);

    private GerenciadorTarefas gerenciador;

    @BeforeEach
    void preparar() {
        gerenciador = new GerenciadorTarefas(new TarefaRepositorioEmMemoria(), RELOGIO);
    }

    private Tarefa criar(String titulo, LocalDateTime prazo, Prioridade prioridade) {
        return gerenciador.criar(new CriarTarefaComando(1L, titulo, "", prazo, prioridade));
    }

    @Test
    void naoCriaTarefaComPrazoNoPassado() {
        assertThrows(ValidacaoException.class,
                () -> criar("Velha", AGORA.minusDays(1), Prioridade.BAIXA));
    }

    @Test
    void usaPrioridadeMediaPorPadrao() {
        assertEquals(Prioridade.MEDIA, criar("Padrão", null, null).getPrioridade());
    }

    @Test
    void ordenaPorPrioridadeDepoisPrazo() {
        criar("Baixa", AGORA.plusDays(1), Prioridade.BAIXA);
        criar("Critica", AGORA.plusDays(5), Prioridade.CRITICA);
        criar("Alta cedo", AGORA.plusDays(1), Prioridade.ALTA);
        criar("Alta tarde", AGORA.plusDays(3), Prioridade.ALTA);

        List<String> titulos = gerenciador.listar(1L).stream().map(Tarefa::getTitulo).toList();
        assertEquals(List.of("Critica", "Alta cedo", "Alta tarde", "Baixa"), titulos);
    }

    @Test
    void contaTarefasPorStatus() {
        Tarefa a = criar("A", null, null);
        criar("B", null, null);
        gerenciador.concluir(a.getId());

        var contagem = gerenciador.contarPorStatus(1L);
        assertEquals(1L, contagem.get(StatusTarefa.CONCLUIDA));
        assertEquals(1L, contagem.get(StatusTarefa.PENDENTE));
    }

    @Test
    void listaTarefasQueVencemNaJanela() {
        criar("Em 30min", AGORA.plusMinutes(30), null);
        criar("Em 3 dias", AGORA.plusDays(3), null);

        var vencendo = gerenciador.listarVencendoEm(Duration.ofHours(1));
        assertEquals(1, vencendo.size());
        assertEquals("Em 30min", vencendo.get(0).getTitulo());
    }

    @Test
    void notificaOuvintesEIsolaFalhas() {
        List<String> recebidos = new ArrayList<>();
        gerenciador.registrarOuvinte(e -> { throw new IllegalStateException("ouvinte quebrado"); });
        gerenciador.registrarOuvinte(e -> recebidos.add(e.getClass().getSimpleName()));

        criar("X", null, null);
        assertEquals(List.of("Criada"), recebidos);
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        assertThrows(TarefaNaoEncontradaException.class, () -> gerenciador.buscarPorId(999));
    }
}
