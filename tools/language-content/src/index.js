#!/usr/bin/env node
/**
 * Gera todo o conteúdo do curso de inglês.
 *
 * Roda offline, em segundos, e não chama nenhum serviço pago: as fontes são
 * arquivos abertos em data/ (veja data/LICENSES.md). A saída vai para
 * src/main/resources/language/en/, de onde o seeder da API carrega no boot.
 *
 *   npm run generate
 */

import { writeFile, mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { montarDicionario } from "./dictionary.js";
import { montarCurriculo } from "./build.js";

const SAIDA = join(
  dirname(fileURLToPath(import.meta.url)),
  "..", "..", "..", "src", "main", "resources", "language", "en",
);

/** Sobe junto com o conteúdo; o seeder compara e só reimporta se mudou. */
const CONTENT_VERSION = 1;

const log = (m) => console.log(m);

async function gravar(nome, dados) {
  const caminho = join(SAIDA, nome);
  await writeFile(caminho, JSON.stringify(dados));
  const kb = (JSON.stringify(dados).length / 1024).toFixed(0);
  log(`  ${nome.padEnd(18)} ${kb.padStart(6)} KB`);
}

async function main() {
  log("\n== dicionário ==");
  const dicionario = await montarDicionario((m) => log(`  ${m}`));

  log("\n== currículo ==");
  const unidades = montarCurriculo(dicionario);
  const licoes = unidades.flatMap((u) => u.lessons);
  const exercicios = licoes.flatMap((l) => l.exercises);
  log(`  ${unidades.length} unidades, ${licoes.length} lições, ${exercicios.length} exercícios`);

  const porTipo = {};
  for (const e of exercicios) porTipo[e.type] = (porTipo[e.type] ?? 0) + 1;
  for (const [tipo, n] of Object.entries(porTipo).sort((a, b) => b[1] - a[1])) {
    log(`    ${tipo.padEnd(12)} ${String(n).padStart(6)}`);
  }

  log("\n== gravando ==");
  await mkdir(SAIDA, { recursive: true });
  await gravar("words.json", { contentVersion: CONTENT_VERSION, language: "en", words: dicionario });
  await gravar("curriculum.json", { contentVersion: CONTENT_VERSION, language: "en", units: unidades });

  log("\npronto — custo: US$ 0,00\n");
}

main().catch((erro) => {
  console.error("\nfalhou:", erro.message);
  process.exit(1);
});
