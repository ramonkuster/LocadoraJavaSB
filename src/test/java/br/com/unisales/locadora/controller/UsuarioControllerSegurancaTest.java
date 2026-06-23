package br.com.unisales.locadora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração que comprovam, de ponta a ponta (HTTP -> Controller -> Service -> Repository -> H2),
 * que as vulnerabilidades de SQL Injection e exposição de senha em texto plano foram corrigidas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Monta o JSON da requisição manualmente em um Map (e não a partir da entidade Usuario).
     * Isso é necessário porque o campo password da entidade é @JsonProperty(WRITE_ONLY):
     * isso impede a senha de aparecer quando um Usuario é SERIALIZADO para JSON (o que é ótimo
     * para a resposta da API, mas significa que não podemos usar objectMapper.writeValueAsString(usuario)
     * para gerar o corpo da REQUISIÇÃO, pois a senha seria omitida também ali).
     */
    private String corpoJson(String username, String senha) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", senha);
        body.put("role", "USER");
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void cadastro_respostaJson_naoDeveExporASenha() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType("application/json")
                        .content(corpoJson("maria", "minhaSenha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria"))
                .andExpect(jsonPath("$.password").doesNotExist()); // antes da correção, a senha em texto plano vinha aqui
    }

    @Test
    void login_comCredenciaisCorretas_devePermitirAcesso() throws Exception {
        mockMvc.perform(post("/usuarios")
                .contentType("application/json")
                .content(corpoJson("pedro", "senhaForte1")));

        mockMvc.perform(post("/usuarios/login")
                        .contentType("application/json")
                        .content(corpoJson("pedro", "senhaForte1")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bem-vindo")));
    }

    @Test
    void login_comSenhaIncorreta_deveNegarAcesso() throws Exception {
        mockMvc.perform(post("/usuarios")
                .contentType("application/json")
                .content(corpoJson("ana", "senhaCorreta")));

        mockMvc.perform(post("/usuarios/login")
                        .contentType("application/json")
                        .content(corpoJson("ana", "senhaErrada")))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário ou senha incorretos."));
    }

    @Test
    void login_comPayloadClassicoDeSqlInjection_naoDeveBurlarAAutenticacao() throws Exception {
        // Antes da correção, este payload ('OR '1'='1) era suficiente para logar sem senha válida,
        // pois a query era montada por concatenação de strings.
        mockMvc.perform(post("/usuarios/login")
                        .contentType("application/json")
                        .content(corpoJson("' OR '1'='1", "' OR '1'='1")))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário ou senha incorretos."));
    }

    @Test
    void login_comPayloadDeSqlInjectionViaComentario_naoDeveBurlarAAutenticacao() throws Exception {
        // Outro payload clássico: tenta comentar o resto da query SQL com "--"
        mockMvc.perform(post("/usuarios/login")
                        .contentType("application/json")
                        .content(corpoJson("admin'--", "qualquercoisa")))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário ou senha incorretos."));
    }
}