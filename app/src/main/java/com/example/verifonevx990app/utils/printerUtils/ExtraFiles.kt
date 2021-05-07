package com.example.verifonevx990app.utils.printerUtils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.*

/**
 * Created by Simon on 2018/5/9.
 */
class ExtraFiles {
    fun test(ctxDealFile: Context) {
        var ctxDealFile = ctxDealFile
        try {
            ctxDealFile = ctxDealFile.createPackageContext(
                "com.example.test",
                Context.CONTEXT_IGNORE_SECURITY
            )
        } catch (e1: PackageManager.NameNotFoundException) {
            // TODO Auto-generated catch block
            e1.printStackTrace()
        }
        val uiFileName = "applist"
        deepFile(ctxDealFile, uiFileName, "")
    }

    /** 上边的内容卸载onCreate中
     * 遍历assets下的所有文件
     * @param ctxDealFile
     * @param path
     */
    fun deepFile(
        ctxDealFile: Context,
        path: String,
        wantFileName: String
    ): ByteArray? {
        var path = path
        var buffer: ByteArray? = null
        try {
            val str = ctxDealFile.assets.list(path)
            if (str!!.size > 0) { // 如果是目录
                val file = File("/data/$path")
                //                file.mkdirs();
                for (string in str) {
                    path = "$path/$string"
                    //println("zhoulc:\t$path")
                    // textView.setText(textView.getText()+"\t"+path+"\t");
                    buffer = deepFile(ctxDealFile, path, wantFileName)
                    if (buffer != null) {
                        return buffer
                    }
                    path = path.substring(0, path.lastIndexOf('/'))
                }
            } else { // 如果是文件
                if (wantFileName == path) {
                    return readAssets(path, ctxDealFile.assets)
                }
                //                InputStream is = ctxDealFile.getAssets().open(path);
//                FileOutputStream fos = new FileOutputStream(new File("/data/"
//                        + path));
//                byte[] buffer = new byte[1024];
//                int count = 0;
//                while (true) {
//                    count++;
//                    int len = is.read(buffer);
//                    if (len == -1) {
//                        break;
//                    }
//                    fos.write(buffer, 0, len);
//                }
//                is.close();
//                fos.close();
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return null
    }

    /**
     *
     * 将assets的文件写到固定目录下
     */
    private fun importDB(
        assetFileName: String,
        innerFileParent: String,
        innerFileChild: String,
        assets: AssetManager
    ) {
        var innerFileParent: String? = innerFileParent
        var innerFileChild: String? = innerFileChild
        innerFileParent = "data/data/com.example.test/"
        innerFileChild = "applist/applist.preincluded.description"
        val file = File(innerFileParent, innerFileChild)
        // String DbName = "people_db";
        // 判断是否存在
        if (file.exists() && file.length() > 0) {
        } else {
            // 使用AssetManager类来访问assets文件夹
            var ins: InputStream? = null
            var fos: FileOutputStream? = null
            try {
                ins = assets.open(assetFileName)
                fos = FileOutputStream(file)
                var len = 0
                val buf = ByteArray(1024)
                while (ins.read(buf).also { len = it } != -1) {
                    fos.write(buf, 0, len)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    ins?.close()
                    fos?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadImage(assets: AssetManager): Bitmap? {
//        ImageView imageView = (ImageView) findViewById(R.id.image);
        /**
         * 使用assets下的图片
         * http://www.2cto.com/kf/201408/322920.html
         */
        var bmp: Bitmap? = null
        val ins: InputStream
        try {
            ins = assets.open("applist/applogo.png")
            bmp = BitmapFactory.decodeStream(ins)
            //            imageView.setImageBitmap(bmp);
        } catch (e2: IOException) {
            // TODO Auto-generated catch block
            e2.printStackTrace()
        }
        return bmp
    }

    /**
     * 往固定的目录下的文件中写内容
     * @param fileName 要操作的绝对路径/data/data/包名/路径+文件名
     * @param write_str 要写入的内容
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeSDFile(
        fileName: String?,
        write_str: String,
        context: Context
    ) {
        val file = File(fileName)
        val fos = FileOutputStream(file)
        val fos1 = context.openFileOutput(
            fileName,
            Context.MODE_PRIVATE
        )
        val bytes = write_str.toByteArray()
        fos1.write(bytes)
        fos.close()
    }

    /**
     * 读取固定目录下的文件（外部存储的操作。真机没有root是不可以的）
     * @param fileName
     * @return
     * @throws IOException
     * 参考博客：http://blog.csdn.net/ztp800201/article/details/7322110
     */
    @Throws(IOException::class)
    fun readSDFile(fileName: String?, context: Context): String {
        val file = File(fileName)
        // fileinputstream是不能传入路径的，只传入名称就找不到文件。所以需要传入file
        val fis = FileInputStream(file)
        val fis1 = context.openFileInput(fileName)
        val length = fis1.available()
        val buffer = ByteArray(length)
        fis1.read(buffer)

        // String res = EncodingUtils.getString(buffer, "UTF-8");
        fis1.close()
        // return res;
        return String(buffer)
    }

    /**
     * 内部存储的写方法
     */
    fun writeNeibu(context: Context) {
        val str = "测试内容111"
        // getFileDir()方法获得是file的路径，就是data/data/包名/file
        // 但是我想在自定义的路径下生成文件，我就获得file路径的父路径
        val dataDir = context.filesDir.parentFile
        val mydir = File(dataDir, "aaa")
        // 创建data/data/包名/aaa路径
        mydir.mkdir()
        val file = File(mydir, "test.txt")
        var bw: BufferedWriter? = null
        try {
            file.createNewFile()
            // fileoutputstream的第二个参数，就是决定是否追加 ，false为替换，true就会在尾部追加内容
            bw = BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(file, false), "UTF-8"
                )
            )
            // fw.append("测试内容");
            bw.write(str)
            bw.flush()
            bw.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 内部存储的读方法
     */
    fun readNeibu(context: Context) {
        val dataDir = context.filesDir.parentFile
        val mydir = File(dataDir, "aaa")
        val file = File(mydir, "test.txt")
        var br: BufferedReader? = null
        try {
            br = BufferedReader(
                InputStreamReader(
                    FileInputStream(
                        file
                    ), "UTF-8"
                )
            )
            var str1: String? = null
            var a: Int
            while (br.read().also { a = it } != -1) {
                str1 = br.readLine()
                //println(str1)
            }
            br.close()
        } catch (e: UnsupportedEncodingException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "VFI.ExtraFiles"

        /**
         * @param fileName read the file in assets
         * @param assets the assets set by getAssets
         * @return the content of the file in byte[]
         */
        fun readAssets(fileName: String?, assets: AssetManager): ByteArray {
            var buffer: ByteArray? = null
            try {
                //
                val ins = assets.open(fileName!!)
                // get the size
                val size = ins.available()
                // crete the array of byte
                buffer = ByteArray(size)
                ins.read(buffer)
                // close the stream
                ins.close()
                //            // byte to String
//            String text = new String(buffer, "UTF-8");
            } catch (e: IOException) {
                // Should never happen!
                throw RuntimeException(e)
            }
            return buffer
        }

        /**
         * copy file in assets to system (sdcard)
         * @param sourceAssetFileName the source file in assets
         * @param targetPath the target path
         * @param targetFileName the target fileName
         * @param assets the AssetManager from getAssets
         * @return true for success, false for failure
         */
        fun copy(
            sourceAssetFileName: String?,
            targetPath: String,
            targetFileName: String,
            assets: AssetManager?,
            overWriteIfExists: Boolean
        ): Boolean {
            val fullFileName = targetPath + targetFileName
            val file = File(fullFileName)
            if (file.exists() && !overWriteIfExists) {
                Log.i(
                    TAG,
                    "return while file [$fullFileName] exists"
                )
                return true
            }
            val dir = File(targetPath)
            // try create the fold if not exists
            if (!dir.exists()) dir.mkdirs()
            try {
                if (!File(fullFileName).exists()) {
                    val ins = assets?.open(sourceAssetFileName!!)
                    val fos = FileOutputStream(fullFileName)
                    val buffer = ByteArray(4096)
                    var count = 0
                    while (ins?.read(buffer).also {
                            if (it != null) {
                                count = it
                            }
                        }!! > 0) {
                        fos.write(buffer, 0, count)
                    }
                    fos.close()
                    ins?.close()
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }
}