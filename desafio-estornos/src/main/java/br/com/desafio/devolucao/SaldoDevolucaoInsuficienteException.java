package br.com.desafio.devolucao;

public class SaldoDevolucaoInsuficienteException extends RuntimeException {
    public SaldoDevolucaoInsuficienteException(String message) {
        super(message);
    }
}
