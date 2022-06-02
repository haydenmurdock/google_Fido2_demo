package com.example.fido2.ui.FidoPicker

import androidx.lifecycle.ViewModel

import com.example.fido2.repository.FIDORepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FidoPickerViewModel @Inject constructor(
    private val repository: FIDORepository
) : ViewModel() {

    fun userSelectedFido2(){
        repository.userSelectedFido2()
    }

    fun userSelectedUAF(){
        repository.userSelectedUAF()
    }
}