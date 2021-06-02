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
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*

class PendingPreAuthFragment : Fragment() {

    private val authData: AuthCompletionData by lazy { AuthCompletionData() }

    val preAuthDataList by lazy {
        arguments?.getSerializable("PreAuthData") as ArrayList<PendingPreauthData>
    }

    val cardProcessData by lazy {
        arguments?.getSerializable("CardProcessData") as CardProcessedDataModal
    }

    //creating our adapter
    val mAdapter by lazy {
        PendingPreauthAdapter(preAuthDataList) { data, position ->

            onTouchViewShowDialog(data, position)
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

    private fun onTouchViewShowDialog(pendingPreauthData: PendingPreauthData, position: Int) {

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
        bindingg.enterAmountView.visibility = View.VISIBLE
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
        var isClicablebtn = false

        /*  bindingg.amountEt.addTextChangedListener(OnTextChange {
              //  binding?.ifProceedBtn?.isEnabled = it.length == 8
               if(it.toFloat()>=1) {
                   bindingg.completeBtnn.setShapeType(ShapeType.FLAT)
                   isClicablebtn=true
                   bindingg.completeBtnn.setTextColor(Color.WHITE)
               }else{
                   bindingg.completeBtnn.setShapeType(ShapeType.PRESSED)
                 isClicablebtn=false
                   bindingg.completeBtnn.setTextColor(Color.WHITE)

               }
              //actionDone( view.if_et)
          })*/

        bindingg.amountEt.setOnClickListener {
            showEditTextSelected(bindingg.amountEt, bindingg.enterAmountView, requireContext())
        }


        bindingg.printBtnn.setOnClickListener {
            val arList = arrayListOf<PendingPreauthData>()
            arList.add(pendingPreauthData)

            printReceipt(arList, dialogBuilder)

        }

        bindingg.completeBtnn.setOnClickListener {
            //  VFService.showToast("COMP")
            if (bindingg.amountEt.text.toString().toFloat() >= 1) {
                val tpt = TerminalParameterTable.selectFromSchemeTable()
                authData.authTid = tpt?.terminalId

                authData.authAmt = bindingg.amountEt.text.toString()
                //   authData.authAmt = "%.2f".format(pendingPreauthData.amount)
                authData.authBatchNo = invoiceWithPadding(pendingPreauthData.batch.toString())
                authData.authRoc = invoiceWithPadding(pendingPreauthData.roc.toString())
                GlobalScope.async(Dispatchers.IO) {
                    confirmCompletePreAuth(authData, position)

                }
                dialogBuilder.hide()
            } else {
                VFService.showToast("Amount should be greater than 1 rs")

            }
        }
        dialogBuilder.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }


    private suspend fun confirmCompletePreAuth(
        authCompletionData: AuthCompletionData,
        position: Int
    ) {
        var isSuccessComp = false
        val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
        val transactionalAmount = authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.PRE_AUTH_COMPLETE.type)
            setProcessingCode(ProcessingCode.PRE_SALE_COMPLETE.code)
            setAuthBatch(authCompletionData.authBatchNo.toString())
            setAuthRoc(authCompletionData.authRoc.toString())
            setAuthTid(authCompletionData.authTid.toString())
        }
        val transactionISO = CreateAuthPacket().createPreAuthCompleteAndVoidPreauthISOPacket(
            authCompletionData,
            cardProcessedData
        )
        //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
        try {
            val date2: Long = Calendar.getInstance().timeInMillis
            val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
            cardProcessedData.setTime(timeFormater.format(date2))
            val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
            cardProcessedData.setDate(dateFormater.format(date2))
            cardProcessedData.setTimeStamp(date2.toString())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        //    logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        var gg = GlobalScope.async(Dispatchers.IO) {
            activity?.let {
                SyncAuthTransToHost(it as BaseActivity).checkReversalPerformAuthTransaction(
                    transactionISO, cardProcessedData
                ) { isSuccess, msg ->

                    if (isSuccess) {
                        mAdapter.refreshListRemoveAt(position)
                    }
                    VFService.showToast("$msg----------->  $isSuccess")
                    logger("PREAUTHCOMP", "Is success --->  $isSuccess  Msg --->  $msg")
                    //   parentFragmentManager.popBackStackImmediate()
                }
            }
        }
        gg.await()


    }


}

class PendingPreauthAdapter(
    val pendPreauthData: ArrayList<PendingPreauthData>,
    var ontouchView: (PendingPreauthData, Int) -> Unit
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
            ontouchView(pendPreauthData[position], position)

            /* if( ontouchView(pendPreauthData[position])) {
                 pendPreauthData.removeAt(position);
                 notifyItemRemoved(position);
                 notifyItemRangeChanged(position, pendPreauthData.size);
             }*/
        }

    }


    fun refreshListRemoveAt(position: Int) {
        pendPreauthData.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, pendPreauthData.size)
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