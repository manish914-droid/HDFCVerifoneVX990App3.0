package com.example.verifonevx990app.merchantPromo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.ActivitySettlementViewBinding
import com.example.verifonevx990app.databinding.ItemMerchantPromoBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PromoListFragment : Fragment() {

    private var binding: ActivitySettlementViewBinding? = null
    private var promoList = arrayListOf<PromoData>()
    private var mobileNo: String? = null
    private var age: String? = null
    private var slectedIndex = -1
    private var selectedPromoId = "0"
    private var gender: String? = null
    private var promoType = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            promoList = it?.getSerializable("promoList") as ArrayList<PromoData>
            mobileNo = it.getString("mobNo") ?: "NoMobno"
            mobileNo = if (mobileNo == "") "30" else mobileNo
            age = it.getString("age") ?: "30"
            age = if (age == "") "30" else age
            gender = it.getString("gender") ?: "M"
            promoType = it.getInt("promoType")

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = ActivitySettlementViewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        binding?.subHeaderView?.subHeaderText?.text = "Select Promo"
        binding?.lvHeadingView?.visibility = View.GONE
        binding?.settlementBatchBtn?.text = "SEND"
        binding?.settlementBatchBtn?.icon = (resources.getDrawable(R.drawable.ic_brand_emi))
        binding?.settlementBatchBtn?.setOnClickListener {

            if (slectedIndex < 0) {
                VFService.showToast("SELECT A PROMO")

            } else {
                VFService.showToast("PERFECT....@")
                selectedPromoId = promoList[slectedIndex].promoID
                gotoHost()


            }

        }
        binding?.settlementRv?.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            adapter = PromoRecyclerView(promoList)
        }


    }

    private fun gotoHost() {
        (activity as BaseActivity).showProgress()
        GlobalScope.launch(Dispatchers.IO) {
            val tpt = TerminalParameterTable.selectFromSchemeTable()
            if (tpt != null) {
                //210219121526|8287305603|34|M|2144
                val field57Data = "${tpt.promoVersionNo}|${
                    mobileNo
                }|${age}|${gender}|${selectedPromoId}"
                getPromotionData(field57Data, ProcessingCode.SEND_PROMO.code, tpt)
                { isSuccess, responseMsg, responsef57, fullResponse ->
                    (activity as BaseActivity).hideProgress()
                    if (isSuccess) {
                        val responseIsoData: IsoDataReader =
                            readIso(fullResponse, false)
                        val successResponseCode =
                            responseIsoData.isoMap[39]?.parseRaw2String()
                                .toString()
                        when (successResponseCode) {
                            "00" -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    Log.e(
                                        "SEND PROMO",
                                        "CUSTOMER SEND PROMOTION, SUCCESSFULLY"
                                    )
                                    (activity as BaseActivity).alertBoxWithAction(
                                        null,
                                        null,
                                        getString(R.string.success_message),
                                        responseMsg,
                                        false,
                                        getString(R.string.positive_button_ok),
                                        {
                                            parentFragmentManager.popBackStack()
                                        },
                                        {
                                            // for no button
                                        })
                                }
                            }

                            "06" -> {
                                if (promoType == 2) {
                                    (activity as MainActivity).transactFragment(
                                        InitiatePromoFragment().apply {
                                            arguments = Bundle().apply {
                                                putString(
                                                    MainActivity.INPUT_SUB_HEADING,
                                                    context?.getString(R.string.add_customer)
                                                )
                                                putString(
                                                    MainActivity.INPUT_SUB_HEADING,
                                                    context?.getString(R.string.send_promo)
                                                )
                                                putString("mobNo", mobileNo)
                                                putString("promoId", selectedPromoId)
                                                putInt("promoType", 3)
                                                putBoolean("addAndSend", true)
                                            }
                                        },
                                        isBackStackAdded = true
                                    )
                                }
                            }

                            else -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    Log.e(
                                        "SEND PROMO",
                                        "ERROR  Else part Send promo"
                                    )
                                    (activity as BaseActivity).alertBoxWithAction(
                                        null,
                                        null,
                                        getString(R.string.fail),
                                        responseMsg,
                                        false,
                                        getString(R.string.positive_button_ok),
                                        {
                                            parentFragmentManager.popBackStackImmediate()
                                            //    getPromFromServerAndShow()
                                        },
                                        {
                                            // for no button
                                            // parentFragmentManager.popBackStackImmediate()
                                        })
                                }
                            }
                        }
                    } else {

                        VFService.showToast(responseMsg)
                    }
                }
            }
        }

    }

    inner class PromoRecyclerView(
        var promoList: ArrayList<PromoData>
    ) :
        RecyclerView.Adapter<PromoRecyclerView.PromoRecyclerViewHolder>() {


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PromoRecyclerViewHolder {
            return PromoRecyclerViewHolder(
                ItemMerchantPromoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: PromoRecyclerViewHolder, position: Int) {
            holder.view.promoName.text = promoList[position].promoName
            holder.view.parentLL.setOnClickListener {
                slectedIndex = position
                notifyDataSetChanged()
            }
            if (slectedIndex == position) {
                holder.view.schemeCheckIv.visibility = View.VISIBLE
            } else {
                holder.view.schemeCheckIv.visibility = View.GONE
            }


        }


        override fun getItemCount(): Int = promoList.size

        inner class PromoRecyclerViewHolder(val view: ItemMerchantPromoBinding) :
            RecyclerView.ViewHolder(view.root)
    }

}

