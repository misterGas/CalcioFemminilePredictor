package com.embeddedproject.calciofemminileitaliano.helpers

class TeamEventInfo(private val teamName: String, private val isPrivate: Boolean = false, private val password: String? = null, private val passwordSalt: String? = null) {

    fun getTeamName() = teamName

    fun getIsPrivate() = isPrivate

    fun getPassword() = password

    fun getPasswordSalt() = passwordSalt
}