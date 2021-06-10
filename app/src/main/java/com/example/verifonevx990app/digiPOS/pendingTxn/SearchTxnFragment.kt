package com.example.verifonevx990app.digiPOS.pendingTxn

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.databinding.FragmentSearchTxnBinding
import com.example.verifonevx990app.vxUtils.VFService

class SearchTxnFragment : Fragment() {
    var binding: FragmentSearchTxnBinding? = null
    var pendingFragListner: IPendingTxnListner? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IPendingTxnListner) {
            pendingFragListner = context
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearchTxnBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.searchTxnBtn?.setOnClickListener {
            validateAndHitServer()

        }
    }

    private fun validateAndHitServer() {
        val txn_id_String = binding?.txnIdSearchET?.text.toString()
        if (txn_id_String.length > 2) {
            pendingFragListner?.getTxnStatus()

        } else
            VFService.showToast("NO Txn ID")


    }


}

