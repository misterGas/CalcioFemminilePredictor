package com.embeddedproject.calciofemminileitaliano

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns.EMAIL_ADDRESS
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.helpers.User
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.stream.IntStream.range

class LoginRegistration : Fragment() {

    private var generateRandomSecureCode = "0"

    private lateinit var db: FirebaseDatabase
    private lateinit var authentication: FirebaseAuth
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.teal_toolbar)
        activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.white)

        authentication = FirebaseAuth.getInstance()
        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase
        val findNicknamesInDB = mutableListOf<String>()
        view.findViewById<TextView>(R.id.forgotten).setOnClickListener {
            val navigateToForgottenPassword = LoginRegistrationDirections.actionLoginRegistrationToForgottenPassword()
            view.findNavController().navigate(navigateToForgottenPassword)
        }
        reference.child("Users").get().addOnCompleteListener {
            val nicknames = it.result.value.toString().split("nickname=")
            for (i in range(1, nicknames.size)) {
                findNicknamesInDB.add(nicknames[i].substring(0, nicknames[i].indexOf("}")))
            }

            generateRandomSecureCode = if (savedInstanceState?.containsKey("Secure Code") == true) {
                savedInstanceState.getString("Secure Code")!!
            }
            else {
                "${(0..9).random()}${(0..9).random()}${(0..9).random()}${(0..9).random()}"
            }

            val emailLoginEditText = view.findViewById<EditText>(R.id.email_login)
            val passwordLoginEditText = view.findViewById<EditText>(R.id.password_login)

            val loginButton = view.findViewById<Button>(R.id.submit_login)
            loginButton.setOnClickListener {
                val emailLogin = emailLoginEditText.text.toString()
                val passwordLogin = passwordLoginEditText.text.toString()

                if (emailLogin.isNotEmpty() && passwordLogin.isNotEmpty()) {
                    if (EMAIL_ADDRESS.matcher(emailLogin).matches()) {
                        authentication.signInWithEmailAndPassword(emailLogin, passwordLogin).addOnCompleteListener { it2 ->
                            if (it2.isSuccessful) {
                                reference.child("Users").child(emailLogin.replace(".", "-")).get().addOnCompleteListener { it3 ->
                                    val userNickname = it3.result.child("nickname").value.toString()
                                    Toast.makeText(view.context, R.string.login_completed, Toast.LENGTH_LONG).show()
                                    val userLoginInDatabase = ContentValues()
                                    userLoginInDatabase.put("UserNickname", userNickname)
                                    dbReference.insert("USER", null, userLoginInDatabase)
                                    val navigateToMatchPredictions = LoginRegistrationDirections.actionLoginRegistrationToSelectChampionship(userNickname)
                                    view.findNavController().navigate(navigateToMatchPredictions)
                                }
                            }
                            else {
                                Toast.makeText(view.context, R.string.incorrect, Toast.LENGTH_LONG).show()
                            }
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

            val emailRegistrationEditText = view.findViewById<EditText>(R.id.email_registration)
            val passwordRegistrationEditText = view.findViewById<EditText>(R.id.password_registration)
            val firstNameEditText = view.findViewById<EditText>(R.id.first_name)
            val lastNameEditText = view.findViewById<EditText>(R.id.last_name)
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname)
            val secureCodeEditText = view.findViewById<EditText>(R.id.secure_code_sum)
            secureCodeEditText.hint = "${getString(R.string.secure_code_header)}: $generateRandomSecureCode"

            val privacyPolicyLink: TextView = view.findViewById(R.id.privacy_policy_link)
            val privacyPolicyCheckbox: CheckBox = view.findViewById(R.id.privacy_policy_checkbox)

            privacyPolicyLink.setOnClickListener {
                val url = "https://sites.google.com/view/calciofemminilepredictor"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }

            val subscribeButton = view.findViewById<Button>(R.id.submit_registration)
            subscribeButton.setOnClickListener {
                val emailRegistration = emailRegistrationEditText.text.toString()
                val passwordRegistration = passwordRegistrationEditText.text.toString()
                val firstName = firstNameEditText.text.toString()
                val lastName = lastNameEditText.text.toString()
                val nickname = nicknameEditText.text.toString()
                val secureCodeResult = secureCodeEditText.text.toString()
                var numbers = 0
                for (i in generateRandomSecureCode) {
                    numbers += i - '0'
                }
                val computeSecureCode = numbers.toString()[numbers.toString().length - 1] - '0'

                if (emailRegistration.isNotEmpty() && passwordRegistration.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty() && nickname.isNotEmpty() && secureCodeResult.isNotEmpty()) {
                    if (privacyPolicyCheckbox.isChecked) {
                        if (EMAIL_ADDRESS.matcher(emailRegistration).matches()) {
                            if (passwordRegistration.length >= 6) {
                                if (secureCodeResult.toInt() == computeSecureCode) {
                                    if (!findNicknamesInDB.contains(nickname)) {
                                        authentication.createUserWithEmailAndPassword(emailRegistration, passwordRegistration).addOnCompleteListener { it2 ->
                                            if (it2.exception.toString() == "null") {
                                                val user = User(firstName, lastName, nickname, emailRegistration)
                                                reference.child("Users").child(emailRegistration.replace(".", "-")).setValue(user).addOnCompleteListener {
                                                    Toast.makeText(view.context, R.string.registration_completed, Toast.LENGTH_LONG).show()
                                                    emailRegistrationEditText.setText("")
                                                    passwordRegistrationEditText.setText("")
                                                    firstNameEditText.setText("")
                                                    lastNameEditText.setText("")
                                                    nicknameEditText.setText("")
                                                    secureCodeEditText.hint = ""
                                                    secureCodeEditText.setText("")
                                                    val navigateToLogin = LoginRegistrationDirections.actionLoginRegistrationSelf()
                                                    view.findNavController().navigate(navigateToLogin)
                                                }
                                            }
                                            else {
                                                Toast.makeText(view.context, R.string.email_already_used, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    else {
                                        Toast.makeText(view.context, R.string.nickname_already_used, Toast.LENGTH_LONG).show()
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
                                Toast.makeText(view.context, R.string.password_too_short, Toast.LENGTH_LONG).show()
                            }
                        }
                        else {
                            Toast.makeText(view.context, R.string.email_unmarked, Toast.LENGTH_LONG).show()
                        }
                    }
                    else {
                        Toast.makeText(view.context, R.string.you_need_to_accept_privacy_policy, Toast.LENGTH_LONG).show()
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