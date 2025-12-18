
package com.fahim.alyfobserver.ai

// This data class is a direct copy from the 'ammu' project to support the copied AI test functionality.
data class ClassifiedComment(
    val id: String,
    val text: String,
    val from: String = "",
    val priority: String = "Pending",
    val reason: String = "",
    val reply: String = "",
    val hasPageReplied: Boolean = false,
    val replyStatusChecked: Boolean = false
)
