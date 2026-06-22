package br.com.unisales.locadora.controller;

import br.com.unisales.locadora.model.Usuario;
import br.com.unisales.locadora.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @PostMapping
    public Usuario cadastrar(@RequestBody Usuario usuario) {
        return service.cadastrar(usuario);
    }

    @PostMapping("/login")
    public String login(@RequestBody Usuario loginDados) {
        // Autenticação via JPA (query parametrizada) + comparação de hash BCrypt.
        // Não há mais concatenação de SQL nem exposição de detalhes internos do banco.
        Optional<Usuario> usuario = service.autenticar(loginDados.getUsername(), loginDados.getPassword());
        if (usuario.isPresent()) {
            return "Login realizado! Bem-vindo, " + usuario.get().getUsername();
        } else {
            return "Usuário ou senha incorretos.";
        }
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping
    public List<Usuario> listarTodos() {
        return service.listarTodos();
    }

    @PutMapping("/alterar/{id}")
    public Usuario alterar(@PathVariable Long id, @RequestBody Usuario dadosNovos) {
        return service.alterar(id, dadosNovos);
    }
}