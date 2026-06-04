package br.com.lima.pockub.controller;

import br.com.lima.pockub.dto.LogAcessoResponse;
import br.com.lima.pockub.repository.LogAcessoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogAcessoController {

    private final LogAcessoRepository logAcessoRepository;

    public LogAcessoController(LogAcessoRepository logAcessoRepository) {
        this.logAcessoRepository = logAcessoRepository;
    }

    @GetMapping
    public ResponseEntity<List<LogAcessoResponse>> listar(@RequestParam(required = false) Long produtoId) {
        List<LogAcessoResponse> logs = produtoId != null
                ? logAcessoRepository.findByProdutoIdOrderByTimestampDesc(produtoId).stream()
                        .map(LogAcessoResponse::from).toList()
                : logAcessoRepository.findAllByOrderByTimestampDesc().stream()
                        .map(LogAcessoResponse::from).toList();
        return ResponseEntity.ok(logs);
    }
}
