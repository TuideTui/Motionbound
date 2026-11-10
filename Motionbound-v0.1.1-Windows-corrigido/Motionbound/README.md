# 🎮 Motionbound

**Um dungeon crawler 2D em pixel art controlado por movimentos físicos.**

**Motionbound** é um projeto acadêmico de jogo sério desenvolvido para a disciplina de Jogos Digitais. Inspirado nas mecânicas de exploração e combate de jogos do gênero *dungeon crawler*, o projeto propõe uma aventura de fantasia em que o jogador explora masmorras, enfrenta monstros e utiliza ataques, magias e cura. O diferencial está no controle: em vez dos comandos tradicionais de teclado, os movimentos do personagem serão realizados por meio de um dispositivo físico com Arduino/ESP32 e sensores.

O projeto busca explorar a **gamificação de atividades motoras de mão, punho e antebraço**, reunindo uma experiência de jogo com indicadores de execução e evolução dos movimentos. Trata-se de um protótipo educacional, não de uma ferramenta clínica validada.

> **Estado atual: campanha de cinco fases para Windows com controle USB.** A BlackBoard Edge controla o movimento por inclinação; o APDS-9960 reconhece espada/magia e o Keypad navega pelos menus. O jogo oferece calibração, sensibilidade, troca/inversão de eixos, cura automática e pausa por desconexão. O modo convencional e a bancada de simulação continuam disponíveis. O [firmware e as instruções do controle](firmware/README.md) estão incluídos. O nome do JAR continua `Motionbound-0.1.1-all.jar`.

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

A abertura oferece **Modo convencional** e **Modo reabilitação**. O convencional usa os controles abaixo. Na reabilitação com a Edge, inclinação move e gestos acionam espada/magia com mira assistida. Sem a manopla, **Experimentar simulação** permite testar uma bancada virtual: `I J K L` simulam inclinação, `F` simula aperto e `Shift + F` simula pressão acima da faixa. Solte e aperte novamente para cada golpe. A bancada não simula magia; o controle físico já a aciona por gesto. **A cura é automática com 1 coração ou menos, enquanto o personagem estiver vivo:** consome uma das 3 cargas da fase, recupera 2 corações e remove fogo/veneno, com intervalo de 1,7 segundo. Um aviso pixelado no canto esquerdo informa as cargas restantes por 4 segundos; o contador também aparece no HUD. Pausar congela o aviso e a cura; o save preserva as cargas. Coletar corações continua recuperando vida sem gastar cargas. No modo convencional, a cura permanece manual pelo Espaço.

**Controle físico:** feche o Monitor Serial, conecte a Edge com o firmware atualizado e escolha Reabilitação. Confirme a calibração e mantenha a mão confortável e parada por 2 segundos; depois confirme para iniciar. A Edge é localizada pelo adaptador USB Silicon Labs e transmite a 115200 baud. O Keypad funciona desde a seleção de modo: 1 pausa/volta, 2 anterior, 3 confirma, 4 próximo, incluindo abas e fichas. Gestos horizontais acionam a espada; para cima, a magia. Em Ajustar controle (também nas Configurações da pausa), altere sensibilidade, troca de eixos e inversões. Configurações são preservadas em `.motionbound/preferences`; a posição neutra é recalibrada em cada conexão. Sem dados válidos por 0,5 segundo ou com falha em qualquer sensor, a partida pausa e exige recalibração para retomar. Nenhum indicador de pressão é atribuído ao dispositivo físico: ele usa acelerômetro e gestos, sem FSR. Saves distinguem `physicalController` de `simulatedSensors`.

