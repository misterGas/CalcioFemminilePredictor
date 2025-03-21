package com.embeddedproject.calciofemminileitaliano.helpers

class ScorerStanding (val team: String, val shirt: Int) {

    override fun equals(other: Any?): Boolean {
        val otherObject = other as ScorerStanding
        return team == otherObject.team && shirt == otherObject.shirt
    }

    override fun hashCode(): Int {
        var result = team.hashCode()
        result = 31 * result + shirt
        return result
    }
}