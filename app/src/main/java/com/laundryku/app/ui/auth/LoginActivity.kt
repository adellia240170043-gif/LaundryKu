package com.laundryku.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.laundryku.app.R
import com.laundryku.app.databinding.ActivityLoginBinding
import com.laundryku.app.ui.main.MainActivity
import com.laundryku.app.util.UiState

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sesi tersimpan: kalau sudah login, langsung ke halaman utama
        if (viewModel.isLoggedIn()) {
            openMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        observeState()
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (validate(email, password)) {
            viewModel.login(email, password)
        }
    }

    private fun validate(email: String, password: String): Boolean {
        binding.tilEmail.error = when {
            email.isEmpty() -> getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                getString(R.string.error_email_invalid)
            else -> null
        }
        binding.tilPassword.error =
            if (password.isEmpty()) getString(R.string.error_password_empty) else null

        return binding.tilEmail.error == null && binding.tilPassword.error == null
    }

    private fun observeState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is UiState.Idle -> setLoading(false)
                is UiState.Loading -> setLoading(true)
                is UiState.Success -> {
                    setLoading(false)
                    openMain()
                }
                is UiState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
                    viewModel.resetState()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        binding.btnGoRegister.isEnabled = !loading
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}