package br.com.seunome.todolist.presentation.cli;

import br.com.seunome.todolist.application.service.GerenciadorTarefas;
import br.com.seunome.todolist.domain.model.Prioridade;
import br.com.seunome.todolist.domain.model.Tarefa;
import br.com.seunome.todolist.infrastructure.persistence.TarefaRepositorioEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuConsoleTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC);

    private GerenciadorTarefas gerenciador;
    private ByteArrayOutputStream buffer;

    @BeforeEach
    void preparar() {
        gerenciador = new GerenciadorTarefas(new TarefaRepositorioEmMemoria(), RELOGIO);
        buffer = new ByteArrayOutputStream();
    }

    /** Executa o menu com as linhas digitadas e devolve tudo que foi impresso. */
    private String executarComEntrada(String... linhas) {
        String entrada = String.join("\n", linhas) + "\n";
        var menu = new MenuConsole(gerenciador, RELOGIO, 1L,
                new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        menu.executar();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void criaTarefaPeloMenu() {
        // 1=criar, título, descrição, prazo (vazio), prioridade 3=ALTA, 2=listar, 0=sair
        String saida = executarComEntrada("1", "Estudar Java", "capítulo de streams", "", "3", "2", "0");

        List<Tarefa> tarefas = gerenciador.listar(1L);
        assertEquals(1, tarefas.size());
        assertEquals(Prioridade.ALTA, tarefas.get(0).getPrioridade());
        assertTrue(saida.contains("Estudar Java"));
    }

    @Test
    void entradasInvalidasNaoDerrubamOMenu() {
        String saida = executarComEntrada("99", "3", "abc", "1", "", "", "", "", "0");

        assertTrue(saida.contains("Opção inválida"));
        assertTrue(saida.contains("número válido"));
        assertTrue(saida.contains("O título é obrigatório"));
        assertTrue(saida.contains("Até logo"));
    }

    @Test
    void dataInvalidaMostraMensagemAmigavel() {
        String saida = executarComEntrada("1", "Tarefa", "", "amanhã", "0");
        assertTrue(saida.contains("Data inválida"));
    }

    @Test
    void fimDaEntradaEncerraSemErro() {
        String saida = executarComEntrada();
        assertTrue(saida.contains("Até logo"));
    }
}
