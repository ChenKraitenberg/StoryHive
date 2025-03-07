package com.example.storyhive.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentLoginBinding

/**
 * Login screen for user authentication.
 * - Allows users to sign in using email and password.
 * - Navigates to the sign-up screen if the user is not registered.
 * - Observes authentication state and navigates to the home screen upon success.
 */
class LoginFragment : Fragment() {

    // View Binding for accessing UI elements
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // ViewModel for authentication logic
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called after the view is created.
     * - Sets up UI event listeners.
     * - Observes ViewModel state changes.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()


    }

    /**
     * Sets up UI event listeners for login and sign-up navigation.
     */
    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            // Validate user input before attempting login
            if (validateInput(email, password)) {
                viewModel.signIn(email, password)
            }
        }

        // Navigate to the sign-up screen
        binding.signUpTextView.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }


    /**
     * Observes authentication state changes from the ViewModel.
     * - Navigates to the home screen upon successful login.
     * - Displays an error message if login fails.
     */
    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Success -> {
                    findNavController().navigate(R.id.action_login_to_home)
                }

                is AuthState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                is AuthState.Loading -> {
                }
                else -> {
                }
            }
        }
    }


    /**
     * Validates user input fields.
     * - Ensures email and password are not empty.
     * - Displays an error message for invalid fields.
     */
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.emailInput.error = "יש למלא אימייל"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "יש למלא סיסמה"
            isValid = false
        }

        return isValid
    }

    /**
     * Cleans up View Binding to prevent memory leaks when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}