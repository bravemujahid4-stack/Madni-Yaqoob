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
import com.example.data.AppUser
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersRolesScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("users") } // users, roles, switch
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Users & Access Control",
            subtitle = "Role-based permissions (RBAC), multi-user profiles & active session management."
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "users", onClick = { subTab = "users" }, label = { Text("Users (${users.size})") }) }
            item { FilterChip(selected = subTab == "switch", onClick = { subTab = "switch" }, label = { Text("Switch Active User") }) }
            item { FilterChip(selected = subTab == "roles", onClick = { subTab = "roles" }, label = { Text("Permissions Matrix") }) }
        }

        when (subTab) {
            "users" -> UsersListView(users, currentUser)
            "switch" -> SwitchActiveUserView(viewModel, users, currentUser)
            "roles" -> RolesPermissionMatrixView()
        }
    }
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
