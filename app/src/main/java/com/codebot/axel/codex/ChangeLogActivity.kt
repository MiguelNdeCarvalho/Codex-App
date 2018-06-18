package com.codebot.axel.codex

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.ContextThemeWrapper
import android.view.View
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_change_log.*
import okhttp3.*
import java.io.IOException

class ChangeLogActivity : AppCompatActivity() {

    val URL = "https://raw.githubusercontent.com/AxelBlaz3/Codex-Kernel/gh-pages/whyred.json"
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_log)

        pref = getSharedPreferences(getString(R.string.key_custom_pref), Context.MODE_PRIVATE)
        setSupportActionBar(toolbar1)
        appBar1.bringToFront()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        changelog_progressBar.isIndeterminate = true
        changelog_progressBar.visibility = View.VISIBLE
        fetchJSON(this)
    }

    fun fetchJSON(context: Context) {
        val editor = pref.edit()
        if (!isNetworkAvailable()) {
            if (pref.getString(getString(R.string.changelog_json), "NA") == "NA")
                promptNoNetwork(context)
            else
                loadChangelogFromPreferences()
        } else {
            val client = OkHttpClient()

            val request = Request.Builder().url(URL).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    e!!.printStackTrace()
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val bodyOfJSON = response?.body()?.string()
                    editor.putString(getString(R.string.changelog_json), bodyOfJSON)
                    editor.apply()
                    val gson = GsonBuilder().create()
                    val codexData = gson.fromJson(bodyOfJSON, CodexInfo::class.java)
                    runOnUiThread {
                        changelog_progressBar.visibility = View.GONE
                        recyclerView.adapter = ChangeLogAdapter(codexData)
                        recyclerView.layoutManager = LinearLayoutManager(context)
                    }
//                    val stringBuilder = SpannableStringBuilder(codexData.changelog.get(0).version)
//                    stringBuilder.setSpan(StyleSpan(Typeface.BOLD), 0, stringBuilder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//                    println(isNetworkAvailable())
//                    for (i in 0..codexData.changelog.get(0).added.size - 2) {
//                        println(i)
//                        stringBuilder.append("\n")
//                        stringBuilder.append("• " + codexData.changelog.get(0).added[i])
//                    }
//                    println("outside for")
//                    if (isNetworkAvailable()) {
//                        if (pref.getString(getString(R.string.key_custom_pref), "changelog") != stringBuilder.toString()) {
//                            println("Inside if")
//                            val editor = pref.edit()
//                            editor.putString(getString(R.string.key_custom_pref), stringBuilder.toString())
//                            editor.apply()
//                        }
//                    }
//                    runOnUiThread {
//                        logTextView.text = pref.getString(getString(R.string.key_custom_pref), "changelog")
//                    }
//                }
                }
            })
        }
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

    fun loadChangelogFromPreferences() {
        val jsonBody = pref.getString(getString(R.string.changelog_json), "NA")
        val gson = GsonBuilder().create()
        val codexInfo = gson.fromJson(jsonBody, CodexInfo::class.java)
        changelog_progressBar.visibility = View.GONE
        recyclerView.adapter = ChangeLogAdapter(codexInfo)
        recyclerView.layoutManager = LinearLayoutManager(this)

    }
}
