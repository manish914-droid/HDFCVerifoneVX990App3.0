package com.example.verifonevx990app.bankemi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customneumorphic.NeumorphButton
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentPendingPreAuthBinding
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SubHeaderTitle
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.transactions.NewInputAmountFragment


class TestEmiOptionFragment : Fragment() {

    val testEmiOptionList by lazy {

        val option = arrayListOf<EmiTestOptionModel>()
        option.add(EmiTestOptionModel("1", "Base TID"))
        option.add(EmiTestOptionModel("2", "OFFUS TID"))
        option.add(EmiTestOptionModel("3", "3 M TID"))
        option.add(EmiTestOptionModel("6", "6 M TID"))
        option.add(EmiTestOptionModel("9", "9 M TID"))
        option.add(EmiTestOptionModel("12", "12 M TID"))
        option
    }

    private val transactionType by lazy { arguments?.getSerializable("type") as EDashboardItem }

    //creating our adapter
    private val mAdapter by lazy {
        TestEmiOptionAdapter(testEmiOptionList) {
            onOptionClickListner(it)
        }
    }
    private var binding: FragmentPendingPreAuthBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingPreAuthBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.test_emi)
        binding?.subHeaderView?.headerImage?.visibility = View.VISIBLE
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)
        binding?.pendingPreAuthPrintBtn?.visibility = View.GONE


        binding?.pendingPreRv?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
        }
    }


    private fun onOptionClickListner(testEmiOption: EmiTestOptionModel) {
        (activity as MainActivity).inflateInputFragment(
            NewInputAmountFragment(),
            SubHeaderTitle.TEST_EMI.title,
            EDashboardItem.TEST_EMI, testEmiOption.id
        )
    }
}


class TestEmiOptionAdapter(
    var optionList: ArrayList<EmiTestOptionModel>,
    var cb: (EmiTestOptionModel) -> Unit
) : RecyclerView.Adapter<TestEmiOptionAdapter.TestEmiOptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestEmiOptionViewHolder {
        return TestEmiOptionViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pre_auth_frag, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TestEmiOptionViewHolder, position: Int) {
        holder.preAuthOption.also {
            it.text = optionList[position].name

            it.setOnClickListener {
                cb(optionList[position])
            }
        }
    }

    override fun getItemCount(): Int = optionList.size

    class TestEmiOptionViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var preAuthOption = view.findViewById<NeumorphButton>(R.id.pre_auth_option)

    }
}

class EmiTestOptionModel(var id: String, var name: String)