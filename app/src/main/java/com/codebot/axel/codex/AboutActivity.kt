package com.codebot.axel.codex

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_about.*
import okhttp3.*
import java.io.File
import java.io.IOException

class AboutActivity : AppCompatActivity() {

    val APP_VERSION = "1.1"
    val URL = "https://www.miguelndecarvalho.me/codex/codex.json"
    lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(toolbar2)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appBar2.bringToFront()

        about_logo.scaleType = ImageView.ScaleType.FIT_XY
        context = this

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        about_app_ver_textView.text = Html.fromHtml("<b>${getString(R.string.app_version)}</b>" + " $APP_VERSION")

        about_github.setOnClickListener {
            val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/MiguelNdeCarvalho"))
            startActivity(githubIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.about_items, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.about_check_for_updates -> {
                if (isNetworkAvailable())
                    checkForAppUpdates(context)
                else
                    promptNoNetwork(context)
                return true
            }
            R.id.install_packages -> {
                val client = OkHttpClient()

                val request = Request.Builder().url(URL).build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        e!!.printStackTrace()
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        val bodyOfJSON = response?.body()?.string()
                        val gson = GsonBuilder().create()
                        val codexData = gson.fromJson(bodyOfJSON, Codex::class.java)
                        verifyAndInstall(context, codexData)
                    }
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun checkForAppUpdates(context: Context) {
        val client = OkHttpClient()

        val request = Request.Builder().url(URL).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                e!!.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response?) {
                val bodyOfJSON = response?.body()?.string()
                val gson = GsonBuilder().create()
                val codexData = gson.fromJson(bodyOfJSON, Codex::class.java)
                val fileName = codexData.url.substring(codexData.url.lastIndexOf('/') + 1, codexData.url.length)
                if (File(Environment.getExternalStorageDirectory().toString() + "/Codex-builds/", fileName).exists())
                    verifyAndInstall(context, codexData)
                else {
                    if (!codexData.version.equals(APP_VERSION)) {
                        alertUpdate(context, codexData)

                    } else {
                        runOnUiThread {
                            val mySnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), "No Update available", Snackbar.LENGTH_LONG)
                            mySnackbar.view.setBackgroundColor(getColor(R.color.background))
                            mySnackbar.show()
                        }
                    }
                }
            }
        })
    }

    fun alertUpdate(context: Context, codexInfo: Codex) {
        runOnUiThread {
            val fileName = codexInfo.url.substring(codexInfo.url.lastIndexOf('/') + 1, codexInfo.url.length)
            val dialogListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        if (!File(Environment.getExternalStorageDirectory().toString() + "/Codex-builds/", fileName).exists())
                            DownloadTask(context, codexInfo.url, false, false, about_progressBar, about_percentage_textView)
                        else {
                            if (!APP_VERSION.equals(codexInfo.version)) {
                                verifyAndInstall(context, codexInfo)
                            } else {
                                val mySnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), "You already have the latest version", Snackbar.LENGTH_LONG)
                                mySnackbar.view.setBackgroundColor(getColor(R.color.background))
                                mySnackbar.show()
                            }
                        }
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {
                        dialog.dismiss()
                    }
                }
            }
            val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))
            builder.setTitle("App Update Available")
                    .setMessage("Download update now?")
                    .setPositiveButton("Yes", dialogListener)
                    .setNegativeButton("Later", dialogListener)
                    .show()
        }
    }

    fun verifyAndInstall(context: Context, codexInfo: Codex) {
        val fileName = codexInfo.url.substring(codexInfo.url.lastIndexOf('/') + 1, codexInfo.url.length)
        runOnUiThread {
            if (File(Environment.getExternalStorageDirectory().toString() + "/Codex-builds/", fileName).exists()) {
                val dialogListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val file = File(Environment.getExternalStorageDirectory().toString() + "/Codex-builds/", fileName)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                            startActivity(intent)
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            dialog.dismiss()
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
                DialogInterface.BUTTON_NEGATIVE -> {
                    dialog.dismiss()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))
        builder.setTitle("No Package found")
                .setMessage("No package is found to install")
                .setCancelable(false)
                .setNegativeButton("Ok", dialogListener)
                .show()
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
                    dialog.dismiss()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

        builder.setTitle("No Internet!")
                .setMessage("Please check your network connection and try again")
                .setNegativeButton("Ok", dialogClickListener)
                .setCancelable(false)
                .show()
    }
}
