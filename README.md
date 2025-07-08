# -Challenge-Back-End-one
 Challenge Back End

# 📚 ForumHub API - Spring Boot REST

API desenvolvida para simular um fórum de discussão com funcionalidades de autenticação, segurança com JWT e persistência de dados.

---

### 🚀 Tecnologias utilizadas
- Java 17
- Spring Boot
- Spring Security + JWT
- Spring Data JPA
- H2 Database (memória)
- Maven

---

### 🧠 Funcionalidades

- ✅ Cadastrar, listar, editar e deletar tópicos (CRUD)
- 🔐 Autenticação de usuários via token JWT
- 🛡️ Segurança com filtros de token e validação
- 📂 Camada de DTOs para requisições limpas
- 📑 Estrutura RESTful com boas práticas

---

### 📦 Como executar o projeto

1. Clone este repositório
2. Execute com sua IDE (IntelliJ, VS Code, etc.)
3. Acesse o H2 console:  
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:testdb`
   - Usuário: `sa`
   - Senha: *(em branco)*

---

### 🔑 Requisições protegidas

1. Autentique-se com:
```json
POST /auth
{
  "email": "usuario@email.com",
  "senha": "123456"
}
