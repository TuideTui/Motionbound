# Motionbound

**Um dungeon crawler 2D em pixel art controlado por movimentos físicos.**

**Motionbound** é um projeto acadêmico de jogo sério desenvolvido para a disciplina de Jogos Digitais. Inspirado nas mecânicas de exploração e combate de jogos do gênero *dungeon crawler*, o projeto propõe uma aventura de fantasia em que o jogador explora masmorras, enfrenta monstros e utiliza ataques, magias e cura. O diferencial está no controle: em vez dos comandos tradicionais de teclado, os movimentos do personagem serão realizados por meio de um dispositivo físico com Arduino/ESP32 e sensores.

O projeto busca explorar a **gamificação de atividades motoras de mão, punho e antebraço**, reunindo uma experiência de jogo com indicadores de execução e evolução dos movimentos. Trata-se de um protótipo educacional, não de uma ferramenta clínica validada.

> **Estado atual:** fase de definição e planejamento. As mecânicas, os componentes e os cenários descritos neste documento constituem a **proposta de desenvolvimento**, não funcionalidades já implementadas.

## Informações do projeto

| Item | Definição |
| --- | --- |
| **Nome** | Motionbound |
| **Gênero** | Dungeon crawler / RPG de ação |
| **Estilo visual** | 2D top-down, pixel art |
| **Linguagem e framework** | Java com libGDX |
| **Dispositivo de entrada** | Arduino ou ESP32 com sensores (a definir) |
| **Foco motor** | Mão, punho e antebraço |
| **Estrutura** | Fase 0 (tutorial) e fases 1 a 5 |
| **Metodologia** | Kanban, com acompanhamento semanal pelo Trello |
| **Turma** | 4H12 |

## Objetivo

Criar um jogo funcional no qual a movimentação e as principais ações do personagem sejam controladas por dados coletados de um dispositivo IoT. A proposta é transformar movimentos físicos em desafios de exploração e combate, com feedback sobre aspectos como amplitude, estabilidade e precisão.

O projeto procura trabalhar, de forma lúdica:

- movimentos de inclinação e rotação de mão e punho;
- coordenação motora durante comandos direcionais;
- estabilidade ao manter determinadas posições;
- controle da pressão exercida pela mão, se o sensor escolhido permitir;
- percepção da evolução do desempenho ao longo das partidas.

**Importante:** os exercícios e faixas de movimento deverão ser calibrados conforme limites confortáveis do usuário. A dificuldade do jogo não deve exigir aumento indiscriminado de esforço físico. O dispositivo e a proposta de uso de outros movimentos IoT serão validados com o professor.

## Como será o jogo

O jogador controlará um personagem em uma aventura por masmorras e cenários de fantasia. A progressão será baseada na exploração, no enfrentamento de inimigos e no uso estratégico de habilidades.

**Mecânicas planejadas:**

- movimentação em diferentes direções, caminhada e corrida;
- combate corpo a corpo e ataque carregado;
- lançamento de magia;
- cura e gerenciamento de vida;
- inimigos com comportamentos e ataques próprios;
- cenários, armadilhas, objetivos e progressão de fases;
- interface com vida, habilidades, pausa e reinício;
- resultados ao final das fases com métricas dos movimentos.

### Controles por movimentos físicos

A associação inicial entre movimentos e ações será testada e ajustada após a montagem do protótipo:

| Movimento captado | Ação planejada no jogo | Aspecto motor observado |
| --- | --- | --- |
| Inclinar o controlador | Andar na direção correspondente | Direção e amplitude |
| Aumentar a inclinação dentro de uma faixa confortável | Correr | Controle gradual do movimento |
| Pressionar um sensor de preensão | Ataque básico | Controle da pressão |
| Manter pressão por um intervalo curto | Ataque carregado | Manutenção da pressão |
| Rotacionar o punho | Lançar magia | Rotação e precisão |
| Manter a posição estável por alguns segundos | Ativar cura | Estabilidade |

As ações deverão ser executadas **pelo dispositivo físico**, sem exigir teclado ou controle convencional durante a partida. Controles temporários de teste podem ser utilizados apenas nas primeiras etapas de desenvolvimento.

## Integração com Arduino/IoT

O protótipo deverá captar movimentos do jogador, processá-los e enviá-los ao aplicativo em Java/libGDX.

**Componentes em avaliação:**

- **Arduino ou ESP32:** placa responsável pela leitura dos sensores;
- **MPU6050:** sensor inercial para estimar inclinação, rotação e movimentação;
- **sensor de pressão/preensão:** para reconhecer comandos de ataque;
- **conexão USB/Serial:** canal inicial proposto para enviar os dados ao jogo.

**Fluxo planejado:**

```text
Movimentos da mão / punho
           |
           v
    Sensores físicos
           |
           v
     Arduino / ESP32
           |
           v
  Comunicação USB/Serial
           |
           v
   Leitura de dados em Java
           |
           v
  Controle e lógica do libGDX
           |
           v
   Personagem e habilidades
           |
           v
       Métricas do jogo
```

