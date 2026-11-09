package br.com.lima.pockub;

import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.repository.LogAcessoRepository;
import br.com.lima.pockub.service.LogAcessoService;
import br.com.lima.pockub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Prova que a gravação do log roda no pool {@code log-async-*}, e não na thread da requisição. */
class LogAssincronoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LogAcessoService logAcessoService;

    @MockitoSpyBean
    private LogAcessoRepository logAcessoRepository;

    @Test
    void registrarGravaEmThreadDoPoolAssincrono() {
        List<String> threads = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            threads.add(Thread.currentThread().getName());
            return inv.callRealMethod();
        }).when(logAcessoRepository).save(any());

        String threadChamadora = Thread.currentThread().getName();
        logAcessoService.registrar(1L, "X", Operacao.CONSULTA, LocalDateTime.now(), "ip");

        await().atMost(Duration.ofSeconds(10)).until(() -> !threads.isEmpty());
        assertThat(threads.get(0)).startsWith("log-async-").isNotEqualTo(threadChamadora);
    }
}
