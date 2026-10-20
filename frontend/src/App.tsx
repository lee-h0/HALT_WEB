import { Outlet } from 'react-router-dom'
import Header from './components/Header'

/** 상단 메뉴가 있는 일반 화면의 공통 틀 */
export default function App() {
  return (
    <>
      <Header />
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
