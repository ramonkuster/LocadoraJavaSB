package br.com.unisales.locadora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.unisales.locadora.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring Data JPA gera a query (SELECT ... WHERE username = ?) já parametrizada,
    // eliminando o risco de SQL Injection que existia na query concatenada manualmente.
    Optional<Usuario> findByUsername(String username);
}