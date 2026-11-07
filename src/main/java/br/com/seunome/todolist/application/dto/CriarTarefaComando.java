package br.com.seunome.todolist.application.dto;

import br.com.seunome.todolist.domain.model.Prioridade;
import java.time.LocalDateTime;

public record CriarTarefaComando(
        long usuarioId,
        String titulo,
        String descricao,
        LocalDateTime prazo,
        Prioridade prioridade) {}
