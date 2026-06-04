package br.com.lima.pockub.service;

import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.domain.Produto;
import br.com.lima.pockub.dto.ProdutoRequest;
import br.com.lima.pockub.dto.ProdutoResponse;
import br.com.lima.pockub.exception.ProdutoNotFoundException;
import br.com.lima.pockub.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final LogAcessoService logAcessoService;

    public ProdutoService(ProdutoRepository produtoRepository, LogAcessoService logAcessoService) {
        this.produtoRepository = produtoRepository;
        this.logAcessoService = logAcessoService;
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto();
        aplicarDados(produto, request);
        return ProdutoResponse.from(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarOuLancar(id);
        aplicarDados(produto, request);
        return ProdutoResponse.from(produtoRepository.save(produto));
    }

    @Transactional
    public void excluir(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new ProdutoNotFoundException(id);
        }
        produtoRepository.deleteById(id);
    }

    public ProdutoResponse buscarPorId(Long id, String origem) {
        Produto produto = buscarOuLancar(id);
        logAcessoService.registrar(produto.getId(), produto.getNome(), Operacao.CONSULTA, origem);
        return ProdutoResponse.from(produto);
    }

    public List<ProdutoResponse> listar(String origem) {
        List<Produto> produtos = produtoRepository.findAll();
        logAcessoService.registrar(null, null, Operacao.LISTAGEM, origem);
        return produtos.stream().map(ProdutoResponse::from).toList();
    }

    private Produto buscarOuLancar(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNotFoundException(id));
    }

    private void aplicarDados(Produto produto, ProdutoRequest request) {
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setQuantidadeEstoque(request.quantidadeEstoque());
    }
}
