/**
 * Carrega e normaliza as cinco fontes de dados do curso.
 *
 * Nenhuma delas chama serviço pago: tudo é arquivo aberto versionado em data/.
 * Veja data/LICENSES.md para a atribuição de cada uma.
 *
 * O trabalho aqui é de tradução de formatos e de reconciliação: cada fonte
 * indexa palavra de um jeito (o CMUdict aceita variantes entre parênteses, o
 * Wiktionary mistura glosa com marcação, a lista de frequência conta formas
 * flexionadas), e o resto do gerador só quer saber de lema.
 */

import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { arpabetParaIpa } from "./ipa.js";

const DATA = join(dirname(fileURLToPath(import.meta.url)), "..", "data");

const ler = (arquivo) => readFile(join(DATA, arquivo), "utf8");

/**
 * Classes gramaticais em português.
 *
 * As duas fontes etiquetam diferente: a planilha escreve por extenso ("noun"),
 * o Wiktionary abrevia ("n"). As duas grafias entram no mesmo mapa.
 */
const CLASSES = {
  noun: "substantivo", n: "substantivo",
  verb: "verbo", v: "verbo",
  adjective: "adjetivo", adj: "adjetivo",
  adverb: "advérbio", adv: "advérbio",
  pronoun: "pronome", pron: "pronome",
  preposition: "preposição", prep: "preposição",
  conjunction: "conjunção", conj: "conjunção",
  article: "artigo",
  determiner: "determinante", det: "determinante",
  number: "numeral", num: "numeral",
  "verb modal": "verbo modal",
  interjection: "interjeição", interj: "interjeição",
};

/**
 * Etiquetas do Wiktionary que desqualificam a palavra para o curso.
 * `prop` são nomes próprios (2.839 deles) — "Lisbon", "Shakespeare". Ensinar
 * nome próprio como vocabulário é ocupar vaga sem ensinar língua.
 */
const CLASSES_REJEITADAS = new Set([
  "prop", "prefix", "suffix", "initialism", "abbr", "acronym", "phrase", "particle",
]);

/**
 * Interjeição, nome próprio e anotação de efeito sonoro de legenda
 * ("[chuckles]", "[grunts]") sobem muito alto na lista de frequência do
 * OpenSubtitles e não têm o que ensinar. Palavrão fica de fora por escolha:
 * é um curso que alguém pode usar perto dos filhos.
 */
const DESCARTAR = new Set([
  "um","uh","uhh","hmm","hm","mm","mmm","mhm","ooh","oooh","ah","ahh","aah","oh","ohh","eh",
  "er","err","huh","heh","hey","yo","ya","yeah","yep","yup","nope","nah","whoa","wow","ugh",
  "argh","shh","psst","gosh","golly","bravo","cheers","ho","ha","hah","aw","aww","ow","ouch",
  "chuckles","grunts","groans","sighs","laughs","gasps","screams","chuckling","grunting",
  "sniffles","sobbing","panting","whispering","indistinct","chatter","bleep","beep",
  "lt","sgt","mr","mrs","ms","dr","jr","sr","st","mister","madam","ma","fella","folks",
  "fuck","fucking","fucked","shit","bitch","asshole","damn","goddamn","motherfucker","pussy",
  "dick","cunt","bastard","whore","slut","nigga","faggot",
]);

/** Só letras minúsculas; nada de número, hífen ou apóstrofo. */
const ehPalavra = (w) => /^[a-z]{2,}$/.test(w);

// ---------------------------------------------------------------------------

/** Planilha do usuário: 5.000 palavras com tradução pt-BR feita por humano. */
export async function carregarListaDoUsuario() {
  const texto = await ler("user_5000_pt.csv");
  const mapa = new Map();

  for (const linha of texto.split("\n")) {
    // CSV com campos entre aspas: "o, a, os, as" é UM campo, não quatro.
    const campos = linha.match(/("([^"]*)"|[^,]*)(,|$)/g)?.map((c) =>
      c.replace(/,$/, "").replace(/^"|"$/g, "").trim(),
    );
    if (!campos || campos.length < 4) continue;
    const [rank, palavra, traducao, classe] = campos;
    if (!/^\d+$/.test(rank)) continue;

    // O arquivo lista verbo como "to be"; o curso indexa por lema.
    const lema = palavra.trim().toLowerCase().replace(/^to /, "");
    if (!ehPalavra(lema) || !traducao) continue;

    mapa.set(lema, {
      traducao: limparTraducao(traducao),
      classe: CLASSES[classe.trim().toLowerCase()] ?? "outro",
      rankUsuario: Number(rank),
    });
  }
  return mapa;
}

/**
 * A planilha tem erros de digitação do autor ("essea, aquelea") e às vezes
 * lista cinco sinônimos. Cortamos em dois e tiramos espaço duplicado — sem
 * inventar tradução, só limpando o que está lá.
 */
