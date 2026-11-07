/**
 * 만료 시간이 있는 LRU 캐시.
 */
class TtlLruCache {
  /**
   * @param {{ capacity: number, ttlMs: number, now?: () => number }} options
   */
  constructor({ capacity, ttlMs, now = Date.now }) {
    // TODO
  }

  /**
   * @param {*} key
   * @param {*} value
   * @param {number} [ttlMs] 이 항목만 쓸 만료 시간 (밀리초)
   */
  set(key, value, ttlMs) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /** @returns {*} 값. 없거나 만료되었으면 undefined */
  get(key) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /** @returns {boolean} */
  has(key) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /** @returns {boolean} 지웠으면 true */
  delete(key) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /** @returns {number} 저장된 항목 수 */
  get size() {
    throw new Error('아직 구현되지 않았습니다.')
  }
}

module.exports = { TtlLruCache }
