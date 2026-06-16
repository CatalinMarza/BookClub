// file: com/example/bookclub/ui/inbox/InboxUi.kt
package com.example.bookclub.ui.inbox

import java.time.Instant

data class InboxUi(
    val id: Long,
    val clubId: Long,
    val title: String,
    val message: String,
    val coverUrl: String?,
    val startAt: Instant?,
    val createdAt: Instant,
    val isRead: Boolean
)