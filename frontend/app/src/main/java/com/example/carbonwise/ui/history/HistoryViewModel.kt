package com.example.carbonwise.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HistoryViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "HISTORY FRAGMENT"
    }
    val text: LiveData<String> = _text
}