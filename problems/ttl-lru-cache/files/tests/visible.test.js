const test = require('node:test')
const assert = require('node:assert/strict')
const { TtlLruCache } = require('../src/cache.js')

/** 테스트용 시계. clock.time을 바꿔 시간을 흐르게 한다 */
function fakeClock() {
  const clock = { time: 0 }
  clock.now = () => clock.time
  return clock
}

test('저장한 값을 읽는다', () => {
  const clock = fakeClock()
  const cache = new TtlLruCache({ capacity: 2, ttlMs: 1000, now: clock.now })
  cache.set('a', 1)
  assert.equal(cache.get('a'), 1)
  assert.equal(cache.get('b'), undefined)
  assert.equal(cache.size, 1)
})

test('가득 차면 가장 오래된 항목을 지운다', () => {
  const clock = fakeClock()
  const cache = new TtlLruCache({ capacity: 2, ttlMs: 1000, now: clock.now })
  cache.set('a', 1)
  cache.set('b', 2)
  cache.set('c', 3)
  assert.equal(cache.has('a'), false)
  assert.equal(cache.get('b'), 2)
  assert.equal(cache.get('c'), 3)
})

test('만료 시간이 지나면 값이 사라진다', () => {
  const clock = fakeClock()
  const cache = new TtlLruCache({ capacity: 2, ttlMs: 1000, now: clock.now })
  cache.set('a', 1)
  clock.time = 500
  assert.equal(cache.get('a'), 1)
  clock.time = 1500
  assert.equal(cache.get('a'), undefined)
})

test('지운 항목은 더 이상 읽을 수 없다', () => {
  const clock = fakeClock()
  const cache = new TtlLruCache({ capacity: 2, ttlMs: 1000, now: clock.now })
  cache.set('a', 1)
  assert.equal(cache.delete('a'), true)
  assert.equal(cache.delete('a'), false)
  assert.equal(cache.get('a'), undefined)
})