Antes de definir os valores de ativação, será criada uma etapa de **calibração**, incluindo posição neutra e amplitude confortável. O sensor inercial não mede diretamente força de preensão: essa informação dependerá de um sensor específico.

## Fases planejadas

| Fase | Cenário | Objetivo e novidade |
| --- | --- | --- |
| **0** | Campo de Treinamento | Tutorial, calibração e aprendizado dos comandos físicos. |
| **1** | Cripta Abandonada | Exploração e primeiros confrontos contra inimigos simples. |
| **2** | Minas Perdidas | Armadilhas e inimigos com ataques à distância. |
| **3** | Floresta Corrompida | Inimigos especiais e utilização estratégica das habilidades. |
| **4** | Fortaleza | Combinação de desafios e mecânicas das fases anteriores. |
| **5** | Dungeon Final | Confronto com um chefe e encerramento da aventura. |

A dificuldade aumentará principalmente na complexidade das situações e decisões do jogo, **não necessariamente no esforço físico solicitado**.

## Métricas de desempenho

O jogo pretende registrar indicadores derivados das leituras do controlador e apresentá-los ao final das fases:

- **Amplitude:** extensão dos movimentos realizados em relação à calibração;
- **Precisão:** proximidade dos movimentos em relação às faixas ou posições solicitadas;
- **Estabilidade:** variação do sinal durante desafios de manutenção de posição;
- **Controle de pressão:** comportamento do sensor de preensão durante os comandos;
- **Evolução:** comparação dos indicadores entre fases ou sessões, quando implementada.

As métricas servirão para dar feedback sobre a execução **dentro do jogo**. Elas não substituem avaliação médica, fisioterapêutica ou de terapia ocupacional e não comprovam, por si só, melhora clínica.

## Tecnologias previstas

- **Java** — lógica e sistemas do jogo;
- **libGDX** — renderização, entrada, cenas e mecânicas 2D;
- **Arduino/ESP32** — leitura dos sensores de movimento;
- **Comunicação Serial** — integração inicial hardware/software;
- **Gradle** — gerenciamento e build do projeto Java;
- **Git e GitHub** — versionamento e acompanhamento de contribuições;
- **Trello** — organização das entregas pelo método Kanban.

> A arquitetura das classes, bibliotecas de comunicação Serial e requisitos de execução serão documentados quando o protótipo for criado.

## Organização prevista do projeto

```text
Motionbound/
├── README.md
├── docs/                 # Planejamento e documentação do projeto
├── game/                 # Código Java/libGDX (estrutura a definir)
├── hardware/             # Código e esquemas do controlador
└── assets/               # Sprites, mapas, sons e recursos do jogo
```

*Esta árvore é uma sugestão de organização, não a estrutura atual do repositório. Ela poderá mudar quando o projeto libGDX for inicializado.*

## Situação e etapas de desenvolvimento

- [x] Definir nome e conceito geral do jogo;
- [x] Escolher metodologia Kanban e organizar o Trello;
- [x] Criar o repositório e confirmar o cadastro na disciplina;
- [ ] Validar com o professor o controlador físico e os sensores propostos;
- [ ] Construir e testar o controlador;
- [ ] Inicializar o jogo em libGDX;
- [ ] Integrar Arduino e controles do personagem;
- [ ] Desenvolver combate, habilidades e interface;
- [ ] Implementar tutorial e as cinco fases;
- [ ] Implementar métricas e tela de resultados;
- [ ] Testar o jogo, gerar executável e produzir o short paper.

## 📋 Organização e acompanhamento

O desenvolvimento será acompanhado em um **quadro Kanban** com cinco estados: **Backlog, A fazer, Em andamento, Em revisão e Concluído**. Cada card representa uma entrega, dividida em atividades menores por meio de checklists.

**Commits semanais:** cada integrante deve realizar pelo menos um commit de sua própria autoria **entre segunda-feira e domingo** e enviá-lo ao GitHub. Os commits devem representar alterações reais no projeto. O e-mail configurado no Git precisa estar associado à conta do GitHub correspondente.

- [Quadro do Motionbound no Trello](https://trello.com/b/iN2g5cul/projeto-de-jogos-motionbound)
- [Repositório do projeto](https://github.com/TuideTui/Motionbound)
- [Cadastro confirmado na disciplina — Issue #42](https://github.com/leobrasil/jogos_digitais_2026-1/issues/42)

## Equipe

**Arthur Cezar da Silveira Lima** — [@TuideTui](https://github.com/TuideTui)  
**Turma:** 4H12

*Outros integrantes poderão ser adicionados aqui, caso o grupo seja ampliado.*

## Contexto acadêmico

Projeto desenvolvido para a disciplina de **Jogos Digitais (2026)**, a partir da proposta de desenvolvimento de um jogo sério de reabilitação física com **libGDX e IoT**. Os entregáveis previstos incluem o código-fonte com commits semanais, um executável ou APK e um short paper de até seis páginas.

---

**Motionbound - do movimento real à aventura virtual.**
