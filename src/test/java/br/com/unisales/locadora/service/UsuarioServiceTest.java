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

    @Test
    void alterar_comNovaSenha_deveCriptografarANovaSenhaAntesDeSalvar() {
        // Este teste cobre a MESMA correção de segurança (hash de senha), só que no fluxo de atualização.
        Usuario existente = new Usuario();
        existente.setId(1L);
        existente.setUsername("joao");
        existente.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("senhaAntiga"));
        existente.setRole("USER");

        Usuario dadosNovos = new Usuario();
        dadosNovos.setUsername("joao");
        dadosNovos.setPassword("senhaNova123");
        dadosNovos.setRole("USER");

        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario atualizado = service.alterar(1L, dadosNovos);

        assertNotNull(atualizado);
        assertNotEquals("senhaNova123", atualizado.getPassword(), "A nova senha também deve ser criptografada, nunca salva em texto plano");
        assertTrue(atualizado.getPassword().startsWith("$2"));
    }

    @Test
    void alterar_comUsuarioInexistente_deveRetornarNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Usuario resultado = service.alterar(99L, new Usuario());

        assertNull(resultado);
    }

    @Test
    void buscarPorId_comIdExistente_deveRetornarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setUsername("carla");
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));

        Usuario resultado = service.buscarPorId(5L);

        assertNotNull(resultado);
        assertEquals("carla", resultado.getUsername());
    }

    @Test
    void listarTodos_deveRetornarListaDoRepositorio() {
        Usuario u1 = new Usuario();
        u1.setUsername("a");
        Usuario u2 = new Usuario();
        u2.setUsername("b");
        when(repository.findAll()).thenReturn(java.util.List.of(u1, u2));

        var resultado = service.listarTodos();

        assertEquals(2, resultado.size());
    }
}