package com.annalisetarhan.torch.ui

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.annalisetarhan.torch.R
import com.annalisetarhan.torch.databinding.FragmentHashtagsBinding

class HashtagsFragment : Fragment() {

    lateinit var binding: FragmentHashtagsBinding
    var numHashtags = 0
    val hashtags = arrayOfNulls<String>(5)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentHashtagsBinding.inflate(inflater, container, false)

        // TODO: fetch old hashtags from savedInstanceState

        if ((activity as MainActivity).hasWifiAware) {
            setVisibility()
            setClickListeners()
        } else {
            showProblem()
        }

        return binding.root
    }

    private fun showProblem() {
        binding.emptyHashtagListText.text = "Your device doesn't support WiFi Aware :("
        binding.fab.visibility = GONE
        binding.hashtagOne.visibility = GONE
        binding.hashtagTwo.visibility = GONE
        binding.hashtagThree.visibility = GONE
        binding.hashtagFour.visibility = GONE
        binding.hashtagFive.visibility = GONE
    }

    private fun setClickListeners() {
        binding.fab.setOnClickListener {
            if (numHashtags > 4) {
                Toast.makeText(activity, "Only five hashtags at a time, please!", Toast.LENGTH_SHORT).show()
            } else {
                showDialog()
            }
        }

        binding.hashtagOne.setOnClickListener {
            val bundle = bundleOf("hashtag" to hashtags[0])
            findNavController().navigate(R.id.action_HashtagsFragment_to_ConvoFragment, bundle)
        }
        binding.hashtagTwo.setOnClickListener {
            val bundle = bundleOf("hashtag" to hashtags[1])
            findNavController().navigate(R.id.action_HashtagsFragment_to_ConvoFragment, bundle)
        }
        binding.hashtagThree.setOnClickListener {
            val bundle = bundleOf("hashtag" to hashtags[2])
            findNavController().navigate(R.id.action_HashtagsFragment_to_ConvoFragment, bundle)
        }
        binding.hashtagFour.setOnClickListener {
            val bundle = bundleOf("hashtag" to hashtags[3])
            findNavController().navigate(R.id.action_HashtagsFragment_to_ConvoFragment, bundle)
        }
        binding.hashtagFive.setOnClickListener {
            val bundle = bundleOf("hashtag" to hashtags[4])
            findNavController().navigate(R.id.action_HashtagsFragment_to_ConvoFragment, bundle)
        }
    }

    private fun showDialog() {
        val hashtagEditText = EditText(activity)
        hashtagEditText.gravity = Gravity.CENTER_HORIZONTAL
        AlertDialog.Builder(activity)
            .setTitle("Choose Hashtag")
            .setView(hashtagEditText)
            .setPositiveButton("Add") { _, _ ->
                when {
                    hashtagEditText.text.isNullOrEmpty() -> {
                        Toast.makeText(activity, "Hashtag can't be blank!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    hashtagEditText.text.toString().length > 35 -> {
                        Toast.makeText(activity, "That's a long hashtag! It's been shortened a bit.", Toast.LENGTH_SHORT)
                            .show()
                        addNewHashtag(hashtagEditText.text.toString().take(35))
                    }
                    else -> {
                        addNewHashtag(hashtagEditText.text.toString())
                    }
                }
            }
            .create()
            .show()
    }

    private fun addNewHashtag(hashtag: String) {
        binding.emptyHashtagListText.visibility = GONE
        numHashtags += 1

        val strippedHashtag = hashtag.trim().trim('#')  // Remove leading and trailing whitespace and #s
        when (numHashtags) {
            1 -> {
                hashtags[0] = strippedHashtag
                binding.hashtagOne.text = strippedHashtag
                binding.hashtagOne.visibility = VISIBLE
            }
            2 -> {
                hashtags[1] = strippedHashtag
                binding.hashtagTwo.text = strippedHashtag
                binding.hashtagTwo.visibility = VISIBLE
            }
            3 -> {
                hashtags[2] = strippedHashtag
                binding.hashtagThree.text = strippedHashtag
                binding.hashtagThree.visibility = VISIBLE
            }
            4 -> {
                hashtags[3] = strippedHashtag
                binding.hashtagFour.text = strippedHashtag
                binding.hashtagFour.visibility = VISIBLE
            }
            5 -> {
                hashtags[4] = strippedHashtag
                binding.hashtagFive.text = strippedHashtag
                binding.hashtagFive.visibility = VISIBLE
            }
        }

    }

    // Todo: How to remove hashtags?
    private fun removeHashtag(hashtag: String) {
        var deletedFlag = false
        for (i in 0..3) {
            if (hashtags[i].toString() == hashtag) {
                hashtags[i] = hashtags[i+1]
                deletedFlag = true
            } else if (deletedFlag) {
                hashtags[i] = hashtags[i+1]
            }
        }
        hashtags[4] = null
        numHashtags -= 1
        setVisibility()
    }

    private fun setVisibility() {
        if (!hashtags[0].isNullOrEmpty()) {
            binding.emptyHashtagListText.visibility = GONE
            binding.hashtagOne.text = hashtags[0]
            binding.hashtagOne.visibility = VISIBLE
        } else {
            binding.hashtagOne.visibility = GONE
            binding.emptyHashtagListText.visibility = VISIBLE
        }
        if (!hashtags[1].isNullOrEmpty()) {
            binding.hashtagTwo.text = hashtags[1]
            binding.hashtagTwo.visibility = VISIBLE
        } else {
            binding.hashtagOne.visibility = GONE
        }
        if (!hashtags[2].isNullOrEmpty()) {
            binding.hashtagThree.text = hashtags[2]
            binding.hashtagThree.visibility = VISIBLE
        } else {
            binding.hashtagThree.visibility = GONE
        }
        if (!hashtags[3].isNullOrEmpty()) {
            binding.hashtagFour.text = hashtags[3]
            binding.hashtagFour.visibility = VISIBLE
        } else {
            binding.hashtagFour.visibility = GONE
        }
        if (!hashtags[4].isNullOrEmpty()) {
            binding.hashtagFive.text = hashtags[4]
            binding.hashtagFive.visibility = VISIBLE
        } else {
            binding.hashtagFive.visibility = GONE
        }
    }
}