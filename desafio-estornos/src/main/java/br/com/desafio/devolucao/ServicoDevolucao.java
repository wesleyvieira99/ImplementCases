package br.com.desafio.devolucao;

public final class ServicoDevolucao {

    private final RepositorioDevolucoes repositorio;

    public ServicoDevolucao(RepositorioDevolucoes repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException(
                    "repositorio não pode ser nulo"
            );
        }

        this.repositorio = repositorio;
    }

    public void registrarTransacao(
            String transacaoId,
            long valorCentavos
    ) throws IllegalAccessException {
        validarIdentificador(transacaoId, "transacaoId");
        validarValor(valorCentavos);

        repositorio.cadastrar(
                transacaoId,
                valorCentavos
        );
    }

    public ResultadoDevolucao devolver(PedidoDevolucao pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException(
                    "pedido não pode ser nulo"
            );
        }

        validarIdentificador(
                pedido.transacaoId(),
                "transacaoId"
        );

        validarIdentificador(
                pedido.chaveIdempotencia(),
                "chaveIdempotencia"
        );

        validarValor(pedido.valorCentavos());

        return repositorio.executarComTransacao(
                pedido.transacaoId(),
                transacao -> transacao.devolver(pedido)
        );
    }

    public long totalDevolvido(String transacaoId) {
        validarIdentificador(
                transacaoId,
                "transacaoId"
        );

        return repositorio.executarComTransacao(
                transacaoId,
                transacao -> transacao.totalDevolvido()
        );
    }

    private static void validarIdentificador(
            String valor,
            String campo
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    campo + " não pode ser nulo ou em branco"
            );
        }
    }

    private static void validarValor(long valorCentavos) {
        if (valorCentavos <= 0) {
            throw new IllegalArgumentException(
                    "valorCentavos deve ser positivo"
            );
        }
    }
}