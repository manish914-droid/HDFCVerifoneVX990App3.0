package com.example.verifonevx990app.digiPOS

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.DigiPosTxnListBinding
import com.example.verifonevx990app.databinding.DigiPosTxnListItemBinding
import com.example.verifonevx990app.databinding.ItemPendingTxnBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SplitterTypes
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.util.*

class DigiPosTxnListFragment : Fragment() {
    private var iDialog: IDialog? = null
    private var sheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var binding: DigiPosTxnListBinding? = null
    private var selectedFilterTransactionType: String = ""
    private var selectedFilterTxnID: String = ""
    private var selectedFilterTxnIDValue: String = ""
    private var selectedFilterAmountValue: String = ""
    private var hasMoreData = false
    private var perPageRecord = "0"
    private var totalRecord = "0"
    private var pageNumber = "1"
    private var partnerTransactionID = ""
    private var mTransactionID = ""
    private var bottomSheetAmountData = ""
    private var filterTransactionType = ""
    private var tempDataList = mutableListOf<String>()
    private var requestTypeID = EnumDigiPosProcess.TXN_LIST.code
    private var field57RequestData =
        "$requestTypeID^$totalRecord^$filterTransactionType^$bottomSheetAmountData^$partnerTransactionID^$mTransactionID^$pageNumber^"
    private var processingCode = EnumDigiPosProcessingCode.DIGIPOSPROCODE.code
    private var txnDataList = mutableListOf<DigiPosTxnModal>()
    private lateinit var  digiPosTxnListAdapter :DigiPosTxnListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DigiPosTxnListBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        digiPosTxnListAdapter = DigiPosTxnListAdapter(txnDataList,
                ::onItemClickCB
            )



        sheetBehavior = binding?.bottomSheet?.let { BottomSheetBehavior.from(it.bottomLayout) }

