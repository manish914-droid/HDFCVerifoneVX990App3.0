package com.example.verifonevx990app.init

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankemi.TestEmiOptionFragment
import com.example.verifonevx990app.databinding.FragmentSubmenuBinding
import com.example.verifonevx990app.digiPOS.QrScanFragment
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.main.SubHeaderTitle
import com.example.verifonevx990app.offlinemanualsale.OfflineSalePrintReceipt
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.transactions.NewInputAmountFragment
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.voidrefund.VoidRefundSalePrintReceipt
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EOptionGroup(val heading: String) {
    FUNCTIONS("BANK FUNCTIONS"), REPORT("REPORT"), NONE("NONE")
}

enum class BankOptions(val _name: String, val group: String, val res: Int = 0) {
    INITT("INIT", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_init),
    DOWNLOAD_TMK("Download TMK", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_key_exchange),
    TEST_EMI("Test EMI", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_brand_emi),
    TPT("Terminal Param", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_tpt_img),
    CPT("Com Param", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_copt),
    ENV("ENV Param", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_env),

    // CDT("CDT Param", EOptionGroup.FUNCTIONS, R.drawable.ic_cdt),
    //  IPT("IPT Param", EOptionGroup.FUNCTIONS, R.drawable.ic_ipt),
    CR("Clear Reversal", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_clear_reversal),
    CB("Clear Batch", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_clear_batch),
    APPUPDATE("Application Update", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_app_update),

    LAST_RECEIPT("Last Receipt", EOptionGroup.REPORT.heading, R.drawable.ic_last_receipt),
    LAST_CANCEL_RECEIPT("Last Cancel Receipt", EOptionGroup.REPORT.heading, R.drawable.ic_clear_reversal),
    ANY_RECEIPT("Any Receipt", EOptionGroup.REPORT.heading, R.drawable.ic_any_report),
    DETAIL_REPORT("Detail Report", EOptionGroup.REPORT.heading, R.drawable.ic_detail_report),
    SUMMERY_REPORT("Summary Report", EOptionGroup.REPORT.heading, R.drawable.ic_summer_report),
    LAST_SUMMERY_REPORT("Last Summary Report", EOptionGroup.REPORT.heading, R.drawable.ic_summer_report),

    HOME("", EOptionGroup.NONE.heading),
    INIT("", EOptionGroup.NONE.heading),
    KEY_EXCHANGE("", EOptionGroup.NONE.heading),
    TMK_EXCHANGE_HDFC("", EOptionGroup.NONE.heading),
    TXN_COMM_PARAM_TABLE("TXN Com Param", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_copt),
    APP_UPDATE_COMM_PARAM_TABLE("App Update Com Param", EOptionGroup.FUNCTIONS.heading, R.drawable.ic_copt);

    override fun toString(): String {
        return "[$_name, $group]"
    }

}

//region===========Table Editing===================

class TableEditFragment : Fragment() {

    private val dataList = ArrayList<TableEditHelper>()
    private var type: Int = 0
    private var typeId = ""
    private val mAdapter = TableEditAdapter(dataList, ::itemClicked)

    companion object {
        private val TAG = TableEditFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        type = arguments?.getInt("type") ?: 0
        typeId = arguments?.getString("id") ?: ""
        val v = inflater.inflate(R.layout.fragment_table_edit, container, false)
        initUI(v)
        return v
    }


