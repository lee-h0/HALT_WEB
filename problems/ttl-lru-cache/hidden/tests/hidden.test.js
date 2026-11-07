const test = require('node:test')
const assert = require('node:assert/strict')
const { TtlLruCache } = require('../src/cache.js')

function fakeClock() {
  const clock = { time: 0 }
  clock.now = () => clock.time
  return clock
}

function newCache(capacity = 2, ttlMs = 1000) {
  const clock = fakeClock()
  return { clock, cache: new TtlLruCache({ capacity, ttlMs, now: clock.now }) }
}

test('잘못된 capacity, ttlMs는 RangeError', () => {
  assert.throws(() => new TtlLruCache({ capacity: 0, ttlMs: 1000 }), RangeError)
  assert.throws(() => new TtlLruCache({ capacity: 1.5, ttlMs: 1000 }), RangeError)
  assert.throws(() => new TtlLruCache({ capacity: 2, ttlMs: 0 }), RangeError)
  assert.throws(() => new TtlLruCache({ capacity: 2, ttlMs: -1 }), RangeError)
  const { cache } = newCache()
  assert.throws(() => cache.set('a', 1, 0), RangeError)
})

test('get으로 읽은 항목은 최근 사용이 되어 지워지지 않는다', () => {
  const { cache } = newCache()
  cache.set('a', 1)
  cache.set('b', 2)
  cache.get('a')
  cache.set('c', 3)
  assert.equal(cache.get('a'), 1)
  assert.equal(cache.has('b'), false)
})

test('has는 사용 순서를 바꾸지 않는다', () => {
  const { cache } = newCache()
  cache.set('a', 1)
  cache.set('b', 2)
  cache.has('a')
  cache.set('c', 3)
  assert.equal(cache.has('a'), false)
  assert.equal(cache.get('b'), 2)
})

test('있는 키를 다시 set하면 값, 만료 시각, 순서가 새로 정해진다', () => {
  const { clock, cache } = newCache()
  cache.set('a', 1)
  cache.set('b', 2)
  clock.time = 800
  cache.set('a', 10)
  assert.equal(cache.size, 2)
  cache.set('c', 3)
  assert.equal(cache.has('b'), false)
  clock.time = 1500
  assert.equal(cache.get('a'), 10)
})

test('만료 시각과 현재 시각이 같으면 만료다', () => {
  const { clock, cache } = newCache()
  cache.set('a', 1)
  clock.time = 1000
  assert.equal(cache.get('a'), undefined)
  assert.equal(cache.has('a'), false)
})

test('항목별 ttlMs를 따른다', () => {
  const { clock, cache } = newCache()
  cache.set('short', 1, 100)
  cache.set('long', 2)
  clock.time = 100
  assert.equal(cache.get('short'), undefined)
  assert.equal(cache.get('long'), 2)
})

test('가득 찼을 때 만료된 항목을 LRU보다 먼저 지운다', () => {
  const { clock, cache } = newCache()
  cache.set('a', 1)
  cache.set('b', 2, 100)
  clock.time = 200
  cache.set('c', 3)
  assert.equal(cache.get('a'), 1)
  assert.equal(cache.get('c'), 3)
})

test('size는 만료된 항목을 세지 않는다', () => {
  const { clock, cache } = newCache(3)
  cache.set('a', 1, 100)
  cache.set('b', 2)
  cache.set('c', 3)
  clock.time = 150
  assert.equal(cache.size, 2)
})

test('숫자, 객체 키를 쓸 수 있고 1과 "1"은 다른 키다', () => {
  const { cache } = newCache(3)
  const objectKey = { id: 1 }
  cache.set(1, 'number')
  cache.set('1', 'string')
  cache.set(objectKey, 'object')
  assert.equal(cache.get(1), 'number')
  assert.equal(cache.get('1'), 'string')
  assert.equal(cache.get(objectKey), 'object')
  assert.equal(cache.get({ id: 1 }), undefined)
})
