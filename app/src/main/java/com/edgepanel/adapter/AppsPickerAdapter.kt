package com.edgepanel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.R
import com.edgepanel.model.AppItem

class AppsPickerAdapter(
    private var apps: MutableList<AppItem>,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppsPickerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.item_app_icon)
        val name: TextView = v.findViewById(R.id.item_app_name)
        val check: CheckBox = v.findViewById(R.id.item_app_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app_picker, parent, false))

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.appName
        holder.check.isChecked = app.isSelected
        holder.itemView.setOnClickListener {
            app.isSelected = !app.isSelected
            holder.check.isChecked = app.isSelected
            onToggle(app.packageName, app.isSelected)
        }
    }

    fun updateList(newList: MutableList<AppItem>) {
        apps = newList
        notifyDataSetChanged()
    }
}
