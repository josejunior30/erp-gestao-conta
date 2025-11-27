# API ERP + Open Finance (Java / Spring Boot + Pluggy)

Este projeto é uma API backend construída com **Spring Boot 3** e **Java 21** que combina:

- Um módulo simples de **gestão de usuários e perfis (User / Role)** com autenticação via **JWT RS256**.
- Uma integração de **Open Finance com a Pluggy**, incluindo:
  - obtenção de **apiKey**;
  - geração de **connect token**;
  - consumo de **itens** e **transações**;
  - recebimento e persistência de **webhooks**.

O objetivo é demonstrar, de forma prática, como estruturar uma API segura, integrando com um provedor externo financeiro (Pluggy), com código testável (JUnit + Mockito) e configuração limpa via `@ConfigurationProperties`.

> 🔑 **Pré-requisito Pluggy:**  
> Para usar a integração de Open Finance, é necessário criar uma conta na **Pluggy**, registrar uma aplicação no dashboard e obter o **clientId** e o **clientSecret**.  
> Esses valores devem ser configurados em `application.properties` (ou no profile correspondente) nas propriedades:
> - `pluggy.client-id`
> - `pluggy.client-secret`

---

## 2. Funcionalidades principais

### 2.1. Autenticação e Autorização (JWT RS256)

- Autenticação baseada em **JWT** usando algoritmo **RS256** (chaves assimétricas).
- As chaves são carregadas a partir de arquivos PEM (`public.pem` e `private.pem`) via `SecurityConfig`.
- Serviço `JwtService` responsável por:
  - Gerar tokens contendo:
    - `sub` → id do usuário;
    - `email` → e-mail de login;
    - `roles` → autoridades/perfis associados.
  - Validar e decodificar tokens via `JwtDecoder`.

Integração com Spring Security como **Resource Server OAuth2**:

- Sessão **stateless** (`SessionCreationPolicy.STATELESS`).
- Conversão de claims para authorities via `JwtAuthenticationConverter` (claim `roles` sem prefixo `ROLE_` automático).
- Configuração de CORS e rotas públicas/privadas em `SecurityFilterConfig`.

Principais classes:

- `SecurityConfig`
- `SecurityFilterConfig`
- `JwtService`
- `SecurityUserDetails`
- `CustomUserDetailsService`

---

### 2.2. Gestão de Usuários e Perfis (User / Role)

Módulo de usuários com JPA:

- Entidade `User`:
  - `id`, `firstName`, `lastName`, `email`, `password`;
  - relacionamento **many-to-many** com `Role`.
- Entidade `Role`:
  - `id`, `authority` (ex.: `ROLE_ADMIN`, `ROLE_USER`).

Integração com Spring Security:

- Classe `SecurityUserDetails` implementa `UserDetails` a partir da entidade `User`.
- `CustomUserDetailsService` carrega usuários por e-mail (`loadUserByUsername`).

Serviço de domínio (ex.: `UserService`) com operações típicas:

- Listar todos os usuários.
- Buscar por id.
- Inserir novo usuário com senha codificada em **BCrypt**.
- Atualizar dados de usuário e perfis.
- Deletar usuário.

Testes:

- `UserServiceTest` cobrindo `findAll`, `findById`, `insert`, `update` e `delete`.

---

### 2.3. Integração com Pluggy (Open Finance)

Integração configurada via `PluggyProperties`:

- `baseUrl`
- `clientId`
- `clientSecret`
- `webhookUrl`

Configuração base:

- `HttpClientsConfig` expõe:
  - `OkHttpClient` compartilhado;
  - `PluggyClient` (SDK oficial `ai.pluggy:pluggy-java`) pronto para ser utilizado.

#### 2.3.1. Autenticação na Pluggy (`/auth` → apiKey)

`PluggyAuthService`:

- Chama `POST /auth` na Pluggy, enviando `clientId` e `clientSecret`.
- Faz cache da `apiKey` em memória com um TTL configurado (aprox. 110 minutos).
- Usa `OkHttpClient` e `ObjectMapper` (Jackson) para parsear o JSON.
- Em caso de falha HTTP, lança `IllegalStateException`.

Testes:

- `PluggyAuthServiceTest`:
  - cenário de sucesso retornando `"apiKey"`;
  - cenário com falha HTTP (status 500).

---

#### 2.3.2. Connect Token (para o widget no frontend)

`PluggyConnectTokenService`:

- Cria o **connect token** via `POST /connect_token`.
- Monta JSON com:
  - `options.clientUserId`;
  - `options.webhookUrl` (se configurado em `PluggyProperties`);
  - `options.oauthRedirectUri` (opcional);
  - `options.avoidDuplicates`;
  - `itemId` (opcional).
- Usa cabeçalho `X-API-KEY` com a `apiKey` obtida em `PluggyAuthService`.
- Retorna o campo `connectToken` (ou `accessToken` como fallback), validando a resposta.

Testes:

- `PluggyConnectTokenServiceTest`:
  - sucesso retornando `"connectToken"`;
  - falha HTTP (status 400) levantando exceção.

Rotas (segundo configuração de segurança):

- `/api/pluggy/connect-token` está liberado sem autenticação para ser chamado pelo frontend.

---

#### 2.3.3. Itens (Items)

`PluggyItemService`:

- Busca detalhes de um item via `GET /items/{itemId}`.
- Mapeia a resposta para um `ItemDetailsDto` contendo:
  - `id` do item;
  - `ConnectorDto` com:
    - `id`,
    - `name`,
    - `primaryColor`,
    - `institutionUrl` (ou `imageUrl`/`logoUrl` como fallback),
    - `country`,
    - `type`.

Tratamento de erro:

- Em falhas HTTP, lança exceções específicas (por exemplo `UpstreamException`, `UpstreamIoException`), logando status e corpo da resposta.

Testes:

- `PluggyItemServiceTest`:
  - cenário de sucesso parseando JSON simples de item + connector;
  - cenário de erro HTTP (500) levantando exceção.

Entidade de persistência:

- `PluggyItem`:
  - `UUID id` (gerado em `@PrePersist`);
  - `String itemId` com constraint `unique` e `index`;
  - representa itens Pluggy já conhecidos pela aplicação.

---

#### 2.3.4. Transações (Transactions)

`PluggyTransactionsHttpService`:

- Responsável por agregar **todas as transações de todas as contas de um item**.
- Funcionamento:
  1. Lista todas as contas do item via `GET /accounts?itemId=...&page=...&pageSize=...`.
  2. Para cada conta, busca transações via `GET /transactions?accountId=...`.
  3. Aplica filtros:
     - `from` e `to` (convertidos para `Instant` usando `ZoneId.of("America/Sao_Paulo")`);
     - `status` (opcional);
     - `pageSize` (máximo de 500).
- Retorno consolidado em um `ObjectNode`:

  ```json
  {
    "itemId": "item123",
    "count": 42,
    "transactions": [ ... ]
  }
  
#### 2.3.5. Integração com o Frontend

- Repositório do frontend: **[josejunior30/erp-conta-FrontEnd](https://github.com/josejunior30/erp-conta-FrontEnd)**


