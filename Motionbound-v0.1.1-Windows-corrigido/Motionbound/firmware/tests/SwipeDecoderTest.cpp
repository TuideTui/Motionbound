#include "../MotionboundDiagnostico/SwipeDecoder.h"
#include <assert.h>
#include <stdio.h>

using Direction = SwipeDecoder::Direction;

Direction gesture(SwipeDecoder &decoder, int du, int dl, uint32_t start) {
  for (int i = 0; i < 12; ++i) {
    const int sign = i < 6 ? 1 : -1;
    const uint32_t now = start + uint32_t(i * 20);
    decoder.feed(100 + du * sign, 100 - du * sign, 100 + dl * sign, 100 - dl * sign, now);
    assert(decoder.poll(true, now) == Direction::None);
  }
  return decoder.poll(false, start + 400);
}

int main() {
  SwipeDecoder decoder;
  assert(gesture(decoder, 60, 0, 0) == Direction::Up);
  assert(gesture(decoder, -60, 0, 500) == Direction::Down);
  assert(gesture(decoder, 0, 60, 1000) == Direction::Left);
  assert(gesture(decoder, 0, -60, 1500) == Direction::Right);
  assert(gesture(decoder, 5, 8, 2000) == Direction::None);
  assert(gesture(decoder, 60, 15, 2500) == Direction::Up);
  assert(gesture(decoder, 10, -60, 3000) == Direction::Right);
  assert(decoder.poll(false, 3500) == Direction::None); // no duplicated event
  for (uint32_t now = 4000; now <= 8000; now += 20) {
    decoder.feed(100, 100, 100, 100, now);
    assert(decoder.poll(true, now) == Direction::None);
  }
  assert(decoder.takeTimeout());
  assert(!decoder.takeTimeout());
  assert(gesture(decoder, 60, 0, 8100) == Direction::None); // still needs release
  decoder.poll(false, 8700);
  assert(gesture(decoder, 60, 0, 9000) == Direction::Up);
  for (int i = 0; i < 12; ++i) decoder.feed(0, 0, 255, 255, uint32_t(10000 + i * 20));
  assert(decoder.poll(false, 10400) == Direction::None);
  assert(gesture(decoder, 60, 0, UINT32_MAX - 100) == Direction::Up);
  decoder.reset();
  decoder.feed(160, 40, 100, 100, 200);
  assert(decoder.poll(false, 400) == Direction::None); // short noise burst
  puts("PASS: four directions, dominant axis, noise rejection, hold timeout, release, one event and millis wrap");
}
