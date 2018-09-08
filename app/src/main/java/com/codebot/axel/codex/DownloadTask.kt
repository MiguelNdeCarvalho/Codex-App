package com.codebot.axel.codex

/**
 * Created by Axel on 6/8/2018.
 */
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadTask(private val context: Context, downloadUrl: String, autoUpdates: Boolean, isPopup: Boolean, progressBar: ProgressBar, percentageTextView: TextView) {

    private var downloadUrl = ""
    private var downloadFileName = ""
    private var bar = progressBar
    private var percentage = percentageTextView

    init {
        this.downloadUrl = downloadUrl
        Log.d("MIUI Test: ", this.downloadUrl)
        downloadFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/'), downloadUrl.length)//Create file name by picking download file name from URL
        Log.e(TAG, downloadFileName)
        if (autoUpdates) {
            if (!File(Environment.getExternalStorageDirectory().toString() + "/CodeX-builds" + downloadFileName).exists())
                DownloadingTask(this).execute()
        } else {
            if (File(Environment.getExternalStorageDirectory().toString() + "/CodeX-builds" + downloadFileName).exists())
                alertUser()
            else
                DownloadingTask(this).execute()
        }
        if (isPopup)
            promptUserToFlash(context)
    }

    companion object {
        private val TAG = "Download Task"

        private class DownloadingTask(private val downloadTask: DownloadTask) : AsyncTask<Void, String, Void>() {

            internal var apkStorage: File? = null
            internal var outputFile: File? = null

            override fun onPreExecute() {
                super.onPreExecute()
                println("onPreExecute")
                downloadTask.bar.max = 100
                downloadTask.bar.visibility = View.VISIBLE
                downloadTask.percentage.visibility = View.VISIBLE
            }

            override fun onPostExecute(result: Void?) {
                downloadTask.bar.visibility = View.GONE
                downloadTask.percentage.visibility = View.GONE
                try {
                    if (outputFile != null) {
                        Toast.makeText(downloadTask.context, "Downloaded Successfully", Toast.LENGTH_SHORT).show()
                    } else {

                        Handler().postDelayed({ }, 3000)

                        Log.e(TAG, "Download Failed")

                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    //Change button text if exception occurs

                    Handler().postDelayed({ }, 3000)
                    Log.e(TAG, "Download Failed with Exception - " + e.localizedMessage)

                }


                super.onPostExecute(result)
            }

            override fun doInBackground(vararg arg0: Void): Void? {
                try {
                    val url = URL(downloadTask.downloadUrl)//Create Download URl
                    val c = url.openConnection() as HttpURLConnection//Open Url Connection
                    c.requestMethod = "GET"//Set Request Method to "GET" since we are grtting data
                    c.connect()//connect the URL Connection

                    val filelength = c.contentLength
                    println("fileLength: " + filelength)

                    //If Connection response is not OK then show Logs
                    if (c.responseCode != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "Server returned HTTP " + c.responseCode
                                + " " + c.responseMessage)

                    }
                    Log.d(TAG, Environment.getExternalStorageDirectory().toString() + "/CodeX-builds")
                    //Get File if SD card is present
                    if (downloadTask.isSDCardPresent()) {

                        apkStorage = File(
                                Environment.getExternalStorageDirectory().toString() + "/"
                                        + "CodeX-builds")
                    } else
                        Toast.makeText(downloadTask.context, "Oops!! There is no SD Card.", Toast.LENGTH_SHORT).show()

                    //If File is not present create directory
                    if (!apkStorage!!.exists()) {
                        apkStorage!!.mkdir()
                        Log.e(TAG, "Directory Created.")
                    }

                    outputFile = File(apkStorage, downloadTask.downloadFileName)//Create Output file in Main File

                    //Create New File if not present
                    if (!outputFile!!.exists()) {
                        outputFile!!.createNewFile()
                        Log.e(TAG, "File Created")
                    }

                    val fos = FileOutputStream(outputFile!!)//Get OutputStream for NewFile Location

                    val inputStream = c.inputStream//Get InputStream for connection

                    val buffer = ByteArray(1024)//Set buffer type
                    var total = inputStream.read(buffer)
                    var count = 0
                    while (total != -1) {
                        count += total
                        println("progress: " + count * 100 / filelength)
                        publishProgress("" + ((count * 100) / filelength))
                        fos.write(buffer, 0, total)//Write new file
                        total = inputStream.read(buffer)
                        println("total: " + count)
                        if (isCancelled)
                            break
                    }

                    //Close all connection after doing task
                    fos.close()
                    inputStream.close()

                } catch (e: Exception) {

                    //Read exception if something went wrong
                    e.printStackTrace()
                    outputFile = null
                    Log.e(TAG, "Download Error Exception " + e.message)
                }

                return null
            }

            override fun onProgressUpdate(vararg values: String?) {
                val percentage = values[0] + "%"
                downloadTask.percentage.text = percentage
                downloadTask.bar.progress = values[0]!!.toInt()
            }
        }
    }

    //Check If SD Card is present or not method
    fun isSDCardPresent(): Boolean {
        if (Environment.getExternalStorageState().equals(

                        Environment.MEDIA_MOUNTED)) {
            return true
        }
        return false
    }

    fun alertUser() {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_NEGATIVE -> {
                    dialog.dismiss()
                }
                DialogInterface.BUTTON_POSITIVE -> {
                    DownloadingTask(this).execute()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

        builder.setTitle("Hang On!")
                .setMessage("I've already found a same build on your device. You still want to continue downloading?")
                .setPositiveButton("Yes, I'm crazy", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .setCancelable(false)
                .show()
    }

    fun promptUserToFlash(context: Context) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_NEGATIVE -> {
                    dialog.dismiss()
                }
                DialogInterface.BUTTON_POSITIVE -> {
                    context.startActivity(Intent(context, FlashActivity::class.java))
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

        builder.setTitle("Flash Kernel?")
                .setMessage("Package has been downloaded. Reboot and flash now?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .setCancelable(false)
                .show()
    }
}