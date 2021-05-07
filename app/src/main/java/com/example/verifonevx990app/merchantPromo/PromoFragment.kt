package com.example.verifonevx990app.merchantPromo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.CrossSellViewBinding
import com.example.verifonevx990app.main.MainActivity

class PromoFragment : Fragment(R.layout.cross_sell_view) {
    // Here using the same view as of Cross-sell because of similar UI
    private var promoViewBinding: CrossSellViewBinding? = null
    private var redeemPromoBtn: Button? = null
    private var sendPromoBtn: Button? = null
    private var addPromoBtn: Button? = null
    private var flexiPaybtn: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        promoViewBinding = CrossSellViewBinding.inflate(inflater, container, false)
        return promoViewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        redeemPromoBtn = promoViewBinding?.hdfcCreditCard
        sendPromoBtn = promoViewBinding?.instaLoan
        addPromoBtn = promoViewBinding?.jumboLoan
        flexiPaybtn = promoViewBinding?.flexipay
        promoViewBinding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        promoViewBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.bonus_promo)
        promoViewBinding?.reports?.visibility = View.GONE
        flexiPaybtn?.visibility = View.GONE

        redeemPromoBtn?.apply {
            visibility = View.VISIBLE
            text = context.getString(R.string.redeem_promo)
            setOnClickListener {
                (activity as MainActivity).transactFragment(InitiatePromoFragment().apply {
                    arguments = Bundle().apply {
                        putString(MainActivity.INPUT_SUB_HEADING, text.toString())
                        putInt("promoType", 1)
                    }
                }, isBackStackAdded = true)

            }
        }

        sendPromoBtn?.apply {
            visibility = View.VISIBLE
            text = context.getString(R.string.send_promo)
            setOnClickListener {
                (activity as MainActivity).transactFragment(InitiatePromoFragment().apply {
                    arguments = Bundle().apply {
                        putString(MainActivity.INPUT_SUB_HEADING, text.toString())
                        putInt("promoType", 2)

                    }
                }, isBackStackAdded = true)
            }

        }

        addPromoBtn?.apply {
            visibility = View.VISIBLE
            text = context.getString(R.string.add_customer)
            setOnClickListener {
                (activity as MainActivity).transactFragment(InitiatePromoFragment().apply {
                    arguments = Bundle().apply {
                        putString(MainActivity.INPUT_SUB_HEADING, text.toString())
                        putInt("promoType", 3)

                    }
                }, isBackStackAdded = true)
            }
        }
    }

}