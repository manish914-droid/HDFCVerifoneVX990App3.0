package com.example.verifonevx990app.vxUtils

import android.content.Context
import android.util.Log
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject

object AppPreference {

    private val TAG = AppPreference::class.java.simpleName

    const val BATCH_LOCK = "b_l_k"

    const val BANKCODE = "01" // "07" for amex, 08 for Hitachi
    const val WALLET_ISSUER_ID = "50"


    const val HDFC_BANK_CODE = "01"
    const val HDFC_ISSUER_CODE = "50"

    const val AMEX_BANK_CODE = "07"
    const val AMEX_WALLET_ISSUER = "53"

    private const val PREFERENCE_NAME = "PaxApp"  // todo App Name to be changed
    const val LOGIN = "login"

    //region========EMV=======
    const val PC_NUMBER_KEY = "pc_number_key"
    const val PC_NUMBER_KEY_2 = "pc_number_key_2"

    const val BANK_CODE_KEY = "bank_code_key"

    const val LAST_BATCH = "last_batch"
    const val HEADER_FOOTER = "header_footer"
    const val LAST_SAVED_AUTO_SETTLE_DATE = "last_saved_auto_settle_date"
    const val IsAutoSettleDone = "is_auto_settle_done"

    const val F48IdentifierAndSuccesssTxn = "f48id_txnDate"

    const val BrandID = "brandID"

    @JvmStatic
    fun getBankCode(): String {
        val tBc = getString(BANK_CODE_KEY)
        return addPad(if (tBc.isNotEmpty()) tBc else BANKCODE, "0", 2)
    }

    @JvmStatic
    fun setBankCode(bankCode: String) {
        saveString(BANK_CODE_KEY, bankCode)
    }


    const val CRDB_ISSUER_ID_KEY = "crdb_issuer_id_key"
    const val ACC_SEL_KEY = "acc_sel_key"
    //endregion


    const val REVERSAL_DATA = "reversal_data"

    const val IS_LOGED_ON = "is_logon"

    const val BLOCK_COUNTER = "BLOCK_COUNTER"


    const val LAST_EMV_PARAM = "last_emv_param"

    @Deprecated("Use ROC_V2")
    const val ROC = "roc_tan"

    const val ROC_V2 = "roc_tan_v2"

    const val F48_STAMP = "f48timestamp"

    const val IS_AUTO_SETTLE_ENABLED = "activate_auto_settle"

    private const val IS_AUTO_SETTLE_ALIVE = "auto_settle_alive"

    const val ABATCH_KEY = "abatch_key"
    const val EMI_Field58 = "emi_field58"
    const val GENERIC_REVERSAL_KEY = "generic_reversal_key"
    const val ONLINE_EMV_DECLINED = "online_emv_Declined"
    const val LAST_SUCCESS_RECEIPT_KEY = "Last_Success_Receipt"

    @JvmStatic
    fun saveBoolean(name: String, b: Boolean) {
        val p = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val edit = p.edit()
        edit.putBoolean(name, b)
        edit.apply()
    }

    @JvmStatic
    fun getBoolean(name: String): Boolean =
        VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .getBoolean(name, false)

    @JvmStatic
    fun saveLogin(b: Boolean) {
        val p = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val edit = p.edit()
        edit.putBoolean(LOGIN, b)
        edit.apply()
    }

    @JvmStatic
    fun saveString(prefName: String, content: String?) {
        var newContent = content
        if (prefName == GENERIC_REVERSAL_KEY) {
            val jj = JSONObject(content)
            val activeFieldArr = jj.get("activeField") as JSONArray
            // var loopIterator=
            if (activeFieldArr.length() > 0) {
                for (i in 0 until activeFieldArr.length() - 1) {
                    if (i == activeFieldArr.length() - 1) {
                        break
                    } else {
                        if (activeFieldArr[i].toString() == "52" || activeFieldArr[i].toString() == "31" || activeFieldArr[i].toString() == "54") {
                            activeFieldArr.remove(i)
                            //  loopIterator -= 1
                        }
                    }
                }

            }
            val isoMap = jj.get("isoMap") as JSONObject
            isoMap.remove("52")
            newContent = jj.toString()
        }

        val p = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val edit = p?.edit()
        edit?.putString(prefName, newContent)
        edit?.apply()
    }

    @JvmStatic
    fun getString(prefName: String): String {
        val p = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return p?.getString(prefName, null) ?: ""
    }


    @JvmStatic
    fun getLogin(): Boolean =
        VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .getBoolean(LOGIN, false)

    @JvmStatic
    fun getIntData(key: String): Int {
        val v = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return v?.getInt(key, 0) ?: 0
    }

    @JvmStatic
    fun setIntData(key: String, value: Int) {
        val p = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val edit = p?.edit()
        edit?.putInt(key, value)
        edit?.apply()
    }


/*    @JvmStatic
    fun saveReversal(isoDataWriter: IsoDataWriter): Boolean {
        logger(TAG, "========saveReversal=========", "e")
        val json = Gson().toJson(isoDataWriter, object : TypeToken<IsoDataWriter>() {}.type) ?: ""
        if (json.isNotEmpty()) {
            VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).apply {
                val ed = edit()
                return if (ed != null) {
                    ed.putString(REVERSAL_DATA, json)
                    ed.apply()
                    true
                } else {
                    false
                }
            }
        } else {
            return false
        }
    }*/

    //Edited by Lucky
    @JvmStatic
    fun clearReversal() {
        logger(TAG, "========clearReversal=========", "e")
        VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).apply {
            val ed = edit()
            ed?.putString(GENERIC_REVERSAL_KEY, "")
            ed?.apply()
            Log.e("REVERSAL", "CLEAR REVERSAL")
        }
    }

    @JvmStatic
    fun getReversal(): IsoDataWriter? {
        logger(TAG, "========getReversal=========", "e")
        val v = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return if (v != null) {
            try {
                val str = v.getString(GENERIC_REVERSAL_KEY, "")
                if (!str.isNullOrEmpty()) {
                    Gson().fromJson<IsoDataWriter>(str, object : TypeToken<IsoDataWriter>() {}.type)
                } else null
            } catch (ex: Exception) {
                throw Exception("Reversal error!!!")
            }
        } else
            null
    }


    @JvmStatic
    suspend fun getAutoSettle(): Boolean = VerifoneApp.appContext.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    ).getBoolean(IS_AUTO_SETTLE_ALIVE, false)


    suspend fun setAutoSettle(isActive: Boolean) {
        VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).apply {
            val ed = edit()
            ed?.putBoolean(IS_AUTO_SETTLE_ALIVE, isActive)
            ed?.apply()
        }
    }

    @JvmStatic
    fun getLastSuccessReceipt(): BatchFileDataTable? {
        logger(TAG, "========getLastSuccessReceipt=========", "e")
        val v = VerifoneApp.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return if (v != null) {
            try {
                val str = v.getString(LAST_SUCCESS_RECEIPT_KEY, "")
                if (!str.isNullOrEmpty()) {
                    Gson().fromJson<BatchFileDataTable>(
                        str,
                        object : TypeToken<BatchFileDataTable>() {}.type
                    )
                } else null
            } catch (ex: Exception) {
                throw Exception("Last Success Receipt Error!!!")
            }
        } else
            null
    }


}
