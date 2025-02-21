package com.embeddedproject.calciofemminileitaliano.helpers

class UserTotalPoints (private val userNickname: String, private val totalPoints: Int, private val position: Int = 0) {

    fun getUserNickname() = userNickname

    fun getTotalPoints() = totalPoints

    fun getPosition() = position
}