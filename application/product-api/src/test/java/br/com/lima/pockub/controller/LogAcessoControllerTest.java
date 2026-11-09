package br.com.lima.pockub.controller;

import br.com.lima.pockub.domain.LogAcesso;
import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.repository.LogAcessoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogAcessoController.class)
class LogAcessoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogAcessoRepository repository;

    @Test
    void getLogsVazioRetorna200ComArrayVazio() throws Exception {
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        mockMvc.perform(get("/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getLogsRetornaTodosOsCampos() throws Exception {
        LogAcesso log = LogAcesso.builder()
                .id("abc123")
                .produtoId(5L)
                .nomeProduto("Caneta")
                .operacao(Operacao.CONSULTA)
                .timestamp(LocalDateTime.of(2026, 3, 4, 5, 6, 7))
                .origemRequisicao("10.0.0.9")
                .build();
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log));

        mockMvc.perform(get("/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("abc123"))
                .andExpect(jsonPath("$[0].produtoId").value(5))
                .andExpect(jsonPath("$[0].nomeProduto").value("Caneta"))
                .andExpect(jsonPath("$[0].operacao").value("CONSULTA"))
                .andExpect(jsonPath("$[0].timestamp").exists())
                .andExpect(jsonPath("$[0].origemRequisicao").value("10.0.0.9"));
    }

    @Test
    void getLogsComProdutoIdUsaOFiltro() throws Exception {
        when(repository.findByProdutoIdOrderByTimestampDesc(5L)).thenReturn(List.of());

        mockMvc.perform(get("/logs").param("produtoId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(repository).findByProdutoIdOrderByTimestampDesc(5L);
    }

    @Test
    void produtoIdNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/logs").param("produtoId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
        verifyNoInteractions(repository);
    }
}
