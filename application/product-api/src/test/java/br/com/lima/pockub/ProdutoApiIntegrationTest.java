package br.com.lima.pockub;

import br.com.lima.pockub.domain.LogAcesso;
import br.com.lima.pockub.domain.Operacao;
import br.com.lima.pockub.repository.LogAcessoRepository;
import br.com.lima.pockub.repository.ProdutoRepository;
import br.com.lima.pockub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo completo com PostgreSQL e MongoDB reais (Testcontainers). */
@AutoConfigureMockMvc
class ProdutoApiIntegrationTest extends AbstractIntegrationTest {

    private static final String CORPO = """
            {"nome":"Caneta","descricao":"azul","preco":9.90,"quantidadeEstoque":3}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private LogAcessoRepository logRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void limpar() {
        produtoRepository.deleteAll();
        logRepository.deleteAll();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode criarProduto(String corpo) throws Exception {
        return json(mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn());
    }

    private void aguardarLogs(long quantidade) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(logRepository.count()).isEqualTo(quantidade));
    }

    // --- US-001 ---

    @Test
    void produtoCriadoEPersistidoERecuperavelComDatasPreenchidas() throws Exception {
        JsonNode criado = criarProduto(CORPO);
        long id = criado.get("id").asLong();

        assertThat(criado.get("dataCriacao").isNull()).isFalse();
        assertThat(criado.get("dataAtualizacao").isNull()).isFalse();

        JsonNode lido = json(mockMvc.perform(get("/produtos/" + id)).andExpect(status().isOk()).andReturn());
        assertThat(lido.get("nome").asString()).isEqualTo("Caneta");
        assertThat(lido.get("preco").decimalValue()).isEqualByComparingTo("9.90");
        assertThat(lido.get("dataCriacao").isNull()).isFalse();
        aguardarLogs(1);
    }

    // --- US-002 ---

    @Test
    void atualizacaoPersisteMudaDataAtualizacaoEPreservaDataCriacao() throws Exception {
        JsonNode criado = criarProduto(CORPO);
        long id = criado.get("id").asLong();
        Thread.sleep(20);

        JsonNode atualizado = json(mockMvc.perform(put("/produtos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Lapis\",\"preco\":1.50,\"quantidadeEstoque\":10}"))
                .andExpect(status().isOk()).andReturn());

        assertThat(atualizado.get("nome").asString()).isEqualTo("Lapis");
        assertThat(LocalDateTime.parse(atualizado.get("dataCriacao").asString()))
                .isEqualTo(LocalDateTime.parse(criado.get("dataCriacao").asString()));
        assertThat(LocalDateTime.parse(atualizado.get("dataAtualizacao").asString()))
                .isAfter(LocalDateTime.parse(criado.get("dataAtualizacao").asString()));

        var persistido = produtoRepository.findById(id).orElseThrow();
        assertThat(persistido.getNome()).isEqualTo("Lapis");
        assertThat(persistido.getQuantidadeEstoque()).isEqualTo(10);
        assertThat(persistido.getDataAtualizacao()).isAfter(persistido.getDataCriacao());
    }

    // --- US-003 ---

    @Test
    void exclusaoRemoveOProdutoMasPreservaOsLogsDeAuditoria() throws Exception {
        long id = criarProduto(CORPO).get("id").asLong();
        mockMvc.perform(get("/produtos/" + id)).andExpect(status().isOk());
        aguardarLogs(1);

        mockMvc.perform(delete("/produtos/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/produtos/" + id)).andExpect(status().isNotFound());
        assertThat(logRepository.findByProdutoIdOrderByTimestampDesc(id)).hasSize(1);
        assertThat(logRepository.findByProdutoIdOrderByTimestampDesc(id).get(0).getNomeProduto())
                .isEqualTo("Caneta");
    }

    // --- US-004 / US-006 ---

    @Test
    void consultaGeraLogConsultaNaCollectionAccessLogs() throws Exception {
        long id = criarProduto(CORPO).get("id").asLong();

        mockMvc.perform(get("/produtos/" + id).with(req -> {
            req.setRemoteAddr("10.9.8.7");
            return req;
        })).andExpect(status().isOk());

        aguardarLogs(1);
        assertThat(mongoTemplate.getCollection("access_logs").countDocuments()).isEqualTo(1);
        LogAcesso log = logRepository.findAll().get(0);
        assertThat(log.getOperacao()).isEqualTo(Operacao.CONSULTA);
        assertThat(log.getProdutoId()).isEqualTo(id);
        assertThat(log.getNomeProduto()).isEqualTo("Caneta");
        assertThat(log.getOrigemRequisicao()).isEqualTo("10.9.8.7");
        assertThat(log.getTimestamp()).isNotNull();
    }

    @Test
    void nomeNoLogEhSnapshotImutavelAposRenomear() throws Exception {
        long id = criarProduto(CORPO).get("id").asLong();
        mockMvc.perform(get("/produtos/" + id)).andExpect(status().isOk());
        aguardarLogs(1);

        mockMvc.perform(put("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Renomeado\",\"preco\":1.00,\"quantidadeEstoque\":1}"))
                .andExpect(status().isOk());

        assertThat(logRepository.findAll().get(0).getNomeProduto()).isEqualTo("Caneta");
    }

    @Test
    void escritasNaoGeramLog() throws Exception {
        long id = criarProduto(CORPO).get("id").asLong();
        mockMvc.perform(put("/produtos/" + id).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/produtos/" + id)).andExpect(status().isNoContent());

        Thread.sleep(300);
        assertThat(logRepository.count()).isZero();
    }

    // --- US-005 ---

    @Test
    void listagemVaziaRetornaArrayVazioEGeraLogListagemSemProduto() throws Exception {
        JsonNode lista = json(mockMvc.perform(get("/produtos")).andExpect(status().isOk()).andReturn());
        assertThat(lista.isArray()).isTrue();
        assertThat(lista.size()).isZero();

        aguardarLogs(1);
        LogAcesso log = logRepository.findAll().get(0);
        assertThat(log.getOperacao()).isEqualTo(Operacao.LISTAGEM);
        assertThat(log.getProdutoId()).isNull();
        assertThat(log.getNomeProduto()).isNull();
    }

    // --- US-007 ---

    @Test
    void logsVaziosRetornamArrayVazio() throws Exception {
        JsonNode logs = json(mockMvc.perform(get("/logs")).andExpect(status().isOk()).andReturn());
        assertThat(logs.isArray()).isTrue();
        assertThat(logs.size()).isZero();
    }

    @Test
    void logsOrdenadosPorTimestampDecrescente() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 5, 1, 12, 0);
        logRepository.saveAll(List.of(
                LogAcesso.builder().produtoId(1L).nomeProduto("A").operacao(Operacao.CONSULTA)
                        .timestamp(base).origemRequisicao("ip").build(),
                LogAcesso.builder().produtoId(1L).nomeProduto("A").operacao(Operacao.CONSULTA)
                        .timestamp(base.plusMinutes(10)).origemRequisicao("ip").build(),
                LogAcesso.builder().operacao(Operacao.LISTAGEM)
                        .timestamp(base.plusMinutes(5)).origemRequisicao("ip").build()));

        JsonNode logs = json(mockMvc.perform(get("/logs")).andExpect(status().isOk()).andReturn());

        assertThat(logs.size()).isEqualTo(3);
        List<LocalDateTime> timestamps = List.of(0, 1, 2).stream()
                .map(i -> LocalDateTime.parse(logs.get(i).get("timestamp").asString())).toList();
        assertThat(timestamps).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(timestamps.get(0)).isEqualTo(base.plusMinutes(10));
    }

    // --- US-008 ---

    @Test
    void filtroPorProdutoIdRetornaApenasOProdutoEmOrdemDecrescenteSemListagem() throws Exception {
        LocalDateTime base = LocalDateTime.of(2026, 5, 1, 12, 0);
        logRepository.saveAll(List.of(
                LogAcesso.builder().produtoId(1L).nomeProduto("A").operacao(Operacao.CONSULTA)
                        .timestamp(base).origemRequisicao("ip").build(),
                LogAcesso.builder().produtoId(1L).nomeProduto("A").operacao(Operacao.CONSULTA)
                        .timestamp(base.plusMinutes(1)).origemRequisicao("ip").build(),
                LogAcesso.builder().produtoId(2L).nomeProduto("B").operacao(Operacao.CONSULTA)
                        .timestamp(base.plusMinutes(2)).origemRequisicao("ip").build(),
                LogAcesso.builder().operacao(Operacao.LISTAGEM)
                        .timestamp(base.plusMinutes(3)).origemRequisicao("ip").build()));

        JsonNode logs = json(mockMvc.perform(get("/logs").param("produtoId", "1"))
                .andExpect(status().isOk()).andReturn());

        assertThat(logs.size()).isEqualTo(2);
        assertThat(LocalDateTime.parse(logs.get(0).get("timestamp").asString())).isEqualTo(base.plusMinutes(1));
        assertThat(LocalDateTime.parse(logs.get(1).get("timestamp").asString())).isEqualTo(base);
        logs.forEach(l -> assertThat(l.get("produtoId").asLong()).isEqualTo(1L));
    }

    @Test
    void filtroSemResultadoRetornaArrayVazio() throws Exception {
        JsonNode logs = json(mockMvc.perform(get("/logs").param("produtoId", "12345"))
                .andExpect(status().isOk()).andReturn());
        assertThat(logs.isArray()).isTrue();
        assertThat(logs.size()).isZero();
    }

    @Test
    void filtroFuncionaParaProdutoJaExcluido() throws Exception {
        long id = criarProduto(CORPO).get("id").asLong();
        mockMvc.perform(get("/produtos/" + id)).andExpect(status().isOk());
        aguardarLogs(1);
        mockMvc.perform(delete("/produtos/" + id)).andExpect(status().isNoContent());

        JsonNode logs = json(mockMvc.perform(get("/logs").param("produtoId", String.valueOf(id)))
                .andExpect(status().isOk()).andReturn());
        assertThat(logs.size()).isEqualTo(1);
    }
}
