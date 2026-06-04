package br.com.lima.pockub.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "access_logs")
@Getter
@Builder
public class LogAcesso {

    @Id
    private String id;

    @Indexed
    private Long produtoId;

    private String nomeProduto;
    private Operacao operacao;

    @Indexed
    private LocalDateTime timestamp;

    private String origemRequisicao;
}
