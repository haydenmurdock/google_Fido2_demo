/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.fido2.ui.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fido2.api.PresidioIdentityAuthApi
import com.example.fido2.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsernameViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val presidioApi: PresidioIdentityAuthApi
) : ViewModel() {

    private val _sending = MutableStateFlow(false)
    val sending = _sending.asStateFlow()

    val username = MutableStateFlow("")

    val nextEnabled = combine(sending, username) { isSending, username ->
        !isSending && username.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun sendUsername(mUserName: String) {
//        val username = mUserName
//        println("Sending: $username")
//        if (username.isNotBlank()) {
//            viewModelScope.launch {
//                _sending.value = true
//                try {
//                    repository.username(username)
//                } finally {
//                    _sending.value = false
//                }
//            }
//        }
    }
    fun goToHomeScreen(mUserName: String){
    //    var response:ApiResult<PublicKeyCredentialCreationOptions>? = null

        if (mUserName.isNotBlank()) {
            viewModelScope.launch{
                    repository.signedInWorkaround()
            }
        }
    }
}
