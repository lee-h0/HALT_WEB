const test = require('node:test')
const assert = require('node:assert/strict')
const { Library } = require('../src/library.js')

function setup() {
  const library = new Library()
  library.addBook('111', '클린 코드', 30000)
  library.addBook('222', '리팩터링', 35000)
  library.addBook('333', '오브젝트', 28000)
  library.addBook('444', '테스트 주도 개발', 500)
  library.addBook('555', '실용주의 프로그래머', 25000)
  return library
}

test('등록되지 않은 책을 빌리면 BOOK_NOT_FOUND', () => {
  const library = setup()
  assert.throws(() => library.borrow('kim', '999', '2026-11-01'), { message: 'BOOK_NOT_FOUND' })
})

test('다른 사람이 빌린 책을 빌리면 ALREADY_BORROWED', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  assert.throws(() => library.borrow('lee', '111', '2026-11-02'), { message: 'ALREADY_BORROWED' })
})

test('한 회원은 최대 3권까지 빌린다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  library.borrow('kim', '222', '2026-11-01')
  library.borrow('kim', '333', '2026-11-01')
  assert.throws(() => library.borrow('kim', '555', '2026-11-02'), { message: 'LOAN_LIMIT' })
})

test('연체 중인 책이 있으면 새로 빌릴 수 없다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  // 반납 예정일 2026-11-15 가 지났다
  assert.throws(() => library.borrow('kim', '222', '2026-11-16'), { message: 'HAS_OVERDUE' })
})

test('반납 예정일 당일까지는 연체가 아니다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  library.borrow('kim', '222', '2026-11-15')
  assert.equal(library.returnBook('kim', '111', '2026-11-15'), 0)
})

test('반납 예정일은 월과 연도가 바뀌어도 정확하다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-12-25')
  library.borrow('lee', '222', '2027-02-20')
  assert.equal(library.getLoans('kim')[0].dueDate, '2027-01-08')
  assert.equal(library.getLoans('lee')[0].dueDate, '2027-03-06')
  assert.equal(library.returnBook('kim', '111', '2027-01-10'), 200)
})

test('연체료는 책 가격을 넘지 않는다', () => {
  const library = setup()
  library.borrow('kim', '444', '2026-11-01')
  assert.equal(library.returnBook('kim', '444', '2026-12-31'), 500)
})

test('자기가 빌리지 않은 책을 반납하면 NOT_BORROWED', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  assert.throws(() => library.returnBook('lee', '111', '2026-11-02'), { message: 'NOT_BORROWED' })
  assert.throws(() => library.returnBook('kim', '222', '2026-11-02'), { message: 'NOT_BORROWED' })
})

test('대출 목록은 반납 예정일이 빠른 순이다', () => {
  const library = setup()
  library.borrow('kim', '222', '2026-11-05')
  library.borrow('kim', '111', '2026-11-01')
  library.borrow('kim', '333', '2026-11-03')
  assert.deepEqual(
    library.getLoans('kim').map((loan) => loan.isbn),
    ['111', '333', '222'],
  )
})

test('돌려받은 대출 목록을 바꿔도 기록은 그대로다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  const loans = library.getLoans('kim')
  loans[0].dueDate = '2099-01-01'
  loans.push({ isbn: '222', title: '가짜', dueDate: '2026-11-20' })
  assert.deepEqual(library.getLoans('kim'), [{ isbn: '111', title: '클린 코드', dueDate: '2026-11-15' }])
})
