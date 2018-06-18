package com.codebot.axel.codex

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.*
import java.io.IOException


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val KERNEL = "CodeX"
    val KERNEL_VERSION_FULL = System.getProperty("os.version")
    val KERNEL_VERSION = KERNEL_VERSION_FULL.substring(KERNEL_VERSION_FULL.lastIndexOf('-') + 1, KERNEL_VERSION_FULL.length)
    val KERNEL_NAME = KERNEL_VERSION_FULL.substring(KERNEL_VERSION_FULL.indexOf('-') + 1, KERNEL_VERSION_FULL.lastIndexOf('-'))
    val URL = "https://raw.githubusercontent.com/AxelBlaz3/Codex-Kernel/gh-pages/whyred.json"
    lateinit var preferences: SharedPreferences
    val context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        appBar.bringToFront()

        println(KERNEL_VERSION)
        progressBar2.visibility = View.INVISIBLE
        logo_imageView.scaleType = ImageView.ScaleType.FIT_XY

        if (KERNEL_NAME != KERNEL) {
            alertUser()
        } else {
            // Runtime.getRuntime().exec("su")

            isStoragePermissionGranted()
            device_textView.text = Html.fromHtml("<b>" + getString(R.string.device) + "</b>" + " " + Build.DEVICE)
            model_textView.text = Html.fromHtml("<b>" + getString(R.string.model) + "</b>" + " " + Build.MODEL)
            kernel_textView.text = Html.fromHtml("<b>" + getString(R.string.kernel) + "</b>" + " " + KERNEL_NAME + " " + KERNEL_VERSION)

            val toggle = ActionBarDrawerToggle(
                    this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer_layout.addDrawerListener(toggle)
            toggle.syncState()

            nav_view.setNavigationItemSelectedListener(this)
//        if (File("${Environment.getExternalStorageDirectory()}/CodeX-builds/CodeX.apk").exists()) {
//            Shell.SU.run("su -c mount -o rw,remount /system")
//            Shell.SU.run("su -c mv /sdcard/CodeX-builds/CodeX.apk /system/priv-app/CodeX/CodeX.apk")
//            Shell.SU.run("su -c chmod 644 /system/priv-app/CodeX/CodeX.apk")
//            Toast.makeText(this, "Copied", Toast.LENGTH_LONG).show()
//        }

            preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val autoUpdates = preferences.getBoolean(getString(R.string.key_auto_updates), false)

            if (preferences.getBoolean(getString(R.string.key_check_updates), true))
                checkForUpdates(this, true)

            if (autoUpdates) {
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
                        runOnUiThread {
                            DownloadTask(context, codexData.downloads.url, autoUpdates, false, progressBar2, percentage_textView)
                        }
                    }
                })
            }

            updates_layout.setOnClickListener {
                val mySnackbar = Snackbar.make(findViewById(R.id.home_coordinatorLayout), "Checking for kernel updates", Snackbar.LENGTH_LONG)
                mySnackbar.view.setBackgroundColor(getColor(R.color.background))
                mySnackbar.show()
                checkForUpdates(this, true)
            }

            xda_constraint_layout.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.xda-developers.com/redmi-note-5-pro/development/kernel-codex-kernel-v1-0-t3805198"))
                startActivity(browserIntent)
            }

            telegram_constraint_layout.setOnClickListener {
                val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AxelBlaz3"))
                startActivity(telegramIntent)
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_downloads -> {
                if (isNetworkAvailable()) {
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
                            runOnUiThread {
                                DownloadTask(context, codexData.downloads.url, false, false, progressBar2, percentage_textView)
                            }
                        }
                    })
                } else
                    promptNoNetwork(context)
            }
            R.id.nav_settings -> {
                val settings_intent = Intent(this, SettingsActivity::class.java)
                startActivity(settings_intent)
            }
            R.id.nav_flash -> {
                val flash_intent = Intent(this, FlashActivity::class.java)
                startActivity(flash_intent)
            }
            R.id.nav_about -> {
                val about_intent = Intent(this, AboutActivity::class.java)
                startActivity(about_intent)
            }
            R.id.nav_changelog -> {
                val changeLog_intent = Intent(this, ChangeLogActivity::class.java)
                startActivity(changeLog_intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun alertUser() {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_NEGATIVE -> {
                    finish()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

        builder.setTitle("CodeX not found!")
                .setMessage("Sorry, this app is exclusively made to work with CodeX kernel")
                .setNegativeButton("Exit", dialogClickListener)
                .setCancelable(false)
                .show()
    }

    fun checkForUpdates(context: Context, isPopup: Boolean) {
        if (isNetworkAvailable()) {
            if (preferences.getBoolean(getString(R.string.key_wifi_only), false)) {
                if (isWifi()) {
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
                            val version = codexData.downloads.ver
                            verifyUpdate(version, context, isPopup)
                        }
                    })
                } else
                    alertUserForWifi()
            } else {
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
                        val version = codexData.downloads.ver
                        verifyUpdate(version, context, isPopup)
                    }
                })
            }
        } else
            promptNoNetwork(context)
    }


    fun verifyUpdate(version: String, context: Context, isPopup: Boolean) {
        val currentVersion = KERNEL_VERSION_FULL.substring(KERNEL_VERSION_FULL.lastIndexOf('-') + 1, KERNEL_VERSION_FULL.length)
        if (currentVersion != version) {
            runOnUiThread {
                val dialogListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            runOnUiThread {
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
                                        val url = codexData.downloads.url
                                        runOnUiThread {
                                            DownloadTask(context, url, false, isPopup, progressBar2, percentage_textView)
                                        }
                                    }
                                })

                            }
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            dialog.dismiss()
                        }
                    }
                }

                val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))
                builder.setTitle("Update Available")
                        .setMessage("Download update now?")
                        .setPositiveButton("Yes", dialogListener)
                        .setNegativeButton("Later", dialogListener)
                        .show()

            }
        } else {
            runOnUiThread {
                val mySnackbar = Snackbar.make(findViewById(R.id.home_coordinatorLayout), "No Update available", Snackbar.LENGTH_LONG)
                mySnackbar.view.setBackgroundColor(getColor(R.color.background))
                mySnackbar.show()
            }
        }
    }

    fun isWifi(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (mWifi.isConnected)
            return true
        return false
    }

    fun alertUserForWifi() {
        val dialogListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    dialog.dismiss()
                }
            }
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))
        builder.setMessage("You're not on Wi-Fi. Please change the settings of restrict mobile data")
                .setPositiveButton("Ok", dialogListener)
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

    fun isStoragePermissionGranted(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return true
        else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return false
        }
    }
}


