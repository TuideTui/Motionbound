/*
 * Motionbound - teste da BlackBoard Edge + BRIICK Keypad + BRIICK APDS-9960.
 * Placa: ESP32 Dev Module. Monitor Serial: 115200 baud.
 * Este sketch verifica os componentes; ainda nao envia comandos ao jogo.
 */
#include <Wire.h>
#include <RoboCore_MMA8452Q.h>
#include <BRIICK_Keypad.h>
#include <SparkFun_APDS9960.h>
#include "SwipeDecoder.h"
#include <esp_random.h>

MMA8452Q accelerometer;
BRIICK_Keypad keypad;
SparkFun_APDS9960 gestures;
SwipeDecoder swipe;

bool accelerometerOk = false;
bool keypadOk = false;
bool gesturesOk = false;
uint8_t candidateButtons = 0;
uint8_t stableButtons = 0;
uint32_t buttonChangedAt = 0;
uint32_t lastButtonsAt = 0;
uint32_t lastTiltAt = 0;
uint32_t lastStatusAt = 0;
uint32_t lastGestureAt = 0;
bool keypadReadFailed = false;
bool gestureReadFailed = false;
bool streamMode = false;
bool tiltReadOk = false;
uint8_t acceleratorAddress = 0x1D;
uint32_t bootId = 0, sequence = 0, attackCount = 0, magicCount = 0, lastStreamAt = 0;
char serialCommand[24];
uint8_t serialCommandLength = 0;

bool readRegisters(uint8_t address, uint8_t reg, uint8_t *data, size_t size) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0 || Wire.requestFrom(address, size) != size) return false;
  for (size_t i = 0; i < size; ++i) data[i] = uint8_t(Wire.read());
  return true;
}

bool writeRegister(uint8_t address, uint8_t reg, uint8_t data) {
  Wire.beginTransmission(address); Wire.write(reg); Wire.write(data);
  return Wire.endTransmission() == 0;
}

bool configureAccelerometer(uint8_t address) {
  uint8_t id = 0;
  if (!readRegisters(address, 0x0D, &id, 1) || id != 0x2A || !writeRegister(address, 0x2A, 0)) return false;
  delay(3); // wait for standby before changing range/ODR, during setup only
  return writeRegister(address, 0x0E, 0)
      && writeRegister(address, 0x2A, 0x19); // 2g, 100 Hz, active; bounded I2C throughout
}

bool presentAt(uint8_t address) {
  Wire.beginTransmission(address);
  return Wire.endTransmission() == 0;
}

void showStatus() {
  Serial.println();
  Serial.println("--- ESTADO DOS COMPONENTES ---");
  Serial.println(accelerometerOk ? "[OK] MMA8452Q / inclinacao da Edge" : "[FALHA] MMA8452Q / inclinacao da Edge");
  Serial.println(keypadOk ? "[OK] BRIICK Keypad / 4 botoes" : "[FALHA] BRIICK Keypad / 4 botoes");
  Serial.println(gesturesOk ? "[OK] APDS-9960 / gestos" : "[FALHA] APDS-9960 / gestos");
  Serial.println("Incline a Edge, pressione cada botao e passe a mao sobre o APDS.");
  Serial.println("Gestos: passe a mao a 2-5 cm do sensor e retire-a depois do movimento.");
  Serial.println("Leitura de gestos v2: inclinacao e botoes continuam ativos durante o gesto.");
  Serial.println("Se houver falha: desligue o USB antes de conferir os cabos BRIICK.");
  Serial.println();
}

void scanBus() {
  Serial.println("Dispositivos no barramento I2C:");
  unsigned int found = 0;
  for (uint8_t address = 8; address < 0x78; ++address) {
    if (!presentAt(address)) continue;
    Serial.printf("  0x%02X", address);
    if (address == 0x1D || address == 0x1C) Serial.print(" - acelerometro");
    if (address == 0x27) Serial.print(" - Keypad");
    if (address == 0x39) Serial.print(" - APDS-9960");
    Serial.println();
    ++found;
  }
  if (found == 0) Serial.println("  Nenhum dispositivo encontrado.");
}

