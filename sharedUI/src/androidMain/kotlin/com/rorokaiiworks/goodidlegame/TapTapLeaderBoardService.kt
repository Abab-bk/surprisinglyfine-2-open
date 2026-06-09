package com.rorokaiiworks.goodidlegame

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.leaderBoard.ILeaderboardService
import com.rorokaiiworks.goodidlegame.core.leaderBoard.LeaderboardEntry
import com.rorokaiiworks.goodidlegame.ui.leaderBoard.LeaderboardScreenViewModel
import com.taptap.sdk.leaderboard.androidx.TapTapLeaderboard
import com.taptap.sdk.leaderboard.callback.TapTapLeaderboardResponseCallback
import com.taptap.sdk.leaderboard.data.request.LeaderboardCollection
import com.taptap.sdk.leaderboard.data.request.SubmitScoresRequest
import com.taptap.sdk.leaderboard.data.response.LeaderboardScoresResponse
import com.taptap.sdk.leaderboard.data.response.SubmitScoresResponse
import com.taptap.sdk.leaderboard.data.response.UserScoreResponse
import com.taptap.sdk.leaderboard.data.response.common.Score
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.coroutines.resume

fun Score.toLeaderboardEntry(): LeaderboardEntry {
    return LeaderboardEntry(
        rank = rank?.toInt() ?: 0,
        userId = user?.openid ?: "",
        userName = user?.name ?: "",
        score = score ?: 0L
    )
}


class TapTapLeaderBoardService : ILeaderboardService, KoinComponent {
    private val logger: Logger by inject { parametersOf("TapTapLeaderBoardService") }

    private fun convertLeaderBoardId(leaderboardName: String): String {
        return when (leaderboardName) {
            LeaderboardScreenViewModel.DEFAULT_LEADERBOARD -> "ojfylen94zq8mcx6hg"
            else -> leaderboardName
        }
    }

    override suspend fun uploadScore(
        leaderboardName: String,
        score: Int
    ): Resource<Boolean> = suspendCancellableCoroutine { continuation ->
        val leaderboardId = convertLeaderBoardId(leaderboardName)

        TapTapLeaderboard.submitScores(
            scores = listOf(SubmitScoresRequest.ScoreItem(
                leaderboardId = leaderboardId,
                score = score.toLong()
            )),
            callback = object : TapTapLeaderboardResponseCallback<SubmitScoresResponse>() {
                override fun onSuccess(data: SubmitScoresResponse) {
                    logger.d("提交成功: $data")
                    continuation.resume(Resource.Success(true))
                }

                override fun onFailure(code: Int, message: String) {
                    logger.e("提交失败: code=$code, message=$message")
                    continuation.resume(Resource.Error(code = code, message = message))
                }
            }
        )
    }

    override suspend fun fetchTopScores(
        leaderboardName: String,
        count: Int
    ): Resource<List<LeaderboardEntry>> = suspendCancellableCoroutine { continuation ->
        val leaderboardId = convertLeaderBoardId(leaderboardName)

        TapTapLeaderboard.loadLeaderboardScores(
            leaderboardId = leaderboardId,
            leaderboardCollection = LeaderboardCollection.FRIENDS,
            nextPage = null,
            callback = object : TapTapLeaderboardResponseCallback<LeaderboardScoresResponse>() {
                override fun onSuccess(data: LeaderboardScoresResponse) {
                    val scores = data.scores
                    return continuation.resume(
                        Resource.Success(
                            scores.map { it.toLeaderboardEntry() }
                        )
                    )
                }

                override fun onFailure(code: Int, message: String) {
                    logger.e("获取排行榜数据失败: code=$code, message=$message")
                    continuation.resume(Resource.Error(code = code, message = message))
                }
            }
        )
    }

    override suspend fun getPlayerRank(leaderboardName: String): Resource<LeaderboardEntry?> = suspendCancellableCoroutine {
        continuation ->
        val leaderboardId = convertLeaderBoardId(leaderboardName)

        TapTapLeaderboard.loadCurrentPlayerLeaderboardScore(
            leaderboardId = leaderboardId,
            leaderboardCollection = LeaderboardCollection.FRIENDS,
            periodToken = null,
            callback = object : TapTapLeaderboardResponseCallback<UserScoreResponse>() {
                override fun onSuccess(data: UserScoreResponse) {
                    val score = data.currentUserScore
                    logger.d("用户分数: $score")
                    continuation.resume(Resource.Success(score?.toLeaderboardEntry()))
                }

                override fun onFailure(code: Int, message: String) {
                    logger.e("获取用户分数失败: code=$code, message=$message")
                    continuation.resume(Resource.Error(code = code, message = message))
                }
            }
        )
    }
}