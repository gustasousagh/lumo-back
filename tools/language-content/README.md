# Gerador de conteúdo do curso de inglês

Transforma listas abertas de vocabulário, pronúncia e frases no conteúdo que a
API carrega no boot: **11.829 palavras, 55 unidades, 385 lições e 16.017
exercícios**.

```sh
npm run generate    # ~2 segundos, offline
npm run validate    # confere o que foi gerado
```

Sem dependências no `node_modules` e **sem custo**: nenhuma chamada a serviço
pago. As fontes estão em `data/` — veja [data/LICENSES.md](data/LICENSES.md).

## Como funciona

```
data/  ─┬─ user_5000_pt.csv ──────┐
        ├─ wiktionary_en_pt.txt ──┼─→ dictionary.js ─→ words.json
        ├─ cmudict.dict ──────────┤        │
        ├─ Tatoeba.en-pt.* ───────┘        │
        └─ en_freq_50k.txt ────────────────┤
                                           ↓
           src/curriculum.js ─────→ build.js ─→ curriculum.json
           (55 unidades à mão)
```

| Arquivo | Papel |
|---|---|
| `src/curriculum.js` | **A única decisão pedagógica.** As 55 unidades escritas à mão: ordem, ponto de gramática e tema. Tudo o mais é derivado daqui. |
| `src/sources.js` | Lê e normaliza as cinco fontes; concilia as classes gramaticais e descarta nome próprio, interjeição e ruído de legenda |
| `src/ipa.js` | Converte ARPAbet do CMUdict para IPA, recuando a marca de tônica para o início da sílaba |
| `src/sentences.js` | Escolhe a frase de exemplo exigindo que **todas** as outras palavras sejam mais frequentes que a palavra-alvo — a frase sai graduada por construção |
| `src/distractors.js` | Monta as alternativas erradas: mesma classe, frequência vizinha, sem sinônimo |
| `src/dictionary.js` | Junta tudo em `words.json` |
| `src/build.js` | Expande o currículo em lições e exercícios |
| `src/validate.js` | Pega erro sistemático antes de virar banco |

## Decisões que valem saber

**Uma palavra só entra se tiver tradução, pronúncia e classe.** Meio verbete é
pior que verbete nenhum: apareceria quebrado em alguma tela.

**Nome próprio fica de fora.** O Wiktionary marca 2.839 deles (`Lisbon`,
`Shakespeare`); ensinar nome próprio é ocupar vaga sem ensinar língua. O mesmo
para interjeição (`hmm`, `whoa`) e anotação de legenda (`[chuckles]`), que
sobem alto na lista de frequência do OpenSubtitles.

**A ordem de frequência é a ordem pedagógica.** A unidade 1 recebe as 75
palavras mais comuns do inglês; a 55 recebe as menos comuns das 4.125
ensinadas. As outras ~7.700 vivem no dicionário e na revisão espaçada.

**Tudo é determinístico.** Nenhum sorteio: rodar de novo não embaralha a trilha
de quem já está no meio dela.

## O que NÃO sai daqui

As **55 lições de gramática** são texto pedagógico e são escritas à mão em
`grammar/`. O `curriculum.js` guarda só o roteiro de cada uma (`points`); o
texto em si e os exercícios de gramática vêm do arquivo escrito.
