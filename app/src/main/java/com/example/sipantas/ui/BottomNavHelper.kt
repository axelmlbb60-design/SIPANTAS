package com.example.sipantas.ui

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.sipantas.*

object BottomNavHelper {

    const val TAB_BERANDA = 0
    const val TAB_PENGAJUAN = 1
    const val TAB_KABAR = 2
    const val TAB_BANTUAN = 3

    fun setupBottomNav(activity: Activity, activeTab: Int) {
        val navBeranda = activity.findViewById<View>(R.id.nav_beranda) ?: return
        val navPengajuan = activity.findViewById<View>(R.id.nav_pengajuan) ?: return
        val navKabar = activity.findViewById<View>(R.id.nav_kabar) ?: return
        val navBantuan = activity.findViewById<View>(R.id.nav_bantuan) ?: return

        val ivBeranda = activity.findViewById<ImageView>(R.id.iv_nav_beranda)
        val tvBeranda = activity.findViewById<TextView>(R.id.tv_nav_beranda)
        val ivPengajuan = activity.findViewById<ImageView>(R.id.iv_nav_pengajuan)
        val tvPengajuan = activity.findViewById<TextView>(R.id.tv_nav_pengajuan)
        val ivKabar = activity.findViewById<ImageView>(R.id.iv_nav_kabar)
        val tvKabar = activity.findViewById<TextView>(R.id.tv_nav_kabar)
        val ivBantuan = activity.findViewById<ImageView>(R.id.iv_nav_bantuan)
        val tvBantuan = activity.findViewById<TextView>(R.id.tv_nav_bantuan)

        val activeColor = ContextCompat.getColor(activity, R.color.brand_primary)
        val mutedColor = ContextCompat.getColor(activity, R.color.text_muted)
        val textSecondary = ContextCompat.getColor(activity, R.color.text_secondary)

        // Reset all tabs
        ivBeranda?.setColorFilter(mutedColor)
        tvBeranda?.setTextColor(textSecondary)
        ivPengajuan?.setColorFilter(mutedColor)
        tvPengajuan?.setTextColor(textSecondary)
        ivKabar?.setColorFilter(mutedColor)
        tvKabar?.setTextColor(textSecondary)
        ivBantuan?.setColorFilter(mutedColor)
        tvBantuan?.setTextColor(textSecondary)

        // Highlight active tab
        when (activeTab) {
            TAB_BERANDA -> {
                ivBeranda?.setColorFilter(activeColor)
                tvBeranda?.setTextColor(activeColor)
            }
            TAB_PENGAJUAN -> {
                ivPengajuan?.setColorFilter(activeColor)
                tvPengajuan?.setTextColor(activeColor)
            }
            TAB_KABAR -> {
                ivKabar?.setColorFilter(activeColor)
                tvKabar?.setTextColor(activeColor)
            }
            TAB_BANTUAN -> {
                ivBantuan?.setColorFilter(activeColor)
                tvBantuan?.setTextColor(activeColor)
            }
        }

        navBeranda.setOnClickListener {
            if (activeTab != TAB_BERANDA) {
                val intent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navPengajuan.setOnClickListener {
            if (activeTab != TAB_PENGAJUAN) {
                val intent = Intent(activity, ServiceListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navKabar.setOnClickListener {
            if (activeTab != TAB_KABAR) {
                val intent = Intent(activity, NotificationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navBantuan.setOnClickListener {
            if (activeTab != TAB_BANTUAN) {
                val intent = Intent(activity, ProfileActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }
}
