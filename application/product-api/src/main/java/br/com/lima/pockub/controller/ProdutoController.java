package br.com.lima.pockub.controller;

import br.com.lima.pockub.dto.ProdutoRequest;
import br.com.lima.pockub.dto.ProdutoResponse;
import br.com.lima.pockub.service.ProdutoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(produtoService.buscarPorId(id, request.getRemoteAddr()));
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listar(HttpServletRequest request) {
        return ResponseEntity.ok(produtoService.listar(request.getRemoteAddr()));
    }
}
