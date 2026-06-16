// file: com/example/bookclub/ui/inbox/InboxAdapter.kt
package com.example.bookclub.ui.inbox

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.bookclub.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class InboxAdapter(
    private val onClick: (InboxUi) -> Unit,
    private val onDeleteClick: (InboxUi) -> Unit
) : ListAdapter<InboxUi, InboxAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<InboxUi>() {
        override fun areItemsTheSame(a: InboxUi, b: InboxUi) = a.id == b.id
        override fun areContentsTheSame(a: InboxUi, b: InboxUi) = a == b
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val img: ImageView = view.findViewById(R.id.imgCover)
        private val message: TextView = view.findViewById(R.id.txtMessage)
        private val title: TextView = view.findViewById(R.id.txtTitle)
        private val whenTxt: TextView = view.findViewById(R.id.txtWhen)
        private val dot: View = view.findViewById(R.id.unreadDot)
        private val deleteButton: ImageButton = view.findViewById(R.id.btnDeleteNotification)

        fun bind(item: InboxUi) {
            val ctx = itemView.context

            message.text = item.message

            title.text = item.title.ifBlank {
                ctx.getString(R.string.unknown_title)
            }

            whenTxt.text = item.startAt
                ?.let { it.pretty(ctx.getString(R.string.starts_at_fmt)) }
                ?: ""

            dot.isVisible = !item.isRead

            message.setTypeface(
                null,
                if (item.isRead) Typeface.NORMAL else Typeface.BOLD
            )

            title.setTypeface(
                null,
                if (item.isRead) Typeface.NORMAL else Typeface.BOLD
            )

            itemView.alpha = if (item.isRead) 0.70f else 1f

            img.load(item.coverUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_book_placeholder)
                error(R.drawable.ic_book_placeholder)
            }

            itemView.setOnClickListener {
                onClick(item)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox, parent, false)

        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}

private val inboxFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        .withZone(ZoneId.systemDefault())

private fun Instant.pretty(prefixFmt: String): String =
    prefixFmt.format(inboxFormatter.format(this))