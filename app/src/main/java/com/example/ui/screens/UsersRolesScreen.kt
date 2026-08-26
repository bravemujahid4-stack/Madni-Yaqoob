package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersRolesScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("users") } // users, switch, roles, sync
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()
    var showAddUserDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Users & Access Control",
            subtitle = "Role-based permissions (RBAC), multi-user profiles & cloud sync storage.",
            actionButton = {
                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "users", onClick = { subTab = "users" }, label = { Text("Users (${users.size})") }) }
            item { FilterChip(selected = subTab == "switch", onClick = { subTab = "switch" }, label = { Text("Switch Role / User") }) }
            item { FilterChip(selected = subTab == "sync", onClick = { subTab = "sync" }, label = { Text("Cloud Storage & Sync") }) }
            item { FilterChip(selected = subTab == "roles", onClick = { subTab = "roles" }, label = { Text("Permissions Matrix") }) }
        }

        when (subTab) {
            "users" -> UsersListView(users, currentUser)
            "switch" -> SwitchActiveUserView(viewModel, users, currentUser)
            "sync" -> CloudSyncView(viewModel, cloudSyncState)
            "roles" -> RolesPermissionMatrixView()
        }

        if (showAddUserDialog) {
            AddUserDialog(
                onSave = { newUser ->
                    viewModel.addUser(newUser)
                    showAddUserDialog = false
                },
                onDismiss = { showAddUserDialog = false }
            )
        }
    }
}

@Composable
fun CloudSyncView(viewModel: MasViewModel, syncState: CloudSyncState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MasPaperSoft),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MasBlue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cloud Storage & Persistence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        PillBadge(
                            when {
                                syncState.isSyncing -> "Syncing..."
                                !syncState.isOnline -> "Offline"
                                else -> "Online & Synced"
                            },
                            when {
                                syncState.isSyncing -> "orange"
                                !syncState.isOnline -> "red"
                                else -> "green"
                            }
                        )
                    }

                    Text(
                        "All accounting vouchers, party accounts, factory stock balances, cash accounts, and journals are persisted locally and synchronized securely to persistent storage.",
                        fontSize = 11.5.sp,
                        color = MasMuted
                    )

                    Text("Status: ${syncState.cloudStatusMessage}", fontSize = 11.sp, color = MasInk, fontFamily = FontFamily.Monospace)
                    Text("Last Synced: ${syncState.lastSyncTime}", fontSize = 11.sp, color = MasMuted, fontFamily = FontFamily.Monospace)

                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !syncState.isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = MasBlue),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (syncState.isSyncing) "Syncing Data..." else "Sync Now to Cloud & Storage", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onSave: (AppUser) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Admin") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val roleOptions = listOf("Admin", "Accountant", "Sales Manager", "Purchase Manager", "Production Supervisor", "Viewer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add User Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Assign Role:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MasMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(roleOptions) { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.5.sp) }
                        )
                    }
                }

                errorMessage?.let {
                    Text(it, color = MasRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Name is required"
                        return@Button
                    }
                    if (email.isBlank()) {
                        errorMessage = "Email is required"
                        return@Button
                    }
                    val newUser = AppUser(
                        id = (System.currentTimeMillis() % 100000).toInt(),
                        name = name.trim(),
                        email = email.trim(),
                        role = role,
                        status = "Active"
                    )
                    onSave(newUser)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MasRed)
            ) {
                Text("Create User", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun UsersListView(users: List<AppUser>, currentUser: AppUser) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(users) { u ->
            val isCurrent = u.id == currentUser.id

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isCurrent) MasRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(u.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                PillBadge("Active Session", "green")
                            }
                        }
                        Text("${u.email} · ${u.role}", color = MasMuted, fontSize = 11.sp)
                    }
                    PillBadge(u.role, if (u.role == "Admin") "red" else "blue")
                }
            }
        }
    }
}

@Composable
fun SwitchActiveUserView(viewModel: MasViewModel, users: List<AppUser>, currentUser: AppUser) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("Select a user to switch roles and test role-based access:", fontSize = 12.5.sp, color = MasMuted)
        }

        items(users) { u ->
            val isCurrent = u.id == currentUser.id

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MasRedLight else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isCurrent) MasRed else MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.currentUser.value = u
                    viewModel.showMessage("Switched active user to ${u.name} (${u.role}).")
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(u.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text("Role: ${u.role} · ${u.email}", color = MasMuted, fontSize = 11.sp)
                    }
                    if (isCurrent) {
                        PillBadge("Current User", "green")
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.currentUser.value = u
                                viewModel.showMessage("Switched active user to ${u.name} (${u.role}).")
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Switch", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RolesPermissionMatrixView() {
    val roles = listOf(
        Triple("Admin", "Full access to all 18 modules, approvals, year-end closing, user management", "All Permissions"),
        Triple("Accountant", "General Ledger, Journal Entries, Receivables, Payables, Financial Statements", "GL & Finance"),
        Triple("Sales Manager", "Sales Invoices, Quotations, Orders, Returns, Customer Receipts", "Sales Module"),
        Triple("Purchase Manager", "Purchase Bills, Orders, Returns, Supplier Payments", "Purchase Module"),
        Triple("Production Supervisor", "BOMs, Production Orders, Material Consumptions, Finished Goods Output", "Manufacturing"),
        Triple("Inventory Officer", "Stock Items, Stock Movements, Warehouse Transfers, Physical Count Adjustments", "Inventory"),
        Triple("Auditor (Read-Only)", "View-only access to all reports, audit trail, ledgers, no edit rights", "Audit Only")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(roles) { (role, desc, scope) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(role, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        PillBadge(scope, "blue")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = MasMuted, fontSize = 11.5.sp)
                }
            }
        }
    }
}
