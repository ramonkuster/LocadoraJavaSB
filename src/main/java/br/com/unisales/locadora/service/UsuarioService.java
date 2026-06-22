package br.com.unisales.locadora.service;

import br.com.unisales.locadora.model.Usuario;
import br.com.unisales.locadora.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    // BCrypt: gera um hash diferente a cada vez (salt aleatório) e é resistente a ataques de força bruta.
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Usuario cadastrar(Usuario usuario) {
        // Nunca salvar a senha em texto plano: aplica o hash antes de persistir.
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return repository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Usuario alterar(Long id, Usuario dadosNovos) {
        Usuario user = repository.findById(id).orElse(null);
        if (user != null) {
            user.setUsername(dadosNovos.getUsername());
            // Se uma nova senha foi enviada, ela também precisa ser criptografada.
            if (dadosNovos.getPassword() != null && !dadosNovos.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(dadosNovos.getPassword()));
            }
            user.setRole(dadosNovos.getRole());
            return repository.save(user);
        }
        return null;
    }

    /**
     * Autenticação segura: busca o usuário por username (query parametrizada,
     * sem concatenação de SQL) e compara a senha informada com o hash salvo no banco.
     * Retorna Optional vazio se usuário não existir OU senha não conferir,
     * sem revelar qual dos dois motivos causou a falha (evita enumeração de usuários).
     */
    public Optional<Usuario> autenticar(String username, String rawPassword) {
        Optional<Usuario> usuarioOpt = repository.findByUsername(username);
        if (usuarioOpt.isPresent() && passwordEncoder.matches(rawPassword, usuarioOpt.get().getPassword())) {
            return usuarioOpt;
        }
        return Optional.empty();
    }
}