    private fun updateTable() {
        val data = dataList.filter { it.isUpdated }
        val table: Any? = getTable()

        if (table != null) {
            if (data.isNotEmpty()) {
                data.forEach { ed ->
                    ed.isUpdated = false
                    val props = table::class.java.declaredFields
                    for (prop in props) {
                        val ann = prop.getAnnotation(BHFieldName::class.java)
                        if (ann != null && ann.name == ed.titleName) {
                            prop.isAccessible = true
                            val value = prop.get(table)
                            if (value is String) {
                                prop.set(table, ed.titleValue)
                            }
                        }
                    }
                }

                /*Condition to check whether terminal id is
                changed by user if so then we need to Navigate user to
                MainActivity and auto perform fresh init with new terminal id:-
                 */

                //Below conditional code will only execute in case of Change TID:-
                if (data[0].titleName.equals("Terminal ID", ignoreCase = true)) {
                    if (data[0].titleValue != TerminalParameterTable.selectFromSchemeTable()?.terminalId
                        && data[0].titleName.equals("Terminal ID", ignoreCase = true)
                    ) {
                        if (data[0].titleValue.length == 8) {
                            TerminalParameterTable.updateTerminalID(data[0].titleValue)
                            startActivity(Intent(context, MainActivity::class.java).apply {
                                putExtra("changeTID", true)
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        } else {
                            VFService.showToast(getString(R.string.enter_terminal_id_must_be_valid_8digit))
                        }
                    }
                }

                //Below conditional code will only execute in case of cLEAR FBATCH :-
                if (data[0].titleName.equals("CLEAR FBATCH", ignoreCase = true)) {
                    if (data[0].titleValue == "0" && data[0].titleName.equals(
                            "CLEAR FBATCH",
                            ignoreCase = true
                        )
                    ) {
                        AppPreference.saveBoolean(
                            PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                            false
                        )
                    } else {
                        AppPreference.saveBoolean(
                            PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                            true
                        )
                    }
                }


                when (table) {
                    is TerminalParameterTable -> {
                        table.actionId = "1"
                        TerminalParameterTable.performOperation(table) {
                            logger(TAG, "Terminal parameter Table updated successfully")
                        }
                    }
                    is TerminalCommunicationTable -> {
                        table.actionId = "1"
                        TerminalCommunicationTable.performOperation(table) {
                            logger(TAG, "Terminal Communication Table updated successfully")
                        }
                    }
                    is IssuerParameterTable -> {
                        table.actionId = "1"
                        IssuerParameterTable.performOperation(table) {
                            logger(TAG, "Issuer Parameter Table updated successfully")
                        }
                    }
                    is CardDataTable -> {
                        table.actionId = "1"
                        CardDataTable.performOperation(table) {
                            logger(TAG, "Card Data Table updated successfully")
                        }
                    }
                    else -> logger(TAG, "None of the type is found to update")
                }
            } else logger(TAG, "No data to update is found")
        }
    }

    private fun itemClicked(position: Int) {
        /*Below condition is executed only in case when user try to change terminal id ,
        So we need to check below condition and then perform fresh init with new terminal id:-
        1.Batch should be empty or settled
        2.Server Hit Status should be false
        3.Reversal should be cleared or synced to host successfully.
        */
        if (dataList[position].titleValue == TerminalParameterTable.selectFromSchemeTable()?.terminalId.toString()) {
            verifySuperAdminPasswordDialog(requireActivity()) { success ->
                if (success) {
                    val batchData = BatchFileDataTable.selectBatchData()
                    when {
                        AppPreference.getBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString()) ->
                            VFService.showToast(getString(R.string.please_clear_fbatch_before_init))

                        !TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)) ->
                            VFService.showToast(getString(R.string.reversal_found_please_clear_or_settle_first_before_init))

                        batchData.size > 0 -> VFService.showToast(getString(R.string.please_settle_batch_first_before_init))
                        else -> {
                            updateTPTOptionsValue(position, dataList[position].titleName)
                        }
                    }
                }
            }
        } else {
            updateTPTOptionsValue(position, dataList[position].titleName)
        }
    }

    //Below code is to perform update of values in TPT Options:-
    private fun updateTPTOptionsValue(position: Int, titleName: String) {
        getInputDialog(
            context as Context,
            getString(R.string.update),
            dataList[position].titleValue
        ) {
            if (titleName.equals("Terminal ID", ignoreCase = true)) {
                when {
                    it == dataList[position].titleValue -> {
                        VFService.showToast("TID Unchanged")
                    }
                    it.length < 8 -> {
                        VFService.showToast("Please enter a valid 8 digit TID")
                    }
                    else -> {
                        dataList[position].titleValue = it
                        dataList[position].isUpdated = true
                        mAdapter.notifyItemChanged(position)
                        GlobalScope.launch {
                            updateTable()
                        }

                    }
                }

            } else {
                dataList[position].titleValue = it
                dataList[position].isUpdated = true
                mAdapter.notifyItemChanged(position)
                GlobalScope.launch {
                    updateTable()
                }
            }
        }
    }


    private fun getTable(): Any? = when (type) {
        BankOptions.TPT.ordinal -> TerminalParameterTable.selectFromSchemeTable()
        BankOptions.CPT.ordinal -> TerminalCommunicationTable.selectFromSchemeTable()
        BankOptions.TXN_COMM_PARAM_TABLE.ordinal -> TerminalCommunicationTable.selectCommTableByRecordType("1")
        BankOptions.APP_UPDATE_COMM_PARAM_TABLE.ordinal -> TerminalCommunicationTable.selectCommTableByRecordType("2")

        /* BankOptions.IPT.ordinal -> IssuerParameterTable.selectFromIssuerParameterTable(typeId)
         BankOptions.CDT.ordinal -> CardDataTable.selecteAllCardsData()
             .first { it.cardTableIndex == typeId }*/
        else -> null
    }

    private fun initUI(v: View) {
        GlobalScope.launch {
            val table: Any? = getTable()
            if (table != null) {
                val props = table::class.java.declaredFields
                for (prop in props) {
                    val ann = prop.getAnnotation(BHFieldName::class.java)
                    if (ann != null && ann.isToShow) {
                        prop.isAccessible = true
                        val value = prop.get(table)
                        if (value is String) {
                            dataList.add(TableEditHelper(ann.name, value))
                        }
                    }
                }

                //Adding Extra Field Locally [Clear Server Hit Flag] for internal use purpose:-

                if (type == BankOptions.TPT.ordinal) {
                    dataList.add(
                        TableEditHelper(
                            "Clear FBatch",
                            if (AppPreference.getBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString()))
                                "1"
                            else
                                "0"
                        )
                    )
                    //In Case Of AMEX only below arrayList items options are shown to user (In TPT table)
                    val requiredField = arrayListOf(
                        "Terminal Id",
                        "Merchant Id",
                        "STAN",
                        "Batch Number",
                        "Invoice Number",
                        "Clear FBatch"
                    )
                    val requiredList =
                        dataList.filter { dl -> requiredField.any { rf -> rf == dl.titleName } } as ArrayList<TableEditHelper>
                    dataList.clear()
                    dataList.addAll(requiredList)
                }
                launch(Dispatchers.Main) {
                    v.findViewById<RecyclerView>(R.id.table_edit_rv).apply {
                        layoutManager = LinearLayoutManager(activity)
                        adapter = mAdapter
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

}


internal class TableEditAdapter(val data: List<TableEditHelper>, val callback: (Int) -> Unit) :
    RecyclerView.Adapter<TableEditAdapter.TableEditHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): TableEditHolder = TableEditHolder(
        LayoutInflater.from(p0.context)
            .inflate(R.layout.item_edit_table, p0, false)
    )

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(p0: TableEditHolder, p1: Int) {
        p0.run {
            titleTv.text = data[p1].titleName
            valueTv.text = data[p1].titleValue
        }
    }

    inner class TableEditHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv = view.findViewById<TextView>(R.id.t_field_tv)
        val valueTv = view.findViewById<TextView>(R.id.t_value_tv)

        init {
            view.findViewById<View>(R.id.edit_ll).setOnClickListener {
                callback(adapterPosition)
                //   true
            }
        }
    }

}

