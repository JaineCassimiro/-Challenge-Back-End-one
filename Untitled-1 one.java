// Projeto ForumHub - API RESTful com Spring Boot
// Desenvolvido por: Senhorita Jaine 🌸

// Estrutura do projeto Spring Boot com base no desafio ForumHub
// Arquivos e pacotes principais para a implementação

// 1. Application.java (classe principal)
package com.forumhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ForumHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForumHubApplication.class, args);
    }
}

// 2. Entidades (models)
package com.forumhub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Topico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String mensagem;
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private StatusTopico status = StatusTopico.NAO_RESPONDIDO;

    @ManyToOne
    private Usuario autor;

    private String curso;

    @OneToMany(mappedBy = "topico")
    private List<Resposta> respostas;
    // getters e setters
}

@Entity
public class Usuario implements org.springframework.security.core.userdetails.UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    private String senha;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    // getters e setters
}

@Entity
public class Resposta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mensagem;
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @ManyToOne
    private Usuario autor;

    @ManyToOne
    private Topico topico;
    // getters e setters
}

public enum StatusTopico {
    NAO_RESPONDIDO, RESPONDIDO
}

// 3. Repositórios
package com.forumhub.repository;

import com.forumhub.model.Topico;
import com.forumhub.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TopicoRepository extends JpaRepository<Topico, Long> {}
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}

// 4. DTOs
package com.forumhub.dto;

public record DadosTopico(String titulo, String mensagem, String curso) {}
public record DadosLogin(String email, String senha) {}
public record DadosToken(String token) {}

// 5. Controller
package com.forumhub.controller;

import com.forumhub.model.Topico;
import com.forumhub.repository.TopicoRepository;
import com.forumhub.dto.DadosTopico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/topicos")
public class TopicoController {

    @Autowired
    private TopicoRepository repository;

    @GetMapping
    public List<Topico> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Topico cadastrar(@RequestBody DadosTopico dados) {
        Topico topico = new Topico();
        topico.setTitulo(dados.titulo());
        topico.setMensagem(dados.mensagem());
        topico.setCurso(dados.curso());
        return repository.save(topico);
    }

    @GetMapping("/{id}")
    public Topico buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Topico atualizar(@PathVariable Long id, @RequestBody DadosTopico dados) {
        Topico topico = repository.findById(id).orElseThrow();
        topico.setTitulo(dados.titulo());
        topico.setMensagem(dados.mensagem());
        return repository.save(topico);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }
}

// 6. Autenticação
package com.forumhub.controller;

import com.forumhub.dto.DadosLogin;
import com.forumhub.dto.DadosToken;
import com.forumhub.infra.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @PostMapping
    public DadosToken login(@RequestBody DadosLogin dados) {
        Authentication auth = manager.authenticate(
            new UsernamePasswordAuthenticationToken(dados.email(), dados.senha()));
        String token = tokenService.gerarToken((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal());
        return new DadosToken(token);
    }
}

// 7. Security Configuration
package com.forumhub.infra.security;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth", "/usuarios").permitAll()
                    .anyRequest().authenticated())
                .addFilterBefore(new SecurityFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// 8. Token Service
package com.forumhub.infra.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    public String gerarToken(UserDetails usuario) {
        return Jwts.builder()
                .setSubject(usuario.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public String validarToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
    }
}

// 9. Security Filter
package com.forumhub.infra.security;

import com.forumhub.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = tokenService.validarToken(token);
            if (email != null) {
                var usuario = repository.findByEmail(email).orElseThrow();
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}

// 10. application.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update
jwt.secret=segredo123
