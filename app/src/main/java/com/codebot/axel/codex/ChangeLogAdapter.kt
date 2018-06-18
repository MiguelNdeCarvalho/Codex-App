package com.codebot.axel.codex

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_change_log_items.view.*

class ChangeLogAdapter(codexInfo: CodexInfo) : RecyclerView.Adapter<ChangeLogViewHolder>() {

    var changeLog: ArrayList<String> = ArrayList()
    val sizeOfChangelog = codexInfo.changelog.size

    init {
        for (i in 0..sizeOfChangelog - 1) {
            val sizeOfArray = codexInfo.changelog.get(i).added.size
            val currentChangelog = codexInfo.changelog.get(i).added
            for (j in 0..sizeOfArray - 1) {
                changeLog.add(currentChangelog[j])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangeLogViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val currentRow = layoutInflater.inflate(R.layout.activity_change_log_items, parent, false)
        return ChangeLogViewHolder(currentRow)
    }

    override fun onBindViewHolder(holder: ChangeLogViewHolder, position: Int) {
        if (changeLog[position].contains("Version", false)) {
            val string = Html.fromHtml("<h2>" + "<font color=#109B98>" + changeLog[position] + "</font></h2>").trim()
            holder.view.changelog_bullet_textView.visibility = View.GONE
            holder.view.changelog_added_textView.text = string
        } else
            holder.view.changelog_added_textView.text = changeLog[position]

    }

    override fun getItemCount(): Int {
        return changeLog.size
    }

}

class ChangeLogViewHolder(val view: View) : RecyclerView.ViewHolder(view)