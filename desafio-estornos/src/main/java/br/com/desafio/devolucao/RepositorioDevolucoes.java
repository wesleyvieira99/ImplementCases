package br.com.desafio.devolucao;

import java.util.function.Function;

public interface RepositorioDevolucoes {

    void cadastrar(
            String transacaoId,
            long valorCentavos
    ) throws IllegalAccessException;

    /**
     * Executa a operação com exclusão mútua por transação.
     *
     * O callback não deve guardar nem retornar a referência
     * de Transacao, nem acessar outra transação.
     *
     * Retorne somente dados imutáveis.
     *
     * Esta operação fornece exclusão mútua,
     * não rollback automático.
     */
    <T> T executarComTransacao(
            String transacaoId,
            Function<Transacao, T> operacao
    );
}
