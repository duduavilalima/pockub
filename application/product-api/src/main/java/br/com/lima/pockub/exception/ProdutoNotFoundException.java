package br.com.lima.pockub.exception;

public class ProdutoNotFoundException extends RuntimeException {

    public ProdutoNotFoundException(Long id) {
        super("Produto " + id + " não encontrado");
    }
}
