package com.example.sds200_communicator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class ConnectionSettingsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_settings)

        val prefs = getSharedPreferences("connection_prefs", Context.MODE_PRIVATE)
        
        // Init navigation
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val toolbar = findViewById<Toolbar>(R.id.top_app_bar)
        val toolbarContainer = findViewById<View>(R.id.toolbar_container)
        
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Connection settings"

        // Base padding
        val basePadding = (16 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            
            val leftInset = maxOf(systemBars.left, cutout.left)
            val topInset = maxOf(systemBars.top, cutout.top)
            val rightInset = maxOf(systemBars.right, cutout.right)
            val bottomInset = systemBars.bottom

            toolbarContainer.setPadding(leftInset, topInset, rightInset, 0)

            val finalLeft = maxOf(leftInset, basePadding)
            val finalRight = maxOf(rightInset, basePadding)
            findViewById<View>(R.id.settings_container)?.setPadding(finalLeft, basePadding, finalRight, bottomInset + basePadding)

            return@setOnApplyWindowInsetsListener insets
        }

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_main -> {
                    startActivity(Intent(this, Sds200DisplayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                }
                R.id.menu_connection -> { }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val switchEnabled = findViewById<SwitchCompat>(R.id.switch_connection_enabled)
        val editIp = findViewById<EditText>(R.id.edit_ip_address)
        val editPort = findViewById<EditText>(R.id.edit_port)
        val buttonSave = findViewById<Button>(R.id.button_save)

        switchEnabled.isChecked = prefs.getBoolean("enabled", true)
        editIp.setText(prefs.getString("ip", "192.168.0.91"))
        editPort.setText(prefs.getInt("port", 50536).toString())

        buttonSave.setOnClickListener {
            val ip = editIp.text.toString()
            val port = editPort.text.toString().toIntOrNull() ?: 50536
            val enabled = switchEnabled.isChecked

            prefs.edit().apply {
                putString("ip", ip)
                putInt("port", port)
                putBoolean("enabled", enabled)
                putBoolean("settings_changed", true)
                apply()
            }
            finish()
        }

        // Modern back press handler
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
    }
}
