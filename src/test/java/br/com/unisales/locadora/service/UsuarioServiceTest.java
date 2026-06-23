package br.com.unisales.locadora.service;

import br.com.unisales.locadora.model.Usuario;
import br.com.unisales.locadora.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários da correção da vulnerabilidade:
 * - SQL Injection no login
 * - Senha armazenada em texto plano
 */
class UsuarioServiceTest {

    private UsuarioRepository repository;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        service = new UsuarioService();
        // injeta o mock manualmente no campo @Autowired (sem precisar subir o contexto Spring)
        try {
            var field = UsuarioService.class.getDeclaredField("repository");
            field.setAccessible(true);
            field.set(service, repository);
        } catch (Exception e) {
            fail("Não foi possível injetar o mock: " + e.getMessage());
        }
    }

    @Test
    void cadastrar_devePersistirSenhaComHashEDiferenteDoTextoOriginal() {
        Usuario novo = new Usuario();
        novo.setUsername("joao");
        novo.setPassword("senha123");

        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario salvo = service.cadastrar(novo);

        assertNotEquals("senha123", salvo.getPassword(), "A senha NUNCA deve ser salva em texto plano");
        assertTrue(salvo.getPassword().startsWith("$2"), "O hash deve estar no formato BCrypt ($2a$/$2b$...)");
    }

    @Test
    void autenticar_comCredenciaisCorretas_deveRetornarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("joao");
        usuario.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("senha123"));

        when(repository.findByUsername("joao")).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = service.autenticar("joao", "senha123");

        assertTrue(resultado.isPresent());
        assertEquals("joao", resultado.get().getUsername());
    }

    @Test
    void autenticar_comSenhaErrada_deveRetornarVazio() {
        Usuario usuario = new Usuario();
        usuario.setUsername("joao");
        usuario.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("senha123"));

        when(repository.findByUsername("joao")).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = service.autenticar("joao", "senhaErrada");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void autenticar_comPayloadDeSqlInjectionNoUsername_naoEncontraNinguemEDeveRetornarVazio() {
        // Como agora usamos findByUsername (query parametrizada pelo Spring Data JPA),
        // um payload de SQL Injection é tratado como um username comum (string literal),
        // e simplesmente não é encontrado no banco — não há mais bypass de autenticação.
        String payload = "' OR '1'='1";
        when(repository.findByUsername(payload)).thenReturn(Optional.empty());

        Optional<Usuario> resultado = service.autenticar(payload, "qualquerSenha");

        assertTrue(resultado.isEmpty());
        verify(repository, times(1)).findByUsername(payload);
    }
}
