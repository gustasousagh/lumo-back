/**
 * O currículo de inglês, escrito à mão.
 *
 * Este é o único arquivo do gerador que é decisão pedagógica, não automação:
 * define quais pontos de gramática existem, em que ordem, e quanto vocabulário
 * cada unidade carrega. Tudo o mais (palavras, frases, exercícios) é derivado
 * daqui pelo `build.js`.
 *
 * A ordem das unidades importa: ela é ao mesmo tempo a progressão gramatical e
 * o critério de distribuição do vocabulário. A unidade 1 recebe as palavras mais
 * frequentes do inglês, a unidade 40 as menos frequentes das 3.000 — assim quem
 * está começando aprende primeiro o que vai encontrar mais.
 *
 * `points` é o roteiro que o Claude recebe para escrever a explicação da
 * gramática. Quanto mais específico, menos genérica fica a lição.
 */

/**
 * Tamanho da unidade e da lição.
 *
 * Oito palavras por lição, e não quinze, por uma razão medida: com quinze
 * palavras e os exercícios que cada uma merece, a lição chegava a 57
 * exercícios — quinze minutos sentado. Lição boa se faz numa parada de
 * ônibus. Oito palavras rendem cerca de dezesseis exercícios, uns cinco
 * minutos, e a palavra volta de qualquer forma na revisão espaçada.
 */
export const WORDS_PER_UNIT = 72;
export const WORDS_PER_LESSON = 8;
export const VOCAB_LESSONS_PER_UNIT = 9;

/** Frases por unidade (55 x 50 = 2.750). */
export const SENTENCES_PER_UNIT = 50;

