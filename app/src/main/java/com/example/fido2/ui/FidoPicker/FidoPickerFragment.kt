package com.example.fido2.ui.FidoPicker


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.widget.Button
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.fido2.R

class FidoPickerFragment : Fragment() {

    companion object {
        fun newInstance() = FidoPickerFragment()
    }

   private val viewModel: FidoPickerViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fido_picker, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
                val fido2Btn: Button = activity!!.findViewById(R.id.FIDO_2_btn)
                val uafBtn: Button = activity!!.findViewById(R.id.UAF_btn)

            fido2Btn.setOnClickListener{
                viewModel.userSelectedFido2()
            }

            uafBtn.setOnClickListener {
                viewModel.userSelectedFido2()
            }
        }


    }


}