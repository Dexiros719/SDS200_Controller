package com.example.sds200_communicator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

class Sds200DisplayActivity : AppCompatActivity() {

    private val viewModel: Sds200ViewModel by viewModels()

    private lateinit var vTextView: TextView
    private lateinit var sTextView: TextView
    private lateinit var dateTimeTextView: TextView
    private lateinit var systemNameTextView: TextView
    private lateinit var deptNameTextView: TextView
    private lateinit var channelNameTextView: TextView
    private lateinit var infoAreaTextView: TextView
    private lateinit var freqTextView: TextView
    private lateinit var svcTypeTextView: TextView
    private lateinit var amTextView: TextView
    private lateinit var statusMessageTextView: TextView
    private lateinit var normalDisplayContainer: View
    private lateinit var drawerLayout: DrawerLayout

    private var timeUpdateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sds200_display)

        // Load settings
        val prefs = getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "192.168.0.91") ?: "192.168.0.91"
        val port = prefs.getInt("port", 50536)
        val enabled = prefs.getBoolean("enabled", true)

        // Initialize ViewModel
        viewModel.init(ip, port, enabled)

        // Init views
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val toolbar = findViewById<Toolbar>(R.id.top_app_bar)
        val toolbarContainer = findViewById<View>(R.id.toolbar_container)
        
        setSupportActionBar(toolbar)
        supportActionBar?.title = "SDS200 Controller"

        // Modern back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // Handle window insets correctly
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            
            val left = maxOf(systemBars.left, cutout.left)
            val top = maxOf(systemBars.top, cutout.top)
            val right = maxOf(systemBars.right, cutout.right)
            val bottom = systemBars.bottom

            // Apply padding to container
            toolbarContainer.setPadding(left, top, right, 0)
            
            findViewById<View>(R.id.display_screen)?.setPadding(left, 0, 0, 0)
            findViewById<View>(R.id.keyboard_layout)?.setPadding(0, 0, right, bottom)

            return@setOnApplyWindowInsetsListener insets
        }

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_main -> {}
                R.id.menu_connection -> {
                    startActivity(Intent(this, ConnectionSettingsActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        vTextView = findViewById(R.id.v_text_view)
        sTextView = findViewById(R.id.s_text_view)
        dateTimeTextView = findViewById(R.id.date_time_text_view)
        systemNameTextView = findViewById(R.id.system_name_text_view)
        deptNameTextView = findViewById(R.id.dept_name_text_view)
        channelNameTextView = findViewById(R.id.channel_name_text_view)
        infoAreaTextView = findViewById(R.id.info_area_text_view)
        freqTextView = findViewById(R.id.freq_text_view)
        svcTypeTextView = findViewById(R.id.svc_type_text_view)
        amTextView = findViewById(R.id.am_text_view)
        statusMessageTextView = findViewById(R.id.status_message_text_view)
        normalDisplayContainer = findViewById(R.id.normal_display_container)

        initKeypad()

        viewModel.onStatusUpdated = { _ ->
            runOnUiThread { updateUi() }
        }

        viewModel.controller.onButtonCompleted = { success ->
            runOnUiThread {
                if (!success) {
                    Toast.makeText(this, "Action failed (check connection)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateUi()
    }

    private fun initKeypad() {
        findViewById<TextView>(R.id.button_system).setOnClickListener { viewModel.controller.sendButton("S") }
        findViewById<TextView>(R.id.button_dept).setOnClickListener { viewModel.controller.sendButton("D") }
        findViewById<TextView>(R.id.button_channel).setOnClickListener { viewModel.controller.sendButton("C") }

        findViewById<View>(R.id.button_0).setOnClickListener { viewModel.controller.sendButton("0") }
        findViewById<View>(R.id.button_1).setOnClickListener { viewModel.controller.sendButton("1") }
        findViewById<View>(R.id.button_2).setOnClickListener { viewModel.controller.sendButton("2") }
        findViewById<View>(R.id.button_3).setOnClickListener { viewModel.controller.sendButton("3") }
        findViewById<View>(R.id.button_4).setOnClickListener { viewModel.controller.sendButton("4") }
        findViewById<View>(R.id.button_5).setOnClickListener { viewModel.controller.sendButton("5") }
        findViewById<View>(R.id.button_6).setOnClickListener { viewModel.controller.sendButton("6") }
        findViewById<View>(R.id.button_7).setOnClickListener { viewModel.controller.sendButton("7") }
        findViewById<View>(R.id.button_8).setOnClickListener { viewModel.controller.sendButton("8") }
        findViewById<View>(R.id.button_9).setOnClickListener { viewModel.controller.sendButton("9") }
        findViewById<View>(R.id.button_dot).setOnClickListener { viewModel.controller.sendButton(".") }
        findViewById<View>(R.id.button_e).setOnClickListener { viewModel.controller.sendButton("E yes") }

        findViewById<Button>(R.id.button_zip).setOnClickListener { viewModel.controller.sendButton("Zip") }
        findViewById<Button>(R.id.button_replay).setOnClickListener { viewModel.controller.sendButton("REPLAY") }
        findViewById<Button>(R.id.button_avoid).setOnClickListener { viewModel.controller.sendButton("AVOID") }
        findViewById<Button>(R.id.button_rot).setOnClickListener { viewModel.controller.sendButton("ROT") }

        findViewById<Button>(R.id.button_func).setOnClickListener { viewModel.controller.sendButton("Func") }
        findViewById<Button>(R.id.button_menu).setOnClickListener { viewModel.controller.sendButton("Menu") }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("settings_changed", false)) {
            val ip = prefs.getString("ip", "192.168.0.91") ?: "192.168.0.91"
            val port = prefs.getInt("port", 50536)
            val enabled = prefs.getBoolean("enabled", true)
            viewModel.communicator.updateSettings(ip, port, enabled)
            prefs.edit { putBoolean("settings_changed", false) }
            if (!enabled) viewModel.isLastStatusSuccess = false
            updateUi()
        }
        startTimeUpdates()
    }

    private fun startTimeUpdates() {
        timeUpdateJob?.cancel()
        timeUpdateJob = lifecycleScope.launch {
            while (isActive) {
                updateDateTime()
                delay(1000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        timeUpdateJob?.cancel()
    }

    private fun updateUi() {
        val prefs = getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", true)

        if (!enabled) {
            statusMessageTextView.text = "Connection Disabled"
            statusMessageTextView.visibility = View.VISIBLE
            normalDisplayContainer.visibility = View.GONE
            vTextView.text = "V:---"
            sTextView.text = "S:---"
            return
        }

        if (!viewModel.isLastStatusSuccess) {
            statusMessageTextView.text = "Scanner Unavailable"
            statusMessageTextView.visibility = View.VISIBLE
            normalDisplayContainer.visibility = View.GONE
            vTextView.text = "V:---"
            sTextView.text = "S:---"
            return
        }

        statusMessageTextView.visibility = View.GONE
        normalDisplayContainer.visibility = View.VISIBLE

        val status = viewModel.repository.getStatus() ?: return
        val convFrequency = status.convFrequencies.firstOrNull()

        vTextView.text = "V:${status.vScreenDisplay}"
        sTextView.text = "S:${status.sLevel}"

        systemNameTextView.text = status.systems.firstOrNull()?.name ?: "---"
        deptNameTextView.text = status.departments.firstOrNull()?.name ?: "---"
        channelNameTextView.text = convFrequency?.name ?: "---"

        freqTextView.text = convFrequency?.freq?.let { "${it}MHz" } ?: "---"
        svcTypeTextView.text = convFrequency?.svcType ?: "---"
        amTextView.text = convFrequency?.mod ?: "---"

        if (convFrequency?.hold == true) {
            infoAreaTextView.text = convFrequency.name
        } else {
            infoAreaTextView.text = status.viewTexts.info1
        }
    }

    private fun updateDateTime() {
        val sdf = SimpleDateFormat("MMMdd HH:mm", Locale.US)
        val currentDate = sdf.format(Date())
        dateTimeTextView.text = currentDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeUpdateJob?.cancel()
        viewModel.onStatusUpdated = null
    }
}
