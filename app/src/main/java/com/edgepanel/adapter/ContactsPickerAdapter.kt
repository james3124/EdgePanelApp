package com.edgepanel.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.R
import com.edgepanel.model.PersonItem

class ContactsPickerAdapter(
    private val contacts: List<PersonItem>,
    private val selectedIds: MutableSet<String>,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsPickerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val photo: ImageView = v.findViewById(R.id.contact_photo)
        val name: TextView = v.findViewById(R.id.contact_name)
        val phone: TextView = v.findViewById(R.id.contact_phone)
        val check: CheckBox = v.findViewById(R.id.contact_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_picker, parent, false))

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val person = contacts[position]
        holder.name.text = person.name
        holder.phone.text = person.phone
        holder.check.isChecked = selectedIds.contains(person.contactId)

        if (person.photoUri != null) {
            try { holder.photo.setImageURI(Uri.parse(person.photoUri)) }
            catch (_: Exception) { holder.photo.setImageResource(R.drawable.ic_person_placeholder) }
        } else {
            holder.photo.setImageResource(R.drawable.ic_person_placeholder)
        }

        holder.itemView.setOnClickListener {
            val selected = !selectedIds.contains(person.contactId)
            if (selected) selectedIds.add(person.contactId) else selectedIds.remove(person.contactId)
            holder.check.isChecked = selected
            onToggle(person.contactId, selected)
        }
    }
}
