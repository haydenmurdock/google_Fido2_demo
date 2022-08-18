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

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.fido2.R
import com.example.fido2.databinding.UsernameFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class UsernameFragment : Fragment(){

    private val viewModel: UsernameViewModel by viewModels()
    private lateinit var binding: UsernameFragmentBinding
    private var cardIsUp = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = UsernameFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.sending.collect { sending ->
                viewModel.goToHomeScreen("Hayden")
            }
        }
        val userNameEditText = activity?.findViewById<EditText>(R.id.editTextPersonName)
        val confirmBtn = activity?.findViewById<ImageButton>(R.id.confirmation_btn)
        val progressbar = activity?.findViewById<ProgressBar>(R.id.sending_userName_loader)

        userNameEditText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                moveCardViewUp()
            }
            if (!hasFocus && cardIsUp) {
                println("card is up and focus is gone")
                moveCardViewDown()
            }
        }

        confirmBtn?.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    print("confirm Button Pressed")
                    viewModel.sendUsername(userNameEditText?.text.toString())
                    confirmBtn.isEnabled = false
                   confirmBtn.setImageResource(android.R.color.transparent)
                    progressbar?.visibility = View.VISIBLE
                    progressbar?.animate()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        userNameEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if(s == null){ return}
              if (s.count() >= 3) {
                  showConfirmBtn(confirmBtn)
              } else {
                  hideConfirmBtn(confirmBtn)
              }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


    }
//        binding.inputUsername.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_NEXT) {
//                viewModel.sendUsername()
//                true
//            } else {
//                false
//            }
//        }

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
        params.topMargin = 150
        cardLayout.requestLayout()
        cardIsUp = true
    }
    private fun moveCardViewDown(){
        val cardLayout =
            activity?.findViewById<ConstraintLayout>(R.id.log_in_constraintLayout)
        val params = cardLayout?.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = 300
        cardLayout.requestLayout()
        cardIsUp = false
    }
}
