package com.example.verifonevx990app.preAuth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentPendingPreAuthBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.BHTextView
import com.example.verifonevx990app.vxUtils.invoiceWithPadding

class PendingPreAuthFragment : Fragment() {

    val preAuthDataList by lazy {
        arguments?.getSerializable("PreAuthData") as ArrayList<PendingPreauthData>
    }

    val cardProcessData by lazy {
        arguments?.getSerializable("CardProcessData") as CardProcessedDataModal
    }

    //creating our adapter
    val mAdapter by lazy {
        PendingPreauthAdapter(preAuthDataList)
    }

    private var binding: FragmentPendingPreAuthBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingPreAuthBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.pending_pre_auth)

        binding?.pendingPreAuthPrintBtn?.setOnClickListener {
            PrintUtil(context).printPendingPreauth(
                cardProcessData,
                context,
                preAuthDataList
            ) { printCB ->
                if (!printCB) {
                    //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                    //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-
                    /*   if (!TextUtils.isEmpty(autoSettlementCheck))
                           syncOfflineSaleAndAskAutoSettlement(
                               autoSettlementCheck.substring(
                                   0,
                                   1
                               ), context as BaseActivity
                           )*/
                }

            }


        }

        binding?.pendingPreRv?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
        }
    }

}

class PendingPreauthAdapter(val pendPreauthData: ArrayList<PendingPreauthData>) :
    RecyclerView.Adapter<PendingPreauthAdapter.PendingPreAuthViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingPreAuthViewHolder {
        return PendingPreAuthViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pending_preauth, parent, false)
        )
    }

    override fun getItemCount(): Int = pendPreauthData.size

    override fun onBindViewHolder(holder: PendingPreAuthViewHolder, position: Int) {

        val batchNo = "BATCH NO : " + invoiceWithPadding(pendPreauthData[position].batch.toString())
        holder.batchNoTv?.text = batchNo
        val roc = "ROC : " + invoiceWithPadding(pendPreauthData[position].roc.toString())
        holder.rocTV?.text = roc

        val pan = "PAN : " + pendPreauthData[position].pan
        holder.panNoTv?.text = pan
        val amt = "AMT : " + "%.2f".format(pendPreauthData[position].amount)
        holder.amtTv?.text = amt

        val date = "DATE : " + pendPreauthData[position].date
        holder.dateTv?.text = date
        val time = "TIME : " + pendPreauthData[position].time
        holder.timeTv?.text = time

    }

    class PendingPreAuthViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var batchNoTv = view.findViewById<BHTextView>(R.id.batch_no_tv)
        var rocTV = view.findViewById<BHTextView>(R.id.roc_tv)
        var panNoTv = view.findViewById<BHTextView>(R.id.pan_no_tv)
        var amtTv = view.findViewById<BHTextView>(R.id.amt_tv)
        var dateTv = view.findViewById<BHTextView>(R.id.date_tv)
        var timeTv = view.findViewById<BHTextView>(R.id.time_tv)
    }
}