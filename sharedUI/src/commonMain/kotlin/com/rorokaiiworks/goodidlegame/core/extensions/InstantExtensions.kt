@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.extensions

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun Instant.truncateToSeconds(): Instant =
    Instant.fromEpochSeconds(this.epochSeconds)
