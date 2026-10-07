import { Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import ProtectedRoute from './components/ProtectedRoute'
import MainPage from './pages/MainPage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import PlaceholderPage from './pages/PlaceholderPage'

export default function App() {
  return (
    <>
      <Header />
      <main className="container">
        <Routes>
          <Route path="/" element={<MainPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* 로그인해야 들어갈 수 있는 화면. 아직 만들지 않은 화면은 임시 페이지를 보여준다 */}
          <Route
            path="/mypage"
            element={
              <ProtectedRoute>
                <PlaceholderPage title="마이페이지" />
              </ProtectedRoute>
            }
          />
          <Route
            path="/problems"
            element={
              <ProtectedRoute>
                <PlaceholderPage title="문제풀이" />
              </ProtectedRoute>
            }
          />
          <Route path="/ranking" element={<PlaceholderPage title="랭킹" />} />
          <Route path="*" element={<PlaceholderPage title="페이지를 찾을 수 없습니다" />} />
        </Routes>
      </main>
    </>
  )
}
