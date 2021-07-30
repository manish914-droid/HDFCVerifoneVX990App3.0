package com.example.verifonevx990app.init

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentCommunicationOptionBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.TerminalCommunicationTable
import com.example.verifonevx990app.vxUtils.VFService

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
                   putString("heading", "TXN PARM")
                }
            })


        }
        binding?.appUpdateComParamOption?.setOnClickListener {
            if (TerminalCommunicationTable.selectCommTableByRecordType("2") != null) {
                (activity as MainActivity).transactFragment(TableEditFragment().apply {
                    arguments = Bundle().apply {
                        putInt("type", BankOptions.APP_UPDATE_COMM_PARAM_TABLE.ordinal)
                      putString("heading", "APP UPDATE PARAM")
                    }
                })
            }
            else{
                VFService.showToast(
                    "No App update param available"
                )
            }
        }
    }

}