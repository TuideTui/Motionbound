# Motionbound ⚔️

**Do movimento real à aventura virtual.**

**Nome:** Arthur Cezar da Silveira Lima
**RA:** 10409172

Motionbound é um jogo sério de ação e aventura 2D, com visão *top-down* e arte em *pixel art*, desenvolvido em Java com o framework libGDX para a disciplina de Jogos Digitais. O jogador atravessa cinco cenários, enfrenta criaturas e utiliza espada, magia e cura para avançar na campanha.

O diferencial do projeto é o **modo reabilitação**, no qual os comandos são produzidos por movimentos físicos captados por uma **BlackBoard Edge (ESP32)** e sensores conectados. A proposta é utilizar a gamificação para estimular coordenação, inclinação e controle de movimentos de mão e punho, integrando uma atividade motora a uma experiência de exploração e combate.

> **Versão documentada:** `0.1.1` — protótipo acadêmico para Windows, com cinco fases e integração USB implementada. O Motionbound não é um dispositivo médico nem uma ferramenta de reabilitação clinicamente validada.

## 1. Visão geral

| Item | Descrição |
| --- | --- |
| Gênero | RPG de ação / *dungeon crawler* |
| Perspectiva | 2D *top-down* |
| Visual | *Pixel art* |
| Jogadores | 1 |
| Plataforma | Windows (desktop) |
| Linguagem | Java 17+ |
| Framework | libGDX 1.14.2, backend LWJGL3 |
| Build | Gradle 9.1.0, obtido pelo script de inicialização |
| Comunicação com hardware | USB/Serial com jSerialComm 2.11.4 |
| Controle IoT | BlackBoard Edge, acelerômetro, BRIICK APDS-9960 e BRIICK Keypad |
| Modos | Convencional, reabilitação com hardware e simulação para testes |
| Organização | Git/GitHub e quadro Kanban no Trello |

