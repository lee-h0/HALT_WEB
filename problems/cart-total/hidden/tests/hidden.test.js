const test = require('node:test')
const assert = require('node:assert/strict')
const { calculateTotal } = require('../src/cart.js')

test('수량이 1 미만인 상품은 제외한다', () => {
  const items = [
    { name: '정상', price: 1000, quantity: 2 },
    { name: '수량 0', price: 5000, quantity: 0 },
    { name: '음수', price: 5000, quantity: -1 },
  ]
  assert.equal(calculateTotal(items, null), 2000)
})

test('수량이 정수가 아닌 상품은 제외한다', () => {
  const items = [
    { name: '정상', price: 1000, quantity: 1 },
    { name: '소수', price: 4000, quantity: 1.5 },
  ]
  assert.equal(calculateTotal(items, null), 1000)
})

test('정률 쿠폰의 할인액은 최대 10,000원이다', () => {
  const items = [{ name: '모니터', price: 200000, quantity: 1 }]
  assert.equal(calculateTotal(items, { type: 'percent', value: 10 }), 190000)
})

test('할인액은 원 단위로 내림한다', () => {
  const items = [{ name: '지우개', price: 999, quantity: 1 }]
  // 할인액 149.85원 -> 149원
  assert.equal(calculateTotal(items, { type: 'percent', value: 15 }), 850)
})

test('결제 금액은 0원 미만이 될 수 없다', () => {
  const items = [{ name: '사탕', price: 500, quantity: 1 }]
  assert.equal(calculateTotal(items, { type: 'fixed', value: 3000 }), 0)
})

test('입력 배열과 객체를 변경하지 않는다', () => {
  const items = [
    { name: '노트', price: 2000, quantity: 3 },
    { name: '수량 0', price: 5000, quantity: 0 },
  ]
  const before = JSON.stringify(items)
  items.forEach((item) => Object.freeze(item))
  Object.freeze(items)

  assert.equal(calculateTotal(items, { type: 'percent', value: 10 }), 5400)
  assert.equal(JSON.stringify(items), before)
})
