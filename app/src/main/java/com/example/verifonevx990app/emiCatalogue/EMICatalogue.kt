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
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SubHeaderTitle
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.IDialog

class EMICatalogue : Fragment() {
    private var iDialog: IDialog? = null
    private var binding: FragmentEmiCatalogueBinding? = null
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var tptData: TerminalParameterTable? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
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

        //region================Brand and Bank EMI Catalogue Button Hide/Show Conditions:-
        /* if(tptData?.reservedValues?.substring(9, 10) == "1" && tptData?.reservedValues?.substring(5, 6) == "1"){
             binding?.buttonBrandEmi?.visibility = View.VISIBLE
             binding?.buttonBankEmi?.visibility = View.VISIBLE
         }else if(tptData?.reservedValues?.substring(9, 10) == "1" && tptData?.reservedValues?.substring(5, 6) == "0"){
             binding?.buttonBrandEmi?.visibility = View.VISIBLE
             binding?.buttonBankEmi?.visibility = View.GONE
         }else if(tptData?.reservedValues?.substring(9, 10) == "0" && tptData?.reservedValues?.substring(5, 6) == "1"){
             binding?.buttonBrandEmi?.visibility = View.GONE
             binding?.buttonBankEmi?.visibility = View.VISIBLE
         }*/
        //endregion

        //Navigate BrandEMI Page by onClick event of BrandEMI Button:-

        binding?.buttonBrandEmi?.setOnClickListener {
            (activity as MainActivity).transactFragment(BrandEMIMasterCategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", action)
                    putString(
                        MainActivity.INPUT_SUB_HEADING,
                        SubHeaderTitle.Brand_EMI_Master_Category.title
                    )
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