package com.example.verifonevx990app.init

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentCommunicationOptionBinding
import com.example.verifonevx990app.databinding.FragmentNewInputAmountBinding
import com.example.verifonevx990app.digiPOS.UpiSmsDynamicPayQrInputDetailFragment
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.vxUtils.VxEvent

class CommunicationOptionFragment : Fragment() {
    private var binding: FragmentCommunicationOptionBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCommunicationOptionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.comm_param)

        binding?.transComParamOption?.setOnClickListener {

            (activity as MainActivity).transactFragment(TableEditFragment().apply {
                arguments = Bundle().apply {
                    putInt("type", BankOptions.TXN_COMM_PARAM_TABLE.ordinal)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })


        }
        binding?.appUpdateComParamOption?.setOnClickListener {
            (activity as MainActivity).transactFragment(TableEditFragment().apply {
                arguments = Bundle().apply {
                    putInt("type", BankOptions.APP_UPDATE_COMM_PARAM_TABLE.ordinal)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })

        }
    }

}