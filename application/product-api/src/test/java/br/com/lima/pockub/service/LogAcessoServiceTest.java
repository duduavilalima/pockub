package br.com.lima.pockub.service;

import br.com.lima.pockub.domain.LogAcesso;
import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.repository.LogAcessoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAcessoServiceTest {

    @Mock
    private LogAcessoRepository repository;

    @InjectMocks
    private LogAcessoService service;

    @Test
    void registrarPersisteLogComOTimestampRecebidoDoChamador() {
        LocalDateTime momentoDaRequisicao = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

        service.registrar(7L, "Caneta", Operacao.CONSULTA, momentoDaRequisicao, "10.0.0.1");

        ArgumentCaptor<LogAcesso> captor = ArgumentCaptor.forClass(LogAcesso.class);
        verify(repository).save(captor.capture());
        LogAcesso salvo = captor.getValue();
        assertThat(salvo.getProdutoId()).isEqualTo(7L);
        assertThat(salvo.getNomeProduto()).isEqualTo("Caneta");
        assertThat(salvo.getOperacao()).isEqualTo(Operacao.CONSULTA);
        assertThat(salvo.getTimestamp()).isEqualTo(momentoDaRequisicao);
        assertThat(salvo.getOrigemRequisicao()).isEqualTo("10.0.0.1");
    }

    @Test
    void registrarListagemGravaProdutoIdENomeNulos() {
        service.registrar(null, null, Operacao.LISTAGEM, LocalDateTime.now(), "10.0.0.1");

        ArgumentCaptor<LogAcesso> captor = ArgumentCaptor.forClass(LogAcesso.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getProdutoId()).isNull();
        assertThat(captor.getValue().getNomeProduto()).isNull();
        assertThat(captor.getValue().getOperacao()).isEqualTo(Operacao.LISTAGEM);
    }

    @Test
    void falhaNaGravacaoNaoPropagaExcecao() {
        when(repository.save(any(LogAcesso.class))).thenThrow(new RuntimeException("mongo fora do ar"));

        assertThatCode(() -> service.registrar(1L, "X", Operacao.CONSULTA, LocalDateTime.now(), "ip"))
                .doesNotThrowAnyException();
    }
}
