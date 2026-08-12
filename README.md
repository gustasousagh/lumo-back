# 🧡 Lumo API

Backend do **Lumo** — rede social de mídia (veja, ouça, leia junto): perfis, amizade por
convite, mural, listas, progresso de exibição, salas de *watch party* em tempo real e
notificações push.

Front correspondente: **[lumo-front](https://github.com/gustasousagh/lumo-front)**.

- **Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT) · JPA/Hibernate · WebSocket STOMP · Spring Mail
- **Banco:** MySQL 8 (schema criado pelo Hibernate)
- **Mídia:** esta API é quem fala com o **GoCine** — busca, home, detalhe e resolução de stream. Snapshots ficam em `media_catalog`.

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
| `GOCINE_API_URL` | **sim** | — | Base da API do GoCine. Sem ela, `/api/media/*` responde 500 |
| `GOCINE_JWT_SECRET` | **sim** | — | Token do GoCine |
| `GOCINE_CACHE_MINUTES` | não | `5` | TTL do cache em memória das respostas do GoCine |
| `GOCINE_CACHE_MAX_ENTRIES` | não | `500` | Teto de entradas no cache |
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
| `/api/media/home` | Destaques + seções de carrossel (GoCine) |
| `/api/media/search?q=` | Busca no catálogo |
| `/api/media/detail?type=&id=&kind=` | Detalhe de filme/série/anime — **sem** os links de vídeo |
| `/api/media/stream?type=&id=&kind=&season=&episode=` | Players disponíveis para assistir |
| `/api/media/{type}/{id}/**` | Reações e comentários por episódio |
| `/api/notifications/**` | Notificações |
| `/api/uploads` | Upload de imagens (máx. 5 MB) |
| `/api/health` | Health check *(público)* |
| `/ws` | WebSocket STOMP — `/topic/room/{id}`, `/user/queue/notifications` |

Tudo que não é público exige `Authorization: Bearer <jwt>`.

## Usando o catálogo sem o front

O catálogo é da API, então qualquer cliente (app mobile, script, Postman) consome direto:

```bash
# 1) autentica
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"voce@exemplo.com","password":"sua-senha"}' | jq -r .token)

# 2) usa o catálogo
curl -s localhost:8080/api/media/home            -H "Authorization: Bearer $TOKEN" | jq '.sections[].key'
curl -s 'localhost:8080/api/media/search?q=matrix' -H "Authorization: Bearer $TOKEN" | jq '.results[0]'
curl -s 'localhost:8080/api/media/detail?type=movie&id=3' -H "Authorization: Bearer $TOKEN" | jq
curl -s 'localhost:8080/api/media/stream?type=tv&id=3556&kind=series&season=1&episode=1' \
  -H "Authorization: Bearer $TOKEN" | jq '.streams'
```

Dois detalhes de contrato:

- **`/detail` não devolve links de vídeo**, só `hasStream: true|false`. O link sai em
  `/stream`, quando alguém realmente vai assistir.
- **`/stream` responde 404** com `{"streams":[],"streamUrl":null,"reason":"not_found"}`
  quando o GoCine não tem player para aquele episódio. É resposta esperada, não erro.

## Deploy (Docker)

```bash
docker build -t lumo-api .
docker run -p 8080:8080 --env-file .env -v lumo-uploads:/app/data lumo-api
```

Build multi-stage (JDK 21 para compilar, JRE 21 para rodar), roda como usuário não-root.
Todas as variáveis da tabela acima entram em **runtime** — nenhuma é build arg.

Dois pontos de atenção:

- **Monte um volume em `/app/data`.** Avatares e capas gravam em `data/uploads`; sem
  volume, somem a cada redeploy. Melhor ainda em produção: trocar por S3/Cloudinary.
- **`JPA_DDL_AUTO=update`** é conveniente no começo, mas deixa o Hibernate alterar o
  schema sozinho. Quando estabilizar, troque para `validate` e versione com Flyway.
