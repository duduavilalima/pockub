package br.com.lima.pockub.service;

import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.domain.Produto;
import br.com.lima.pockub.dto.ProdutoRequest;
import br.com.lima.pockub.dto.ProdutoResponse;
import br.com.lima.pockub.exception.ProdutoNotFoundException;
import br.com.lima.pockub.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private LogAcessoService logAcessoService;

    @InjectMocks
    private ProdutoService service;

    private static Produto produto(Long id, String nome) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPreco(new BigDecimal("9.90"));
        p.setQuantidadeEstoque(3);
        return p;
    }

    private static ProdutoRequest request() {
        return new ProdutoRequest("Caneta", "azul", new BigDecimal("9.90"), 3);
    }

    // --- US-002 / US-003: 404 ---

    @Test
    void atualizarComIdInexistenteLancaNotFound() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request()))
                .isInstanceOf(ProdutoNotFoundException.class)
                .hasMessage("Produto 99 não encontrado");
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void excluirComIdInexistenteLancaNotFound() {
        when(produtoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(ProdutoNotFoundException.class);
        verify(produtoRepository, never()).deleteById(anyLong());
    }

    @Test
    void excluirRemoveOProdutoExistente() {
        when(produtoRepository.existsById(1L)).thenReturn(true);

        service.excluir(1L);

        verify(produtoRepository).deleteById(1L);
    }

    // --- US-004: consulta ---

    @Test
    void buscarPorIdInexistenteLancaNotFoundESemLog() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L, "ip")).isInstanceOf(ProdutoNotFoundException.class);
        verifyNoInteractions(logAcessoService);
    }

    @Test
    void buscarPorIdRegistraLogDeConsultaComSnapshotDoNome() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "Caneta")));
        LocalDateTime antes = LocalDateTime.now();

        ProdutoResponse resposta = service.buscarPorId(1L, "10.0.0.1");

        assertThat(resposta.id()).isEqualTo(1L);
        verify(logAcessoService).registrar(eq(1L), eq("Caneta"), eq(Operacao.CONSULTA),
                org.mockito.ArgumentMatchers.argThat(t -> !t.isBefore(antes) && !t.isAfter(LocalDateTime.now())),
                eq("10.0.0.1"));
    }

    // --- US-005: listagem ---

    @Test
    void listarComListaVaziaRetornaVazioEAindaRegistraLog() {
        when(produtoRepository.findAll()).thenReturn(List.of());

        assertThat(service.listar("ip")).isEmpty();

        verify(logAcessoService).registrar(eq(null), eq(null), eq(Operacao.LISTAGEM), any(LocalDateTime.class), eq("ip"));
    }

    @Test
    void listarRetornaProdutosERegistraLogDeListagem() {
        when(produtoRepository.findAll()).thenReturn(List.of(produto(1L, "A"), produto(2L, "B")));

        assertThat(service.listar("ip")).extracting(ProdutoResponse::nome).containsExactly("A", "B");

        verify(logAcessoService).registrar(eq(null), eq(null), eq(Operacao.LISTAGEM), any(LocalDateTime.class), eq("ip"));
    }

    // --- US-006: só leituras geram log ---

    @Test
    void criarAtualizarEExcluirNaoGeramLog() {
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(produtoRepository.saveAndFlush(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "Caneta")));
        when(produtoRepository.existsById(1L)).thenReturn(true);

        service.criar(request());
        service.atualizar(1L, request());
        service.excluir(1L);

        verify(logAcessoService, never()).registrar(any(), any(), any(), any(), any());
    }
}
