package com.wolferdwolf.drop.domain

object ProductPromise {
    const val APP_NAME = "Drop"
    const val TAGLINE = "Turn anything on your phone into the next useful action."

    fun isValidSharedText(value: String?): Boolean = !value.isNullOrBlank()
}
