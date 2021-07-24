package com.example.verifonevx990app.init


import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentInitBinding
import com.example.verifonevx990app.main.IFragmentRequest
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.vxUtils.*
import com.example.verifonevx990app.vxUtils.ROCProviderV2.refreshToolbarLogos

class InitFragment : Fragment() {

    private var iFragmentRequest: IFragmentRequest? = null
    private var iDialog: IDialog? = null
    private var binding: FragmentInitBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInitBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
      //  val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.ma_bnv)
        AppPreference.saveBoolean(
            PrefConstant.INIT_AFTER_SETTLE_BATCH_SUCCESS.keyName.toString(),
            true
        )
        binding?.ifProceedBtn?.isEnabled = false
        binding?.ifProceedBtn?.isClickable = false
        binding?.ifEt?.addTextChangedListener(textWatcher)
        /* if (bottomNavigationView is BottomNavigationView) {
             bottomNavigationView.menu.findItem(R.id.home)?.isChecked = true
         }*/
        activity?.let { refreshToolbarLogos(it) }
        //  (iDialog as MainActivity).findViewById<BottomNavigationView>(R.id.ma_bnv).visibility = View.GONE
        binding?.ifEt?.transformationMethod = null

        //Below Code write App Revision ID to file when first time Init Screen opens
        //in App after that this file will override after settlement:-
        context?.let { writeAppRevisionIDInFile(it) }

        binding?.ifEt?.addTextChangedListener(OnTextChange {
            binding?.ifProceedBtn?.isEnabled = it.length == 8

             if(binding?.ifProceedBtn?.isEnabled == true)
                 binding?.ifProceedBtn?.getContext()?.getResources()?.let { it1 ->
                     binding?.ifProceedBtn?.setBackgroundColor(
                         it1.getColor(R.color.hdfc_red_color))
                 };
            //actionDone( view.if_et)
        })

        binding?.ifProceedBtn?.setOnClickListener {
            actionDone(view)
        }

        binding?.ifEt?.setOnEditorActionListener(getEditorActionListener(::actionDone))

        context?.getString(R.string.init)?.let { VxEvent.ChangeTitle(it) }?.let {
            iDialog?.onEvents(
                it
            )
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            if (start < 8) {

                binding?.ifProceedBtn?.getContext()?.getResources()?.let { it1 ->
                    binding?.ifProceedBtn?.setBackgroundColor(
                        it1.getColor(R.color.colorGrey))
                };
            }
            else if(start>=8){
                binding?.ifProceedBtn?.getContext()?.getResources()?.let { it1 ->
                    binding?.ifProceedBtn?.setBackgroundColor(
                        it1.getColor(R.color.hdfc_red_color))
                };
            }
        }
    }

    private fun actionDone(view: View) {
        if (binding?.ifEt?.text.toString().length == 8) {
            val tid = binding?.ifEt?.text.toString()
            iFragmentRequest?.onFragmentRequest(
                UiAction.INIT_WITH_KEY_EXCHANGE,
                tid
            )
        } else
            iDialog?.showToast("Invalid TID")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFragmentRequest = context
        }
        if (context is IDialog) {
            iDialog = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        iFragmentRequest = null
        iDialog = null
    }

}

fun getEditorActionListener(callback: (v: TextView) -> Unit): TextView.OnEditorActionListener {
    val oal = TextView.OnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback(v)
            true
        } else if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (event != null) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    callback(v)
                    true
                } else false
            } else false
        } else false
    }
    return oal
}
