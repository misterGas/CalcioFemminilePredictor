package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SeasonBest11Statistics : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_season_best11_statistics, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = SeasonBest11StatisticsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season

        reference.get().addOnCompleteListener {
            val playersChosenTotalPoints = mutableMapOf<Player,Int>()
            val championshipReference = it.result.child("Championships").child(championship).child(season)
            for (r in championshipReference.child("Matches").children) {
                if (r.hasChild("Best11PredictionsPoints") && r.child("Best11PredictionsPoints").hasChild(user)) {
                    val roundBest11Points = r.child("Best11PredictionsPoints").child(user)
                }
            }
        }
    }
}