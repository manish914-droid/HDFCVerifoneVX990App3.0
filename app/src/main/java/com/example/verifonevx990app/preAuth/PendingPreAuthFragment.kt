package com.example.verifonevx990app.preAuth

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentPendingPreAuthBinding
import com.example.verifonevx990app.databinding.ItemPendingPreauthBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.BHTextView
import com.example.verifonevx990app.vxUtils.BaseActivity
import com.example.verifonevx990app.vxUtils.VFService
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
        PendingPreauthAdapter(preAuthDataList) {
            onTouchViewShowDialog(it)
        }
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
            printReceipt(preAuthDataList)


        }

        binding?.pendingPreRv?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
        }
    }

    private fun printReceipt(
        preauthDatalist: ArrayList<PendingPreauthData>,
        dialog: Dialog? = null
    ): Boolean {
        (activity as BaseActivity).showProgress(getString(R.string.printing))
        var printsts = false
        PrintUtil(context).printPendingPreauth(
            cardProcessData,
            context,
            preauthDatalist
        ) { printCB ->
            (activity as BaseActivity).hideProgress()
            if (printCB) {
                printsts = printCB
                dialog?.dismiss()
            } else {
                VFService.showToast(getString(R.string.printer_error))
            }
        }
        return printsts
    }

    private fun onTouchViewShowDialog(pendingPreauthData: PendingPreauthData): Boolean {
        var isSuccess = false
        val dialogBuilder = Dialog(requireActivity())
        //  builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = ItemPendingPreauthBinding.inflate(LayoutInflater.from(context))

        dialogBuilder.setContentView(bindingg.root)

        dialogBuilder.setCancelable(true)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        bindingg.preautnPendingBtnsView.visibility = View.VISIBLE
        val batchNo = "BATCH NO : " + invoiceWithPadding(pendingPreauthData.batch.toString())
        bindingg.batchNoTv.text = batchNo
        val roc = "ROC : " + invoiceWithPadding(pendingPreauthData.roc.toString())
        bindingg.rocTv.text = roc
        val pan = "PAN : " + pendingPreauthData.pan
        bindingg.panNoTv.text = pan
        val amt = "AMT : " + "%.2f".format(pendingPreauthData.amount)
        bindingg.amtTv.text = amt
        val date = "DATE : " + pendingPreauthData.date
        bindingg.dateTv.text = date
        val time = "TIME : " + pendingPreauthData.time
        bindingg.timeTv.text = time

        bindingg.printBtnn.setOnClickListener {
            val arList = arrayListOf<PendingPreauthData>()
            arList.add(pendingPreauthData)

            printReceipt(arList, dialogBuilder)

        }

        bindingg.completeBtnn.setOnClickListener {

        }

        dialogBuilder.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))



        return isSuccess
    }


}

class PendingPreauthAdapter(
    val pendPreauthData: ArrayList<PendingPreauthData>,
    var ontouchView: (PendingPreauthData) -> Boolean
) :
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

        holder.cardView.setOnClickListener {
            ontouchView(pendPreauthData[position])
        }

    }

    class PendingPreAuthViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var batchNoTv = view.findViewById<BHTextView>(R.id.batch_no_tv)
        var rocTV = view.findViewById<BHTextView>(R.id.roc_tv)
        var panNoTv = view.findViewById<BHTextView>(R.id.pan_no_tv)
        var amtTv = view.findViewById<BHTextView>(R.id.amt_tv)
        var dateTv = view.findViewById<BHTextView>(R.id.date_tv)
        var timeTv = view.findViewById<BHTextView>(R.id.time_tv)
        var cardView = view.findViewById<NeumorphCardView>(R.id.pending_pre_auth_crdView)
    }
}