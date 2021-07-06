package com.example.verifonevx990app.digiPOS

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.verifonevx990app.databinding.FragmentDigiPosMenuBinding
import com.example.verifonevx990app.digiPOS.BitmapUtils.convertBitmapToByteArray
import com.example.verifonevx990app.digiPOS.pendingTxn.PendingTxnFragment
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.BaseActivity
import com.example.verifonevx990app.vxUtils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class DigiPosMenuFragment : Fragment() {

    val tpt by lazy {
        TerminalParameterTable.selectFromSchemeTable()
    }
    private var binding: FragmentDigiPosMenuBinding? = null
    private lateinit var transactionType: EDashboardItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDigiPosMenuBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.headerImage?.visibility = View.VISIBLE
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)
        binding?.subHeaderView?.subHeaderText?.text = transactionType.title

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        if (tpt?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            binding?.upiBtn?.visibility = View.VISIBLE
        }
        if (tpt?.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            binding?.staticQrBtn?.visibility = View.VISIBLE
            binding?.dynamicQrBtn?.visibility = View.VISIBLE
        }
        if (tpt?.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            binding?.smsPayBtn?.visibility = View.VISIBLE
        }

        binding?.smsPayBtn?.visibility = View.VISIBLE
        setClickListener()
    }

    private fun setClickListener() {
        //UPI CLICK
        binding?.upiBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.UPI)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })


        }
        //sms click
        binding?.smsPayBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.SMS_PAY)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }


// pending transaction
        binding?.pendingTxnBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(PendingTxnFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.PENDING_TXN)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }

        //region===========txn List Click:-
        binding?.txnListBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(DigiPosTxnListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.TXN_LIST)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion

        //region==========Static_QR  Click:-
        binding?.staticQrBtn?.setOnClickListener {
            var imgbm: Bitmap? = null
            runBlocking(Dispatchers.IO) {
                imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                if(imgbm!=null) {
                    val bmBytes = convertBitmapToByteArray(imgbm)
                    logger("StaticQr", "Already parsed Bitmap", "e")
                    (activity as MainActivity).transactFragment(QrScanFragment().apply {
                        arguments = Bundle().apply {
                            putByteArray("QrByteArray", bmBytes)
                            putSerializable("type",transactionType)
                            putSerializable("type", EDashboardItem.STATIC_QR)
                           // putParcelable("tabledata",tabledata)
                        }
                    })
                }
                else {
                  getStaticQrFromServerAndSaveToFile(activity as BaseActivity) {
                      if (it) {
                          logger("StaticQr", "Get Static Qr from server and  saves to file success ", "e")
                          lifecycleScope.launch(Dispatchers.IO) {
                              imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                          if (imgbm != null) {
                              val bmBytes = convertBitmapToByteArray(imgbm)
                              logger("StaticQr", "Already parsed Bitmap", "e")
                              (activity as MainActivity).transactFragment(QrScanFragment().apply {
                                  arguments = Bundle().apply {
                                      putByteArray("QrByteArray", bmBytes)
                                      putSerializable("type", transactionType)
                                      putSerializable("type", EDashboardItem.STATIC_QR)
                                      // putParcelable("tabledata",tabledata)
                                  }
                              })
                          }
                      }

                  }else{
                          logger("StaticQr", "Get Static Qr from server and  file not successfully saved", "e")

                      }

                  }
                }
            }



        }
        //endregion

        //region==========Dynamic QR  Click:-
        binding?.dynamicQrBtn?.setOnClickListener {
            (activity as MainActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BHARAT_QR)
                    // putString(INPUT_SUB_HEADING, "")
                }
            })

        }
        //endregion

    }

}