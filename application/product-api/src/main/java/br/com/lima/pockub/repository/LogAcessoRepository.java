package br.com.lima.pockub.repository;

import br.com.lima.pockub.domain.LogAcesso;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LogAcessoRepository extends MongoRepository<LogAcesso, String> {

    List<LogAcesso> findAllByOrderByTimestampDesc();

    List<LogAcesso> findByProdutoIdOrderByTimestampDesc(Long produtoId);
}
