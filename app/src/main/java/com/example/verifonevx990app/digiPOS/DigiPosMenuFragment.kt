package com.example.verifonevx990app.digiPOS

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.databinding.FragmentDigiPosMenuBinding
import com.example.verifonevx990app.digiPOS.pendingTxn.PendingTxnFragment
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.DigiPosDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.logger
import com.google.gson.Gson


class DigiPosMenuFragment : Fragment() {

    val tpt by lazy {
        TerminalParameterTable.selectFromSchemeTable()
    }
    private var binding: FragmentDigiPosMenuBinding? = null
    private lateinit var transactionType: EDashboardItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDigiPosMenuBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.headerImage?.visibility = View.VISIBLE
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)
        binding?.subHeaderView?.subHeaderText?.text = transactionType.title

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        if (tpt?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            binding?.upiBtn?.visibility = View.VISIBLE
        }
        if (tpt?.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            binding?.staticQrBtn?.visibility = View.VISIBLE
            binding?.dynamicQrBtn?.visibility = View.VISIBLE
        }
        if (tpt?.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            binding?.smsPayBtn?.visibility = View.VISIBLE
        }

        binding?.smsPayBtn?.visibility = View.VISIBLE
        setClickListener()
    }

    private fun setClickListener() {
        //UPI CLICK
        binding?.upiBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(UpiSmsPayEnterDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.UPI)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }
        //sms click
        binding?.smsPayBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(UpiSmsPayEnterDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.SMS_PAY)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }

        binding?.dynamicQrBtn?.setOnClickListener {
            DigiPosDataTable.clear()
            val dd=DigiPosDataTable.selectAllDigiPosData()
            val jsonData=Gson().toJson(dd)
            logger("DIGI EMPTY",jsonData,"e")

        }
// pending transaction
        binding?.pendingTxnBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(PendingTxnFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.PENDING_TXN)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }

        //region===========txn List Click:-
        binding?.txnListBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(DigiPosTxnListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.TXN_LIST)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion

    }

}