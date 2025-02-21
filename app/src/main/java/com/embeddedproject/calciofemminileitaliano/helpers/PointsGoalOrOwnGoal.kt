package com.embeddedproject.calciofemminileitaliano.helpers

class PointsGoalOrOwnGoal(val goalType: String, val shirt: Int, val team: String) {

    override fun equals(other: Any?): Boolean {
        val otherObject = other as PointsGoalOrOwnGoal
        return goalType == otherObject.goalType && shirt == otherObject.shirt && team == otherObject.team
    }

    override fun hashCode(): Int {
        var result = goalType.hashCode()
        result = 31 * result + shirt
        result = 31 * result + team.hashCode()
        return result
    }
}