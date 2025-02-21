package com.embeddedproject.calciofemminileitaliano

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.stream.IntStream

class ForgottenPassword : Fragment() {

    private var generateRandomSecureCode = "0"

    private lateinit var db: FirebaseDatabase
    private lateinit var authentication: FirebaseAuth
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_forgotten_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = FirebaseDatabase.getInstance()
        reference = db.reference
        authentication = FirebaseAuth.getInstance()
        val findNicknamesInDB = mutableListOf<String>()

        view.findViewById<Button>(R.id.back_to_login).setOnClickListener {
            val navigateToLoginRegistration = ForgottenPasswordDirections.actionForgottenPasswordToLoginRegistration()
            view.findNavController().navigate(navigateToLoginRegistration)
        }

        reference.child("Users").get().addOnCompleteListener {
            val nicknames = it.result.value.toString().split("nickname=")
            for (i in IntStream.range(1, nicknames.size)) {
                findNicknamesInDB.add(nicknames[i].substring(0, nicknames[i].indexOf("}")))
            }

            generateRandomSecureCode = if (savedInstanceState?.containsKey("Secure Code") == true) {
                savedInstanceState.getString("Secure Code")!!
            }
            else {
                "${(0..9).random()}${(0..9).random()}${(0..9).random()}${(0..9).random()}"
            }

            val emailLoginEditText = view.findViewById<EditText>(R.id.email_modify)
            val secureCodeEditText = view.findViewById<EditText>(R.id.secure_code_mul)
            secureCodeEditText.hint = "${getString(R.string.secure_code_header)}: $generateRandomSecureCode"
            val modifyButton = view.findViewById<Button>(R.id.submit_modify)

            modifyButton.setOnClickListener {
                val email = emailLoginEditText.text.toString()
                val secureCodeResult = secureCodeEditText.text.toString()
                var numbers = 1
                for (i in generateRandomSecureCode) {
                    numbers *= i - '0'
                }
                val computeSecureCode = numbers.toString()[numbers.toString().length - 1] - '0'

                if (email.isNotEmpty() && secureCodeResult.isNotEmpty()) {
                    if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        if (secureCodeResult.toInt() == computeSecureCode) {
                            authentication.sendPasswordResetEmail(email).addOnCompleteListener { it2 ->
                                if (it2.exception.toString() == "null") {
                                    Toast.makeText(view.context, R.string.watch_emails, Toast.LENGTH_LONG).show()
                                    val navigateToLoginRegistration = ForgottenPasswordDirections.actionForgottenPasswordToLoginRegistration()
                                    view.findNavController().navigate(navigateToLoginRegistration)
                                }
                                else {
                                    Toast.makeText(view.context, R.string.email_not_found, Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                        else {
                            Toast.makeText(view.context, R.string.secure_code_incorrect, Toast.LENGTH_LONG).show()
                            generateRandomSecureCode = "${(0..9).random()}${(0..9).random()}${(0..9).random()}${(0..9).random()}"
                            secureCodeEditText.setText("")
                            secureCodeEditText.hint = "${getString(R.string.secure_code_header)}: $generateRandomSecureCode"
                        }
                    }
                    else {
                        Toast.makeText(view.context, R.string.email_unmarked, Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    Toast.makeText(view.context, R.string.all_fields_required, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("Secure Code", generateRandomSecureCode)
    }
}