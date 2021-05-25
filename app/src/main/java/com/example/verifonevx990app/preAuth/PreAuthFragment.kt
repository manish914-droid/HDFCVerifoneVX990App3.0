package com.example.verifonevx990app.preAuth

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
import com.example.verifonevx990app.main.IFragmentRequest
import com.example.verifonevx990app.realmtables.EDashboardItem


class PreAuthFragment : Fragment() {
    val preAuthOptionList by lazy {
        arguments?.getSerializable("preAuthOptionList") as ArrayList<EDashboardItem>
    }

    //creating our adapter
    val mAdapter by lazy {
        PreAuthOptionAdapter(preAuthOptionList) {
            onOptionClickListner(it)
        }
    }
    private var binding: FragmentPendingPreAuthBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingPreAuthBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.pre_auth)

        binding?.pendingPreAuthPrintBtn?.visibility = View.GONE


        binding?.pendingPreRv?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
        }
    }


    private fun onOptionClickListner(option: EDashboardItem) {
        (activity as IFragmentRequest).onDashBoardItemClick(option)

        /*when (option) {
            EDashboardItem.PREAUTH -> {

            }
            EDashboardItem.PREAUTH_COMPLETE -> {

            }
            EDashboardItem.VOID_PREAUTH -> {

            }
            EDashboardItem.PENDING_PREAUTH -> {

            }

            else -> {

            }
        }*/
    }

}


class PreAuthOptionAdapter(
    var optionList: ArrayList<EDashboardItem>,
    var cb: (EDashboardItem) -> Unit
) : RecyclerView.Adapter<PreAuthOptionAdapter.PreAuthOptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreAuthOptionViewHolder {
        return PreAuthOptionViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pre_auth_frag, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PreAuthOptionViewHolder, position: Int) {
        holder.preAuthOption.also {
            it.text = optionList[position].title
            it.setOnClickListener {
                cb(optionList[position])
            }
        }
    }

    override fun getItemCount(): Int = optionList.size

    class PreAuthOptionViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var preAuthOption = view.findViewById<NeumorphButton>(R.id.pre_auth_option)

    }
}


