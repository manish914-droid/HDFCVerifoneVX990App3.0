package com.example.verifonevx990app.emiCatalogue

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.EmiCompareFragmentBinding
import com.example.verifonevx990app.databinding.ItemEmiCompareViewBinding
import com.example.verifonevx990app.vxUtils.IDialog
import com.example.verifonevx990app.vxUtils.UiAction
import com.google.gson.Gson

class EMICompareFragment : Fragment() {

    private var iDialog: IDialog? = null
    private var dataList: MutableList<IssuerBankModal> = mutableListOf()
    private var binding: EmiCompareFragmentBinding? = null
    private val compareActionName by lazy { arguments?.getString("compareActionName") ?: "" }
    private val emiCompareAdapter by lazy {
        EMICompareAdapter(
            compareActionName,
            dataList,
            ::onItemDeleteClick
        )
    }
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private val selectedTenure by lazy { arguments?.getString("selectedTenure") ?: "" }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = EmiCompareFragmentBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataList =
            arguments?.getParcelableArrayList<IssuerBankModal>("dataModal") as MutableList<IssuerBankModal>
        Log.d("Data:- ", Gson().toJson(dataList))

        if (action == UiAction.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
        }


        //region=============Show/Hide Compare By Tenure and Compare By Bank Buttons:-
        when (compareActionName) {
            CompareActionType.COMPARE_BY_BANK.compareType -> {
                binding?.compareByBankCV?.visibility = View.VISIBLE
                binding?.compareByTenureCV?.visibility = View.GONE
                binding?.issuerBankIcon?.visibility = View.VISIBLE
                binding?.tenureText?.visibility = View.GONE

                if (dataList.size == 1) {
                    binding?.issuerBankIcon?.setImageResource(dataList[0].bankLogo)
                }
            }

            CompareActionType.COMPARE_BY_TENURE.compareType -> {
                binding?.compareByBankCV?.visibility = View.GONE
                binding?.compareByTenureCV?.visibility = View.VISIBLE
                binding?.issuerBankIcon?.visibility = View.GONE
                binding?.tenureText?.visibility = View.VISIBLE

                if (!TextUtils.isEmpty(selectedTenure)) {
                    binding?.tenureText?.text = selectedTenure
                }
            }
        }
        //endregion

        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        //region============Setting Up RecyclerView:-
        binding?.compareRV?.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            if (dataList.size > 1) {
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.HORIZONTAL
                    )
                )
            }
            adapter = emiCompareAdapter
        }
        //endregion
    }

    //region=================Method To Delete RecyclerView Cell on click of Delete Icon:-
    private fun onItemDeleteClick(position: Int) {
        if (position > -1) {
            Log.d("PositionClicked:- ", position.toString())
        }
    }
    //endregion

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

//region===============Below adapter is used to show the All Issuer Bank lists available:-
class EMICompareAdapter(
    var compareActionName: String,
    var dataList: MutableList<IssuerBankModal>,
    var cb: (Int) -> Unit
) :
    RecyclerView.Adapter<EMICompareAdapter.EMICompareViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EMICompareViewHolder {
        val itemBinding =
            ItemEmiCompareViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EMICompareViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: EMICompareViewHolder, position: Int) {
        val modal = dataList[position]
        when (compareActionName) {
            CompareActionType.COMPARE_BY_BANK.compareType -> {
                holder.viewBinding.topHeaderBT.text = modal.issuerBankTenure
            }
            CompareActionType.COMPARE_BY_TENURE.compareType -> {
                holder.viewBinding.topHeaderBT.text = modal.issuerBankName
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    inner class EMICompareViewHolder(var viewBinding: ItemEmiCompareViewBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.issuerDeleteIV.setOnClickListener { cb(absoluteAdapterPosition) }
        }
    }
}
//endregion