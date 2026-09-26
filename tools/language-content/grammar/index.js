/**
 * Junta as lições de gramática de todos os níveis.
 *
 * Um arquivo por nível, e não um por lição, porque revisar a progressão de A1
 * exige ver as doze lições de A1 lado a lado — a ordem em que os pontos
 * aparecem é decisão pedagógica tanto quanto o texto de cada um.
 */

import { A1 } from "./a1.js";

export const GRAMATICA = { ...A1 };

/** Slugs sem lição escrita ainda, para o gerador avisar em vez de falhar. */
export function pendentes(slugs) {
  return slugs.filter((slug) => !GRAMATICA[slug]);
}
