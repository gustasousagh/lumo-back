/**
 * Monta o dicionário do curso a partir das fontes abertas.
 *
 * Uma palavra só entra se tiver TUDO que os exercícios precisam: tradução,
 * pronúncia e classe gramatical. Sem isso ela apareceria pela metade em alguma
 * tela, e meio verbete é pior que verbete nenhum.
 *
 * A ordem é a de frequência, e é ela que define o nível: as primeiras 750 são
 * o que alguém encontra no primeiro dia de contato com a língua; da 4.000ª em
 * diante já é vocabulário de quem lê jornal.
 */

import {
  carregarFrases, carregarFrequencia, carregarLemas, carregarListaDoUsuario,
  carregarPronuncias, carregarWiktionary, DESCARTAR,
} from "./sources.js";
import { escolherFrase, indexarFrases } from "./sentences.js";
import { escolherDistratores } from "./distractors.js";

/** Faixas de frequência → nível. */
const FAIXAS = [
  [750, "A1"], [1500, "A2"], [2500, "B1"], [4000, "B2"], [Infinity, "C1"],
];

const nivelPara = (rank) => FAIXAS.find(([teto]) => rank <= teto)[1];

export async function montarDicionario(log = () => {}) {
  const [usuario, wiktionary, pronuncias, lemas] = await Promise.all([
    carregarListaDoUsuario(), carregarWiktionary(), carregarPronuncias(), carregarLemas(),
  ]);
  const ordem = await carregarFrequencia(lemas);
  log(`fontes carregadas — ${usuario.size} da planilha, ${wiktionary.size} do wiktionary`);

  // ---- 1. quem entra ----
  const palavras = [];
  const descartes = { lixo: 0, semTraducao: 0, semPronuncia: 0 };

  for (const lema of ordem) {
    if (DESCARTAR.has(lema)) { descartes.lixo++; continue; }

    const daPlanilha = usuario.get(lema);
    const doWikt = wiktionary.get(lema);
    const fonte = daPlanilha ?? doWikt;
    if (!fonte?.traducao) { descartes.semTraducao++; continue; }

    const ipa = pronuncias.get(lema);
    if (!ipa) { descartes.semPronuncia++; continue; }

    // A classe da planilha ganha da do Wiktionary: foi revisada por humano.
    const pos = daPlanilha?.classe && daPlanilha.classe !== "outro"
      ? daPlanilha.classe
      : (doWikt?.classe ?? daPlanilha?.classe ?? "outro");

    const rank = palavras.length + 1;
    palavras.push({
      id: rank,
      lemma: lema,
      pos,
      translationPt: fonte.traducao,
      ipa,
      cefr: nivelPara(rank),
      frequencyRank: rank,
      exampleEn: null,
      examplePt: null,
      distractors: [],
    });
  }
  log(`${palavras.length} palavras entraram (descartes: ${descartes.lixo} lixo, ${descartes.semTraducao} sem tradução, ${descartes.semPronuncia} sem pronúncia)`);

  // ---- 2. frase de exemplo ----
  const frases = await carregarFrases();
  const indice = indexarFrases(frases, lemas);
  const posicao = new Map(palavras.map((p) => [p.lemma, p.frequencyRank]));

  let comFrase = 0;
  for (const palavra of palavras) {
    const frase = escolherFrase(palavra.lemma, { indice, frases, posicao });
    if (frase) {
      palavra.exampleEn = frase.en;
      palavra.examplePt = frase.pt;
      comFrase++;
    }
  }
  log(`${comFrase} palavras com frase de exemplo (${((comFrase / palavras.length) * 100).toFixed(1)}%)`);

  // ---- 3. distratores ----
  const porClasse = new Map();
  for (const palavra of palavras) {
    let lista = porClasse.get(palavra.pos);
    if (!lista) porClasse.set(palavra.pos, (lista = []));
    lista.push(palavra);
  }

  let comDistratores = 0;
  for (const palavra of palavras) {
    palavra.distractors = escolherDistratores(palavra, porClasse);
    if (palavra.distractors.length === 3) comDistratores++;
  }
  log(`${comDistratores} palavras com 3 distratores (${((comDistratores / palavras.length) * 100).toFixed(1)}%)`);

  return palavras;
}
