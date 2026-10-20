쇼핑몰 장바구니의 결제 금액을 계산하는 함수를 완성하세요.

할 일: `src/cart.js`의 `calculateTotal(items, coupon)` 을 구현하세요.

입력
- `items`: 상품 배열. 각 상품은 `{ name, price, quantity }` 형태입니다.
- `coupon`: 쿠폰. 없으면 `null` 입니다.
  - 정률 쿠폰: `{ type: 'percent', value: 10 }` (10% 할인)
  - 정액 쿠폰: `{ type: 'fixed', value: 3000 }` (3,000원 할인)

출력
- 결제 금액(원 단위 정수)

기술 메모: 테스트는 `node --test` 로 실행합니다. 외부 라이브러리는 쓰지 않습니다.
