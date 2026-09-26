/**
 * Expande o currículo em lições e exercícios concretos.
 *
 * Esta é a etapa que transforma "unidade 3 ensina 75 palavras" em 385 lições
 * com ~4.000 exercícios prontos. É tudo determinístico: mesma entrada, mesma
 * saída, sem sorteio — rodar de novo não embaralha a trilha de quem já está
 * no meio dela.
 *
 * As palavras são distribuídas por frequência: a unidade 1 recebe as 75 mais
 * comuns do inglês, a unidade 55 as menos comuns das 4.125 ensinadas. Assim a
 * ordem pedagógica e a ordem de utilidade são a mesma coisa.
 *
 * A lição de GRAMÁTICA sai daqui sem exercícios de propósito: eles dependem do
 * texto da explicação, que é escrito à mão em grammar/. O seeder junta os dois.
 */

import {
  SENTENCES_PER_UNIT, UNITS, VOCAB_LESSONS_PER_UNIT, WORDS_PER_LESSON,
} from "./curriculum.js";
import { GRAMATICA } from "../grammar/index.js";

/** Embaralho determinístico: mesma semente, mesma ordem, sempre. */
function embaralhar(itens, semente) {
  const saida = [...itens];
  let estado = semente * 9301 + 49297;
  for (let i = saida.length - 1; i > 0; i--) {
    estado = (estado * 9301 + 49297) % 233280;
    const j = Math.floor((estado / 233280) * (i + 1));
    [saida[i], saida[j]] = [saida[j], saida[i]];
  }
  return saida;
}

