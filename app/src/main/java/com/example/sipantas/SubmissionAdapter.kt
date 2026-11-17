package com.example.sipantas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantas.data.PengajuanItem

class SubmissionAdapter(
    private var items: List<PengajuanItem>,
    private val onItemClick: (PengajuanItem) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

    fun updateData(newItems: List<PengajuanItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvJenisSurat: TextView = itemView.findViewById(R.id.tv_jenis_surat)
        private val tvNomorSurat: TextView = itemView.findViewById(R.id.tv_nomor_surat)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tv_status_badge)
        private val tvPemohon: TextView = itemView.findViewById(R.id.tv_pemohon)
        private val tvTanggal: TextView = itemView.findViewById(R.id.tv_tanggal)

        fun bind(item: PengajuanItem, onItemClick: (PengajuanItem) -> Unit) {
            val context = itemView.context

            tvJenisSurat.text = item.jenisSurat
            tvNomorSurat.text = "Nomor: ${item.nomorSurat}"
            val pemohonName = item.namaLengkap ?: "Warga"
            val rtRw = if (!item.nomorRt.isNullOrBlank() && !item.nomorRw.isNullOrBlank()) " · RT ${item.nomorRt}/RW ${item.nomorRw}" else ""
            tvPemohon.text = "Pemohon: $pemohonName$rtRw"
            tvTanggal.text = item.createdAt.take(10)

            when (item.status) {
                "menunggu_rt" -> {
                    tvStatusBadge.text = "⏳ Menunggu TTD RT"
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                }
                "disetujui_rt", "menunggu_kecamatan" -> {
                    tvStatusBadge.text = "✍ Menunggu Kelurahan"
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                }
                "selesai" -> {
                    tvStatusBadge.text = "✓ Selesai & Terverifikasi"
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_success))
                }
                "ditolak_rt", "ditolak_kecamatan" -> {
                    tvStatusBadge.text = "✗ Ditolak"
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                }
                else -> {
                    tvStatusBadge.text = item.status
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
