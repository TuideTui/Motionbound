# Controle fisico do Motionbound

## Primeiro teste: componentes

O sketch `MotionboundDiagnostico/MotionboundDiagnostico.ino` verifica a BlackBoard Edge,
o BRIICK Keypad e o BRIICK APDS-9960 ligados pelos cabos BRIICK. O mesmo firmware
tambem transmite os dados ao jogo quando recebe o comando `MB_START`.

1. Arduino IDE: selecione **ESP32 Dev Module** e a porta da Edge (neste computador, **COM3**).
2. Instale **RoboCore - MMA8452Q**, **RoboCore - BRIICK Keypad** e
   **SparkFun APDS9960 RGB and Gesture Sensor** pelo Library Manager.
3. Abra o `.ino` pelo menu **File > Open** e clique em **Upload** (seta para a direita).
4. Abra **Tools > Serial Monitor** e selecione **115200 baud**.
5. Se o cabecalho inicial ja tiver passado, pressione **RESET** na Edge ou espere
   a repeticao do estado, a cada 8 segundos. Nao pressione RESET enquanto estiver gravando.

Ao iniciar, o programa procura os enderecos I2C `0x1D` (ou `0x1C`) do acelerometro,
`0x27` do Keypad e `0x39` do APDS. Depois deve mostrar `[OK]` para cada componente.
Essas mensagens confirmam a inicializacao; o funcionamento precisa ser conferido
tambem pelos testes abaixo. Se houver falha, desligue o USB antes de ajustar os cabos.

| Teste | Resultado esperado no Monitor Serial |
| --- | --- |
| Inclinar a Edge para cada lado | Valores `INCLINACAO: X=... Y=... Z=...` variando |
| Pressionar e soltar botoes 1 a 4 | Numero correto e mensagens `PRESSIONADO` / `SOLTO`; o LED correspondente acompanha o botao |
| Passar a mao horizontalmente sobre o APDS | `GESTO ESQUERDA` ou `GESTO DIREITA` |
| Passar a mao de baixo para cima sobre o APDS | `GESTO CIMA` (a orientacao fisica sera ajustada na calibracao) |

O APDS deve ficar parado, com o sensor voltado para a mao. Passe a mao cerca de
2 a 5 cm acima dele e retire-a ao concluir. Cima/baixo/esquerda/direita sao relativos
ao sensor; a calibracao do jogo devera ajustar essa orientacao. O APDS nao reconhece
mao aberta/fechada como uma camera.

A biblioteca SparkFun inicializa o APDS, mas o teste usa um decodificador proprio
em `SwipeDecoder.h`: le pequenas quantidades da fila U/D/L/R a cada 20 ms, em vez
de aguardar a mao sair dentro de `readGesture()`. A inclinacao, os botoes e as
mensagens continuam ativos com a mao sobre o sensor. Apos 1,6 segundo de gesto,
a tentativa expira e exige afastar a mao para armar outra; ruido, fila saturada
e leituras incompletas nao geram comandos. Sao reconhecidas quatro direcoes;
aproximacao e afastamento, sozinhos, nao estao associados a comandos.

Depois de atualizar o sketch, feche e reabra seu arquivo na Arduino IDE para
carregar tambem o novo `SwipeDecoder.h`, grave novamente e abra o Monitor Serial
em 115200. O resumo deve incluir **Leitura de gestos v2**. Se o monitor ficar vazio,
pressione e solte RESET com o monitor aberto; copie as ultimas linhas caso pare.

Nao deixe o Monitor Serial aberto ao usar posteriormente a porta no jogo: apenas
um programa deve abrir a COM3 por vez. Este teste nao mede pressao/preensao.

## Comandos combinados para a integracao

- Edge na mao direita: inclinacao para movimentar, com posicao neutra e sensibilidade calibradas.
- APDS apoiado e imovel: gesto horizontal para espada; gesto para cima para magia.
- Keypad: 1 pausa/volta; 2 item anterior; 3 confirma; 4 proximo item, incluindo abas e submenus.
- Cura no jogo: automatica com 1 coracao ou menos, usando as cargas da fase.

## Jogar pelo USB

1. Grave o sketch atualizado com `SwipeDecoder.h` na mesma pasta. A gravacao inicializa a Edge.
2. Feche o Monitor Serial para liberar a porta e abra o jogo normalmente.
3. No Keypad, use 4 para selecionar Reabilitacao e 3 para confirmar.
4. Confirme **Calibrar a posicao da mao**, mantendo a mao direita em posicao confortavel por 2 segundos.
5. Confirme **Iniciar com a manopla**. O APDS fica fixo na mesa, voltado para a outra mao.
6. Se as direcoes nao coincidirem, pause e abra **Configuracoes > Controle USB**.
   Troque os eixos ou inverta esquerda/direita e frente/tras. A sensibilidade tem tres niveis.

A corrida e calculada no jogo, usando o mesmo firmware: inclinar forte na mesma
direcao principal por 4 segundos ativa a corrida. Suas diagonais mantem a contagem.
Ela comeca em 60% da faixa configurada (maior eixo) e e cancelada abaixo de 40%,
ao parar ou ao mudar a direcao principal. O HUD preenche quatro segmentos junto
ao vigor e mostra ATIVA ao correr. Esgotar vigor pede RELAXE: alivie a mao para
iniciar outra contagem. Pausa/calibracao/carregamento tambem reiniciam o contador.

O jogo usa jSerialComm 2.11.4, recepcao em thread separada e descoberta automatica
do adaptador Silicon Labs (VID 0x10C4, PID 0xEA60). Apenas dados com o prefixo e formato
esperados sao aceitos. Abertura/reconexao ou reinicio da placa nao repetem gestos anteriores;
um botao mantido pressionado na conexao precisa ser solto antes de navegar.

O comando `MB_START` ativa o fluxo ASCII a 50 Hz, em 115200 baud. Cada linha tem:

```text
MB1;boot;sequencia;aceleracaoX;aceleracaoY;aceleracaoZ;botoes;contadorEspada;contadorMagia;estado
```

Numeros de boot, sequencia e contadores sao inteiros de 32 bits sem sinal.
Os botoes usam bits 0 a 3. `estado=7` significa leituras saudaveis do acelerometro,
Keypad e APDS (bits 0, 1 e 2). Pacotes duplicados, fora de ordem ou invalidos nao
geram acoes. Gestos sao contadores para evitar repeticao ao renderizar mais de um quadro.
Leituras incompletas e expiradas interrompem a entrada; sem dados saudaveis por 0,5 segundo,
o jogo pausa. A retomada exige nova calibracao. O save guarda qual fonte de controle foi usada.

`MB_STOP` volta ao diagnostico; RESET tambem volta ao diagnostico inicial. O jogo
solicita `MB_START` automaticamente. Nao abra o Monitor Serial enquanto joga.
O acelerometro usa transacoes limitadas pelo timeout I2C, incluindo a inicializacao;
a leitura nao fica em loops esperando bytes de um dispositivo desconectado.

Referencias do fabricante:

- [Preparacao da Edge](https://www.robocore.net/tutoriais/primeiros-passos-com-blackboard-edge)
- [Acelerometro](https://www.robocore.net/tutoriais/leitura-acelerometro-blackboard-edge)
- [Keypad](https://www.robocore.net/tutorials/primeiros-passos-briick-keypad)
- [Gestos APDS](https://www.robocore.net/tutorials/primeiros-passos-com-briick-apds9960)
