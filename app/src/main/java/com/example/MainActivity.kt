package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MasLogoBadge
import com.example.ui.components.PillBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import kotlinx.coroutines.launch

data class NavMenuItem(
    val id: String,
    val title: String,
    val section: String,
    val icon: ImageVector,
    val badge: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MasErpApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasErpApp(viewModel: MasViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("step1") }

    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val journal by viewModel.journal.collectAsState()
    val pendingJEs = journal.count { it.status == "Pending Approval" }

    val navItems = listOf(
        // Executive
        NavMenuItem("step1", "Executive Dashboard", "Executive", Icons.Default.Dashboard),
        NavMenuItem("step17", "Financial Reports (P&L/BS)", "Executive", Icons.Default.Assessment),
        NavMenuItem("step18", "Business Analytics", "Executive", Icons.Default.Insights),
        NavMenuItem("docs", "Document Center & Print", "Executive", Icons.Default.Print),

        // Core Accounting
        NavMenuItem("step2", "Company & Fiscal Setup", "Core Finance", Icons.Default.Business),
        NavMenuItem("parties", "Parties & Account Types", "Core Finance", Icons.Default.Groups),
        NavMenuItem("step3", "Chart of Accounts", "Core Finance", Icons.Default.AccountTree),
        NavMenuItem("step4", "General Ledger & JEs", "Core Finance", Icons.Default.Book, if (pendingJEs > 0) "$pendingJEs" else null),
        NavMenuItem("step9", "Cash & Bank Management", "Core Finance", Icons.Default.AccountBalance),
        NavMenuItem("step10", "Expenses & Overheads", "Core Finance", Icons.Default.CreditCard),
        NavMenuItem("closing", "Period Closing & Audit", "Core Finance", Icons.Default.LockClock),

        // Commercial
        NavMenuItem("step5", "Customers & Receivables", "Sales & Customers", Icons.Default.People),
        NavMenuItem("step6", "Sales (Invoices & Orders)", "Sales & Customers", Icons.Default.PointOfSale),
        NavMenuItem("step7", "Suppliers & Payables", "Purchases & Vendors", Icons.Default.LocalShipping),
        NavMenuItem("step8", "Purchase & Bills", "Purchases & Vendors", Icons.Default.ShoppingCart),

        // Operations
        NavMenuItem("step11", "Inventory & Stock Control", "Operations", Icons.Default.Inventory2),
        NavMenuItem("step12", "Manufacturing & BOM", "Operations", Icons.Default.PrecisionManufacturing),
        NavMenuItem("step13", "Fixed Assets Register", "Operations", Icons.Default.Domain),

        // Administration
        NavMenuItem("users", "Users & Access Control", "Administration", Icons.Default.AdminPanelSettings)
    )

    val currentItem = navItems.find { it.id == currentRoute } ?: navItems.first()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MasInk,
                drawerContentColor = Color.White,
                modifier = Modifier.width(310.dp)
            ) {
                DrawerContent(
                    items = navItems,
                    currentRoute = currentRoute,
                    onSelect = {
                        currentRoute = it
                        scope.launch { drawerState.close() }
                    },
                    currentUser = currentUser,
                    companyName = companyProfile.name
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MasLogoBadge(size = 30.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentItem.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${companyProfile.name} · FY2026",
                                    fontSize = 10.5.sp,
                                    color = MasMuted
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MasRed)
                        }
                    },
                    actions = {
                        // User Profile Badge
                        Surface(
                            color = MasRedLight,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clickable { currentRoute = "users" }
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MasRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser.name.take(1),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentUser.role,
                                    color = MasRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "step1",
                        onClick = { currentRoute = "step1" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MasRed,
                            selectedTextColor = MasRed,
                            indicatorColor = MasRedLight
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "step4",
                        onClick = { currentRoute = "step4" },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Ledger") },
                        label = { Text("Ledger", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MasRed,
                            selectedTextColor = MasRed,
                            indicatorColor = MasRedLight
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "step6",
                        onClick = { currentRoute = "step6" },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Sales") },
                        label = { Text("Sales", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MasRed,
                            selectedTextColor = MasRed,
                            indicatorColor = MasRedLight
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "step8",
                        onClick = { currentRoute = "step8" },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Purchases") },
                        label = { Text("Purchases", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MasRed,
                            selectedTextColor = MasRed,
                            indicatorColor = MasRedLight
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "step17",
                        onClick = { currentRoute = "step17" },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                        label = { Text("Reports", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MasRed,
                            selectedTextColor = MasRed,
                            indicatorColor = MasRedLight
                        )
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    "step1" -> DashboardScreen(viewModel = viewModel, onNavigateToModule = { currentRoute = it })
                    "step2" -> CompanySetupScreen(viewModel = viewModel)
                    "parties" -> PartyAccountsScreen(viewModel = viewModel)
                    "step3" -> ChartOfAccountsScreen(viewModel = viewModel, onNavigateToParties = { currentRoute = "parties" })
                    "step4" -> GeneralLedgerScreen(viewModel = viewModel)
                    "step5" -> CustomersScreen(viewModel = viewModel)
                    "step6" -> SalesScreen(viewModel = viewModel)
                    "step7" -> SuppliersScreen(viewModel = viewModel)
                    "step8" -> PurchasesScreen(viewModel = viewModel)
                    "step9" -> CashBankScreen(viewModel = viewModel)
                    "step10" -> ExpenseScreen(viewModel = viewModel)
                    "step11" -> InventoryScreen(viewModel = viewModel)
                    "step12" -> ManufacturingScreen(viewModel = viewModel)
                    "step13" -> FixedAssetsScreen(viewModel = viewModel)
                    "step17" -> FinancialReportsScreen(viewModel = viewModel)
                    "step18" -> BusinessReportsScreen(viewModel = viewModel)
                    "docs" -> DocumentsScreen(viewModel = viewModel)
                    "closing" -> PeriodClosingScreen(viewModel = viewModel)
                    "users" -> UsersRolesScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel, onNavigateToModule = { currentRoute = it })
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    items: List<NavMenuItem>,
    currentRoute: String,
    onSelect: (String) -> Unit,
    currentUser: com.example.data.AppUser,
    companyName: String
) {
    var searchFilter by remember { mutableStateOf("") }
    val grouped = items.filter {
        searchFilter.isBlank() || it.title.contains(searchFilter, ignoreCase = true) || it.section.contains(searchFilter, ignoreCase = true)
    }.groupBy { it.section }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // App Branding Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MasLogoBadge(size = 38.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "MAS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = companyName,
                    color = MasRailMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quick Search Modules
        OutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            placeholder = { Text("Filter modules...", fontSize = 12.sp, color = MasRailMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MasRailMuted, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MasInkLight,
                unfocusedContainerColor = MasInkLight,
                focusedBorderColor = MasRed,
                unfocusedBorderColor = MasRailBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        HorizontalDivider(color = MasRailBorder, modifier = Modifier.padding(vertical = 6.dp))

        // Navigation Sections List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            grouped.forEach { (section, sectionItems) ->
                item {
                    Text(
                        text = section.uppercase(),
                        color = MasRed,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(sectionItems) { item ->
                    val isSelected = item.id == currentRoute
                    Surface(
                        color = if (isSelected) MasRed else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) Color.White else MasRailMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.title,
                                    color = if (isSelected) Color.White else MasRailText,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (item.badge != null) {
                                Surface(
                                    color = if (isSelected) Color.White else MasAmber,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        color = if (isSelected) MasRed else MasInk,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MasRailBorder, modifier = Modifier.padding(vertical = 6.dp))

        // Logged In User Info in Drawer Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MasRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.name.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser.name,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${currentUser.role} · Online",
                    color = MasGreen,
                    fontSize = 10.5.sp
                )
            }
            IconButton(onClick = { onSelect("users") }) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Switch User",
                    tint = MasRailMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