class TableEditHelper(
    var titleName: String,
    var titleValue: String,
    var isUpdated: Boolean = false
) :
    Comparable<TableEditHelper> {
    override fun compareTo(other: TableEditHelper): Int =
        if (titleName > other.titleName) 1 else if (titleName < other.titleName) -1 else 0
}


//endregion


//region=========Setting for Sub menus==================

class SubMenuFragment : Fragment(), IOnSubMenuItemSelectListener {



    private var iDiag: IDialog? = null

    private val optionList by lazy { mutableListOf<BankOptions>() }
    private val mAdapter by lazy { SubMenuFragmentAdapter(optionList, this) }
    private val option by lazy { arguments?.getSerializable("option") as EOptionGroup }
    private var binding: FragmentSubmenuBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        for (e in BankOptions.values()) {
            if (e.group == option.heading) {
                optionList.add(e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSubmenuBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
    }

    private fun initUI(v: View) {
        iDiag?.onEvents(VxEvent.ChangeTitle(option.name))
        binding?.fSmTitleTv?.text = option.heading

        binding?.fSmRv?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDiag = context
    }

    override fun onDetach() {
        super.onDetach()
        iDiag = null
    }


    override fun onSubmenuItemSelected(type: BankOptions, data: Any?) {
        if (type.group == EOptionGroup.FUNCTIONS.heading) {
            when (type) {
                BankOptions.INITT -> {
                    iDiag?.onEvents(VxEvent.InitTerminal)
                }

                BankOptions.APPUPDATE -> {
                    iDiag?.onEvents(VxEvent.AppUpdate)
                }

                BankOptions.DOWNLOAD_TMK -> {
                    verifySuperAdminPasswordDialog(requireActivity()) { correctPasswordSuccess ->
                        if (correctPasswordSuccess) {
                            iDiag?.alertBoxWithAction(null, null,
                                getString(R.string.download_tmk),
                                getString(R.string.do_you_want_to_download_tmk),
                                true,
                                getString(R.string.yes),
                                { iDiag?.onEvents(VxEvent.DownloadTMKForHDFC) },
                                { Log.d("NO:- ", "Clicked") })
                        }
                    }
                }

                BankOptions.TEST_EMI -> {
                    if (checkInternetConnection()) {
                        verifySuperAdminPasswordDialog(requireActivity()) { correctPasswordSuccess ->
                            if (correctPasswordSuccess) {
                                    // open TestEMi
                                (activity as BaseActivity).transactFragment(TestEmiOptionFragment().apply {
                                    arguments = Bundle().apply {
                                      //  putSerializable("type", EDashboardItem.DYNAMIC_QR)

                                    }
                                })

                            }
                        }
                    }else {
                            VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
                        }
                    }


                else -> {
                    verifySuperAdminPasswordDialog(requireActivity()) { success ->
                        if (success) {
                            when (type) {

                                BankOptions.TPT-> {
                                    val bundle = Bundle()
                                    bundle.putInt("type", type.ordinal)
                                    iDiag?.onEvents(VxEvent.ReplaceFragment(TableEditFragment().apply {
                                        arguments = bundle
                                    }))
                                }
                                BankOptions.CPT->{
                                    iDiag?.onEvents(VxEvent.ReplaceFragment(CommunicationOptionFragment()))
                                }

                                BankOptions.ENV -> changeEnvParam()

                                //   BankOptions.CDT, BankOptions.IPT -> openChooser(type.ordinal)

                                BankOptions.CR -> {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)))
                                            iDiag?.alertBoxWithAction(
                                                null,
                                                null,
                                                getString(R.string.reversal),
                                                getString(R.string.reversal_clear),
                                                true,
                                                getString(R.string.yes),
                                                { alertPositiveCallback ->
                                                    if (alertPositiveCallback) {
                                                        AppPreference.clearReversal()
                                                        iDiag?.showToast("Reversal clear successfully")
                                                    }
                                                    //    declinedTransaction()
                                                },
                                                {})
                                        else
                                            iDiag?.alertBoxWithAction(
                                                null,
                                                null,
                                                getString(R.string.reversal),
                                                getString(R.string.no_reversal_found),
                                                false,
                                                getString(R.string.positive_button_ok),
                                                {},
                                                {})


                                    }
                                }
                                BankOptions.CB -> {
                                    val batchList = BatchFileDataTable.selectBatchData()
                                    if (batchList.size > 0) {
                                        iDiag?.alertBoxWithAction(
                                            null,
                                            null,
                                            "Delete",
                                            "Do you want to delete batch data?",
                                            true,
                                            "YES", {
                                                val batchNumber =
                                                    AppPreference.getIntData(PrefConstant.SETTLEMENT_BATCH_INCREMENT.keyName.toString()) + 1
                                                AppPreference.setIntData(
                                                    PrefConstant.SETTLEMENT_BATCH_INCREMENT.keyName.toString(),
                                                    batchNumber
                                                )
                                                TerminalParameterTable.updateSaleBatchNumber(
                                                    batchNumber.toString()
                                                )
                                                // Added by MKK for automatic FBatch value zero in case of Clear Batch
                                                AppPreference.saveBoolean(
                                                    PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                                                    false
                                                )
                                                //

                                                ROCProviderV2.saveBatchInPreference(batchList)
                                                //Delete All BatchFile Data from Table after Settlement:-
                                                GlobalScope.launch(Dispatchers.IO) {
                                                    deleteBatchTableDataInDB()
                                                    withContext(Dispatchers.Main) {
                                                        VFService.showToast("Batch Deleted Successfully")
                                                    }
                                                }
                                            }, {

                                            }
                                        )
                                    } else {
                                        iDiag?.alertBoxWithAction(
                                            null,
                                            null,
                                            "Empty",
                                            "Batch is empty",
                                            false,
                                            "OK", {
                                            }, {
                                                // Added by MKK for automatic FBatch value zero in case of Clear Batch
                                                AppPreference.saveBoolean(
                                                    PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                                                    false
                                                )
                                                //
                                            }
                                        )
                                    }
                                }
                                else -> {
                                    iDiag?.showToast("Invalid Option")
                                }
                            }
                        }
                    }


                }
            }


        } else if (type.group == EOptionGroup.REPORT.heading) {
            when (type) {
                //   BankOptions.LAST_RECEIPT -> Log.d("REPORTS", "LAST_RECEIPT")
                BankOptions.LAST_RECEIPT -> {
                    //
                    val lastReceiptData = AppPreference.getLastSuccessReceipt()
                    if (lastReceiptData != null) {
                        GlobalScope.launch(Dispatchers.Main) {
                            iDiag?.showProgress(getString(R.string.printing_last_receipt))
                        }
                        when (lastReceiptData.transactionType) {
                            TransactionType.SALE.type, TransactionType.TIP_SALE.type, TransactionType.REFUND.type, TransactionType.VOID.type -> {
                                PrintUtil(activity).startPrinting(
                                    lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        Log.e("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }
                            }
                            TransactionType.EMI_SALE.type -> {
                                PrintUtil(activity).printEMISale(
                                    lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        Log.e("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }
                            }
                            TransactionType.PRE_AUTH_COMPLETE.type -> {
                                PrintUtil(activity).printAuthCompleteChargeSlip(
                                    lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) {
                                    if (it) {
                                        iDiag?.hideProgress()
                                        Log.e("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }
                            }
                            TransactionType.VOID_PREAUTH.type -> {
                                PrintUtil(activity).printAuthCompleteChargeSlip(
                                    lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) {
                                    if (it) {
                                        iDiag?.hideProgress()
                                        Log.e("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }
                            }
                            TransactionType.OFFLINE_SALE.type -> {
                                activity?.let {
                                    OfflineSalePrintReceipt().offlineSalePrint(
                                        lastReceiptData, EPrintCopyType.DUPLICATE,
                                        it
                                    ) { printCB, printingFail ->
                                        if (printCB) {
                                            iDiag?.hideProgress()
                                            Log.e("PRINTING", "LAST_RECEIPT")
                                        } else {
                                            iDiag?.hideProgress()
                                        }
                                    }
                                }
                            }
                            TransactionType.VOID_REFUND.type -> {
                                VoidRefundSalePrintReceipt().startPrintingVoidRefund(
                                    lastReceiptData,
                                    TransactionType.VOID_REFUND.type,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) { _, _ ->
                                    iDiag?.hideProgress()
                                }
                            }
                            else -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    iDiag?.hideProgress()
                                    VFService.showToast("Something wrong Transaction Not Defined")
                                }
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            //    iDiag?.hideProgress()
                            //    iDiag?.showToast(getString(R.string.empty_batch))
                            //-
                            GlobalScope.launch(Dispatchers.Main) {
                                iDiag?.alertBoxWithAction(null,
                                    null,
                                    VerifoneApp.appContext.getString(R.string.empty_batch),
                                    VerifoneApp.appContext.getString(R.string.last_receipt_not_available),
                                    false,
                                    VerifoneApp.appContext.getString(R.string.positive_button_ok),
                                    {},
                                    {})
                            }
                        }
                    }


                } // End of Last Receipt case

                BankOptions.ANY_RECEIPT -> {
                    context?.let {
                        getInputDialog(it, "Enter Invoice Number", "", true) { invoice ->
                            //   iDiag?.showProgress()
                            iDiag?.showProgress(getString(R.string.printing_receipt))
                            GlobalScope.launch {
                                val bat = BatchFileDataTable.selectBatchData()
                                try {
                                    val b =
                                        bat.first { it.hostInvoice.toLong() == invoice.toLong() }
                                    //    printBatch(b)
                                    when (b.transactionType) {
                                        TransactionType.SALE.type, TransactionType.TIP_SALE.type, TransactionType.REFUND.type, TransactionType.VOID.type -> {
                                            PrintUtil(activity).startPrinting(
                                                b,
                                                EPrintCopyType.DUPLICATE,
                                                activity
                                            ) { printCB, printingFail ->
                                                if (printCB) {
                                                    iDiag?.hideProgress()
                                                    Log.e("PRINTING", "LAST_RECEIPT")
                                                } else {
                                                    iDiag?.hideProgress()
                                                }
                                            }
                                        }
                                        TransactionType.EMI_SALE.type -> {
                                            PrintUtil(activity).printEMISale(
                                                b,
                                                EPrintCopyType.DUPLICATE,
                                                activity
                                            ) { printCB, printingFail ->
                                                if (printCB) {
                                                    iDiag?.hideProgress()
                                                    Log.e("PRINTING", "LAST_RECEIPT")
                                                } else {
                                                    iDiag?.hideProgress()
                                                }
                                            }
                                        }
                                        TransactionType.PRE_AUTH_COMPLETE.type -> {
                                            PrintUtil(activity).printAuthCompleteChargeSlip(
                                                b,
                                                EPrintCopyType.DUPLICATE,
                                                activity
                                            ) {
                                                if (it) {
                                                    iDiag?.hideProgress()
                                                    Log.e("PRINTING", "LAST_RECEIPT")
                                                } else {
                                                    iDiag?.hideProgress()
                                                }
                                            }
                                        }
                                        TransactionType.OFFLINE_SALE.type -> {
                                            activity?.let { it1 ->
                                                OfflineSalePrintReceipt().offlineSalePrint(
                                                    b, EPrintCopyType.DUPLICATE,
                                                    it1
                                                ) { printCB, printingFail ->
                                                    if (printCB) {
                                                        iDiag?.hideProgress()
                                                        Log.e("PRINTING", "LAST_RECEIPT")
                                                    } else {
                                                        iDiag?.hideProgress()
                                                    }
                                                }
                                            }
                                        }
                                        else -> {
                                            iDiag?.hideProgress()
                                            VFService.showToast("Something wrong Transaction Not Defined")
                                        }
                                    }
                                } catch (ex: Exception) {
                                    launch(Dispatchers.Main) {
                                        iDiag?.hideProgress()
                                        //    iDiag?.showToast("Invoice is invalid.")
                                        GlobalScope.launch(Dispatchers.Main) {
                                            iDiag?.alertBoxWithAction(null,
                                                null,
                                                VerifoneApp.appContext.getString(R.string.invalid_invoice),
                                                VerifoneApp.appContext.getString(R.string.invoice_is_invalid),
                                                false,
                                                VerifoneApp.appContext.getString(R.string.positive_button_ok),
                                                {},
                                                {})
                                        }


                                    }
                                }
                            }
                        }
                    }
                } // End of Any Receipt case

                BankOptions.DETAIL_REPORT -> {
                    val batchData = BatchFileDataTable.selectBatchData()
                    if (batchData.isNotEmpty()) {
                        iDiag?.getMsgDialog(
                            getString(R.string.confirmation),
                            getString(R.string.want_print_detail),
                            getString(R.string.yes),
                            getString(R.string.no),
                            {
                                GlobalScope.launch {
                                    val bat = BatchFileDataTable.selectBatchData()
                                    if (bat.isNotEmpty()) {
                                        try {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                iDiag?.showProgress(getString(R.string.printing_detail))
                                            }
                                            PrintUtil(activity).printDetailReportupdate(bat, activity) {
                                                iDiag?.hideProgress()
                                            }

                                        } catch (ex: java.lang.Exception) {
                                            ex.message ?: getString(R.string.error_in_printing)
                                            // "catch toast"
                                        } finally {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                                //  iDiag?.showToast(msg)
                                            }
                                        }

                                    } else {
                                        GlobalScope.launch(Dispatchers.Main) {
                                            iDiag?.hideProgress()
                                            iDiag?.showToast("  Batch is empty.  ")
                                        }
                                    }

                                }

                            },
                            {
                                //handle cancel here
                            })
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            iDiag?.alertBoxWithAction(null,
                                null,
                                VerifoneApp.appContext.getString(R.string.empty_batch),
                                VerifoneApp.appContext.getString(R.string.detail_report_not_found),
                                false,
                                VerifoneApp.appContext.getString(R.string.positive_button_ok),
                                {},
                                {})
                        }

                    }
                }// End of Detail report

                BankOptions.SUMMERY_REPORT -> {
                    val batList = BatchFileDataTable.selectBatchData()
                    if (batList.isNotEmpty()) {
                        iDiag?.getMsgDialog(
                            getString(R.string.confirmation),
                            "Do you want to print summary Report",
                            "Yes",
                            "No",
                            {

                                GlobalScope.launch {
                                    if (batList.isNotEmpty()) {
                                        GlobalScope.launch(Dispatchers.Main) {
                                            iDiag?.showProgress(
                                                getString(R.string.printing_summary_report)
                                            )
                                        }
                                        try {
                                            PrintUtil(context).printSettlementReportupdate(
                                                context,
                                                batList
                                            ) {
                                                iDiag?.hideProgress()
                                            }
                                            //  printSummery(batList)
                                            //  getString(R.string.summery_report_printed)

                                        } catch (ex: java.lang.Exception) {
                                            //  ex.message ?: getString(R.string.error_in_printing)
                                            ex.printStackTrace()
                                        } finally {
                                            launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                                // iDiag?.showToast(msg)
                                            }
                                        }

                                    } else {
                                        launch(Dispatchers.Main) {
                                            iDiag?.hideProgress()
                                            iDiag?.getInfoDialog(
                                                "Error",
                                                " Summery is not available."
                                            ) {}
                                        }
                                    }

                                }
                            },
                            {
                                //Cancel handle here

                            })
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            iDiag?.alertBoxWithAction(null,
                                null,
                                VerifoneApp.appContext.getString(R.string.empty_batch),
                                VerifoneApp.appContext.getString(R.string.summary_report_not_available),
                                false,
                                VerifoneApp.appContext.getString(R.string.positive_button_ok),
                                {},
                                {})
                        }

                    }

                } // End of Summery Report

                BankOptions.LAST_CANCEL_RECEIPT -> {
                    val isoW = AppPreference.getReversal()
                    if (isoW != null) {
                        iDiag?.getMsgDialog(
                            getString(R.string.confirmation),
                            getString(R.string.last_cancel_report_confirmation),
                            "Yes",
                            "Cancel",
                            {

                                GlobalScope.launch(Dispatchers.Main) {
                                    iDiag?.showProgress(getString(R.string.printing_last_cancel_receipt))
                                }
                                GlobalScope.launch {
                                    try {
                                        PrintUtil(context).printReversal(context,"") {
                                            //  VFService.showToast(it)
                                            iDiag?.hideProgress()
                                        }
                                    } catch (ex: java.lang.Exception) {
                                        ex.printStackTrace()
                                        iDiag?.hideProgress()
                                    }

                                }
                            },
                            {
                                //Cancel Handling
                            })
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            iDiag?.alertBoxWithAction(null,
                                null,
                                VerifoneApp.appContext.getString(R.string.no_receipt),
                                VerifoneApp.appContext.getString(R.string.no_cancel_receipt_found),
                                false,
                                VerifoneApp.appContext.getString(R.string.positive_button_ok),
                                {},
                                {})
                        }


                    }

                }// End of last cancel receipt

                BankOptions.LAST_SUMMERY_REPORT -> {
                    val str = AppPreference.getString(AppPreference.LAST_BATCH)
                    val batList = Gson().fromJson<List<BatchFileDataTable>>(
                        str,
                        object : TypeToken<List<BatchFileDataTable>>() {}.type
                    )
                    if (batList != null) {
                        iDiag?.getMsgDialog(
                            getString(R.string.confirmation),
                            getString(R.string.last_summary_confirmation),
                            "Yes",
                            "Cancel",
                            {
                                GlobalScope.launch(Dispatchers.Main) {
                                    iDiag?.showProgress(
                                        getString(
                                            R.string.printing_last_summary_report
                                        )
                                    )
                                }

                                GlobalScope.launch {
                                    val str1 = AppPreference.getString(AppPreference.LAST_BATCH)
                                    val batList1 = Gson().fromJson<List<BatchFileDataTable>>(
                                        str1,
                                        object : TypeToken<List<BatchFileDataTable>>() {}.type
                                    )

                                    if (batList1 != null) {
                                        try {
                                            PrintUtil(context).printSettlementReportupdate(
                                                context,
                                                batList1 as MutableList<BatchFileDataTable>,
                                                isSettlementSuccess = false,
                                                isLastSummary = true
                                            ) {
                                                iDiag?.hideProgress()
                                            }
                                        } catch (ex: java.lang.Exception) {
                                            ex.message ?: getString(R.string.error_in_printing)
                                        } finally {
                                            launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                                //   iDiag?.showToast(msg)
                                            }
                                        }
                                    } else {
                                        launch(Dispatchers.Main) {
                                            iDiag?.hideProgress()
                                            iDiag?.getInfoDialog(
                                                "Error",
                                                "Last Summery is not available."
                                            ) {}
                                        }
                                    }

                                }
                            },
                            {
                                //Cancel Handling
                            })
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            iDiag?.alertBoxWithAction(null,
                                null,
                                VerifoneApp.appContext.getString(R.string.no_receipt),
                                VerifoneApp.appContext.getString(R.string.last_summary_not_available),
                                false,
                                VerifoneApp.appContext.getString(R.string.positive_button_ok),
                                {},
                                {})
                        }
                    }

                }// End of last Summery Report receipt

                else -> {
                    iDiag?.showToast("No Option Found")
                }

            }// End of when

        }

    }


    fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit) {
        val dialog = Dialog(VerifoneApp.appContext)
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.msg_dialog)
            setCancelable(false)

            findViewById<TextView>(R.id.msg_dialog_title).text = title
            findViewById<TextView>(R.id.msg_dialog_msg).text = msg

            with(findViewById<TextView>(R.id.msg_dialog_ok)) {
                setOnClickListener {
                    dismiss()
                    acceptCb()
                }
                text = "OK"
            }


            findViewById<TextView>(R.id.msg_dialog_cancel).visibility = View.INVISIBLE
        }.show()
    }

    private fun openChooser(tableType: Int) {

        fun action(listId: String) {
            val bundle = Bundle().apply {
                putInt("type", tableType)
                putString("id", listId)
            }
            iDiag?.onEvents(VxEvent.ReplaceFragment(TableEditFragment().apply {
                arguments = bundle
            }))

        }

        GlobalScope.launch {
            val list = arrayListOf<TableEditHelper>()
            /*  if (tableType == BankOptions.CDT.ordinal) {
                  val i = CardDataTable.selecteAllCardsData()
                  for (e in i) {
                      list.add(TableEditHelper(e.cardLabel, e.cardTableIndex))
                  }
              } else {
                  val i = IssuerParameterTable.selectFromIssuerParameterTable()
                  for (e in i) {
                      list.add(TableEditHelper(e.issuerName, e.issuerId))
                  }
              }*/

            launch(Dispatchers.Main) {
                if (list.size > 1) {
                    context?.let {
                        Dialog(it).apply {
                            requestWindowFeature(Window.FEATURE_NO_TITLE)
                            setContentView(R.layout.dialog_chooser)
                            setCancelable(false)
                            val rb = this.findViewById<RadioGroup>(R.id.d_chooser_rg)
                            list.forEach {
                                val r = RadioButton(context).apply {
                                    text = it.titleName
                                    tag = it.titleValue
                                    textSize = 20f
                                    setPadding(5, 10, 5, 10)
                                    id = View.generateViewId()
                                }
                                rb.addView(r)
                            }

                            this.findViewById<View>(R.id.d_chooser_done_btn).setOnClickListener {
                                var index = 0
                                while (index < rb.childCount) {
                                    val vi = rb.getChildAt(index)
                                    if (vi is RadioButton) {
                                        if (vi.isChecked) {
                                            action(vi.tag as String)
                                            break
                                        }
                                        index++
                                    }
                                }
                                dismiss()
                            }
                            this.findViewById<View>(R.id.d_chooser_cancel_btn)
                                .setOnClickListener { dismiss() }

                        }.show()
                    }
                } else if (list.size == 1) action(list[0].titleValue)
            }
        }
    }


    private fun changeEnvParam() {
        GlobalScope.launch {
            val list = arrayListOf<TableEditHelper>()
            val i = IssuerParameterTable.selectFromIssuerParameterTable()
            for (e in i) {
                list.add(TableEditHelper(e.issuerName, e.issuerId))
            }

            launch(Dispatchers.Main) {
                var isEdit = false
                context?.let {
                    Dialog(it).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        setContentView(R.layout.dialog_emv)
                        setCancelable(false)

                        val container = findViewById<LinearLayout>(R.id.emv_edit_ll)
                        val sep = findViewById<View>(R.id.emv_separator_v)

                        findViewById<View>(R.id.emv_close_btn).setOnClickListener { dismiss() }

                        val pcEt = findViewById<EditText>(R.id.emv_pcno_et)
                        val bankEt = findViewById<EditText>(R.id.emv_bankcode_et)
                        val issuerEt = findViewById<EditText>(R.id.emv_issuerid_et)
                        val accEt = findViewById<EditText>(R.id.emv_ac_selection_et)

                        pcEt.setText(AppPreference.getString(AppPreference.PC_NUMBER_KEY))
                        bankEt.setText(AppPreference.getBankCode())
                        if (AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY).isEmpty()) {
                            val issuerId = addPad(AppPreference.WALLET_ISSUER_ID, "0", 2)
                            issuerEt.setText(issuerId)
                        } else {
                            issuerEt.setText(AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY))
                            //  issuerEt.setText(AppPreference.getString(AppPreference.WALLET_ISSUER_ID))
                        }
                        accEt.setText(AppPreference.getString(AppPreference.ACC_SEL_KEY))

                        val rg = findViewById<RadioGroup>(R.id.emv_radio_grp_btn)

                        rg.setOnCheckedChangeListener { _rbg, id ->
                            val rb = _rbg.findViewById<RadioButton>(id)
                            val value = rb.tag as String
                            if (value.isNotEmpty()) {
                                GlobalScope.launch {
                                    val data =
                                        IssuerParameterTable.selectFromIssuerParameterTable(value)
                                    if (data != null) {
                                        val issuerName = data.issuerId
                                        launch(Dispatchers.Main) {
                                            issuerEt.setText(issuerName)
                                        }
                                        AppPreference.saveString(
                                            AppPreference.CRDB_ISSUER_ID_KEY,
                                            issuerName
                                        )

                                    }
                                }
                            }
                        }

                        list.forEach {
                            val rBtn = RadioButton(context).apply {
                                text = it.titleName
                                tag = it.titleValue
                                setPadding(5, 20, 5, 20)
                            }
                            rg.addView(rBtn)
                            if (it.titleValue == issuerEt.text.toString()) {
                                rBtn.isChecked = true
                            }

                        }

                        fun hh(liView: Array<View>, liEt: Array<EditText>, visibility: Int) {
                            for (e in liView) e.visibility = visibility
                            for (e in liEt) {
                                e.isFocusableInTouchMode = isEdit
                                e.isClickable = isEdit
                                e.isFocusable = isEdit
                            }
                        }

                        hh(arrayOf(container, sep), arrayOf(pcEt, bankEt, accEt), View.GONE)

                        findViewById<TextView>(R.id.emv_edit).setOnClickListener {
                            isEdit = !isEdit
                            val tv = it as TextView
                            if (isEdit) {
                                hh(
                                    arrayOf(container, sep),
                                    arrayOf(pcEt, bankEt, accEt, issuerEt),
                                    View.VISIBLE
                                )
                                tv.text = getString(R.string.save)
                            } else {
                                GlobalScope.launch {
                                    AppPreference.saveString(
                                        AppPreference.PC_NUMBER_KEY,
                                        pcEt.text.toString()
                                    )
                                    AppPreference.setBankCode(bankEt.text.toString())
                                    AppPreference.saveString(
                                        AppPreference.ACC_SEL_KEY,
                                        accEt.text.toString()
                                    )
                                    AppPreference.saveString(
                                        AppPreference.CRDB_ISSUER_ID_KEY,
                                        issuerEt.text.toString()
                                    )

                                }
                                hh(arrayOf(container, sep), arrayOf(pcEt, bankEt, accEt), View.GONE)
                                activity?.let { it1 -> ROCProviderV2.refreshToolbarLogos(it1) }
                                tv.text = getString(R.string.edit)
                            }
                        }

                    }.show()
                }
            }
        }
    }

}


