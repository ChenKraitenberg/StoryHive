package com.example.storyhive.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storyhive.databinding.FragmentProfileBinding
import com.example.storyhive.ui.home.PostsAdapter
import com.example.storyhive.data.models.Post

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val postsAdapter = PostsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // הגדרת RecyclerView
        binding.postsRecyclerView.adapter = postsAdapter
        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // נתוני דוגמא
        val mockPosts = listOf(
            Post(id = "1", userName = "Avital Aizenkot", bookTitle = "Harry Potter", content = "Great book!"),
            Post(id = "2", userName = "Avital Aizenkot", bookTitle = "Romeo and Juliet", content = "A timeless classic.")
        )
        postsAdapter.submitList(mockPosts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
