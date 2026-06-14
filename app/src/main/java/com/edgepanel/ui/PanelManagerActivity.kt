package com.edgepanel.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edgepanel.R
import com.edgepanel.model.PanelConfig
import com.edgepanel.utils.Prefs

class PanelManagerActivity : AppCompatActivity() {

    private lateinit var panels: MutableList<PanelConfig>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_manager)

        panels = Prefs.loadPanelConfigs(this).toMutableList()
        renderPanels()
    }

    private fun renderPanels() {
        val container = findViewById<LinearLayout>(R.id.panels_container) ?: return
        container.removeAllViews()

        panels.sortedBy { it.order }.forEach { panel ->
            val row = layoutInflater.inflate(R.layout.item_panel_row, container, false)
            row.findViewById<TextView>(R.id.tv_panel_name)?.text = panel.title
            val sw = row.findViewById<Switch>(R.id.sw_panel_enabled)
            sw.isChecked = panel.isEnabled
            sw.setOnCheckedChangeListener { _, checked ->
                panel.isEnabled = checked
                Prefs.savePanelConfigs(this, panels)
            }
            container.addView(row)
        }
    }
}
