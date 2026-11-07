import { createBrowserRouter } from 'react-router-dom'
import App from './App'
import ProtectedRoute from './components/ProtectedRoute'
import MainPage from './pages/MainPage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import PlaceholderPage from './pages/PlaceholderPage'
import ProblemListPage from './pages/ProblemListPage'
import ProblemDetailPage from './pages/ProblemDetailPage'
import SolvePage from './pages/SolvePage'
import RankingPage from './pages/RankingPage'
import MyPage from './pages/MyPage'
import SolutionListPage from './pages/SolutionListPage'
import SolutionDetailPage from './pages/SolutionDetailPage'
import FeedbackPage from './pages/FeedbackPage'

/**
 * 화면 주소 목록.
 * 문제 풀이 화면은 페이지 이탈 경고(useBlocker)가 필요해서 createBrowserRouter 방식을 쓴다.
 */
export const router = createBrowserRouter([
  {
    element: <App />,
    children: [
      { path: '/', element: <MainPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/signup', element: <SignupPage /> },
      // 랭킹은 로그인하지 않아도 볼 수 있다 (로그인하면 내 순위도 표시)
      { path: '/ranking', element: <RankingPage /> },
      {
        path: '/mypage',
        element: (
          <ProtectedRoute>
            <MyPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/problems',
        element: (
          <ProtectedRoute>
            <ProblemListPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/problems/:problemId',
        element: (
          <ProtectedRoute>
            <ProblemDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/problems/:problemId/solutions',
        element: (
          <ProtectedRoute>
            <SolutionListPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/solutions/:attemptId',
        element: (
          <ProtectedRoute>
            <SolutionDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/attempts/:attemptId/feedback',
        element: (
          <ProtectedRoute>
            <FeedbackPage />
          </ProtectedRoute>
        ),
      },
      { path: '*', element: <PlaceholderPage title="페이지를 찾을 수 없습니다" /> },
    ],
  },
  // 풀이 화면은 상단 메뉴 없이 화면 전체를 쓴다
  {
    path: '/problems/:problemId/solve',
    element: (
      <ProtectedRoute>
        <SolvePage />
      </ProtectedRoute>
    ),
  },
])
