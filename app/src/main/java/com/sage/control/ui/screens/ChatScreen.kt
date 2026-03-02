package com.sage.control.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage.control.data.model.Message
import com.sage.control.data.model.OperationEvent
import com.sage.control.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionKey: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val inlineOperations by viewModel.inlineOperations.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom when new messages arrive or streaming
    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    (messages.size + if (streamingContent.isNotEmpty()) 1 else 0).coerceAtLeast(0)
                )
            }
        }
    }

    // Set session when screen opens
    LaunchedEffect(sessionKey) {
        viewModel.setSession(sessionKey)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sessionKey.take(20) + if (sessionKey.length > 20) "..." else "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        ConnectionStatusText(connectionStatus)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.isStreaming || isThinking) {
                        IconButton(onClick = { viewModel.cancel() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { /* Show theme picker */ }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                // Streaming content
                if (streamingContent.isNotEmpty()) {
                    item {
                        StreamingBubble(
                            content = streamingContent,
                            operations = inlineOperations
                        )
                    }
                }

                // Thinking indicator
                if (isThinking) {
                    item {
                        ThinkingBubble()
                    }
                }
            }

            // Input area
            ChatInput(
                value = draft,
                onValueChange = viewModel::setDraft,
                onSend = {
                    if (draft.isNotBlank()) {
                        viewModel.sendMessage(draft)
                        keyboardController?.hide()
                    }
                },
                enabled = connectionStatus is com.sage.control.data.api.ConnectionStatus.Connected
            )
        }
    }
}

@Composable
private fun ConnectionStatusText(status: com.sage.control.data.api.ConnectionStatus) {
    val text = when (status) {
        is com.sage.control.data.api.ConnectionStatus.Connected -> "Connected"
        is com.sage.control.data.api.ConnectionStatus.Connecting -> "Connecting..."
        is com.sage.control.data.api.ConnectionStatus.Disconnected -> "Disconnected"
        is com.sage.control.data.api.ConnectionStatus.Error -> "Error"
    }
    val color = when (status) {
        is com.sage.control.data.api.ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        is com.sage.control.data.api.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == "user"
    val backgroundColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        message.isThinking -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        message.cancelled -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        message.cancelled -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Operations
                message.operations?.forEach { op ->
                    OperationChip(operation = op)
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(content: String, operations: List<OperationEvent>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Show operations inline
                operations.forEach { op ->
                    OperationChip(operation = op)
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Thinking...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun OperationChip(operation: OperationEvent) {
    val (icon, color) = when (operation.type) {
        "tool_start" -> Icons.Default.Build to MaterialTheme.colorScheme.primary
        "tool_end" -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        "thinking" -> Icons.Default.Psychology to MaterialTheme.colorScheme.tertiary
        else -> Icons.Default.Code to MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = operation.name,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Voice button
            IconButton(
                onClick = { /* Voice input */ },
                enabled = enabled
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice input")
            }

            // Text field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = false,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                trailingIcon = {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Send button
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}