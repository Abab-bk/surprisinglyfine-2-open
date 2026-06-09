package com.rorokaiiworks.goodidlegame.core

import java.text.DecimalFormat

private val decimal = DecimalFormat("#.##")

fun Float.prettyPrint(): String {
    return decimal.format(this)
}
