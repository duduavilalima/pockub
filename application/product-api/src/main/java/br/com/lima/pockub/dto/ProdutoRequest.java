package br.com.lima.pockub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank @Size(max = 255) String nome,
        String descricao,
        @NotNull @DecimalMin("0.01") BigDecimal preco,
        @NotNull @Min(0) Integer quantidadeEstoque
) {}
