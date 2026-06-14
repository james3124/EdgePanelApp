package com.edgepanel.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.R
import com.edgepanel.adapter.AppsPickerAdapter
import com.edgepanel.model.AppItem
import com.edgepanel.utils.Prefs

class AppsPanelEditActivity : AppCompatActivity() {

    private lateinit var adapter: AppsPickerAdapter
    private var allApps = listOf<AppItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps_edit)

        val recycler = findViewById<RecyclerView>(R.id.rv_apps)
        val search = findViewById<EditText>(R.id.et_search_apps)
        val btnSave = findViewById<Button>(R.id.btn_save_apps)

        val selectedPkgs = Prefs.loadSelectedApps(this).toMutableSet()

        // Load installed apps in background
        Thread {
            val pm = packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .map { ri ->
                    AppItem(
                        packageName = ri.activityInfo.packageName,
                        appName = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm),
                        isSelected = selectedPkgs.contains(ri.activityInfo.packageName)
                    )
                }
                .sortedBy { it.appName }

            allApps = apps
            runOnUiThread {
                adapter = AppsPickerAdapter(apps.toMutableList()) { pkg, selected ->
                    if (selected) selectedPkgs.add(pkg) else selectedPkgs.remove(pkg)
                }
                recycler.layoutManager = LinearLayoutManager(this)
                recycler.adapter = adapter
            }
        }.start()

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = allApps.filter { it.appName.lowercase().contains(query) }
                if (::adapter.isInitialized) adapter.updateList(filtered.toMutableList())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSave.setOnClickListener {
            Prefs.saveSelectedApps(this, selectedPkgs.toList())
            Toast.makeText(this, "Apps saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
