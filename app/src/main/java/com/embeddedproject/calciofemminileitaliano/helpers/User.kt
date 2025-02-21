package com.embeddedproject.calciofemminileitaliano.helpers


class User(private val firstName: String, private val lastName: String, private val nickname: String, private val email: String) {

    fun getFirstName() = firstName

    fun getLastName() = lastName

    fun getNickname() = nickname

    fun getEmail() = email
}