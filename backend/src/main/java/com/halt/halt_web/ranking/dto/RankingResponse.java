package com.halt.halt_web.ranking.dto;

import java.util.List;

/**
 * 랭킹 화면 응답.
 *
 * @param points       포인트 1~10등
 * @param solved       푼 문제 수 1~10등
 * @param pointsRanked 포인트 랭킹 대상 인원 (포인트가 1 이상인 회원)
 * @param solvedRanked 풀이 수 랭킹 대상 인원 (1문제 이상 푼 회원)
 * @param me           내 순위. 로그인하지 않았으면 null
 */
public record RankingResponse(
        List<RankEntry> points,
        List<RankEntry> solved,
        long pointsRanked,
        long solvedRanked,
        MyRank me
) {

    /** 랭킹 한 줄. 회원 번호는 노출하지 않고, 내 줄인지만 알려 준다 */
    public record RankEntry(int rank, String nickname, int totalPoints, int solvedCount, boolean me) {
    }

    /** 내 순위. 아직 0이라 랭킹 대상이 아니면 순위는 null */
    public record MyRank(String nickname, int totalPoints, Integer pointsRank,
                         int solvedCount, Integer solvedRank) {
    }
}
