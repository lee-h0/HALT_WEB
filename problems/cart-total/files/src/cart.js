/**
 * 장바구니 결제 금액을 계산한다.
 *
 * @param {{ name: string, price: number, quantity: number }[]} items
 * @param {{ type: 'percent' | 'fixed', value: number } | null} coupon
 * @returns {number} 결제 금액 (원 단위 정수)
 */
function calculateTotal(items, coupon) {
  throw new Error('아직 구현되지 않았습니다.')
}

module.exports = { calculateTotal }
