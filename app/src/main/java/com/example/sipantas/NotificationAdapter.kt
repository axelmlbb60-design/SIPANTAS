package com.example.sipantas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantas.data.NotificationItem

class NotificationAdapter(
    private var list: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    fun updateData(newList: List<NotificationItem>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_notif_icon)
        private val dotUnread: View = itemView.findViewById(R.id.dot_unread)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_notif_title)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_notif_time)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_notif_message)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_notif_category_chip)

        fun bind(item: NotificationItem) {
            tvTitle.text = item.judul
            tvMessage.text = item.pesan
            tvTime.text = item.waktuLalu ?: "Baru saja"

            dotUnread.visibility = if (item.isRead == 0) View.VISIBLE else View.GONE

            val (iconRes, categoryText) = when (item.tipe?.lowercase()) {
                "surat" -> Pair(R.drawable.ic_document, "Surat")
                "antrean" -> Pair(R.drawable.ic_today, "Antrean")
                "laporan" -> Pair(R.drawable.ic_help_support, "Laporan")
                else -> Pair(R.drawable.ic_notifications, "Informasi")
            }

            ivIcon.setImageResource(iconRes)
            tvCategory.text = categoryText

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
