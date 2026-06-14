package com.edgepanel.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.edgepanel.R
import com.edgepanel.service.EdgePanelService
import com.edgepanel.utils.Prefs

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggleSwitch = findViewById<Switch>(R.id.switch_enable)
        val statusText = findViewById<TextView>(R.id.tv_status)
        val btnHandle = findViewById<Button>(R.id.btn_handle_settings)
        val btnPanels = findViewById<Button>(R.id.btn_panel_settings)
        val btnPermission = findViewById<Button>(R.id.btn_permission)

        // Reflect current state
        toggleSwitch.isChecked = Prefs.isServiceEnabled(this)
        updateStatus(statusText, toggleSwitch.isChecked)

        toggleSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (!Settings.canDrawOverlays(this)) {
                    toggleSwitch.isChecked = false
                    Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                    return@setOnCheckedChangeListener
                }
                Prefs.setServiceEnabled(this, true)
                startEdgeService()
            } else {
                Prefs.setServiceEnabled(this, false)
                stopService(Intent(this, EdgePanelService::class.java))
            }
            updateStatus(statusText, checked)
        }

        btnHandle.setOnClickListener {
            startActivity(Intent(this, HandleSettingsActivity::class.java))
        }

        btnPanels.setOnClickListener {
            startActivity(Intent(this, PanelManagerActivity::class.java))
        }

        btnPermission.setOnClickListener {
            requestOverlayPermission()
        }

        // Check permission indicator
        updatePermissionButton(btnPermission)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionButton(findViewById(R.id.btn_permission))
    }

    private fun startEdgeService() {
        val intent = Intent(this, EdgePanelService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun updateStatus(tv: TextView, enabled: Boolean) {
        tv.text = if (enabled) "✅ Edge Panel is Active" else "⭕ Edge Panel is Inactive"
        tv.setTextColor(if (enabled) 0xFF4CAF50.toInt() else 0xFFAAAAAA.toInt())
    }

    private fun updatePermissionButton(btn: Button) {
        if (Settings.canDrawOverlays(this)) {
            btn.text = "✅ Overlay Permission Granted"
            btn.isEnabled = false
        } else {
            btn.text = "⚠ Grant Overlay Permission"
            btn.isEnabled = true
        }
    }
}
