package br.com.lima.pockub.dto;

import br.com.lima.pockub.domain.LogAcesso;
import br.com.lima.pockub.domain.Operacao;

import java.time.LocalDateTime;

public record LogAcessoResponse(
        String id,
        Long produtoId,
        String nomeProduto,
        Operacao operacao,
        LocalDateTime timestamp,
        String origemRequisicao
) {
    public static LogAcessoResponse from(LogAcesso log) {
        return new LogAcessoResponse(
                log.getId(),
                log.getProdutoId(),
                log.getNomeProduto(),
                log.getOperacao(),
                log.getTimestamp(),
                log.getOrigemRequisicao()
        );
    }
}
