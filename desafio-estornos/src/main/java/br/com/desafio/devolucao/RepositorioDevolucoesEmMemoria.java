package br.com.desafio.devolucao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class RepositorioDevolucoesEmMemoria
        implements RepositorioDevolucoes {

    private final ConcurrentHashMap<String, Transacao> transacoes =
            new ConcurrentHashMap<>();

    @Override
    public void cadastrar(
            String transacaoId,
            long valorCentavos
    ) throws IllegalAccessException {

        Transacao nova = new Transacao(
                transacaoId,
                valorCentavos
        );

        Transacao existente = transacoes.putIfAbsent(
                transacaoId,
                nova
        );

        if (existente != null) {
            throw new IllegalAccessException(
                    "Transacao ja cadastrada: " + transacaoId
            );
        }
    }

    @Override
    public <T> T executarComTransacao(
            String transacaoId,
            Function<Transacao, T> operacao
    ) {
        Transacao transacao = transacoes.get(transacaoId);
        if(transacao == null){
            throw new TransacaoNaoEncontradaException(
                    "Transacao nao encontrada: " + transacaoId
            );
        }

        synchronized (transacao) {
            return operacao.apply(transacao);
        }
    }
}
