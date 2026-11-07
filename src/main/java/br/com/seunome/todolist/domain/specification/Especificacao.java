package br.com.seunome.todolist.domain.specification;

@FunctionalInterface
public interface Especificacao<T> {

    boolean eSatisfeitaPor(T candidato);

    default Especificacao<T> e(Especificacao<T> outra) {
        return c -> eSatisfeitaPor(c) && outra.eSatisfeitaPor(c);
    }

    default Especificacao<T> ou(Especificacao<T> outra) {
        return c -> eSatisfeitaPor(c) || outra.eSatisfeitaPor(c);
    }

    default Especificacao<T> nao() {
        return c -> !eSatisfeitaPor(c);
    }

    static <T> Especificacao<T> todas() {
        return c -> true;
    }
}
