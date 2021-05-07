/*package com.example.verifonevx990app.disputetransaction

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.*
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleToHost
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.vxUtils.*
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class VoidFragment : Fragment(R.layout.activity_void_view) {
    private var batchList: MutableList<BatchFileDataTable> = mutableListOf()
    private var tempbatchList: MutableList<BatchFileDataTable> = mutableListOf()
    private val voidAdapter: VoidAdapter by lazy { VoidAdapter(batchList) }
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var void_rv: RecyclerView? = null
    private var void_sale_btn: ExtendedFloatingActionButton? = null
    private var empty_view_placeholder: ImageView? = null
    private var lv_heading_view: LinearLayout? = null
    private var voidByteArray: ByteArray? = null
    private var back_image_button: ImageView? = null
    private var sub_header_text: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        void_rv = view.findViewById(R.id.void_rv)
        void_sale_btn = view.findViewById(R.id.void_sale_btn)
        back_image_button = view.findViewById(R.id.back_image_button)
        sub_header_text = view.findViewById(R.id.sub_header_text)
        back_image_button?.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
        }
        sub_header_text?.text = title
        void_sale_btn?.setOnClickListener(this::onContinueClicked)
        lv_heading_view = view.findViewById(R.id.lv_heading_view)
        empty_view_placeholder = view.findViewById(R.id.empty_view_placeholder)
        if (AppPreference.getIntData(PrefConstant.VOID_ROC_INCREMENT.keyName.toString()) == 0) {
            AppPreference.setIntData(PrefConstant.VOID_ROC_INCREMENT.keyName.toString(), 0)
        }
        getAndInflateVoidData()
    }

    //Method is to get Sale Batch Data for Void RecyclerView listing:-
    private fun getAndInflateVoidData(){
        batchList = BatchFileDataTable.selectBatchData()
        tempbatchList = BatchFileDataTable.selectBatchData()
        if(batchList.size == 0){
            conditionBasedShowHideViews(false)
        }else {
            conditionBasedShowHideViews(true)
            void_rv?.apply {
                layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                adapter = voidAdapter
                itemAnimator = DefaultItemAnimator()
            }
        }
    }

    private fun onContinueClicked(v: View) {
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            SyncVoidTransactionToHost(AppPreference.getReversal()) { status, responseCode, reversalMsg, printExtraData ->
                (context as MainActivity).hideProgress()
                if (status && responseCode == "00") {
                    GlobalScope.launch {
                        val batchList: MutableList<BatchFileDataTable> = voidAdapter.selectedbatchList
                        batchList.forEachIndexed{
                                index,
                                batchFileDataTable ->
                            if (batchFileDataTable.isChecked) {
                                delay(1000)
                                VoidAdapter.VoidHelper( activity as MainActivity,  batchFileDataTable) { code, respnosedatareader, msg ->
                                    GlobalScope.launch(Dispatchers.Main) {
                                        when (code) {
                                            0 -> {
                                                if (msg.isNotEmpty()) Toast.makeText(activity as Context, msg, Toast.LENGTH_SHORT).show()
                                                println("Index and batchListsize in fail is"+ index +" and "+ " batch " + (batchList.size-1))
                                                if(index == batchList.size-1){
                                                    refreshRecyclerViewItems(code, respnosedatareader) { b: Boolean, index: Int, batchlistsize: Int ->
                                                        val autoSettlementCheck = respnosedatareader.isoMap[60]?.parseRaw2String().toString()
                                                        syncOfflineSaleAndAskAutoSettlement(autoSettlementCheck.substring(0, 1))

                                                    }
                                                }

                                            }  //  Success case
                                            1 -> {
                                                println("Index and batchListsize in success is"+ index +" and "+ " batch " + (batchList.size-1))
                                                if(index == batchList.size-1){
                                                    refreshRecyclerViewItems(code, respnosedatareader) { b: Boolean, index: Int, batchlistsize: Int ->
                                                        val autoSettlementCheck = respnosedatareader.isoMap[60]?.parseRaw2String().toString()
                                                        syncOfflineSaleAndAskAutoSettlement(autoSettlementCheck.substring(0, 1))
                                                    }
                                                }
                                                else {
                                                    voidAdapter.notifyDataSetChanged()
                                                }
                                            }
                                        }
                                    }
                                }.start()
                                return@forEachIndexed
                            }

                        }
                    }

                }
                else if (status && responseCode != "00") {
                    GlobalScope.launch(Dispatchers.Main) {
                        VFService.showToast("$responseCode ------> $reversalMsg")
                    }
                }else {
                    GlobalScope.launch(Dispatchers.Main) {
                        (context as MainActivity).hideProgress()
                        VFService.showToast(reversalMsg)
                    }
                }
            }
        }
        else{
            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                GlobalScope.launch {
                    val batchList: MutableList<BatchFileDataTable> = voidAdapter.selectedbatchList
                    if(batchList.size!=0) {
                        batchList.forEachIndexed { index, batchFileDataTable ->
                            if (batchFileDataTable.isChecked) {
                                delay(1000)
                                VoidAdapter.VoidHelper(
                                    activity as MainActivity,
                                    batchFileDataTable
                                ) { code, respnosedatareader, msg ->
                                    GlobalScope.launch(Dispatchers.Main) {
                                        when (code) {
                                            0 -> {
                                                if (msg.isNotEmpty()) Toast.makeText(
                                                    activity as Context,
                                                    msg,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                if (index == batchList.size - 1) {
                                                    refreshRecyclerViewItems(
                                                        code,
                                                        respnosedatareader
                                                    ) { b: Boolean, index: Int, batchlistsize: Int ->
                                                        val autoSettlementCheck =
                                                            respnosedatareader.isoMap[60]?.parseRaw2String()
                                                                .toString()
                                                        syncOfflineSaleAndAskAutoSettlement(
                                                            autoSettlementCheck.substring(0, 1)
                                                        )

                                                    }
                                                }

                                            }  //  Success case
                                            1 -> {
                                                println("Index and batchListsize is" + index + " and " + " batch " + (batchList.size - 1))
                                                if (index == batchList.size - 1) {
                                                    refreshRecyclerViewItems(
                                                        code,
                                                        respnosedatareader
                                                    ) { b: Boolean, index: Int, batchlistsize: Int ->
                                                        val autoSettlementCheck =
                                                            respnosedatareader.isoMap[60]?.parseRaw2String()
                                                                .toString()
                                                        syncOfflineSaleAndAskAutoSettlement(
                                                            autoSettlementCheck.substring(0, 1)
                                                        )
                                                    }
                                                } else {
                                                    voidAdapter.notifyDataSetChanged()
                                                }
                                            }
                                        }
                                    }
                                }.start()
                                return@forEachIndexed
                            }

                        }
                    }else{
                        VFService.showToast("Select a Transaction for void")
                    }
                }
            }
        }

    }

    //Below method is used to Sync Offline Sale and Ask for Auto Settlement:-
    private fun syncOfflineSaleAndAskAutoSettlement(autoSettleCode: String) {
        val offlineSaleData = BatchFileDataTable.selectOfflineSaleBatchData()
        if (offlineSaleData.size > 0) {
            (context as MainActivity).runOnUiThread {  (context as MainActivity).showProgress( (context as MainActivity).getString(R.string.please_wait_offline_sale_sync)) }
            SyncOfflineSaleToHost { offlineSaleStatus ->
                if (offlineSaleStatus)
                    (context as MainActivity).runOnUiThread {
                        (context as MainActivity).hideProgress()
                        if (autoSettleCode == "1") {
                            (context as MainActivity).alertBoxWithAction(null, null,
                                (context as MainActivity).getString(R.string.batch_settle),
                                (context as MainActivity).getString(R.string.do_you_want_to_settle_batch),
                                true,  (context as MainActivity).getString(R.string.positive_button_yes), {
                                    (context as MainActivity).startActivity(
                                        Intent(
                                            (context as MainActivity),
                                            MainActivity::class.java
                                        ).apply {
                                            putExtra("appUpdateFromSale", true)
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                }, {
                                    (context as MainActivity).startActivity(
                                        Intent(
                                            (context as MainActivity),
                                            MainActivity::class.java
                                        ).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                })
                        } else {
                            (context as MainActivity).startActivity(
                                Intent( (context as MainActivity), MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }
                    }
                else
                    (context as MainActivity).runOnUiThread {
                        (context as MainActivity).hideProgress()
                        if (autoSettleCode == "1") {
                            (context as MainActivity).alertBoxWithAction(null, null,
                                (context as MainActivity).getString(R.string.batch_settle),
                                (context as MainActivity).getString(R.string.do_you_want_to_settle_batch),
                                true,  (context as MainActivity).getString(R.string.positive_button_yes), {
                                    (context as MainActivity).startActivity(
                                        Intent(
                                            (context as MainActivity),
                                            MainActivity::class.java
                                        ).apply {
                                            putExtra("appUpdateFromSale", true)
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                }, {
                                    (context as MainActivity).startActivity(
                                        Intent(
                                            (context as MainActivity),
                                            MainActivity::class.java
                                        ).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                })
                        } else {
                            (context as MainActivity).startActivity(
                                Intent( (context as MainActivity), MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                        }
                    }
            }
        } else {
            if (autoSettleCode == "1") {
                (context as MainActivity).alertBoxWithAction(null, null,
                    (context as MainActivity).getString(R.string.batch_settle),
                    (context as MainActivity).getString(R.string.do_you_want_to_settle_batch),
                    true,  (context as MainActivity).getString(R.string.positive_button_yes), {
                        (context as MainActivity).startActivity(
                            Intent( (context as MainActivity), MainActivity::class.java).apply {
                                putExtra("appUpdateFromSale", true)
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                    }, {
                        (context as MainActivity).startActivity(
                            Intent( (context as MainActivity), MainActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                    })
            } else {
                (context as MainActivity).startActivity(
                    Intent( (context as MainActivity), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
            }
        }
    }

    //Below method is used to refresh recyclerview with updated items:-
    private fun refreshRecyclerViewItems(transtatus: Int, respnosedatareader: IsoDataReader,callback: (Boolean,Int,Int) -> Unit){
Delete Data From BatchFileTableList which are checked and synced to Host Successfully:-

       var invoiceno = respnosedatareader.isoMap[62]?.parseRaw2String().toString()
        var i=0
        val tempDataList  = mutableListOf<BatchFileDataTable>()

        while (batchList.size > 0){
            if(i == batchList.size)
                break
            if(batchList[i].isChecked && batchList[i].isVoid){
                deleteBatchTableDataInDBWithInvoiceNumber(batchList[i].invoiceNumber)
                //
                batchList.removeAt(i)
            }else{
                tempDataList.add(batchList[i])
                i++
            }
        }
        //Here we inflate Refreshed BatchFileData List in RecyclerView with the help of RecyclerViewUpdateUtils class:-
        voidAdapter.updateVoidAdapter(tempDataList)

        //Below we are saving void batch data which are checked by user and void successfully:-
        ROCProviderV2.saveBatchInPreference(tempDataList)
        callback(true,i,batchList.size)
    }

    //Below method is used to show/hide views on basis of recyclerview data list size:-
    private fun conditionBasedShowHideViews(showStatus: Boolean) {
        if(!showStatus){
            lv_heading_view?.visibility = View.GONE
            void_sale_btn?.visibility = View.GONE
            void_rv?.visibility = View.GONE
            empty_view_placeholder?.visibility = View.VISIBLE
        }else{
            lv_heading_view?.visibility = View.VISIBLE
            void_sale_btn?.visibility = View.VISIBLE
            void_rv?.visibility = View.VISIBLE
            empty_view_placeholder?.visibility = View.GONE
        }
    }

}

internal class VoidAdapter(private val list: MutableList<BatchFileDataTable>) : RecyclerView.Adapter<VoidAdapter.VoidHolder>() {
    var selectedbatchList  = mutableListOf<BatchFileDataTable>()
    private var batchDataList = mutableListOf<BatchFileDataTable>()
    companion object {
        val TAG = VoidAdapter::class.java.simpleName
    }

    init {
        this.batchDataList.addAll(list)
    }

    fun updateVoidAdapter(newBatchFileList: MutableList<BatchFileDataTable>) {
        val diffResult = DiffUtil.calculateDiff(RecyclerViewUpdateUtils(this.batchDataList, newBatchFileList))
        this.batchDataList.clear()
        this.selectedbatchList.clear()
        this.batchDataList.addAll(newBatchFileList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): VoidHolder {
        val inflater = LayoutInflater.from(p0.context).inflate(R.layout.item_void , p0 , false)
        return VoidHolder(inflater)
    }

    override fun getItemCount(): Int {
        return batchDataList.size
    }

    override fun onBindViewHolder(p0: VoidHolder, position: Int) {
        val batchModal = batchDataList[position]
        p0.invoiceText.text = invoiceWithPadding(batchModal.invoiceNumber)

        val amount = "%.2f".format(batchModal.transactionalAmmount.toFloat()/100)
        p0.baseAmountText.text = amount
        p0.transactionDateText.text = batchModal.transactionDate

        //CheckBox onClickEvent:-
        p0.voidCheckBox.setOnClickListener { v ->
            val checkBox = v as CheckBox
            val batchFileData = checkBox.tag as BatchFileDataTable
            if (checkBox.isChecked) {
                batchFileData.isChecked = true
                selectedbatchList.add(batchFileData)
                notifyDataSetChanged()
            } else {
                batchFileData.isChecked = false
                if(null !=selectedbatchList && selectedbatchList.size > 0)
                   selectedbatchList.remove(batchFileData)
                notifyDataSetChanged()
            }

        }

        p0.voidCheckBox.tag = batchModal
        p0.voidCheckBox.isChecked = batchModal.isChecked
    }


    inner class VoidHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val invoiceText = view.findViewById<TextView>(R.id.tv_invoice_number)
        val baseAmountText = view.findViewById<TextView>(R.id.tv_base_amount)
        val transactionDateText = view.findViewById<TextView>(R.id.tv_transaction_date)
        val voidCheckBox = view.findViewById<CheckBox>(R.id.checkbox)
    }

    internal class VoidHelper(val context: Activity, val batch: BatchFileDataTable, private val callback: (Int, IsoDataReader, String) -> Unit) {
        companion object
        val TAG = VoidHelper::class.java.simpleName
        fun start() {
            GlobalScope.launch {
                val transactionISO = CreateVoidPacket(batch).createVoidISOPacket()
                //logger1("Transaction REQUEST PACKET --->>", transactionISO.generateIsoByteRequest(), "e")
                (context as MainActivity).runOnUiThread { (context).showProgress((context).getString(R.string.void_data_sync)) }
                GlobalScope.launch(Dispatchers.Main) {
                    checkReversal(transactionISO)
                }
            }
        }

        private fun checkReversal(transactionISOByteArray: IsoDataWriter) {

            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
              //  (context as MainActivity).showProgress((context).getString(R.string.please_wait_offline_sale_sync))
                SyncVoidTransactionToHost(
                    transactionISOByteArray,
                    cardProcessedDataModal = CardProcessedDataModal()
                ) { syncStatus, responseCode, transactionMsg, printExtraData ->
                    (context as MainActivity).hideProgress()
                    if (syncStatus && responseCode == "00") {
                        val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                        batch.isVoid = true
                        //   batch.isChecked = false
                        callback(
                            1,
                            responseIsoData,
                            responseIsoData.isoMap[39]?.parseRaw2String().toString()
                        )
                    } else if (syncStatus && responseCode != "00") {
                        GlobalScope.launch(Dispatchers.Main) {
                            VFService.showToast("$responseCode ------> $transactionMsg")
                            val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                            callback(
                                0,
                                responseIsoData,
                                responseIsoData.isoMap[39]?.parseRaw2String().toString()
                            )

                        }
                    } else {
                        (context).runOnUiThread {
                            (context).hideProgress()
                        }
                        val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                        callback(0, responseIsoData, "")
                    }
                }
            }
        }

    }


}*/
