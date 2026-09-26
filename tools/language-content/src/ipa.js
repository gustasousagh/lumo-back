/**
 * Converte a pronúncia do CMUdict (ARPAbet) para IPA.
 *
 * O CMUdict escreve "water" como `W AO1 T ER0`. O aluno precisa ver /ˈwɔːtər/.
 * A conversão é tabela, exceto por uma sutileza: o dígito de tônica (1 = forte,
 * 2 = média, 0 = átona) vem grudado na VOGAL, mas o IPA põe a marca de tônica
 * antes da SÍLABA inteira. Então recuamos a marca por cima do grupo de
 * consoantes que antecede a vogal — é o que faz /ˈwɔːtər/ sair certo em vez de
 * /wˈɔːtər/.
 *
 * Não é silabificação de verdade (isso exige regras de ataque máximo e exceções
 * do inglês), mas acerta a esmagadora maioria das palavras de uma a três
 * sílabas, que é o vocabulário do curso inteiro.
 */

const VOGAIS = {
  AA: "ɑː", AE: "æ", AH: "ʌ", AO: "ɔː", AW: "aʊ", AY: "aɪ",
  EH: "ɛ", ER: "ɜːr", EY: "eɪ", IH: "ɪ", IY: "iː",
  OW: "oʊ", OY: "ɔɪ", UH: "ʊ", UW: "uː",
};

const CONSOANTES = {
  B: "b", CH: "tʃ", D: "d", DH: "ð", F: "f", G: "ɡ", HH: "h", JH: "dʒ",
  K: "k", L: "l", M: "m", N: "n", NG: "ŋ", P: "p", R: "r", S: "s",
  SH: "ʃ", T: "t", TH: "θ", V: "v", W: "w", Y: "j", Z: "z", ZH: "ʒ",
};

/** Vogal átona vira schwa — é o que dá o som natural do inglês falado. */
const ATONAS = { AH: "ə", ER: "ər", AO: "ə", AA: "ə", AE: "ə", EH: "ə", IH: "ɪ", UH: "ʊ", IY: "i", UW: "u", OW: "oʊ" };

export function arpabetParaIpa(fones) {
  const pedacos = [];

  for (const bruto of fones) {
    const tonica = /\d$/.test(bruto) ? Number(bruto.slice(-1)) : null;
    const fone = bruto.replace(/\d$/, "").toUpperCase();

    if (fone in VOGAIS) {
      const som = tonica === 0 ? (ATONAS[fone] ?? VOGAIS[fone]) : VOGAIS[fone];
      pedacos.push({ som, vogal: true, marca: tonica === 1 ? "ˈ" : tonica === 2 ? "ˌ" : "" });
    } else if (fone in CONSOANTES) {
      pedacos.push({ som: CONSOANTES[fone], vogal: false, marca: "" });
    }
  }

  /*
   * O CMUdict marca DUAS tônicas primárias em ~1,3% das entradas
   * ("engineer EH1 N JH AH0 N IH1 R"). Em IPA isso não existe: a palavra tem
   * uma tônica só. A convenção é que a última é a principal e as anteriores
   * viram secundárias — /ˌɛndʒəˈnɪr/, não /ˈɛndʒəˈnɪr/.
   */
  const primarias = pedacos.filter((p) => p.marca === "ˈ");
  if (primarias.length > 1) {
    for (const p of primarias.slice(0, -1)) p.marca = "ˌ";
  }

  // Recua cada marca de tônica até o começo do grupo de consoantes da sílaba.
  const saida = pedacos.map((p) => p.som);
  for (let i = pedacos.length - 1; i >= 0; i--) {
    if (!pedacos[i].vogal || !pedacos[i].marca) continue;
    let inicio = i;
    while (inicio > 0 && !pedacos[inicio - 1].vogal) inicio--;
    // Sílaba no começo da palavra sem marca secundária anterior não precisa de
    // marca: "cat" é /kæt/, não /ˈkæt/.
    if (inicio === 0 && !pedacos.slice(i + 1).some((p) => p.vogal)) continue;
    saida[inicio] = pedacos[i].marca + saida[inicio];
  }

  return `/${saida.join("")}/`;
}