function limparTraducao(bruto) {
  return bruto
    .split(/[,;]/)
    .map((t) => t.trim())
    .filter(Boolean)
    .slice(0, 2)
    .join(", ")
    .replace(/\s+/g, " ");
}

/** Wiktionary EN→PT: cobre a cauda que a planilha não alcança. */
export async function carregarWiktionary() {
  const texto = await ler("wiktionary_en_pt.txt");
  const mapa = new Map();

  for (const linha of texto.split("\n")) {
    if (linha.startsWith("#") || !linha.includes("::")) continue;
    const [esquerda, direita] = linha.split("::");
    if (direita.includes("SEE:")) continue;

    const lema = esquerda.replace(/\{.*?\}|\(.*?\)/g, "").trim().toLowerCase();
    if (!ehPalavra(lema)) continue;

    // A classe vem entre chaves: "dog {noun} (animal) :: cão"
    const classeBruta = esquerda.match(/\{(\w+)\}/)?.[1]?.toLowerCase();
    if (classeBruta && CLASSES_REJEITADAS.has(classeBruta)) continue;
    const traducoes = direita
      .replace(/\{.*?\}|\(.*?\)|\[.*?\]|\/.*?\//g, "")
      .split(/[,;]/)
      .map((t) => t.trim())
      .filter((t) => t && /^[a-zà-ú]/i.test(t));
    if (!traducoes.length) continue;

    // Primeira ocorrência ganha: o Wiktionary lista da acepção mais comum
    // para a mais rara.
    if (!mapa.has(lema)) {
      mapa.set(lema, {
        traducao: traducoes.slice(0, 2).join(", "),
        classe: CLASSES[classeBruta] ?? "outro",
      });
    }
  }
  return mapa;
}

/** CMUdict → IPA. Variantes ("read(2)") são ignoradas: fica a principal. */
export async function carregarPronuncias() {
  const texto = await ler("cmudict.dict");
  const mapa = new Map();

  for (const linha of texto.split("\n")) {
    const [cabeca, ...fones] = linha.trim().split(/\s+/);
    if (!cabeca || !fones.length) continue;
    if (cabeca.includes("(")) continue;
    const palavra = cabeca.toLowerCase();
    if (!ehPalavra(palavra) || mapa.has(palavra)) continue;
    // O dicionário tem comentários depois de "#".
    const limpos = fones.filter((f) => /^[A-Z]+\d?$/.test(f));
    if (limpos.length) mapa.set(palavra, arpabetParaIpa(limpos));
  }
  return mapa;
}

/** Mapa forma flexionada → lema, da NGSL. */
export async function carregarLemas() {
  const texto = await ler("ngsl_form_lemmas.txt");
  const mapa = new Map();
  for (const linha of texto.split("\n")) {
    if (linha.startsWith("#") || !linha.includes("\t")) continue;
    const [forma, lema] = linha.split("\t");
    if (forma && lema) mapa.set(forma.trim().toLowerCase(), lema.trim().toLowerCase());
  }
  return mapa;
}

/** Ordem de frequência (OpenSubtitles), já reduzida a lemas únicos. */
export async function carregarFrequencia(lemas) {
  const texto = await ler("en_freq_50k.txt");
  const vistos = new Set();
  const ordem = [];
  for (const linha of texto.split("\n")) {
    const palavra = linha.split(/\s+/)[0]?.toLowerCase();
    if (!palavra || !ehPalavra(palavra)) continue;
    const lema = lemas.get(palavra) ?? palavra;
    if (vistos.has(lema)) continue;
    vistos.add(lema);
    ordem.push(lema);
  }
  return ordem;
}

/** Pares de frase EN↔PT do Tatoeba, filtrados por tamanho utilizável. */
export async function carregarFrases() {
  const [en, pt] = await Promise.all([
    ler("Tatoeba.en-pt.en"),
    ler("Tatoeba.en-pt.pt"),
  ]);
  const linhasEn = en.split("\n");
  const linhasPt = pt.split("\n");
  const frases = [];

  for (let i = 0; i < linhasEn.length; i++) {
    const textoEn = linhasEn[i]?.trim();
    const textoPt = linhasPt[i]?.trim();
    if (!textoEn || !textoPt) continue;
    const palavras = textoEn.toLowerCase().match(/[a-z']+/g) ?? [];
    // Menos de quatro palavras não ensina estrutura; mais de doze cansa e
    // costuma trazer vocabulário fora do nível.
    if (palavras.length < 4 || palavras.length > 12) continue;
    frases.push({ en: textoEn, pt: textoPt, palavras });
  }
  return frases;
}

export { DESCARTAR, ehPalavra };
