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

package com.example.fido2.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.fido2.R
import com.example.fido2.api.ApiResult
import com.example.fido2.databinding.HomeFragmentBinding
import com.example.fido2.providers.ScopedFragment
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class HomeFragment() : ScopedFragment(), DeleteConfirmationFragment.Listener {

    companion object {
        private const val TAG = "HomeFragment"
        private const val FRAGMENT_DELETE_CONFIRMATION = "delete_confirmation"
    }

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: HomeFragmentBinding
    private var publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptions? = null

    private val createCredentialIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleCreateCredentialResult
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = HomeFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val credentialAdapter = CredentialAdapter { credentialId ->
            println("User requested deleting credentials: Showing delete confirmation fragment. credential id $credentialId")
            DeleteConfirmationFragment.newInstance(credentialId)
                .show(childFragmentManager, FRAGMENT_DELETE_CONFIRMATION)
        }
       val fingerPrintImageView = activity?.findViewById<ImageView>(R.id.imageView4)
        val sendUserNameBtn = activity?.findViewById<Button>(R.id.sendUsernameBtn)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.credentials.collect { credentials ->
                credentialAdapter.submitList(credentials)
            }
        }

        sendUserNameBtn?.setOnClickListener {
            launch {
                val result = viewModel.sendUsernameToPI("Hayden")
                when (result) {
                 is  ApiResult.Success -> {
                   publicKeyCredentialCreationOptions =  result.data
                 }
                    else -> {
                        Toast.makeText(this@HomeFragment.context, "Issue getting data from P.I.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.processing.collect { processing -> }
        }
        fingerPrintImageView?.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    println("confirm Button Pressed")
                    lifecycleScope.launch {
                        var intent:PendingIntent? = null
                        if (publicKeyCredentialCreationOptions != null){
                            intent = registerPIRequest(publicKeyCredentialCreationOptions!!)
                            if (intent != null) { println("$intent")
                                createCredentialIntentLauncher.launch(
                                    IntentSenderRequest.Builder(intent).build()
                                )
                            }
                        } else {
                            Toast.makeText(this@HomeFragment.context, "Return from P.I. was null", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            v?.onTouchEvent(event) ?: true
        }
    }
    private fun handleCreateCredentialResult(activityResult: ActivityResult) {
        // TODO(3): Receive ActivityResult with the new Credential
        // - Extract byte array from result data using Fido.FIDO2_KEY_CREDENTIAL_EXTRA.
        // (continued below
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                Toast.makeText(requireContext(), R.string.cancelled, Toast.LENGTH_LONG).show()
            bytes == null ->
                Toast.makeText(requireContext(), R.string.credential_error, Toast.LENGTH_LONG)
                    .show()
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)

                val response = credential.response
                println("response from fingerprint dialog: $response")
                if (response is AuthenticatorErrorResponse) {
                    Toast.makeText(requireContext(), response.errorMessage, Toast.LENGTH_LONG)
                        .show()
                } else {
                    viewModel.registerResponse(credential)
                    inflateLoadingView()
                }
            }
        }
    }
//    * Respond with required information to call navigator.credential.create()
//    * Input is passed via `req.body` with similar format as output
//    * Output format:
//    * ```{
//        rp: {
//                id: String,
//                name: String
//        },
//        user: {
//                displayName: String,
//                id: String,
//                name: String
//        },
//        publicKeyCredParams: [{  // @herrjemand
//            type: 'public-key', alg: -7
//        }],
//        timeout: Number,
//        challenge: String,
//        excludeCredentials: [{
//            id: String,
//            type: 'public-key',
//            transports: [('ble'|'nfc'|'usb'|'internal'), ...]
//        }, ...],
//        authenticatorSelection: {
//            authenticatorAttachment: ('platform'|'cross-platform'),
//            requireResidentKey: Boolean,
//            userVerification: ('required'|'preferred'|'discouraged')
//        },
//        attestation: ('none'|'indirect'|'direct')
//    private fun getTestPublicKeyCredentials(): PublicKeyCredentialCreationOptions {
//        val parameters = mutableListOf<PublicKeyCredentialParameters>()
//        parameters.add(PublicKeyCredentialParameters("public-key", -7))
//        parameters.add(PublicKeyCredentialParameters("public-key", -257))
//        var authSelectionBuilder = AuthenticatorSelectionCriteria.Builder().setAttachment(Attachment.fromString("platform")).setRequireResidentKey(false).build()
//        val list = mutableListOf<PublicKeyCredentialDescriptor>()
//        list.add(PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY.toString(), "".decodeBase64(), null))
//        val challenge = ChallengeHolder.findChallengeString() ?: ""
//        val userIdString = ChallengeHolder.findUserString() ?: ""
//        println("using $challenge for challenge. userIdString $userIdString")
//        val builder = PublicKeyCredentialCreationOptions.Builder()
//            .setRp(PublicKeyCredentialRpEntity("develop.presidioidentity.net", "Presidio Identity", /* icon */ null))
//            .setUser(PublicKeyCredentialUserEntity("Ik5qSmxObVkzTVRGaE5UaGlNelV3TURJMk56SXpPVGhoIg==".decodeBase64(), "kehban", null, "kaue6767"))
//            .setChallenge("zm31u6LtC5Fwrz3Sr1xJDei9Fn4CyxG6AIYUQGcVIU".decodeBase64())
//            .setParameters(parameters)
//            .setAuthenticatorSelection(authSelectionBuilder)
//            .setExcludeList(list)
//            .setTimeoutSeconds(1000000.00)
//        return builder.build()
//    }
    private suspend fun registerPIRequest(options: PublicKeyCredentialCreationOptions): PendingIntent? {
        val fido2client = viewModel.getFido2client()
        fido2client?.let { client ->
            try {
                val task = client.getRegisterPendingIntent(options)
                println("apiResult data: ${options}")
                return task.await()
            } catch (e: IllegalStateException){
                return  null
            }
        }
        return null
    }


   private fun inflateLoadingView(){
        val viewGroup = activity?.findViewById<View>(android.R.id.content) as ViewGroup
         View.inflate(this.context, R.layout.setting_up_device, viewGroup)
       val timer = object: CountDownTimer(5000, 1000) {
           override fun onTick(millisUntilFinished: Long) {
               }

           override fun onFinish() {
               updateToSuccess()
           }
       }
       timer.start()
    }

    private fun updateToSuccess(){
        val viewGroup = activity?.findViewById<View>(android.R.id.content) as ViewGroup
        View.inflate(this.context, R.layout.succesful_setup, viewGroup)
        val timer = object: CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                val viewGroup = activity?.findViewById<View>(android.R.id.content) as ViewGroup
                viewGroup.removeViewAt(viewGroup.childCount - 1)
                viewGroup.removeViewAt(viewGroup.childCount - 1)
                View.inflate(activity, R.layout.bank_account, viewGroup)
            }
        }
        timer.start()

    }

        override fun onDeleteConfirmed(credentialId: String) {
            viewModel.removeKey(credentialId)
        }
}
