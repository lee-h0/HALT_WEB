const test = require('node:test')
const assert = require('node:assert/strict')
const { Library } = require('../src/library.js')

function setup() {
  const library = new Library()
  library.addBook('111', '클린 코드', 30000)
  library.addBook('222', '리팩터링', 35000)
  return library
}

test('빌린 책은 대출 목록에 반납 예정일과 함께 나온다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  assert.deepEqual(library.getLoans('kim'), [{ isbn: '111', title: '클린 코드', dueDate: '2026-11-15' }])
})

test('기간 안에 반납하면 연체료가 없다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  assert.equal(library.returnBook('kim', '111', '2026-11-10'), 0)
  assert.deepEqual(library.getLoans('kim'), [])
})

test('늦게 반납하면 하루에 100원씩 연체료를 낸다', () => {
  const library = setup()
  library.borrow('kim', '222', '2026-11-01')
  assert.equal(library.returnBook('kim', '222', '2026-11-18'), 300)
})

test('반납한 책은 다른 회원이 빌릴 수 있다', () => {
  const library = setup()
  library.borrow('kim', '111', '2026-11-01')
  library.returnBook('kim', '111', '2026-11-02')
  library.borrow('lee', '111', '2026-11-03')
  assert.equal(library.getLoans('lee').length, 1)
})
