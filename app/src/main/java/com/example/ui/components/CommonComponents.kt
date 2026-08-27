package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// ============================================================================
// MAS Brand & UI Components
// ============================================================================

fun formatMoney(amount: Double, currency: String = "Rs "): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 2
    return "$currency${formatter.format(amount)}"
}

fun formatQty(qty: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 2
    return formatter.format(qty)
}

@Composable
fun MasLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0A0A))
            .border(0.5.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.img_mas_logo),
            contentDescription = "MAS Official Logo",
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
fun MasBrandLogo(
    modifier: Modifier = Modifier,
    height: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF000000))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.img_mas_logo),
            contentDescription = "MAS Official Logo",
            modifier = Modifier.fillMaxHeight(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
fun MasHeroLogoBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF080808))
                        .border(1.dp, MasRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.img_mas_logo),
                        contentDescription = "MAS Logo",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MAS ENTERPRISE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Official Accounting & ERP System",
                        color = MasRedBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Surface(
                color = MasRed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, MasRed.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "VERIFIED",
                    color = MasRedBright,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}


@Composable
fun MasTopBar(
    title: String,
    subtitle: String? = null,
    onMenuClick: () -> Unit,
    userRole: String = "Admin",
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = MasInk,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    MasLogoBadge(size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MAS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MasRed.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = userRole,
                                    color = MasRedBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (title != "MAS" && title.isNotEmpty()) {
                            Text(
                                text = title,
                                color = MasRailMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
    }
}

@Composable
fun PillBadge(
    text: String,
    tone: String = "muted",
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tone.lowercase()) {
        "green", "active", "posted", "received", "paid", "balanced", "completed", "ok" ->
            Pair(MasGreenSoft, MasGreen)
        "red", "inactive", "rejected", "overdue", "voided", "critical", "low" ->
            Pair(MasRedLight, MasRed)
        "amber", "pending", "pending approval", "locked", "in progress", "confirmed", "sent", "due soon" ->
            Pair(MasAmberSoft, MasAmber)
        "blue", "cash", "bank" ->
            Pair(MasBlueSoft, MasBlue)
        else -> Pair(MasPaperSoft, MasMuted)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector? = null,
    tone: Color = MaterialTheme.colorScheme.onSurface,
    sub: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = tone,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
            if (sub != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionButton: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionButton != null) {
            actionButton()
        }
    }
}

// ============================================================================
// Canvas-based Charts for High Performance & Responsive Fit
// ============================================================================

data class ChartBarData(val label: String, val value: Double, val color: Color)

@Composable
fun MasBarChart(
    data: List<ChartBarData>,
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp)
) {
    if (data.isEmpty()) return
    val maxValue = (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = (size.width / (data.size * 1.5f)).coerceAtLeast(12f)
                val spacing = size.width / data.size

                // Background gridlines
                for (i in 1..3) {
                    val y = size.height * (1f - (i / 3f))
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                data.forEachIndexed { index, item ->
                    val barHeight = ((item.value / maxValue).toFloat() * (size.height - 10f)).coerceAtLeast(4f)
                    val left = index * spacing + (spacing - barWidth) / 2f
                    val top = size.height - barHeight

                    drawRoundRect(
                        color = item.color,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MasLineTrendChart(
    points: List<Pair<String, Double>>,
    lineColor: Color = MasInk,
    modifier: Modifier = Modifier.fillMaxWidth().height(150.dp)
) {
    if (points.size < 2) return
    val maxValue = (points.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val minValue = (points.minOfOrNull { it.second } ?: 0.0)
    val range = (maxValue - minValue).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                val stepX = size.width / (points.size - 1)

                val path = Path()
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val normalizedY = ((p.second - minValue) / range).toFloat()
                    val y = size.height * (1f - normalizedY)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                    drawCircle(
                        color = lineColor,
                        radius = 3.5f,
                        center = Offset(x, y)
                    )
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { p ->
                Text(
                    text = p.first,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MasDonutChart(
    values: List<Pair<String, Double>>,
    colors: List<Color>,
    modifier: Modifier = Modifier.size(130.dp)
) {
    val total = values.sumOf { it.second }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = size.width * 0.22f

        values.forEachIndexed { index, item ->
            val sweep = ((item.second / total) * 360f).toFloat()
            drawArc(
                color = colors.getOrElse(index) { MasRed },
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            )
            startAngle += sweep
        }
    }
}

@Composable
fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MasMuted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MasMuted)
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MasRed,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        modifier = modifier
    )
}

data class AccountPickerOption(
    val id: String,
    val code: String,
    val name: String,
    val category: String, // Customer, Supplier, Cash in Hand, Bank, Expense, Revenue, Owner, etc.
    val balance: Double = 0.0,
    val drCrIndicator: String = "",
    val subtitle: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableAccountPicker(
    label: String,
    selectedAccountName: String,
    options: List<AccountPickerOption>,
    onAccountSelected: (AccountPickerOption) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search or select account / party...",
    filterCategories: List<String> = listOf("All", "Customer", "Supplier", "Cash", "Bank", "Expense", "Revenue", "Owner"),
    allowCustomEntry: Boolean = true,
    hideZeroBalancesByDefault: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var hideZeroBalances by remember { mutableStateOf(hideZeroBalancesByDefault) }
    var customInput by remember { mutableStateOf("") }

    val selectedOption = options.find { it.name.equals(selectedAccountName, ignoreCase = true) || it.id.equals(selectedAccountName, ignoreCase = true) || it.code.equals(selectedAccountName, ignoreCase = true) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    searchQuery = ""
                    selectedCategory = "All"
                    showDialog = true
                },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (selectedAccountName.isNotBlank()) MasRed else MaterialTheme.colorScheme.outline),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedAccountName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        val icon = when {
                            selectedOption?.category?.contains("Customer", ignoreCase = true) == true -> Icons.Default.Person
                            selectedOption?.category?.contains("Supplier", ignoreCase = true) == true -> Icons.Default.LocalShipping
                            selectedOption?.category?.contains("Cash", ignoreCase = true) == true -> Icons.Default.Money
                            selectedOption?.category?.contains("Bank", ignoreCase = true) == true -> Icons.Default.AccountBalance
                            selectedOption?.category?.contains("Expense", ignoreCase = true) == true -> Icons.Default.Receipt
                            selectedOption?.category?.contains("Revenue", ignoreCase = true) == true -> Icons.Default.TrendingUp
                            else -> Icons.Default.AccountBox
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MasRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedAccountName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedOption != null) {
                                Text(
                                    text = "${selectedOption.category} · ${selectedOption.code} ${if (selectedOption.balance != 0.0) "· ${formatMoney(selectedOption.balance)} ${selectedOption.drCrIndicator}" else ""}",
                                    fontSize = 11.sp,
                                    color = MasMuted
                                )
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MasMuted, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(placeholder, fontSize = 13.sp, color = MasMuted)
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = MasMuted)
            }
        }
    }

    if (showDialog) {
        val filteredOptions = options.filter { opt ->
            val matchQuery = searchQuery.isBlank() ||
                    opt.name.contains(searchQuery, ignoreCase = true) ||
                    opt.code.contains(searchQuery, ignoreCase = true) ||
                    opt.category.contains(searchQuery, ignoreCase = true) ||
                    opt.subtitle.contains(searchQuery, ignoreCase = true)

            val matchCategory = when (selectedCategory) {
                "All" -> true
                "Customer" -> opt.category.contains("Customer", ignoreCase = true)
                "Supplier" -> opt.category.contains("Supplier", ignoreCase = true)
                "Cash" -> opt.category.contains("Cash", ignoreCase = true)
                "Bank" -> opt.category.contains("Bank", ignoreCase = true)
                "Expense" -> opt.category.contains("Expense", ignoreCase = true)
                "Revenue" -> opt.category.contains("Revenue", ignoreCase = true) || opt.category.contains("Income", ignoreCase = true) || opt.category.contains("Sales", ignoreCase = true)
                "Owner" -> opt.category.contains("Owner", ignoreCase = true) || opt.category.contains("Capital", ignoreCase = true) || opt.category.contains("Investor", ignoreCase = true)
                else -> opt.category.contains(selectedCategory, ignoreCase = true)
            }

            val matchZeroBalance = if (hideZeroBalances && (opt.category.contains("Customer", ignoreCase = true) || opt.category.contains("Supplier", ignoreCase = true))) {
                Math.abs(opt.balance) > 0.001
            } else {
                true
            }

            matchQuery && matchCategory && matchZeroBalance
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Account / Party",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = { showDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Bar
                    SearchBarField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Type name or code to filter..."
                    )

                    // Category Filter Chips
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterCategories.size) { idx ->
                            val cat = filterCategories[idx]
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Zero Balance Filter Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hideZeroBalances = !hideZeroBalances }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hide Rs 0.00 Balances (Due only)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = hideZeroBalances,
                            onCheckedChange = { hideZeroBalances = it },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // Accounts List
                    if (filteredOptions.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = MasMuted, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No matching accounts found", fontSize = 13.sp, color = MasMuted)
                            if (allowCustomEntry && searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        onAccountSelected(
                                            AccountPickerOption(
                                                id = "CUSTOM-${System.currentTimeMillis() % 10000}",
                                                code = "CUSTOM",
                                                name = searchQuery.trim(),
                                                category = "Custom Account"
                                            )
                                        )
                                        showDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Use '${searchQuery.trim()}' as Contra Account", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredOptions.size) { idx ->
                                val opt = filteredOptions[idx]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        if (opt.name.equals(selectedAccountName, ignoreCase = true)) MasRed else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAccountSelected(opt)
                                            showDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = opt.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = when {
                                                        opt.category.contains("Customer", ignoreCase = true) -> MasGreenSoft
                                                        opt.category.contains("Supplier", ignoreCase = true) -> MasRedLight
                                                        opt.category.contains("Cash", ignoreCase = true) -> MasGreenSoft
                                                        opt.category.contains("Bank", ignoreCase = true) -> MasBlueSoft
                                                        opt.category.contains("Expense", ignoreCase = true) -> MasAmberSoft
                                                        else -> MasPaperSoft
                                                    },
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = opt.category,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when {
                                                            opt.category.contains("Customer", ignoreCase = true) -> MasGreen
                                                            opt.category.contains("Supplier", ignoreCase = true) -> MasRed
                                                            opt.category.contains("Cash", ignoreCase = true) -> MasGreen
                                                            opt.category.contains("Bank", ignoreCase = true) -> MasBlue
                                                            opt.category.contains("Expense", ignoreCase = true) -> MasAmber
                                                            else -> MasInk
                                                        },
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Code: ${opt.code} ${if (opt.subtitle.isNotBlank()) "· ${opt.subtitle}" else ""}",
                                                fontSize = 11.sp,
                                                color = MasMuted
                                            )
                                        }
                                        if (opt.balance != 0.0) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${formatMoney(Math.abs(opt.balance))} ${opt.drCrIndicator}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (opt.drCrIndicator == "Dr") MasGreen else MasRed
                                                )
                                                Text(
                                                    text = "Balance",
                                                    fontSize = 9.5.sp,
                                                    color = MasMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
