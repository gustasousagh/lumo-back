/**
 * As lições de gramática do nível A1.
 *
 * Este é o conteúdo escrito à mão do curso — a única parte que não se deriva
 * de lista aberta, porque explicação boa depende de saber onde o aluno
 * brasileiro tropeça, e isso não está em dataset nenhum.
 *
 * Cada lição tem o texto (markdown) e os exercícios que praticam exatamente
 * aquele ponto. O `slug` casa com o `grammar.slug` da unidade em curriculum.js.
 *
 * Princípios que valem para todas:
 *   - Explicar o PORQUÊ, não só a regra. "É assim" não fixa.
 *   - Comparar com o português onde as línguas divergem — é ali que se erra.
 *   - Tabela para o que é sistema (conjugação), prosa para o que é nuance.
 *   - Toda lição nomeia a armadilha específica do falante de português.
 */

export const A1 = {
  // =========================================================================
  'present-to-be': {
    title: 'To be no presente',
    markdown: `O verbo **to be** é "ser" e "estar" ao mesmo tempo. O inglês não separa os dois — quem separa é o contexto.

Ele tem três formas no presente, e a escolha depende só do sujeito:

| Sujeito | Forma | Contração |
|---|---|---|
| I | am | I'm |
| he / she / it | is | he's, she's, it's |
| you / we / they | are | you're, we're, they're |

**Na fala, a contração é o normal.** "I am tired" soa formal ou enfático; "I'm tired" é como as pessoas falam de verdade.

### Negativa

Basta pôr **not** depois do verbo. Existem duas contrações possíveis, e as duas estão certas:

- She **is not** ready. → She **isn't** ready. / She**'s not** ready.

### Pergunta

Inverta o verbo e o sujeito. **Não** existe um "do" aqui:

- You are Brazilian. → **Are you** Brazilian?
- He is late. → **Is he** late?

### A armadilha do brasileiro

Em português a idade se *tem*; em inglês ela se *é*:

- ❌ I have 20 years.
- ✅ I **am** 20 years old.

O mesmo vale para fome, sede, frio, calor e medo: **I'm hungry**, **I'm cold**, **I'm scared** — nunca "I have hunger".`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'I ___ a student.', answer: 'am', options: ['am', 'is', 'are'], hint: 'Com I, sempre am.' },
      { type: 'FILL_BLANK', prompt: 'She ___ from Brazil.', answer: 'is', options: ['am', 'is', 'are'] },
      { type: 'FILL_BLANK', prompt: 'They ___ my friends.', answer: 'are', options: ['am', 'is', 'are'] },
      { type: 'CHOICE', prompt: 'Como se diz "Eu tenho 20 anos"?', answer: 'I am 20 years old.', options: ['I am 20 years old.', 'I have 20 years.', 'I have 20 years old.', 'I am 20 years.'], hint: 'Em inglês a idade se é, não se tem.' },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Você está cansado?', answer: 'Are you tired' },
      { type: 'CHOICE', prompt: 'Qual é a negativa de "He is late."?', answer: "He isn't late.", options: ["He isn't late.", "He doesn't late.", 'He not is late.', "He don't is late."] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Eu não estou com fome.', answer: 'I am not hungry' },
      { type: 'FILL_BLANK', prompt: '___ you ready?', answer: 'Are', options: ['Am', 'Is', 'Are'] },
      { type: 'CHOICE', prompt: 'O que significa "She\'s not married."?', answer: 'Ela não é casada.', options: ['Ela não é casada.', 'Ela não está em casa.', 'Ela não quer casar.', 'Ela não é madura.'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Nós somos do Brasil.', answer: 'We are from Brazil' },
    ],
  },

  // =========================================================================
  'articles-plural': {
    title: 'A, an, the e o plural',
    markdown: `### a ou an?

A escolha é pelo **som** da próxima palavra, não pela letra:

- **a** antes de som de consoante: a car, a book, **a university** (soa "iuniversity")
- **an** antes de som de vogal: an apple, an egg, **an hour** (o H é mudo, soa "auer")

Repare nos dois últimos: *university* começa com vogal mas leva **a**; *hour* começa com consoante mas leva **an**. Quem decide é o ouvido.

### the

Use **the** quando a pessoa já sabe de qual você está falando — porque foi citado antes, porque só existe um, ou porque está na frente dos dois:

- I bought a car. **The** car is red. *(primeiro apresenta, depois retoma)*
- **The** sun is hot. *(só existe um)*

### Quando NÃO usar artigo

Aqui o português atrapalha, porque ele usa artigo onde o inglês não usa:

| Português | Inglês |
|---|---|
| **Os** cachorros são leais. | ❌ The dogs are loyal. → ✅ **Dogs** are loyal. |
| Eu falo **o** inglês. | ❌ I speak the English. → ✅ I speak **English**. |
| **O** almoço está pronto. | ❌ The lunch is ready. → ✅ **Lunch** is ready. |

A regra: plural genérico, nome de idioma e nome de refeição vão **sem artigo**.

### Plural

Regular com **-s**. Depois de *s, x, ch, sh* entra **-es** (buses, boxes, watches). Consoante + y vira **-ies** (city → cit**ies**).

Os irregulares que mais aparecem valem decorar:

| Singular | Plural |
|---|---|
| child | children |
| person | people |
| man / woman | men / women |
| foot / tooth | feet / teeth |`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'She is ___ engineer.', answer: 'an', options: ['a', 'an', 'the'], hint: 'O som de "engineer" começa com vogal.' },
      { type: 'FILL_BLANK', prompt: 'He studies at ___ university.', answer: 'a', options: ['a', 'an', 'the'], hint: 'Soa "iuniversity" — som de consoante.' },
      { type: 'FILL_BLANK', prompt: 'I waited for ___ hour.', answer: 'an', options: ['a', 'an', 'the'], hint: 'O H de "hour" é mudo.' },
      { type: 'CHOICE', prompt: 'Como se diz "Eu falo inglês"?', answer: 'I speak English.', options: ['I speak English.', 'I speak the English.', 'I speak a English.', 'I speak an English.'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'Dogs are loyal animals.', options: ['Dogs are loyal animals.', 'The dogs are loyal animals.', 'A dogs are loyal animals.', 'Dog are loyal animals.'], hint: 'Plural genérico não leva artigo.' },
      { type: 'CHOICE', prompt: 'Qual é o plural de "child"?', answer: 'children', options: ['children', 'childs', 'childes', 'childrens'] },
      { type: 'CHOICE', prompt: 'Qual é o plural de "person"?', answer: 'people', options: ['people', 'persons', 'peoples', 'persones'] },
      { type: 'FILL_BLANK', prompt: 'There are three ___ in the kitchen.', answer: 'women', options: ['women', 'womans', 'womens', 'woman'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'O sol está quente hoje.', answer: 'The sun is hot today' },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'Lunch is ready.', options: ['Lunch is ready.', 'The lunch is ready.', 'A lunch is ready.', 'Lunch are ready.'] },
    ],
  },

  // =========================================================================
  'pronouns-possessives': {
    title: 'Pronomes e posse',
    markdown: `### Sujeito e objeto

O inglês muda a forma do pronome conforme ele faz ou recebe a ação:

| Faz (sujeito) | Recebe (objeto) |
|---|---|
| I | me |
| you | you |
| he / she / it | him / her / it |
| we | us |
| they | them |

- **I** called **her**. → **Eu** liguei para **ela**.
- **She** called **me**. → **Ela** ligou para **mim**.

### Posse

São duas famílias, e a diferença é se vem um substantivo depois:

| Antes do substantivo | Sozinho |
|---|---|
| my car | mine |
| your car | yours |
| his / her car | his / hers |
| our car | ours |
| their car | theirs |

- This is **my** book. → Este é **meu** livro.
- This book is **mine**. → Este livro é **meu**.

### O genitivo com 's

Para dizer de quem é algo, o inglês inverte a ordem do português:

- o carro **da minha irmã** → **my sister's** car
- o nome **do cachorro** → **the dog's** name

Plural terminado em s leva só o apóstrofo: the student**s'** books.

### its ou it's?

Esta é a confusão mais comum do inglês escrito — inclusive entre nativos:

- **its** = dele/dela (posse): The dog wagged **its** tail.
- **it's** = it is (verbo): **It's** cold today.

Teste rápido: se dá para trocar por "it is", é **it's**. Se não dá, é **its**.`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'She called ___ yesterday.', answer: 'me', options: ['I', 'me', 'my', 'mine'] },
      { type: 'FILL_BLANK', prompt: 'This is ___ book, not yours.', answer: 'my', options: ['my', 'mine', 'me', 'I'], hint: 'Vem um substantivo depois.' },
      { type: 'FILL_BLANK', prompt: 'This book is ___.', answer: 'mine', options: ['my', 'mine', 'me', 'I'], hint: 'Está sozinho, sem substantivo depois.' },
      { type: 'CHOICE', prompt: 'Como se diz "o carro da minha irmã"?', answer: "my sister's car", options: ["my sister's car", 'the car of my sister', "my sister car's", 'car my sister'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: "It's cold today.", options: ["It's cold today.", 'Its cold today.', "Its' cold today.", 'It is cold today are.'], hint: 'Dá para trocar por "it is"?' },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'The dog wagged its tail.', options: ['The dog wagged its tail.', "The dog wagged it's tail.", 'The dog wagged his tail of it.', 'The dog wagged tail.'] },
      { type: 'FILL_BLANK', prompt: 'We saw ___ at the party.', answer: 'them', options: ['they', 'them', 'their', 'theirs'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Este é o nome do cachorro dela.', answer: "This is her dog's name" },
      { type: 'CHOICE', prompt: 'O que significa "Theirs is bigger."?', answer: 'O deles é maior.', options: ['O deles é maior.', 'Eles são maiores.', 'Lá é maior.', 'Aquilo é maior que eles.'] },
      { type: 'FILL_BLANK', prompt: '___ house is very old.', answer: 'Their', options: ['They', 'Them', 'Their', 'Theirs'] },
    ],
  },

  // =========================================================================
  'present-simple': {
    title: 'Present simple: afirmativa',
    markdown: `O present simple é o tempo da **rotina**, do **fato permanente** e da **verdade geral**:

- I work in São Paulo. *(rotina)*
- Water boils at 100 degrees. *(verdade geral)*

Ele **não** é o tempo do que está acontecendo agora — isso é outro tempo, que vem mais pra frente no curso.

### O -s da terceira pessoa

Esta é a única mudança, e é a que mais se erra:

| Sujeito | Verbo |
|---|---|
| I / you / we / they | work |
| **he / she / it** | work**s** |

- I **work** here. / She **works** here.

**Por que só a terceira pessoa?** Porque é o resto que sumiu. O inglês antigo conjugava tudo, como o português; ao longo dos séculos as terminações caíram, e sobrou essa. Não tem lógica a extrair — tem um resquício histórico a respeitar.

### Como escrever o -s

| Terminação | Vira | Exemplo |
|---|---|---|
| s, x, ch, sh, o | **-es** | watch → watch**es**, go → go**es** |
| consoante + y | **-ies** | study → stud**ies** |
| vogal + y | **-s** | play → play**s** |

E **have** é irregular: he/she/it **has**.

### O erro mais comum do curso inteiro

Esquecer o -s é o erro número um de quem está começando. E ele não passa despercebido: para o ouvido nativo, "she work here" soa como "ela trabalha aqui" dito errado — do mesmo jeito que "nós vai" soa em português.

Vale checar toda frase: **o sujeito é ele, ela ou isso? Então o verbo termina em -s.**`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'She ___ in a hospital.', answer: 'works', options: ['work', 'works', 'working', 'worked'] },
      { type: 'FILL_BLANK', prompt: 'They ___ English every day.', answer: 'study', options: ['study', 'studies', 'studys', 'studying'] },
      { type: 'FILL_BLANK', prompt: 'He ___ TV after dinner.', answer: 'watches', options: ['watch', 'watchs', 'watches', 'watching'], hint: 'Termina em ch.' },
      { type: 'FILL_BLANK', prompt: 'My sister ___ two cats.', answer: 'has', options: ['have', 'has', 'haves', 'having'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'My brother goes to school by bus.', options: ['My brother goes to school by bus.', 'My brother go to school by bus.', 'My brother gos to school by bus.', 'My brother going to school by bus.'] },
      { type: 'FILL_BLANK', prompt: 'The baby ___ a lot at night.', answer: 'cries', options: ['cry', 'crys', 'cries', 'cryes'], hint: 'Consoante + y.' },
      { type: 'FILL_BLANK', prompt: 'We ___ football on Sundays.', answer: 'play', options: ['play', 'plays', 'playes', 'playing'] },
      { type: 'CHOICE', prompt: 'Como se diz "Ela mora perto daqui"?', answer: 'She lives near here.', options: ['She lives near here.', 'She live near here.', 'She living near here.', 'She is live near here.'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Ele trabalha numa escola.', answer: 'He works in a school' },
      { type: 'TYPE', prompt: 'Complete com o verbo "to teach": "My mother ___ math."', answer: 'teaches' },
    ],
  },

  // =========================================================================
  'do-does': {
    title: 'Do e does',
    markdown: `Para negar ou perguntar no present simple, o inglês precisa de um auxiliar. Ele **não traduz nada** — só monta a estrutura, como um andaime.

| Sujeito | Auxiliar |
|---|---|
| I / you / we / they | **do** |
| he / she / it | **does** |

### Negativa

- I **don't** like coffee.
- She **doesn't** work here.

### Pergunta

- **Do** you speak English?
- **Does** he live in Rio?

### A regra que todo mundo esquece

Quando **does** aparece, o verbo principal **perde o -s**. O -s já está no auxiliar; repetir seria conjugar duas vezes:

- ❌ Does she work**s** here?
- ✅ **Does** she **work** here?
- ❌ He doesn't like**s** it.
- ✅ He **doesn't like** it.

Pense assim: o -s é um só, e quem fica com ele é o auxiliar.

### Perguntas abertas

A palavra interrogativa vem na frente, e o resto continua igual:

- **What** do you do?
- **Where** does she live?
- **Why** don't you come with us?

E cuidado: **to be não usa do**. "Are you tired?", nunca "Do you are tired?".`,
    exercises: [
      { type: 'FILL_BLANK', prompt: '___ you speak English?', answer: 'Do', options: ['Do', 'Does', 'Are', 'Is'] },
      { type: 'FILL_BLANK', prompt: '___ she live in Rio?', answer: 'Does', options: ['Do', 'Does', 'Are', 'Is'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'Does he work here?', options: ['Does he work here?', 'Does he works here?', 'Do he works here?', 'He does work here?'], hint: 'Com does, o verbo perde o -s.' },
      { type: 'FILL_BLANK', prompt: 'She ___ like coffee.', answer: "doesn't", options: ["don't", "doesn't", "isn't", "aren't"] },
      { type: 'CHOICE', prompt: 'Como se pergunta "Você está cansado?"', answer: 'Are you tired?', options: ['Are you tired?', 'Do you tired?', 'Does you tired?', 'Do you are tired?'], hint: 'To be não usa do.' },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Onde ela mora?', answer: 'Where does she live' },
      { type: 'FILL_BLANK', prompt: 'We ___ have time today.', answer: "don't", options: ["don't", "doesn't", "isn't", "not"] },
      { type: 'CHOICE', prompt: 'O que significa "What do you do?"', answer: 'Qual é a sua profissão?', options: ['Qual é a sua profissão?', 'O que você está fazendo?', 'O que você fez?', 'O que você quer fazer?'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Ele não fala português.', answer: 'He does not speak Portuguese' },
      { type: 'TYPE', prompt: 'Complete: "___ your brother like music?" (do ou does)', answer: 'does' },
    ],
  },

  // =========================================================================
  'there-is-there-are': {
    title: 'Existência: there is / there are',
    markdown: `Para dizer que algo **existe** em algum lugar, o inglês usa uma estrutura fixa:

- **There is** a problem. → **Tem** um problema. / **Há** um problema.
- **There are** two people waiting. → **Tem** duas pessoas esperando.

A escolha depende do que vem depois: **is** para singular, **are** para plural.

### A confusão que o português causa

Em português, "tem" e "é" fazem serviços diferentes que o inglês separa em duas estruturas:

| Português | Inglês | O que faz |
|---|---|---|
| **Tem** um carro na rua. | **There is** a car in the street. | diz que existe |
| **É** um carro bonito. | **It is** a nice car. | descreve algo já conhecido |

Trocar um pelo outro é o erro clássico: "It has a problem" não quer dizer "tem um problema" — quer dizer "ele possui um problema", o que é outra coisa.

### Negativa e pergunta

- There **isn't** any milk. / There**'s no** milk.
- There **aren't** any chairs.
- **Is there** a bathroom here?
- **Are there** any questions?

### Contar

Para perguntar quantidade, o **How many** vem na frente:

- **How many** people **are there** in your family?`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'There ___ a problem with the car.', answer: 'is', options: ['is', 'are', 'has', 'have'] },
      { type: 'FILL_BLANK', prompt: 'There ___ three people in the room.', answer: 'are', options: ['is', 'are', 'has', 'have'] },
      { type: 'CHOICE', prompt: 'Como se diz "Tem um carro na rua"?', answer: 'There is a car in the street.', options: ['There is a car in the street.', 'It has a car in the street.', 'Have a car in the street.', 'It is a car in the street.'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'Are there any questions?', options: ['Are there any questions?', 'There are any questions?', 'Have there questions?', 'Is there any questions?'] },
      { type: 'FILL_BLANK', prompt: 'There ___ any milk in the fridge.', answer: "isn't", options: ["isn't", "aren't", "doesn't", "haven't"] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Tem um banheiro aqui?', answer: 'Is there a bathroom here' },
      { type: 'CHOICE', prompt: 'O que significa "It is a nice car."?', answer: 'É um carro bonito.', options: ['É um carro bonito.', 'Tem um carro bonito.', 'Há um carro bonito.', 'Existe um carro bonito.'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Não tem cadeiras aqui.', answer: 'There are no chairs here' },
      { type: 'FILL_BLANK', prompt: 'How many people ___ there in your family?', answer: 'are', options: ['is', 'are', 'has', 'have'] },
      { type: 'TYPE', prompt: 'Complete: "There ___ two books on the table."', answer: 'are' },
    ],
  },

  // =========================================================================
  'can-cannot': {
    title: 'Can: habilidade, permissão e pedido',
    markdown: `**Can** é o verbo mais fácil do inglês, porque ele não muda nunca:

- I can / you can / **he can** / we can / they can

Nada de "he cans". E o verbo que vem depois fica na **forma base**, sem *to*:

- ✅ I can **swim**.
- ❌ I can to swim. / I can swimming.

### Três usos

| Uso | Exemplo |
|---|---|
| Sei fazer | I **can** drive. |
| Posso / é permitido | You **can** park here. |
| Você pode me fazer o favor? | **Can** you help me? |

### Negativa

**can't** (ou *cannot*, que é mais formal e se escreve junto):

- I **can't** come tomorrow.

### O detalhe da pronúncia

Na fala, **can** átono fica bem curto, quase "kn" — e **can't** tem a vogal aberta e clara. É por aí que o ouvido distingue, e não pelo T final, que muitas vezes some:

- "I can go" → soa /aɪ kn ɡoʊ/
- "I can't go" → soa /aɪ kænt ɡoʊ/

Se você não tem certeza do que ouviu, repare no **som da vogal**, não no T.`,
    exercises: [
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'She can swim very well.', options: ['She can swim very well.', 'She cans swim very well.', 'She can to swim very well.', 'She can swimming very well.'] },
      { type: 'FILL_BLANK', prompt: 'I ___ come tomorrow, sorry.', answer: "can't", options: ["can't", "doesn't", "aren't", "don't"] },
      { type: 'CHOICE', prompt: 'Como se pede ajuda educadamente?', answer: 'Can you help me?', options: ['Can you help me?', 'You can help me?', 'Can you to help me?', 'Do you can help me?'] },
      { type: 'FILL_BLANK', prompt: 'He ___ speak three languages.', answer: 'can', options: ['can', 'cans', 'is can', 'does can'] },
      { type: 'CHOICE', prompt: 'O que significa "You can park here."?', answer: 'Você pode estacionar aqui.', options: ['Você pode estacionar aqui.', 'Você sabe estacionar aqui.', 'Você estacionou aqui.', 'Você vai estacionar aqui.'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Eu não sei dirigir.', answer: 'I can not drive' },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Você pode me ajudar?', answer: 'Can you help me' },
      { type: 'CHOICE', prompt: 'Qual está errada?', answer: 'He cans play guitar.', options: ['He cans play guitar.', 'He can play guitar.', "He can't play guitar.", 'Can he play guitar?'] },
      { type: 'TYPE', prompt: 'Como se escreve a negativa de "can", na forma curta?', answer: "can't" },
      { type: 'FILL_BLANK', prompt: '___ you speak English?', answer: 'Can', options: ['Can', 'Do', 'Are', 'Does'] },
    ],
  },

  // =========================================================================
  'prepositions-place': {
    title: 'in, on, at e companhia',
    markdown: `As três preposições básicas de lugar seguem uma lógica de **tamanho e forma**:

| Preposição | Ideia | Exemplos |
|---|---|---|
| **in** | dentro de um espaço fechado | in the box, in the room, in Brazil |
| **on** | sobre uma superfície | on the table, on the wall, on the floor |
| **at** | num ponto, num endereço | at the door, at work, at home |

### A exceção que todo mundo erra

- **in** the car, **in** a taxi
- **on** the bus, **on** the train, **on** a plane

Por quê? Porque em carro você fica *dentro* de um espaço apertado; em ônibus, trem e avião você *embarca* e anda sobre um piso. É a mesma lógica de "on the floor".

### Posição

| Inglês | Português |
|---|---|
| under | debaixo de |
| behind | atrás de |
| in front of | na frente de |
| between | entre (dois) |
| next to | ao lado de |
| opposite | em frente a |

### Direções

- Go **straight** ahead. → Siga em frente.
- **Turn left** at the corner. → Vire à esquerda na esquina.
- It's **across from** the bank. → É em frente ao banco.

Preposição errada é o que mais denuncia quem aprendeu inglês na marra — e é o tipo de coisa que só entra com repetição, não com regra.`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'The keys are ___ the table.', answer: 'on', options: ['in', 'on', 'at', 'to'] },
      { type: 'FILL_BLANK', prompt: 'She lives ___ Brazil.', answer: 'in', options: ['in', 'on', 'at', 'to'] },
      { type: 'FILL_BLANK', prompt: "I'm ___ home right now.", answer: 'at', options: ['in', 'on', 'at', 'to'] },
      { type: 'FILL_BLANK', prompt: 'He is ___ the bus.', answer: 'on', options: ['in', 'on', 'at', 'under'], hint: 'Ônibus você embarca e anda sobre o piso.' },
      { type: 'FILL_BLANK', prompt: 'They are ___ the car.', answer: 'in', options: ['in', 'on', 'at', 'under'] },
      { type: 'CHOICE', prompt: 'Como se diz "ao lado do banco"?', answer: 'next to the bank', options: ['next to the bank', 'side of the bank', 'at side the bank', 'in the bank side'] },
      { type: 'CHOICE', prompt: 'O que significa "Turn left at the corner."?', answer: 'Vire à esquerda na esquina.', options: ['Vire à esquerda na esquina.', 'Siga em frente até a esquina.', 'A esquina fica à esquerda.', 'Volte para a esquina.'] },
      { type: 'FILL_BLANK', prompt: 'The cat is ___ the table.', answer: 'under', options: ['under', 'on', 'in', 'at'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'O banco fica em frente ao hotel.', answer: 'The bank is opposite the hotel' },
      { type: 'TYPE', prompt: 'Complete: "The picture is ___ the wall."', answer: 'on' },
    ],
  },

  // =========================================================================
  'prepositions-time': {
    title: 'in, on, at no tempo',
    markdown: `As mesmas três preposições valem para tempo, e a lógica também é de **tamanho** — só que agora do período:

| Preposição | Período | Exemplos |
|---|---|---|
| **at** | ponto exato | at 3 o'clock, at noon, **at night** |
| **on** | dia inteiro | on Monday, on July 4th, on my birthday |
| **in** | período longo | in May, in 2026, in the morning, in the summer |

### A exceção famosa

- **in** the morning / **in** the afternoon / **in** the evening
- mas **at** night

Não há lógica a extrair — é uso consolidado. Decore essa e você evita o erro mais comum da lista.

### Quando NÃO usar preposição

Com estas expressões, a preposição simplesmente não entra:

- **today**, **tomorrow**, **yesterday**
- **next** week, **last** month
- **every** day

- ❌ I will call you in tomorrow.
- ✅ I will call you **tomorrow**.

### Que horas são

| Relógio | Como se fala |
|---|---|
| 3:00 | three o'clock |
| 3:15 | quarter past three |
| 3:30 | half past three |
| 3:45 | quarter **to** four |

Repare no 3:45: conta-se para a hora **seguinte**. O digital ("three forty-five") também é aceito e é mais comum nos EUA.`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'The meeting is ___ 3 o\'clock.', answer: 'at', options: ['in', 'on', 'at', 'to'] },
      { type: 'FILL_BLANK', prompt: 'I was born ___ May.', answer: 'in', options: ['in', 'on', 'at', 'to'] },
      { type: 'FILL_BLANK', prompt: 'See you ___ Monday.', answer: 'on', options: ['in', 'on', 'at', 'to'] },
      { type: 'FILL_BLANK', prompt: 'I read ___ night.', answer: 'at', options: ['in', 'on', 'at', 'to'], hint: 'A exceção famosa.' },
      { type: 'FILL_BLANK', prompt: 'She studies ___ the morning.', answer: 'in', options: ['in', 'on', 'at', 'to'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'I will call you tomorrow.', options: ['I will call you tomorrow.', 'I will call you in tomorrow.', 'I will call you at tomorrow.', 'I will call you on tomorrow.'] },
      { type: 'CHOICE', prompt: 'Como se fala 3:45?', answer: 'quarter to four', options: ['quarter to four', 'quarter past four', 'quarter to three', 'half past three'] },
      { type: 'CHOICE', prompt: 'Como se fala 3:30?', answer: 'half past three', options: ['half past three', 'half to four', 'thirty past three', 'half three o\'clock'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'A aula começa às oito.', answer: 'The class starts at eight' },
      { type: 'TYPE', prompt: 'Complete: "My birthday is ___ July." (in, on ou at)', answer: 'in' },
    ],
  },

  // =========================================================================
  'imperative-requests': {
    title: 'Dar instruções e pedir com educação',
    markdown: `### Imperativo

É o verbo puro, sem sujeito. Nada mais simples:

- **Open** the door.
- **Don't touch** that.
- **Turn** right.

Serve para instrução, receita, placa, manual — qualquer coisa que diga o que fazer.

### Por que o imperativo soa grosseiro

Aqui o português engana. "Me dá um café" em português é normal; **"Give me a coffee"** em inglês soa como ordem. O inglês espera uma escada de indireção:

| Frase | Grau |
|---|---|
| Give me a coffee. | ordem — evite |
| **Can I have** a coffee? | normal |
| **Could I have** a coffee? | educado |
| **I'd like** a coffee, please. | educado e natural em restaurante |

Nenhuma delas é errada gramaticalmente. A diferença é social — e num país de língua inglesa o custo de errar isso é a pessoa te achar rude sem saber por quê.

### Sugerir

**Let's** + verbo base:

- **Let's go** to the beach.
- **Let's not** talk about it.

### Onde colocar o please

No fim é o mais comum e o mais natural: "Could you help me, **please**?". No começo também funciona e soa um pouco mais insistente: "**Please** sit down."`,
    exercises: [
      { type: 'CHOICE', prompt: 'Qual é a forma mais educada de pedir um café?', answer: 'Could I have a coffee, please?', options: ['Could I have a coffee, please?', 'Give me a coffee.', 'I want a coffee.', 'You give me coffee.'] },
      { type: 'FILL_BLANK', prompt: '___ go to the beach!', answer: "Let's", options: ["Let's", 'Lets us', 'We let', 'Let we'] },
      { type: 'CHOICE', prompt: 'Como se diz "Não toque nisso"?', answer: "Don't touch that.", options: ["Don't touch that.", 'No touch that.', "Doesn't touch that.", 'Not touch that.'] },
      { type: 'CHOICE', prompt: 'Qual soa como ordem, e não como pedido?', answer: 'Give me the keys.', options: ['Give me the keys.', 'Could you give me the keys?', 'Can I have the keys?', "I'd like the keys, please."] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Por favor, sente-se.', answer: 'Please sit down' },
      { type: 'FILL_BLANK', prompt: '___ you help me, please?', answer: 'Could', options: ['Could', 'Do', 'Are', 'Let'] },
      { type: 'CHOICE', prompt: 'O que significa "Let\'s not talk about it."?', answer: 'Vamos não falar disso.', options: ['Vamos não falar disso.', 'Não deixe ele falar disso.', 'Não falamos disso.', 'Deixe-me falar disso.'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'Abra a janela, por favor.', answer: 'Open the window please' },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: "Let's meet at seven.", options: ["Let's meet at seven.", "Let's to meet at seven.", "Let's meeting at seven.", 'Let we meet at seven.'] },
      { type: 'TYPE', prompt: 'Complete o pedido educado: "___ I have the bill, please?"', answer: 'Could' },
    ],
  },

  // =========================================================================
  'present-continuous': {
    title: 'Acontecendo agora: -ing',
    markdown: `São **dois pedaços**, e os dois são obrigatórios: o verbo **to be** mais o verbo principal com **-ing**.

- I **am working**.
- She **is sleeping**.
- They **are waiting**.

Esquecer o *to be* é o erro mais comum: "I working" não existe.

### Como escrever o -ing

| Regra | Exemplo |
|---|---|
| normal | work → work**ing** |
| termina em -e mudo: corta o e | make → mak**ing**, write → writ**ing** |
| uma sílaba, consoante-vogal-consoante: dobra | run → ru**nn**ing, sit → si**tt**ing |

### Present simple ou continuous?

| | Quando | Exemplo |
|---|---|---|
| **simple** | rotina, sempre | I **work** in a bank. |
| **continuous** | agora, temporário | I **am working** from home this week. |

### Os verbos que não vão para -ing

Alguns verbos descrevem **estado**, não ação, e por isso normalmente não aceitam o -ing:

- know, want, like, love, hate, need, understand, believe, prefer

- ❌ I am knowing the answer.
- ✅ I **know** the answer.

### O uso que surpreende

Present continuous também serve para **plano já marcado** no futuro próximo:

- I**'m meeting** her tomorrow. → Vou encontrar com ela amanhã.

Não é erro nem gíria: é o jeito mais natural de falar de compromisso agendado.`,
    exercises: [
      { type: 'FILL_BLANK', prompt: 'She ___ sleeping right now.', answer: 'is', options: ['is', 'are', 'am', 'does'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'I am working from home today.', options: ['I am working from home today.', 'I working from home today.', 'I am work from home today.', 'I working am from home today.'] },
      { type: 'CHOICE', prompt: 'Qual é o -ing de "run"?', answer: 'running', options: ['running', 'runing', 'runnning', 'runnig'] },
      { type: 'CHOICE', prompt: 'Qual é o -ing de "make"?', answer: 'making', options: ['making', 'makeing', 'makking', 'maiking'] },
      { type: 'CHOICE', prompt: 'Qual está errada?', answer: 'I am knowing the answer.', options: ['I am knowing the answer.', 'I know the answer.', 'I am learning English.', 'She is waiting outside.'], hint: 'know descreve estado, não ação.' },
      { type: 'FILL_BLANK', prompt: 'They ___ waiting for us.', answer: 'are', options: ['is', 'are', 'am', 'do'] },
      { type: 'CHOICE', prompt: 'Qual frase fala de rotina?', answer: 'I work in a bank.', options: ['I work in a bank.', 'I am working right now.', 'I am working this week.', 'I am working from home today.'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'O que você está fazendo?', answer: 'What are you doing' },
      { type: 'CHOICE', prompt: 'O que significa "I\'m meeting her tomorrow."?', answer: 'Vou encontrar com ela amanhã.', options: ['Vou encontrar com ela amanhã.', 'Estou encontrando ela amanhã.', 'Encontro com ela todo dia.', 'Encontrei com ela ontem.'] },
      { type: 'TYPE', prompt: 'Escreva o -ing de "sit".', answer: 'sitting' },
    ],
  },

  // =========================================================================
  'adjectives-adverbs': {
    title: 'Descrever com precisão',
    markdown: `### O adjetivo vem antes

Esta é a inversão que mais pega brasileiro:

- ❌ a car red
- ✅ a **red** car

E o adjetivo **nunca vai para o plural**. Ele não concorda com nada:

- ❌ two reds cars
- ✅ two **red** cars

### Quando há mais de um

Existe uma ordem que soa natural ao ouvido nativo, mesmo que ninguém a ensine explicitamente por lá:

**opinião → tamanho → idade → cor → origem → material**

- a **beautiful big old red Italian** car

Na prática, você raramente empilha mais de dois ou três. Mas "a red big car" soa errado mesmo para quem não sabe explicar por quê.

### Advérbio: como a ação é feita

Em geral, adjetivo **+ -ly**:

| Adjetivo | Advérbio |
|---|---|
| quick | quick**ly** |
| careful | careful**ly** |
| easy | eas**ily** |

Os irregulares mais importantes:

| Adjetivo | Advérbio |
|---|---|
| good | **well** |
| fast | **fast** |
| hard | **hard** |

- She is a **good** singer. → She sings **well**.

### Intensificadores

**very**, **really**, **quite**, **too** vêm antes:

- It's **very** cold. / It's **too** expensive.

**enough** é a exceção: vem **depois** do adjetivo.

- It's **big enough**. *(grande o bastante)*
- ❌ It's enough big.`,
    exercises: [
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'She has a red car.', options: ['She has a red car.', 'She has a car red.', 'She has a reds car.', 'She has red a car.'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: 'They bought two red cars.', options: ['They bought two red cars.', 'They bought two reds cars.', 'They bought two cars reds.', 'They bought two car reds.'] },
      { type: 'FILL_BLANK', prompt: 'He speaks English very ___.', answer: 'well', options: ['well', 'good', 'goodly', 'fine'] },
      { type: 'CHOICE', prompt: 'Qual é o advérbio de "easy"?', answer: 'easily', options: ['easily', 'easyly', 'easely', 'easy'] },
      { type: 'CHOICE', prompt: 'Qual está certa?', answer: "It's big enough.", options: ["It's big enough.", "It's enough big.", "It's enough of big.", "It's big of enough."] },
      { type: 'FILL_BLANK', prompt: 'She drives very ___.', answer: 'fast', options: ['fast', 'fastly', 'faster', 'fastily'] },
      { type: 'CHOICE', prompt: 'Qual ordem soa natural?', answer: 'a big old house', options: ['a big old house', 'an old big house', 'a house big old', 'an old house big'] },
      { type: 'WORD_BANK', prompt: 'Monte a frase', sentencePt: 'É caro demais.', answer: 'It is too expensive' },
      { type: 'CHOICE', prompt: 'O que significa "She sings well."?', answer: 'Ela canta bem.', options: ['Ela canta bem.', 'Ela é uma boa cantora.', 'Ela canta alto.', 'Ela cantou bem.'] },
      { type: 'TYPE', prompt: 'Escreva o advérbio de "careful".', answer: 'carefully' },
    ],
  },
};
