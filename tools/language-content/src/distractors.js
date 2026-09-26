/**
 * Monta as alternativas erradas da múltipla escolha.
 *
 * Distrator aleatório estraga o exercício: se a pergunta é "o que significa
 * 'chair'?" e as opções são "mesa", "correr" e "azul", acerta quem não faz
 * ideia — basta ver qual é substantivo. O distrator tem que ser do mesmo tipo
 * e da mesma vizinhança de dificuldade, para a escolha depender de saber a
 * palavra.
 *
 * Três regras, todas aprendidas vendo o resultado ruim antes de acertar:
 *   1. Mesma classe gramatical e frequência vizinha (vizinho de frequência é
 *      vizinho de dificuldade).
 *   2. Uma alternativa é UMA palavra. A tradução "escolher, colher" vira só
 *      "escolher" — alternativa com vírgula denuncia qual é a certa quando as
 *      outras não têm.
 *   3. Nada que se confunda com a resposta certa nem com outra alternativa:
 *      sinônimo torna o "erro" injusto, e repetição torna o acerto grátis.
 */

/** Primeira acepção apenas, em minúscula. */
function forma(traducao) {
  return traducao.split(/[,;]/)[0].trim().toLowerCase();
}

/** Palavras significativas, para detectar sobreposição de sentido. */
function tokens(texto) {
  return new Set(texto.toLowerCase().split(/[\s,;]+/).filter((t) => t.length > 2));
}

/**
 * Classes vagas demais para servir de pool: "outro" junta substantivo, verbo e
 * advérbio no mesmo balaio, e o distrator sai absurdo.
 */
const CLASSES_CONFIAVEIS = new Set([
  "substantivo", "verbo", "adjetivo", "advérbio", "pronome", "preposição",
]);

export function escolherDistratores(alvo, porClasse) {
  if (!CLASSES_CONFIAVEIS.has(alvo.pos)) return [];

  const candidatos = porClasse.get(alvo.pos);
  if (!candidatos || candidatos.length < 8) return [];

  const certa = forma(alvo.translationPt);
  const tokensCerta = tokens(alvo.translationPt);
  const escolhidos = [];
  const usados = new Set([certa]);

  const centro = candidatos.indexOf(alvo);
  for (let raio = 1; raio < candidatos.length && escolhidos.length < 3; raio++) {
    for (const i of [centro - raio, centro + raio]) {
      if (escolhidos.length >= 3) break;
      const outro = candidatos[i];
      if (!outro || outro === alvo) continue;

      const texto = forma(outro.translationPt);
      if (!texto || usados.has(texto)) continue;
      // Uma palavra contida na outra ("civil" / "civilizado") confunde sem
      // testar conhecimento.
      if (texto.includes(certa) || certa.includes(texto)) continue;
      if ([...tokens(outro.translationPt)].some((t) => tokensCerta.has(t))) continue;

      usados.add(texto);
      escolhidos.push(texto);
    }
  }

  return escolhidos.length === 3 ? escolhidos : [];
}
