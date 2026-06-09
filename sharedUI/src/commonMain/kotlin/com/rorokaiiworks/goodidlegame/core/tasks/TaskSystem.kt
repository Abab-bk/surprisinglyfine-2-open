@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import kotlin.uuid.ExperimentalUuidApi

class TaskSystem : IPersistable {
    var currentSession: TaskSession? by mutableStateOf(null)
        private set
    var passiveSessions: SnapshotStateList<TaskSession> = mutableStateListOf()

    private fun stopCurrentSession() {
        currentSession?.stop()
        currentSession = null
    }

    fun tick(currentMills: Long) {
        if (currentSession?.tick(currentMills) == false) {
            stopCurrentSession()
        }

        passiveSessions.removeAll {
            val shouldStop = !it.tick(currentMills)
            if (shouldStop) {
                it.stop()
            }
            shouldStop
        }
    }

    fun archaeologyIsRunning(): Boolean {
        return passiveSessions.any {
            it.task.action.skillId == "skill_archaeology"
        }
    }

    fun getArchaeologySession(): TaskSession? {
        return passiveSessions.find {
            it.task.action.skillId == "skill_archaeology"
        }
    }

    fun startSession(session: TaskSession, currentMills: Long) {
        if (session.isPassive) {
            passiveSessions.add(session)
            session.start(currentMills)
        } else {
            stopCurrentSession()
            currentSession = session
            session.start(currentMills)
        }
    }

    fun stopTaskBySkillActionId(skillActionId: String) {
        val passiveSessionToStop = passiveSessions.find {
            it.task.action.id == skillActionId
        }

        if (passiveSessionToStop != null) {
            passiveSessionToStop.stop()
            passiveSessions.remove(passiveSessionToStop)
            return
        }

        if (currentSession?.task?.action?.id == skillActionId) {
            stopCurrentSession()
        }
    }

    fun findSessionBySkillActionId(skillActionId: String): TaskSession? {
        if (currentSession?.task?.action?.id == skillActionId) return currentSession

        return passiveSessions.find {
            it.task.action.id == skillActionId
        }
    }

    fun findSessionBySkillId(skillId: String): TaskSession? {
        if (currentSession?.task?.action?.skillId == skillId) return currentSession

        return passiveSessions.find {
            it.task.action.skillId == skillId
        }
    }

    fun skillActionIsRunning(skillAction: SkillAction?): Boolean {
        if (skillAction == null) return false
        passiveSessions.forEach {
            if (it.task.action.id == skillAction.id) return true
        }
        return currentSession?.task?.action?.id == skillAction.id
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.taskSessionSave = currentSession?.task?.action?.id?.let {
            TaskSessionSave(
                skillActionId = it,
                passiveSkillActionIds = passiveSessions.map { session -> session.task.action.id }
            )
        }
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
    }
}
