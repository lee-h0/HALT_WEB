// 모범 답안. 서버는 solution 폴더를 읽지 않는다 (팀원이 테스트를 검증할 때만 쓴다).
// Map은 넣은 순서를 기억하므로, 사용할 때마다 지웠다 다시 넣으면 맨 앞이 가장 오래 안 쓴 항목이 된다.

function assertPositive(ttlMs) {
  if (typeof ttlMs !== 'number' || !(ttlMs > 0)) throw new RangeError('ttlMs는 0보다 커야 합니다.')
}

class TtlLruCache {
  constructor({ capacity, ttlMs, now = Date.now }) {
    if (!Number.isInteger(capacity) || capacity < 1) throw new RangeError('capacity는 1 이상의 정수여야 합니다.')
    assertPositive(ttlMs)
    this.capacity = capacity
    this.ttlMs = ttlMs
    this.now = now
    this.entries = new Map() // key -> { value, expiresAt }
  }

  isExpired(entry) {
    return entry.expiresAt <= this.now()
  }

  removeExpired() {
    for (const [key, entry] of this.entries) {
      if (this.isExpired(entry)) this.entries.delete(key)
    }
  }

  set(key, value, ttlMs = this.ttlMs) {
    assertPositive(ttlMs)
    const existed = this.entries.delete(key)
    if (!existed && this.entries.size >= this.capacity) {
      this.removeExpired()
      if (this.entries.size >= this.capacity) {
        const oldestKey = this.entries.keys().next().value
        this.entries.delete(oldestKey)
      }
    }
    this.entries.set(key, { value, expiresAt: this.now() + ttlMs })
  }

  get(key) {
    const entry = this.entries.get(key)
    if (!entry) return undefined
    if (this.isExpired(entry)) {
      this.entries.delete(key)
      return undefined
    }
    this.entries.delete(key)
    this.entries.set(key, entry)
    return entry.value
  }

  has(key) {
    const entry = this.entries.get(key)
    return entry !== undefined && !this.isExpired(entry)
  }

  delete(key) {
    return this.entries.delete(key)
  }

  get size() {
    this.removeExpired()
    return this.entries.size
  }
}

module.exports = { TtlLruCache }
