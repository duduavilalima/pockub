package br.com.lima.pockub.dto;

import br.com.lima.pockub.domain.Produto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer quantidadeEstoque,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
    public static ProdutoResponse from(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getQuantidadeEstoque(),
                produto.getDataCriacao(),
                produto.getDataAtualizacao()
        );
    }
}
