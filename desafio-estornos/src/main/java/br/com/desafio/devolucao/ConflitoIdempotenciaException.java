package br.com.desafio.devolucao;

public class ConflitoIdempotenciaException extends RuntimeException{
    public ConflitoIdempotenciaException(String message){
        super(message);
    }
}
