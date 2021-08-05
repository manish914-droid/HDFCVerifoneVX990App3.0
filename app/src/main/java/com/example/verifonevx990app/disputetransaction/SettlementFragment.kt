package com.example.verifonevx990app.disputetransaction

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customneumorphic.NeumorphCardView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.digiPOS.uploadPendingDigiPosTxn
import com.example.verifonevx990app.emv.transactionprocess.SyncReversalToHost
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.offlinemanualsale.SyncOfflineSaleSettlementToHost
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.AppPreference.GENERIC_REVERSAL_KEY
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettlementFragment : Fragment(R.layout.activity_settlement_view) {

    private var batchList: MutableList<BatchFileDataTable> = mutableListOf()
    private val settlementAdapter: SettlementAdapter by lazy { SettlementAdapter(batchList) }
    private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private var empty_view_placeholder: ImageView? = null
    private var settlement_rv: RecyclerView? = null
    private var settlement_batch_btn: ExtendedFloatingActionButton? = null
    private var lv_heading_view: LinearLayout? = null
    private var settlementByteArray: ByteArray? = null
    private var back_image_button: ImageView? = null
    private var sub_header_text: TextView? = null
    private var emptyBatchTv: TextView? = null
    private var header_cardview: NeumorphCardView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settlement_rv = view.findViewById(R.id.settlement_rv)
        settlement_batch_btn = view.findViewById(R.id.settlement_batch_btn)
        back_image_button = view.findViewById(R.id.back_image_button)
        sub_header_text = view.findViewById(R.id.sub_header_text)
        back_image_button?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        sub_header_text?.text = title
        lv_heading_view = view.findViewById(R.id.lv_heading_view)
        empty_view_placeholder = view.findViewById(R.id.empty_view_placeholder)
        header_cardview = view.findViewById(R.id.header_cardview)
        emptyBatchTv=view.findViewById(R.id.batch_emty_txt)
        view.findViewById<ImageView>(R.id.header_Image)?.setImageResource(R.drawable.ic_new_settlement)
        batchList = BatchFileDataTable.selectBatchData()
        getAndInflateSettlementData()

        settlement_batch_btn?.setOnClickListener {
            enableDisableSettlementButton(false)
            //when no reversal found:-
            if (TextUtils.isEmpty(AppPreference.getString(GENERIC_REVERSAL_KEY))) {
                (activity as MainActivity).showProgress(getString(R.string.please_wait_offline_sale_sync))
                SyncOfflineSaleSettlementToHost { offlineSaleStatus, responseValidationMsg ->
                    if (offlineSaleStatus) {
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as MainActivity).hideProgress()
                            if (VFService.vfPrinter?.status != 240 && VFService.vfPrinter?.status == 0) {
                                /* if (batchList.size > 0) {*/
                                (activity as MainActivity).alertBoxWithAction(
                                    null,
                                    null,
                                    getString(R.string.settlement),
                                    getString(R.string.settle_batch_hint),
                                    true,
                                    getString(R.string.positive_button_yes),
                                    {
                                        (activity as MainActivity).showProgress()
                                       viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                            Log.e("UPLOAD DIGI"," ----------------------->  START")
                                            uploadPendingDigiPosTxn(activity as BaseActivity) {
                                                Log.e("UPLOAD DIGI"," ----------------------->  BEFOR PRINT")
                                                PrintUtil(activity).printDetailReportupdate(
                                                    batchList,
                                                    activity
                                                ) { detailPrintStatus ->
                                                    if (detailPrintStatus) {
                                                        GlobalScope.launch(
                                                            Dispatchers.IO
                                                        ) {
                                                            val data =
                                                                CreateSettlementPacket(
                                                                    ProcessingCode.SETTLEMENT.code,
                                                                    batchList
                                                                ).createSettlementISOPacket()
                                                            settlementByteArray =
                                                                data.generateIsoByteRequest()
                                                            try {
                                                                (activity as MainActivity).settleBatch(
                                                                    settlementByteArray
                                                                ) {
                                                                    if (!it)
                                                                        enableDisableSettlementButton(
                                                                            true
                                                                        )
                                                                }
                                                            } catch (ex: Exception) {
                                                                (activity as MainActivity).hideProgress()
                                                                enableDisableSettlementButton(
                                                                    true
                                                                )
                                                                ex.printStackTrace()
                                                            }
                                                        }
                                                    } else {
                                                        (activity as MainActivity).hideProgress()
                                                        GlobalScope.launch(Dispatchers.Main) {
                                                            (activity as MainActivity).alertBoxWithAction(
                                                                null,
                                                                null,
                                                                getString(R.string.printer_error),
                                                                getString(R.string.please_check_printing_roll),
                                                                true,
                                                                getString(R.string.yes),
                                                                {
                                                                    val data =
                                                                        CreateSettlementPacket(
                                                                            ProcessingCode.SETTLEMENT.code,
                                                                            batchList
                                                                        ).createSettlementISOPacket()
                                                                    settlementByteArray =
                                                                        data.generateIsoByteRequest()
                                                                    try {
                                                                        (activity as MainActivity).hideProgress()
                                                                        GlobalScope.launch(
                                                                            Dispatchers.IO
                                                                        ) {
                                                                            (activity as MainActivity).settleBatch(
                                                                                settlementByteArray
                                                                            ) {
                                                                                if (!it)
                                                                                    enableDisableSettlementButton(
                                                                                        true
                                                                                    )
                                                                            }
                                                                        }
                                                                    } catch (ex: Exception) {
                                                                        (activity as MainActivity).hideProgress()
                                                                        enableDisableSettlementButton(
                                                                            true
                                                                        )
                                                                        ex.printStackTrace()
                                                                    }
                                                                },
                                                                {})
                                                        }
                                                    }
                                                }

                                            }

                                        }
                                    },
                                    { navigateToMain() })
                                /* } else
                                     GlobalScope.launch(Dispatchers.Main) {
                                         enableDisableSettlementButton(true)
                                         VFService.showToast(getString(R.string.empty_batch_data))
                                     }*/
                            } else {
                                enableDisableSettlementButton(true)
                                VFService.showToast(getString(R.string.printing_roll_error))
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            enableDisableSettlementButton(true)
                            (activity as MainActivity).hideProgress()
                            VFService.showToast(getString(R.string.offline_sale_upload_fails_please_try_again) + "\n" + responseValidationMsg)
                            (activity as MainActivity).alertBoxWithAction(null,
                                null,
                                getString(R.string.offline_upload),
                                getString(R.string.offline_sale_failed_to_upload),
                                false,
                                getString(R.string.positive_button_ok),
                                {},
                                {})
                        }
                    }
                }

            } else {
                //Send Reversal Data to Server First Then Settle Batch will happen:-
                (activity as MainActivity).showProgress("Sending Reversal.....")
                SyncReversalToHost(AppPreference.getReversal()) { status, msg ->
                    if (status) {
                        AppPreference.clearReversal()
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as MainActivity).hideProgress()
                            (activity as MainActivity).showProgress(getString(R.string.please_wait_offline_sale_sync))
                        }
                        SyncOfflineSaleSettlementToHost { offlineSaleStatus, responseValidationMsg ->
                            if (offlineSaleStatus) {
                                GlobalScope.launch(Dispatchers.Main) {
                                    (activity as MainActivity).hideProgress()
                                    if (VFService.vfPrinter?.status != 240 && VFService.vfPrinter?.status == 0) {
                                        /* if (batchList.size > 0) {*/
                                        (activity as MainActivity).alertBoxWithAction(
                                            null,
                                            null,
                                            getString(R.string.settlement),
                                            getString(R.string.settle_batch_hint),
                                            true,
                                            getString(R.string.positive_button_yes),
                                            {
                                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                                    Log.e("UPLOAD DIGI"," ----------------------->  START")
                                                    uploadPendingDigiPosTxn(activity as BaseActivity) {
                                                        PrintUtil(activity).printDetailReportupdate(
                                                            batchList,
                                                            activity
                                                        ) { detailPrintStatus ->
                                                            if (detailPrintStatus) {
                                                                GlobalScope.launch(
                                                                    Dispatchers.IO
                                                                ) {
                                                                    val data =
                                                                        CreateSettlementPacket(
                                                                            ProcessingCode.SETTLEMENT.code,
                                                                            batchList
                                                                        ).createSettlementISOPacket()
                                                                    settlementByteArray =
                                                                        data.generateIsoByteRequest()
                                                                    try {
                                                                        (activity as MainActivity).settleBatch(
                                                                            settlementByteArray
                                                                        ) {
                                                                            if (!it)
                                                                                enableDisableSettlementButton(
                                                                                    true
                                                                                )
                                                                        }
                                                                    } catch (ex: Exception) {
                                                                        (activity as MainActivity).hideProgress()
                                                                        enableDisableSettlementButton(
                                                                            true
                                                                        )
                                                                        ex.printStackTrace()
                                                                    }
                                                                }
                                                            } else {
                                                                (activity as MainActivity).hideProgress()
                                                                GlobalScope.launch(Dispatchers.Main) {
                                                                    (activity as MainActivity).alertBoxWithAction(
                                                                        null,
                                                                        null,
                                                                        getString(R.string.printer_error),
                                                                        getString(R.string.please_check_printing_roll),
                                                                        true,
                                                                        getString(R.string.yes),
                                                                        {
                                                                            val data =
                                                                                CreateSettlementPacket(
                                                                                    ProcessingCode.SETTLEMENT.code,
                                                                                    batchList
                                                                                ).createSettlementISOPacket()
                                                                            settlementByteArray =
                                                                                data.generateIsoByteRequest()
                                                                            try {
                                                                                (activity as MainActivity).hideProgress()
                                                                                GlobalScope.launch(
                                                                                    Dispatchers.IO
                                                                                ) {
                                                                                    (activity as MainActivity).settleBatch(
                                                                                        settlementByteArray
                                                                                    ) {
                                                                                        if (!it)
                                                                                            enableDisableSettlementButton(
                                                                                                true
                                                                                            )
                                                                                    }
                                                                                }
                                                                            } catch (ex: Exception) {
                                                                                (activity as MainActivity).hideProgress()
                                                                                enableDisableSettlementButton(
                                                                                    true
                                                                                )
                                                                                ex.printStackTrace()
                                                                            }
                                                                        },
                                                                        {})
                                                                }
                                                            }
                                                        }

                                                    }

                                                }


                                            },
                                            { navigateToMain() })
                                        /* } else
                                             GlobalScope.launch(Dispatchers.Main) {
                                                 enableDisableSettlementButton(true)
                                                 VFService.showToast(getString(R.string.empty_batch_data))
                                             }*/
                                    } else {
                                        enableDisableSettlementButton(true)
                                        (activity as MainActivity).hideProgress()
                                        VFService.showToast(getString(R.string.printing_roll_error))
                                    }
                                }
                            } else {
                                enableDisableSettlementButton(isEnable = true)
                                GlobalScope.launch(Dispatchers.Main) {
                                    enableDisableSettlementButton(true)
                                    (activity as MainActivity).hideProgress()
                                    VFService.showToast(getString(R.string.offline_sale_upload_fails_please_try_again) + "\n" + responseValidationMsg)
                                    (activity as MainActivity).alertBoxWithAction(null,
                                        null,
                                        getString(R.string.offline_upload),
                                        getString(R.string.offline_sale_failed_to_upload),
                                        false,
                                        getString(R.string.positive_button_ok),
                                        {},
                                        {})
                                }
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            (activity as MainActivity).hideProgress()
                            VFService.showToast(msg)
                        }
                    }
                }
            }
        }
    }

    //region==============================method to enable/disable settlement Floating button:-
    private fun enableDisableSettlementButton(isEnable: Boolean) {
        settlement_batch_btn?.isEnabled = isEnable
    }
    //endregion

    //Method is to get Sale Batch Data for Void RecyclerView listing:-
    private fun getAndInflateSettlementData() {
        if (batchList.size == 0) {
            conditionBasedShowHideViews(false)
        } else {
            conditionBasedShowHideViews(true)
            settlement_rv?.apply {
                layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                adapter = settlementAdapter
                itemAnimator = DefaultItemAnimator()
            }
        }
    }

    //Below method is used to show/hide views on basis of recyclerview data list size:-
    private fun conditionBasedShowHideViews(showStatus: Boolean) {
        if (!showStatus) {
            lv_heading_view?.visibility = View.GONE
            header_cardview?.visibility = View.GONE
            settlement_rv?.visibility = View.GONE
            empty_view_placeholder?.visibility = View.VISIBLE
            emptyBatchTv?.visibility=View.VISIBLE
        } else {
            lv_heading_view?.visibility = View.VISIBLE
            header_cardview?.visibility = View.VISIBLE
            settlement_rv?.visibility = View.VISIBLE
            empty_view_placeholder?.visibility = View.GONE
            emptyBatchTv?.visibility = View.GONE
        }
    }

    //Below method to Navigate merchant to MainActivity:-
    private fun navigateToMain() {
        startActivity(Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}

internal class SettlementAdapter(private val list: List<BatchFileDataTable>) :
    RecyclerView.Adapter<SettlementAdapter.SettlementHolder>() {


    companion object {
        val TAG = SettlementAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SettlementHolder {
        val inflater = LayoutInflater.from(p0.context).inflate(R.layout.item_settlement, p0, false)
        return SettlementHolder(inflater)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(p0: SettlementHolder, p1: Int) {

        p0.invoiceText.text = invoiceWithPadding(list[p1].hostInvoice)
        var amount = "0"

        divideAmountBy100(amount.toDouble().toInt())
        if (list[p1].transactionType == TransactionType.TIP_SALE.type || list[p1].transactionType == TransactionType.SALE_WITH_CASH.type) {
            amount = "%.2f".format(list[p1].totalAmmount.toFloat() / 100)
        }else {
            amount = "%.2f".format(list[p1].transactionalAmmount.toFloat() / 100)
        }

        p0.baseAmountText.text = amount



        p0.transactionType.text = list[p1].getTransactionType()

        p0.transactionDateText.text = list[p1].transactionDate
    }


    class SettlementHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val invoiceText = view.findViewById<TextView>(R.id.tv_invoice_number)
        val baseAmountText = view.findViewById<TextView>(R.id.tv_base_amount)
        val transactionType = view.findViewById<TextView>(R.id.tv_transaction_type)
        val transactionDateText = view.findViewById<TextView>(R.id.tv_transaction_date)
    }
}
