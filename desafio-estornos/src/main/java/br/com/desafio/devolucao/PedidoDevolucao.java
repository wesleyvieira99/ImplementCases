package br.com.desafio.devolucao;

public record PedidoDevolucao(
        String transacaoId,
        String chaveIdempotencia,
        long valorCentavos
) {
}