export const UNITS = [
  // ===========================================================================
  //  A1 — do zero até se virar em situações previsíveis
  // ===========================================================================
  {
    slug: 'a1-verb-to-be',
    level: 'A1',
    title: 'O verbo to be',
    theme: 'Apresentações, países e nacionalidades',
    grammar: {
      slug: 'present-to-be',
      title: 'To be no presente',
      points: [
        'as três formas: am, is, are, e com quais pronomes cada uma vai',
        'contrações naturais: I\'m, you\'re, he\'s, we\'re — em inglês falado a forma cheia soa formal',
        'negativa com not e as duas contrações possíveis (isn\'t / \'s not)',
        'pergunta invertendo o verbo com o sujeito, sem usar do',
        'erro clássico de brasileiro: "I have 20 years" em vez de "I am 20 years old"',
      ],
    },
  },
  {
    slug: 'a1-articles-plural',
    level: 'A1',
    title: 'Artigos e plural',
    theme: 'Objetos do dia a dia, cores e números',
    grammar: {
      slug: 'articles-plural',
      title: 'A, an, the e o plural',
      points: [
        'a e an: a escolha é pelo SOM da próxima palavra, não pela letra (a university, an hour)',
        'the para algo já conhecido ou único',
        'quando NÃO usar artigo: plurais genéricos, nomes de idiomas, refeições',
        'plural regular com -s, -es, -ies',
        'plurais irregulares mais usados: children, people, men, women, feet',
      ],
    },
  },
  {
    slug: 'a1-pronouns-possessives',
    level: 'A1',
    title: 'Pronomes e possessivos',
    theme: 'Família e relações',
    grammar: {
      slug: 'pronouns-possessives',
      title: 'Pronomes e posse',
      points: [
        'pronomes sujeito (I, you, he...) e objeto (me, you, him...)',
        'adjetivos possessivos: my, your, his, her, its, our, their',
        'pronomes possessivos: mine, yours, his, hers, ours, theirs',
        'o genitivo com \'s: my sister\'s car — a diferença de "the car of my sister"',
        'its vs it\'s: a confusão mais comum do inglês escrito',
      ],
    },
  },
  {
    slug: 'a1-present-simple',
    level: 'A1',
    title: 'Present simple',
    theme: 'Rotina e dias da semana',
    grammar: {
      slug: 'present-simple',
      title: 'Present simple: afirmativa',
      points: [
        'para que serve: rotina, fato permanente, verdade geral',
        'o -s da terceira pessoa (he works) e por que ele some no plural',
        'regras de escrita do -s: -es depois de s/x/ch/sh, -ies depois de consoante + y',
        'o verbo have vira has na terceira pessoa',
        'o -s da terceira pessoa é o erro mais frequente de quem está começando',
      ],
    },
  },
  {
    slug: 'a1-present-simple-questions',
    level: 'A1',
    title: 'Perguntas e negativas no presente',
    theme: 'Trabalho, profissões e estudo',
    grammar: {
      slug: 'do-does',
      title: 'Do e does',
      points: [
        'do/does como auxiliar: não traduz nada, só monta a estrutura',
        'negativa: don\'t / doesn\'t + verbo na forma base',
        'pergunta: Do you...? / Does she...?',
        'o verbo principal perde o -s quando does aparece (NÃO "Does she works")',
        'perguntas com What, Where, When, Who, Why, How',
      ],
    },
  },
  {
    slug: 'a1-there-is-are',
    level: 'A1',
    title: 'There is / there are',
    theme: 'Casa, móveis e lugares',
    grammar: {
      slug: 'there-is-there-are',
      title: 'Existência: there is / there are',
      points: [
        'there is para singular, there are para plural',
        'a diferença entre there is (existe) e it is (é) — em português os dois viram "tem/é"',
        'negativa: there isn\'t / there aren\'t / there\'s no',
        'perguntas: Is there...? Are there...? e as respostas curtas',
        'How many... are there?',
      ],
    },
  },
  {
    slug: 'a1-can',
    level: 'A1',
    title: 'Can e can\'t',
    theme: 'Habilidades, corpo e esportes',
    grammar: {
      slug: 'can-cannot',
      title: 'Can: habilidade, permissão e pedido',
      points: [
        'can nunca muda de forma: nada de "he cans"',
        'depois de can o verbo vem sempre na forma base, sem to',
        'os três usos: sei fazer, posso fazer, você pode me fazer o favor',
        'negativa can\'t / cannot',
        'a diferença de som entre can e can\'t no inglês falado',
      ],
    },
  },
  {
    slug: 'a1-prepositions-place',
    level: 'A1',
    title: 'Preposições de lugar',
    theme: 'Cidade, transporte e direções',
    grammar: {
      slug: 'prepositions-place',
      title: 'in, on, at e companhia',
      points: [
        'in (dentro de um espaço), on (sobre uma superfície), at (num ponto)',
        'in the car mas on the bus — a lógica por trás da exceção',
        'preposições de posição: under, behind, between, next to, in front of, opposite',
        'direções: go straight, turn left, across from',
        'preposição errada é o erro que mais denuncia sotaque escrito',
      ],
    },
  },
  {
    slug: 'a1-prepositions-time',
    level: 'A1',
    title: 'Horas e preposições de tempo',
    theme: 'Tempo, calendário e clima',
    grammar: {
      slug: 'prepositions-time',
      title: 'in, on, at no tempo',
      points: [
        'at para horas, on para dias e datas, in para meses, anos e períodos longos',
        'como dizer as horas: half past, quarter to, o formato falado x o digital',
        'expressões sem preposição: today, tomorrow, next week, every day',
        'in the morning mas at night — a exceção que todo mundo erra',
        'ordem natural: hora antes de dia, dia antes de lugar',
      ],
    },
  },
  {
    slug: 'a1-imperative',
    level: 'A1',
    title: 'Imperativo e pedidos',
    theme: 'Comida, restaurante e compras',
    grammar: {
      slug: 'imperative-requests',
      title: 'Dar instruções e pedir com educação',
      points: [
        'imperativo é o verbo puro: Open the door. Don\'t touch that.',
        'por que o imperativo seco soa grosseiro em inglês e o português aceita melhor',
        'a escada da educação: Give me... / Can I have... / Could I have... / I\'d like...',
        'Let\'s para sugerir',
        'please e thank you: onde entram na frase',
      ],
    },
  },
  {
    slug: 'a1-present-continuous',
    level: 'A1',
    title: 'Present continuous',
    theme: 'Ações agora, roupas e clima',
    grammar: {
      slug: 'present-continuous',
      title: 'Acontecendo agora: -ing',
      points: [
        'be + verbo-ing: os dois pedaços são obrigatórios',
        'regras de escrita do -ing: corta o -e (make/making), dobra a consoante (run/running)',
        'present simple x present continuous: rotina contra agora',
        'verbos que normalmente NÃO vão para -ing: know, want, like, need, understand',
        'present continuous também serve para plano já marcado: I\'m meeting her tomorrow',
      ],
    },
  },
  {
    slug: 'a1-adjectives-adverbs',
    level: 'A1',
    title: 'Adjetivos e advérbios',
    theme: 'Descrever pessoas, lugares e coisas',
    grammar: {
      slug: 'adjectives-adverbs',
      title: 'Descrever com precisão',
      points: [
        'adjetivo vem ANTES do substantivo em inglês: a red car, não "a car red"',
        'adjetivo nunca vai para o plural: two red cars',
        'advérbio de modo com -ly e os irregulares (good/well, fast/fast)',
        'ordem dos adjetivos quando há mais de um: opinião, tamanho, idade, cor, origem',
        'intensificadores: very, really, quite, too, enough (e onde enough fica na frase)',
      ],
    },
  },

  // ===========================================================================
  //  A2 — passado, futuro e a capacidade de contar uma história
  // ===========================================================================
  {
    slug: 'a2-past-simple-regular',
    level: 'A2',
    title: 'Past simple: regulares',
    theme: 'Ontem, infância e memórias',
    grammar: {
      slug: 'past-simple-regular',
      title: 'O passado com -ed',
      points: [
        'was/were para o verbo to be no passado',
        '-ed para todos os verbos regulares, em todas as pessoas, sem exceção',
        'regras de escrita: -d, -ied, consoante dobrada',
        'as três pronúncias de -ed: /t/, /d/ e /ɪd/ — e quando cada uma aparece',
        'marcadores de tempo: yesterday, last week, ago, in 2010',
      ],
    },
  },
  {
    slug: 'a2-past-simple-irregular',
    level: 'A2',
    title: 'Past simple: irregulares',
    theme: 'Viagens e experiências',
    grammar: {
      slug: 'past-simple-irregular',
      title: 'Os verbos irregulares',
      points: [
        'não há regra: é lista, mas uma lista curta e muito frequente',
        'os grupos que ajudam a memorizar: think/thought, buy/bought; sing/sang, drink/drank',
        'os que não mudam: put, cut, let, cost, hit',
        'os 30 irregulares que cobrem a maior parte do uso real',
        'a terceira coluna (past participle) existe e vai ser usada mais para a frente',
      ],
    },
  },
  {
    slug: 'a2-past-questions',
    level: 'A2',
    title: 'Perguntas e negativas no passado',
    theme: 'Contar o que aconteceu',
    grammar: {
      slug: 'did',
      title: 'Did: o passado de do',
      points: [
        'did serve para todas as pessoas, sem exceção',
        'com did, o verbo principal VOLTA para a forma base: "Did you go?", nunca "Did you went?"',
        'negativa: didn\'t + forma base',
        'to be é a exceção: was/were não usam did (Were you there?)',
        'perguntas abertas no passado: What did you do? Where did she go?',
      ],
    },
  },
  {
    slug: 'a2-going-to',
    level: 'A2',
    title: 'Futuro com going to',
    theme: 'Planos, férias e projetos',
    grammar: {
      slug: 'going-to',
      title: 'Going to: plano e previsão com evidência',
      points: [
        'be going to + verbo base',
        'quando usar: decisão já tomada, plano com data, ou evidência visível agora',
        '"gonna" é a forma falada — entender sempre, escrever quase nunca',
        'going to go soa repetitivo: a forma natural é "I\'m going to the beach"',
        'a diferença de going to para will, que vem na próxima unidade',
      ],
    },
  },
  {
    slug: 'a2-will',
    level: 'A2',
    title: 'Futuro com will',
    theme: 'Previsões, promessas e tecnologia',
    grammar: {
      slug: 'will-future',
      title: 'Will: decisão na hora, promessa e previsão',
      points: [
        'will + verbo base, igual para todas as pessoas',
        'contrações \'ll e won\'t',
        'will para decisão tomada no momento da fala; going to para plano anterior',
        'will para oferta e promessa: I\'ll help you.',
        'depois de if e when o futuro vira presente: "when I arrive", não "when I will arrive"',
      ],
    },
  },
  {
    slug: 'a2-comparatives',
    level: 'A2',
    title: 'Comparativos e superlativos',
    theme: 'Comparar lugares, preços e pessoas',
    grammar: {
      slug: 'comparatives-superlatives',
      title: 'Mais, menos, o mais',
      points: [
        'adjetivo curto: -er / the -est',
        'adjetivo longo: more / the most',
        'irregulares: good/better/best, bad/worse/worst, far/further',
        'than na comparação e the no superlativo',
        'as...as para igualdade, not as...as para inferioridade',
      ],
    },
  },
  {
    slug: 'a2-quantifiers',
    level: 'A2',
    title: 'Quantidade: contável e incontável',
    theme: 'Supermercado, receitas e dinheiro',
    grammar: {
      slug: 'countable-uncountable',
      title: 'Some, any, much, many',
      points: [
        'contável x incontável, e por que water e money não têm plural',
        'much para incontável, many para contável, a lot of para os dois',
        'some na afirmativa, any na negativa e na pergunta',
        'some em pergunta quando é oferta: Would you like some coffee?',
        'medidas que contornam o incontável: a piece of, a glass of, a bit of',
      ],
    },
  },
  {
    slug: 'a2-frequency',
    level: 'A2',
    title: 'Advérbios de frequência',
    theme: 'Hábitos, saúde e exercício',
    grammar: {
      slug: 'adverbs-frequency',
      title: 'Com que frequência',
      points: [
        'a escala: always, usually, often, sometimes, rarely, never',
        'posição na frase: antes do verbo principal, DEPOIS do to be',
        'never já é negativo: nada de "I don\'t never"',
        'expressões longas vão para o fim: every day, twice a week, once a month',
        'How often...? e como responder',
      ],
    },
  },
  {
    slug: 'a2-modals-advice',
    level: 'A2',
    title: 'Should, must e have to',
    theme: 'Conselhos, regras e trabalho',
    grammar: {
      slug: 'modals-obligation',
      title: 'Conselho e obrigação',
      points: [
        'should para conselho, must para obrigação forte, have to para regra externa',
        'mustn\'t (proibido) x don\'t have to (não precisa) — significados opostos',
        'have to é o único que conjuga: he has to',
        'no passado só have to sobrevive: had to',
        'had better para conselho urgente',
      ],
    },
  },
  {
    slug: 'a2-past-continuous',
    level: 'A2',
    title: 'Past continuous e narrativa',
    theme: 'Contar histórias e acidentes',
    grammar: {
      slug: 'past-continuous',
      title: 'Cenário e interrupção',
      points: [
        'was/were + -ing para a ação em andamento no passado',
        'a dupla clássica: past continuous monta o cenário, past simple interrompe',
        'when + past simple, while + past continuous',
        'dois past continuous juntos para ações simultâneas',
        'conectores de narrativa: then, after that, suddenly, finally',
      ],
    },
  },

  // ===========================================================================
  //  B1 — precisão, nuance e estruturas que o português não tem
  // ===========================================================================
  {
    slug: 'b1-present-perfect',
    level: 'B1',
    title: 'Present perfect',
    theme: 'Experiências de vida',
    grammar: {
      slug: 'present-perfect',
      title: 'have + particípio',
      points: [
        'have/has + past participle (a terceira coluna dos irregulares)',
        'a ideia central: passado que ainda importa agora',
        'experiência de vida sem dizer quando: I have been to Japan.',
        'ever e never em perguntas e respostas',
        'been x gone: "he has been to Paris" (foi e voltou) x "he has gone to Paris" (está lá)',
      ],
    },
  },
  {
    slug: 'b1-perfect-vs-past',
    level: 'B1',
    title: 'Present perfect x past simple',
    theme: 'Conversa e atualidades',
    grammar: {
      slug: 'perfect-vs-past',
      title: 'A escolha que define o nível',
      points: [
        'o teste: a frase diz QUANDO aconteceu? Se diz, é past simple.',
        'yesterday, last year, in 2010, ago obrigam past simple',
        'a conversa típica: abre no present perfect, detalha no past simple',
        'por que "I have seen him yesterday" é sempre errado',
        'este é o ponto em que o inglês e o português mais divergem — o português usa o passado simples nos dois casos',
      ],
    },
  },
  {
    slug: 'b1-for-since',
    level: 'B1',
    title: 'For, since, already, yet',
    theme: 'Duração e mudanças',
    grammar: {
      slug: 'for-since-yet',
      title: 'Marcadores do present perfect',
      points: [
        'for + período (for three years), since + ponto de partida (since 2020)',
        'already na afirmativa, yet na negativa e na pergunta, still para o que continua',
        'just para o que acabou de acontecer',
        'How long have you...? e a resposta com for/since',
        'present perfect continuous: have been + -ing, quando a duração é o ponto',
      ],
    },
  },
  {
    slug: 'b1-conditionals-01',
    level: 'B1',
    title: 'Condicional zero e primeira',
    theme: 'Causa, efeito e ciência',
    grammar: {
      slug: 'conditionals-zero-first',
      title: 'If real',
      points: [
        'zero conditional: if + presente, presente — para fato sempre verdadeiro',
        'first conditional: if + presente, will — para possibilidade real no futuro',
        'nunca will depois de if: "If it will rain" está errado',
        'unless = if not',
        'a vírgula só aparece quando o if vem primeiro',
      ],
    },
  },
  {
    slug: 'b1-conditional-2',
    level: 'B1',
    title: 'Segunda condicional',
    theme: 'Hipóteses, sonhos e dinheiro',
    grammar: {
      slug: 'conditional-second',
      title: 'If irreal',
      points: [
        'if + past simple, would + verbo base',
        'o passado aqui não é passado: é distância da realidade',
        'were para todas as pessoas no registro cuidado: If I were you...',
        'If I were you, I would... é a forma padrão de dar conselho',
        'a diferença de sentido entre a primeira e a segunda condicional na mesma situação',
      ],
    },
  },
  {
    slug: 'b1-passive',
    level: 'B1',
    title: 'Voz passiva',
    theme: 'Notícias, processos e história',
    grammar: {
      slug: 'passive-voice',
      title: 'Quando quem faz não importa',
      points: [
        'be + past participle, em qualquer tempo verbal',
        'para que serve: o foco vai para o resultado, não para quem agiu',
        'by só entra quando o agente é informação relevante',
        'a passiva é muito mais comum em inglês formal e jornalístico do que em português',
        'verbos com dois objetos: I was given a book / A book was given to me',
      ],
    },
  },
  {
    slug: 'b1-relative-clauses',
    level: 'B1',
    title: 'Relative clauses',
    theme: 'Definir e descrever',
    grammar: {
      slug: 'relative-clauses',
      title: 'who, which, that, where',
      points: [
        'who para pessoa, which para coisa, that para os dois (informal)',
        'defining x non-defining: a vírgula muda o sentido da frase',
        'that nunca entra em non-defining clause',
        'quando o pronome pode sumir: the film (that) I watched',
        'whose para posse, where para lugar, when para tempo',
      ],
    },
  },
  {
    slug: 'b1-reported-speech',
    level: 'B1',
    title: 'Reported speech',
    theme: 'Relatar conversas',
    grammar: {
      slug: 'reported-speech',
      title: 'Contar o que alguém disse',
      points: [
        'o recuo de tempo: present vira past, past vira past perfect, will vira would',
        'mudanças de pronome e de marcador: here/there, today/that day, tomorrow/the next day',
        'say x tell: tell exige a pessoa (tell me), say não',
        'perguntas relatadas perdem a inversão: "He asked where I lived", não "where did I live"',
        'quando o recuo NÃO acontece: o fato ainda é verdadeiro agora',
      ],
    },
  },
  {
    slug: 'b1-gerund-infinitive',
    level: 'B1',
    title: 'Gerúndio e infinitivo',
    theme: 'Preferências, planos e decisões',
    grammar: {
      slug: 'gerund-infinitive',
      title: '-ing ou to?',
      points: [
        'verbos que pedem -ing: enjoy, avoid, finish, suggest, mind, practice',
        'verbos que pedem to: want, decide, hope, promise, agree, learn',
        'depois de preposição é SEMPRE -ing: good at cooking, interested in learning',
        'os que aceitam os dois com sentidos diferentes: stop, remember, forget, try',
        '-ing como sujeito da frase: Swimming is good for you.',
      ],
    },
  },
  {
    slug: 'b1-phrasal-verbs',
    level: 'B1',
    title: 'Phrasal verbs essenciais',
    theme: 'Inglês informal do dia a dia',
    grammar: {
      slug: 'phrasal-verbs-basics',
      title: 'Verbo + partícula',
      points: [
        'a partícula muda tudo: look, look for, look after, look up, look forward to',
        'separáveis x inseparáveis, e por que "turn it on" é certo e "turn on it" não',
        'com pronome, o separável OBRIGA a separação: pick her up',
        'os 40 phrasal verbs que cobrem a maior parte da conversa real',
        'quando o phrasal verb é a forma natural e o verbo "culto" soa estranho',
      ],
    },
  },

  // ===========================================================================
  //  B2 — nuance, registro e naturalidade
  // ===========================================================================
  {
    slug: 'b2-conditional-3',
    level: 'B2',
    title: 'Terceira condicional e mistas',
    theme: 'Arrependimento e passado alternativo',
    grammar: {
      slug: 'conditional-third',
      title: 'O passado que não aconteceu',
      points: [
        'if + past perfect, would have + past participle',
        'para o passado impossível de mudar: arrependimento, crítica, alívio',
        'mixed conditional: condição no passado, consequência no presente',
        'a forma falada comprime tudo: "If I\'d known, I\'d have called"',
        'should have / could have / might have para julgar o passado sem o if',
      ],
    },
  },
  {
    slug: 'b2-modals-deduction',
    level: 'B2',
    title: 'Modais de dedução',
    theme: 'Especulação e investigação',
    grammar: {
      slug: 'modals-deduction',
      title: 'Grau de certeza',
      points: [
        'must be (quase certeza que sim), can\'t be (quase certeza que não)',
        'might / may / could be para possibilidade aberta',
        'dedução sobre o passado: must have been, can\'t have been, might have been',
        'a escala completa de certeza, do 100% ao 10%',
        'por que can\'t be, e não mustn\'t be, é a negativa da dedução',
      ],
    },
  },
  {
    slug: 'b2-used-to',
    level: 'B2',
    title: 'Used to, would, be used to',
    theme: 'Costumes e mudanças de vida',
    grammar: {
      slug: 'used-to',
      title: 'Três estruturas quase iguais',
      points: [
        'used to + base: hábito do passado que acabou',
        'would + base: hábito repetido do passado, só para ações, não para estados',
        'be used to + -ing: estar acostumado — estrutura completamente diferente',
        'get used to + -ing: o processo de se acostumar',
        'a confusão entre "I used to swim" e "I am used to swimming" é a armadilha clássica',
      ],
    },
  },
  {
    slug: 'b2-discourse-markers',
    level: 'B2',
    title: 'Conectores e argumentação',
    theme: 'Opinião, debate e redação',
    grammar: {
      slug: 'discourse-markers',
      title: 'Ligar ideias com precisão',
      points: [
        'contraste: however, although, though, despite, in spite of, whereas',
        'although + frase, despite + substantivo ou -ing — a diferença estrutural',
        'adição e ênfase: moreover, furthermore, besides, what\'s more',
        'consequência: therefore, thus, consequently, as a result',
        'registro: however é escrito, but é falado — misturar destoa',
      ],
    },
  },
  {
    slug: 'b2-cleft-emphasis',
    level: 'B2',
    title: 'Ênfase e cleft sentences',
    theme: 'Estilo e persuasão',
    grammar: {
      slug: 'cleft-sentences',
      title: 'Dar destaque a uma parte da frase',
      points: [
        'It was John who called, não Mary — o foco no sujeito',
        'What I need is more time — o foco no complemento',
        'The reason why... / The place where... para destacar circunstância',
        'do/does/did enfático: I DO like it',
        'inglês não tem a liberdade de ordem do português, então a ênfase vira estrutura',
      ],
    },
  },
  {
    slug: 'b2-inversion',
    level: 'B2',
    title: 'Inversão e registro formal',
    theme: 'Inglês escrito e acadêmico',
    grammar: {
      slug: 'inversion',
      title: 'Quando o verbo vem antes do sujeito',
      points: [
        'advérbio negativo no início força inversão: Never have I seen...',
        'Not only... but also..., Rarely, Seldom, Hardly... when',
        'Only after / Only when no início da frase',
        'condicional sem if: Had I known..., Were I you...',
        'onde isso aparece de verdade: discurso, literatura, texto formal — não em conversa',
      ],
    },
  },
  {
    slug: 'b2-collocations',
    level: 'B2',
    title: 'Collocations',
    theme: 'Soar natural',
    grammar: {
      slug: 'collocations',
      title: 'As palavras que andam juntas',
      points: [
        'make x do: make a decision, do the dishes — não há lógica, há uso',
        'verbos que grudam: take a photo, have breakfast, give a speech, pay attention',
        'adjetivo + substantivo: heavy rain, strong coffee, deep sleep',
        'traduzir palavra por palavra do português produz frases gramaticais mas estranhas',
        'collocation é o que separa o inglês correto do inglês natural',
      ],
    },
  },
  {
    slug: 'b2-idioms-advanced',
    level: 'B2',
    title: 'Phrasal verbs avançados e expressões',
    theme: 'Fluência e conversa real',
    grammar: {
      slug: 'idioms-advanced',
      title: 'Expressões idiomáticas',
      points: [
        'phrasal verbs de três partes: put up with, look forward to, get away with',
        'expressões de alta frequência: it\'s up to you, I\'m into it, no big deal',
        'como reconhecer que uma expressão é idiomática e não deve ser traduzida ao pé da letra',
        'registro: quais expressões cabem no trabalho e quais não',
        'estratégia para continuar aprendendo sozinho depois do curso',
      ],
    },
  },

  // ===========================================================================
  //  C1 — precisão, registro e o inglês que se usa trabalhando
  // ===========================================================================
  {
    slug: 'c1-narrative-tenses',
    level: 'C1',
    title: 'Tempos da narrativa combinados',
    theme: 'Contar história complexa',
    grammar: {
      slug: 'narrative-tenses',
      title: 'Past perfect, past perfect continuous e past simple juntos',
      points: [
        'a linha do tempo dentro do passado: o que aconteceu antes do que já é passado',
        'past perfect para o anterior, past continuous para o cenário, past simple para a sequência',
        'past perfect continuous quando a DURAÇÃO antes do evento é o ponto',
        'quando o past perfect é dispensável: se before ou after já deixam a ordem clara',
        'o erro comum é usar past perfect em tudo que é antigo — ele marca ordem, não distância',
      ],
    },
  },
  {
    slug: 'c1-future-advanced',
    level: 'C1',
    title: 'Futuro avançado',
    theme: 'Projeções, prazos e planejamento',
    grammar: {
      slug: 'future-advanced',
      title: 'Future perfect, continuous e formas de iminência',
      points: [
        'future perfect: o que já estará pronto num ponto futuro (by Friday I will have finished)',
        'future continuous: o que estará em andamento (this time tomorrow I will be flying)',
        'be about to e be on the point of para iminência',
        'be due to e be set to para o que está agendado — muito comum em notícia e trabalho',
        'present simple para horário fixo: the train leaves at six',
      ],
    },
  },
  {
    slug: 'c1-passive-advanced',
    level: 'C1',
    title: 'Passiva avançada',
    theme: 'Notícia, serviço e relato impessoal',
    grammar: {
      slug: 'passive-advanced',
      title: 'Have something done e passiva de relato',
      points: [
        'have/get something done: o serviço que outra pessoa faz por você (I had my hair cut)',
        'a diferença entre "I cut my hair" e "I had my hair cut" — uma delas é uma história bem diferente',
        'passiva de relato: it is said that... / he is said to be...',
        'para que serve: dizer o que se diz sem assumir a autoria da afirmação',
        'onde isso aparece de verdade: jornal, relatório, texto acadêmico',
      ],
    },
  },
  {
    slug: 'c1-reduced-clauses',
    level: 'C1',
    title: 'Orações reduzidas',
    theme: 'Escrever mais enxuto',
    grammar: {
      slug: 'reduced-clauses',
      title: 'Cortar o que sobra na relativa',
      points: [
        'the man who is sitting there → the man sitting there',
        'the book which was written by her → the book written by her',
        'particípio presente para voz ativa, particípio passado para passiva',
        'reduzir oração de tempo e causa: having finished the report, she left',
        'é o que mais aproxima a escrita de quem aprendeu da escrita de quem é nativo',
      ],
    },
  },
  {
    slug: 'c1-wish-regret',
    level: 'C1',
    title: 'Wish, if only e desejo',
    theme: 'Arrependimento, desejo e frustração',
    grammar: {
      slug: 'wish-regret',
      title: 'Falar do que não é',
      points: [
        'wish + past simple para o presente que você queria diferente (I wish I had more time)',
        'wish + past perfect para o passado que você queria diferente (I wish I had said no)',
        'wish + would para reclamar de hábito alheio (I wish you would listen)',
        'if only é o mesmo com mais peso emocional',
        'it\'s time + past simple e I\'d rather + past simple, que seguem a mesma lógica de distância',
      ],
    },
  },
  {
    slug: 'c1-hedging',
    level: 'C1',
    title: 'Atenuação e cautela',
    theme: 'Inglês acadêmico e profissional',
    grammar: {
      slug: 'hedging',
      title: 'Dizer sem cravar',
      points: [
        'por que o inglês profissional evita afirmação absoluta',
        'verbos de atenuação: seem to, tend to, appear to, suggest that',
        'advérbios: arguably, presumably, broadly speaking, to some extent',
        'modais de probabilidade: may well, might arguably, would tend to',
        'o efeito contrário: afirmação categórica soa arrogante ou ingênua em texto técnico',
      ],
    },
  },
  {
    slug: 'c1-nominalisation',
    level: 'C1',
    title: 'Nominalização e registro formal',
    theme: 'Relatório e documento',
    grammar: {
      slug: 'nominalisation',
      title: 'Transformar ação em coisa',
      points: [
        'we implemented it → the implementation of it',
        'por que texto formal em inglês prefere substantivo a verbo',
        'sufixos que criam o substantivo: -tion, -ment, -ance, -ity, -al',
        'o preço: nominalizar demais deixa o texto pesado e esconde quem fez o quê',
        'quando voltar para o verbo — a escrita moderna de negócios está indo nessa direção',
      ],
    },
  },
  {
    slug: 'c1-concession',
    level: 'C1',
    title: 'Concessão e contraste avançados',
    theme: 'Argumentar',
    grammar: {
      slug: 'concession',
      title: 'Admitir um ponto sem perder o seu',
      points: [
        'albeit, notwithstanding, whereas, granted that, admittedly',
        'while e whilst como contraste, não como tempo',
        'much as I understand..., for all its problems...',
        'a estrutura do argumento: concede, contrasta, conclui',
        'registro: estas formas são de texto escrito; em conversa soam empoladas',
      ],
    },
  },
  {
    slug: 'c1-phrasal-three-part',
    level: 'C1',
    title: 'Phrasal verbs de três partes',
    theme: 'Conversa natural',
    grammar: {
      slug: 'phrasal-three-part',
      title: 'Verbo + partícula + preposição',
      points: [
        'put up with, look forward to, get away with, come up with, run out of',
        'estes NUNCA se separam: nada de "put up it with"',
        'look forward to pede -ing, porque to aqui é preposição e não infinitivo',
        'como distinguir os que se separam dos que não se separam',
        'os 30 de três partes que cobrem a maior parte do uso real',
      ],
    },
  },
  {
    slug: 'c1-business-collocations',
    level: 'C1',
    title: 'Collocations de trabalho',
    theme: 'Reunião, projeto e prazo',
    grammar: {
      slug: 'business-collocations',
      title: 'As combinações do inglês corporativo',
      points: [
        'reach a decision, meet a deadline, raise an issue, take ownership',
        'por que "make a decision" e "reach a decision" não são intercambiáveis',
        'verbos que grudam com projeto: kick off, scope out, roll out, scale back',
        'o vocabulário de risco e prazo: blocker, bandwidth, trade-off, lead time',
        'traduzir do português produz frase gramatical que ninguém diz',
      ],
    },
  },
  {
    slug: 'c1-meetings',
    level: 'C1',
    title: 'Reunião e negociação',
    theme: 'Falar em reunião',
    grammar: {
      slug: 'meetings',
      title: 'A linguagem da reunião',
      points: [
        'tomar a palavra sem interromper mal: can I just jump in, building on that',
        'discordar sem criar atrito: I see it differently, I\'m not sure I follow',
        'checar entendimento: just to make sure I got that right',
        'propor e negociar: what if we..., would you be open to...',
        'fechar: so the next step is..., I\'ll follow up on...',
      ],
    },
  },
  {
    slug: 'c1-politeness',
    level: 'C1',
    title: 'Polidez indireta',
    theme: 'Pedir, recusar e discordar',
    grammar: {
      slug: 'politeness',
      title: 'Quanto mais longe, mais educado',
      points: [
        'a escada: can you → could you → would you mind → I was wondering if you could',
        'por que o passado deixa o pedido mais educado em inglês',
        'I don\'t suppose you could...? — o grau máximo de indireção',
        'recusar sem ofender: I\'d love to, but... / that might be tricky',
        'o erro do brasileiro é soar seco sem querer: direto demais lê como rude',
      ],
    },
  },
  {
    slug: 'c1-idioms-metaphor',
    level: 'C1',
    title: 'Expressões e metáforas',
    theme: 'Entender o que não está escrito',
    grammar: {
      slug: 'idioms-metaphor',
      title: 'Quando a frase não quer dizer o que diz',
      points: [
        'metáforas de negócio: move the needle, on the same page, ballpark figure',
        'metáforas de esporte que viraram inglês comum: touch base, drop the ball, home run',
        'como deduzir o sentido pelo contexto em vez de decorar lista',
        'o risco de usar expressão idiomática cedo demais: soa decorado',
        'reconhecer sempre, usar com parcimônia',
      ],
    },
  },
  {
    slug: 'c1-understatement',
    level: 'C1',
    title: 'Ironia e understatement',
    theme: 'Humor e subtexto',
    grammar: {
      slug: 'understatement',
      title: 'Dizer menos para dizer mais',
      points: [
        '"not bad" querendo dizer "muito bom"',
        '"I have a slight problem" quando a casa está pegando fogo',
        'litotes: not unreasonable, not without merit',
        'diferença britânica x americana no uso de ironia',
        'por que interpretar isso ao pé da letra causa mal-entendido de verdade no trabalho',
      ],
    },
  },
  {
    slug: 'c1-writing',
    level: 'C1',
    title: 'Escrita profissional',
    theme: 'E-mail, relatório e resumo',
    grammar: {
      slug: 'writing',
      title: 'Escrever e ser lido',
      points: [
        'a estrutura do e-mail: pedido no primeiro parágrafo, contexto depois',
        'linhas de abertura e fecho por grau de formalidade',
        'como escrever um resumo executivo: conclusão primeiro, evidência depois',
        'voz ativa e frase curta vencem — apesar de tudo que a unidade de nominalização mostrou',
        'revisar: cortar advérbio, cortar hedge desnecessário, cortar repetição',
      ],
    },
  },
];

if (UNITS.length !== 55) {
  throw new Error(`O currículo deve ter 55 unidades, tem ${UNITS.length}`);
}
