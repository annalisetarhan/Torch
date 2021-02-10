package com.annalisetarhan.torch.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.annalisetarhan.torch.connection.WiFiConnection
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var connection: WiFiConnection? = null
    private lateinit var handler : Handler
    init {
        // TODO: find out (from application) if this device even supports WiFi Aware. Or, handle exception somewhere.
        viewModelScope.launch {
            handler = Handler(Looper.getMainLooper())
            connection = WiFiConnection(application, handler)

        }
    }

    override fun onCleared() {
        super.onCleared()
        connection?.endSession()
    }
}