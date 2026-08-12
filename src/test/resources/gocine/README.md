# Fixtures do GoCine

Estes arquivos sustentam os testes de equivalência do catálogo (`GocineMapperTest`,
`GocineServiceTest`). São dados reais, não inventados.

| Arquivo | O que é |
|---|---|
| `raw-*.json` | Respostas cruas do GoCine — a **entrada** do mapper |
| `gold-*.json` | Saída das antigas rotas `/api/media/*` do Next — o **esperado** |

Os `gold-*` foram gravados antes da migração do catálogo para o backend. Enquanto
os testes passam, o Java produz exatamente o mesmo JSON que o TypeScript produzia,
e o front não percebe diferença.

## Por que as duas metades têm que vir do mesmo payload

O feed do GoCine muda a cada requisição — a seção `choosed`, por exemplo, é
rotativa. Gravar `raw` e `gold` em chamadas separadas produz fixtures que não
combinam, e o teste acusa uma diferença que não existe no código.

Por isso o `gold` é gerado servindo o `raw` já gravado para o Next, por um stub
local:

```python
# stub.py — serve as fixtures nos caminhos que o GoCine usaria
import http.server, os, sys
D = "src/test/resources/gocine"
ROUTES = {
    "/media/mobile/default": "raw-home.json",
    "/search/matrix/EASYPLEX": "raw-search.json",
    "/media/detail/3/default": "raw-movie.json",
    "/series/show/3556/default": "raw-series.json",
}
class H(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        name = ROUTES.get(self.path.split("?")[0])
        if not name:
            self.send_response(404); self.end_headers()
            self.wfile.write(b'{"message":"not found"}'); return
        body = open(os.path.join(D, name), "rb").read()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers(); self.wfile.write(body)
    def log_message(self, *a): pass
http.server.ThreadingHTTPServer(("127.0.0.1", int(sys.argv[1])), H).serve_forever()
```

Com o stub no ar, aponte o Next para ele (`GOCINE_API_URL=http://127.0.0.1:3399`),
suba `next start` e salve o corpo de cada rota `/api/media/*` no `gold-*` correspondente.

Duas armadilhas nesse processo:

- **O Next cacheia as respostas** (`revalidate: 300`). Depois de mexer num `raw`,
  apague `.next/cache` e reinicie o servidor, senão o `gold` sai do payload antigo.
- **As rotas `/api/media/*` do Next foram removidas** na migração. Para regerar,
  restaure-as do histórico: elas e o `gocine.server.ts` existem até o commit que
  trouxe o catálogo para cá.

## Sobre o tamanho

`raw-series.json` foi reduzido de 8 temporadas / 171 episódios para 2 temporadas
de 3 episódios. Isso mantém o que os testes precisam exercitar — ordenação de
temporada e episódio, vídeos por episódio, `episode_count` ausente caindo para o
tamanho da lista — sem carregar 420 KB no repositório.