void setup() {
  bootId = esp_random();
  Serial.begin(115200);
  delay(1200);
  Serial.println();
  Serial.println("=== MOTIONBOUND / DIAGNOSTICO DO CONTROLE ===");
  Wire.begin();
  Wire.setTimeOut(50);
  scanBus();

  if (presentAt(0x1D)) {
    Serial.println("Iniciando acelerometro em 0x1D...");
    accelerometerOk = configureAccelerometer(acceleratorAddress);
  } else if (presentAt(0x1C)) {
    Serial.println("Iniciando acelerometro em 0x1C...");
    acceleratorAddress = 0x1C;
    accelerometerOk = configureAccelerometer(acceleratorAddress);
  }
  Serial.println("Iniciando Keypad...");
  keypadOk = presentAt(KEYPAD_ADDRESS) && keypad.config();
  if (keypadOk) keypad.attachLED(KEYPAD_LED_ALL);
  Serial.println("Iniciando APDS-9960...");
  gesturesOk = presentAt(0x39) && gestures.init() && gestures.enableGestureSensor(false);
  showStatus();
  lastStatusAt = millis();
}

void readButtons(uint32_t now) {
  if (!keypadOk || now - lastButtonsAt < 10) return;
  lastButtonsAt = now;
  const uint8_t buttons = keypad.readButton(KEYPAD_BTN_ALL);
  if (buttons == KEYPAD_ERROR) {
    if (!keypadReadFailed && !streamMode) Serial.println("[FALHA] Nao foi possivel ler o Keypad; confira os cabos com o USB desligado.");
    keypadReadFailed = true;
    return;
  }
  keypadReadFailed = false;
  if (buttons != candidateButtons) {
    candidateButtons = buttons;
    buttonChangedAt = now;
  }
  if (now - buttonChangedAt < 30 || candidateButtons == stableButtons) return;
  const uint8_t changed = candidateButtons ^ stableButtons;
  stableButtons = candidateButtons;
  if (streamMode) return;
  const uint8_t masks[] = {KEYPAD_BTN_1, KEYPAD_BTN_2, KEYPAD_BTN_3, KEYPAD_BTN_4};
  const char *actions[] = {"PAUSAR/VOLTAR", "ITEM ANTERIOR", "CONFIRMAR", "PROXIMO ITEM"};
  for (uint8_t i = 0; i < 4; ++i) {
    if (!(changed & masks[i])) continue;
    Serial.printf("BOTAO %u %s -> %s\n", i + 1,
                  (stableButtons & masks[i]) ? "PRESSIONADO" : "SOLTO", actions[i]);
  }
}

void readTilt(uint32_t now) {
  if (!accelerometerOk || now - lastTiltAt < (streamMode ? 20U : 350U)) return;
  lastTiltAt = now;
  uint8_t data[6];
  tiltReadOk = readRegisters(acceleratorAddress, 0x01, data, 6);
  if (!tiltReadOk) {
    if (!streamMode) Serial.println("[FALHA] Acelerometro nao responde.");
    return;
  }
  accelerometer.x = (int16_t(uint16_t(data[0]) << 8 | data[1]) >> 4) / 1024.0f;
  accelerometer.y = (int16_t(uint16_t(data[2]) << 8 | data[3]) >> 4) / 1024.0f;
  accelerometer.z = (int16_t(uint16_t(data[4]) << 8 | data[5]) >> 4) / 1024.0f;
  if (streamMode) return;
  Serial.printf("INCLINACAO: X=%+.3f  Y=%+.3f  Z=%+.3f g\n",
                accelerometer.x, accelerometer.y, accelerometer.z);
}

// requestFrom has a Wire timeout; a missing/short response is a failed sample,
// never an unbounded while(!Wire.available()) wait.
bool readApdsBytes(uint8_t reg, uint8_t *data, size_t size) {
  return readRegisters(0x39, reg, data, size);
}

void printGesture(SwipeDecoder::Direction gesture) {
  if (gesture == SwipeDecoder::Direction::Left || gesture == SwipeDecoder::Direction::Right) ++attackCount;
  if (gesture == SwipeDecoder::Direction::Up) ++magicCount;
  if (streamMode) return;
  switch (gesture) {
    case SwipeDecoder::Direction::Left: Serial.println("GESTO ESQUERDA -> ESPADA"); break;
    case SwipeDecoder::Direction::Right: Serial.println("GESTO DIREITA -> ESPADA"); break;
    case SwipeDecoder::Direction::Up: Serial.println("GESTO CIMA -> MAGIA"); break;
    case SwipeDecoder::Direction::Down: Serial.println("GESTO BAIXO -> SEM ACAO (cura sera automatica)"); break;
    default: break;
  }
}

