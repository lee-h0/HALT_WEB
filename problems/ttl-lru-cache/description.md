API 응답을 잠깐 저장해 두는 캐시를 만드세요. 캐시는 정해진 개수만 저장하고, 각 항목은 정해진 시간이 지나면 만료됩니다.

할 일: `src/cache.js`의 `TtlLruCache` 클래스를 구현하세요.

생성자: `new TtlLruCache({ capacity, ttlMs, now })`
- `capacity`: 최대 저장 개수
- `ttlMs`: 기본 만료 시간(밀리초)
- `now`: 현재 시각(밀리초)을 돌려주는 함수. 없으면 `Date.now`를 씁니다. 테스트는 이 함수로 시간을 조절합니다.

메서드
- `set(key, value, ttlMs?)`: 값을 저장합니다. `ttlMs`를 주면 이 항목만 그 시간으로 만료됩니다.
- `get(key)`: 값을 돌려줍니다. 없거나 만료되었으면 `undefined`입니다.
- `has(key)`: 만료되지 않은 값이 있으면 `true`입니다.
- `delete(key)`: 항목을 지웁니다. 지웠으면 `true`, 없었으면 `false`입니다.
- `size` (getter): 저장된 항목 수입니다.

가득 찬 상태에서 새 키를 저장하면, 가장 오래 사용하지 않은 항목(LRU)을 지웁니다.

기술 메모: 테스트는 `node --test` 로 실행합니다. 외부 라이브러리는 쓰지 않습니다.
