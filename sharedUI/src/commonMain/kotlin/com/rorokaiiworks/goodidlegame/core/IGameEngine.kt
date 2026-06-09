package com.rorokaiiworks.goodidlegame.core

interface IGameEngine {
    fun start()
    fun stop()
    fun tick1(delta: Float, timeProvider: ITimeProvider)
    fun tick2(delta: Float, timeProvider: ITimeProvider)
}