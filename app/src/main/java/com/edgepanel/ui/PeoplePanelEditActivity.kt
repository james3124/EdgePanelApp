package com.edgepanel.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.R
import com.edgepanel.adapter.ContactsPickerAdapter
import com.edgepanel.model.PersonItem
import com.edgepanel.utils.Prefs

class PeoplePanelEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people_edit)

        val recycler = findViewById<RecyclerView>(R.id.rv_contacts)
        val btnSave = findViewById<Button>(R.id.btn_save_contacts)
        val selectedIds = Prefs.loadSelectedContacts(this).toMutableSet()
        val contacts = mutableListOf<PersonItem>()

        // Load contacts
        Thread {
            val cursor = contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                android.provider.ContactsContract.Contacts.DISPLAY_NAME
            )
            cursor?.use {
                val seen = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                    if (seen.contains(id)) continue
                    seen.add(id)
                    val name = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: continue
                    val phone = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                    val photo = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                    contacts.add(PersonItem(id, name, phone, photo))
                }
            }

            runOnUiThread {
                val adapter = ContactsPickerAdapter(contacts, selectedIds) { id, selected ->
                    if (selected) selectedIds.add(id) else selectedIds.remove(id)
                }
                recycler.layoutManager = LinearLayoutManager(this)
                recycler.adapter = adapter
            }
        }.start()

        btnSave.setOnClickListener {
            Prefs.saveSelectedContacts(this, selectedIds.toList())
            Toast.makeText(this, "Contacts saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
