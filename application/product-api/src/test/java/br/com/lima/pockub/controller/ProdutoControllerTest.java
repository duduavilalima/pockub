package br.com.lima.pockub.controller;

import br.com.lima.pockub.dto.ProdutoResponse;
import br.com.lima.pockub.exception.ProdutoNotFoundException;
import br.com.lima.pockub.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProdutoController.class)
class ProdutoControllerTest {

    private static final String VALIDO = """
            {"nome":"Caneta","descricao":"azul","preco":9.90,"quantidadeEstoque":3}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProdutoService produtoService;

    private static ProdutoResponse resposta(Long id) {
        LocalDateTime agora = LocalDateTime.of(2026, 1, 1, 10, 0);
        return new ProdutoResponse(id, "Caneta", "azul", new BigDecimal("9.90"), 3, agora, agora);
    }

    // --- US-001 ---

    @Test
    void postValidoRetorna201ComId() throws Exception {
        when(produtoService.criar(any())).thenReturn(resposta(1L));

        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Caneta"))
                .andExpect(jsonPath("$.preco").value(9.90))
                .andExpect(jsonPath("$.quantidadeEstoque").value(3));
    }

    @Test
    void postInvalidoRetorna400ENaoChamaOService() throws Exception {
        String[] invalidos = {
                """
                {"descricao":"x","preco":9.90,"quantidadeEstoque":3}""",
                """
                {"nome":"","preco":9.90,"quantidadeEstoque":3}""",
                """
                {"nome":"   ","preco":9.90,"quantidadeEstoque":3}""",
                "{\"nome\":\"" + "a".repeat(256) + "\",\"preco\":9.90,\"quantidadeEstoque\":3}",
                """
                {"nome":"A","preco":0,"quantidadeEstoque":3}""",
                """
                {"nome":"A","preco":-1,"quantidadeEstoque":3}""",
                """
                {"nome":"A","quantidadeEstoque":3}""",
                """
                {"nome":"A","preco":9.90,"quantidadeEstoque":-1}""",
                """
                {"nome":"A","preco":9.90}"""
        };
        for (String corpo : invalidos) {
            mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros").isArray());
        }
        verifyNoInteractions(produtoService);
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    // --- US-002 ---

    @Test
    void putValidoRetorna200() throws Exception {
        when(produtoService.atualizar(eq(1L), any())).thenReturn(resposta(1L));

        mockMvc.perform(put("/produtos/1").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void putComIdInexistenteRetorna404() throws Exception {
        when(produtoService.atualizar(eq(99L), any())).thenThrow(new ProdutoNotFoundException(99L));

        mockMvc.perform(put("/produtos/99").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Produto 99 não encontrado"));
    }

    @Test
    void putComCorpoInvalidoRetorna400() throws Exception {
        mockMvc.perform(put("/produtos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"A\",\"preco\":0,\"quantidadeEstoque\":3}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(produtoService);
    }

    // --- US-003 ---

    @Test
    void deleteRetorna204() throws Exception {
        mockMvc.perform(delete("/produtos/1")).andExpect(status().isNoContent());
        verify(produtoService).excluir(1L);
    }

    @Test
    void deleteComIdInexistenteRetorna404() throws Exception {
        doThrow(new ProdutoNotFoundException(99L)).when(produtoService).excluir(99L);

        mockMvc.perform(delete("/produtos/99")).andExpect(status().isNotFound());
    }

    // --- US-004 / US-005 / US-006 ---

    @Test
    void getPorIdRetorna200EPassaOIpDeOrigemDoRemoteAddr() throws Exception {
        when(produtoService.buscarPorId(eq(1L), eq("10.1.2.3"))).thenReturn(resposta(1L));

        mockMvc.perform(get("/produtos/1").with(req -> {
                    req.setRemoteAddr("10.1.2.3");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(produtoService).buscarPorId(1L, "10.1.2.3");
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        when(produtoService.buscarPorId(eq(99L), any())).thenThrow(new ProdutoNotFoundException(99L));

        mockMvc.perform(get("/produtos/99")).andExpect(status().isNotFound());
    }

    @Test
    void getPorIdNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/produtos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
        verifyNoInteractions(produtoService);
    }

    @Test
    void getListaRetornaArrayEPassaOIpDeOrigem() throws Exception {
        when(produtoService.listar(eq("10.1.2.3"))).thenReturn(List.of(resposta(1L), resposta(2L)));

        mockMvc.perform(get("/produtos").with(req -> {
                    req.setRemoteAddr("10.1.2.3");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getListaVaziaRetornaArrayVazio() throws Exception {
        when(produtoService.listar(any())).thenReturn(List.of());

        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rotaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/nao-existe")).andExpect(status().isNotFound());
    }
}
