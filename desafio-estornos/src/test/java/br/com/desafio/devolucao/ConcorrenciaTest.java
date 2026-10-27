package br.com.desafio.devolucao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class ConcorrenciaTest {

    private RepositorioDevolucoes repositorio;
    private ServicoDevolucao servico;

    @BeforeEach
    void preparar() throws IllegalAccessException {
        repositorio = new RepositorioDevolucoesEmMemoria();
        servico = new ServicoDevolucao(repositorio);

        servico.registrarTransacao("pix-1", 10_000L);
    }

    @Test
    void retriesConcorrentesEmVariosServicosDevemGerarUmaAprovacao()
            throws Exception {

        var pedido = new PedidoDevolucao(
                "pix-1",
                "req-a",
                10_000L
        );

        var tarefas =
                new ArrayList<Callable<ResultadoDevolucao>>();

        for (int i = 0; i < 12; i++) {
            var outroServico =
                    new ServicoDevolucao(repositorio);

            tarefas.add(
                    () -> outroServico.devolver(pedido)
            );
        }

        List<ResultadoDevolucao> resultados =
                executarSimultaneamente(tarefas);

        for (var resultado : resultados) {
            assertEquals(
                    resultados.get(0),
                    resultado
            );
        }

        assertEquals(
                10_000L,
                servico.totalDevolvido("pix-1")
        );
    }

    @Test
    void chavesDiferentesConcorrentesNaoDevemUltrapassarSaldo()
            throws Exception {

        var outroServico =
                new ServicoDevolucao(repositorio);

        List<Object> resultados = executarSimultaneamente(
                List.of(
                        () -> devolverOuCapturarRejeicao(
                                servico,
                                new PedidoDevolucao(
                                        "pix-1",
                                        "req-a",
                                        7_000L
                                )
                        ),
                        () -> devolverOuCapturarRejeicao(
                                outroServico,
                                new PedidoDevolucao(
                                        "pix-1",
                                        "req-b",
                                        7_000L
                                )
                        )
                )
        );

        long aprovados = resultados.stream()
                .filter(ResultadoDevolucao.class::isInstance)
                .count();

        long rejeitados = resultados.stream()
                .filter(
                        SaldoDevolucaoInsuficienteException.class::isInstance
                )
                .count();

        assertAll(
                () -> assertEquals(1L, aprovados),
                () -> assertEquals(1L, rejeitados),
                () -> assertEquals(
                        7_000L,
                        servico.totalDevolvido("pix-1")
                )
        );
    }

    @Test
    void mesmaChaveConcorrenteComValoresDiferentesDeveGerarUmConflito()
            throws Exception {

        var outroServico =
                new ServicoDevolucao(repositorio);

        List<Object> resultados = executarSimultaneamente(
                List.of(
                        () -> devolverOuCapturarRejeicao(
                                servico,
                                new PedidoDevolucao(
                                        "pix-1",
                                        "req-a",
                                        7_000L
                                )
                        ),
                        () -> devolverOuCapturarRejeicao(
                                outroServico,
                                new PedidoDevolucao(
                                        "pix-1",
                                        "req-a",
                                        8_000L
                                )
                        )
                )
        );

        var aprovacoes = resultados.stream()
                .filter(ResultadoDevolucao.class::isInstance)
                .map(ResultadoDevolucao.class::cast)
                .toList();

        long conflitos = resultados.stream()
                .filter(
                        ConflitoIdempotenciaException.class::isInstance
                )
                .count();

        assertEquals(1, aprovacoes.size());
        assertEquals(1L, conflitos);

        assertEquals(
                aprovacoes.get(0).valorCentavos(),
                servico.totalDevolvido("pix-1")
        );
    }

    @Test
    void cadastrosConcorrentesDevemPreservarSomenteUmOriginal()
            throws Exception {

        var outroServico =
                new ServicoDevolucao(repositorio);

        List<Object> resultados = executarSimultaneamente(
                List.of(
                        () -> cadastrarOuCapturarDuplicado(
                                servico,
                                10_000L
                        ),
                        () -> cadastrarOuCapturarDuplicado(
                                outroServico,
                                20_000L
                        )
                )
        );

        var valoresCadastrados = resultados.stream()
                .filter(Long.class::isInstance)
                .map(Long.class::cast)
                .toList();

        long duplicados = resultados.stream()
                .filter(
                        IllegalArgumentException.class::isInstance
                )
                .count();

        assertEquals(1, valoresCadastrados.size());
        assertEquals(
                0L,
                servico.totalDevolvido("pix-cadastro")
        );

        long valorOriginal = valoresCadastrados.get(0);

        servico.devolver(
                new PedidoDevolucao(
                        "pix-cadastro",
                        "req-a",
                        valorOriginal
                )
        );

        assertEquals(
                valorOriginal,
                servico.totalDevolvido("pix-cadastro")
        );

        assertThrows(
                SaldoDevolucaoInsuficienteException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-cadastro",
                                "req-b",
                                1L
                        )
                )
        );
    }

    @Test
    void transacoesDistintasDevemProgredirIndependentemente()
            throws Exception {

        servico.registrarTransacao(
                "pix-2",
                20_000L
        );

        var entrouNaPrimeira = new CountDownLatch(1);
        var liberarPrimeira = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Long> primeira = executor.submit(
                    () -> repositorio.executarComTransacao(
                            "pix-1",
                            transacao -> {
                                entrouNaPrimeira.countDown();

                                try {
                                    assertTrue(
                                            liberarPrimeira.await(
                                                    5,
                                                    TimeUnit.SECONDS
                                            ),
                                            "A primeira transação deve ser liberada pelo teste"
                                    );
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();

                                    throw new IllegalStateException(
                                            "Teste interrompido",
                                            e
                                    );
                                }

                                return transacao.totalDevolvido();
                            }
                    )
            );

            assertTrue(
                    entrouNaPrimeira.await(
                            5,
                            TimeUnit.SECONDS
                    )
            );

            Future<ResultadoDevolucao> segunda =
                    executor.submit(
                            () -> servico.devolver(
                                    new PedidoDevolucao(
                                            "pix-2",
                                            "req-a",
                                            7_000L
                                    )
                            )
                    );

            var resultado = segunda.get(
                    3,
                    TimeUnit.SECONDS
            );

            assertEquals(
                    7_000L,
                    resultado.valorCentavos()
            );

            assertFalse(primeira.isDone());

            liberarPrimeira.countDown();

            assertEquals(
                    0L,
                    primeira.get(
                            5,
                            TimeUnit.SECONDS
                    ).longValue()
            );

            assertEquals(
                    7_000L,
                    servico.totalDevolvido("pix-2")
            );
        } finally {
            liberarPrimeira.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    )
            );
        }
    }

    private Object devolverOuCapturarRejeicao(
            ServicoDevolucao alvo,
            PedidoDevolucao pedido
    ) {
        try {
            return alvo.devolver(pedido);
        } catch (
                SaldoDevolucaoInsuficienteException
                | ConflitoIdempotenciaException e
        ) {
            return e;
        }
    }

    private Object cadastrarOuCapturarDuplicado(
            ServicoDevolucao alvo,
            long valor
    ) {
        try {
            alvo.registrarTransacao(
                    "pix-cadastro",
                    valor
            );

            return valor;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            return e;
        }
    }

    private <T> List<T> executarSimultaneamente(
            List<Callable<T>> tarefas
    ) throws Exception {

        var prontas = new CountDownLatch(tarefas.size());
        var iniciar = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(tarefas.size());

        var futuros = new ArrayList<Future<T>>();

        try {
            for (Callable<T> tarefa : tarefas) {
                futuros.add(
                        executor.submit(() -> {
                            prontas.countDown();

                            assertTrue(
                                    iniciar.await(
                                            5,
                                            TimeUnit.SECONDS
                                    )
                            );

                            return tarefa.call();
                        })
                );
            }

            assertTrue(
                    prontas.await(
                            5,
                            TimeUnit.SECONDS
                    )
            );

            iniciar.countDown();

            var resultados = new ArrayList<T>();

            for (Future<T> futuro : futuros) {
                resultados.add(
                        futuro.get(
                                5,
                                TimeUnit.SECONDS
                        )
                );
            }

            return resultados;
        } finally {
            iniciar.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    )
            );
        }
    }
}