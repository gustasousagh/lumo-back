#!/usr/bin/env node
/**
 * Publica o curso para o front consumir como arquivo estático.
 *
 * Enquanto a API `/api/language` não existe, a aba /idiomas lê o curso de
 * `public/language/en/course.json`. Isso não é rascunho: é o conteúdo real,
 * o mesmo que o seeder vai carregar no banco. Serve para o app ficar
 * utilizável — e publicável — antes do backend existir.
 *
 * Só as unidades vão: cada exercício já carrega enunciado, resposta e opções
 * prontos, então o player não precisa do dicionário. As 11.829 palavras ficam
 * de fora do que o navegador baixa (seriam 2,7 MB para nada).
 *
 *   node src/export-web.js
 */

import { readFile, writeFile, mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "..");
const ENTRADA = join(RAIZ, "src", "main", "resources", "language", "en", "curriculum.json");
const SAIDA = join(RAIZ, "..", "lumo-web", "public", "language", "en");

const { contentVersion, units } = JSON.parse(await readFile(ENTRADA, "utf8"));

// `wordIds` só interessa ao seeder, para ligar unidade e palavra no banco.
const enxutas = units.map(({ wordIds, ...unidade }) => ({
  ...unidade,
  lessons: unidade.lessons.map((licao) => ({ ...licao, unitId: unidade.id })),
}));

await mkdir(SAIDA, { recursive: true });
const json = JSON.stringify({ contentVersion, language: "en", units: enxutas });
await writeFile(join(SAIDA, "course.json"), json);

const licoes = enxutas.flatMap((u) => u.lessons);
console.log(`${join(SAIDA, "course.json")}`);
console.log(`  ${enxutas.length} unidades · ${licoes.length} lições · ${licoes.flatMap((l) => l.exercises).length} exercícios · ${(json.length / 1024).toFixed(0)} KB`);
