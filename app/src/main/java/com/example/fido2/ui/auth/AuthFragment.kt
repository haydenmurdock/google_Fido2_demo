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

package com.example.fido2.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.fido2.R
import com.example.fido2.databinding.AuthFragmentBinding
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()
    private var goPasswordLessView: View? = null
    private var popUpView: View? = null
    private var blackBackground: View? = null
    private lateinit var binding: AuthFragmentBinding
    private var cardIsUp = false


    private val signIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleSignResult
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AuthFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val passwordEditText = activity?.findViewById<EditText>(R.id.passwordEditText)
        val confirmationBtn = activity?.findViewById<ImageButton>(R.id.confirmation_btn_password)
        val logInConstraintLayout = activity?.findViewById<ConstraintLayout>(R.id.log_in_constraintLayout)

        passwordEditText?.setOnTouchListener { v, event ->
            when(event.action){
              ACTION_DOWN -> {
                    if(!cardIsUp){
                        moveCardViewUp()
                       view.requestFocus()
                        showKeyboard(activity!!)
                    }
                    true
                }
                else -> {
                    view.requestFocus()
                    showKeyboard(activity!!)

                    true
                }
            }
        }
        passwordEditText?.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                moveCardViewUp()
            }
        }

        logInConstraintLayout?.setOnClickListener {
            if (!cardIsUp) {
                moveCardViewUp()
            }
            if (cardIsUp) {
                moveCardViewDown()
            }
        }
        confirmationBtn?.setOnTouchListener { v, event ->
            when (event?.action) {
             ACTION_DOWN ->{
                    print("confirm Button Pressed")
                    inflatePasswordLessAlert(passwordEditText?.text.toString())
                    moveCardViewDown()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            launch {
                viewModel.signinRequests.collect { intent ->
                    // TODO(6): Open the fingerprint dialog.
                    // - Open the fingerprint dialog by launching the intent from FIDO2 API.
                    signIntentLauncher.launch(IntentSenderRequest.Builder(intent).build())
                }
            }
            launch {
                viewModel.processing.collect { processing ->
                    if (processing) {
//                            binding.processing.show()
//                        } else {
//                            binding.processing.hide()
//                        }
                    }
                }
            }
        }

        passwordEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if(s == null){ return}
                if (s.count() >= 3) {
                    showConfirmBtn(confirmationBtn)
                } else {
                    hideConfirmBtn(confirmationBtn)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showConfirmBtn(confirmBtn: ImageButton?){
        if(confirmBtn == null) {return}
        if(confirmBtn.visibility == View.INVISIBLE){
            confirmBtn.visibility = View.VISIBLE
        }
    }
    private fun hideConfirmBtn(confirmBtn: ImageButton?){
        if(confirmBtn == null) {return}
        if(confirmBtn.visibility == View.VISIBLE){
            confirmBtn.visibility = View.INVISIBLE
        }
    }

    private fun moveCardViewUp(){
        val cardLayout =
            activity?.findViewById<ConstraintLayout>(R.id.log_in_constraintLayout)

        val params = cardLayout?.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = 200
        cardLayout.requestLayout()
        cardIsUp = true
    }
    private fun moveCardViewDown(){
        val cardLayout =
            activity?.findViewById<ConstraintLayout>(R.id.log_in_constraintLayout)
        val params = cardLayout?.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = 700
        cardLayout.requestLayout()
        cardIsUp = false
    }

    private fun handleSignResult(activityResult: ActivityResult) {

        // TODO(7): Handle the ActivityResult
        // - Extract byte array from result data using Fido.FIDO2_KEY_CREDENTIAL_EXTRA.
        // (continued below)
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                Toast.makeText(requireContext(), R.string.cancelled, Toast.LENGTH_SHORT).show()
            bytes == null ->
                Toast.makeText(requireContext(), R.string.auth_error, Toast.LENGTH_SHORT)
                    .show()
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    Toast.makeText(requireContext(), response.errorMessage, Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.signinResponse(credential)
                }
            }
        }
    }
    private fun inflatePasswordLessAlert(mPassword: String) {
        hideKeyboard(activity!!)
        if(popUpView != null){
            popUpView!!.visibility = View.VISIBLE
            return
        }

        if(blackBackground != null) {
                blackBackground!!.visibility = View.VISIBLE
        }
        val viewGroup = activity?.findViewById<View>(android.R.id.content) as ViewGroup
        inflate(this.context, R.layout.black_screen,viewGroup)
        goPasswordLessView = inflate(this.context, R.layout.passwordless_dialog, viewGroup)
        val goPasswordLessBtn = activity?.findViewById<Button>(R.id.go_passwordless_btn)
        val noThanksBtn = activity?.findViewById<Button>(R.id.no_thanks_btn)
        popUpView = activity?.findViewById(R.id.passwordless_whiteBackground_view)
        blackBackground = activity?.findViewById(R.id.black_screen_view)
        noThanksBtn?.setOnClickListener {
             removePasswordLessAlert()
        }
        goPasswordLessBtn?.setOnClickListener {
            removePasswordLessAlert()
           viewModel.submitPassword(mPassword)
        }
    }

    private fun removePasswordLessAlert() {
        if(popUpView != null) {
            if(popUpView!!.visibility == View.VISIBLE){
                popUpView?.visibility = View.GONE
            }
        }
        if(blackBackground != null){
            if(blackBackground!!.visibility == View.VISIBLE){
                blackBackground?.visibility = View.GONE
            }
        }
        moveCardViewDown()
    }
    private fun showKeyboard(activity: Activity){
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.showSoftInput(view, 0)
    }
    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}





