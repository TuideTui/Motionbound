# 🎮 Motionbound

**Um dungeon crawler 2D em pixel art controlado por movimentos físicos.**

**Motionbound** é um projeto acadêmico de jogo sério desenvolvido para a disciplina de Jogos Digitais. Inspirado nas mecânicas de exploração e combate de jogos do gênero *dungeon crawler*, o projeto propõe uma aventura de fantasia em que o jogador explora masmorras, enfrenta monstros e utiliza ataques, magias e cura. O diferencial está no controle: em vez dos comandos tradicionais de teclado, os movimentos do personagem serão realizados por meio de um dispositivo físico com Arduino/ESP32 e sensores.

O projeto busca explorar a **gamificação de atividades motoras de mão, punho e antebraço**, reunindo uma experiência de jogo com indicadores de execução e evolução dos movimentos. Trata-se de um protótipo educacional, não de uma ferramenta clínica validada.

> **Estado atual: campanha de cinco fases para Windows.** Mapas próprios, dificuldade crescente, apresentação e contagem regressiva pixeladas, combate com mira livre, magia com recuo, cura, pausa e salvamento entre fases. A abertura oferece modo convencional e simulação do modo reabilitação. **USB/Serial, firmware e calibração com os sensores físicos ainda não foram implementados.** O nome do JAR continua `Motionbound-0.1.1-all.jar`.

**Interface atualizada:** abertura em tela cheia, fonte Kenney Pixel, vida em corações, vigor com raio e segmentos, mana em cristais, molduras de pedra/bronze e menu `Esc` em pergaminho. A cripta tem quatro ambientes decorados, tochas animadas e detalhes do Tiny Dungeon e Tiny Town já incluídos. **Salvar e sair** e **Continuar expedição** mantêm a partida. Consulte [Interface e salvamento](docs/INTERFACE-E-SALVAMENTO.md) e [Arte e créditos](docs/ASSETS.md).

## 🖥️ Como executar no Windows — versão 0.1.1

> **IMPORTANTE:** o ZIP desta versão contém **uma única pasta chamada `Motionbound`**. Abra **essa pasta**, não a pasta acima. O arquivo `build.gradle` precisa estar junto de `iniciar-jogo.bat`.

1. Extraia `Motionbound-v0.1.1-Windows.zip` em `C:\Users\ThurC\OneDrive\Desktop\VSCode\Jogo Projeto Jogos Digitais`. A estrutura correta ficará assim:

   ```text
   Jogo Projeto Jogos Digitais/
   └── Motionbound/
       ├── build.gradle
       ├── settings.gradle
       ├── iniciar-jogo.bat
       ├── verificar-ambiente.bat
       ├── src/
       ├── assets/
       └── scripts/
   ```

2. Abra a pasta **Motionbound** no VS Code (`Arquivo > Abrir Pasta`). Você também pode executar diretamente pelo Explorador de Arquivos.
3. (Opcional) Execute `verificar-ambiente.bat`. O iniciador seleciona automaticamente o JDK onde está o `javac`, mesmo se `java -version` global indicar Java 8.
4. Execute **`iniciar-jogo.bat`** com dois cliques. Na primeira execução, o script baixa **Gradle 9.1.0** e as dependências libGDX pela internet. **Não execute `gradle run` na pasta acima do projeto.**
5. A janela Motionbound abrirá após o build, desde que as bibliotecas tenham sido baixadas e o código compile corretamente.

**Também é possível executar pelo terminal integrado do VS Code (PowerShell):**

```powershell
cd -LiteralPath 'C:\Users\ThurC\OneDrive\Desktop\VSCode\Jogo Projeto Jogos Digitais\Motionbound'
.\iniciar-jogo.bat
```

O script utiliza explicitamente a pasta onde está o arquivo `build.gradle`, independentemente da pasta atual do terminal. Para gerar um JAR com as dependências, use `gerar-executavel.bat` (arquivo esperado: `build/libs/Motionbound-0.1.1-all.jar`).

