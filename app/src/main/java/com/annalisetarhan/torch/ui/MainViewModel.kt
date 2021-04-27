package com.annalisetarhan.torch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.annalisetarhan.torch.Repository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var repo: Repository = Repository(application)

    fun sendMessage(message: String, hashtag: String) {
        repo.sendStandardMessage(message, hashtag)
    }
}