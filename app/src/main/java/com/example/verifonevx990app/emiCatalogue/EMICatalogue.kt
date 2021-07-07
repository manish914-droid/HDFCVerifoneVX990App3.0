package com.example.verifonevx990app.emiCatalogue

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.brandemi.BrandEMIMasterCategoryFragment
import com.example.verifonevx990app.databinding.FragmentEmiCatalogueBinding
import com.example.verifonevx990app.main.IFragmentRequest
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.transactions.NewInputAmountFragment
import com.example.verifonevx990app.vxUtils.IDialog

class EMICatalogue : Fragment() {
    private var iDialog: IDialog? = null
    private var binding: FragmentEmiCatalogueBinding? = null
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var tptData: TerminalParameterTable? = null
    private var iFrReq: IFragmentRequest? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) iFrReq = context
        if (context is IDialog) iDialog = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEmiCatalogueBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.emi_catalogue)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        Log.d("EMI Catalogue Action:- ", (action as EDashboardItem).toString())
        tptData = TerminalParameterTable.selectFromSchemeTable()
//11111011000000000000
        //region================Brand and Bank EMI Catalogue Button Hide/Show Conditions:-
        /* if (tptData?.reservedValues?.substring(9, 10) == "1" && tptData?.reservedValues?.substring(
                 5,
                 6
             ) == "1"
         ) {
             binding?.brandEmiCv?.visibility = View.VISIBLE
             binding?.bankEmiCv?.visibility = View.VISIBLE
         } else if (tptData?.reservedValues?.substring(
                 9,
                 10
             ) == "1" && tptData?.reservedValues?.substring(5, 6) == "0"
         ) {
             binding?.brandEmiCv?.visibility = View.VISIBLE
             binding?.bankEmiCv?.visibility = View.GONE
         } else if (tptData?.reservedValues?.substring(
                 9,
                 10
             ) == "0" && tptData?.reservedValues?.substring(5, 6) == "1"
         ) {
             binding?.brandEmiCv?.visibility = View.GONE
             binding?.bankEmiCv?.visibility = View.VISIBLE
         }*/
        tptData?.let {
            enabledEmiOptions(it) { isBankEmiOn, isBrandEmiOn ->
                if (isBankEmiOn) {
                    binding?.bankEmiCv?.visibility = View.VISIBLE
                }
                if (isBrandEmiOn) {
                    binding?.brandEmiCv?.visibility = View.VISIBLE
                }

            }
        }


        //endregion

        //region================Navigate to NewInputAmount Fragment on Click Event of BankEMI Button:-
        binding?.buttonBankEmi?.setOnClickListener {
            (activity as MainActivity).transactFragment(NewInputAmountFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BANK_EMI_CATALOGUE)
                    putString(MainActivity.INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion

        //region================Navigate BrandEMI Page by onClick event of BrandEMI Button:-
        binding?.buttonBrandEmi?.setOnClickListener {
            (activity as MainActivity).transactFragment(BrandEMIMasterCategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BRAND_EMI_CATALOGUE)
                    putString(MainActivity.INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}

fun enabledEmiOptions(tpt: TerminalParameterTable, cb: (Boolean, Boolean) -> Unit) {
    var brandEmiOn = false
    var bankEmiOn = false

    when (tpt.reservedValues[6]) {
        '1' -> {
            // bank emi on
            bankEmiOn = true
        }
    }
    when (tpt.reservedValues[10]) {
        '1' -> {
            // brand emi on
            brandEmiOn = true
        }


    }
    cb(bankEmiOn, brandEmiOn)

}