**Sprites:** os packs oficiais [Tiny Dungeon](https://kenney.nl/assets/tiny-dungeon), [Tiny Town](https://kenney.nl/assets/tiny-town), [Tiny Farm](https://kenney.nl/assets/tiny-farm) e [Tiny Ski](https://kenney.nl/assets/tiny-ski), assim como a fonte Kenney Pixel, já estão incluídos sob CC0, com suas licenças em `assets/`. O herói usa um atlas gerado com IA, normalizado para células de 32×32 pixels: caminhada, preparação, golpe, recuperação, magia e cura em quatro direções. A espada acompanha a mão e gira livremente conforme a mira. Os arquivos e a especificação da geração estão em [art-source](art-source/hero-actions.prompt.txt); essa arte não pertence ao Kenney. Não há downloads de arte durante o jogo.

**Se der erro:** envie a mensagem exibida na janela. O arquivo `docs/COMO-INICIAR-WINDOWS.md` inclui soluções para Gradle iniciado na pasta errada, Java 8 no PATH e download de dependências.

### Combate com mouse e escolha de modo

A abertura oferece **Modo convencional** e **Modo reabilitação**. O convencional usa os controles abaixo. Na reabilitação, **Experimentar simulação** permite testar uma bancada virtual: `I J K L` simulam inclinação, `F` simula aperto e `Shift + F` simula pressão acima da faixa. Solte e aperte novamente para cada golpe. A mira busca inimigos próximos na direção da inclinação; o personagem e os inimigos se movem mais lentamente. Nessa demonstração, a cura vem da coleta de corações e a magia fica para a próxima etapa.

**O dispositivo ainda não está conectado:** a simulação alimenta `SensorControlSource` com amostras normalizadas, sem usar o mouse. O processamento inclui suavização, zona neutra, limiares separados de aperto/soltura e interrupção do movimento ao perder amostras. USB/Serial, firmware para ESP32 + MPU6050 + FSR402 e calibração real serão implementados após a chegada das peças. Os indicadores de pressão são dados artificiais, identificados na interface e no save.

| Tecla | Ação |
| --- | --- |
| `W A S D` ou setas | Andar em 8 direções |
| `Shift` | Correr (consome energia) |
| Mouse | Mirar livremente, inclusive nas diagonais |
| Clique esquerdo | Espada: preparação, golpe e recuperação |
| Clique direito | Disparar magia na direção do cursor (25 de mana) |
| `Espaço` | Curar 2 corações com uma poção (até 3 usos) |
| `Esc` | Abrir pausa; voltar do tutorial/configurações ao resumo; retomar pelo resumo |
| `Enter` | Selecionar a opção destacada nos menus |
| `Setas` / mouse | Navegar e selecionar opções nos menus |
| `F11` | Alternar entre tela cheia e janela |
| `R` | Reiniciar fase durante a partida |

A espada alcança **32,2 unidades** em um arco de 120 graus, um aumento de 15% sobre o alcance anterior de 28. A direção é fixada no início do golpe; o impacto acontece após 0,16 s e atinge cada inimigo uma única vez, respeitando paredes. Movimento e mira são independentes. A arma acompanha a mão de cada pose, com o cabo coberto pelos dedos e profundidade conforme a direção do corpo. Os inimigos reagem ao impacto com partículas e recuo.

A magia causa **1 de dano** (antes, 2) e empurra o alvo na direção do projétil. Durante o recuo, o inimigo interrompe a perseguição e o ataque de contato; o deslocamento respeita as paredes. O custo continua sendo 25 de mana.

Cada inimigo derrotado tem **30% de chance de deixar um coração no chão**. A coleta recupera **1 coração perdido**, até o limite atual de 6; ela não aumenta a vida máxima. Com vida cheia, o item permanece no chão. Corações e ataques em andamento são preservados no salvamento. Saves dos formatos 1 e 2 continuam na cripta da fase 1; saves do formato 3 mantêm a fase e o progresso. Todos são convertidos para o formato 4 no próximo salvamento. Na vila reformulada, posições antigas que coincidam com construções ou árvores são transferidas para o chão livre mais próximo, preservando vida, recursos e inimigos derrotados. Existe um único espaço de save: iniciar outro modo não apaga o anterior, mas **Salvar e sair** o substitui.

### Experimentar e comparar

Use `iniciar-jogo.bat` para a implementação atual. O trabalho está na branch local `experimento/combate-mira`; commits e push são feitos manualmente pelo aluno. A `main` mantém o histórico anterior, mas alterações sem commit acompanham a troca de branch; trocar de branch sozinho não desfaz a experiência.

Neste computador, `comparar-versao-anterior.bat` executa a cópia anterior preservada em `build/rollback/Motionbound-antes-combate.jar`. Também foram preservados o checkpoint anterior em `build/rollback/expedition-antes-combate.json` e o JAR imediatamente anterior à campanha em `build/rollback/Motionbound-antes-fases.jar`. O iniciador de comparação não restaura nem modifica os arquivos do projeto. O jogo antigo não lê os novos formatos de save: para comparar, inicie uma nova partida e feche a janela sem salvar. A pasta `build/` é local, ignorada pelo Git e removida por `gradle clean`; preserve essas cópias antes de limpar o build.

Para verificar combate, salvamento e conectividade dos cinco mapas, execute `gradle check`. `gradle verifyDesktop` abre brevemente o jogo, verifica os controles, a contagem regressiva, as cinco transições, derrota/reinício e continuação de saves. Grava capturas em `build/review/`, usando saves de teste separados do seu checkpoint. `gradle fatJar` gera o executável com todos os recursos.

**Objetivo da campanha:** derrotar todos os guardiões de cada mapa e alcançar o baú da saída, marcado no minimapa. O baú só libera a próxima fase quando todos os inimigos caem. Após a fase 5, aparece a vitória da campanha.

**Sem Arduino por enquanto:** esta é uma versão provisória de desenvolvimento, com seleção de modo e simulação de sensores. A estrutura `ControlSource` recebe comandos do teclado/mouse ou de amostras normalizadas; o transporte USB e a calibração real ainda faltam. A versão apresentada como jogo sério para avaliação do professor deverá funcionar com o dispositivo IoT.

**Assets:** os cenários misturam Tiny Dungeon, Tiny Town, Tiny Farm e Tiny Ski (16×16, CC0). Do Ski são usados neve, pinheiros e os números 1, 2 e 3 da contagem regressiva; equipamentos esportivos e veículos não fazem parte da aventura. Slimes, esqueletos e detalhes complementares da interface são originais do projeto; o herói é arte gerada com IA, documentada em `art-source/`.

## 📌 Informações do projeto

| Item | Definição |
| --- | --- |
| **Nome** | Motionbound |
| **Gênero** | Dungeon crawler / RPG de ação |
| **Estilo visual** | 2D top-down, pixel art |
| **Linguagem e framework** | Java com libGDX |
| **Dispositivo de entrada** | Arduino ou ESP32 com sensores (a definir) |
| **Foco motor** | Mão, punho e antebraço |
| **Estrutura atual** | Tutorial no menu e campanha com fases 1 a 5 |
| **Metodologia** | Kanban, com acompanhamento semanal pelo Trello |
| **Turma** | 4H12 |

## 🎯 Objetivo

Criar um jogo funcional no qual a movimentação e as principais ações do personagem sejam controladas por dados coletados de um dispositivo IoT. A proposta é transformar movimentos físicos em desafios de exploração e combate, com feedback sobre aspectos como amplitude, estabilidade e precisão.

O projeto procura trabalhar, de forma lúdica:

- movimentos de inclinação e rotação de mão e punho;
- coordenação motora durante comandos direcionais;
- estabilidade ao manter determinadas posições;
- controle da pressão exercida pela mão, se o sensor escolhido permitir;
- percepção da evolução do desempenho ao longo das partidas.

**Importante:** os exercícios e faixas de movimento deverão ser calibrados conforme limites confortáveis do usuário. A dificuldade do jogo não deve exigir aumento indiscriminado de esforço físico. O dispositivo e a proposta de uso de outros movimentos IoT serão validados com o professor.

## ⚔️ Como será o jogo

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

### 🕹️ Controles por movimentos físicos

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

## 🔌 Integração com Arduino/IoT

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

## 🗺️ Campanha implementada

| Fase | Cenário | Dificuldade | Guardiões | Ambientação |
| --- | --- | --- | --- | --- |
| **1** | A Cripta Esquecida | Fácil | 7 | Masmorra original com quatro salas, túmulos, tochas e vegetação. |
| **2** | Vila das Lanternas | Moderado | 10 | Casas com janelas e chaminés, praça de pedra, jardins, poço, apiário cercado, moradores decorativos e portaria com torres. |
| **3** | Campos Corrompidos | Intermediário | 14 | Lavouras, celeiro, casa, recipientes e cercas com Tiny Farm e Town. |
| **4** | Passagem Congelada | Difícil | 18 | Caminho em patamares, pinheiros, neve e flocos animados do Tiny Ski. |
| **5** | Fortaleza do Crepúsculo | Muito difícil | 22 | Salões e corredores conectados, lápides, mobiliário e tapete cerimonial. |

Cada entrada mostra **Fase N - Nível ...** e o nome do mapa com a fonte pixelada da interface. A contagem **3, 2, 1** usa os próprios sprites do Tiny Ski, ampliados sem suavização. Durante a apresentação, personagens, combate e cronômetro ficam parados. `Esc` pausa a apresentação e retoma de onde parou.

As tochas de parede aparecem somente nas paredes dos mapas internos. As fases externas não recebem tochas soltas nem os respectivos efeitos de luz sobre o chão.

Ao concluir uma etapa, o jogador pode descansar, **seguir para a próxima fase** ou **salvar e sair**. A próxima etapa repõe vida, vigor, mana e três poções; os contadores e métricas são específicos de cada fase. A derrota e `R` reiniciam somente a fase atual; após vencer a quinta fase, **Nova campanha** volta à primeira.

O save em `%USERPROFILE%\.motionbound\saves\expedition.json` preserva número da fase, mapa, inimigos, corações, recursos, combate, tempo restante da apresentação e conclusão da etapa. Um save na tela de conclusão volta para essa tela, permitindo seguir sem repetir a fase. O salvamento é acionado pelo jogador em **Salvar e sair**, na pausa ou entre fases.

A dificuldade aumenta pela quantidade de guardiões, maior presença de esqueletos e disposição dos caminhos. Os controles, o dano dos inimigos e os limiares de entrada permanecem iguais. O modo reabilitação usa a mesma campanha, com mira assistida e ritmo reduzido; o terreno nevado é visual, sem exigir gestos extras ou controle de escorregamento.

## 📊 Métricas de desempenho

O jogo pretende registrar indicadores derivados das leituras do controlador e apresentá-los ao final das fases:

- **Amplitude:** extensão dos movimentos realizados em relação à calibração;
- **Precisão:** proximidade dos movimentos em relação às faixas ou posições solicitadas;
- **Estabilidade:** variação do sinal durante desafios de manutenção de posição;
- **Controle de pressão:** comportamento do sensor de preensão durante os comandos;
- **Evolução:** comparação dos indicadores entre fases ou sessões, quando implementada.

As métricas servirão para dar feedback sobre a execução **dentro do jogo**. Elas não substituem avaliação médica, fisioterapêutica ou de terapia ocupacional e não comprovam, por si só, melhora clínica.

## 🛠️ Tecnologias previstas

- **Java** — lógica e sistemas do jogo;
- **libGDX** — renderização, entrada, cenas e mecânicas 2D;
- **Arduino/ESP32** — leitura dos sensores de movimento;
- **Comunicação Serial** — integração inicial hardware/software;
- **Gradle** — gerenciamento e build do projeto Java;
- **Git e GitHub** — versionamento e acompanhamento de contribuições;
- **Trello** — organização das entregas pelo método Kanban.

> A arquitetura das classes, bibliotecas de comunicação Serial e requisitos de execução serão documentados quando o protótipo for criado.

## 📂 Estrutura atual do protótipo

```text
Motionbound/
├── README.md
├── build.gradle             # Dependências e execução do libGDX
├── settings.gradle
├── iniciar-jogo.bat         # Executa no Windows
├── verificar-ambiente.bat   # Diagnóstico de Java/Git/VS Code
├── instalar-sprites-kenney.bat
├── gerar-executavel.bat
├── assets/
│   ├── fallback/           # Tiles originais provisórios
│   ├── characters/         # Sprites temporários animados
│   └── kenney/             # Atlas oficial opcional
├── src/main/java/br/edu/motionbound/
│   ├── DesktopLauncher.java
│   ├── MotionboundGame.java
│   ├── game/
│   └── input/              # ControlSource, base para integração IoT
├── scripts/
└── docs/
```

## 🚧 Situação e etapas de desenvolvimento

- [x] Definir nome e conceito geral do jogo;
- [x] Escolher metodologia Kanban e organizar o Trello;
- [x] Criar o repositório e confirmar o cadastro na disciplina;
- [ ] Validar com o professor o controlador físico e os sensores propostos;
- [ ] Construir e testar o controlador;
- [x] Inicializar e verificar o protótipo do jogo em libGDX no Windows;
- [ ] Integrar Arduino e controles do personagem;
- [x] Desenvolver combate, habilidades e interface;
- [x] Implementar tutorial de comandos no menu e as cinco fases;
- [x] Implementar resultados e métricas de jogo/simulação;
- [ ] Implementar calibração e métricas dos sensores físicos;
- [ ] Testar o jogo, gerar executável e produzir o short paper.

## 📋 Organização e acompanhamento

O desenvolvimento será acompanhado em um **quadro Kanban** com cinco estados: **Backlog, A fazer, Em andamento, Em revisão e Concluído**. Cada card representa uma entrega, dividida em atividades menores por meio de checklists.

**Commits semanais:** cada integrante deve realizar pelo menos um commit de sua própria autoria **entre segunda-feira e domingo** e enviá-lo ao GitHub. Os commits devem representar alterações reais no projeto, publicadas manualmente pelo integrante. O e-mail configurado no Git precisa estar associado à conta do GitHub correspondente.

- [Quadro do Motionbound no Trello](https://trello.com/b/iN2g5cul/projeto-de-jogos-motionbound)
- [Repositório do projeto](https://github.com/TuideTui/Motionbound)
- [Cadastro confirmado na disciplina — Issue #42](https://github.com/leobrasil/jogos_digitais_2026-1/issues/42)

## 👤 Equipe

**Arthur Cezar da Silveira Lima** — [@TuideTui](https://github.com/TuideTui)  
**Turma:** 4H12

*Outros integrantes poderão ser adicionados aqui, caso o grupo seja ampliado.*

## 📚 Contexto acadêmico

Projeto desenvolvido para a disciplina de **Jogos Digitais (2026)**, a partir da proposta de desenvolvimento de um jogo sério de reabilitação física com **libGDX e IoT**. Os entregáveis previstos incluem o código-fonte com commits semanais, um executável ou APK e um short paper de até seis páginas.

---

**Motionbound — do movimento real à aventura virtual.**
