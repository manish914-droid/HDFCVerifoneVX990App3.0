package com.example.verifonevx990app.nontransaction


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.verifonevx990app.R
import com.example.verifonevx990app.bankEmiEnquiry.CreateEMIEnquiryTransactionPacket
import com.example.verifonevx990app.databinding.FragmentEmiBinding
import com.example.verifonevx990app.databinding.FragmentEmiDetailBinding
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.emv.transactionprocess.SyncEmiEnquiryTransactionToHost
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.*
import com.example.verifonevx990app.transactions.*
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.VerifoneApp.Companion.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

/* This Class is of No Use Now , Please Ignore it*/
class EmiActivity : BaseActivity(), IBenefitTable, View.OnClickListener {

    private lateinit var iptList: List<IssuerParameterTable>
    private lateinit var pagerAdapter: EmiPagerAdapter
    private val mAmount: Double by lazy { intent.getDoubleExtra("amount", 0.0) }
    private var cardProcessedData: CardProcessedDataModal? = null
    //  private val cardProcessedData: CardProcessedDataModal by lazy { intent.getSerializableExtra("cardprocess") as? CardProcessedDataModal ?: cardProcessedData}

    private val pan: String by lazy { intent.getStringExtra("pan") ?: "" }

    private val isBankEmi by lazy { intent.getBooleanExtra("is_bank", false) }

    private val benifitTable: List<BenifitSlabTable> by lazy { BenifitSlabTable.selectFromBenifitSlabTable() }
    private val emiSchemeTable: List<EmiSchemeTable> by lazy { EmiSchemeTable.selectFromEmiSchemeTable()/*.filter { it.isActive == "1" }*/ } // all active emi scheme data

    private var mCurrentFragment = 0

    private val mListFragment = mutableListOf<EmiDetailFragment>()
    private val mListTitle = mutableListOf<String>()
    private var binding: FragmentEmiBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentEmiBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val intent = Intent().apply {
            if (null != intent) {
                cardProcessedData =
                    intent.getSerializableExtra("cardprocess") as? CardProcessedDataModal
                //println("CardProcess data in emi is " + cardProcessedData)
            }
        }

        initUI()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.emi_print -> {
                print()
                true
            }
            R.id.emi_send_sms -> {
                sendEmiSms()
                true
            }
            R.id.emi_filter_bank -> {
                openBankList()
                true
            }
            else -> false
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.emi_frag_print_btn -> {  // Case for Printing Btn
                print()
            }

            R.id.emi_frag_send_sms_btn -> { // Send SMS Btn
                sendEmiSms()
            }

