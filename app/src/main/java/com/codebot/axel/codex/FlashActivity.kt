package com.codebot.axel.codex

import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.os.RecoverySystem
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.ContextThemeWrapper
import android.view.View
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_flash.*
import okhttp3.*
import java.io.File
import java.io.IOException

class FlashActivity : AppCompatActivity() {

    val URL = "https://www.miguelndecarvalho.me/codex/whyred.json"
    val KERNEL_VERSION_FULL = System.getProperty("os.version")
    val KERNEL_VERSION = KERNEL_VERSION_FULL.substring(KERNEL_VERSION_FULL.lastIndexOf('-') + 1, KERNEL_VERSION_FULL.length)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flash)

        setSupportActionBar(toolbar1)
        appBar1.bringToFront()
        progressBar.isIndeterminate = true
        progressBar.visibility = View.VISIBLE
        if (!isNetworkAvailable())
            promptNoNetwork(this)
        else
            getUrlAndFlash(this)
    }

    fun getUrlAndFlash(context: Context) {
        val client = OkHttpClient()

        val request = Request.Builder().url(URL).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                e!!.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response?) {
                val bodyOfJSON = response?.body()?.string()
                val gson = GsonBuilder().create()
                val codexData = gson.fromJson(bodyOfJSON, CodexInfo::class.java)
                val downloadUrl = codexData.downloads.url
                val fileToFlash = downloadUrl.substring(downloadUrl.lastIndexOf('/'), downloadUrl.length)
                runOnUiThread {
                    verifyAndFlash(context, fileToFlash)
                }
            }

            fun verifyAndFlash(context: Context, fileToFlash: String) {
                if (fileToFlash.substring(fileToFlash.indexOf('-') + 1, fileToFlash.lastIndexOf('.')).equals(KERNEL_VERSION))
                    noFlashNeeded(context)
                else {
                    if (File(Environment.getExternalStorageDirectory().toString() + "/CodeX-builds$fileToFlash").exists()) {
                        progressBar.visibility = View.GONE
                        val dialogListener = DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    flashPackage(context, fileToFlash)
                                }
                                DialogInterface.BUTTON_NEGATIVE -> {
                                    finish()
                                }
                            }
                        }

                        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))
                        builder.setTitle("Package found")
                                .setMessage("Do you want to install the package now?")
                                .setCancelable(false)
                                .setPositiveButton("Yes", dialogListener)
                                .setNegativeButton("Later", dialogListener)
                                .show()
                    } else
                        alertUser(context)
                }
            }

            fun alertUser(context: Context) {
                val dialogListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            finish()
                        }
                    }
                }

                val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))
                builder.setMessage("I haven't found any latest build. Make sure you have downloaded latest build before flashing")
                        .setCancelable(false)
                        .setTitle("Oops!")
                        .setPositiveButton("Ok", dialogListener)
                        .show()
            }

            fun flashPackage(context: Context, packageName: String) {
                val installPackage = File(Environment.getExternalStorageDirectory().toString() + "/CodeX-builds$packageName")
                RecoverySystem.installPackage(context, installPackage)
                installPackage.delete()
            }
        })
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun promptNoNetwork(context: Context) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_NEGATIVE -> {
                    finish()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

        builder.setTitle("No Internet!")
                .setMessage("Please check your network connection and try again")
                .setNegativeButton("Exit", dialogClickListener)
                .setCancelable(false)
                .show()
    }

    fun noFlashNeeded(context: Context) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_NEGATIVE -> {
                    finish()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

        builder.setTitle("Already Installed!")
                .setMessage("You already have the latest CodeX installed.")
                .setNegativeButton("Ok", dialogClickListener)
                .setCancelable(false)
                .show()
    }
}
