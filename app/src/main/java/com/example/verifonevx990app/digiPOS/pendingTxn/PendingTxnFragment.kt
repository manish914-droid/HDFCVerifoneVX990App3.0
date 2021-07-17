package com.example.verifonevx990app.digiPOS.pendingTxn
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentPendingTxnBinding
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.hideSoftKeyboard


class PendingTxnFragment : Fragment(), IPendingTxnListner {
    private var binding: FragmentPendingTxnBinding? = null
    private lateinit var transactionType: EDashboardItem
    private var mContainerId: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingTxnBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionType = arguments?.getSerializable("type") as EDashboardItem
        binding?.subHeaderView?.headerImage?.visibility = View.VISIBLE
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)
        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        mContainerId = binding?.pendingTxnContainer

        val fr: Fragment = PendingTxnListFragment()
        replaceFragment(fr)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.pendingTxnBtnn?.setOnClickListener {
            //--region --- Managing view appearance on click
            binding?.pendingTxnBtnn?.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#A9A9A9")))
            binding?.pendingTxnBtnn?.setStrokeWidth(2f)
            binding?.searchBtnn?.setStrokeColor(null)
            binding?.searchBtnn?.setStrokeWidth(0f)
// endregion
            val fr1: Fragment = PendingTxnListFragment()
            replaceFragment(fr1)
        }
        binding?.searchBtnn?.setOnClickListener {
            //--region --- Managing view appearance on click
            binding?.searchBtnn?.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#A9A9A9")))
            binding?.searchBtnn?.setStrokeWidth(2f)
            binding?.pendingTxnBtnn?.setStrokeColor(null)
            binding?.pendingTxnBtnn?.setStrokeWidth(0f)
            // endregion
            val fr2: Fragment = SearchTxnFragment()
            replaceFragment(fr2)
        }
    }

    override fun getTxnStatus() {
        VFService.showToast("***Getting TXN Plz Wait***")
    }

    private fun replaceFragment(fragment: Fragment?) {
        val fragmentManager: FragmentManager = childFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        if (fragment != null) {
           fragmentTransaction .setCustomAnimations(R.anim.anim_enter, R.anim.anim_exit, R.anim.anim_enter, R.anim.anim_exit)
            fragmentTransaction.replace(R.id.pending_txn_container, fragment, fragment.toString())
        }
        //  fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.commit()
    }
    override fun onDetach() {
        super.onDetach()
        hideSoftKeyboard(requireActivity())
    }
}


interface IPendingTxnListner {
    fun getTxnStatus()
}