**Bancada virtual, sem a Edge:** a simulação usa `SensorControlSource`, suavização, zona neutra, limiares separados de aperto/soltura e interrupção ao perder amostras. IJKL e F são recursos de teste, identificados na interface e no save. A implementação USB usa `EdgeController`, protocolo `MB1`, validação de pacotes, sequência, identificação de reinício e contadores de gestos, com recepção fora da renderização. A biblioteca serial é [jSerialComm 2.11.4](https://fazecast.github.io/jSerialComm/).

**Corrida automática com a Edge:** manter inclinação forte na mesma direção principal por 4 segundos ativa a corrida, inclusive ao alternar entre suas duas diagonais. A contagem começa a partir de 60% da faixa de sensibilidade configurada, considerando o maior eixo; uma inclinação leve nunca inicia o contador. Depois de iniciada, a faixa entre 40% e 60% mantém a intenção, evitando oscilações. Aliviar abaixo de 40%, voltar ao neutro ou sair da direção principal reinicia a contagem. O indicador pixelado junto ao vigor preenche quatro segmentos, mostra os segundos restantes e muda para **ATIVA** durante a corrida. A velocidade aumenta de 54 para 85 unidades por segundo, preservando a direção e a suavização existentes; correr consome vigor. Quando ele acaba, o HUD pede **RELAXE**: alivie a mão antes de iniciar outra contagem, evitando corridas em pequenos pulsos durante a recuperação. Pausa, calibração, troca de fase, carregamento e perda de conexão limpam o contador; tempo em menus não conta. O firmware atual já fornece todos os dados necessários, sem nova gravação.

**Espada guiada na reabilitação:** o inimigo visível mais próximo, em qualquer direção, recebe uma marca pixelada até 3 vezes o alcance da espada (96,6 unidades). Dourado indica avanço: o gesto lateral aproxima o herói e então inicia o golpe. Verde indica combate próximo: golpe imediato, seguido de recuo de até 18 unidades após o impacto. A espada continua causando 1 de dano a todos os inimigos dentro de seu arco de 120° e alcance normal de 32,2 unidades; o círculo auxiliar não causa dano. Paredes bloqueiam a marcação e interrompem avanços/recuos, com colisões verificadas a cada 2 unidades. A direção acompanha apenas o alvo escolhido durante o avanço, com distância total limitada, e fica travada durante o golpe. Cada ataque guiado tem intervalo de 0,8 segundo e breve proteção durante o deslocamento e a chegada; efeitos periódicos continuam ativos. Sem alvo, o gesto executa o golpe comum. Pausa congela a ação, saves preservam avanço/golpe/recuo sem repetir o impacto, e teletransporte ou derrota cancelam a assistência. O movimento da Edge e o firmware permanecem os mesmos; a simulação de reabilitação também usa a assistência.

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

### Inimigos, frascos e efeitos

Os inimigos avisam seus ataques por uma preparação animada, medidor e marcas no chão. Golpes de espada e magia interrompem essa preparação por meio do recuo. Uma navegação compartilhada permite contornar casas e paredes.

| Inimigo | Comportamento |
| --- | --- |
| Slime | Prepara e executa um salto curto. |
| Esqueleto | Prepara um golpe frontal; sair do alcance permite desviar. |
| Mago roxo | Dois frascos de veneno, depois cinco raios em um leque de 45°. A direção fica fixa durante a sequência. |
| Bruxo encapuzado | Usa lentidão seguida de três frascos de fogo em um leque de 45°. Quando o jogador se aproxima, contra-ataca com um frasco cinza rápido. |
| Ciclope | Tem 4 pontos de vida e prepara um impacto circular mais lento. |
| Fantasma | Nas fases 4 e 5, lança um frasco azul. Depois, flutua e desfere investidas de meio coração. |
| Morcego | É invocado pela fenda, bate asas e prepara um mergulho; tem 1 ponto de vida. |

| Frasco | Efeito ao atingir a área marcada |
| --- | --- |
| Vermelho | Área de chamas e brasas por **5 segundos**. Queimadura: 4 pulsos de **meio coração**, um por segundo; total de **2 corações**. |
| Verde | Poça verde brilhante e borbulhante por **5 segundos**. Veneno: a mesma dose de 4 pulsos e 2 corações. |
| Cinza | Névoa cinza por **5 segundos**. Lentidão por 4 segundos, com velocidade reduzida para 60%. |
| Azul | Redemoinho azul com partículas atraídas ao centro por **5 segundos**. Teleporte para uma área livre e **3 morcegos na primeira ativação**. |

Os frascos têm trajetória em arco, aviso do local de queda, animação de quebra e partículas. **Todos deixam uma pequena área por 5 segundos, mesmo no impacto direto no jogador.** O impacto aplica o efeito; depois, atravessar a área também pode aplicá-lo. Veneno, queimadura e lentidão não reiniciam enquanto estão ativos; podem ser adquiridos novamente quando terminam, se a área ainda existir. A duração da área é independente da duração do estado adquirido: sair dela ou vê-la desaparecer não cancela a dose. Fogo e veneno não acumulam entre si. A cura remove ambos; saia da área antes de usá-la para evitar uma nova dose.

O contra-ataque cinza do bruxo viaja em linha direta e pode acertar durante o voo: desviar para o lado ajuda a escapar. O HUD mostra meios corações, nome do estado e tempo restante. A pausa congela efeitos, áreas, suas animações, frascos e raios. As áreas respeitam as paredes e são preservadas no salvamento. A magia pode interromper a preparação e a sequência de raios com seu knockback.

**Partículas nas laterais da câmera:** fogo e fumaça usam desenhos de pixel art com quatro quadros de animação próprios. As chamas têm contornos em blocos e cores chapadas vermelhas, laranjas e amarelas; mudam de formato enquanto sobem. As nuvens cinza se deformam lentamente com transparência. Os sprites são ampliados com filtro Nearest, sem desfoque, e reutilizados em memória. As bolhas verdes do veneno flutuam e estouram, maiores e em menor quantidade para parecerem próximas da câmera sem sobrecarregar as bordas. Os efeitos ficam atrás do HUD e preservam o centro da tela, mesmo com estados combinados. A animação congela ao pausar e desaparece após o estado terminar ou ser removido pela cura. Menus e novas fases limpam a camada; carregar uma partida com estados ativos restaura os indicadores.

**Transição da fenda:** antes de mudar de posição, o jogador passa por **0,3 segundo de sucção**. Um shader contrai e torce a imagem real do cenário, seguido de uma expansão breve na chegada, sem lançar partículas azuis pela tela. O HUD é desenhado depois da distorção e permanece legível. Os cantos da imagem permanecem preenchidos e o filtro Nearest preserva os pixels. O movimento e os ataques ficam suspensos durante a sucção e o herói recebe proteção. As invocações continuam limitadas e não se repetem ao carregar o jogo. Pausar congela a sequência, e um save feito durante a sucção retoma o mesmo destino e tempo restante.

**Tela preenchida:** o cenário usa uma câmera que se adapta à proporção da janela, inclusive 4:3, 16:10 e telas largas, preenchendo toda a área disponível sem faixas de proporção. A escala dos sprites é uniforme. A área visível do mapa e os limites da câmera acompanham o tamanho da tela, assim como a mira do mouse.

A abertura usa **tela cheia sem bordas**, permitindo sobrepor a ferramenta de captura do Windows (`Windows + Shift + S` ou `Print Screen`) sem minimizar o jogo. Perder o foco não pausa automaticamente a partida; a pausa manual continua em `Esc`. `F11` alterna entre tela cheia sem bordas e janela com moldura.

**Acabamento visual da jornada:** corações coletados dão um salto e voam em curva até o coração correspondente no HUD, que pulsa na chegada. A recuperação de vida acontece imediatamente e uma única vez; o voo dura 0,8 segundo, acompanha mudanças no tamanho da janela e congela ao pausar. Dano quebra visualmente os corações perdidos; vigor recuperado ilumina seus segmentos, mana consumida deixa um rastro nos cristais, e a magia recebe um destaque quando o intervalo termina e há mana suficiente para usá-la.

Os impactos de espada e magia têm faíscas distintas e um breve brilho no inimigo. Golpes fatais recebem uma pausa de impacto de 45 ms. Esqueletos se desmontam, slimes se achatam e se desfazem, fantasmas evaporam, morcegos se dispersam e os demais inimigos tombam. A preparação dos ataques inclui inclinação do corpo, frascos erguidos e o cajado do mago acumulando energia. A saída fecha uma íris de blocos ao redor do personagem; a fase seguinte se revela por uma abertura pixelada antes da contagem terminar. Menus têm marcadores animados, transições de pergaminho entre páginas e abas e resultados revelados em sequência. A vegetação balança, bandeiras ondulam, a água do poço cintila e cada ambiente tem partículas discretas, incluindo neve, poeira e pequenos insetos.

**Conforto visual:** em `Esc → Configurações → Efeitos reduzidos`, é possível desligar tremores, flashes de impacto, pausa de impacto e distorção de teleporte, além de reduzir partículas e movimento da interface e da vegetação. O modo reabilitação começa com essa opção ligada; uma escolha manual passa a valer para os dois modos e é preservada entre sessões em `.motionbound/preferences`, na pasta do usuário. A configuração altera a apresentação visual; danos, efeitos dos frascos, inimigos, cura e controles mantêm suas regras.

O **fantasma recebe o frasco azul nas fases 4 e 5**, com uma tentativa por fantasma e até duas tentativas de fenda por fase, no máximo seis morcegos adicionais. Depois do arremesso, volta ao combate próximo. O redemoinho pode teleportar novamente após a chegada e sua proteção breve; cada fenda invoca seus três morcegos apenas na primeira ativação. Assim, voltar ao mesmo redemoinho continua perigoso sem gerar ondas infinitas. Os morcegos só agem após sua animação de aparição, entram na contagem necessária para liberar a saída e não deixam corações. Os primeiros mapas introduzem as criaturas gradualmente; a fase final combina os tipos. **Esc > Bestiário > Frascos** explica as áreas, durações e regras de reaplicação.

Cada guardião derrotado tem **30% de chance de deixar um coração no chão**; os morcegos invocados não deixam itens. A coleta recupera **1 coração perdido**, até o limite atual de 6; ela não aumenta a vida máxima. Com vida cheia, o item permanece no chão. Corações, ataques, preparações dos inimigos, frascos em voo, raios, sequência de disparos, fogo no chão, efeitos e invocações são preservados no salvamento. Saves dos formatos 1 e 2 continuam na cripta da fase 1; saves dos formatos 3, 4, 5, 6 e 7 mantêm a fase e o progresso. Todos são convertidos para o formato 8 no próximo salvamento. Na vila e na fortaleza reformuladas, posições antigas que coincidam com construções são transferidas para o chão livre mais próximo. Vida, recursos e inimigos derrotados são preservados. Existe um único espaço de save: iniciar outro modo não apaga o anterior, mas **Salvar e sair** o substitui.

### Experimentar e comparar

**Esc > Tutorial** apresenta duas abas independentes: **Modo Convencional** e **Modo Reabilitação**. As teclas e os botões do mouse usam os ícones oficiais do [Kenney Input Prompts Pixel](https://kenney.nl/assets/input-prompts-pixel), incluídos no projeto com sua licença CC0. A aba de reabilitação ilustra inclinação, gestos de espada/magia, Keypad e cura automática. Ler outra aba não altera o modo da partida. O Keypad percorre as abas e confirma sua seleção sem teclado ou mouse.

**Esc > Bestiário** separa **Bestiário** e **Frascos** em catálogos. Cada uma das sete criaturas e dos quatro frascos tem uma ficha **Saiba mais**, com comportamento, dano/duração e estratégias. Clique nos cartões ou navegue com **Tab / Cima / Baixo** e confirme com **Enter**. **Esquerda / Direita** troca a aba; **Esc** fecha a ficha, depois volta ao resumo da expedição. A aventura permanece pausada durante a leitura.

Use `iniciar-jogo.bat` para a implementação atual. O trabalho está na branch local `experimento/combate-mira`; commits e push são feitos manualmente pelo aluno. A `main` mantém o histórico anterior, mas alterações sem commit acompanham a troca de branch; trocar de branch sozinho não desfaz a experiência.

Neste computador, `comparar-versao-anterior.bat` executa a cópia anterior preservada em `build/rollback/Motionbound-antes-combate.jar`. Também foram preservados o checkpoint anterior em `build/rollback/expedition-antes-combate.json` e o JAR imediatamente anterior à campanha em `build/rollback/Motionbound-antes-fases.jar`. O iniciador de comparação não restaura nem modifica os arquivos do projeto. O jogo antigo não lê os novos formatos de save: para comparar, inicie uma nova partida e feche a janela sem salvar. A pasta `build/` é local, ignorada pelo Git e removida por `gradle clean`; preserve essas cópias antes de limpar o build.

Para verificar combate, efeitos, navegação, salvamento e conectividade dos cinco mapas, execute `gradle check`. `gradle verifyDesktop` abre brevemente o jogo e verifica também os frascos, doses, pausa, teleporte, invocações, contagem regressiva, transições, derrota e continuação de saves. Grava capturas em `build/review/`, usando saves de teste separados do seu checkpoint. `gradle fatJar` gera o executável com todos os recursos.

**Objetivo da campanha:** derrotar todos os guardiões de cada mapa e alcançar o baú da saída, marcado no minimapa. O baú só libera a próxima fase quando todos os inimigos caem. Após a fase 5, aparece a vitória da campanha.

**Sem Arduino por enquanto:** esta é uma versão provisória de desenvolvimento, com seleção de modo e simulação de sensores. A estrutura `ControlSource` recebe comandos do teclado/mouse ou de amostras normalizadas; o transporte USB e a calibração real ainda faltam. A versão apresentada como jogo sério para avaliação do professor deverá funcionar com o dispositivo IoT.

**Assets:** os cenários misturam Tiny Dungeon, Tiny Town, Tiny Farm e Tiny Ski (16×16, CC0). Os novos inimigos e frascos usam os sprites oficiais do Tiny Dungeon, com animações de preparação, flutuação, salto, arremesso e asas feitas em código. Do Ski são usados neve, pinheiros e os números 1, 2 e 3 da contagem regressiva. Slimes, esqueletos e detalhes complementares da interface são originais do projeto; o herói é arte gerada com IA, documentada em `art-source/`.

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

A associação atual entre movimentos e ações é ajustável nos testes do protótipo:

| Movimento captado | Ação planejada no jogo | Aspecto motor observado |
| --- | --- | --- |
| Inclinar o controlador | Andar na direção correspondente | Direção e amplitude |
| Sustentar inclinação forte por 4 segundos | Correr, consumindo vigor | Direção sustentada |
| Passar a mão horizontalmente sobre o APDS | Ataque básico | Movimento lateral |
| Passar a mão para cima sobre o APDS | Lançar magia | Movimento direcional |
| Pressionar os quatro botões do Keypad | Pausa e navegação por menus | Coordenação dos dedos |
| Vida em 1 coração ou menos | Cura automática limitada a 3 cargas por fase | Permite concentrar-se nos movimentos |

As ações deverão ser executadas **pelo dispositivo físico**, sem exigir teclado ou controle convencional durante a partida. Controles temporários de teste podem ser utilizados apenas nas primeiras etapas de desenvolvimento.

## 🔌 Integração com Arduino/IoT

O protótipo deverá captar movimentos do jogador, processá-los e enviá-los ao aplicativo em Java/libGDX.

**Componentes do protótipo:**

- **BlackBoard Edge (ESP32):** placa com acelerômetro MMA8452Q integrado;
- **BRIICK APDS-9960:** sensor óptico para gestos direcionais;
- **BRIICK Keypad:** quatro botões para menus;
- **conexão USB/Serial:** alimentação e dados em um cabo; os módulos usam os cabos BRIICK.

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
| **5** | Fortaleza do Crepúsculo | Muito difícil | 22 | Pátios de areia entre ruínas com muralhas, laboratório, treinamento e cemitério elevado. |

Cada entrada mostra **Fase N - Nível ...** e o nome do mapa com a fonte pixelada da interface. A contagem **3, 2, 1** usa os próprios sprites do Tiny Ski, ampliados sem suavização. Durante a apresentação, personagens, combate e cronômetro ficam parados. `Esc` pausa a apresentação e retoma de onde parou.

As tochas de parede aparecem somente nas paredes dos mapas internos. As fases externas não recebem tochas soltas nem os respectivos efeitos de luz sobre o chão.

Ao concluir uma etapa, o jogador pode descansar, **seguir para a próxima fase** ou **salvar e sair**. A próxima etapa repõe vida, vigor, mana e três poções; os contadores e métricas são específicos de cada fase. A derrota e `R` reiniciam somente a fase atual; após vencer a quinta fase, **Nova campanha** volta à primeira.

O save em `%USERPROFILE%\.motionbound\saves\expedition.json` preserva número da fase, mapa, inimigos, corações, recursos, combate, tempo restante da apresentação e conclusão da etapa. Um save na tela de conclusão volta para essa tela, permitindo seguir sem repetir a fase. O salvamento é acionado pelo jogador em **Salvar e sair**, na pausa ou entre fases.

A dificuldade aumenta pela quantidade e variedade de guardiões e pela disposição dos caminhos. O modo reabilitação usa a mesma campanha, com mira assistida, inimigos mais lentos, preparações mais longas e frascos com voo mais demorado. Os limiares de entrada dos sensores permanecem iguais; a neve é visual, sem exigir gestos extras ou controle de escorregamento.

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
