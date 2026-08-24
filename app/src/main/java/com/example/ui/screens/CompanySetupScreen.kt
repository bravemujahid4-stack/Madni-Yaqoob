package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Branch
import com.example.data.Department
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySetupScreen(viewModel: MasViewModel) {
    val profile by viewModel.companyProfile.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val departments by viewModel.departments.collectAsState()

    var name by remember(profile) { mutableStateOf(profile.name) }
    var businessType by remember(profile) { mutableStateOf(profile.businessType) }
    var country by remember(profile) { mutableStateOf(profile.country) }
    var currency by remember(profile) { mutableStateOf(profile.currency) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var invoicePrefix by remember(profile) { mutableStateOf(profile.invoicePrefix) }
    var billPrefix by remember(profile) { mutableStateOf(profile.billPrefix) }

    var newBranchName by remember { mutableStateOf("") }
    var newDeptName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        item {
            SectionHeader(
                title = "Company & System Setup",
                subtitle = "Configure identity, fiscal rules, document numbering, branches & departments."
            )
        }

        // Company Identity Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MasLogoBadge(size = 28.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Company Identity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = businessType,
                            onValueChange = { businessType = it },
                            label = { Text("Business Type") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Contact & Address
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Contact & Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Numbering Schemes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Document Numbering", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it },
                            label = { Text("Invoice Prefix") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = billPrefix,
                            onValueChange = { billPrefix = it },
                            label = { Text("Bill Prefix") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Branches Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Branches & Locations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    branches.forEach { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(b.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            PillBadge("Active", "green")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newBranchName,
                            onValueChange = { newBranchName = it },
                            placeholder = { Text("Add Branch name", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newBranchName.isNotBlank()) {
                                    viewModel.branches.value = viewModel.branches.value + Branch("BR-${System.currentTimeMillis() % 1000}", newBranchName.trim())
                                    newBranchName = ""
                                    viewModel.showMessage("Branch added.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MasRed)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    viewModel.companyProfile.value = profile.copy(
                        name = name,
                        businessType = businessType,
                        country = country,
                        currency = currency,
                        address = address,
                        phone = phone,
                        email = email,
                        invoicePrefix = invoicePrefix,
                        billPrefix = billPrefix
                    )
                    viewModel.showMessage("Company setup updated successfully.")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Company Setup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
