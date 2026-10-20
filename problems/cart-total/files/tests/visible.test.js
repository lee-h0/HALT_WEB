const test = require('node:test')
const assert = require('node:assert/strict')
const { calculateTotal } = require('../src/cart.js')

test('빈 장바구니는 0원이다', () => {
  assert.equal(calculateTotal([], null), 0)
})

test('가격과 수량을 곱해 더한다', () => {
  const items = [
    { name: '노트', price: 2000, quantity: 3 },
    { name: '펜', price: 1500, quantity: 2 },
  ]
  assert.equal(calculateTotal(items, null), 9000)
})

test('정률 쿠폰을 적용한다', () => {
  const items = [{ name: '키보드', price: 20000, quantity: 1 }]
  assert.equal(calculateTotal(items, { type: 'percent', value: 10 }), 18000)
})

test('정액 쿠폰을 적용한다', () => {
  const items = [{ name: '마우스', price: 15000, quantity: 1 }]
  assert.equal(calculateTotal(items, { type: 'fixed', value: 3000 }), 12000)
})
