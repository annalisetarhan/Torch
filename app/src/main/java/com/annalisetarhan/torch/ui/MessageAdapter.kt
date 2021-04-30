package com.annalisetarhan.torch.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.annalisetarhan.torch.databinding.ReceivedMessageBinding
import com.annalisetarhan.torch.databinding.SentMessageBinding

const val RECEIVED = 1
const val SENT = 2

class MessageAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var messages = emptyList<DomainMessage>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            RECEIVED -> {
                val binding = ReceivedMessageBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding)
            }
            SENT -> {
                val binding = SentMessageBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val current = messages[position]
        when (holder) {
            is ReceivedMessageViewHolder -> holder.bind(current)
            is SentMessageViewHolder -> holder.bind(current)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sentByMe) {
            SENT
        } else {
            RECEIVED
        }
    }

    fun setMessages(messages: List<DomainMessage>) {
        this.messages = messages
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = messages.size

    inner class ReceivedMessageViewHolder(private val binding: ReceivedMessageBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: DomainMessage) {
            binding.message = message.message
            binding.userId = message.senderPublicKeyTrunc.toString()
        }
    }

    inner class SentMessageViewHolder(private val binding: SentMessageBinding)
        : RecyclerView.ViewHolder(binding.root) {
            fun bind(message: DomainMessage) {
                binding.message = message.message
            }
        }
}