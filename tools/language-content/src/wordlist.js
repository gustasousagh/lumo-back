/**
 * Monta a lista de 3.000 palavras que o curso ensina.
 *
 * A espinha dorsal é a NGSL (New General Service List): 2.818 headwords
 * ordenados por frequência que cobrem cerca de 92% do inglês geral. É uma lista
 * curada e lematizada — muito melhor ponto de partida do que "as 3.000 palavras
 * mais comuns", que viria cheia de nome próprio, fragmento de contração e vinte
 * formas do mesmo verbo.
 *
 * O que falta para chegar a 3.000 vem da lista de frequência do OpenSubtitles,
 * que puxa o vocabulário de fala corrente que a NGSL (baseada em corpus escrito)
 * subrepresenta. Filtramos contra o mapa forma->lema da própria NGSL para não
 * repetir "walked" depois de já ter "walk".
 *
 * Fontes em data/ — veja data/LICENSES.md.
 */

import { createReadStream } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { createInterface } from 'node:readline';

const DATA_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'data');

/** Quantas palavras o curso tem no fim. */
export const TARGET_WORDS = 3000;

/**
 * Pedimos mais candidatos do que o necessário porque o Claude ainda vai
 * descartar o que sobreviveu aos filtros mecânicos (nome próprio, sigla,
 * fragmento). Sobrando candidato, chegamos aos 3.000 sem uma segunda rodada.
 */
export const CANDIDATE_OVERFETCH = 3400;

/**
 * Nomes próprios frequentes em legendas. A lista de frequência não marca classe
 * gramatical, e nomes de personagem aparecem muito alto — "john" fica na frente
 * de palavras que o curso realmente precisa ensinar.
 */
const COMMON_NAMES = new Set([
  'john', 'tom', 'jack', 'mike', 'david', 'peter', 'george', 'james', 'harry',
  'sam', 'frank', 'charlie', 'billy', 'joe', 'bob', 'paul', 'nick', 'ben',
  'mary', 'sarah', 'anna', 'lucy', 'kate', 'jane', 'emma', 'alice', 'laura',
  'chris', 'danny', 'eddie', 'jimmy', 'johnny', 'tony', 'max', 'alex', 'ray',
  'america', 'american', 'england', 'english', 'french', 'german', 'china',
  'york', 'london', 'paris', 'christmas', 'god', 'jesus', 'lord', 'sir',
  'mr', 'mrs', 'ms', 'dr', 'jr',
]);

/** Só letras minúsculas, sem hífen nem apóstrofo, com pelo menos duas letras. */
function isPlausibleWord(word) {
  if (!/^[a-z]+$/.test(word)) return false;
  if (word.length < 2) return word === 'a' || word === 'i';
  return true;
}

/** Lê a NGSL: cada linha é `headword,definição`, já em ordem de frequência. */
async function readNgsl() {
  const raw = await readFile(join(DATA_DIR, 'ngsl.csv'), 'utf8');
  const words = [];
  const seen = new Set();

  // A NGSL original vem com quebra de linha \r (Mac clássico). Normalizamos
  // antes de qualquer coisa, senão o arquivo inteiro vira uma linha só.
  for (const line of raw.replace(/\r\n?/g, '\n').split('\n')) {
    if (!line.trim()) continue;
    const comma = line.indexOf(',');
    if (comma < 0) continue;

    const lemma = line.slice(0, comma).trim().toLowerCase();
    let definition = line.slice(comma + 1).trim();
    if (definition.startsWith('"') && definition.endsWith('"')) {
      definition = definition.slice(1, -1).replace(/""/g, '"');
    }
    // \xa0 (espaço não separável) aparece solto no meio das definições da fonte.
    definition = definition.replace(/ /g, ' ').replace(/\s+/g, ' ').trim();

    if (!isPlausibleWord(lemma) || seen.has(lemma)) continue;
    seen.add(lemma);
    words.push({ lemma, sourceDefinition: definition, source: 'ngsl' });
  }
  return words;
}

/** Mapa forma flexionada -> headword, para não reintroduzir o que já existe. */
async function readLemmaMap() {
  const raw = await readFile(join(DATA_DIR, 'ngsl_form_lemmas.txt'), 'utf8');
  const map = new Map();
  for (const line of raw.split('\n')) {
    if (!line.trim() || line.startsWith('#')) continue;
    const [form, head] = line.split('\t');
    if (form && head) map.set(form.trim().toLowerCase(), head.trim().toLowerCase());
  }
  return map;
}

/** Lista de frequência do OpenSubtitles: `palavra contagem` por linha. */
async function readFrequencyList() {
  const out = [];
  const stream = createInterface({
    input: createReadStream(join(DATA_DIR, 'en_freq_10k.txt'), 'utf8'),
    crlfDelay: Infinity,
  });
  for await (const line of stream) {
    const [word] = line.trim().split(/\s+/);
    if (word) out.push(word.toLowerCase());
  }
  return out;
}

/**
 * Devolve os candidatos em ordem de frequência: a NGSL inteira primeiro, depois
 * o complemento vindo das legendas. O índice no array é o `frequencyRank`, e é
 * ele que decide em que unidade a palavra vai cair.
 */
export async function buildCandidates() {
  const [ngsl, lemmaMap, frequency] = await Promise.all([
    readNgsl(),
    readLemmaMap(),
    readFrequencyList(),
  ]);

  const taken = new Set(ngsl.map((w) => w.lemma));
  const candidates = [...ngsl];

  for (const word of frequency) {
    if (candidates.length >= CANDIDATE_OVERFETCH) break;
    if (!isPlausibleWord(word)) continue;
    if (COMMON_NAMES.has(word)) continue;
    if (taken.has(word)) continue;

    // Se a palavra é uma forma flexionada de algo que a NGSL já tem, o curso já
    // ensina o lema — repetir "children" depois de "child" só ocuparia vaga.
    const head = lemmaMap.get(word);
    if (head && taken.has(head)) continue;

    taken.add(word);
    candidates.push({ lemma: word, sourceDefinition: null, source: 'opensubtitles' });
  }

  return candidates.map((word, index) => ({ ...word, frequencyRank: index + 1 }));
}
