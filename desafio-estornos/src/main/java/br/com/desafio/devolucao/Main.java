package br.com.desafio.devolucao;

public class Main {
    public static void main(String[] args) throws IllegalAccessException {
        RepositorioDevolucoes repositorio =
                new RepositorioDevolucoesEmMemoria();


        ServicoDevolucao servico =
                new ServicoDevolucao(repositorio);

        servico.registrarTransacao(
                "pix-1",
                10_000L
        );

        PedidoDevolucao pedido = new PedidoDevolucao(
                "pix-1",
                "req-a",
                3_000L
        );

        ResultadoDevolucao primeira = servico.devolver(pedido);
        ResultadoDevolucao repetida = servico.devolver(pedido);

        ResultadoDevolucao segunda = servico.devolver(
                new PedidoDevolucao(
                        "pix-1",
                        "req-b",
                        7_000L
                )
        );

        System.out.println("Primeira: " + primeira);
        System.out.println("Retry: " + repetida);

        System.out.println(
                "Mesmo resultado: " + primeira.equals(repetida)
        );

        System.out.println("Segunda: " + segunda);

        System.out.println(
                "Total em centavos: "
                        + servico.totalDevolvido("pix-1")
        );

        System.out.println(
                "Replay com saldo zero: "
                        + servico.devolver(pedido).equals(primeira)
        );

        try {
            servico.devolver(
                    new PedidoDevolucao(
                            "pix-1",
                            "req-c",
                            1L
                    )
            );
        } catch (SaldoDevolucaoInsuficienteException e) {
            System.out.println(
                    "Pedido rejeitado: " + e.getMessage()
            );
        }
    }
};
