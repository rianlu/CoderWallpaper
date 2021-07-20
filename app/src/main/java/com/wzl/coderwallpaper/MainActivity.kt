package com.wzl.coderwallpaper

import android.app.AppOpsManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var sp: SharedPreferences
    private lateinit var displayClockSwitch: SwitchMaterial
    private lateinit var displaySecondSwitch: SwitchMaterial
    private lateinit var autoDarkSwitch: SwitchMaterial
    private lateinit var jikeSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp = getSharedPreferences("coder_wallpaper_settings", Context.MODE_PRIVATE)

        displayClockSwitch = findViewById(R.id.displayClockSwitch)
        displaySecondSwitch = findViewById(R.id.displaySecondSwitch)
        autoDarkSwitch = findViewById(R.id.autoDarkSwitch)
        jikeSwitch = findViewById(R.id.jikeSwitch)

        displayClockSwitch.isChecked = sp.getBoolean("display_clock", true)
        displaySecondSwitch.isChecked = sp.getBoolean("display_second", true)
        autoDarkSwitch.isChecked = sp.getBoolean("auto_dark", true)
        jikeSwitch.isChecked = sp.getBoolean("jike_1024", true)

        autoDarkSwitch.text =
            getString(
                if (sp.getBoolean("always_dark", false)) {
                    R.string.always_dark
                } else {
                    R.string.auto_dark
                }
            )
        findViewById<ExtendedFloatingActionButton>(R.id.settingWallpaperBtn).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, MainWallpaperService::class.java)
            )
            startActivity(intent)
        }

        displayClockSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            changeClockDisplay(isChecked)
        }

        displaySecondSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            changeSecondDisplay(isChecked)
        }

        autoDarkSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            changeAutoDark(isChecked)
        }
        autoDarkSwitch.setOnLongClickListener {
            changeDarkType()
            true
        }
        jikeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (checkAccessPermission()) {
                changeJikeUseStats(isChecked)
            } else {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        findViewById<AppCompatImageView>(R.id.appResourceInfoView).setOnClickListener {
            val uri: Uri = Uri.parse("https://dribbble.com/kunchevsky")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    private fun changeClockDisplay(isChecked: Boolean) {
        val editor = sp.edit()
        editor.putBoolean("display_clock", isChecked)
        editor.apply()
        if (!isChecked) {
            changeSecondDisplay(false)
            displaySecondSwitch.isEnabled = false
        } else {
            displaySecondSwitch.isEnabled = true
        }
    }

    private fun changeSecondDisplay(isChecked: Boolean) {
        val editor = sp.edit()
        editor.putBoolean("display_second", isChecked)
        editor.apply()
    }

    private fun changeAutoDark(isChecked: Boolean) {
        val editor = sp.edit()
        editor.putBoolean("auto_dark", isChecked)
        editor.apply()
    }

    private fun changeJikeUseStats(isChecked: Boolean) {
        val editor = sp.edit()
        editor.putBoolean("jike_1024", isChecked)
        editor.apply()
    }

    private fun checkAccessPermission(): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName
                )
            } else {
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 切换自动深色/总是深色
     */
    private fun changeDarkType() {
        val alwaysDark = sp.getBoolean("always_dark", false)
        val editor = sp.edit()
        editor.putBoolean("always_dark", !alwaysDark)
        editor.apply()
        autoDarkSwitch.text = getString(
            if (!alwaysDark) {
                R.string.always_dark
            } else {
                R.string.auto_dark
            }
        )
    }
}