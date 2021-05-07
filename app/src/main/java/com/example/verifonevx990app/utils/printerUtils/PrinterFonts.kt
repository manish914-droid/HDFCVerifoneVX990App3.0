package com.example.verifonevx990app.utils.printerUtils

import android.content.res.AssetManager
import com.example.verifonevx990app.vxUtils.VerifoneApp

/**
 * Created by Lucky on 08/07/2020.
 */
object PrinterFonts {

    const val FONT_AGENCYR = "AGENCYR.TTF"
    var path = ""
    fun initialize(assets: AssetManager?) {
        val fileName = FONT_AGENCYR
        path = VerifoneApp.appContext.externalCacheDir?.path + "/fonts/"
        ExtraFiles.copy("fonts/$fileName", path, fileName, assets, false)
    }
}

