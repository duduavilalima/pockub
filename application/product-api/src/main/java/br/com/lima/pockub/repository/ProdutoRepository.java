package br.com.lima.pockub.repository;

import br.com.lima.pockub.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
