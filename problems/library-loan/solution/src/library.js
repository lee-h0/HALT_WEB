// 모범 답안. 서버는 solution 폴더를 읽지 않는다 (팀원이 테스트를 검증할 때만 쓴다).
const LOAN_DAYS = 14
const MAX_LOANS = 3
const FEE_PER_DAY = 100
const DAY_MS = 24 * 60 * 60 * 1000

/** 'YYYY-MM-DD' -> UTC 자정 기준 밀리초 (시간대 영향 없음) */
function toTime(date) {
  const [y, m, d] = date.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

function toDate(time) {
  return new Date(time).toISOString().slice(0, 10)
}

class Library {
  constructor() {
    this.books = new Map() // isbn -> { title, price }
    this.loans = new Map() // isbn -> { memberId, dueDate }
  }

  addBook(isbn, title, price) {
    this.books.set(isbn, { title, price })
  }

  borrow(memberId, isbn, date) {
    if (!this.books.has(isbn)) throw new Error('BOOK_NOT_FOUND')
    if (this.loans.has(isbn)) throw new Error('ALREADY_BORROWED')

    const mine = [...this.loans.values()].filter((loan) => loan.memberId === memberId)
    if (mine.some((loan) => toTime(date) > toTime(loan.dueDate))) throw new Error('HAS_OVERDUE')
    if (mine.length >= MAX_LOANS) throw new Error('LOAN_LIMIT')

    this.loans.set(isbn, { memberId, dueDate: toDate(toTime(date) + LOAN_DAYS * DAY_MS) })
  }

  returnBook(memberId, isbn, date) {
    const loan = this.loans.get(isbn)
    if (!loan || loan.memberId !== memberId) throw new Error('NOT_BORROWED')

    this.loans.delete(isbn)
    const lateDays = Math.max(0, Math.round((toTime(date) - toTime(loan.dueDate)) / DAY_MS))
    return Math.min(lateDays * FEE_PER_DAY, this.books.get(isbn).price)
  }

  getLoans(memberId) {
    return [...this.loans.entries()]
      .filter(([, loan]) => loan.memberId === memberId)
      .map(([isbn, loan]) => ({ isbn, title: this.books.get(isbn).title, dueDate: loan.dueDate }))
      .sort((a, b) => toTime(a.dueDate) - toTime(b.dueDate))
  }
}

module.exports = { Library }
