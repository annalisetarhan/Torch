package com.annalisetarhan.torch.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.annalisetarhan.torch.R
import com.annalisetarhan.torch.databinding.FragmentConvoBinding

class ConvoFragment : Fragment() {
    lateinit var viewModel: MainViewModel
    lateinit var binding: FragmentConvoBinding
    lateinit var hashtag: String
    lateinit var adapter: MessageAdapter

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
        binding.hashtag = "#$hashtag"
        viewModel = ViewModelProvider(requireActivity(), defaultViewModelProviderFactory).get(MainViewModel::class.java)

        sendMessageButtonSetup()
        watchForNewSentMessages()
        watchForNewReceivedMessages()
        setUpRecyclerView()

        return binding.root
    }

    /* If scrollToBottom() is called in onCreateView instead, going back to fragment (from settings,
    * for example) shows the very top of the message thread instead of the most recent messages.*/
    override fun onResume() {
        scrollToBottom()
        super.onResume()
    }

    private fun sendMessageButtonSetup() {
        binding.sendMessageButton.setOnClickListener {
            val message = binding.sendMessageEditText.text.toString()
            if (message.isNotEmpty() && message.length <= 160) {        // length <= 160 is redundant, see xml
                viewModel.sendMessage(message, hashtag)
            }
        }
    }

    private fun watchForNewSentMessages() {
        binding.sendMessageButton.setOnClickListener {
            val message = binding.sendMessageEditText.text.toString()
            if (message != "") {
                viewModel.sendMessage(message, hashtag)
                binding.sendMessageEditText.text.clear()
            }
        }
    }

    private fun watchForNewReceivedMessages() {
        viewModel.getHashtagMessagesLivedata(hashtag)?.observe(viewLifecycleOwner, { messages ->
            messages?.let { adapter.setMessages(it) }
            scrollToBottom()
        })
    }

    private fun setUpRecyclerView() {
        adapter = MessageAdapter(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun scrollToBottom() {
        binding.recyclerView.scrollToPosition(adapter.itemCount-1)
    }
}