void readGestures(uint32_t now) {
  if (!gesturesOk || uint32_t(now - lastGestureAt) < 20) return;
  lastGestureAt = now;
  uint8_t status = 0, level = 0;
  bool ok = readApdsBytes(APDS9960_GSTATUS, &status, 1)
      && readApdsBytes(APDS9960_GFLVL, &level, 1);
  // FIFO overflow discards an incomplete gesture instead of inventing a swipe.
  if (ok && ((status & 2) || level > 32)) {
    const bool stopped = gestures.disableGestureSensor();
    bool drained = stopped;
    uint8_t discarded[64];
    uint8_t remaining = level > 32 ? 32 : level;
    // At most two bounded reads while the gesture engine is disabled.
    for (uint8_t chunk = 0; drained && remaining > 0 && chunk < 2; ++chunk) {
      const uint8_t count = remaining > 16 ? 16 : remaining;
      drained = readApdsBytes(APDS9960_GFIFO_U, discarded, size_t(count) * 4);
      remaining -= count;
    }
    gesturesOk = drained && gestures.enableGestureSensor(false);
    swipe.reset();
    if (!streamMode) Serial.println(gesturesOk ? "GESTO: fila reiniciada; tente um movimento mais lento."
        : "[FALHA] Nao foi possivel reiniciar a fila do APDS.");
    return;
  }
  uint8_t samples[64];
  const uint8_t count = level > 16 ? 16 : level;
  if (ok && count > 0) ok = readApdsBytes(APDS9960_GFIFO_U, samples, size_t(count) * 4);
  if (!ok) {
    if (!gestureReadFailed && !streamMode) Serial.println("[FALHA] Leitura do APDS-9960; inclinacao e botoes continuam ativos.");
    gestureReadFailed = true;
    swipe.reset();
    return;
  }
  gestureReadFailed = false;
  for (uint8_t i = 0; i < count; ++i)
    swipe.feed(samples[i * 4], samples[i * 4 + 1], samples[i * 4 + 2], samples[i * 4 + 3], now);
  printGesture(swipe.poll(level > 0 || (status & 1), now));
  if (swipe.takeTimeout() && !streamMode) Serial.println("GESTO: movimento longo ou mao parada. Afaste a mao para tentar novamente.");
}

void readSerialCommand() {
  for (uint8_t i = 0; i < 32 && Serial.available(); ++i) {
    char c = char(Serial.read());
    if (c == '\n') {
      serialCommand[serialCommandLength] = '\0';
      if (strcmp(serialCommand, "MB_START") == 0) streamMode = true;
      if (strcmp(serialCommand, "MB_STOP") == 0) { streamMode = false; showStatus(); }
      serialCommandLength = 0;
    } else if (c != '\r') {
      if (serialCommandLength < sizeof(serialCommand) - 1) serialCommand[serialCommandLength++] = c;
      else serialCommandLength = 0;
    }
  }
}

void streamState(uint32_t now) {
  if (!streamMode || uint32_t(now - lastStreamAt) < 20) return;
  lastStreamAt = now;
  const uint8_t flags = (accelerometerOk && tiltReadOk ? 1 : 0)
      | (keypadOk && !keypadReadFailed ? 2 : 0) | (gesturesOk && !gestureReadFailed ? 4 : 0);
  Serial.printf("MB1;%lu;%lu;%.4f;%.4f;%.4f;%u;%lu;%lu;%u\n",
      (unsigned long)bootId, (unsigned long)sequence++, accelerometer.x, accelerometer.y,
      accelerometer.z, stableButtons >> 2, (unsigned long)attackCount, (unsigned long)magicCount, flags);
}

void loop() {
  const uint32_t now = millis();
  readSerialCommand();
  readButtons(now);
  readTilt(now);
  if (!streamMode && now - lastStatusAt >= 8000) {
    lastStatusAt = now;
    showStatus();
  }
  readGestures(now);
  streamState(millis());
  delay(2);
}
