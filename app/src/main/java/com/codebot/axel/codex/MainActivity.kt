package com.codebot.axel.codex

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
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
    val URL = "https://www.miguelndecarvalho.me/codex/whyred.json"
    lateinit var preferences: SharedPreferences
    lateinit var pref: SharedPreferences
    val context = this
    var flag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        appBar.bringToFront()

        flag = false
        pref = getSharedPreferences(getString(R.string.key_notification_check), Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString("notification", "0")
        editor.apply()

        progressBar2.visibility = View.INVISIBLE
        logo_imageView.scaleType = ImageView.ScaleType.FIT_XY

        if (KERNEL_NAME != KERNEL) {
            alertUser()
        } else {
            // Runtime.getRuntime().exec("su")
            preferences = PreferenceManager.getDefaultSharedPreferences(this)

            if(preferences.getBoolean(getString(R.string.key_miui_check), false))
                flag  = true
            Log.e("flag: ", "$flag")
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
                        val url: String
                        if(flag)
                            url = "Okay MIUI URL set!"
                        else
                            url = codexData.downloads.url
                        Log.d("Download URL: ", url)
                        runOnUiThread {
                            DownloadTask(context, url, autoUpdates, false, progressBar2, percentage_textView)
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
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://codexxda.miguelndecarvalho.me"))
                startActivity(browserIntent)
            }

            telegram_constraint_layout.setOnClickListener {
                val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://telegram.miguelndecarvalho.me"))
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
                            val url: String
                            if(flag)
                                url = "Okay MIUI URL set!"
                            else
                                url = codexData.downloads.url
                            runOnUiThread {
                                DownloadTask(context, url, false, false, progressBar2, percentage_textView)
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
        println("URL: " + URL)
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
        var currentVersion = KERNEL_VERSION_FULL.substring(KERNEL_VERSION_FULL.lastIndexOf('-') + 1, KERNEL_VERSION_FULL.length)
        // Remove 'v' from versions to compare!
        currentVersion = currentVersion.substring(1, currentVersion.length)
        Log.d("currentVersion: ", currentVersion)
        val remoteVersion = version.substring(1, version.length)
        Log.d("remoteVersion: ", remoteVersion)
        if (currentVersion.toDouble() < remoteVersion.toDouble()) {
            runOnUiThread {
                if (pref.getString("notification", "0").equals("0"))
                    updateNotification()
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
                                        val url: String
                                        if(flag)
                                            url = "Okay MIUI URL set!"
                                        else
                                            url = codexData.downloads.url
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

    fun updateNotification() {

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, "1")
        builder.setSmallIcon(R.mipmap.ic_notify)
        builder.setContentTitle("Kernel Update Available!")
        builder.setContentText("Tap here to open the app and download the update")
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        val notificationChannel = NotificationChannel("1", "name", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        notificationManager.notify(1, builder.build())

        val editor = pref.edit()
        editor.putString("notification", "1")
        editor.apply()
    }

    override fun onDestroy() {
        val editor = pref.edit()
        editor.putString("notification", "0")
        editor.apply()
        super.onDestroy()
    }
}


