package com.dinamikada

import android.animation.LayoutTransition
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BarsView(c: Context) : View(c) {
    var playing = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        p.color = context.getSharedPreferences("p", 0).getInt("accent", 0xFF4CC9FF.toInt())
        val n = 4
        val bw = width / (n * 2f - 1f)
        val t = System.currentTimeMillis() / 180.0
        for (i in 0 until n) {
            val h = height * (0.3f + 0.7f * Math.abs(Math.sin(t + i * 1.3)).toFloat())
            val x = i * bw * 2f
            cv.drawRoundRect(x, (height - h) / 2f, x + bw, (height + h) / 2f, bw / 2f, bw / 2f, p)
        }
        if (playing) postInvalidateOnAnimation()
    }
}

class IslandService : Service() {
    companion object { @Volatile var instance: IslandService? = null }

    private val h = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var card: LinearLayout
    private lateinit var clock: TextView
    private lateinit var bars: BarsView
    private lateinit var panel: LinearLayout
    private lateinit var mediaBox: LinearLayout
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var playBtn: ImageButton
    private lateinit var notifTv: TextView
    private var ctrl: MediaController? = null
    private var msm: MediaSessionManager? = null
    private var added = false
    private val collapse = Runnable { setOpen(false) }
    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { refresh() }
        override fun onPlaybackStateChanged(state: PlaybackState?) { refresh() }
    }
    private val tick = object : Runnable {
        override fun run() {
            clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (ctrl == null) initMedia()
            h.postDelayed(this, 15000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        startFg()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        build()
        tick.run()
    }

    private fun startFg() {
        val ch = NotificationChannel("island", "Dinamik Ada", NotificationManager.IMPORTANCE_MIN)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        val n = Notification.Builder(this, "island").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Dinamik Ada çalışıyor").build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
    }

    private fun ib(res: Int, onClick: () -> Unit): ImageButton {
        val b = ImageButton(this)
        b.setImageResource(res)
        b.setColorFilter(Color.WHITE)
        b.setBackgroundColor(Color.TRANSPARENT)
        b.layoutParams = LinearLayout.LayoutParams(dp(56), dp(44))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun build() {
        val bg = GradientDrawable()
        bg.setColor(Color.BLACK)
        bg.cornerRadius = dp(22).toFloat()
        card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = bg
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(18), dp(7), dp(18), dp(7))
        card.minimumWidth = dp(130)
        card.layoutTransition = LayoutTransition()

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        bars = BarsView(this)
        bars.visibility = View.GONE
        val bp = LinearLayout.LayoutParams(dp(18), dp(14))
        bp.rightMargin = dp(8)
        top.addView(bars, bp)
        clock = TextView(this)
        clock.setTextColor(Color.WHITE)
        clock.textSize = 14f
        clock.setTypeface(clock.typeface, android.graphics.Typeface.BOLD)
        top.addView(clock)
        card.addView(top)

        panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.gravity = Gravity.CENTER_HORIZONTAL
        panel.visibility = View.GONE
        mediaBox = LinearLayout(this)
        mediaBox.orientation = LinearLayout.VERTICAL
        mediaBox.gravity = Gravity.CENTER_HORIZONTAL
        title = TextView(this)
        title.setTextColor(Color.WHITE); title.textSize = 15f; title.maxLines = 1; title.ellipsize = TextUtils.TruncateAt.END
        title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
        title.maxWidth = dp(260)
        artist = TextView(this)
        artist.setTextColor(0xFF9A9AA2.toInt()); artist.textSize = 12f; artist.maxLines = 1; artist.ellipsize = TextUtils.TruncateAt.END
        artist.maxWidth = dp(260)
        val ctl = LinearLayout(this)
        ctl.orientation = LinearLayout.HORIZONTAL
        ctl.gravity = Gravity.CENTER
        playBtn = ib(android.R.drawable.ic_media_play) { ctrl?.let { c -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play() } }
        ctl.addView(ib(android.R.drawable.ic_media_previous) { ctrl?.transportControls?.skipToPrevious() })
        ctl.addView(playBtn)
        ctl.addView(ib(android.R.drawable.ic_media_next) { ctrl?.transportControls?.skipToNext() })
        mediaBox.addView(title); mediaBox.addView(artist); mediaBox.addView(ctl)
        notifTv = TextView(this)
        notifTv.setTextColor(Color.WHITE); notifTv.textSize = 13f; notifTv.maxWidth = dp(280); notifTv.visibility = View.GONE
        panel.addView(mediaBox); panel.addView(notifTv)
        card.addView(panel)

        card.setOnClickListener {
            if (panel.visibility == View.GONE) {
                mediaBox.visibility = View.VISIBLE; notifTv.visibility = View.GONE
                setOpen(true); h.removeCallbacks(collapse); h.postDelayed(collapse, 8000)
            } else setOpen(false)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = dp(6)
        wm.addView(card, lp)
        added = true
        refresh()
    }

    private fun setOpen(o: Boolean) { panel.visibility = if (o) View.VISIBLE else View.GONE }

    private fun refresh() {
        val playing = ctrl?.playbackState?.state == PlaybackState.STATE_PLAYING
        val md = ctrl?.metadata
        title.text = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Çalan müzik yok"
        artist.text = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        playBtn.setImageResource(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        bars.visibility = if (playing) View.VISIBLE else View.GONE
        bars.playing = playing
        if (playing) bars.invalidate()
    }

    private fun pick(l: List<MediaController>?) {
        ctrl?.unregisterCallback(cb)
        ctrl = l?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: l?.firstOrNull()
        ctrl?.registerCallback(cb, h)
        refresh()
    }

    private fun initMedia() {
        try {
            val m = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, MediaListener::class.java)
            if (msm == null) {
                msm = m
                m.addOnActiveSessionsChangedListener({ l -> pick(l) }, cn, h)
            }
            pick(m.getActiveSessions(cn))
        } catch (e: SecurityException) {
            // bildirim erişimi henüz verilmedi
        }
    }

    fun onNotif(sbn: StatusBarNotification) {
        val n = sbn.notification
        if (sbn.packageName == packageName || sbn.isOngoing || (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0 || n.category == Notification.CATEGORY_TRANSPORT) return
        val ex = n.extras
        val t = ex.getCharSequence(Notification.EXTRA_TITLE) ?: return
        val x = ex.getCharSequence(Notification.EXTRA_TEXT) ?: ""
        val app = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() } catch (e: Exception) { sbn.packageName }
        h.post {
            if (!added) return@post
            notifTv.text = "$app\n$t\n$x"
            mediaBox.visibility = View.GONE; notifTv.visibility = View.VISIBLE
            setOpen(true)
            h.removeCallbacks(collapse); h.postDelayed(collapse, 5000)
        }
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        ctrl?.unregisterCallback(cb)
        if (added) try { wm.removeView(card) } catch (e: Exception) { }
        instance = null
        super.onDestroy()
    }
}
