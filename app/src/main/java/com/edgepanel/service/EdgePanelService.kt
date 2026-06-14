package com.edgepanel.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import com.edgepanel.R
import com.edgepanel.model.*
import com.edgepanel.ui.MainActivity
import com.edgepanel.utils.Prefs

class EdgePanelService : Service() {

    private lateinit var windowManager: WindowManager
    private var handleView: View? = null
    private var panelView: View? = null
    private var currentState = PanelState.HIDDEN

    private lateinit var handleConfig: HandleConfig
    private val CHANNEL_ID = "edge_panel_channel"
    private val NOTIF_ID = 1001

    // Panel dimensions
    private val PANEL_WIDTH_NORMAL = 320   // dp
    private val PANEL_WIDTH_MAX = 420
    private val PANEL_HEIGHT_NORMAL = 600
    private val PANEL_HEIGHT_MAX = 700

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        handleConfig = Prefs.loadHandleConfig(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        showHandle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELOAD_HANDLE -> {
                handleConfig = Prefs.loadHandleConfig(this)
                removeHandle()
                showHandle()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeHandle()
        removePanel()
        super.onDestroy()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Edge Panel Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Running edge panel overlay" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, EdgePanelService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edge Panel Active")
            .setContentText("Swipe from the edge to open")
            .setSmallIcon(R.drawable.ic_edge_panel)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    // ── Handle ────────────────────────────────────────────────────────────────

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showHandle() {
        val inflater = LayoutInflater.from(this)
        handleView = inflater.inflate(R.layout.view_handle, null)

        applyHandleStyle()

        val isRight = handleConfig.position == HandlePosition.RIGHT
        val gravity = if (isRight) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP

        val widthPx = dpToPx(lerp(24f, 36f, handleConfig.width)).toInt()
        val heightPx = dpToPx(lerp(48f, 80f, handleConfig.size)).toInt()

        val params = WindowManager.LayoutParams(
            widthPx, heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            y = handleConfig.offsetY
        }

        var initialY = 0
        var initialTouchY = 0f
        var isDragging = false
        var touchStartTime = 0L

        handleView!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    isDragging = false
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!handleConfig.isLocked && Math.abs(dy) > 10) {
                        isDragging = true
                        params.y = initialY + dy
                        try { windowManager.updateViewLayout(handleView, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - touchStartTime
                    if (!isDragging && elapsed < 300) {
                        // Tap — open panel
                        if (currentState == PanelState.HIDDEN) showPanel()
                        else hidePanel()
                    } else if (isDragging) {
                        handleConfig.offsetY = params.y
                        Prefs.saveHandleConfig(this, handleConfig)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(handleView, params)
    }

    private fun applyHandleStyle() {
        val view = handleView ?: return
        val color = Color.parseColor(handleConfig.colorHex)
        val alpha = (255 * (1f - handleConfig.transparency)).toInt().coerceIn(30, 255)
        view.background?.mutate()?.setTint(color)
        view.alpha = alpha / 255f
    }

    private fun removeHandle() {
        handleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            handleView = null
        }
    }

    // ── Panel ─────────────────────────────────────────────────────────────────

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showPanel() {
        if (panelView != null) return

        val inflater = LayoutInflater.from(this)
        panelView = inflater.inflate(R.layout.view_panel, null)

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            dpToPx(PANEL_WIDTH_NORMAL.toFloat()).toInt(),
            dpToPx(PANEL_HEIGHT_NORMAL.toFloat()).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - dpToPx(PANEL_WIDTH_NORMAL.toFloat()).toInt()) / 2
            y = (screenHeight - dpToPx(PANEL_HEIGHT_NORMAL.toFloat()).toInt()) / 2
        }

        currentState = PanelState.NORMAL

        setupPanelControls(params)
        setupDragging(params)
        setupPanelTabs()

        windowManager.addView(panelView, params)
        animatePanelIn()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragging(params: WindowManager.LayoutParams) {
        val titleBar = panelView?.findViewById<View>(R.id.panel_title_bar) ?: return
        var lastX = 0f; var lastY = 0f

        titleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    if (currentState != PanelState.FULLSCREEN) {
                        params.x += (event.rawX - lastX).toInt()
                        params.y += (event.rawY - lastY).toInt()
                        lastX = event.rawX; lastY = event.rawY
                        try { windowManager.updateViewLayout(panelView, params) } catch (_: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupPanelControls(params: WindowManager.LayoutParams) {
        val panel = panelView ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // ✕ Close button
        panel.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            hidePanel()
        }

        // ⛶ Fullscreen button
        panel.findViewById<View>(R.id.btn_fullscreen)?.setOnClickListener {
            if (currentState == PanelState.FULLSCREEN) {
                // Restore normal
                params.width = dpToPx(PANEL_WIDTH_NORMAL.toFloat()).toInt()
                params.height = dpToPx(PANEL_HEIGHT_NORMAL.toFloat()).toInt()
                params.x = (screenWidth - params.width) / 2
                params.y = (screenHeight - params.height) / 2
                params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                currentState = PanelState.NORMAL
                panel.findViewById<TextView>(R.id.btn_fullscreen)?.text = "⛶"
            } else {
                // Go fullscreen
                params.width = screenWidth
                params.height = screenHeight
                params.x = 0; params.y = 0
                params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                currentState = PanelState.FULLSCREEN
                panel.findViewById<TextView>(R.id.btn_fullscreen)?.text = "⊡"
            }
            try { windowManager.updateViewLayout(panelView, params) } catch (_: Exception) {}
        }

        // ⬆/⬇ Minimize/Maximize toggle button
        val btnMinMax = panel.findViewById<TextView>(R.id.btn_min_max)
        btnMinMax?.setOnClickListener {
            when (currentState) {
                PanelState.NORMAL, PanelState.MAXIMIZED -> {
                    // Minimize
                    params.width = dpToPx(PANEL_WIDTH_NORMAL.toFloat()).toInt()
                    params.height = dpToPx(80f).toInt()
                    currentState = PanelState.MINIMIZED
                    btnMinMax.text = "⬆"
                    panel.findViewById<View>(R.id.panel_content)?.visibility = View.GONE
                }
                PanelState.MINIMIZED -> {
                    // Maximize
                    params.width = dpToPx(PANEL_WIDTH_MAX.toFloat()).toInt()
                    params.height = dpToPx(PANEL_HEIGHT_MAX.toFloat()).toInt()
                    params.x = (screenWidth - params.width) / 2
                    params.y = (screenHeight - params.height) / 2
                    currentState = PanelState.MAXIMIZED
                    btnMinMax.text = "⬇"
                    panel.findViewById<View>(R.id.panel_content)?.visibility = View.VISIBLE
                }
                else -> {}
            }
            try { windowManager.updateViewLayout(panelView, params) } catch (_: Exception) {}
        }
    }

    private fun setupPanelTabs() {
        val panel = panelView ?: return
        val panels = Prefs.loadPanelConfigs(this).filter { it.isEnabled }.sortedBy { it.order }
        val tabLayout = panel.findViewById<LinearLayout>(R.id.tab_layout) ?: return
        val contentContainer = panel.findViewById<FrameLayout>(R.id.panel_content_container) ?: return

        tabLayout.removeAllViews()

        panels.forEachIndexed { index, config ->
            val tab = TextView(this).apply {
                text = config.title
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(dpToPx(12f).toInt(), dpToPx(8f).toInt(), dpToPx(12f).toInt(), dpToPx(8f).toInt())
                setOnClickListener { loadPanel(config, contentContainer) }
                tag = config.id
            }
            tabLayout.addView(tab)
        }

        // Load first panel
        if (panels.isNotEmpty()) loadPanel(panels[0], contentContainer)
    }

    private fun loadPanel(config: PanelConfig, container: FrameLayout) {
        container.removeAllViews()
        when (config.type) {
            PanelType.APPS -> loadAppsPanel(container)
            PanelType.TASKS -> loadTasksPanel(container)
            PanelType.PEOPLE -> loadPeoplePanel(container)
            PanelType.WEATHER -> loadWeatherPanel(container)
        }
    }

    // ── Apps Panel ────────────────────────────────────────────────────────────

    private fun loadAppsPanel(container: FrameLayout) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.panel_apps, container, false)
        val grid = view.findViewById<GridLayout>(R.id.apps_grid)
        val selectedPkgs = Prefs.loadSelectedApps(this)
        val pm = packageManager

        selectedPkgs.forEach { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val name = pm.getApplicationLabel(info).toString()
                val icon = pm.getApplicationIcon(info)

                val appView = LayoutInflater.from(this).inflate(R.layout.item_app_grid, null)
                appView.findViewById<ImageView>(R.id.app_icon)?.setImageDrawable(icon)
                appView.findViewById<TextView>(R.id.app_name)?.text = name
                appView.setOnClickListener {
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                            launchIntent?.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                                                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                            )
                                val options = android.app.ActivityOptions.makeFreeformAnimation()
                                    try {
                                                startActivity(launchIntent, options.toBundle())
                                    } catch (e: Exception) {
                                                startActivity(launchIntent)
                                    }
                                        hidePanel()
                }
                                    }
                                    }
                            )
                }
                val lp = GridLayout.LayoutParams().apply {
                    width = dpToPx(72f).toInt()
                    height = dpToPx(88f).toInt()
                    setMargins(dpToPx(4f).toInt(), dpToPx(4f).toInt(), dpToPx(4f).toInt(), dpToPx(4f).toInt())
                }
                grid.addView(appView, lp)
            } catch (_: Exception) {}
        }

        val editBtn = view.findViewById<TextView>(R.id.btn_edit_apps)
        editBtn?.setOnClickListener {
            val i = Intent(this, com.edgepanel.ui.AppsPanelEditActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        container.addView(view)
    }

    // ── Tasks Panel ───────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private fun loadTasksPanel(container: FrameLayout) {
        val view = LayoutInflater.from(this).inflate(R.layout.panel_tasks, container, false)
        val listView = view.findViewById<LinearLayout>(R.id.tasks_list)
        val input = view.findViewById<EditText>(R.id.task_input)
        val addBtn = view.findViewById<TextView>(R.id.btn_add_task)

        val tasks = Prefs.loadTasks(this)

        fun renderTasks() {
            listView.removeAllViews()
            tasks.forEach { task ->
                val row = LayoutInflater.from(this).inflate(R.layout.item_task, null)
                val cb = row.findViewById<CheckBox>(R.id.task_checkbox)
                cb.text = task.title
                cb.isChecked = task.isDone
                cb.setTextColor(Color.WHITE)
                cb.setOnCheckedChangeListener { _, checked ->
                    task.isDone = checked
                    Prefs.saveTasks(this, tasks)
                }
                val del = row.findViewById<TextView>(R.id.btn_delete_task)
                del.setOnClickListener {
                    tasks.remove(task)
                    Prefs.saveTasks(this, tasks)
                    renderTasks()
                }
                listView.addView(row)
            }
        }

        renderTasks()

        addBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                tasks.add(TaskItem(title = text))
                Prefs.saveTasks(this, tasks)
                input.setText("")
                renderTasks()
            }
        }

        container.addView(view)
    }

    // ── People Panel ──────────────────────────────────────────────────────────

    private fun loadPeoplePanel(container: FrameLayout) {
        val view = LayoutInflater.from(this).inflate(R.layout.panel_people, container, false)
        val list = view.findViewById<LinearLayout>(R.id.people_list)
        val contactIds = Prefs.loadSelectedContacts(this)

        contactIds.forEach { id ->
            val contact = resolveContact(id) ?: return@forEach
            val row = LayoutInflater.from(this).inflate(R.layout.item_person, null)
            row.findViewById<TextView>(R.id.person_name)?.text = contact.name
            row.findViewById<TextView>(R.id.person_phone)?.text = contact.phone

            if (contact.photoUri != null) {
                try {
                    row.findViewById<ImageView>(R.id.person_photo)
                        ?.setImageURI(android.net.Uri.parse(contact.photoUri))
                } catch (_: Exception) {}
            }

            row.findViewById<View>(R.id.btn_call)?.setOnClickListener {
                val i = Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:${contact.phone}"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
            row.findViewById<View>(R.id.btn_sms)?.setOnClickListener {
                val i = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:${contact.phone}"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
            list.addView(row)
        }

        view.findViewById<TextView>(R.id.btn_edit_people)?.setOnClickListener {
            val i = Intent(this, com.edgepanel.ui.PeoplePanelEditActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        container.addView(view)
    }

    private fun resolveContact(id: String): com.edgepanel.model.PersonItem? {
        return try {
            val uri = android.provider.ContactsContract.Contacts.CONTENT_URI
            val cursor = contentResolver.query(uri, null, "_id=?", arrayOf(id), null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
                    val photoUri = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.PHOTO_URI))
                    // Get phone
                    val phoneCursor = contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?", arrayOf(id), null
                    )
                    val phone = phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) pc.getString(pc.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)) else ""
                    } ?: ""
                    com.edgepanel.model.PersonItem(id, name, phone, photoUri)
                } else null
            }
        } catch (_: Exception) { null }
    }

    // ── Weather Panel ─────────────────────────────────────────────────────────

    private fun loadWeatherPanel(container: FrameLayout) {
        val view = LayoutInflater.from(this).inflate(R.layout.panel_weather, container, false)
        container.addView(view)
        // Weather fetching handled by WeatherFetcher util
        com.edgepanel.utils.WeatherFetcher.fetch(this) { data ->
            view.findViewById<TextView>(R.id.weather_temp)?.post {
                view.findViewById<TextView>(R.id.weather_temp)?.text = data.temp
                view.findViewById<TextView>(R.id.weather_desc)?.text = data.description
                view.findViewById<TextView>(R.id.weather_location)?.text = data.location
                view.findViewById<TextView>(R.id.weather_humidity)?.text = "Humidity: ${data.humidity}"
                view.findViewById<TextView>(R.id.weather_wind)?.text = "Wind: ${data.wind}"
            }
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private fun animatePanelIn() {
        panelView?.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun hidePanel() {
        panelView?.animate()?.alpha(0f)?.scaleX(0.85f)?.scaleY(0.85f)
            ?.setDuration(180)
            ?.withEndAction { removePanel() }
            ?.start()
    }

    private fun removePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            panelView = null
        }
        currentState = PanelState.HIDDEN
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Float) = dp * resources.displayMetrics.density

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    companion object {
        const val ACTION_RELOAD_HANDLE = "com.edgepanel.RELOAD_HANDLE"
        const val ACTION_STOP = "com.edgepanel.STOP"
    }
}
