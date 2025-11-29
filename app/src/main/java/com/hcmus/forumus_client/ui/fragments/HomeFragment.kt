package com.hcmus.forumus_client.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.databinding.FragmentHomeBinding
import com.hcmus.forumus_client.ui.home.MainViewModel
import com.hcmus.forumus_client.ui.home.PostAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        viewModel.loadSamplePosts()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(object : PostAdapter.PostInteractionListener {
            override fun onUpvote(post: com.hcmus.forumus_client.data.model.Post) {
                viewModel.onUpvote(post.id)
            }
            override fun onDownvote(post: com.hcmus.forumus_client.data.model.Post) {
                viewModel.onDownvote(post.id)
            }
            override fun onComments(post: com.hcmus.forumus_client.data.model.Post) { /* TODO */ }
            override fun onShare(post: com.hcmus.forumus_client.data.model.Post) { /* TODO */ }
            override fun onPostClicked(post: com.hcmus.forumus_client.data.model.Post) { /* TODO */ }
        })
        
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = postAdapter
        
        viewModel.posts.observe(viewLifecycleOwner) { list -> 
            postAdapter.submitList(list) 
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}