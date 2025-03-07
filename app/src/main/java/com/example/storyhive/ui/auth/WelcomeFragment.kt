package com.example.storyhive.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.storyhive.R
import com.example.storyhive.databinding.FragmentWelcomeBinding

<<<<<<< HEAD

/**
 * **WelcomeFragment** - Displays the welcome screen with navigation options.
 * - Allows users to navigate to sign-in or sign-up screens.
 */
=======
>>>>>>> main
class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

<<<<<<< HEAD

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     */
=======
>>>>>>> main
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

<<<<<<< HEAD

    /**
     * Initializes UI components and sets up event listeners after the view is created.
     */
=======
>>>>>>> main
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInButton.setOnClickListener {
            // Navigate to the login screen
            findNavController().navigate(R.id.action_welcome_to_login)
        }

        binding.signUpButton.setOnClickListener {
            // Navigate to the sign up screen
            findNavController().navigate(
                WelcomeFragmentDirections.actionWelcomeToSignUp()
            )
        }
    }

<<<<<<< HEAD

    /**
     * Cleans up resources when the view is destroyed to prevent memory leaks.
     */
=======
>>>>>>> main
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}