/** Tira pontuação: os blocos do WORD_BANK não têm vírgula nem ponto. */
const semPontuacao = (frase) =>
  frase.replace(/[.,!?;:"“”'’()]/g, "").replace(/\s+/g, " ").trim();

/** Acha a palavra dentro da frase, aceitando flexão (work → working). */
function acharNaFrase(frase, lema) {
  const regex = new RegExp(`\\b${lema}(s|es|ed|d|ing)?\\b`, "i");
  return frase.match(regex)?.[0] ?? null;
}

export function montarCurriculo(dicionario) {
  const unidades = [];
  let proximoId = { licao: 1, exercicio: 1 };

  UNITS.forEach((unidade, indiceUnidade) => {
    // As 75 palavras desta unidade, na fatia de frequência que lhe cabe.
    const inicio = indiceUnidade * (WORDS_PER_LESSON * VOCAB_LESSONS_PER_UNIT);
    const palavras = dicionario.slice(inicio, inicio + WORDS_PER_LESSON * VOCAB_LESSONS_PER_UNIT);
    if (palavras.length === 0) return;

    const licoes = [];

    /*
     * 1. Gramática — texto e exercícios vêm de grammar/, escritos à mão.
     *
     * Unidade cuja lição ainda não foi escrita simplesmente NÃO ganha lição de
     * gramática; ela começa pelo vocabulário. Emitir uma lição vazia seria pior
     * que não emitir: a pessoa abriria a primeira lição da unidade e encontraria
     * uma tela sem nada para fazer.
     */
    const escrita = GRAMATICA[unidade.grammar.slug];
    if (escrita) {
      licoes.push({
        id: proximoId.licao++,
        order: 1,
        kind: "GRAMMAR",
        title: escrita.title,
        grammarSlug: unidade.grammar.slug,
        grammar: { slug: unidade.grammar.slug, title: escrita.title, markdown: escrita.markdown },
        // O id do exercício é atribuído aqui, não no arquivo escrito: assim
        // acrescentar uma lição no meio não renumera o que já existe.
        exercises: escrita.exercises.map((e) => ({
          ...e,
          id: proximoId.exercicio++,
          answer: e.answer ?? "",
        })),
      });
    }

    // ---- 2. vocabulário ----
    for (let n = 0; n < VOCAB_LESSONS_PER_UNIT; n++) {
      const fatia = palavras.slice(n * WORDS_PER_LESSON, (n + 1) * WORDS_PER_LESSON);
      if (!fatia.length) break;

      const exercicios = [];
      fatia.forEach((palavra, i) => {
        exercicios.push(...exerciciosDaPalavra(palavra, proximoId, i));
      });

      licoes.push({
        id: proximoId.licao++,
        order: licoes.length + 1,
        kind: "VOCAB",
        title: tituloDaLicao(unidade, n),
        exercises: exercicios,
      });
    }

    // ---- 3. revisão ----
    licoes.push({
      id: proximoId.licao++,
      order: licoes.length + 1,
      kind: "REVIEW",
      title: "Revisão da unidade",
      exercises: exerciciosDeRevisao(palavras, proximoId, indiceUnidade),
    });

    unidades.push({
      id: indiceUnidade + 1,
      slug: unidade.slug,
      order: indiceUnidade + 1,
      level: unidade.level,
      title: unidade.title,
      theme: unidade.theme,
      wordIds: palavras.map((p) => p.id),
      lessons: licoes,
    });
  });

  preencherOpcoesDaLacuna(unidades, dicionario);
  return unidades;
}

/**
 * A lacuna testa a palavra EM INGLÊS, então as alternativas também são em
 * inglês — e da mesma classe gramatical, senão a gramática da frase entrega a
 * resposta ("She is ___ teacher" só aceita artigo, e aí não se testa nada).
 */
function preencherOpcoesDaLacuna(unidades, dicionario) {
  const porClasse = new Map();
  for (const palavra of dicionario) {
    let lista = porClasse.get(palavra.pos);
    if (!lista) porClasse.set(palavra.pos, (lista = []));
    lista.push(palavra);
  }
  const porId = new Map(dicionario.map((p) => [p.id, p]));

  for (const unidade of unidades) {
    for (const licao of unidade.lessons) {
      // Percorre de trás para frente: exercício sem alternativa viável é
      // removido, e remover de trás não desloca o que ainda falta visitar.
      for (let i = licao.exercises.length - 1; i >= 0; i--) {
        const exercicio = licao.exercises[i];
        if (exercicio.type !== "FILL_BLANK" || exercicio.options) continue;

        const palavra = porId.get(exercicio.wordId);
        const vizinhas = porClasse.get(palavra?.pos) ?? [];
        const centro = vizinhas.indexOf(palavra);
        const alternativas = [];

        for (let raio = 1; raio < vizinhas.length && alternativas.length < 3; raio++) {
          for (const j of [centro - raio, centro + raio]) {
            if (alternativas.length >= 3) break;
            const outra = vizinhas[j];
            if (!outra || outra === palavra) continue;
            if (alternativas.includes(outra.lemma)) continue;
            alternativas.push(outra.lemma);
          }
        }

        if (alternativas.length < 3) {
          licao.exercises.splice(i, 1);
          continue;
        }
        exercicio.options = embaralhar([exercicio.answer, ...alternativas], exercicio.id);
      }
    }
  }
}

/** Nome da lição de vocabulário: o tema da unidade mais o número. */
function tituloDaLicao(unidade, n) {
  const partes = unidade.theme.split(/,|\se\s/).map((p) => p.trim()).filter(Boolean);
  return partes[n] ? capitalizar(partes[n]) : `Mais vocabulário ${n + 1 - partes.length}`;
}

const capitalizar = (t) => t.charAt(0).toUpperCase() + t.slice(1);

/**
 * Os exercícios de uma palavra dentro da lição que a apresenta: dois.
 *
 * O primeiro é sempre o flashcard, que apresenta. O segundo varia conforme a
 * posição da palavra na lição — escolher, ouvir, completar — para a lição ter
 * ritmo em vez de quinze perguntas iguais em sequência.
 *
 * Dois e não quatro porque a palavra não precisa ser esgotada aqui: ela volta
 * na revisão da unidade e, depois, na revisão espaçada. Enfileirar tudo de uma
 * vez cansa sem fixar melhor.
 */
function exerciciosDaPalavra(palavra, ids, posicao = 0) {
  const saida = [
    {
      id: ids.exercicio++,
      type: "FLASHCARD",
      wordId: palavra.id,
      prompt: palavra.lemma,
      answer: palavra.translationPt,
    },
  ];

  const temOpcoes = palavra.distractors.length === 3;
  const lacunaPossivel = palavra.exampleEn && acharNaFrase(palavra.exampleEn, palavra.lemma);

  // Rodízio pela posição, caindo para o que for possível com os dados desta
  // palavra: nem toda palavra tem frase de exemplo ou distratores.
  const preferencia = ["CHOICE", "LISTEN", "FILL_BLANK"][posicao % 3];
  const ordem = [preferencia, "CHOICE", "LISTEN", "FILL_BLANK"];

  for (const tipo of ordem) {
    if ((tipo === "CHOICE" || tipo === "LISTEN") && !temOpcoes) continue;
    if (tipo === "FILL_BLANK" && !lacunaPossivel) continue;

    if (tipo === "CHOICE") {
      saida.push({
        id: ids.exercicio++,
        type: "CHOICE",
        wordId: palavra.id,
        prompt: `O que significa "${palavra.lemma}"?`,
        answer: palavra.translationPt,
        options: embaralhar([palavra.translationPt, ...palavra.distractors], palavra.id),
      });
    } else if (tipo === "LISTEN") {
      saida.push({
        id: ids.exercicio++,
        type: "LISTEN",
        wordId: palavra.id,
        prompt: palavra.lemma,
        answer: palavra.translationPt,
        options: embaralhar([palavra.translationPt, ...palavra.distractors], palavra.id + 7),
      });
    } else {
      saida.push({
        id: ids.exercicio++,
        type: "FILL_BLANK",
        wordId: palavra.id,
        prompt: palavra.exampleEn.replace(lacunaPossivel, "___"),
        answer: lacunaPossivel,
        options: null,
        sentencePt: palavra.examplePt,
      });
    }
    break;
  }

  return saida;
}

/** Revisão: parear, digitar e montar frase — sem reconhecimento passivo. */
function exerciciosDeRevisao(palavras, ids, semente) {
  const saida = [];
  const comFrase = palavras.filter((p) => p.exampleEn);

  // Parear, em grupos de quatro.
  const paraParear = embaralhar(palavras, semente + 1).slice(0, 8);
  for (let i = 0; i < paraParear.length; i += 4) {
    const grupo = paraParear.slice(i, i + 4);
    if (grupo.length < 4) break;
    saida.push({
      id: ids.exercicio++,
      type: "MATCH",
      prompt: "Ligue as palavras",
      answer: "",
      pairs: grupo.map((p) => ({ en: p.lemma, pt: p.translationPt.split(",")[0].trim() })),
    });
  }

  // Digitar.
  for (const palavra of embaralhar(palavras, semente + 2).slice(0, 4)) {
    saida.push({
      id: ids.exercicio++,
      type: "TYPE",
      wordId: palavra.id,
      prompt: `Como se escreve "${palavra.translationPt.split(",")[0].trim()}" em inglês?`,
      answer: palavra.lemma,
    });
  }

  // Montar frase, só com frases curtas — bloco demais vira quebra-cabeça.
  for (const palavra of embaralhar(comFrase, semente + 3).slice(0, 3)) {
    const limpa = semPontuacao(palavra.exampleEn);
    if (limpa.split(" ").length > 8) continue;
    saida.push({
      id: ids.exercicio++,
      type: "WORD_BANK",
      wordId: palavra.id,
      prompt: "Monte a frase em inglês",
      answer: limpa,
      sentencePt: palavra.examplePt,
    });
  }

  return saida;
}

export { SENTENCES_PER_UNIT };
