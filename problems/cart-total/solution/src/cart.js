// 모범 답안. 서버는 solution 폴더를 읽지 않는다 (팀원이 테스트를 검증할 때만 쓴다).
const MAX_PERCENT_DISCOUNT = 10000

function calculateTotal(items, coupon) {
  const subtotal = items
    .filter((item) => Number.isInteger(item.quantity) && item.quantity >= 1)
    .reduce((sum, item) => sum + item.price * item.quantity, 0)

  let discount = 0
  if (coupon && coupon.type === 'percent') {
    discount = Math.min(Math.floor((subtotal * coupon.value) / 100), MAX_PERCENT_DISCOUNT)
  } else if (coupon && coupon.type === 'fixed') {
    discount = Math.floor(coupon.value)
  }

  return Math.max(subtotal - discount, 0)
}

module.exports = { calculateTotal }
