/**
 * Escolhe a frase de exemplo de cada palavra, do acervo do Tatoeba.
 *
 * O critério é o que torna este caminho melhor que inventar frase: uma boa
 * frase de exemplo para a palavra nº 800 não pode conter a palavra nº 6.000.
 * Como cada palavra tem posição na lista de frequência, dá para exigir que
 * TODAS as outras palavras da frase sejam mais comuns que a palavra-alvo —
 * ou seja, que o aluno já as tenha visto. A frase sai graduada por construção,
 * sem ninguém julgar nível.
 *
 * Entre as frases que passam, vence a mais curta: menos ruído em volta da
 * palavra que está sendo ensinada.
 */

/** Índice invertido lema → índices das frases que o contêm. */
export function indexarFrases(frases, lemas) {
  const indice = new Map();

  frases.forEach((frase, i) => {
    const vistos = new Set();
    for (const token of frase.palavras) {
      const lema = lemas.get(token) ?? token;
      if (vistos.has(lema)) continue;
      vistos.add(lema);
      let lista = indice.get(lema);
      if (!lista) indice.set(lema, (lista = []));
      lista.push(i);
    }
    // Guardamos os lemas da frase para não recalcular na hora de avaliar.
    frase.lemas = vistos;
  });

  return indice;
}

/**
 * @param folga Quantas posições de frequência acima do alvo ainda aceitamos.
 *   Zero seria rígido demais — quase toda frase natural traz um nome ou um
 *   conectivo fora da faixa. Uma folga generosa mantém o espírito (nada de
 *   palavra rara numa frase de iniciante) sem rejeitar frase boa.
 */
export function escolherFrase(lema, { indice, frases, posicao, folga = 1500 }) {
  const candidatas = indice.get(lema);
  if (!candidatas?.length) return null;

  const limite = (posicao.get(lema) ?? 0) + folga;
  let melhor = null;

  for (const i of candidatas) {
    const frase = frases[i];

    let cabe = true;
    for (const outro of frase.lemas) {
      if (outro === lema) continue;
      const pos = posicao.get(outro);
      // Palavra fora da lista de frequência é nome próprio ou coisa rara:
      // não serve de contexto para quem está começando.
      if (pos === undefined || pos > limite) {
        cabe = false;
        break;
      }
    }
    if (!cabe) continue;

    if (!melhor || frase.palavras.length < melhor.palavras.length) melhor = frase;
  }

  return melhor ? { en: melhor.en, pt: melhor.pt } : null;
}
