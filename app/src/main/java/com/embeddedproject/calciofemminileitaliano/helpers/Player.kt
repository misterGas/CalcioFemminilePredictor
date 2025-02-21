package com.embeddedproject.calciofemminileitaliano.helpers

class Player(val firstName: String, val lastName: String, val shirtNumber: Int, val role: String, val team: String) {

    override fun equals(other: Any?): Boolean {
        val otherObject = other as Player
        return firstName == otherObject.firstName && lastName == otherObject.lastName && shirtNumber == otherObject.shirtNumber && role == otherObject.role && team == otherObject.team
    }

    override fun hashCode(): Int {
        var result = firstName.hashCode()
        result = 31 * result + lastName.hashCode()
        result = 31 * result + shirtNumber
        result = 31 * result + role.hashCode()
        result = 31 * result + team.hashCode()
        return result
    }
}