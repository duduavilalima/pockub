package br.com.lima.pockub;

import br.com.lima.pockub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** Sobe o contexto completo: Flyway aplica V1 e o Hibernate valida o schema (ddl-auto: validate). */
class PockubApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
