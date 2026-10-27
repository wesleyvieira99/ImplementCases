package br.com.desafio.devolucao;

public record ResultadoDevolucao(
        String idDevolucao,
        String transacaoId,
        String chaveIdempotencia,
        long valorCentavos
) {
}
