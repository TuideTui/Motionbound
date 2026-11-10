#pragma once
#include <stdint.h>

// Cooperative decoder for APDS-9960 U/D/L/R FIFO samples. It never waits for a
// hand to leave: the main loop keeps polling the other devices between samples.
// Sensor ratios and sign conventions follow the SparkFun APDS-9960 reference.
class SwipeDecoder {
 public:
  enum class Direction { None, Up, Down, Left, Right };
  static constexpr uint32_t QUIET_MS = 120;
  static constexpr uint32_t MAX_GESTURE_MS = 1600;

  void feed(uint8_t up, uint8_t down, uint8_t left, uint8_t right, uint32_t now) {
    if (waitingRelease_ || up <= 10 || down <= 10 || left <= 10 || right <= 10) return;
    if (!capturing_) {
      capturing_ = true;
      startedAt_ = now;
      samples_ = 0;
      firstUD_ = firstLR_ = 0;
      for (uint8_t i = 0; i < 3; ++i) lastUD_[i] = lastLR_[i] = 0;
    }
    const int ud = (int(up) - int(down)) * 100 / (int(up) + int(down));
    const int lr = (int(left) - int(right)) * 100 / (int(left) + int(right));
    if (samples_ < 3) { firstUD_ += ud; firstLR_ += lr; }
    lastUD_[samples_ % 3] = ud;
    lastLR_[samples_ % 3] = lr;
    if (samples_ < 65530) ++samples_;
    lastActivityAt_ = now;
  }

  Direction poll(bool active, uint32_t now) {
    if (active) lastActivityAt_ = now;
    if (waitingRelease_) {
      if (!active && uint32_t(now - lastActivityAt_) >= QUIET_MS) waitingRelease_ = false;
      return Direction::None;
    }
    if (!capturing_) return Direction::None;
    if (uint32_t(now - startedAt_) >= MAX_GESTURE_MS) {
      reset(); waitingRelease_ = true; lastActivityAt_ = now; timedOut_ = true;
      return Direction::None;
    }
    if (active || uint32_t(now - lastActivityAt_) < QUIET_MS) return Direction::None;
    Direction result = Direction::None;
    if (samples_ >= 6) {
      const int ud = (lastUD_[0] + lastUD_[1] + lastUD_[2] - firstUD_) / 3;
      const int lr = (lastLR_[0] + lastLR_[1] + lastLR_[2] - firstLR_) / 3;
      const int absUD = ud < 0 ? -ud : ud, absLR = lr < 0 ? -lr : lr;
      if (absUD >= 40 && absUD > absLR) result = ud < 0 ? Direction::Up : Direction::Down;
      else if (absLR >= 40) result = lr < 0 ? Direction::Left : Direction::Right;
    }
    reset();
    return result;
  }

  bool takeTimeout() { const bool value = timedOut_; timedOut_ = false; return value; }
  void reset() { capturing_ = waitingRelease_ = timedOut_ = false; samples_ = 0; }

 private:
  bool capturing_ = false, waitingRelease_ = false, timedOut_ = false;
  uint16_t samples_ = 0;
  uint32_t startedAt_ = 0, lastActivityAt_ = 0;
  int firstUD_ = 0, firstLR_ = 0;
  int lastUD_[3] = {}, lastLR_[3] = {};
};
