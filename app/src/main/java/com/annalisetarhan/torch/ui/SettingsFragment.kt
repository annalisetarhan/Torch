package com.annalisetarhan.torch.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.annalisetarhan.torch.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    lateinit var binding: FragmentSettingsBinding
    lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity(), defaultViewModelProviderFactory).get(MainViewModel::class.java)

        showPublicKey()
        setUpButtons()

        return binding.root
    }

    private fun showPublicKey() {
        binding.publicKey = viewModel.getPublicKey()
    }

    private fun setUpButtons() {
        binding.changeKeysButton.setOnClickListener {
            viewModel.changeKeys()
            showPublicKey()
        }
    }
}