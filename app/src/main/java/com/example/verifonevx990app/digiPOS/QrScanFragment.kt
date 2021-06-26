package com.example.verifonevx990app.digiPOS

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentQrScanBinding
import com.example.verifonevx990app.digiPOS.BitmapUtils.convertCompressedByteArrayToBitmap
import com.example.verifonevx990app.realmtables.DigiPosDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.utils.printerUtils.EPrintCopyType
import com.example.verifonevx990app.utils.printerUtils.PrintUtil
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class QrScanFragment : Fragment() {
    private lateinit var transactionType: EDashboardItem
    private lateinit var QrBytes: ByteArray
    private lateinit var digiPosTabledata: DigiPosDataTable

    var binding: FragmentQrScanBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentQrScanBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionType = arguments?.getSerializable("type") as EDashboardItem
        digiPosTabledata = arguments?.getParcelable("tabledata") ?: DigiPosDataTable()
        //getting byte[] from argument
        QrBytes = arguments?.getByteArray("QrByteArray") as ByteArray
        //convert byte[] to bitmap
        val qrCodeBitmap = convertCompressedByteArrayToBitmap(QrBytes)
        //showing bitmap on imageView
        binding?.qrImg?.setImageBitmap(qrCodeBitmap)

        binding?.subHeaderView?.headerImage?.visibility = View.VISIBLE
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)
        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

        }

        val paymsg = when (transactionType) {
            EDashboardItem.DYNAMIC_QR -> {

                getString(R.string.scan_qr_code_to_pay_n_nwould_you_like_to_check_payment_status_now)
            }
            else -> {
                binding?.noBtn?.visibility = View.GONE
                binding?.yesBtn?.text = getString(R.string.key_ok)
                getString(R.string.scan_qr_pay)

            }
        }
        binding?.payMsg?.text = paymsg

        binding?.yesBtn?.setOnClickListener {

            when (transactionType) {
                EDashboardItem.DYNAMIC_QR -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            (activity as BaseActivity).showProgress()
                        }
                        val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + digiPosTabledata.partnerTxnId + "^"

                        getDigiPosStatus(
                            req57,
                            EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                            false
                        ) { isSuccess, responseMsg, responsef57, fullResponse ->
                            try {
                                (activity as BaseActivity).hideProgress()
                                if (isSuccess) {
                                    val statusRespDataList =
                                        responsef57.split("^")
                                    val tabledata =
                                        DigiPosDataTable()
                                    tabledata.requestType =
                                        statusRespDataList[0].toInt()
                                    //  tabledata.partnerTxnId = statusRespDataList[1]
                                    tabledata.status =
                                        statusRespDataList[1]
                                    tabledata.statusMsg =
                                        statusRespDataList[2]
                                    tabledata.statusCode =
                                        statusRespDataList[3]
                                    tabledata.mTxnId =
                                        statusRespDataList[4]
                                    tabledata.partnerTxnId =
                                        statusRespDataList[6]
                                    tabledata.transactionTimeStamp =
                                        statusRespDataList[7]
                                    tabledata.displayFormatedDate =
                                        getDateInDisplayFormatDigipos(
                                            statusRespDataList[7]
                                        )
                                    val dateTime =
                                        statusRespDataList[7].split(
                                            " "
                                        )
                                    tabledata.txnDate = dateTime[0]
                                    tabledata.txnTime = dateTime[1]
                                    tabledata.amount =
                                        statusRespDataList[8]
                                    tabledata.paymentMode =
                                        statusRespDataList[9]
                                    tabledata.customerMobileNumber =
                                        statusRespDataList[10]
                                    tabledata.description =
                                        statusRespDataList[11]
                                    tabledata.pgwTxnId =
                                        statusRespDataList[12]


                                    when (statusRespDataList[5]) {
                                        EDigiPosPaymentStatus.Pending.desciption -> {
                                            tabledata.txnStatus =
                                                statusRespDataList[5]

                                            DigiPosDataTable.insertOrUpdateDigiposData(
                                                tabledata
                                            )
                                            Log.e("F56->>", responsef57)
                                            VFService.showToast(getString(R.string.txn_status_still_pending))
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

                                            }

                                        }

                                        EDigiPosPaymentStatus.Approved.desciption -> {
                                            tabledata.txnStatus =
                                                statusRespDataList[5]
                                            DigiPosDataTable.insertOrUpdateDigiposData(
                                                tabledata
                                            )
                                            Log.e("F56->>", responsef57)

                                            txnSuccessToast(activity as Context)
                                            PrintUtil(context).printSMSUPIChagreSlip(
                                                tabledata,
                                                EPrintCopyType.MERCHANT,
                                                context
                                            ) { alertCB, printingFail ->
                                                //context.hideProgress()
                                                if (!alertCB) {
                                                    parentFragmentManager.popBackStack()

                                                }
                                            }
                                        }
                                        else -> {
                                            DigiPosDataTable.deletRecord(
                                                tabledata.partnerTxnId
                                            )
                                            VFService.showToast(statusRespDataList[5])

                                        }
                                    }

                                } else {
                                    lifecycleScope.launch(
                                        Dispatchers.Main
                                    ) {
                                        (activity as BaseActivity).alertBoxWithAction(
                                            null,
                                            null,
                                            getString(R.string.transaction_failed_msg),
                                            responseMsg,
                                            false,
                                            getString(R.string.positive_button_ok),
                                            { alertPositiveCallback ->
                                                if (alertPositiveCallback) {
                                                    DigiPosDataTable.deletRecord(
                                                        digiPosTabledata.partnerTxnId
                                                    )
                                                    parentFragmentManager.popBackStack()
                                                }
                                            },
                                            {})
                                    }
                                }

                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                logger(
                                    LOG_TAG.DIGIPOS.tag,
                                    "Somethig wrong... in response data field 57"
                                )
                            }
                        }
                    }
                }
                EDashboardItem.STATIC_QR -> {
                    // below commented code is for check the deleting qr code
                   /* activity?.deleteFile("$QR_FILE_NAME.jpg")
                    var imgbm: Bitmap? = null
                    runBlocking {
                        imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                    }
                    if (imgbm == null) {
                        logger("StaticQr", "  DELETED SUCCESS", "e")
                    }*/
                    parentFragmentManager.popBackStack()
                }

                else -> {
                }
            }

        }

        binding?.noBtn?.setOnClickListener {
           // parentFragmentManager.popBackStack()
            parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

        }

    }

}