            R.id.emi_frag_filter_btn -> {  // Filter Btn
                openBankList()
            }

        }
    }


    private fun initUI() {

        //region===========Setting Toolbar===============

        setSupportActionBar(binding?.toolbarView?.toolbarV2)

        binding?.toolbarView?.toolbarV2Start?.background =
            ContextCompat.getDrawable(this@EmiActivity, R.drawable.arrow_back)
        binding?.toolbarView?.toolbarV2Start?.setOnClickListener { onBackPressed() }
        binding?.toolbarView?.toolbarV2Tv?.text =
            if (isBankEmi) EDashboardItem.BANK_EMI.title else EDashboardItem.EMI_ENQUIRY.title

        //endregion

        GlobalScope.launch {

            iptList = IssuerParameterTable.selectFromIssuerParameterTable()
            iptList = iptList.filter { it.isActive == "1" }

            if (pan.isNotEmpty()) {
                val temPan = pan.substring(0, 6)
                try {
                    val emiBin = EmiBinTable.selectFromEmiBinTable().first { it.binValue == temPan }
                    iptList = iptList.filter { it.issuerId == emiBin.issuerId }
                } catch (ex: Exception) {
                    iptList = emptyList()
                    ex.printStackTrace()
                }
            }

            iptList.forEach {
                val frag = EmiDetailFragment.getInstance(it, Bundle().apply {
                    putDouble("amount", mAmount)
                    putBoolean("isbank", isBankEmi)
                })
                mListFragment.add(frag)
                mListTitle.add(it.issuerName)
            }

            launch(Dispatchers.Main) {
                pagerAdapter = EmiPagerAdapter(
                    supportFragmentManager,
                    if (isBankEmi) mListFragment else arrayListOf(),
                    if (isBankEmi) mListTitle else arrayListOf()
                )
                binding?.emiFragVp?.adapter = pagerAdapter
                pagerAdapter.notifyDataSetChanged()

                if (!isBankEmi) {
                    openBankList()
                } else {
                    val emiSchemeGroupTable: List<EmiSchemeGroupTable> by lazy { EmiSchemeGroupTable.selectFromEmiSchemeGroupProductTable() }
                    val emiSchemeTableSet = mutableSetOf<String>()

                    var int: Int = 0
                    var emischemeid = ArrayList<String>()
                    for (e in emiSchemeGroupTable) {
                        if (e.isActive == "1" && e.emischemeIds.contains(","))
                            emischemeid = e.emischemeIds.split(",") as ArrayList<String>
                        else if (e.isActive == "1") {
                            for (j in emiSchemeTable) {  //114 for EmiSchemeTable
                                val currDt = getCurrentDate()
                                if (currDt.compareTo(j.startDate) >= 0 && currDt.compareTo(j.endDate) <= 0 && mAmount <= j.maxValue.toDouble() / 100 && mAmount >= j.minValue.toDouble() / 100) {
                                    if (j.isActive == "1" && e.isActive == "1" && j.emiSchemeId == e.emischemeIds) {
                                        emiSchemeTableSet.add(j.schemeId)
                                    }
                                }
                            }
                        }
                    }
                    for (e in emischemeid.iterator()) {
                        for (j in emiSchemeTable) {
                            val currDt = getCurrentDate()
                            if (currDt.compareTo(j.startDate) >= 0 && currDt.compareTo(j.endDate) <= 0 && mAmount <= j.maxValue.toDouble() / 100 && mAmount >= j.minValue.toDouble() / 100) {
                                if (j.isActive == "1" && j.emiSchemeId == emischemeid.get(int)) {
                                    emiSchemeTableSet.add(j.schemeId)
                                }
                            }
                        }
                        int++

                    }


                }


            }


        }

        arrayOf(
            binding?.emiFragPrintBtn,
            binding?.emiFragSendSmsBtn,
            binding?.emiFragFilterBtn
        ).forEach { it?.setOnClickListener(this) }

        if (isBankEmi) {
            //    setImageResource(R.drawable.ic_proceed_button)
            binding?.emiSendFab?.setOnClickListener { nextBankEmi() }
            binding?.emiFragBtnLl?.visibility = View.GONE
        } else {
            binding?.emiSendFab?.visibility = View.GONE
            binding?.emiFragBtnLl?.visibility = View.VISIBLE
        }

        binding?.emiFragVp?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                mCurrentFragment = position
            }

        })


    }

    private fun nextBankEmi() {
        try {

            val frag = pagerAdapter.list[mCurrentFragment]
            val idm = frag.issuerDataModel
            val tdm = frag.getSelectedTenure()
            when {
                idm == null -> showToast("Issuer Error. Please init again")
                tdm == null -> showToast("Please select the tenure")
                else -> {
                    if (tdm.isChecked)
                        onEvents(VxEvent.PayEmi(mAmount.toFloat(), idm.issuerId, tdm))
                    else {
                        showToast("Please select the tenure")
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun print() {
        showProgress()
        GlobalScope.launch {
            val frags = pagerAdapter.list.filter { it.issuerDataModel != null }
            val idmList = mutableListOf<IssuerDataModel>()
            frags.forEach { idmList.add(it.issuerDataModel as IssuerDataModel) }
            try {
                val printingSchemes = idmList[0].schemeDataModel[0].tenureDataModel
                /*  val schemesToPrint = arrayListOf<TenureDataModel>()
                  for (pp in printingSchemes) {
                      if (pp.isChecked) {
                          schemesToPrint.add(pp)
                      }

                  }*/
                //    printEmi(mAmount, idmList, this@EmiActivity)
                //    if (schemesToPrint.size > 0)
                PrintUtil(this@EmiActivity).printTenure(
                    this@EmiActivity,
                    idmList as ArrayList<IssuerDataModel>,
                    mAmount.toFloat()
                )
                /*  else{
                      VFService.showToast("Select Tenure")
                  }*/

                launch(Dispatchers.Main) {
                    hideProgress()
                }
            } catch (pex: Exception) {  //PaxPrinterException
                launch(Dispatchers.Main) {
                    hideProgress()
                    val msg = "Error in printing emi\n${pex.message}"
                    getInfoDialog(getString(R.string.printing_error), msg) {}
                }
            }
        }
    }


    private fun sendEmiSms() {

        getInputDialog(this, "Contact Number", "", true) {
            if (it.length !in 10..13) {
                showToast("Invalid Mobile number")
            } else {
                val data = hashMapOf<String, String>()
                data["issuer"] = iptList[mCurrentFragment].issuerId
                val formattedAmount = "%.2f".format(mAmount)
                data["amount"] = (formattedAmount).replace(".", "")
                data["mobile"] = it

                cardProcessedData?.setEmiType(EIntentRequest.EMI_ENQUIRY.code)
                val transactionEMIISO =
                    CreateEMIEnquiryTransactionPacket(data.toString()).createTransactionPacket()
                logger("SEND SMS REQUEST PACKET --->>", transactionEMIISO.isoMap, "e")
                //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
                GlobalScope.launch(Dispatchers.IO) {
                    checkReversal(transactionEMIISO, cardProcessedData!!, data)
                }

            }
        }

    }

    //  Below method is used to Sync Transaction Data To Server:-
    private fun checkReversal(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal,
        data: HashMap<String, String>
    ) {
        runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            SyncEmiEnquiryTransactionToHost(
                transactionISOByteArray,
                cardProcessedDataModal
            ) { syncStatus, responseCode, transactionMsg, printExtraData ->
                hideProgress()
                if (syncStatus && responseCode == "00" && !AppPreference.getBoolean(AppPreference.ONLINE_EMV_DECLINED)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        //   showToast("Sms send on mobile no ${data["mobile"]}") //SMS has been sent.
                        alertBoxWithAction(null, null,
                            getString(R.string.sms_header),
                            getString(R.string.sms_sent),
                            false,
                            getString(R.string.positive_button_ok),
                            { alertPositiveCallback ->
                                if (alertPositiveCallback) {
                                    // finish()
                                }

                            },
                            {})
                    }

                } else if (syncStatus && responseCode != "00") {
                    GlobalScope.launch(Dispatchers.Main) {
                        showToast(transactionMsg.toString())
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        showToast(transactionMsg.toString())
                    }

                }
            }
        }
        //Below Else case is to Sync Reversal First and The Transaction Data Packet to Host:-
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                SyncReversalToHost(AppPreference.getReversal()) { isSyncToHost, transMsg ->
                    hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            VFService.showToast(transMsg)
                        }
                    }
                }
            }
        }
    }


    private fun onProgress(msg: String, isResp: Boolean, isProgress: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (isProgress) {
                setProgressTitle(msg)
            } else {
                hideProgress()
                if (!isResp) {
                    getInfoDialog("Error", msg) {}
                }
            }
        }
    }

    var hasBenifitSlabTable = false
    var benifitSlabTable: BenifitSlabTable? = null
    override fun getBenifitTable(schemeId: String, tenure: String): BenifitSlabTable? {
        for (e in benifitTable) {
            if (e.emiSchemeId == schemeId && e.tenure == tenure) {
                benifitSlabTable = e
                hasBenifitSlabTable = true
            }
        }
        if (hasBenifitSlabTable) {
            hasBenifitSlabTable = false
        } else {
            benifitSlabTable = null
            hasBenifitSlabTable = false
        }
        return benifitSlabTable
        //return benifitTable?.lastOrNull() { it.emiSchemeId == schemeId && it.tenure == tenure }
    }

    var hasEmiSchemeTable = false
    var emiSchemeTables: EmiSchemeTable? = null
    override fun getEmiSchemeTable(schemeId: String): EmiSchemeTable? {
        //  var emischemetables =  emiSchemeTable.forEach { it } as EmiSchemeTable
        for (e in emiSchemeTable) {
            if (e.schemeId == schemeId) {
                emiSchemeTables = e
                hasEmiSchemeTable = true
            }
        }
        if (hasEmiSchemeTable) {
            hasEmiSchemeTable = false
        } else {
            emiSchemeTables = null
            hasEmiSchemeTable = false
        }
        return emiSchemeTables
    }

    override fun onEvents(event: VxEvent) {
        when (event) {
            is VxEvent.PayEmi -> {
                val total =
                    event.amount + event.tenure.proccesingFee.toFloat() / 100 + ((event.tenure.processingRate.toFloat() / 100) * (event.amount - (event.tenure.emiAmount?.discount
                        ?: 0f))) / 100 - (event.tenure.emiAmount?.cashBack
                        ?: 0f) - (event.tenure.emiAmount?.discount
                        ?: 0f) + (event.tenure.emiAmount?.totalInterest ?: 0f)
                /*   val total =
                       event.amount + (event.tenure.proccesingFee.toFloat()/100+((event.tenure.processingRate.toFloat()/100) * (event.amount-(event.tenure.emiAmount?.discount ?: 0f)))) - (event.tenure.emiAmount?.cashBack
                           ?: 0f) - (event.tenure.emiAmount?.discount
                           ?: 0f) + (event.tenure.emiAmount?.totalInterest
                           ?: 0f)*/
                val cdm = EmiCustomerDetails().apply {
                    emiBin = event.tenure.emiBinValue
                    this.issuerId = event.issuerId
                    this.emiSchemeId = event.tenure.emiSchemeId
                    cashback = event.tenure.emiAmount?.getCashback() ?: ""
                    cashDiscountAmt = event.tenure.emiAmount?.getDiscount() ?: ""
                    transactionAmt =
                        "%d".format((mAmount * 100).toInt())/*event.tenure.emiAmount?.getTransactionAmt() ?: ""*/
                    //      loanAmt = "%d".format((mAmount * 100).toInt())/*event.tenure.emiAmount?.getTransactionAmt() ?: ""*/
                    var loanamounts1 =
                        "${((event.tenure.emiAmount?.principleAmt ?: 0f) - (event.tenure.emiAmount?.discount ?: 0f)).toFormatString()}"
                    //  var loanAmts =((event.tenure.emiAmount?.principleAmt ?: 0f)-(event.tenure.emiAmount?.discount ?: 0f))
                    loanAmt = "%d".format((loanamounts1.toDouble() * 100).toInt())
                    this.tenure = event.tenure.tenure
                    roi = event.tenure.emiAmount?.getRoi() ?: ""
                    monthlyEmi = event.tenure.emiAmount?.getMonthEmi() ?: ""
                    var netPays = total.toFormatString()
                    netPay = "%d".format((netPays.toDouble() * 100).toInt())
                    //     netPay = "%d".format((total * 100).toInt())
                    processingFee =
                        ((event.tenure.proccesingFee.toFloat() / 100) + ((event.tenure.processingRate.toFloat() / 100) * ((event.amount - (event.tenure.emiAmount?.discount
                            ?: 0f))) / 100)).toFormatString()
                    totalInterest = event.tenure.emiAmount?.totalInterest?.toFormatString()
                        ?: 0f.toFormatString()
                    if (event.tenure.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE) {
                        if (cashback == "")
                            cashback = "0"
                        cashBackPercent = (cashback.toFloat() / 100).toString()
                        isCashBackInPercent = false
                    } else {
                        cashBackPercent = event.tenure.emiAmount?.cashBackpercent.toString()
                        isCashBackInPercent = true
                    }
                    // isCashBackInPercent = event.tenure.emiAmount?.benefitCalc==EBenefitCalculation.PERCENTAGE_VALUE
                }

                val intent = Intent().apply {
                    putExtra("emi", cdm)
                    putExtra("cardprocess", cardProcessedData)
                }

                setResult(Activity.RESULT_OK, intent)
                finish()
            }


        }
    }

    //region============Emi Filter Dialog, Adaper, Holder==============


    private fun openBankList() {
        val emiSchemeGroupTable: List<EmiSchemeGroupTable> by lazy { EmiSchemeGroupTable.selectFromEmiSchemeGroupProductTable() }
        val emiSchemeTableSet = mutableSetOf<String>()

        var int: Int = 0
        var emischemeid = ArrayList<String>()
        for (e in emiSchemeGroupTable) {
            if (e.isActive == "1" && e.emischemeIds.contains(","))
                emischemeid = e.emischemeIds.split(",") as ArrayList<String>
            else if (e.isActive == "1") {
                for (j in emiSchemeTable) {  //114 for EmiSchemeTable
                    val currDt = getCurrentDate()
                    if (currDt.compareTo(j.startDate) >= 0 && currDt.compareTo(j.endDate) <= 0 && mAmount <= j.maxValue.toDouble() / 100 && mAmount >= j.minValue.toDouble() / 100) {
                        if (j.isActive == "1" && e.isActive == "1" && j.emiSchemeId == e.emischemeIds) {
                            emiSchemeTableSet.add(j.schemeId)
                        }
                    }
                }
            }
        }
        for (e in emischemeid.iterator()) {
            for (j in emiSchemeTable) {
                val currDt = getCurrentDate()
                if (currDt.compareTo(j.startDate) >= 0 && currDt.compareTo(j.endDate) <= 0 && mAmount <= j.maxValue.toDouble() / 100 && mAmount >= j.minValue.toDouble() / 100) {
                    if (j.isActive == "1" && j.emiSchemeId == emischemeid.get(int)) {
                        emiSchemeTableSet.add(j.schemeId)
                    }
                }
            }
            int++

        }

        if (emiSchemeTableSet.size > 0) {
            Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_bank_list)
                setCancelable(false)

                val banlRl = findViewById<RecyclerView>(R.id.item_bank_rl)

                val currentSet = mutableSetOf<String>()

                for (e in pagerAdapter.list) {
                    currentSet.add(e.ipt.issuerId)
                }

                val filterList = mutableListOf<Array<String>>()
                filterList.add(arrayOf("0", "All", "1"))
                for (e in mListFragment) {
                    val item = Array(3) { "" }
                    item[0] = e.ipt.issuerId
                    item[1] = e.ipt.issuerName
                    item[2] = if (e.ipt.issuerId in currentSet) "1" else {
                        filterList[0][2] = "0"
                        "0"
                    }
                    filterList.add(item)
                }

                findViewById<View>(R.id.dbl_cancel_btn).setOnClickListener { dismiss() }

                //  banlRl.layoutManager = GridLayoutManager(this.context, 2)
                banlRl.layoutManager = LinearLayoutManager(this.context)
                val filterAdapter = EmiFilterAdapter(filterList)
                banlRl.adapter = filterAdapter


                fun filterAdapter(filterResult: (List<EmiDetailFragment>, List<String>) -> Unit) {
                    val selSet = mutableSetOf<String>()
                    val re = filterAdapter.list
                    for (i in 1..re.lastIndex) {
                        if (re[i][2] == "1") {
                            selSet.add(re[i][0])
                        }
                    }
                    // resetting pagerAdapter
                    val tl = mutableListOf<String>()
                    val fl = mListFragment.filter {
                        if (it.ipt.issuerId in selSet) {
                            tl.add(it.ipt.issuerName)
                            true
                        } else false
                    }
                    filterResult(fl, tl)
                }

                findViewById<View>(R.id.dbl_ok_btn).setOnClickListener {
                    filterAdapter { fl, tl ->
                        if (tl.isEmpty()) {
                            showToast("Select at least one bank.")
                        } else {
                            dismiss()
                            pagerAdapter.list = fl
                            pagerAdapter.title = tl
                            pagerAdapter.notifyDataSetChanged()
                        }
                    }
                }

            }.show()
        } else {
            alertBoxWithAction(null, null,
                getString(R.string.emis),
                getString(R.string.no_emi),
                false,
                getString(R.string.positive_button_ok),
                { alertPositiveCallback ->
                    if (alertPositiveCallback) {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                },
                {})
        }

        //  logic for single and multiple bank
        /*      val currentSet = mutableSetOf<String>()

              for (e in pagerAdapter.list) {
                  currentSet.add(e.ipt.issuerId)
              }

              val filterList = mutableListOf<Array<String>>()
              filterList.add(arrayOf("0", "All", "1"))
              for (e in mListFragment) {
                  val item = Array(3) { "" }
                  item[0] = e.ipt.issuerId
                  item[1] = e.ipt.issuerName
                  item[2] = if (e.ipt.issuerId in currentSet) "1" else {
                      filterList[0][2] = "0"
                      "0"
                  }
                  filterList.add(item)
              }

              if(null !=filterList && filterList.size == 2){

                  //   val filterAdapter = EmiFilterAdapter(filterList)
                  fun filterAdapter(filterResult: (List<EmiDetailFragment>, List<String>) -> Unit) {
                      val selSet = mutableSetOf<String>()
                      val tl = mutableListOf<String>()
                      val fl = mListFragment.filter {
                          if (it.ipt.issuerId in "53") {
                              tl.add(it.ipt.issuerName)
                              true
                          } else false
                      }
                      filterResult(fl, tl)
                  }

                  filterAdapter { fl, tl ->
                      pagerAdapter.list = fl
                      pagerAdapter.title = tl
                      pagerAdapter.notifyDataSetChanged()
                  }
              }
              else{
                  Dialog(this).apply {
                      requestWindowFeature(Window.FEATURE_NO_TITLE)
                      setContentView(R.layout.dialog_bank_list)
                      setCancelable(false)

                      val banlRl = findViewById<RecyclerView>(R.id.item_bank_rl)

                      val currentSet = mutableSetOf<String>()

                      for (e in pagerAdapter.list) {
                          currentSet.add(e.ipt.issuerId)
                      }

                      val filterList = mutableListOf<Array<String>>()
                      filterList.add(arrayOf("0", "All", "1"))
                      for (e in mListFragment) {
                          val item = Array(3) { "" }
                          item[0] = e.ipt.issuerId
                          item[1] = e.ipt.issuerName
                          item[2] = if (e.ipt.issuerId in currentSet) "1" else {
                              filterList[0][2] = "0"
                              "0"
                          }
                          filterList.add(item)
                      }

                      findViewById<View>(R.id.dbl_cancel_btn).setOnClickListener { dismiss() }

                      banlRl.layoutManager = GridLayoutManager(this.context, 2)
                      val filterAdapter = EmiFilterAdapter(filterList)
                      banlRl.adapter = filterAdapter


                      fun filterAdapter(filterResult: (List<EmiDetailFragment>, List<String>) -> Unit) {
                          val selSet = mutableSetOf<String>()
                          val re = filterAdapter.list
                          for (i in 1..re.lastIndex) {
                              if (re[i][2] == "1") {
                                  selSet.add(re[i][0])
                              }
                          }
                          // resetting pagerAdapter
                          val tl = mutableListOf<String>()
                          val fl = mListFragment.filter {
                              if (it.ipt.issuerId in selSet) {
                                  tl.add(it.ipt.issuerName)
                                  true
                              } else false
                          }
                          filterResult(fl, tl)
                      }

                      findViewById<View>(R.id.dbl_ok_btn).setOnClickListener {
                          filterAdapter { fl, tl ->
                              if (tl.isEmpty()) {
                                  showToast("Select at least one bank.")
                              } else {
                                  dismiss()
                                  pagerAdapter.list = fl
                                  pagerAdapter.title = tl
                                  pagerAdapter.notifyDataSetChanged()
                              }
                          }
                      }

                  }.show()
              }*/
    }


    private inner class EmiFilterAdapter(val list: List<Array<String>>) :
        RecyclerView.Adapter<EmiFilterHolder>() {

        private var totalCheck = 0

        private val LAST = list.size - 1

        init {
            for (index in 1..list.lastIndex) {
                if (list[index][2] == "1") totalCheck += 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmiFilterHolder {
            return EmiFilterHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_select_bank, parent, false),
                ::onCheck
            )
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: EmiFilterHolder, position: Int) {

            with(holder.ckBX as CheckBox) {
                isChecked = list[position][2] == "1"
                text = list[position][1]
            }
            if (list[position][0] == "53")
                holder.bnkLogo.setBackgroundResource(R.drawable.amex_logo)
        }

        private fun onCheck(position: Int, isCheck: Boolean) {
            val z = if (isCheck) "1" else "0"
            if (position == 0) {
                for (i in list) {
                    i[2] = z
                }
                totalCheck = if (isCheck) {
                    LAST
                } else {
                    0
                }
            } else {
                list[position][2] = z
                if (isCheck) {
                    totalCheck += 1
                } else {
                    totalCheck -= 1
                }
            }
            list[0][2] = if (totalCheck == LAST) "1" else "0"
            notifyDataSetChanged()
        }

    }


    private inner class EmiFilterHolder(val v: View, cb: (Int, Boolean) -> Unit) :
        RecyclerView.ViewHolder(v) {
        var ckBX = v.findViewById<CheckBox>(R.id.ck_bx)
        var bnkLogo = v.findViewById<ImageView>(R.id.bnk_logo)

        init {
            (ckBX as CheckBox).run {
                setOnClickListener { _ ->
                    cb(adapterPosition, isChecked)
                }
            }
        }
    }

    override fun onBackPressed() {
        try {
            if (::pagerAdapter.isInitialized) {
                val frag = pagerAdapter.list[mCurrentFragment]
                val tdm = frag.getSelectedTenure()
                if (null != tdm && tdm.isChecked) {
                    alertBoxWithAction(null, null,
                        getString(R.string.emi_transaction),
                        getString(R.string.emi_check),
                        false,
                        getString(R.string.positive_button_ok),
                        { alertPositiveCallback ->
                            if (alertPositiveCallback) {
                            }

                        },
                        {})
                } else {
                    val intent = Intent().apply {
                        if (null != cardProcessedData)
                            putExtra("cardprocess", cardProcessedData)
                    }
                    setResult(Activity.RESULT_CANCELED, intent)
                    super.onBackPressed()
                }
            } else {
                showToast("Pager adapter not initialized yet")
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}


class EmiDetailFragment : Fragment() {

    lateinit var ipt: IssuerParameterTable
    private val mEmiViewList = mutableListOf<EmiView>()
    private val mAmount: Double by lazy { arguments?.getDouble("amount", 0.0) ?: 0.0 }

    private val mSchemeAdpater: SchemeAdapter by lazy {
        SchemeAdapter(mEmiViewList, mAmount.toFloat())
    }

    private val emiSchemeTable: List<EmiSchemeTable> by lazy { EmiSchemeTable.selectFromEmiSchemeTable() }
    private val emiSchemeGroupTable: List<EmiSchemeGroupTable> by lazy { EmiSchemeGroupTable.selectFromEmiSchemeGroupProductTable() }
    private var iBenefit: IBenefitTable? = null

    var issuerDataModel: IssuerDataModel? = null

    private val isBank: Boolean by lazy { arguments?.getBoolean("isbank", true) ?: true }
    private var binding: FragmentEmiDetailBinding? = null

    companion object {
        fun getInstance(ip: IssuerParameterTable, arg: Bundle): EmiDetailFragment {
            val frag = EmiDetailFragment()
            with(frag) {
                ipt = ip
                arguments = arg
                if (ipt.issuerTypeId == "2" || ipt.issuerTypeId == "3") {
                    val emiBinData = EmiBinTable.selectFromEmiBinTable(ipt.issuerId)
                    if (emiBinData != null) {

                        issuerDataModel = IssuerDataModel().apply { set(ipt) }

                        val emiSchemeTableSet = mutableSetOf<String>()
                        /* for (e in emiSchemeTable) {
                             if (e.isActive == "1") {
                                 emiSchemeTableSet.add(e.schemeId)
                                // emiSchemeTableSet.add(e.emiSchemeId)
                             }
                         }*/


                        var int: Int = 0
                        var emischemeid = ArrayList<String>()
                        for (e in emiSchemeGroupTable) {
                            if (e.isActive == "1" && e.emischemeIds.contains(","))
                                emischemeid = e.emischemeIds.split(",") as ArrayList<String>
                            else if (e.isActive == "1") {
                                for (j in emiSchemeTable) {  //114 for EmiSchemeTable
                                    val currDt = getCurrentDate()
                                    if (currDt.compareTo(j.startDate) >= 0 && currDt.compareTo(j.endDate) <= 0 && mAmount <= j.maxValue.toDouble() / 100 && mAmount >= j.minValue.toDouble() / 100) {
                                        if (j.isActive == "1" && e.isActive == "1" && j.emiSchemeId == e.emischemeIds) {
                                            emiSchemeTableSet.add(j.schemeId)
                                        }
                                    }
                                }
                            }
                        }
                        for (e in emischemeid.iterator()) {
                            for (j in emiSchemeTable) {
                                val currDt = getCurrentDate()
                                if (currDt.compareTo(j.startDate) >= 0 && currDt.compareTo(j.endDate) <= 0 && mAmount <= j.maxValue.toDouble() / 100 && mAmount >= j.minValue.toDouble() / 100) {
                                    if (j.isActive == "1" && j.emiSchemeId == emischemeid.get(int)) {
                                        emiSchemeTableSet.add(j.schemeId)
                                    }
                                }
                            }
                            int++

                        }
                        //112 for Scheme
                        val schemeTables = SchemeTable.selectFromSchemeTable(ipt.issuerId)
                            .filter { it.isActive == "1" && emiSchemeTableSet.contains(it.schemeId) }


                        for (j in schemeTables.indices) {
                            //  for (k in)

                            val schemeDataModel = SchemeDataModel().apply { set(schemeTables[j]) }
                            issuerDataModel?.schemeDataModel?.add(schemeDataModel)

                            //Tenure table
                            val tenureTables =
                                TenureTable.selectFromTenureTable(schemeTables[j].schemeId)

                            val tenArr = Array(tenureTables.size) { TenureDataModel() }

                            for (i in tenureTables.indices) {
                                tenArr[i].set(tenureTables[i])
                                tenArr[i].emiBinValue = emiBinData.binValue
                            }

                            Arrays.sort(tenArr)

                            issuerDataModel?.schemeDataModel?.get(j)?.tenureDataModel = tenArr
                        }

                    }
                }

            }

            return frag
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmiDetailBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
    }

    @SuppressLint("WrongConstant")
    private fun initUI(v: View) {

        if (issuerDataModel != null) {
            val idm = issuerDataModel as IssuerDataModel

            mSchemeAdpater.isBank = isBank
            binding?.fedRv?.apply {
                layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)
                adapter = mSchemeAdpater
            }

            if (mEmiViewList.isEmpty()) {
                for (e in idm.schemeDataModel) {
                    mEmiViewList.add(e)
                    for (ea in e.tenureDataModel) {
                        attachEmiAmountWithValidation(e, ea, mAmount.toFloat(), iBenefit)
                        ea.isChecked = !isBank
                        mEmiViewList.add(ea)
                    }
                }
                mSchemeAdpater.notifyDataSetChanged()
            }
        }

    }

    fun getSelectedTenure(): TenureDataModel? = mSchemeAdpater.selectedTenure

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IBenefitTable) iBenefit = context
    }

    override fun onDetach() {
        super.onDetach()
        iBenefit = null
    }


}

