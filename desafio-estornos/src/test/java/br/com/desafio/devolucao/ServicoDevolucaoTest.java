package br.com.desafio.devolucao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RequisitosAdicionaisTest {

    private ServicoDevolucao servico;

    @BeforeEach
    void preparar() throws IllegalAccessException {
        RepositorioDevolucoes repositorio = new RepositorioDevolucoesEmMemoria();
        servico = new ServicoDevolucao(repositorio);

        servico.registrarTransacao("pix-1", 10_000L);
    }

    @Test
    void deveComecarComTotalZero() {
        assertEquals(
                0L,
                servico.totalDevolvido("pix-1")
        );
    }

    @Test
    void deveExporTodosOsCamposDoResultado() {
        var resultado = servico.devolver(
                new PedidoDevolucao(
                        "pix-1",
                        "req-a",
                        1_234L
                )
        );

        assertAll(
                () -> assertNotNull(resultado.idDevolucao()),
                () -> assertEquals("pix-1", resultado.transacaoId()),
                () -> assertEquals("req-a", resultado.chaveIdempotencia()),
                () -> assertEquals(1_234L, resultado.valorCentavos())
        );
    }

    @Test
    void deveRejeitarRepositorioNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServicoDevolucao(null)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    void deveRejeitarIdentificadorInvalidoNoCadastro(
            String identificador
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> servico.registrarTransacao(
                        identificador,
                        100L
                )
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void deveRejeitarValorInvalidoNoCadastroSemReservarIdentificador(
            long valor
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> servico.registrarTransacao(
                        "pix-nova",
                        valor
                )
        );

        assertDoesNotThrow(
                () -> servico.registrarTransacao(
                        "pix-nova",
                        100L
                )
        );

        assertEquals(
                0L,
                servico.totalDevolvido("pix-nova")
        );
    }

    @Test
    void deveRejeitarPedidoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servico.devolver(null)
        );

        assertEquals(
                0L,
                servico.totalDevolvido("pix-1")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    void deveRejeitarIdentificadorInvalidoNoPedido(
            String identificador
    ) {
        var pedido = new PedidoDevolucao(
                identificador,
                "req-a",
                100L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servico.devolver(pedido)
        );

        assertEquals(
                0L,
                servico.totalDevolvido("pix-1")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    void deveValidarChaveAntesDeLocalizarTransacao(
            String chave
    ) {
        var pedido = new PedidoDevolucao(
                "pix-inexistente",
                chave,
                100L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servico.devolver(pedido)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void deveValidarValorAntesDeLocalizarTransacao(long valor) {
        var pedido = new PedidoDevolucao(
                "pix-inexistente",
                "req-a",
                valor
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servico.devolver(pedido)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void deveRejeitarValorInvalidoSemConsumirSaldoNemReservarChave(
            long valor
    ) {
        var invalido = new PedidoDevolucao(
                "pix-1",
                "req-a",
                valor
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servico.devolver(invalido)
        );

        assertEquals(
                0L,
                servico.totalDevolvido("pix-1")
        );

        assertDoesNotThrow(
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-1",
                                "req-a",
                                10_000L
                        )
                )
        );

        assertEquals(
                10_000L,
                servico.totalDevolvido("pix-1")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    void deveRejeitarIdentificadorInvalidoNaConsulta(
            String identificador
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> servico.totalDevolvido(identificador)
        );
    }

    @Test
    void deveRejeitarDevolucaoDeTransacaoInexistente() {
        assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-inexistente",
                                "req-a",
                                100L
                        )
                )
        );
    }

    @Test
    void deveRejeitarConsultaDeTransacaoInexistente() {
        assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> servico.totalDevolvido("pix-inexistente")
        );
    }

    @Test
    void deveValidarEntradaAntesDeVerificarChaveAprovada() {
        servico.devolver(
                new PedidoDevolucao(
                        "pix-1",
                        "req-a",
                        10_000L
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-1",
                                "req-a",
                                0L
                        )
                )
        );

        assertEquals(
                10_000L,
                servico.totalDevolvido("pix-1")
        );
    }

    @Test
    void deveDetectarConflitoAntesDeVerificarSaldo() {
        servico.devolver(
                new PedidoDevolucao(
                        "pix-1",
                        "req-a",
                        10_000L
                )
        );

        assertThrows(
                ConflitoIdempotenciaException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-1",
                                "req-a",
                                10_001L
                        )
                )
        );

        assertEquals(
                10_000L,
                servico.totalDevolvido("pix-1")
        );
    }

    @Test
    void cadastroDuplicadoDevePreservarValorTotalEChaves() {
        var pedido = new PedidoDevolucao(
                "pix-1",
                "req-a",
                2_000L
        );

        var original = servico.devolver(pedido);

        assertThrows(
                IllegalAccessException.class,
                () -> servico.registrarTransacao(
                        "pix-1",
                        20_000L
                )
        );

        assertEquals(
                2_000L,
                servico.totalDevolvido("pix-1")
        );

        assertEquals(
                original,
                servico.devolver(pedido)
        );

        assertThrows(
                SaldoDevolucaoInsuficienteException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-1",
                                "req-b",
                                8_001L
                        )
                )
        );

        servico.devolver(
                new PedidoDevolucao(
                        "pix-1",
                        "req-b",
                        8_000L
                )
        );

        assertEquals(
                10_000L,
                servico.totalDevolvido("pix-1")
        );
    }

    @Test
    void devePermitirAMesmaChaveEmTransacoesDistintas() throws IllegalAccessException {
        servico.registrarTransacao(
                "pix-2",
                20_000L
        );

        var primeira = servico.devolver(
                new PedidoDevolucao(
                        "pix-1",
                        "req-a",
                        3_000L
                )
        );

        var segunda = servico.devolver(
                new PedidoDevolucao(
                        "pix-2",
                        "req-a",
                        4_000L
                )
        );

        assertAll(
                () -> assertNotEquals(
                        primeira.idDevolucao(),
                        segunda.idDevolucao()
                ),
                () -> assertEquals(
                        3_000L,
                        servico.totalDevolvido("pix-1")
                ),
                () -> assertEquals(
                        4_000L,
                        servico.totalDevolvido("pix-2")
                )
        );
    }

    @Test
    void deveTrabalharNoLimiteDeLongSemOverflow() throws IllegalAccessException {
        servico.registrarTransacao(
                "pix-grande",
                Long.MAX_VALUE
        );

        var pedido = new PedidoDevolucao(
                "pix-grande",
                "req-a",
                Long.MAX_VALUE - 1L
        );

        var original = servico.devolver(pedido);

        assertThrows(
                SaldoDevolucaoInsuficienteException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-grande",
                                "req-b",
                                2L
                        )
                )
        );

        assertEquals(
                Long.MAX_VALUE - 1L,
                servico.totalDevolvido("pix-grande")
        );

        servico.devolver(
                new PedidoDevolucao(
                        "pix-grande",
                        "req-b",
                        1L
                )
        );

        assertEquals(
                Long.MAX_VALUE,
                servico.totalDevolvido("pix-grande")
        );

        assertEquals(
                original,
                servico.devolver(pedido)
        );

        assertThrows(
                SaldoDevolucaoInsuficienteException.class,
                () -> servico.devolver(
                        new PedidoDevolucao(
                                "pix-grande",
                                "req-c",
                                1L
                        )
                )
        );

        assertEquals(
                Long.MAX_VALUE,
                servico.totalDevolvido("pix-grande")
        );
    }
}