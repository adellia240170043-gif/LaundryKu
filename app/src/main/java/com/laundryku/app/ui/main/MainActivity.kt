package com.laundryku.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.laundryku.app.R
import com.laundryku.app.databinding.ActivityMainBinding
import com.laundryku.app.ui.auth.AuthViewModel
import com.laundryku.app.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text =
            getString(R.string.welcome_user, authViewModel.currentUserLabel().orEmpty())

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            val intent = Intent(this, LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            startActivity(intent)
        }
    }
}