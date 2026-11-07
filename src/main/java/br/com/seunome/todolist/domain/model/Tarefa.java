package br.com.seunome.todolist.domain.model;

import br.com.seunome.todolist.domain.exception.TransicaoInvalidaException;
import br.com.seunome.todolist.domain.exception.ValidacaoException;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Tarefa {

    private static final int TAMANHO_MAXIMO_TITULO = 120;

    private final long id;
    private final long usuarioId;
    private final LocalDateTime criadaEm;

    private String titulo;
    private String descricao;
    private LocalDateTime prazo;
    private Prioridade prioridade;
    private StatusTarefa status;
    private LocalDateTime concluidaEm;

    private Tarefa(Builder b) {
        this.id = b.id;
        this.usuarioId = b.usuarioId;
        this.criadaEm = b.criadaEm;
        this.titulo = validarTitulo(b.titulo);
        this.descricao = b.descricao == null ? "" : b.descricao.strip();
        this.prazo = b.prazo;
        this.prioridade = Objects.requireNonNull(b.prioridade, "prioridade");
        this.status = Objects.requireNonNull(b.status, "status");
        this.concluidaEm = b.concluidaEm;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ---- Comportamento de domínio (Tell, Don't Ask)

    public void alterarStatus(StatusTarefa novo, LocalDateTime agora) {
        Objects.requireNonNull(novo, "novo status");
        if (!status.podeTransicionarPara(novo)) {
            throw new TransicaoInvalidaException(id, status, novo);
        }
        this.status = novo;
        this.concluidaEm = (novo == StatusTarefa.CONCLUIDA) ? agora : null;
    }

    public void reagendar(LocalDateTime novoPrazo, LocalDateTime agora) {
        if (status.estaEncerrada()) {
            throw new ValidacaoException("Não é possível reagendar uma tarefa encerrada.");
        }
        if (novoPrazo != null && novoPrazo.isBefore(agora)) {
            throw new ValidacaoException("O novo prazo não pode estar no passado.");
        }
        this.prazo = novoPrazo;
    }

    public void renomear(String novoTitulo) {
        this.titulo = validarTitulo(novoTitulo);
    }

    public boolean estaAtrasada(LocalDateTime agora) {
        return prazo != null && !status.estaEncerrada() && prazo.isBefore(agora);
    }

    private static String validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new ValidacaoException("O título é obrigatório.");
        }
        String limpo = titulo.strip();
        if (limpo.length() > TAMANHO_MAXIMO_TITULO) {
            throw new ValidacaoException("O título deve ter no máximo " + TAMANHO_MAXIMO_TITULO + " caracteres.");
        }
        return limpo;
    }

    // ---- Getters

    public long getId() { return id; }
    public long getUsuarioId() { return usuarioId; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public LocalDateTime getPrazo() { return prazo; }
    public Prioridade getPrioridade() { return prioridade; }
    public StatusTarefa getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Tarefa outra && id == outra.id);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Tarefa[#%d '%s' %s %s prazo=%s]".formatted(id, titulo, prioridade, status, prazo);
    }

    // ---- Builder

    public static final class Builder {
        private Long id;
        private Long usuarioId;
        private String titulo;
        private String descricao = "";
        private LocalDateTime prazo;
        private Prioridade prioridade = Prioridade.MEDIA;
        private StatusTarefa status = StatusTarefa.PENDENTE;
        private LocalDateTime criadaEm;
        private LocalDateTime concluidaEm;

        private Builder() {}

        public Builder id(long id) { this.id = id; return this; }
        public Builder usuarioId(long usuarioId) { this.usuarioId = usuarioId; return this; }
        public Builder titulo(String titulo) { this.titulo = titulo; return this; }
        public Builder descricao(String descricao) { this.descricao = descricao; return this; }
        public Builder prazo(LocalDateTime prazo) { this.prazo = prazo; return this; }
        public Builder prioridade(Prioridade prioridade) { this.prioridade = prioridade; return this; }
        public Builder status(StatusTarefa status) { this.status = status; return this; }
        public Builder criadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; return this; }
        public Builder concluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; return this; }

        public Tarefa build() {
            if (id == null || usuarioId == null || criadaEm == null) {
                throw new ValidacaoException("Id, usuário e data de criação são obrigatórios.");
            }
            return new Tarefa(this);
        }
    }
}
