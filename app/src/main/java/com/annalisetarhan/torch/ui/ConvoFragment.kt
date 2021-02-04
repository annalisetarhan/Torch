package com.annalisetarhan.torch.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.annalisetarhan.torch.R
import com.annalisetarhan.torch.databinding.FragmentConvoBinding

class ConvoFragment : Fragment() {

    lateinit var binding: FragmentConvoBinding
    lateinit var hashtag: String

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        if (arguments?.getString("hashtag").isNullOrEmpty()) {
            findNavController().navigate(R.id.action_ConvoFragment_to_HashtagsFragment)
        } else {
            hashtag = arguments?.getString("hashtag")!!
        }
        binding = FragmentConvoBinding.inflate(inflater, container, false)
        return binding.root
    }
}