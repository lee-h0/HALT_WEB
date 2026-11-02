/** 백엔드 GET /api/ranking 응답 */
export interface RankEntry {
  /** 동점이면 같은 등수 (1, 2, 2, 4) */
  rank: number
  nickname: string
  totalPoints: number
  solvedCount: number
  /** 로그인한 내 줄인지 */
  me: boolean
}

export interface MyRank {
  nickname: string
  totalPoints: number
  /** 포인트가 0이면 아직 순위 없음(null) */
  pointsRank: number | null
  solvedCount: number
  solvedRank: number | null
}

export interface RankingResponse {
  points: RankEntry[]
  solved: RankEntry[]
  /** 포인트 랭킹 대상 인원 */
  pointsRanked: number
  /** 풀이 수 랭킹 대상 인원 */
  solvedRanked: number
  /** 로그인하지 않았으면 null */
  me: MyRank | null
}
