# Arquitetura

```
presentation  ──►  application  ──►  domain  ◄──  infrastructure
```

- **domain**: regras de negócio puras (sem dependência de nenhuma outra camada).
- **application**: casos de uso (`GerenciadorTarefas`), orquestram o domínio.
- **presentation**: interface de linha de comando (`MenuConsole`), que recebe entrada/saída por parâmetro para ser testável.
- **infrastructure**: implementações concretas (repositório em memória, lembretes em background).
- **Main**: *Composition Root*, onde a injeção de dependência é feita manualmente.

## Decisões
- Java puro, sem frameworks, para evidenciar os fundamentos.
- `Clock` injetável para testes determinísticos.
- Specification Pattern para filtros combináveis.
