package com.sage.control.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Session(
    val key: String,
    val label: String,
    val status: String = "active",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val unread: Boolean = false,
    val model: String? = null
)

@Serializable
data class Message(
    val id: String,
    val sessionKey: String? = null,
    val role: String, // user, assistant, system, tool
    val content: String,
    val timestamp: Long,
    val pending: Boolean = false,
    val attachments: List<FileAttachment>? = null,
    val operations: List<Operation>? = null,
    val isSync: Boolean = false,
    val isThinking: Boolean = false,
    val cancelled: Boolean = false
)

@Serializable
data class FileAttachment(
    val name: String,
    val type: String,
    val size: Long,
    val url: String? = null,
    val data: String? = null
)

@Serializable
data class Operation(
    val id: String,
    val type: String,
    val name: String? = null,
    val params: Map<String, String>? = null,
    val result: String? = null,
    val status: String? = null,
    val startedAt: Long? = null
)

@Serializable
data class WebSocketEvent(
    val type: String,
    val sessionKey: String? = null,
    val message: Message? = null,
    val delta: String? = null,
    val messageId: String? = null,
    val seq: Int? = null,
    val thinking: Boolean? = null,
    val operation: OperationEvent? = null,
    val patch: Map<String, String>? = null
)

@Serializable
data class OperationEvent(
    val id: String,
    val type: String, // tool_start, tool_update, tool_end, thinking
    val name: String,
    val sessionKey: String,
    val params: Map<String, String>? = null,
    val result: String? = null,
    val error: String? = null,
    val status: String? = null,
    val streamOutput: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SendMessageRequest(
    val content: String,
    val sessionKey: String,
    val attachments: List<FileAttachment>? = null
)

@Serializable
data class LiveState(
    val status: String, // idle, thinking, streaming
    val streamBuffer: String? = null,
    val runId: String? = null
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)