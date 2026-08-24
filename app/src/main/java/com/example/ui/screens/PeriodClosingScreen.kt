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
import androidx.compose.ui.graphics.Color
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
fun PeriodClosingScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("monthly") } // monthly, yearend, audit
    val periodStatuses by viewModel.periodStatuses.collectAsState()
    val isClosed by viewModel.isFiscalYearClosed.collectAsState()
    val auditLog by viewModel.auditLog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Period Closing & Audit Trail",
            subtitle = "Lock accounting periods, execute fiscal year-end close & view audit logs."
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "monthly", onClick = { subTab = "monthly" }, label = { Text("Monthly Locks") }) }
            item { FilterChip(selected = subTab == "yearend", onClick = { subTab = "yearend" }, label = { Text("Year-End Close") }) }
            item { FilterChip(selected = subTab == "audit", onClick = { subTab = "audit" }, label = { Text("Audit Trail (${auditLog.size})") }) }
        }

        when (subTab) {
            "monthly" -> MonthlyPeriodsView(viewModel, periodStatuses)
            "yearend" -> YearEndCloseView(viewModel, isClosed)
            "audit" -> AuditTrailListView(auditLog)
        }
    }
}

@Composable
fun MonthlyPeriodsView(viewModel: MasViewModel, statuses: Map<String, String>) {
    val months = listOf(
        "2026-01" to "January 2026",
        "2026-02" to "February 2026",
        "2026-03" to "March 2026",
        "2026-04" to "April 2026",
        "2026-05" to "May 2026",
        "2026-06" to "June 2026",
        "2026-07" to "July 2026",
        "2026-08" to "August 2026 (Current)"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(months) { (key, label) ->
            val status = statuses[key] ?: "Open"
            var expanded by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text("Status: $status", color = MasMuted, fontSize = 11.sp)
                    }

                    Box {
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status == "Open") MasGreen else if (status == "Soft Locked") MasAmber else MasRed
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(status, fontSize = 11.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Open (Full editing)") }, onClick = {
                                viewModel.setPeriodStatus(key, "Open")
                                expanded = false
                            })
                            DropdownMenuItem(text = { Text("Soft Locked (Admins only)") }, onClick = {
                                viewModel.setPeriodStatus(key, "Soft Locked")
                                expanded = false
                            })
                            DropdownMenuItem(text = { Text("Hard Closed (Read only)") }, onClick = {
                                viewModel.setPeriodStatus(key, "Hard Closed")
                                expanded = false
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearEndCloseView(viewModel: MasViewModel, isClosed: Boolean) {
    var check1 by remember { mutableStateOf(true) }
    var check2 by remember { mutableStateOf(true) }
    var check3 by remember { mutableStateOf(true) }
    var check4 by remember { mutableStateOf(true) }

    val allChecked = check1 && check2 && check3 && check4

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Fiscal Year-End Closing Wizard (FY2026)", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text(
                        "Closing the fiscal year will zero out all nominal Revenue & Expense accounts, post net profit to Retained Earnings, and roll over opening balances to the next fiscal year.",
                        fontSize = 12.sp,
                        color = MasMuted
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text("Pre-Closing Checklist", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = check1, onCheckedChange = { check1 = it })
                        Text("Bank Reconciliations completed for all accounts", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = check2, onCheckedChange = { check2 = it })
                        Text("Physical Stock Count & Adjustments verified", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = check3, onCheckedChange = { check3 = it })
                        Text("Fixed Assets Depreciation batch executed", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = check4, onCheckedChange = { check4 = it })
                        Text("All draft and pending journal vouchers approved", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.runYearEndClose() },
                        enabled = allChecked && !isClosed,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isClosed) MasGreen else MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isClosed) "FY2026 Closed Successfully" else "Execute Fiscal Year-End Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditTrailListView(auditLog: List<AuditLogEntry>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(auditLog) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PillBadge(log.module, "blue")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(log.details, fontSize = 12.sp, color = MasInk)
                        Text("${log.time} · User: ${log.user} · Target: ${log.transactionId.ifEmpty { "-" }}", fontSize = 10.5.sp, color = MasMuted)
                    }
                }
            }
        }
    }
}