class SubMenuFragmentAdapter(
    val list: MutableList<BankOptions>,
    val caller: IOnSubMenuItemSelectListener?
) :
    RecyclerView.Adapter<SubMenuFragmentAdapter.SubMenuHolder>() {

    //private val mBg = TouchBackgroundD(R.drawable.et_bottomline_bg, R.drawable.bottom_line_et)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SubMenuHolder {
        return SubMenuHolder(
            LayoutInflater.from(p0.context).inflate(
                R.layout.item_sub_menu,
                p0,
                false
            )
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(p0: SubMenuHolder, p1: Int) {
        p0.isfmTitleTV?.text = list[p1]._name
        p0.ifsmIV.background = ContextCompat.getDrawable(p0.view.context, list[p1].res)
    }

    inner class SubMenuHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val isfmTitleTV = view.findViewById<BHTextView>(R.id.ifsm_title_tv)

        // val icLock = view.findViewById<ImageView>(R.id.ic_lock)
        val ifsmIV = view.findViewById<ImageView>(R.id.ifsm_iv)
        val ifsmParentLL = view.findViewById<NeumorphCardView>(R.id.ifsm_parent_ll)

        init {
            //    icLock.visibility = View.GONE
            ifsmParentLL.apply {
                //    setOnTouchListener(SubMenuFragment.onTouchListenerWithBorder)

                setOnClickListener {
                    caller?.onSubmenuItemSelected(list[adapterPosition])
                }
                //  mBg.attachView(view)
            }
        }
    }

}

interface IOnSubMenuItemSelectListener {
    fun onSubmenuItemSelected(type: BankOptions, data: Any? = null)
}

//endregion