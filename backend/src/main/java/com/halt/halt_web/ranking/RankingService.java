package com.halt.halt_web.ranking;

import com.halt.halt_web.domain.User;
import com.halt.halt_web.domain.UserRole;
import com.halt.halt_web.ranking.dto.RankingResponse;
import com.halt.halt_web.ranking.dto.RankingResponse.MyRank;
import com.halt.halt_web.ranking.dto.RankingResponse.RankEntry;
import com.halt.halt_web.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * 랭킹.
 * - 대상: 일반 회원(USER)만. 관리자는 빠진다. 값이 0인 회원은 순위에 넣지 않는다.
 * - 순위 방식: 동점이면 같은 등수, 다음 등수는 그만큼 건너뛴다 (예: 1, 2, 2, 4).
 */
@Service
public class RankingService {

    private static final UserRole RANKED_ROLE = UserRole.USER;

    private final UserRepository userRepository;

    public RankingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** @param userId 로그인한 회원 번호. 로그인하지 않았으면 null */
    @Transactional(readOnly = true)
    public RankingResponse getRanking(Long userId) {
        List<User> byPoints = userRepository
                .findTop10ByRoleAndTotalPointsGreaterThanOrderByTotalPointsDescSolvedCountDescUserIdAsc(RANKED_ROLE, 0);
        List<User> bySolved = userRepository
                .findTop10ByRoleAndSolvedCountGreaterThanOrderBySolvedCountDescTotalPointsDescUserIdAsc(RANKED_ROLE, 0);

        long pointsRanked = userRepository.countByRoleAndTotalPointsGreaterThan(RANKED_ROLE, 0);
        long solvedRanked = userRepository.countByRoleAndSolvedCountGreaterThan(RANKED_ROLE, 0);

        MyRank me = userId == null ? null : userRepository.findById(userId).map(this::myRank).orElse(null);

        return new RankingResponse(
                toEntries(byPoints, User::getTotalPoints, userId),
                toEntries(bySolved, User::getSolvedCount, userId),
                pointsRanked, solvedRanked, me);
    }

    /** 정렬된 목록에 등수를 붙인다. 앞사람과 값이 같으면 같은 등수 */
    private static List<RankEntry> toEntries(List<User> users, ToIntFunction<User> value, Long userId) {
        List<RankEntry> entries = new ArrayList<>();
        int rank = 0;
        Integer previous = null;
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            int current = value.applyAsInt(user);
            if (previous == null || current != previous) {
                rank = i + 1;
            }
            previous = current;
            entries.add(new RankEntry(rank, user.getNickname(), user.getTotalPoints(), user.getSolvedCount(),
                    Objects.equals(user.getUserId(), userId)));
        }
        return entries;
    }

    private MyRank myRank(User user) {
        boolean ranked = user.getRole() == RANKED_ROLE;
        Integer pointsRank = ranked && user.getTotalPoints() > 0
                ? (int) userRepository.countByRoleAndTotalPointsGreaterThan(RANKED_ROLE, user.getTotalPoints()) + 1
                : null;
        Integer solvedRank = ranked && user.getSolvedCount() > 0
                ? (int) userRepository.countByRoleAndSolvedCountGreaterThan(RANKED_ROLE, user.getSolvedCount()) + 1
                : null;
        return new MyRank(user.getNickname(), user.getTotalPoints(), pointsRank, user.getSolvedCount(), solvedRank);
    }
}