        //back Navigation icon click event:-
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.txn_list)

        //filter click event to open bottom sheet:-
        binding?.filterTV?.setOnClickListener { toggleBottomSheet() }

        //bottom sheet close icon click event to close bottom sheet:-
        binding?.bottomSheet?.closeIconBottom?.setOnClickListener { closeBottomSheet() }

        //region===================Filter Transaction Type's RadioButton OnClick events:-
        binding?.bottomSheet?.upiCollectBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.upiCollectBottomRB?.text?.toString() ?: ""
            filterTransactionType = EnumDigiPosProcess.UPIDigiPOS.code
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.dynamicQRBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.dynamicQRBottomRB?.text?.toString() ?: ""
            filterTransactionType = EnumDigiPosProcess.DYNAMIC_QR.code
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.smsPayBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.smsPayBottomRB?.text?.toString() ?: ""
            filterTransactionType = EnumDigiPosProcess.SMS_PAYDigiPOS.code
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.staticQRBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.staticQRBottomRB?.text?.toString() ?: ""
            filterTransactionType = EnumDigiPosProcess.STATIC_QR.code
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }
        //endregion

        //region===================PTXN ID and MTXN ID RadioButtons OnClick Listener event:-
        binding?.bottomSheet?.ptxnIDBottomRB?.setOnClickListener {
            selectedFilterTxnID = binding?.bottomSheet?.ptxnIDBottomRB?.text?.toString() ?: ""
            binding?.bottomSheet?.ptxnIDBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.ptxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.mtxnIDBottomRB?.isChecked = false
            binding?.bottomSheet?.mtxnIDBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.mtxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
        }

        binding?.bottomSheet?.mtxnIDBottomRB?.setOnClickListener {
            selectedFilterTxnID = binding?.bottomSheet?.mtxnIDBottomRB?.text?.toString() ?: ""
            binding?.bottomSheet?.mtxnIDBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.mtxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.ptxnIDBottomRB?.isChecked = false
            binding?.bottomSheet?.ptxnIDBottomRB?.setTextColor(Color.parseColor("#A9A9A9"))
            binding?.bottomSheet?.ptxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
        }
        //endregion



            setUpRecyclerView()
            getDigiPosTransactionListFromHost()


        //region======================Filter Apply Button onclick event:-
        binding?.bottomSheet?.applyFilter?.setOnClickListener {
      val amtStr=   binding?.bottomSheet?.amountBottomET?.text?.toString() ?: "0.0"
            bottomSheetAmountData = if(amtStr=="") "0.0" else amtStr
                if (binding?.bottomSheet?.ptxnIDBottomRB?.isChecked == true)
                partnerTransactionID = binding?.bottomSheet?.transactionIDET?.text.toString()
            if (binding?.bottomSheet?.mtxnIDBottomRB?.isChecked == true)
                mTransactionID = binding?.bottomSheet?.transactionIDET?.text.toString()

            field57RequestData =
                "$requestTypeID^0^$filterTransactionType^$bottomSheetAmountData^$partnerTransactionID^$mTransactionID^1^"
            closeBottomSheet()
            tempDataList.clear()

            getDigiPosTransactionListFromHost()
        }
        //endregion

        //region======================OnScrollListener to Load More Data in RecyclerView:-
        binding?.transactionListRV?.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding?.transactionListRV?.canScrollVertically(1)!! && dy > 0 && hasMoreData) {
                    Log.d("MoreData:- ", "Loading.....")
                    pageNumber = pageNumber.toInt().plus(1).toString()
                    field57RequestData =
                        "$requestTypeID^$totalRecord^$filterTransactionType^$bottomSheetAmountData^$partnerTransactionID^$mTransactionID^" +
                                "$pageNumber^"
                    getDigiPosTransactionListFromHost()
                }
            }

        })
        //endregion

    }

    //region====================SetUp RecyclerView:-
    private fun setUpRecyclerView() {
        binding?.transactionListRV?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            adapter = digiPosTxnListAdapter
        }
    }
    //endregion

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    //region==========================Get DigiPos TXN List Data from Host:-
    private fun getDigiPosTransactionListFromHost() {
        Log.d("Field57:- ", field57RequestData)
        iDialog?.showProgress()
        val idw = runBlocking(Dispatchers.IO) {
            IsoDataWriter().apply {
                val terminalData = TerminalParameterTable.selectFromSchemeTable()
                if (terminalData != null) {
                    mti = Mti.EIGHT_HUNDRED_MTI.mti

                    //Processing Code Field 3
                    addField(3, processingCode)

                    //STAN(ROC) Field 11
                    addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

                //NII Field 24
                addField(24, Nii.BRAND_EMI_MASTER.nii)

                //TID Field 41
                addFieldByHex(41, terminalData.terminalId)

                //Connection Time Stamps Field 48
                addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                //adding Field 57
                addFieldByHex(57, field57RequestData)

                //adding Field 61
                val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                val pcNumber2 =
                    addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
                val f61 = ConnectionType.GPRS.code + addPad(
                    AppPreference.getString("deviceModel"),
                    " ",
                    6,
                    false
                ) + addPad(
                    VerifoneApp.appContext.getString(R.string.app_name),
                    " ",
                    10,
                    false
                ) + version + pcNumber + pcNumber2
                    //adding Field 61
                    addFieldByHex(61, f61)

                    //adding Field 63
                    val deviceSerial =
                        addPad(AppPreference.getString("serialNumber"), " ", 15, false)
                    val bankCode = AppPreference.getBankCode()
                    val f63 = "$deviceSerial$bankCode"
                    addFieldByHex(63, f63)
                }
            }
        }

        logger("DIGIPOS REQ1>>", idw.isoMap, "e")

        // val idwByteArray = idw.generateIsoByteRequest()
        var responseMsg = ""
        var isBool = false
        lifecycleScope.launch(Dispatchers.IO) {
            HitServer.hitDigiPosServer(idw, false) { result, success ->
                responseMsg = result
                if (success) {
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("Transaction RESPONSE ", "---", "e")
                    logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                    Log.e(
                        "Success 39-->  ",
                        responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    )
                    val successResponseCode =
                        responseIsoData.isoMap[39]?.parseRaw2String().toString()
                    if (responseIsoData.isoMap[58] != null) {
                        responseMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    }
                    isBool = successResponseCode == "00"
                    if(isBool) {
                        if (responseIsoData.isoMap[57] != null) {
                            txnDataList.clear()
                            var responseField57 =
                                responseIsoData.isoMap[57]?.parseRaw2String().toString()
                            parseTXNListDataAndShowInRecyclerView(responseField57)
                        }
                    }else{
                        iDialog?.hideProgress()
                    }
                } else {
                    ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )

                    iDialog?.hideProgress()
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDialog?.hideProgress()
                        hasMoreData = false
                        iDialog?.alertBoxWithAction(null, null,
                            getString(R.string.error), result,
                            false, getString(R.string.positive_button_ok),
                            { parentFragmentManager.popBackStackImmediate() }, {})
                    }
                }
            }
        }
    }
    //endregion

    //region========================Parse DigiPos TXN List Data and Show in RecyclerView:-
    private fun parseTXNListDataAndShowInRecyclerView(field57Data: String) {
        if (!TextUtils.isEmpty(field57Data)) {
            val dataList =
                parseDataListWithSplitter(SplitterTypes.VERTICAL_LINE.splitter, field57Data)
            if (dataList.isNotEmpty()) {
                requestTypeID = dataList[0]
                //hasMoreData = dataList[1] ----> This Data from Host will always be "0" so we need to manage Pagination in App-End Side
                perPageRecord = dataList[2]
                totalRecord = (totalRecord.toInt().plus(perPageRecord.toInt()).toString())

                tempDataList.clear()
                tempDataList = dataList.subList(3, dataList.size)
                for (i in tempDataList.indices) {
                    //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        txnDataList.add(
                            DigiPosTxnModal(
                                requestTypeID,
                                splitData[0], splitData[1],
                                splitData[2], splitData[3],
                                splitData[4], splitData[5],
                                splitData[6], splitData[7],
                                splitData[8], splitData[9],
                                splitData[10], splitData[11]
                            )
                        )
                    }
                }
                //Inflate Update Data in Adapter List:-
                lifecycleScope.launch(Dispatchers.Main) {
                    hasMoreData = tempDataList.isNotEmpty() && tempDataList.size >= 10
                    if (txnDataList.isNotEmpty()) {
                        digiPosTxnListAdapter.refreshAdapterList(txnDataList)
                    }
                    iDialog?.hideProgress()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                iDialog?.hideProgress()
                hasMoreData = false
                VFService.showToast("No Data Found")
            }
        }
    }
    //endregion

    //Method to be called on Bottom Sheet Close:-
    private fun closeBottomSheet() {
        if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    //Method to be called when Bottom Sheet Toggle:-
    private fun toggleBottomSheet() {
        if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    //region==================OnItemClickCB:-
    private fun onItemClickCB(position: Int, clickItem: String) {
        if (position > -1) {
            if (clickItem == GET_TXN_STATUS) {
                iDialog?.showProgress()
                lifecycleScope.launch(Dispatchers.IO) {
                    val req57 =
                        "${EnumDigiPosProcess.GET_STATUS.code}^${txnDataList[position].partnerTXNID}^${txnDataList[position].mTXNID}^"
                    Log.d("Field57:- ", req57)
                    getDigiPosStatus(
                        req57,
                        EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                        false
                    ) { isSuccess, responseMsg, responsef57, fullResponse ->
                        try {
                            if (isSuccess) {
                                val statusRespDataList = responsef57.split("^")
                                val modal = txnDataList[position]
                                modal.transactionType = statusRespDataList[0]
                                modal.status = statusRespDataList[1]
                                modal.statusMessage = statusRespDataList[2]
                                modal.statusCode = statusRespDataList[3]
                                modal.mTXNID = statusRespDataList[4]
                                modal.txnStatus = statusRespDataList[5]
                                modal.partnerTXNID = statusRespDataList[6]
                                modal.transactionTime = statusRespDataList[7]
                                modal.amount = statusRespDataList[8]
                                modal.paymentMode = statusRespDataList[9]
                                modal.customerMobileNumber = statusRespDataList[10]
                                modal.description = statusRespDataList[11]
                                modal.pgwTXNID = statusRespDataList[12]
                                lifecycleScope.launch(Dispatchers.Main) {
                                    txnDataList.removeAt(position)
                                    binding?.transactionListRV?.removeViewAt(position)
                                    txnDataList[position] = modal
                                    digiPosTxnListAdapter.notifyItemInserted(position)
                                    digiPosTxnListAdapter.refreshAdapterList(txnDataList)
                                    iDialog?.hideProgress()
                                    binding?.transactionListRV?.smoothScrollToPosition(0)
                                }
                            } else {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    iDialog?.hideProgress()
                                    iDialog?.alertBoxWithAction(null, null,
                                        getString(R.string.error), responseMsg,
                                        false, getString(R.string.positive_button_ok),
                                        {}, {})
                                }
                            }
                        } catch (ex: java.lang.Exception) {
                            iDialog?.hideProgress()
                            ex.printStackTrace()
                            logger(
                                LOG_TAG.DIGIPOS.tag,
                                "Somethig wrong... in response data field 57"
                            )
                        }
                    }
                }
            } else {
                (activity as MainActivity).transactFragment(DigiPosTXNListDetailPage().apply {
                    arguments = Bundle().apply {
                        putParcelable("data", txnDataList[position])
                        // putString(INPUT_SUB_HEADING, "")
                    }
                })
            }
        }
    }
//endregion

    override fun onStop() {
        super.onStop()
        selectedFilterTransactionType = ""
        selectedFilterTxnID = ""
        selectedFilterTxnIDValue = ""
        selectedFilterAmountValue = ""
        hasMoreData = false
        perPageRecord = "0"
        totalRecord = "0"
        pageNumber = "1"
        partnerTransactionID = ""
        mTransactionID = ""
        bottomSheetAmountData = ""
        filterTransactionType = ""
        tempDataList.clear()
        txnDataList.clear()
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

internal class DigiPosTxnListAdapter(
    private var dataList: MutableList<DigiPosTxnModal>?,
    private val onCategoryItemClick: (Int, String) -> Unit
) :
    RecyclerView.Adapter<DigiPosTxnListAdapter.DigiPosTxnViewHolder>() {

    private val adapterTXNList: MutableList<DigiPosTxnModal> = mutableListOf()

    init {
        logger("LIST SIZE","${dataList?.size}","e")
        if (dataList?.isNotEmpty() == true)
            adapterTXNList.addAll(dataList!!)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DigiPosTxnViewHolder {
        val binding: ItemPendingTxnBinding = ItemPendingTxnBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DigiPosTxnViewHolder(binding)
    }

    override fun getItemCount(): Int = adapterTXNList.size

    override fun onBindViewHolder(holder: DigiPosTxnViewHolder, p1: Int) {
        val modal = adapterTXNList[p1]
        if (!TextUtils.isEmpty(modal.partnerTXNID)) {
            holder.binding.smsPay.text = modal.paymentMode
            when {
                modal.paymentMode.toLowerCase(Locale.ROOT).equals("sms pay", true) -> {
                    holder.binding.smsPay.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.sms_icon,
                        0,
                        0,
                        0
                    )
                }
                modal.paymentMode.toLowerCase(Locale.ROOT).equals("upi", true) -> {
                    holder.binding.smsPay.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.upi_icon,
                        0,
                        0,
                        0
                    )
                }
                else -> {
                    holder.binding.smsPay.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_qr_code,
                        0,
                        0,
                        0
                    )
                }
            }
            val amountData = "\u20B9${modal.amount}"
            holder.binding.smsPayTransactionAmount.text = amountData
            holder.binding.dateTV.text = modal.transactionTime
            holder.binding.mobileNumberTV.text = modal.customerMobileNumber

            when {
                modal.txnStatus.toLowerCase(Locale.ROOT).equals("success", true) -> {
                    holder.binding.transactionIV.setImageResource(R.drawable.circle_with_tick_mark_green)
                    holder.binding.getStatusButton.visibility = View.GONE
                }
                else -> {
                    holder.binding.transactionIV.setImageResource(R.drawable.ic_exclaimation_mark_circle_error)
                    holder.binding.getStatusButton.visibility = View.VISIBLE
                }
            }

            //Showing Visibility of All Views:-
            holder.binding.transactionIV.visibility = View.VISIBLE
            holder.binding.parentSubHeader.visibility = View.VISIBLE
            holder.binding.transactionIV.visibility = View.VISIBLE
            holder.binding.mobileNumberTV.visibility = View.VISIBLE
            holder.binding.sepraterLineView.visibility = View.VISIBLE
        }
    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: MutableList<DigiPosTxnModal>) {
        val diffUtilCallBack = DigiPosTXNListDiffUtil(this.adapterTXNList, refreshList)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallBack)
        this.adapterTXNList.clear()
        this.adapterTXNList.addAll(refreshList)
        diffResult.dispatchUpdatesTo(this)
    }
    //endregion

    inner class DigiPosTxnViewHolder(val binding: ItemPendingTxnBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.getStatusButton.setOnClickListener {
                onCategoryItemClick(
                    absoluteAdapterPosition,
                    GET_TXN_STATUS
                )
            }
            binding.parentSubHeader.setOnClickListener {
                onCategoryItemClick(
                    absoluteAdapterPosition,
                    SHOW_TXN_DETAIL_PAGE
                )
            }
        }
    }
}

//region=============================DigiPos Txn List Data Modal==========================
@Parcelize
data class DigiPosTxnModal(
    var transactionType: String,
    var status: String,
    var statusMessage: String,
    var statusCode: String,
    var mTXNID: String,
    var txnStatus: String,
    var partnerTXNID: String,
    var transactionTime: String,
    var amount: String,
    var paymentMode: String,
    var customerMobileNumber: String,
    var description: String,
    var pgwTXNID: String
) : Parcelable
//endregion

//region===================Const To Used to Determine Which Item is Clicked in DigiPosTXN List Fragment:-
const val GET_TXN_STATUS = "getTXNStatus"
const val SHOW_TXN_DETAIL_PAGE = "showTXNDetailPage"
//endregion