**Links do projeto:** [GitHub](https://github.com/TuideTui/Motionbound) · [Trello](https://trello.com/b/iN2g5cul/projeto-de-jogos-motionbound)

## 2. Objetivo e contexto

O Motionbound investiga como ações de um jogo podem ser associadas a movimentos físicos de mão e punho. A inclinação do controlador movimenta o personagem; gestos reconhecidos por um sensor acionam ataques e magia. A campanha oferece objetivos, desafios graduais e retorno visual imediato às ações do jogador.

O objetivo acadêmico é demonstrar a integração entre **jogo digital, sensores e dispositivo IoT**. A dificuldade aumenta pela variedade de inimigos e pelas situações de jogo, sem impor automaticamente maior amplitude de movimento em cada fase. A configuração do controle permite adaptar direção e sensibilidade para uma posição confortável.

## 3. Funcionalidades implementadas

- **Campanha com cinco fases:** mapas e ambientações diferentes, inimigos progressivos e baú de saída liberado após eliminar os guardiões.
- **Dois modos principais:** convencional (teclado/mouse) e reabilitação (BlackBoard Edge + APDS-9960 + Keypad). Há também uma simulação identificada na interface para testes sem hardware.
- **Combate:** espada corpo a corpo, magia à distância, barra de mana, vigor, dano, recuo, cura, coleta de corações e diferentes estados negativos.
- **Combate assistido no modo reabilitação:** marcação do inimigo visível mais próximo, aproximação orientada e golpe por gesto, com recuo após ataques próximos.
- **Inimigos com comportamentos distintos:** slime, esqueleto, mago, bruxo, ciclope, fantasma e morcego.
- **Efeitos de frascos:** fogo, veneno, lentidão e fenda de teletransporte, com áreas temporárias e efeitos visuais próprios.
- **Interface:** menu principal, seleção de modo, tutorial, bestiário, HUD de vida/vigor/mana, pausa, configurações, tela de derrota, conclusão de fase e vitória da campanha.
- **Acessibilidade e conforto visual:** opção de efeitos reduzidos, que diminui animações e flashes e desativa algumas distorções visuais.
- **Persistência:** opção de salvar e sair, continuar expedição e retomar o progresso da campanha.
- **Controle físico:** descoberta da conexão USB, calibração, ajuste de sensibilidade e orientação dos eixos, leitura de inclinação/gestos e pausa segura quando o dispositivo deixa de responder.

### 3.1. Fases da campanha

| Fase | Cenário | Dificuldade | Guardiões iniciais |
| --- | --- | --- | ---: |
| 1 | A Cripta Esquecida | Fácil | 7 |
| 2 | Vila das Lanternas | Moderado | 10 |
| 3 | Campos Corrompidos | Intermediário | 14 |
| 4 | Passagem Congelada | Difícil | 18 |
| 5 | Fortaleza do Crepúsculo | Muito difícil | 22 |

Cada etapa tem apresentação própria, contagem inicial e uma saída identificada no minimapa. Ao concluir uma fase, o jogador pode seguir ou salvar e sair. A dificuldade é ampliada principalmente pela quantidade e diversidade de inimigos. Em algumas fases, as fendas podem invocar morcegos adicionais.

### 3.2. Mecânicas de combate

O personagem utiliza espada e magia, gerenciando vida, vigor e mana. Os inimigos têm tempos de preparação que permitem identificar e evitar ataques. Algumas criaturas lançam frascos que deixam áreas temporárias no cenário:

| Frasco | Efeito |
| --- | --- |
| Vermelho | Queimadura e dano periódico |
| Verde | Veneno e dano periódico |
| Cinza | Lentidão temporária |
| Azul | Teletransporte e invocação limitada de morcegos |

No **modo reabilitação**, o combate usa assistência de mira para reduzir a necessidade de posicionamento fino do personagem. A cura é automática quando a vida chega a um coração ou menos, desde que ainda existam cargas disponíveis. No **modo convencional**, a cura é acionada manualmente.

## 4. Integração IoT: como foi implementada

O controle físico utiliza estes componentes:

| Componente | Responsabilidade |
| --- | --- |
| **BlackBoard Edge (ESP32)** | Executa o firmware e transmite dados ao computador via USB |
| **Acelerômetro MMA8452Q da Edge** | Fornece leituras de aceleração usadas para interpretar a inclinação do controle |
| **BRIICK APDS-9960** | Reconhece gestos da mão realizados sobre o sensor |
| **BRIICK Keypad** | Permite navegar, confirmar e voltar nos menus |

O firmware está em [`firmware/MotionboundDiagnostico/`](Motionbound-v0.1.1-Windows-corrigido/Motionbound/firmware/MotionboundDiagnostico/) e contém o sketch Arduino e o decodificador de gestos `SwipeDecoder.h`.

**Fluxo de comunicação:**

```text
Movimentos físicos e gestos
           |
           v
Edge / acelerômetro / APDS-9960 / Keypad
           |
           v
Firmware no ESP32
           |
           v
USB/Serial (115200 baud; protocolo MB1)
           |
           v
EdgeController (Java + jSerialComm)
           |
           v
Interpretação, calibração e comandos
           |
           v
MotionboundGame (libGDX)
           |
           v
Movimento, combate e interface
```

O firmware transmite amostras de inclinação, estado dos botões e contadores de gestos. A comunicação usa o protocolo `MB1`, com transmissão configurada para 50 Hz. O jogo recebe e valida os pacotes fora da thread de renderização, ignora mensagens inválidas ou repetidas e identifica interrupções na conexão.

A calibração registra a posição neutra com a mão mantida parada por aproximadamente **2 segundos**. O jogador pode escolher três níveis de sensibilidade, trocar os eixos e inverter as direções. Caso não cheguem dados válidos por **0,5 segundo**, a partida é pausada e exige nova calibração antes de retomar.

### 4.1. Comandos do modo reabilitação

| Ação no jogo | Comando físico |
| --- | --- |
| Movimentar o personagem | Inclinar a BlackBoard Edge |
| Correr | Manter inclinação forte na mesma direção principal por 4 segundos |
| Atacar com espada | Fazer gesto horizontal sobre o APDS-9960 |
| Lançar magia | Fazer gesto para cima sobre o APDS-9960 |
| Curar | Ação automática quando a vida está baixa, respeitando as cargas disponíveis |
| Navegar pelos menus | Usar os botões do BRIICK Keypad |

Na corrida, o jogo considera uma faixa de inclinação relativa à sensibilidade configurada. A preparação começa a partir de **60%** da faixa e é cancelada ao retornar abaixo de **40%**, parar ou alterar a direção principal. O HUD mostra o progresso dos quatro segundos e solicita que o jogador relaxe a inclinação quando o vigor se esgota.

**Importante:** o hardware desta versão **não utiliza sensor de pressão ou preensão**. O APDS-9960 interpreta gestos; ele não mede a força da mão. Os indicadores de pressão disponíveis na simulação não representam medições reais da BlackBoard Edge.

### 4.2. Navegação pelo Keypad

| Botão | Função |
| --- | --- |
| 1 | Voltar / pausar |
| 2 | Item anterior |
| 3 | Confirmar |
| 4 | Próximo item |

## 5. Como executar no Windows

### 5.1. Pré-requisitos

- Windows com suporte a aplicações desktop Java.
- **JDK 17 ou superior** instalado e `javac` disponível no ambiente.
- Acesso à internet na primeira execução para obter Gradle e dependências.
- Para o modo reabilitação físico: BlackBoard Edge, APDS-9960 e Keypad, com firmware gravado e conexão USB.

O código do jogo está na pasta:

[`Motionbound-v0.1.1-Windows-corrigido/Motionbound/`](Motionbound-v0.1.1-Windows-corrigido/Motionbound/)

1. Clone o repositório ou baixe e extraia o ZIP do GitHub.
2. Abra a pasta interna **`Motionbound`**, que contém `build.gradle` e `iniciar-jogo.bat`.
3. No Windows, execute **`iniciar-jogo.bat`**.
4. Aguarde a compilação e selecione o modo de jogo desejado.

Na primeira execução, o script baixa localmente o Gradle 9.1.0 e obtém as dependências. Não é necessário instalar o Gradle globalmente.

Para verificar o Java antes de iniciar, utilize `verificar-ambiente.bat`, na mesma pasta do jogo.

### 5.2. Modo convencional

| Comando | Ação |
| --- | --- |
| `WASD` ou setas | Movimentar |
| `Shift` | Correr |
| Mouse | Mirar |
| Clique esquerdo | Espada |
| Clique direito | Magia |
| `Espaço` | Curar |
| `Esc` | Pausar / voltar |
| `R` | Reiniciar a fase |
| `F11` | Alternar tela cheia e janela |

### 5.3. Simulação sem hardware

A opção **Experimentar simulação** permite testar parte do fluxo de controle por sensores sem conectar a Edge:

- `I`, `J`, `K`, `L`: simular inclinação e movimento;
- `F`: simular o acionamento do ataque por pressão;
- `Shift + F`: simular pressão acima da faixa definida.

Os dados desse modo são **artificiais**. A simulação não reproduz todos os gestos do dispositivo físico, como o comando de magia pelo APDS-9960, e não deve ser usada como avaliação de desempenho motor real.

### 5.4. Preparação do controle físico

1. Abra o sketch [`MotionboundDiagnostico.ino`](Motionbound-v0.1.1-Windows-corrigido/Motionbound/firmware/MotionboundDiagnostico/MotionboundDiagnostico.ino) na Arduino IDE, mantendo `SwipeDecoder.h` na mesma pasta.
2. Selecione **ESP32 Dev Module** e a porta serial correspondente à Edge.
3. Instale as bibliotecas **RoboCore - MMA8452Q**, **RoboCore - BRIICK Keypad** e **SparkFun APDS9960 RGB and Gesture Sensor**.
4. Grave o firmware. Se desejar, valide sensores e botões pelo Monitor Serial a **115200 baud**.
5. **Feche o Monitor Serial** antes de abrir o Motionbound, para liberar a porta USB.
6. No jogo, selecione **Reabilitação**, calibre a posição neutra e confirme o início pelo Keypad.

O sensor APDS-9960 deve ficar imóvel e voltado para a mão que executa os gestos. As instruções completas de montagem, testes e protocolo estão em [`firmware/README.md`](Motionbound-v0.1.1-Windows-corrigido/Motionbound/firmware/README.md).

## 6. Métricas e acompanhamento

A implementação atual registra e apresenta **métricas de jogo**, como tempo ativo da fase, guardiões derrotados e informações sobre ataques e acertos. Há também uma métrica de tempo de pressão na faixa **somente na simulação**, calculada com dados artificiais.

**Consistência motora implementada no modo físico:** uma tentativa de manter inclinação forte inicia uma janela de quatro segundos, reaproveitando os critérios da corrida. A nota é `100 × tempo dentro da faixa / tempo válido observado`. A inclinação é adequada quando o maior eixo alcança pelo menos 60% da referência e mantém a direção principal inicial (produto escalar normalizado de pelo menos 0,65, aceitando as diagonais compatíveis). Não há limite superior artificial para a inclinação. Oscilações, relaxamento ou mudança de direção dentro da janela contam fora da faixa mesmo se o contador da corrida reiniciar; isso evita uma pontuação de 100% obtida apenas por observar corridas bem-sucedidas. Caminhada leve fora de uma janela e a corrida mantida depois dos quatro segundos não acrescentam tempo à avaliação.

São usadas apenas amostras reais, válidas e consecutivas do acelerômetro, com o tempo entre suas recepções, nunca o número de quadros desenhados. Mensagens repetidas, saltos de sequência, intervalos acima de 100 ms, menus, pausas, calibração e desconexões não acrescentam tempo. Uma interrupção encerra a janela em andamento e mantém os tempos já medidos; uma nova tentativa inicia outra janela. A nota só aparece com pelo menos quatro segundos acumulados de observação válida e 100 intervalos amostrados. Antes disso, ou no modo convencional/simulado, o resultado físico é **N/D**, sem preencher valores artificiais.

**Critérios iguais nas cinco fases:** a referência de amplitude é fixada pela sensibilidade escolhida ao iniciar a campanha (0,50 g, 0,35 g ou 0,23 g), sempre relativa à posição neutra calibrada. Essa referência é salva e permanece igual entre as fases, mesmo se a sensibilidade de movimentação for ajustada depois. Para uma comparação mais direta, mantenha também a mesma sensibilidade e montagem durante a campanha. O gesto e o movimento da Edge não foram alterados, nem é necessário regravar o firmware.

A tela de conclusão apresenta a porcentagem da fase e uma linha com os resultados das cinco fases. Tempos válidos, tempos na faixa, quantidade de amostras e referência são mantidos no mesmo `expedition.json`, sem um novo arquivo de métricas. Cada conclusão no modo físico grava automaticamente o checkpoint e o histórico; **Salvar e sair** também os preserva durante a partida e na vitória. Ao continuar, os totais retornam, mas a tentativa interrompida precisa recomeçar. Reiniciar uma fase limpa somente sua medição; iniciar uma nova campanha limpa todo o histórico. Saves antigos continuam aceitos e começam com N/D. A comparação se refere às fases da campanha salva, sem criar um histórico separado de pacientes ou sessões.

Essa é uma pontuação de execução da tarefa no jogo. Tempo de fase, acertos no combate e consistência da inclinação não devem ser apresentados como comprovação clínica de reabilitação ou recuperação motora.

## 7. Salvamento e interface

O jogo permite salvar a expedição e continuar posteriormente. O arquivo de progresso é armazenado no Windows em:

```text
%USERPROFILE%\.motionbound\saves\expedition.json
```

O salvamento preserva a fase, a posição do personagem, recursos, inimigos e estados relevantes do combate. As opções de configuração do controle e de apresentação visual são guardadas separadamente nas preferências do aplicativo.

O menu de pausa contém um **Diário da expedição**, **Tutorial**, **Bestiário** e **Configurações**. O bestiário reúne informações sobre inimigos e frascos, enquanto a opção **Efeitos reduzidos** adapta elementos visuais para maior conforto.

## 8. Estrutura do código

```text
Motionbound/                           # Raiz do repositório
├── README.md                          # Este documento
└── Motionbound-v0.1.1-Windows-corrigido/
    └── Motionbound/                    # Projeto Java/libGDX
        ├── build.gradle
        ├── settings.gradle
        ├── iniciar-jogo.bat
        ├── gerar-executavel.bat
        ├── verificar-ambiente.bat
        ├── assets/                     # Sprites, fonte, ícones e licenças
        ├── art-source/                 # Origem e especificação do atlas do herói
        ├── firmware/
        │   ├── README.md
        │   ├── MotionboundDiagnostico/ # Sketch e decodificador de gestos
        │   └── tests/                 # Teste do decodificador
        ├── scripts/                    # Inicialização e utilitários
        └── src/
            ├── main/java/br/edu/motionbound/
            │   ├── DesktopLauncher.java
            │   ├── MotionboundGame.java
            │   ├── game/              # Campanha, mapas, combate, efeitos e save
            │   ├── input/             # Teclado, simulação e controle Edge
            │   └── ui/                # HUD, menus, tutorial e bestiário
            └── verification/java/      # Verificações automatizadas
```

**Organização da implementação:** `MotionboundGame` centraliza o fluxo das partidas; `CampaignLevel` descreve as cinco etapas; `DungeonMap` e classes de combate tratam cenários e confrontos; `EdgeController` faz a comunicação com o dispositivo físico; `GameUi` e componentes de `ui/` implementam as telas; `SaveData` serializa e valida o progresso.

## 9. Compilação, testes e pacote para entrega

Dentro da pasta interna `Motionbound`, execute:

- **`iniciar-jogo.bat`** — compila e inicia o jogo;
- **`gerar-executavel.bat`** — gera o JAR com dependências em `build/libs/Motionbound-0.1.1-all.jar`;
- **`verificar-ambiente.bat`** — verifica o ambiente de execução.

Após a primeira inicialização, também é possível chamar as tarefas de verificação do Gradle instalado localmente:

```powershell
.\.tools\gradle-9.1.0\bin\gradle.bat check
.\.tools\gradle-9.1.0\bin\gradle.bat verifyDesktop
```

As tarefas previstas no `build.gradle` verificam aspectos de campanha, regras de combate, efeitos, controle e salvamento. `verifyDesktop` realiza uma verificação da aplicação desktop. O teste `verifyEdgeUsb` requer o dispositivo físico conectado; não equivale a um teste automatizado sem hardware.

**Observação sobre a entrega:** o arquivo `.jar` contém as dependências Java do jogo, mas **não inclui necessariamente um runtime Java**. Para executá-lo em outro computador, é necessário um Java compatível ou um pacote Windows preparado com runtime. O ZIP de código-fonte e o pacote executável devem ser organizados e testados separadamente antes da entrega acadêmica.

## 10. Arte e recursos externos

O projeto utiliza recursos de *pixel art* da [Kenney](https://kenney.nl/), incluindo **Tiny Dungeon**, **Tiny Town**, **Tiny Farm**, **Tiny Ski**, **Input Prompts Pixel** e **Kenney Pixel**. Os avisos e arquivos de licença correspondentes estão incluídos em `assets/`.

As animações do personagem principal utilizam um atlas produzido com auxílio de IA e adaptado ao projeto. O arquivo de origem e as instruções de geração estão em [`art-source/`](Motionbound-v0.1.1-Windows-corrigido/Motionbound/art-source/). Esse atlas **não é um recurso da Kenney**.

O jogo utiliza ainda libGDX e jSerialComm; os avisos de licença do jSerialComm estão em [`assets/licenses/jSerialComm/`](Motionbound-v0.1.1-Windows-corrigido/Motionbound/assets/licenses/jSerialComm/).

## 11. Desenvolvimento e acompanhamento

O desenvolvimento foi organizado por um quadro **Kanban no Trello**, com atividades planejadas, em andamento, em revisão e concluídas. O repositório GitHub mantém o histórico das alterações realizadas durante o projeto. Os commits são feitos manualmente pelo aluno, conforme o acompanhamento semanal solicitado pela disciplina.

- **Repositório:** https://github.com/TuideTui/Motionbound
- **Quadro Kanban:** https://trello.com/b/iN2g5cul/projeto-de-jogos-motionbound

## 12. Autoria

**Arthur Cezar da Silveira Lima** — RA **10409172**  
**Curso:** Ciência da Computação  
**Turma:** 4H12  
**Instituição:** Universidade Presbiteriana Mackenzie  
**Disciplina:** Jogos Digitais  
**GitHub:** [@TuideTui](https://github.com/TuideTui)

---

*Motionbound - do movimento real à aventura virtual.*
