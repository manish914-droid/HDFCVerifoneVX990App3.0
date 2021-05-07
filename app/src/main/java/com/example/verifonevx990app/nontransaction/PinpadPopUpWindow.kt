package com.example.verifonevx990app.nontransaction

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.Constraints
import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.main.DetectCardType
import com.example.verifonevx990app.main.PosEntryModeType
import com.example.verifonevx990app.utils.Utility
import com.example.verifonevx990app.vxUtils.EFallbackCode
import com.example.verifonevx990app.vxUtils.VFService
import com.example.verifonevx990app.vxUtils.VFService.vfPinPad
import com.vfi.smartpos.deviceservice.aidl.PinInputListener
import com.vfi.smartpos.deviceservice.aidl.PinKeyCoorInfo
import com.vfi.smartpos.deviceservice.constdefine.ConstIPinpad
import java.util.*

class PinpadPopUpWindow(
    context: Context,
    style: Int,
    var handler: Handler?,
    var cardProcessedDataModal: CardProcessedDataModal,
    var title: String,
    var prompt: String
) : Dialog(context, style), View.OnClickListener {

    //  private var handler: Handler? = null
    private val conentView: View? = null
    var listener: OnPwdListener? = null
    var btn = arrayOfNulls<ImageButton>(10)
    var btn_conf: ImageButton? = null
    var btn_cancel: ImageButton? = null
    var btn_del: ImageButton? = null
    var dsp_pwd: EditText? = null
    private val keysMap: MutableMap<String, ImageButton?> = HashMap()
    private var isFirstStart = true


    fun getPwdListener(): OnPwdListener? {
        return listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val convertView = layoutInflater.inflate(R.layout.activity_inner_pwd_layout, null)
        setContentView(convertView)
        window?.setGravity(Gravity.BOTTOM) // 显示在底部
        window?.decorView?.setPadding(0, 0, 0, 0)
        window?.setWindowAnimations(R.style.popup_anim_style) // 添加动画
        val lp = window?.attributes
        lp?.width = WindowManager.LayoutParams.MATCH_PARENT
        lp?.height = 750
        isFirstStart = true
        window?.attributes = lp
        Log.i("InputPwdDialog", "onCreate")
        findView(convertView)
        Log.i("InputPwdDialog", "initViews")
        //TickTimer.cancle();
    }

    //当页面加载完成之后再执行弹出键盘的动作
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val param = Bundle()
            val globleparam = Bundle()
            val panBlock = cardProcessedDataModal.getPanNumberData()
            val pinLimit = byteArrayOf(4, 5, 6)
            param.putByteArray("pinLimit", pinLimit)
            param.putInt("timeout", 60)
            when (cardProcessedDataModal.getIsOnline()) {
                1 -> param.putBoolean(ConstIPinpad.startPinInput.param.KEY_isOnline_boolean, true)
                2 -> param.putBoolean(ConstIPinpad.startPinInput.param.KEY_isOnline_boolean, false)
            }
            param.putBoolean("isOnline", true)
            param.putString("pan", panBlock)
            param.putString("promptString", "please input pin:")
            param.putInt("desType", ConstIPinpad.startPinInput.param.Value_desType_3DES)
            val num = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            param.putByteArray(
                "displayKeyValue",
                num
            ) // TODO - Demo, this is the demo using static sequence of the PIN numbers
            val pinInputListener: PinInputListener = object : PinInputListener.Stub() {
                @Throws(RemoteException::class)
                override fun onInput(len: Int, key: Int) {
                    // the key is * always.
                    Log.d("TAG", "onInput, length:$len,key:$key")
                    val buf = CharArray(len)
                    Arrays.fill(buf, '*')
                    val s = String(buf)
                    Log.d("TAG", s)
                    // call up UI to update the view
                    setContentText(s)
                }

                @Throws(RemoteException::class)
                override fun onConfirm(data: ByteArray, isNonePin: Boolean) {
                    Log.d("TAG", "onConfirm")
                    VFService.vfIEMV?.importPin(1, data)
                    //      exitKeyBoardOnUI();
//                Log.d("TAG", data.toString());
                    Log.d("TAG", Utility.byte2HexStr(data))
                    when (cardProcessedDataModal.getReadCardType()) {
                        // vfIEMV?.importPin(1, data)
                        DetectCardType.EMV_CARD_TYPE -> {
                            try {
                                //    vfIEMV?.importPin(1, data)
                            } catch (ex: DeadObjectException) {

                            } catch (ex: RemoteException) {

                            } catch (ex: Exception) {

                            }

                            Log.d("TAG", data.toString())
                            if (cardProcessedDataModal.getIsOnline() == 1) {
                                cardProcessedDataModal.setGeneratePinBlock(Utility.byte2HexStr(data))
                                //insert with pin
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                            } else {
                                cardProcessedDataModal.setGeneratePinBlock("")
                                //off line pin
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_OFFLINE_PIN.posEntry.toString())
                            }
                        }
                        DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                            //      vfIEMV?.importPin(1, data)
                            Log.d("TAG", data.toString())
                            if (cardProcessedDataModal.getIsOnline() == 1) {
                                cardProcessedDataModal.setGeneratePinBlock(Utility.byte2HexStr(data))
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())
                            } else {
                                cardProcessedDataModal.setGeneratePinBlock("")
                                //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                            }
                        }
                        DetectCardType.MAG_CARD_TYPE -> {
                            //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                            cardProcessedDataModal.setGeneratePinBlock(Utility.byte2HexStr(data))

                            if (cardProcessedDataModal.getFallbackType() == EFallbackCode.EMV_fallback.fallBackCode)
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
                            else
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
                            cardProcessedDataModal.setApplicationPanSequenceValue("00")
                        }

                        else -> {
                        }
                    }
                    if (null != listener) {
                        listener?.onSucc(data)
                    }
                }

                @Throws(RemoteException::class)
                override fun onCancel() {
                    Log.d("TAG", "onCannel")
                    //     exitKeyBoardOnUI();
                    listener?.onErr()
                }

                @Throws(RemoteException::class)
                override fun onError(errorCode: Int) {
                    Log.d("TAG", "onError:$errorCode")
                    //exitKeyBoardOnUI();
                    listener?.onErr()
                }
            }
            val handler = Handler()
            val ret = handler.post {
                Log.d(
                    Constraints.TAG,
                    "start the pinpad"
                )
                val pinKeys = getKeyCoorInfos()
                try {
                    Log.d(
                        Constraints.TAG,
                        "pinKeys size:" + pinKeys.size
                    )


                    var keyMap: MutableMap<String?, String?> = vfPinPad?.initPinInputCustomView(
                        2,
                        param,
                        pinKeys,
                        pinInputListener
                    ) as MutableMap<String?, String?>
                    Log.d("TAG", "size:" + keyMap.size)
                    val entrys =
                        keyMap.entries
                    for ((key, value) in entrys) {
                        Log.d("TAG", key.toString() + "--" + value)
                        val btn: Button? = null
                        val index = key!![4] - '0'
                        if (index >= 0 && index < 10) {
                            //                            mPinpadPopUpWindow.btn[index].setText(entry.getValue() + "\n[" + index + "]");
                            if (value == "2") {
                                //    mPinpadPopUpWindow.btn[index].setText(entry.getValue());
                            } else {
                                //  mPinpadPopUpWindow.btn[index].setText(entry.getValue());
                            }
                        }
                    }
                    vfPinPad!!.startPinInputCustomView()
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        } else listener!!.onErr()
    }


    fun setContentText(content: String?) {
        if (handler != null) {
            handler!!.post {
                if (dsp_pwd != null) {
                    dsp_pwd?.setText(content)
                    dsp_pwd!!.textSize = 25f
                }
            }
        }
    }

    private fun findView(keyboardView: View) {
        btn[0] =
            keyboardView.findViewById<View>(R.id.keyboard_btn0) as ImageButton
        btn[1] =
            keyboardView.findViewById<View>(R.id.keyboard_btn1) as ImageButton
        btn[2] =
            keyboardView.findViewById<View>(R.id.keyboard_btn2) as ImageButton
        btn[3] =
            keyboardView.findViewById<View>(R.id.keyboard_btn3) as ImageButton
        btn[4] =
            keyboardView.findViewById<View>(R.id.keyboard_btn4) as ImageButton
        btn[5] =
            keyboardView.findViewById<View>(R.id.keyboard_btn5) as ImageButton
        btn[6] =
            keyboardView.findViewById<View>(R.id.keyboard_btn6) as ImageButton
        btn[7] =
            keyboardView.findViewById<View>(R.id.keyboard_btn7) as ImageButton
        btn[8] =
            keyboardView.findViewById<View>(R.id.keyboard_btn8) as ImageButton
        btn[9] =
            keyboardView.findViewById<View>(R.id.keyboard_btn9) as ImageButton
        btn_del =
            keyboardView.findViewById<View>(R.id.keyboard_btn_delete) as ImageButton
        btn_cancel =
            keyboardView.findViewById<View>(R.id.keyboard_btn_cancel) as ImageButton
        btn_conf =
            keyboardView.findViewById<View>(R.id.keyboard_btn_confirm) as ImageButton
        for (i in 0..9) {
            btn[i]!!.setOnClickListener(this)
        }
        btn_conf?.setOnClickListener(this)
        btn_cancel?.setOnClickListener(this)
        btn_del?.setOnClickListener(this)
        dsp_pwd = keyboardView.findViewById<View>(R.id.dsp_pwd) as EditText

        val titleTv: TextView = keyboardView.findViewById(R.id.prompt_title)
        titleTv.text = title

        val subtitleTv: TextView = keyboardView.findViewById(R.id.prompt_no_pwd)
        if (prompt != null) {
            subtitleTv.text = prompt
        } else {
            subtitleTv.visibility = View.INVISIBLE
        }
    }

    override fun onClick(v: View?) {
        Log.d("TAG", "onclick...")
    }

    fun getKeyCoorInfos(): List<PinKeyCoorInfo> {
        val keyCoorInfos: MutableList<PinKeyCoorInfo> =
            ArrayList()
        for (i in 0..9) {
            keyCoorInfos.add(getKeyCoorInfo("btn_$i", btn[i], TYPE_NUM))
        }
        keyCoorInfos.add(getKeyCoorInfo("btn_10", btn_conf, TYPE_CONF))
        keyCoorInfos.add(getKeyCoorInfo("btn_11", btn_cancel, TYPE_CANCEL))
        keyCoorInfos.add(getKeyCoorInfo("btn_12", btn_del, TYPE_DEL))
        //            keyCoorInfos.add(getKeyCoorInfo("btn_12", btn_del, KeyCoorInfo.TYPE_DEL_ALL));
        return keyCoorInfos
    }

    private fun getKeyCoorInfo(
        keyId: String,
        btn: ImageButton?,
        type: Int
    ): PinKeyCoorInfo {
        keysMap[keyId] = btn // get the coordinate information and save to may
        //按钮保存到Map中，keyid与返回的map中keyid对应从而找到那个按钮，修改该button的显示内容
        val location = IntArray(2)
        btn!!.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return PinKeyCoorInfo(keyId, x, y, x + btn.width, y + btn.height, type)
    }

    val TYPE_NUM = 0
    val TYPE_CONF = 1
    val TYPE_CANCEL = 2
    val TYPE_DEL = 3
    val TYPE_DEL_ALL = 4

    interface OnPwdListener {
        fun onSucc(data: ByteArray?)
        fun onErr()
    }
}