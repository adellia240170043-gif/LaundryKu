package com.laundryku.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.laundryku.app.R
import com.laundryku.app.databinding.ActivityRegisterBinding
import com.laundryku.app.ui.main.MainActivity
import com.laundryku.app.util.UiState

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.btnGoLogin.setOnClickListener { finish() }
        observeState()
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        if (validate(name, email, password, confirm)) {
            viewModel.register(name, email, password)
        }
    }

    private fun validate(
        name: String,
        email: String,
        password: String,
        confirm: String
    ): Boolean {
        binding.tilName.error =
            if (name.isEmpty()) getString(R.string.error_name_empty) else null

        binding.tilEmail.error = when {
            email.isEmpty() -> getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                getString(R.string.error_email_invalid)
            else -> null
        }

        binding.tilPassword.error = when {
            password.isEmpty() -> getString(R.string.error_password_empty)
            password.length < 6 -> getString(R.string.error_password_short)
            else -> null
        }

        binding.tilConfirmPassword.error =
            if (confirm != password) getString(R.string.error_password_mismatch) else null

        return binding.tilName.error == null &&
                binding.tilEmail.error == null &&
                binding.tilPassword.error == null &&
                binding.tilConfirmPassword.error == null
    }

    private fun observeState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is UiState.Idle -> setLoading(false)
                is UiState.Loading -> setLoading(true)
                is UiState.Success -> {
                    setLoading(false)
                    val intent = Intent(this, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                    startActivity(intent)
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
        binding.btnRegister.isEnabled = !loading
        binding.btnGoLogin.isEnabled = !loading
    }
}