package com.example.verifonevx990app.digiPOS.pendingTxn

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentPendingTxnListBinding
import com.example.verifonevx990app.databinding.ItemPendingTxnBinding
import com.example.verifonevx990app.digiPOS.*
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.DigiPosDataTable
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList


class PendingTxnListFragment : Fragment() {
    var binding:FragmentPendingTxnListBinding?=null

    private var digiPosData = arrayListOf<DigiPosDataTable>()
    private var pendingTxnAdapter :PendingTxnRecyclerView?=null

    override fun onResume() {
        super.onResume()
        Log.e("FRAG","ON RESUME CALLED")

    }
    //region==================OnItemClickCB:-
    private fun onItemClickCB(position: Int, clickItem: String) {

        when (clickItem) {
            GET_TXN_STATUS -> {
                (activity as BaseActivity).showProgress()
                    lifecycleScope.launch(Dispatchers.IO) {
                        val req57 =
                            "${EnumDigiPosProcess.GET_STATUS.code}^${digiPosData[position].partnerTxnId}^^"
                        Log.d("Field57:- ", req57)
                        getDigiPosStatus(
                            req57,
                            EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                            false
                        ) { isSuccess, responseMsg, responsef57, fullResponse ->
                            try {
                                (activity as BaseActivity).hideProgress()
                                if (isSuccess) {
                                    val statusRespDataList =
                                        responsef57.split("^")
                                    val tabledata =
                                        DigiPosDataTable()
                                    tabledata.requestType =
                                        statusRespDataList[0].toInt()
                                    //  tabledata.partnerTxnId = statusRespDataList[1]
                                    tabledata.status =
                                        statusRespDataList[1]
                                    tabledata.statusMsg =
                                        statusRespDataList[2]
                                    tabledata.statusCode =
                                        statusRespDataList[3]
                                    tabledata.mTxnId =
                                        statusRespDataList[4]
                                    tabledata.partnerTxnId =
                                        statusRespDataList[6]
                                    tabledata.transactionTimeStamp = statusRespDataList[7]
                                    tabledata.displayFormatedDate=
                                        getDateInDisplayFormatDigipos(statusRespDataList[7])
                                    val dateTime =
                                        statusRespDataList[7].split(
                                            " "
                                        )
                                    tabledata.txnDate = dateTime[0]
                                    tabledata.txnTime = dateTime[1]
                                    tabledata.amount =
                                        statusRespDataList[8]
                                    tabledata.paymentMode =
                                        statusRespDataList[9]
                                    tabledata.customerMobileNumber =
                                        statusRespDataList[10]
                                    tabledata.description =
                                        statusRespDataList[11]
                                    tabledata.pgwTxnId =
                                        statusRespDataList[12]


                                    when (statusRespDataList[5]) {
                                        EDigiPosPaymentStatus.Pending.desciption -> {
                                            tabledata.txnStatus =statusRespDataList[5]

                                        }
                                        EDigiPosPaymentStatus.Failed.desciption -> {
                                            tabledata.txnStatus =
                                                statusRespDataList[5]
                                        }
                                        EDigiPosPaymentStatus.Approved.desciption -> {
                                            tabledata.txnStatus = statusRespDataList[5]
                                            DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                                            val dp = DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Pending.desciption)
                                            val dpObj = Gson().toJson(dp)
                                            logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                            Log.e("F56->>", responsef57)
                                            runBlocking(Dispatchers.Main) {
                                                pendingTxnAdapter?.refreshAdapterList(dp as ArrayList<DigiPosDataTable>)
                                                (activity as MainActivity).hideProgress()
                                             //   binding?.recyclerView?.smoothScrollToPosition(0)
                                            }
                                        }
                                    }
                                }
                                else {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        (activity as BaseActivity).alertBoxWithAction(null,
                                            null,
                                            getString(R.string.transaction_failed_msg),
                                            responseMsg,
                                            false,
                                            getString(R.string.positive_button_ok),
                                            { alertPositiveCallback ->
                                                if (alertPositiveCallback) {
                                                   /* DigiPosDataTable.deletRecord(
                                                        field57.split("^").last())*/
                                                    parentFragmentManager.popBackStack()
                                                }
                                            },
                                            {})
                                    }
                                }

                            } catch (ex: java.lang.Exception) {
                                ex.printStackTrace()
                                logger(LOG_TAG.DIGIPOS.tag, "Somethig wrong... in response data field 57")
                            }
                        }
                    }

            }
            SHOW_TXN_DETAIL_PAGE -> {
                (activity as MainActivity).transactFragment(PendingDetailFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("data", digiPosData[position])
                        // putString(INPUT_SUB_HEADING, "")
                    }
                })

            }
        }


    }
//endregion


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentPendingTxnListBinding.inflate(inflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        digiPosData=DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Pending.desciption) as ArrayList<DigiPosDataTable>

        pendingTxnAdapter=    PendingTxnRecyclerView(
            digiPosData,
            ::onItemClickCB
        )
        binding?.recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            adapter = pendingTxnAdapter
        }


    }
}

class PendingTxnRecyclerView(val digiData:ArrayList<DigiPosDataTable>,  val onCategoryItemClick: (Int, String) -> Unit) : RecyclerView.Adapter<PendingTxnRecyclerView.PendingTxnViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingTxnViewHolder {
        val binding: ItemPendingTxnBinding = ItemPendingTxnBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PendingTxnViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingTxnViewHolder, position: Int) {
        holder.binding.smsPay.text = digiData[position].paymentMode
        val amountData = "\u20B9 ${digiData[position].amount}"
        holder.binding.smsPayTransactionAmount.text = amountData
        holder.binding.dateTV.text = digiData[position].displayFormatedDate
        holder.binding.mobileNumberTV.text = digiData[position].customerMobileNumber

        when {
            digiData[position].txnStatus.toLowerCase(Locale.ROOT).equals("success", true) -> {
                holder.binding.transactionIV.setImageResource(R.drawable.circle_with_tick_mark_green)
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

    override fun getItemCount(): Int =digiData.size

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: ArrayList<DigiPosDataTable>) {
        this.digiData.clear()
        this.digiData.addAll(refreshList)
       notifyDataSetChanged()
    }
    //endregion


  inner  class PendingTxnViewHolder(val binding: ItemPendingTxnBinding):RecyclerView.ViewHolder(binding.root){
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