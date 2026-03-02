package com.sage.control.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage.control.data.model.Session
import com.sage.control.ui.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onSessionClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessions by viewModel.filteredSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val showTrash by viewModel.showTrash.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { viewModel.createSession { key -> onSessionClick(key) } }) {
                        Icon(Icons.Default.Add, contentDescription = "New session")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createSession { key -> onSessionClick(key) } }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New session")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Connection status indicator
            ConnectionStatusBar(connectionStatus)

            // Search bar
            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Filter sessions...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            // Sessions list
            if (isLoading && sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (sessions.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "No sessions yet",
                    description = "Create your first session to start chatting with Sage"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Recent sessions
                    val recentSessions = sessions.filter { it.status != "archived" && it.status != "trash" }
                    if (recentSessions.isNotEmpty()) {
                        item {
                            SectionHeader("Recent")
                        }
                        items(recentSessions, key = { it.key }) { session ->
                            SessionItem(
                                session = session,
                                onClick = {
                                    viewModel.markAsRead(session.key)
                                    onSessionClick(session.key)
                                },
                                onArchive = { viewModel.archiveSession(session.key) },
                                onDelete = { viewModel.deleteSession(session.key) }
                            )
                        }
                    }

                    // Archived
                    if (viewModel.archivedCount > 0) {
                        item {
                            ArchivedSection(
                                count = viewModel.archivedCount,
                                expanded = showArchived,
                                onToggle = viewModel::toggleArchived
                            )
                        }
                        if (showArchived) {
                            val archivedSessions = sessions.filter { it.status == "archived" }
                            items(archivedSessions, key = { it.key }) { session ->
                                SessionItem(
                                    session = session,
                                    onClick = { onSessionClick(session.key) },
                                    onArchive = null,
                                    onDelete = { viewModel.deleteSession(session.key) }
                                )
                            }
                        }
                    }

                    // Trash
                    if (viewModel.trashCount > 0) {
                        item {
                            TrashSection(
                                count = viewModel.trashCount,
                                expanded = showTrash,
                                onToggle = viewModel::toggleTrash
                            )
                        }
                        if (showTrash) {
                            val trashSessions = sessions.filter { it.status == "trash" }
                            items(trashSessions, key = { it.key }) { session ->
                                SessionItem(
                                    session = session,
                                    onClick = { onSessionClick(session.key) },
                                    onArchive = null,
                                    onDelete = { viewModel.deleteSession(session.key) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBar(status: com.sage.control.data.api.ConnectionStatus) {
    val (color, text) = when (status) {
        is com.sage.control.data.api.ConnectionStatus.Connected -> 
            MaterialTheme.colorScheme.primary to "Connected"
        is com.sage.control.data.api.ConnectionStatus.Connecting -> 
            MaterialTheme.colorScheme.tertiary to "Connecting..."
        is com.sage.control.data.api.ConnectionStatus.Error -> 
            MaterialTheme.colorScheme.error to "Error"
        is com.sage.control.data.api.ConnectionStatus.Disconnected -> 
            MaterialTheme.colorScheme.outline to "Disconnected"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .animateContentSize()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = color,
                    modifier = Modifier.fillMaxSize()
                ) { }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ArchivedSection(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        headlineContent = {
            Text("Archived ($count)")
        },
        leadingContent = {
            Icon(Icons.Default.Archive, contentDescription = null)
        },
        trailingContent = {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    )
}

@Composable
private fun TrashSection(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        headlineContent = {
            Text("Trash ($count)")
        },
        leadingContent = {
            Icon(Icons.Default.Delete, contentDescription = null)
        },
        trailingContent = {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionItem(
    session: Session,
    onClick: () -> Unit,
    onArchive: (() -> Unit)?,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (session.unread) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    ) { }
                }
            }
        },
        supportingContent = {
            Text(
                text = dateFormat.format(Date(session.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (session.unread) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = session.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (session.unread) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    onArchive?.let {
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                showMenu = false
                                it()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}