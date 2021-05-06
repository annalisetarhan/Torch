package com.annalisetarhan.torch.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.annalisetarhan.torch.R
import com.annalisetarhan.torch.databinding.FragmentHashtagsBinding

class HashtagsFragment : Fragment() {

    lateinit var viewModel: MainViewModel
    lateinit var binding: FragmentHashtagsBinding

    private var numHashtags = 0
    private val hashtags = arrayOfNulls<String>(5)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentHashtagsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity(), defaultViewModelProviderFactory).get(MainViewModel::class.java)

        observeHashtags()
        setHashtags()
        setClickListeners()
        setUpContextMenu()

        /* Comment out for testing on emulators or other devices that aren't WiFi Aware enabled. */
        if ((activity as MainActivity).hasWifiAware != true) {
            showProblem()
        }

        return binding.root
    }

    private fun addNewHashtag(hashtag: String) {
        binding.emptyHashtagListText.visibility = GONE
        val strippedHashtag = hashtag.trim().trim('#')
        viewModel.addHashtag(strippedHashtag)
    }

    private fun observeHashtags() {
        val numHashtagsObserver = Observer<Int> { newNum ->
            numHashtags = newNum
            setHashtags()
        }
        viewModel.numHashtags.observe(viewLifecycleOwner, numHashtagsObserver)
    }

    private fun setHashtags() {
        val activeHashtags = viewModel.hashtagToLivedata.keys.toList()
        binding.hashtagOne.visibility = GONE
        binding.hashtagTwo.visibility = GONE
        binding.hashtagThree.visibility = GONE
        binding.hashtagFour.visibility = GONE
        binding.hashtagFive.visibility = GONE
        if (numHashtags > 0) {
            binding.emptyHashtagListText.visibility = GONE
            hashtags[0] = activeHashtags[0]
            binding.hashtagOne.text = getString(R.string.hashtag, activeHashtags[0])
            binding.hashtagOne.visibility = VISIBLE
        } else {
            binding.emptyHashtagListText.visibility = VISIBLE
        }
        if (numHashtags > 1) {
            hashtags[1] = activeHashtags[1]
            binding.hashtagTwo.text = getString(R.string.hashtag, activeHashtags[1])
            binding.hashtagTwo.visibility = VISIBLE
        }
        if (numHashtags > 2) {
            hashtags[2] = activeHashtags[2]
            binding.hashtagThree.text = getString(R.string.hashtag, activeHashtags[2])
            binding.hashtagThree.visibility = VISIBLE
        }
        if (numHashtags > 3) {
            hashtags[3] = activeHashtags[3]
            binding.hashtagFour.text = getString(R.string.hashtag, activeHashtags[3])
            binding.hashtagFour.visibility = VISIBLE
        }
        if (numHashtags > 4) {
            hashtags[4] = activeHashtags[4]
            binding.hashtagFive.text = getString(R.string.hashtag, activeHashtags[4])
            binding.hashtagFive.visibility = VISIBLE
        }
    }

    private fun showProblem() {
        binding.emptyHashtagListText.text = getString(R.string.no_wifi_aware_message)
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
                showAddHashtagDialog()
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

    private fun showAddHashtagDialog() {
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

    /*
     *  DELETE HASHTAGS
     */
    private var doomedHashtag: String? = null

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        /* Store clicked hashtag so there's a reference to it later */
        doomedHashtag = (v as TextView).text.toString().trim('#')

        val inflater: MenuInflater = requireActivity().menuInflater
        inflater.inflate(R.menu.hashtag_options_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_hashtag -> {
                doomedHashtag?.let { viewModel.deleteHashtag(it) }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun setUpContextMenu() {
        this.registerForContextMenu(binding.hashtagOne)
        this.registerForContextMenu(binding.hashtagTwo)
        this.registerForContextMenu(binding.hashtagThree)
        this.registerForContextMenu(binding.hashtagFour)
        this.registerForContextMenu(binding.hashtagFive)
    }
}