class EmiPagerAdapter(
    fm: FragmentManager,
    var list: List<EmiDetailFragment>,
    var title: List<String>
) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return list[position]
    }

    override fun getCount(): Int = list.size

    override fun getPageTitle(position: Int): CharSequence {
        return title[position]
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

}


class SchemeAdapter(private var emiList: List<EmiView>, private var amount: Float) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isBank = true
    var selectedTenure: TenureDataModel? = null

    var currentSelection = -1
    private var preSelection = -1

    override fun getItemCount(): Int = emiList.size

    override fun getItemViewType(position: Int): Int {
        return emiList[position].type.ordinal
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {

        val v = LayoutInflater.from(viewGroup.context).inflate(
            if (i == EmiViewType.SCHEME.ordinal) R.layout.item_scheme_v2 else R.layout.item_emi_detail,
            viewGroup,
            false
        )
        return if (i == EmiViewType.SCHEME.ordinal) SchemeHolderV2(v) else TenureHolderV2(
            v,
            ::onTenureClicked
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (emiList[i].type.ordinal == EmiViewType.SCHEME.ordinal) {
            val data = emiList[i] as SchemeDataModel
            val hold = holder as SchemeHolderV2
            with(hold) {
                item_view.findViewById<BHTextView>(R.id.isv2_scheme_name_tv)?.text = data.schemeName
            }
        } else {
            val data = emiList[i] as TenureDataModel
            val hold = holder as TenureHolderV2

            val totalPayment =
                amount + data.proccesingFee.toFloat() / 100 + ((data.processingRate.toFloat() / 100) * (amount - (data.emiAmount?.discount
                    ?: 0f))) / 100 - (data.emiAmount?.cashBack
                    ?: 0f) - (data.emiAmount?.discount ?: 0f) + (data.emiAmount?.totalInterest
                    ?: 0f)

            with(hold) {

                if (data.isChecked) {
                    item_view.findViewById<ImageView>(R.id.ied_check_iv)?.background =
                        ContextCompat.getDrawable(
                            holder.item_view.context,
                            R.drawable.ic_check_box_selected
                        )
                } else {
                    item_view.findViewById<ImageView>(R.id.ied_check_iv)?.background =
                        ContextCompat.getDrawable(
                            holder.item_view.context,
                            R.drawable.ic_check_box_unselected
                        )
                }

                val tenMsg = "Tenure : ${data.tenure} Month"
                val msg = "Interest Rate :${(data.emiAmount?.roi)?.toFormatString()}%"
                item_view.findViewById<BHTextView>(R.id.ied_tenure_tv)?.text = tenMsg
                item_view.findViewById<BHTextView>(R.id.ied_interest_tv)?.text = msg

                item_view.findViewById<BHTextView>(R.id.BHTextView4)?.visibility = View.VISIBLE
                val iedProcessingFeeTV =
                    item_view.findViewById<BHTextView>(R.id.ied_processing_fee_tv)

                // if(data.proccesingFee.toFloat() > 0.0) {
                val proFee =
                    "${appContext.getString(R.string.rupees_symbol)} " + ((data.proccesingFee.toFloat() / 100) + ((data.processingRate.toFloat() / 100) * ((amount - (data.emiAmount?.discount
                        ?: 0f))) / 100)).toFormatString()

                item_view.findViewById<BHTextView>(R.id.BHTextView4)?.visibility = View.VISIBLE
                iedProcessingFeeTV?.visibility = View.VISIBLE
                iedProcessingFeeTV?.text = proFee
                //     }

                val amt =
                    "${appContext.getString(R.string.rupees_symbol)} ${amount.toFormatString()}"
                val iedAmountTV = item_view.findViewById<BHTextView>(R.id.ied_amount_tv)
                val iedTotalInterestTV =
                    item_view.findViewById<BHTextView>(R.id.ied_total_interest_tv)
                val iedMonthEmiTV = item_view.findViewById<BHTextView>(R.id.ied_month_emi_tv)
                iedAmountTV?.text = amt
                iedAmountTV?.text =
                    "${appContext.getString(R.string.rupees_symbol)}${((data.emiAmount?.principleAmt ?: 0f) - (data.emiAmount?.discount ?: 0f)).toFormatString()}"

                val toint =
                    "${appContext.getString(R.string.rupees_symbol)} ${data.emiAmount?.totalInterest?.toFormatString()}"
                iedTotalInterestTV?.text = toint

                val monEmi =
                    "${appContext.getString(R.string.rupees_symbol)} ${data.emiAmount?.monthlyEmi?.toFormatString()}"
                iedMonthEmiTV?.text = monEmi

                val iedDiscountLL = item_view.findViewById<LinearLayout>(R.id.ied_discount_ll)
                val iedExtraLL = item_view.findViewById<LinearLayout>(R.id.ied_extra_ll)
                val iedExtraTv = item_view.findViewById<BHTextView>(R.id.ied_extra_tv_lbl)
                val iedDiscountTv = item_view.findViewById<BHTextView>(R.id.ied_discount_tv_lbl)
                when {
                    data.emiAmount?.benefitModel == EBenefitModel.DISCOUNT -> {
                        iedDiscountLL?.visibility = View.VISIBLE
                        iedExtraLL?.visibility = View.VISIBLE
                        iedExtraTv?.text = when {
                            data.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE -> "Total Discount"
                            data.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE -> "Total Discount"
                            else -> "Total Discount "
                        }
                        iedDiscountTv?.text = when {
                            data.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE -> "Discount (percent[%])"
                            data.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE -> "Discount (flat)"
                            else -> "Discount "
                        }

                        if (data.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE) {
                            val discount = data.emiAmount?.discount
                            if (discount != null) {
                                if (discount > 0f) {
                                    iedDiscountLL?.visibility = View.VISIBLE
                                    iedExtraLL?.visibility = View.VISIBLE
                                    var percentage = (discount / amount) * 100
                                    iedDiscountTv?.text =
                                        "${data.emiAmount?.discountpercent?.toFormatString()}${"%"}"
                                    //   item_view.ied_discount_tv.text = "${percentage.toFormatString()}${"%"}"
                                    iedExtraTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.discount?.toFormatString()}"
                                } else {
                                    iedDiscountLL?.visibility = View.GONE
                                    iedExtraLL?.visibility = View.GONE
                                }
                            }
                        } else if (data.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE) {
                            val discount = data.emiAmount?.discount
                            if (discount != null) {
                                if (discount > 0f) {
                                    iedDiscountLL?.visibility = View.VISIBLE
                                    iedExtraLL?.visibility = View.VISIBLE
                                    iedDiscountTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.discount?.toFormatString()}"
                                    iedExtraTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.discount?.toFormatString()}"
                                } else {
                                    iedDiscountLL?.visibility = View.GONE
                                    iedExtraLL?.visibility = View.GONE
                                }
                            }
                        } else {
                            val discount = data.emiAmount?.discount
                            if (discount != null) {
                                if (discount > 0f) {
                                    iedDiscountLL?.visibility = View.VISIBLE
                                    iedExtraLL?.visibility = View.VISIBLE
                                    iedDiscountTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.discount?.toFormatString()}"
                                    iedExtraTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.discount?.toFormatString()}"
                                } else {
                                    iedDiscountLL?.visibility = View.GONE
                                    iedExtraLL?.visibility = View.GONE
                                }
                            }

                        }

                        //    item_view.ied_discount_tv.text =  "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.discount?.toFormatString()}"
                        //need to check
                    }
                    data.emiAmount?.benefitModel == EBenefitModel.CASH_BACK -> {
                        iedDiscountLL?.visibility = View.VISIBLE
                        iedExtraLL?.visibility = View.VISIBLE

                        iedExtraTv?.text = when {
                            data.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE -> "Total Cashback"
                            data.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE -> "Total Cashback"
                            else -> "Total Cashback "
                        }
                        iedDiscountTv?.text = when {
                            data.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE -> {
                                "Cashback (percent[%])"
                            }
                            data.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE -> "Cashback (flat)"
                            else -> "Cashback "

                        }


                        if (data.emiAmount?.benefitCalc == EBenefitCalculation.PERCENTAGE_VALUE) {
                            val cashBack = data.emiAmount?.cashBack
                            if (cashBack != null) {
                                if (cashBack > 0f) {
                                    iedDiscountLL?.visibility = View.VISIBLE
                                    iedExtraLL?.visibility = View.VISIBLE
                                    var percentage = (cashBack / amount) * 100
                                    iedDiscountTv?.text =
                                        "${data.emiAmount?.cashBackpercent?.toFormatString()}${"%"}"
                                    //      item_view.ied_discount_tv.text = "${percentage.toFormatString()}${"%"}"
                                    iedExtraTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.cashBack?.toFormatString()}"
                                } else {
                                    iedDiscountLL?.visibility = View.GONE
                                    iedExtraLL?.visibility = View.GONE
                                }
                            }
                        } else if (data.emiAmount?.benefitCalc == EBenefitCalculation.FIXED_VALUE) {
                            var cashBack = data.emiAmount?.cashBack?.toFloat()
                            if (cashBack != null) {
                                if (cashBack > 0f) {
                                    iedDiscountLL?.visibility = View.VISIBLE
                                    iedExtraLL?.visibility = View.VISIBLE
                                    iedDiscountTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.cashBack?.toFormatString()}"
                                    iedExtraTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.cashBack?.toFormatString()}"
                                } else {
                                    iedDiscountLL?.visibility = View.GONE
                                    iedExtraLL?.visibility = View.GONE
                                }
                            }

                        } else {

                            var cashBack = data.emiAmount?.cashBack?.toFloat()
                            if (cashBack != null) {
                                if (cashBack > 0f) {
                                    iedDiscountLL?.visibility = View.VISIBLE
                                    iedExtraLL?.visibility = View.VISIBLE
                                    iedDiscountTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.cashBack?.toFormatString()}"
                                    iedExtraTv?.text =
                                        "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.cashBack?.toFormatString()}"
                                } else {
                                    iedDiscountLL?.visibility = View.GONE
                                    iedExtraLL?.visibility = View.GONE
                                }
                            }

                        }

                        //  item_view.ied_discount_tv.text = "${appContext.getString(R.string.rupees_symbol)}${data.emiAmount?.cashBack?.toFormatString()}"
                        // need to check
                    }
                    else -> {
                        iedExtraLL?.visibility = View.GONE
                        iedDiscountLL?.visibility = View.GONE
                    }
                }
                val iedTotalAmountTv = item_view.findViewById<BHTextView>(R.id.ied_total_amt_tv)
                val tamt =
                    "${appContext.getString(R.string.rupees_symbol)} ${totalPayment.toFormatString()}"
                iedTotalAmountTv?.text = tamt
            }

        }

    }

    var isShowing = true
    private fun onTenureClicked(position: Int) {
        if (isBank) {
            preSelection = currentSelection
            currentSelection = position

            if (preSelection >= 0 && emiList[preSelection] is TenureDataModel) {
                (emiList[preSelection] as TenureDataModel).isChecked = false
                notifyItemChanged(preSelection)
            }

            if (currentSelection >= 0 && emiList[currentSelection] is TenureDataModel) {
                val item = emiList[currentSelection] as TenureDataModel
                if (preSelection == currentSelection && isShowing) {
                    isShowing = false
                    item.isChecked = false
                    return
                }
                item.isChecked = true
                selectedTenure = item
                isShowing = true
                notifyItemChanged(currentSelection)
            }
        } else {
            val item = emiList[position] as TenureDataModel
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
        }

    }


}


class SchemeHolderV2(val item_view: View) : RecyclerView.ViewHolder(item_view)


class TenureHolderV2(val item_view: View, callback: (Int) -> Unit) :
    RecyclerView.ViewHolder(item_view) {

    init {
        itemView.findViewById<ConstraintLayout>(R.id.ied_parent_cl).apply {

            setOnClickListener {
                callback(adapterPosition)
            }

        }
    }


}



