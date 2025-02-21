package com.embeddedproject.calciofemminileitaliano

import android.app.AlertDialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper

class PredictionsRules : Fragment() {

    private lateinit var app: MainApplication

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        app = activity?.application as MainApplication
        return inflater.inflate(R.layout.fragment_predictions_rules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase
        val arguments = PredictionsRulesArgs.fromBundle(requireArguments())
        val user = arguments.userNickname

        view.findViewById<ImageView>(R.id.back_to_select_championship).setOnClickListener {
            val navigateToSelectChampionship = PredictionsRulesDirections.actionPredictionsRulesToSelectChampionship(user)
            view.findNavController().navigate(navigateToSelectChampionship)
            app.hearRulesTextToSpeech.stop()
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = PredictionsRulesDirections.actionPredictionsRulesToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                app.hearRulesTextToSpeech.stop()
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        view.findViewById<ImageView>(R.id.hear_rules).setOnClickListener {
            if (!app.hearRulesTextToSpeech.isSpeaking) {
                val audioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume == 0) {
                    Toast.makeText(view.context, R.string.turn_up_volume, Toast.LENGTH_LONG).show()
                }
                app.hearRulesTextToSpeech.setSpeechRate(0.9.toFloat())
                app.hearRulesTextToSpeech.speak(getString(R.string.rules_1), TextToSpeech.QUEUE_FLUSH, null, "speak_ID")
            }
            else {
                app.hearRulesTextToSpeech.stop()
            }
        }
    }
}