package com.edgepanel.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edgepanel.R
import com.edgepanel.model.HandleConfig
import com.edgepanel.model.HandlePosition
import com.edgepanel.service.EdgePanelService
import com.edgepanel.utils.Prefs

class HandleSettingsActivity : AppCompatActivity() {

    private lateinit var config: HandleConfig

    private val colorOptions = listOf(
        "#F44336", "#FF9800", "#FFEB3B", "#4CAF50",
        "#3D5AFE", "#9C27B0", "#F48FB1", "#FFA000",
        "#212121", "#81D4FA", "#FFFFFF", "#FF6D00"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handle_settings)

        config = Prefs.loadHandleConfig(this)

        setupPositionToggle()
        setupLockSwitch()
        setupColorPicker()
        setupSliders()
        setupSaveButton()
    }

    private fun setupPositionToggle() {
        val radioGroup = findViewById<RadioGroup>(R.id.rg_position)
        val rbLeft = findViewById<RadioButton>(R.id.rb_left)
        val rbRight = findViewById<RadioButton>(R.id.rb_right)

        if (config.position == HandlePosition.LEFT) rbLeft.isChecked = true
        else rbRight.isChecked = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            config.position = if (checkedId == R.id.rb_left) HandlePosition.LEFT else HandlePosition.RIGHT
        }
    }

    private fun setupLockSwitch() {
        val sw = findViewById<Switch>(R.id.switch_lock)
        sw.isChecked = config.isLocked
        sw.setOnCheckedChangeListener { _, checked -> config.isLocked = checked }
    }

    private fun setupColorPicker() {
        val container = findViewById<LinearLayout>(R.id.color_grid)
        container.removeAllViews()

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        colorOptions.forEachIndexed { i, hex ->
            val dot = FrameLayout(this).apply {
                val size = resources.getDimensionPixelSize(R.dimen.color_dot_size)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(8, 8, 8, 8)
                }
                background = getDrawable(R.drawable.circle_shape)
                background.mutate().setTint(Color.parseColor(hex))
                if (hex == config.colorHex) {
                    addView(ImageView(this@HandleSettingsActivity).apply {
                        setImageResource(R.drawable.ic_check)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    })
                }
                setOnClickListener {
                    config.colorHex = hex
                    setupColorPicker() // refresh
                }
            }
            if (i < 6) row1.addView(dot) else row2.addView(dot)
        }

        container.addView(row1)
        container.addView(row2)
    }

    private fun setupSliders() {
        val seekTransparency = findViewById<SeekBar>(R.id.seek_transparency)
        val seekSize = findViewById<SeekBar>(R.id.seek_size)
        val seekWidth = findViewById<SeekBar>(R.id.seek_width)

        seekTransparency.progress = (config.transparency * 100).toInt()
        seekSize.progress = (config.size * 100).toInt()
        seekWidth.progress = (config.width * 100).toInt()

        seekTransparency.setOnSeekBarChangeListener(simpleChange { config.transparency = it / 100f })
        seekSize.setOnSeekBarChangeListener(simpleChange { config.size = it / 100f })
        seekWidth.setOnSeekBarChangeListener(simpleChange { config.width = it / 100f })
    }

    private fun simpleChange(onChanged: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { onChanged(p) }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }

    private fun setupSaveButton() {
        findViewById<Button>(R.id.btn_save_handle)?.setOnClickListener {
            Prefs.saveHandleConfig(this, config)
            // Notify service to reload
            val intent = Intent(this, EdgePanelService::class.java).apply {
                action = EdgePanelService.ACTION_RELOAD_HANDLE
            }
            startService(intent)
            Toast.makeText(this, "Handle settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
