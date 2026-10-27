package br.com.desafio.devolucao;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Transacao {

    private final String id;
    private final long valorOriginalCentavos;

    private final Map<String, ResultadoDevolucao> aprovacoes =
            new HashMap<>();

    private long totalDevolvidoCentavos;

    Transacao(String id, long valorOriginalCentavos) {
        this.id = id;
        this.valorOriginalCentavos = valorOriginalCentavos;
    }

    ResultadoDevolucao devolver(PedidoDevolucao pedido) {
        ResultadoDevolucao anterior = aprovacoes.get(
                pedido.chaveIdempotencia()
        );

        if (anterior != null) {
            if (anterior.valorCentavos() != pedido.valorCentavos()) {
                throw new ConflitoIdempotenciaException(
                        "Chave já aprovada com outro valor: "
                                + pedido.chaveIdempotencia()
                );
            }

            return anterior;
        }

        long saldoDisponivel =
                valorOriginalCentavos - totalDevolvidoCentavos;

        if (pedido.valorCentavos() > saldoDisponivel) {
            throw new SaldoDevolucaoInsuficienteException(
                    "Saldo insuficiente para a transação: " + id
            );
        }

        long novoTotal = Math.addExact(
                totalDevolvidoCentavos,
                pedido.valorCentavos()
        );

        ResultadoDevolucao resultado = new ResultadoDevolucao(
                UUID.randomUUID().toString(),
                id,
                pedido.chaveIdempotencia(),
                pedido.valorCentavos()
        );

        aprovacoes.put(pedido.chaveIdempotencia(), resultado);
        totalDevolvidoCentavos = novoTotal;

        return resultado;
    }

    long totalDevolvido() {
        return totalDevolvidoCentavos;
    }
}