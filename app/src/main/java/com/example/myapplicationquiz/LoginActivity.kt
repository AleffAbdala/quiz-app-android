package com.example.myapplicationquiz

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplicationquiz.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            persistUserProfile(user.uid, user.email.orEmpty())
                        }

                        Toast.makeText(this, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Falha no registro: ${task.exception?.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                }
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            persistUserProfile(user.uid, user.email.orEmpty())
                        }

                        Toast.makeText(this, "Login efetuado com sucesso!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Falha no login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            persistUserProfile(currentUser.uid, currentUser.email.orEmpty())
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun persistUserProfile(uid: String, email: String) {
        val profile = UserProfileModel(
            uid = uid,
            email = email,
            name = email.substringBefore('@'),
            lastLoginAt = System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(uid)
            .setValue(profile)

        val prefs = getSharedPreferences("quiz_user_profile", MODE_PRIVATE)
        prefs.edit()
            .putString("uid", uid)
            .putString("email", email)
            .putString("name", profile.name)
            .putLong("lastLoginAt", profile.lastLoginAt)
            .apply()
    }
}
