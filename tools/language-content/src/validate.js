#!/usr/bin/env node
/**
 * Confere o conteúdo gerado antes de ele virar banco de dados.
 *
 * Dezesseis mil exercícios feitos por máquina não se revisam no olho. As regras
 * aqui são as que pegam erro sistemático: resposta que não está entre as
 * opções, lacuna sem lacuna, alternativa repetida, palavra apontando para id
 * inexistente. Qualquer uma delas passaria despercebida até alguém travar no
 * meio de uma lição.
 *
 *   npm run validate
 */

import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const DIR = join(dirname(fileURLToPath(import.meta.url)),
  "..", "..", "..", "src", "main", "resources", "language", "en");

const problemas = [];
const avisos = [];
const erro = (m) => problemas.push(m);
const aviso = (m) => avisos.push(m);

const { words } = JSON.parse(await readFile(join(DIR, "words.json"), "utf8"));
const { units } = JSON.parse(await readFile(join(DIR, "curriculum.json"), "utf8"));

// ---------------------------------------------------------------- palavras
const porId = new Map();
const lemas = new Set();
for (const p of words) {
  if (porId.has(p.id)) erro(`palavra id ${p.id} duplicado`);
  porId.set(p.id, p);
  if (lemas.has(p.lemma)) erro(`lema "${p.lemma}" duplicado`);
  lemas.add(p.lemma);

  if (!p.translationPt) erro(`"${p.lemma}" sem tradução`);
  if (!/^\/.+\/$/.test(p.ipa ?? "")) erro(`"${p.lemma}" com IPA inválido: ${p.ipa}`);
  if (!["A1", "A2", "B1", "B2", "C1"].includes(p.cefr)) erro(`"${p.lemma}" com nível "${p.cefr}"`);
  if (p.distractors.length && p.distractors.length !== 3) erro(`"${p.lemma}" com ${p.distractors.length} distratores`);
  if (new Set(p.distractors).size !== p.distractors.length) erro(`"${p.lemma}" com distrator repetido`);
  if (p.distractors.includes(p.translationPt)) erro(`"${p.lemma}": distrator igual à tradução`);
  if (p.exampleEn && !p.examplePt) erro(`"${p.lemma}" com frase em inglês mas sem tradução`);
}

// ---------------------------------------------------------------- currículo
const idsLicao = new Set();
const idsExercicio = new Set();
let total = 0;

for (const u of units) {
  if (!u.lessons.length) erro(`unidade ${u.slug} sem lições`);
  const gramatica = u.lessons.filter((l) => l.kind === "GRAMMAR");
  if (gramatica.length > 1) erro(`unidade ${u.slug} tem ${gramatica.length} lições de gramática`);
  if (gramatica.length === 0) aviso(`unidade ${u.slug}: gramática "${u.slug}" ainda não escrita`);

  for (const l of u.lessons) {
    if (idsLicao.has(l.id)) erro(`lição id ${l.id} duplicado`);
    idsLicao.add(l.id);
    if (l.kind !== "GRAMMAR" && !l.exercises.length) erro(`lição ${l.id} (${l.kind}) sem exercícios`);
    // Lição de gramática emitida sem texto ou sem exercício é bug: o build
    // só deve emiti-la quando as duas coisas existem.
    if (l.kind === "GRAMMAR" && (!l.grammar?.markdown || !l.exercises.length))
      erro(`lição de gramática ${l.id} emitida incompleta`);

    for (const e of l.exercises) {
      total++;
      if (idsExercicio.has(e.id)) erro(`exercício id ${e.id} duplicado`);
      idsExercicio.add(e.id);
      if (e.wordId && !porId.has(e.wordId)) erro(`exercício ${e.id} aponta para palavra ${e.wordId} inexistente`);

      if (["CHOICE", "FILL_BLANK", "LISTEN"].includes(e.type)) {
        if (!e.options?.length) { erro(`exercício ${e.id} (${e.type}) sem opções`); continue; }
        if (!e.options.includes(e.answer)) erro(`exercício ${e.id}: resposta "${e.answer}" fora das opções`);
        if (new Set(e.options).size !== e.options.length) erro(`exercício ${e.id}: opção repetida`);
        // Três opções é legítimo quando só existem três respostas possíveis
        // (am/is/are). Menos que isso é fácil demais; mais que quatro cansa.
        if (e.options.length < 3 || e.options.length > 4) aviso(`exercício ${e.id}: ${e.options.length} opções`);
      }
      if (e.type === "FILL_BLANK" && !e.prompt.includes("___")) erro(`exercício ${e.id}: lacuna sem "___"`);
      if (e.type === "WORD_BANK") {
        if (/[.,!?;:]/.test(e.answer)) erro(`exercício ${e.id}: WORD_BANK com pontuação em "${e.answer}"`);
        if (e.answer.split(" ").length < 3) erro(`exercício ${e.id}: WORD_BANK curto demais`);
      }
      if (e.type === "MATCH") {
        if (!e.pairs?.length) erro(`exercício ${e.id}: MATCH sem pares`);
        else {
          if (new Set(e.pairs.map((p) => p.pt)).size !== e.pairs.length)
            erro(`exercício ${e.id}: MATCH com duas traduções iguais — impossível de resolver`);
          if (new Set(e.pairs.map((p) => p.en)).size !== e.pairs.length)
            erro(`exercício ${e.id}: MATCH com palavra repetida`);
        }
      }
      if (e.type === "TYPE" && !e.answer?.trim()) erro(`exercício ${e.id}: TYPE sem resposta`);
    }
  }
}

console.log(`${words.length.toLocaleString("pt-BR")} palavras · ${units.length} unidades · ${idsLicao.size} lições · ${total.toLocaleString("pt-BR")} exercícios\n`);

if (avisos.length) {
  console.log(`${avisos.length} aviso(s):`);
  for (const a of avisos.slice(0, 5)) console.log("  ~ " + a);
  if (avisos.length > 5) console.log(`  ~ ...e mais ${avisos.length - 5}`);
  console.log();
}

if (problemas.length) {
  console.log(`${problemas.length} PROBLEMA(S):`);
  const amostra = problemas.slice(0, 15);
  for (const p of amostra) console.log("  - " + p);
  if (problemas.length > amostra.length) console.log(`  - ...e mais ${problemas.length - amostra.length}`);
  process.exit(1);
}
console.log("conteúdo consistente");
