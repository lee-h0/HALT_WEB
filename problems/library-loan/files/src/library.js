/**
 * 도서 대출 관리.
 * 날짜는 모두 'YYYY-MM-DD' 형식의 문자열이다.
 */
class Library {
  constructor() {
    // TODO
  }

  /**
   * @param {string} isbn
   * @param {string} title
   * @param {number} price 책 가격 (원)
   */
  addBook(isbn, title, price) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /**
   * @param {string} memberId
   * @param {string} isbn
   * @param {string} date 대출일
   */
  borrow(memberId, isbn, date) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /**
   * @param {string} memberId
   * @param {string} isbn
   * @param {string} date 반납일
   * @returns {number} 연체료 (원)
   */
  returnBook(memberId, isbn, date) {
    throw new Error('아직 구현되지 않았습니다.')
  }

  /**
   * @param {string} memberId
   * @returns {{ isbn: string, title: string, dueDate: string }[]}
   */
  getLoans(memberId) {
    throw new Error('아직 구현되지 않았습니다.')
  }
}

module.exports = { Library }
