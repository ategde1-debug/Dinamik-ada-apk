package com.dinamikada

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val colors = intArrayOf(0xFF39FF14.toInt(), 0xFF4CC9FF.toInt(), 0xFF1FF4D0.toInt(), 0xFF3D7BFF.toInt(), 0xFFA855FF.toInt(), 0xFFFF4FD8.toInt(),
        0xFFFF3B4E.toInt(), 0xFFFF8A1F.toInt(), 0xFFFFE426.toInt(), 0xFFB6FF00.toInt(), 0xFFFF7AB8.toInt(), 0xFFF2F2F7.toInt())

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t; v.textSize = size; v.setTextColor(color)
        if (bold) v.setTypeface(v.typeface, android.graphics.Typeface.BOLD)
        v.setPadding(0, dp(6), 0, dp(6))
        return v
    }

    private fun btn(t: String, accent: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = t; b.isAllCaps = false; b.setTextColor(if (accent) Color.BLACK else Color.WHITE)
        val g = GradientDrawable()
        g.cornerRadius = dp(16).toFloat()
        g.setColor(if (accent) Color.WHITE else 0x33FFFFFF)
        g.setStroke(dp(1), 0x44FFFFFF)
        b.background = g
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50))
        lp.topMargin = dp(8)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(24), dp(48), dp(24), dp(24))
        root.setBackgroundColor(0xFF0B0B0E.toInt())
        root.addView(label("Dinamik Ada", 28f, Color.WHITE, true))
        root.addView(label("Telefonun üstünde çentik gibi duran küçük ada: saat, müzik kontrolü ve gelen bildirimler.", 14f, 0xFF9A9AA2.toInt()))
        status = label("", 13f, 0xFF4CC9FF.toInt())
        root.addView(status)
        root.addView(btn("1) Diğer uygulamaların üstünde göster izni", false) {
            if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("2) Bildirim erişimi (müzik ve bildirimler)", false) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        root.addView(btn("3) Bildirim izni (Android 13+)", false) {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        root.addView(btn("Adayı başlat", true) {
            if (!Settings.canDrawOverlays(this)) Toast.makeText(this, "Önce 1. izni ver", Toast.LENGTH_LONG).show()
            else { startForegroundService(Intent(this, IslandService::class.java)); status.postDelayed({ update() }, 500) }
        })
        root.addView(btn("Adayı durdur", false) { stopService(Intent(this, IslandService::class.java)); status.postDelayed({ update() }, 500) })
        root.addView(label("Ada rengi (müzik çubukları)", 14f, Color.WHITE, true))
        val prefs = getSharedPreferences("p", 0)
        for (row in 0 until 2) {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER
            for (i in 0 until 6) {
                val c = colors[row * 6 + i]
                val dot = View(this)
                val g = GradientDrawable()
                g.shape = GradientDrawable.OVAL
                g.setColor(c)
                dot.background = g
                val lp = LinearLayout.LayoutParams(dp(38), dp(38))
                lp.setMargins(dp(6), dp(6), dp(6), dp(6))
                dot.layoutParams = lp
                dot.setOnClickListener { prefs.edit().putInt("accent", c).apply() }
                r.addView(dot)
            }
            root.addView(r)
        }
        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }

    override fun onResume() { super.onResume(); update() }

    private fun update() {
        val ov = Settings.canDrawOverlays(this)
        val na = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val on = IslandService.instance != null
        status.text = "Üstte gösterme izni: ${if (ov) "✓" else "✗"}\nBildirim erişimi: ${if (na) "✓" else "✗"}\nAda: ${if (on) "çalışıyor" else "kapalı"}"
    }
}
