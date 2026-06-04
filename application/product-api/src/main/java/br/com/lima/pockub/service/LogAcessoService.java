package br.com.lima.pockub.service;

import br.com.lima.pockub.domain.LogAcesso;
import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.repository.LogAcessoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogAcessoService {

    private static final Logger log = LoggerFactory.getLogger(LogAcessoService.class);

    private final LogAcessoRepository logAcessoRepository;

    public LogAcessoService(LogAcessoRepository logAcessoRepository) {
        this.logAcessoRepository = logAcessoRepository;
    }

    @Async
    public void registrar(Long produtoId, String nomeProduto, Operacao operacao, String origem) {
        try {
            LogAcesso logAcesso = LogAcesso.builder()
                    .produtoId(produtoId)
                    .nomeProduto(nomeProduto)
                    .operacao(operacao)
                    .timestamp(LocalDateTime.now())
                    .origemRequisicao(origem)
                    .build();
            logAcessoRepository.save(logAcesso);
        } catch (Exception ex) {
            log.error("Falha ao registrar log de acesso: operacao={}, produtoId={}", operacao, produtoId, ex);
        }
    }
}
