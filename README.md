# 🧡 Lumo API

Backend do **Lumo** — rede social de mídia (veja, ouça, leia junto): perfis, amizade por
convite, mural, listas, progresso de exibição, salas de *watch party* em tempo real e
notificações push.

Front correspondente: **[lumo-front](https://github.com/gustasousagh/lumo-front)**.

- **Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT) · JPA/Hibernate · WebSocket STOMP · Spring Mail
- **Banco:** MySQL 8 (schema criado pelo Hibernate)
- **Mídia:** o catálogo vem do GoCine pelo front; aqui só guardamos snapshots em `media_catalog`

## Rodando local

```bash
cp .env.example .env      # preencha as variáveis (veja a tabela abaixo)
set -a && . ./.env && set +a
./gradlew bootRun         # sobe em http://localhost:8080
```

Precisa de um MySQL acessível. Para subir um rapidinho:

```bash
docker run -d --name lumo-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=lumo mysql:8
```

Testes rodam sem MySQL — o profile `test` usa H2 em memória:

```bash
./gradlew test
```

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Para quê |
|---|---|---|---|
| `DB_HOST` | sim | `localhost` | Host do MySQL |
| `DB_PORT` | não | `3306` | Porta do MySQL |
| `DB_NAME` | sim | `lumo` | Nome do database |
| `DB_USERNAME` | sim | `root` | Usuário |
| `DB_PASSWORD` | sim | *(vazio)* | Senha |
| `DB_URL` | não | — | Sobrescreve a JDBC URL inteira |
| `DB_POOL_SIZE` | não | `10` | Conexões máximas no pool Hikari |
| `JPA_DDL_AUTO` | não | `update` | `update` cria/ajusta tabelas; use `validate` quando o schema estabilizar |
| `JWT_SECRET` | **sim** | *(placeholder inseguro)* | Assinatura do JWT. Mín. 32 chars — gere com `openssl rand -base64 48` |
| `MAIL_HOST` / `MAIL_PORT` | não | `smtp.gmail.com` / `587` | Servidor SMTP |
| `MAIL_USERNAME` | sim | — | Conta que envia os emails |
| `MAIL_PASSWORD` | sim | — | Senha de app do Gmail (2FA ligado) |
| `MAIL_FROM` | não | `Lumo 🎬 <MAIL_USERNAME>` | Remetente exibido. O endereço precisa ser o mesmo do `MAIL_USERNAME` |
| `FRONTEND_URL` | sim | `http://localhost:3000` | Origem liberada no CORS e base dos links dos emails |

> Nenhum segredo fica no `application.yml` — tudo vem de env. O `.env` está no `.gitignore`.

## Endpoints principais

| Prefixo | O quê |
|---|---|
| `/api/auth/**` | Cadastro, confirmação por email, login, reset de senha *(público)* |
| `/api/users/**` | Perfil, avatar/capa, busca de pessoas, onboarding |
| `/api/friends/**` | Convites, amizades, sugestões, presença |
| `/api/posts/**` | Mural: posts, reviews, likes, comentários |
| `/api/lists/**` | Listas de títulos |
| `/api/progress/**` | Continue assistindo |
| `/api/rooms/**` | Salas de watch party |
| `/api/media/**` | Reações e comentários por episódio |
| `/api/notifications/**` | Notificações |
| `/api/uploads` | Upload de imagens (máx. 5 MB) |
| `/api/health` | Health check *(público)* |
| `/ws` | WebSocket STOMP — `/topic/room/{id}`, `/user/queue/notifications` |

Tudo que não é público exige `Authorization: Bearer <jwt>`.

## Deploy

Uploads gravam em disco (`data/uploads`) — em produção monte um volume persistente ou
troque por S3/Cloudinary. `JPA_DDL_AUTO=update` é conveniente no começo; depois vale
versionar o schema com Flyway.
