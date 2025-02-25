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

class LoginFragment : Fragment() {

    // שימוש ב־View Binding
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // קבלת ה־ViewModel באמצעות delegation
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // לאחר יצירת התצוגה, מוגדרים המאזינים והמעקב אחרי ה־ViewModel
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()


    }

    // מאזין ללחיצות על כפתור הכניסה
    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            // קריאת הערכים מהקלט של המשתמש
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            // בדיקת תקינות הקלט
            if (validateInput(email, password)) {
                viewModel.signIn(email, password)
            }
        }
        // הוספת מאזין לכפתור ההרשמה
        binding.signUpTextView.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }

    // מעקב אחרי מצב ההתחברות כפי שמספק ה־ViewModel
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
                    // הציגי Spinner אם תרצי, או השאירי ריק
                }
                else -> {
                    // כל מצב אחר שלא נתמך
                }

            }
        }
    }


    // בדיקת תקינות הקלט, עם הודעות שגיאה מתאימות
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

    // שחרור המשאבים של ה־binding כדי למנוע זליגת זיכרון
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
