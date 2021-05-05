package com.annalisetarhan.torch.ui

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.annalisetarhan.torch.BuildConfig
import com.annalisetarhan.torch.database.Repository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("prefs", 0)
    var repo: Repository = Repository(application)

    val hashtagToLivedata = mutableMapOf<String, LiveData<List<DomainMessage>>>()
    val numHashtags = MutableLiveData<Int>()

    init {
        val hashtagSet = prefs.getStringSet("hashtags", null)
        if (hashtagSet != null) {
            for (hashtag in hashtagSet) {
                addHashtagInRepo(hashtag)
            }
        }
    }

    override fun onCleared() {
        repo.killConnection()
        super.onCleared()
    }

    fun sendMessage(message: String, hashtag: String) {
        viewModelScope.launch { repo.sendStandardMessage(hashtag, message) }
    }

    fun getPublicKey(): String {
        return repo.getPublicKey()
    }

    fun changeKeys() {
        repo.resetKeys()
    }

    fun addHashtag(hashtag: String) {
        /* Should only be called when numHashtags < 5 */
        if (BuildConfig.DEBUG && hashtagToLivedata.size >= 5) {
            error("Assertion failed")
        }

        addHashtagInRepo(hashtag)

        /* Add new hashtag to set stored in shared preferences */
        val hashtagSet = prefs.getStringSet("hashtags", null)
        val newHashtagSet = hashtagSet?.plus(hashtag) ?: setOf(hashtag)
        prefs.edit().putStringSet("hashtags", newHashtagSet).apply()
    }

    /* This is separated from addHashtag to avoid unnecessary sharedPrefs writes
    * when reading stored hashtags from sharedPrefs on init */
    private fun addHashtagInRepo(hashtag: String) {
        viewModelScope.launch {
            hashtagToLivedata[hashtag] = repo.addHashtag(hashtag)
            numHashtags.value = hashtagToLivedata.size
        }
    }

    fun getHashtagMessagesLivedata(hashtag: String): LiveData<List<DomainMessage>>? {
        return hashtagToLivedata[hashtag]
    }

    fun deleteHashtag(hashtag: String) {
        /* Should only be called when numHashtags > 0 */
        if (BuildConfig.DEBUG && hashtagToLivedata.isEmpty()) {
            error("Assertion failed")
        }

        hashtagToLivedata.remove(hashtag)
        viewModelScope.launch {
            repo.removeHashtag(hashtag)
            numHashtags.value = hashtagToLivedata.size
        }

        /* Remove hashtag from shared preferences */
        val hashtagSet = prefs.getStringSet("hashtags", null)
        val newHashtagSet = hashtagSet?.minus(hashtag)
        prefs.edit().putStringSet("hashtags", newHashtagSet).apply()
    }
}