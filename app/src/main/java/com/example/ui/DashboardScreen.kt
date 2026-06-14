package com.example.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GoldItem
import com.example.data.MarketSetting
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Theme specific luxury colors
val GoldPrimary = Color(0xFFD4AF37)     // Radiant Metallic Gold
val GoldSecondary = Color(0xFFAA7C11)   // Dark Amber Gold
val GoldAccent = Color(0xFFFFF2B2)      // Bright Champagne Gold
val ObsidianBackground = Color(0xFF0F0E0B) // Luxury Dark Onyx
val CardSurface = Color(0xFF1B1914)     // Charcoal Gold Surface
val DarkBorder = Color(0xFF2C271E)      // Gold infused border
val SuccessGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFFF9800)
val ErrorRed = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GoldViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val prices by viewModel.allPrices.collectAsStateWithLifecycle()
    val settings by viewModel.marketSettings.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingPrices.collectAsStateWithLifecycle()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showMarketParamDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<GoldItem?>(null) }
    var selectedInvoicePathForPreview by remember { mutableStateOf<String?>(null) }
    var showManualPriceDialog by remember { mutableStateOf(false) }

    // Listen to status messages from price API
    LaunchedEffect(Unit) {
        viewModel.priceRefreshStatus.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Automatically trigger biometrics lock screen on startup
    LaunchedEffect(isAppLocked) {
        if (isAppLocked && viewModel.isBiometricSettingActive()) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricPromptManager.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.unlockApp() },
                    onError = { err ->
                        Log.w("DashboardScreen", "Biometric error: $err")
                        // Keep lock state but don't crash, user can authenticate manually with PIN or local button
                    },
                    onFailed = {
                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        if (isAppLocked) {
            // Lock Screen overlay
            LockScreen(
                onUnlockRequested = {
                    val activity = context as? FragmentActivity
                    if (activity != null && viewModel.isBiometricSettingActive()) {
                        BiometricPromptManager.showBiometricPrompt(
                            activity = activity,
                            onSuccess = { viewModel.unlockApp() },
                            onError = { err ->
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                            },
                            onFailed = {
                                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        // Safe bypass for devices that don't have biometrics configured
                        viewModel.unlockApp()
                        Toast.makeText(context, "Unlocked Portfolio", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            // Main App Content
            Scaffold(
                containerColor = ObsidianBackground,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Savings,
                                        contentDescription = "Wallet Logo",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Smart Gold Wallet",
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                                Row {
                                    var showZakatDialog by remember { mutableStateOf(false) }
                                    if (showZakatDialog) {
                                        ZakatDialog(viewModel, onDismiss = { showZakatDialog = false })
                                    }
                                    IconButton(
                                        onClick = { showZakatDialog = true },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Calculate,
                                            contentDescription = "Zakat Calculator",
                                            tint = GoldPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { showMarketParamDialog = true },
                                        modifier = Modifier.testTag("settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = "Market Settings",
                                            tint = GoldPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.lockApp() },
                                        modifier = Modifier.testTag("lock_app_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LockOpen,
                                            contentDescription = "Lock Wallet",
                                            tint = GoldPrimary
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianBackground,
                            titleContentColor = GoldPrimary
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddItemDialog = true },
                        containerColor = GoldPrimary,
                        contentColor = ObsidianBackground,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("add_item_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Gold Item",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            ) { innerPadding ->
                // Portfolio calculations
                val totalPurchaseCost = items.sumOf { it.purchasePrice }
                val totalResaleValue = items.sumOf { viewModel.calculateResaleValue(it) }
                val netProfitLoss = totalResaleValue - totalPurchaseCost

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Live Gold Prices Banner
                    item {
                        val lastUpdatedTimestamp = prices.maxOfOrNull { it.lastUpdated } ?: 0L
                        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val lastUpdatedText = if (lastUpdatedTimestamp > 0) dateFormat.format(Date(lastUpdatedTimestamp)) else "Never updated"
                        
                        val globalMarket = viewModel.globalMarketData.collectAsStateWithLifecycle().value
                        
                        val isFetchedToday = (System.currentTimeMillis() - lastUpdatedTimestamp) < (24 * 60 * 60 * 1000)
                        
                        PricesBanner(
                            buy24 = if (isFetchedToday) viewModel.getPriceForPurity("24K") else 0.0,
                            sell24 = if (isFetchedToday) viewModel.getSellPriceForPurity("24K") else 0.0,
                            buy21 = if (isFetchedToday) viewModel.getPriceForPurity("21K") else 0.0,
                            sell21 = if (isFetchedToday) viewModel.getSellPriceForPurity("21K") else 0.0,
                            buy18 = if (isFetchedToday) viewModel.getPriceForPurity("18K") else 0.0,
                            sell18 = if (isFetchedToday) viewModel.getSellPriceForPurity("18K") else 0.0,
                            buyPound = if (isFetchedToday) viewModel.getPriceForPurity("POUND") else 0.0,
                            sellPound = if (isFetchedToday) viewModel.getSellPriceForPurity("POUND") else 0.0,
                            spotGoldUSD = globalMarket?.spotGoldUSD ?: 0.0,
                            usdEgpRate = globalMarket?.usdEgpRate ?: 0.0,
                            lastUpdated = lastUpdatedText,
                            isRefreshing = isRefreshing,
                            onRefreshPrices = { viewModel.refreshGoldPrices() },
                            onEditManual = { showManualPriceDialog = true }
                        )
                    }

                    // 2. Main Portfolio Metric Card
                    item {
                        PortfolioSummaryCard(
                            totalValue = totalResaleValue,
                            totalCost = totalPurchaseCost,
                            profit = netProfitLoss
                        )
                    }

                    // 2.5 Market Gap & Signal
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                EgyptGlobalGapCard(viewModel)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SmartBuySignalCard(viewModel)
                            }
                        }
                    }

                    // 3. Gold Distribution Meter
                    if (items.isNotEmpty()) {
                        item {
                            GoldDistributionMeter(items = items)
                        }
                    }

                    // 4. Section Title
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Your Holdings (${items.size})",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (items.isNotEmpty()) {
                                Text(
                                    text = "Prices in EGP",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // 5. Empty State or Gold Items List
                    if (items.isEmpty()) {
                        item {
                            EmptyState(onAddClicked = { showAddItemDialog = true })
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            GoldItemRow(
                                item = item,
                                currentPrice = viewModel.getPriceForPurity(item.purity),
                                resaleValue = viewModel.calculateResaleValue(item),
                                profitLoss = viewModel.calculateProfitLoss(item),
                                onEditClicked = { itemToEdit = item },
                                onDeleteClicked = { viewModel.deleteItem(item) },
                                onInvoiceClicked = { path -> selectedInvoicePathForPreview = path },
                                viewModel = viewModel
                            )
                        }
                    }

                    // Spacing at bottom
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // --- dialogs ---

        if (showAddItemDialog) {
            AddOrEditItemDialog(
                onDismiss = { showAddItemDialog = false },
                onAddOrEdit = { name, cat, purity, weight, price, file ->
                    viewModel.addItem(name, cat, purity, weight, price, file)
                    showAddItemDialog = false
                },
                viewModel = viewModel
            )
        }

        itemToEdit?.let { item ->
            AddOrEditItemDialog(
                itemToEdit = item,
                onDismiss = { itemToEdit = null },
                onAddOrEdit = { name, cat, purity, weight, price, file ->
                    viewModel.editItem(item.id, name, cat, purity, weight, price, file)
                    itemToEdit = null
                },
                viewModel = viewModel
            )
        }

        if (showMarketParamDialog) {
            MarketSettingsDialog(
                settings = settings,
                onDismiss = { showMarketParamDialog = false },
                onSave = { category, fee, cashback ->
                    viewModel.updateMarketSettingsManual(category, fee, cashback)
                }
            )
        }

        if (showManualPriceDialog) {
            ManualPriceOverrideDialog(
                price24 = viewModel.getPriceForPurity("24K"),
                price21 = viewModel.getPriceForPurity("21K"),
                price18 = viewModel.getPriceForPurity("18K"),
                onDismiss = { showManualPriceDialog = false },
                onSave = { p24, p21, p18 ->
                    viewModel.updatePurityPriceManual("24K", p24)
                    viewModel.updatePurityPriceManual("21K", p21)
                    viewModel.updatePurityPriceManual("18K", p18)
                    showManualPriceDialog = false
                }
            )
        }

        selectedInvoicePathForPreview?.let { path ->
            InvoicePreviewDialog(
                imagePath = path,
                onDismiss = { selectedInvoicePathForPreview = null }
            )
        }
    }
}

@Composable
fun LockScreen(onUnlockRequested: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Brush.radialGradient(listOf(GoldSecondary.copy(alpha = 0.4f), Color.Transparent)))
                .border(2.dp, GoldPrimary, CircleShape)
                .wrapContentSize(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Secured Wallet",
                tint = GoldPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Smart Gold Wallet",
            color = GoldPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Text(
            text = "محفظة الذهب الذكية",
            color = GoldAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Financial database protected with biometric encryption.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onUnlockRequested,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = ObsidianBackground
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("biometric_unlock_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = "Fingerprint icon"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Unlock Portfolio",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PricesBanner(
    buy24: Double,
    sell24: Double,
    buy21: Double,
    sell21: Double,
    buy18: Double,
    sell18: Double,
    buyPound: Double,
    sellPound: Double,
    spotGoldUSD: Double,
    usdEgpRate: Double,
    lastUpdated: String,
    isRefreshing: Boolean,
    onRefreshPrices: () -> Unit,
    onEditManual: () -> Unit
) {
    var isDebugVisible by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = "Trending",
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp).clickable { isDebugVisible = !isDebugVisible }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Live Prices in Egypt (EGP / gram)",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { isDebugVisible = !isDebugVisible }
                        )
                        Text(
                            text = if (buy24 > 0.0) "Last Updated: $lastUpdated\nUsing Last Verified Prices" else "Updated: $lastUpdated",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (buy24 > 0.0) "Source: iSagha (Verified)" else "No verified gold prices available.",
                            color = if (buy24 > 0.0) SuccessGreen else ErrorRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEditManual,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit manual prices",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onRefreshPrices,
                        modifier = Modifier.size(28.dp),
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh prices",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (spotGoldUSD > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Spot Gold: $${String.format("%,.1f", spotGoldUSD)}/oz", fontSize = 10.sp, color = GoldAccent)
                    Text(text = "USD/EGP: ${String.format("%,.2f", usdEgpRate)}", fontSize = 10.sp, color = GoldSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PriceItemUnit(label = "24K", buyPrice = buy24, sellPrice = sell24)
                DividerVertical()
                PriceItemUnit(label = "21K (سبيكة)", buyPrice = buy21, sellPrice = sell21)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PriceItemUnit(label = "18K (مشغولات)", buyPrice = buy18, sellPrice = sell18)
                DividerVertical()
                PriceItemUnit(label = "الجنيه الذهب (Pound)", buyPrice = buyPound, sellPrice = sellPound)
            }
            
            if (isDebugVisible) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.DarkGray.copy(alpha=0.5f), RoundedCornerShape(8.dp)).padding(12.dp)
                ) {
                    Text("DEVELOPER DEBUG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Current Source: https://market.isagha.com (Jsoup HTML Extract)", color = GoldSecondary, fontSize = 10.sp)
                    Text("Last validation check: $lastUpdated", color = GoldSecondary, fontSize = 10.sp)
                    Text("Validation Rule 1: 24K > 21K > 18K", color = SuccessGreen, fontSize = 10.sp)
                    Text("Validation Rule 2: Pound matches ~ (21K * 8)", color = SuccessGreen, fontSize = 10.sp)
                    Text("Validation Rule 3: Math > 0.0", color = SuccessGreen, fontSize = 10.sp)
                    Text("Parsed Prices Map: 24K=$buy24, 21K=$buy21, 18K=$buy18, Pound=$buyPound", color = GoldAccent, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onRefreshPrices() },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Inspect Source (Force Refresh & Validate)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.PriceItemUnit(label: String, buyPrice: Double, sellPrice: Double) {
    val spread = sellPrice - buyPrice
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "شراء (Buy)",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
                Text(
                    text = String.format("%,.0f", buyPrice),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldSecondary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "بيع (Sell)",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
                Text(
                    text = String.format("%,.0f", sellPrice),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "الفرق: ${String.format("%,.0f", spread)} EGP",
                fontSize = 9.sp,
                color = WarningOrange
            )
        }
    }
}

@Composable
fun DividerVertical() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(DarkBorder)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun EgyptGlobalGapCard(viewModel: GoldViewModel) {
    val gap = viewModel.getEgyptGap21K()
    val gapPct = viewModel.getGapPercentage21K()
    val isFair = gapPct in -1.5..1.5
    val isOver = gapPct > 1.5

    val tintColor = if (isFair) GoldSecondary else if (isOver) ErrorRed else SuccessGreen
    
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, color = tintColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EGYPT GAP",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%+.0f EGP", gap),
                color = tintColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Text(
                text = String.format("(%+.1f%%)", gapPct),
                color = tintColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun SmartBuySignalCard(viewModel: GoldViewModel) {
    val gapPct = viewModel.getGapPercentage21K()
    val signalText: String
    val signalColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    if (gapPct > 3.0) {
        signalText = "OVERPRICED"
        signalColor = ErrorRed
        icon = Icons.Rounded.Warning
    } else if (gapPct < -1.0) {
        signalText = "STRONG BUY"
        signalColor = SuccessGreen
        icon = Icons.Rounded.Verified
    } else {
        signalText = "FAIR VALUE"
        signalColor = GoldSecondary
        icon = Icons.Rounded.CheckCircle
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, color = signalColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = signalColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SMART SIGNAL",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = signalText,
                color = signalColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ZakatDialog(viewModel: GoldViewModel, onDismiss: () -> Unit) {
    val items = viewModel.allItems.collectAsStateWithLifecycle().value
    var totalPureWeight = 0.0
    var totalValue = 0.0

    items.forEach { item ->
        val purityMultiplier = when(item.purity) {
            "24K" -> 1.0
            "21K" -> 21.0 / 24.0
            "18K" -> 18.0 / 24.0
            else -> 0.0
        }
        totalPureWeight += (item.weight * purityMultiplier)
        totalValue += viewModel.calculateResaleValue(item)
    }

    val nisabWeight24K = 85.0
    val requiresZakat = totalPureWeight >= nisabWeight24K
    val zakatAmount = if (requiresZakat) totalValue * 0.025 else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Zakat Calculator",
                    color = GoldPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Pure Gold (24K equiv):", color = Color.Gray, fontSize = 12.sp)
                    Text(String.format("%,.2f g", totalPureWeight), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Portfolio Market Value:", color = Color.Gray, fontSize = 12.sp)
                    Text(String.format("%,.0f EGP", totalValue), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nisab Target (24K):", color = Color.Gray, fontSize = 12.sp)
                    Text("85.0 g", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (requiresZakat) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        if (requiresZakat) {
                            Text("Zakat is Required (حالة النصاب متحققة)", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Amount to Pay (2.5%):", color = Color.LightGray, fontSize = 14.sp)
                            Text(String.format("%,.0f EGP", zakatAmount), color = GoldPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        } else {
                            Text("Zakat Not Required (لم يبلغ النصاب)", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("You need ${String.format("%.1f", nisabWeight24K - totalPureWeight)}g more of pure gold.", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    totalValue: Double,
    totalCost: Double,
    profit: Double
) {
    val isProfit = profit >= 0
    val accentBrush = Brush.linearGradient(
        colors = listOf(CardSurface, CardSurface.copy(alpha = 0.95f))
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .background(accentBrush, shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL PORTFOLIO VALUE (Selling Value)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                letterSpacing = 1.sp
            )

            Text(
                text = String.format("%,.1f EGP", totalValue),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldAccent,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historical Cost (buying price)",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = String.format("%,.1f EGP", totalCost),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Profit / Loss",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isProfit) SuccessGreen else ErrorRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%,.1f EGP", profit),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProfit) SuccessGreen else ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoldDistributionMeter(items: List<GoldItem>) {
    val categoryWeights = items.groupBy { it.category }.mapValues { entry ->
        entry.value.sumOf { it.weight }
    }
    val totalWeight = categoryWeights.values.sum()

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Portfolio Allocation",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stacked bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                val bullionWeight = categoryWeights["BULLION"] ?: 0.0
                val coinWeight = categoryWeights["COIN"] ?: 0.0
                val jewelryWeight = categoryWeights["JEWELRY"] ?: 0.0

                if (totalWeight > 0) {
                    if (bullionWeight > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((bullionWeight / totalWeight).toFloat())
                                .background(GoldPrimary)
                        )
                    }
                    if (coinWeight > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((coinWeight / totalWeight).toFloat())
                                .background(GoldSecondary)
                        )
                    }
                    if (jewelryWeight > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((jewelryWeight / totalWeight).toFloat())
                                .background(GoldAccent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AllocationIndicator(label = "Bullion (سبيكة)", weight = categoryWeights["BULLION"] ?: 0.0, color = GoldPrimary)
                AllocationIndicator(label = "Coin (جنيه)", weight = categoryWeights["COIN"] ?: 0.0, color = GoldSecondary)
                AllocationIndicator(label = "Jewelry (مشغولات)", weight = categoryWeights["JEWELRY"] ?: 0.0, color = GoldAccent)
            }
        }
    }
}

@Composable
fun AllocationIndicator(label: String, weight: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
            Text(text = String.format("%.1f g", weight), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoldItemRow(
    item: GoldItem,
    currentPrice: Double,
    resaleValue: Double,
    profitLoss: Double,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onInvoiceClicked: (String) -> Unit,
    viewModel: GoldViewModel
) {
    val isProfit = profitLoss >= 0
    val displayCategory = when (item.category) {
        "BULLION" -> "Bullion (سبيكة)"
        "COIN" -> "Gold Coin (جنيه ذهب)"
        else -> "Jewelry (مشغولات)"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClicked() }
            .testTag("item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.name,
                        color = GoldAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$displayCategory • ${item.purity}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteClicked,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_item_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete item",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Core measurements
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Weight", fontSize = 11.sp, color = Color.Gray)
                    Text(text = "${item.weight} g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(text = "Purchase Cost", fontSize = 11.sp, color = Color.Gray)
                    Text(text = String.format("%,.0f EGP", item.purchasePrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Resale Value", fontSize = 11.sp, color = Color.Gray)
                    Text(text = String.format("%,.0f EGP", resaleValue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Market Details & Cashback Explanation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Return breakdown details
                Text(
                    text = if (item.category == "JEWELRY") {
                        "Jewelry calculation: Making fees dropped entirely"
                    } else {
                        val cashbackPerGram = viewModel.getCashbackPerGram(item.category)
                        String.format("EGP %,.1f/g Cashback included", cashbackPerGram)
                    },
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.weight(1f)
                )

                // Profit tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background((if (isProfit) SuccessGreen else ErrorRed).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("%+,.0f EGP", profitLoss),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isProfit) SuccessGreen else ErrorRed
                    )
                }
            }

            // Image attachment bar if exists
            item.capturedInvoicePath?.let { path ->
                if (File(path).exists()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInvoiceClicked(path) }
                            .border(1.dp, GoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = "Invoice attached",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Purchase Invoice Linked (Click to View)",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1.0f))
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(path))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Invoice Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(onAddClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Inbox,
            contentDescription = "No elements",
            tint = DarkBorder,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your gold portfolio is currently empty.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tap + to add your first Bullion, Coin, or Jewelry item.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onAddClicked,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Add First Item")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditItemDialog(
    itemToEdit: GoldItem? = null,
    onDismiss: () -> Unit,
    onAddOrEdit: (String, String, String, Double, Double, String?) -> Unit,
    viewModel: GoldViewModel
) {
    val context = LocalContext.current
    var name by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(itemToEdit?.name ?: "") }
    var category by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(itemToEdit?.category ?: "BULLION") }
    var purity by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(itemToEdit?.purity ?: "24K") }
    var weightText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(itemToEdit?.weight?.toString() ?: "") }
    var priceText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(itemToEdit?.purchasePrice?.toString() ?: "") }
    var invoicePath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(itemToEdit?.capturedInvoicePath) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var purityExpanded by remember { mutableStateOf(false) }

    // Camera captures variables - state restored properly (Scenario D)
    var currentPhotoPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var showPermissionSettingsDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("CAMERA_AUDIT", "CAMERA_RESULT_RECEIVED")
            currentPhotoPath?.let { path ->
                invoicePath = path
                Toast.makeText(context, "Invoice image captured successfully!", Toast.LENGTH_SHORT).show()
                Log.d("CAMERA_AUDIT", "Attached image path: $path")
            }
        } else {
            Log.d("CAMERA_AUDIT", "CAMERA_RESULT_RECEIVED (failed_or_cancelled)")
            Toast.makeText(context, "Camera action cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("CAMERA_AUDIT", "CAMERA_PERMISSION_GRANTED")
            val file = viewModel.createInvoiceFile()
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            currentPhotoPath = file.absolutePath
            try {
                Log.d("CAMERA_AUDIT", "CAMERA_INTENT_LAUNCHED")
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Log.e("CAMERA_AUDIT", "CAMERA_ERROR", e)
                Toast.makeText(context, "Could not start camera interface: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Check if permanently denied
            val activity = context as? android.app.Activity
            val showRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.CAMERA)
            } ?: true

            if (!showRationale) {
                Log.d("CAMERA_AUDIT", "CAMERA_PERMISSION_PERMANENTLY_DENIED")
                showPermissionSettingsDialog = true
            } else {
                Log.d("CAMERA_AUDIT", "CAMERA_PERMISSION_DENIED")
                Toast.makeText(context, "Camera permission is denied. We cannot take a photo without it.", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Camera Permission Required", color = Color.White) },
            text = { Text("You have permanently denied camera access. Please open App Settings and grant camera permissions to capture photo invoices.", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettingsDialog = false
                        try {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("CAMERA_AUDIT", "CAMERA_ERROR", e)
                            Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Open Settings", color = GoldPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardSurface,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (itemToEdit == null) "Add Gold Asset" else "Edit Gold Asset",
                    color = GoldPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Item Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name / Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.LightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("item_name_input")
                )

                // Category Dropdown selector
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = when (category) {
                                "BULLION" -> "Bullion (سبيكة)"
                                "COIN" -> "Gold Coin (جنيه ذهب)"
                                else -> "Jewelry (مشغولات)"
                            },
                            onValueChange = {},
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = GoldPrimary,
                                unfocusedLabelColor = Color.LightGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bullion (سبيكة)", color = Color.White) },
                                onClick = {
                                    category = "BULLION"
                                    // Default Purity for bullion is often 24K
                                    purity = "24K"
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("cat_bullion_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Gold Coin (جنيه ذهب)", color = Color.White) },
                                onClick = {
                                    category = "COIN"
                                    // Default Purity for Gold coins in Egypt is usually 21K
                                    purity = "21K"
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("cat_coin_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Jewelry (مشغولات)", color = Color.White) },
                                onClick = {
                                    category = "JEWELRY"
                                    purity = "18K"
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("cat_jewelry_option")
                            )
                        }
                    }
                }

                // Purity Selector
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = purityExpanded,
                        onExpandedChange = { purityExpanded = !purityExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = purity,
                            onValueChange = {},
                            label = { Text("Gold Purity (Carat)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purityExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = GoldPrimary,
                                unfocusedLabelColor = Color.LightGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = purityExpanded,
                            onDismissRequest = { purityExpanded = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("24K (Pure Bullion)", color = Color.White) },
                                onClick = {
                                    purity = "24K"
                                    purityExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("21K (Standard Coin/Jewelry)", color = Color.White) },
                                onClick = {
                                    purity = "21K"
                                    purityExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("18K (Standard Jewelry)", color = Color.White) },
                                onClick = {
                                    purity = "18K"
                                    purityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Weight
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (grams)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.LightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("item_weight_input")
                )

                // Historic Purchase Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Total Buying Cost (including making fees)") },
                    prefix = { Text("EGP ", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.LightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("item_price_input")
                )

                // Invoice Attachment Button
                HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Purchase Invoice Photo",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (invoicePath == null) "No invoice linked" else "Invoice image attached ✓",
                            color = if (invoicePath == null) Color.Gray else SuccessGreen,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val file = viewModel.createInvoiceFile()
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                currentPhotoPath = file.absolutePath
                                try {
                                    Log.d("CAMERA_AUDIT", "CAMERA_INTENT_LAUNCHED")
                                    cameraLauncher.launch(uri)
                                } catch (e: Exception) {
                                    Log.e("CAMERA_AUDIT", "CAMERA_ERROR", e)
                                    val err = e.localizedMessage ?: e.message ?: "Unknown camera application error"
                                    Toast.makeText(context, "Could not start camera interface: $err", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.d("CAMERA_AUDIT", "CAMERA_PERMISSION_REQUESTED")
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                        border = BorderStroke(1.dp, GoldPrimary),
                        modifier = Modifier.testTag("capture_invoice_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Capture")
                    }
                }

                // Show thumbnail preview if image is attached
                invoicePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Invoice thumb",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { invoicePath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Delete photo", tint = Color.White)
                            }
                        }
                    }
                }

                // Buttons layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: 0.0
                            val p = priceText.toDoubleOrNull() ?: 0.0
                            if (name.isBlank() || w <= 0 || p <= 0) {
                                Toast.makeText(context, "Please fill out all fields with valid numbers", Toast.LENGTH_SHORT).show()
                            } else {
                                onAddOrEdit(name, category, purity, w, p, invoicePath)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = ObsidianBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_item_button")
                    ) {
                        Text("Save Asset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MarketSettingsDialog(
    settings: List<MarketSetting>,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var bullionFee by remember { mutableStateOf(settings.find { it.category == "BULLION" }?.defaultMakingFeePerGram?.toString() ?: "120.0") }
    var bullionCashback by remember { mutableStateOf(settings.find { it.category == "BULLION" }?.cashbackPercentage?.let { (it * 100).toString() } ?: "50") }

    var coinFee by remember { mutableStateOf(settings.find { it.category == "COIN" }?.defaultMakingFeePerGram?.toString() ?: "150.0") }
    var coinCashback by remember { mutableStateOf(settings.find { it.category == "COIN" }?.cashbackPercentage?.let { (it * 100).toString() } ?: "45") }

    var jewelryFee by remember { mutableStateOf(settings.find { it.category == "JEWELRY" }?.defaultMakingFeePerGram?.toString() ?: "280.0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Egypt Gold Making Fees & Cashback Settings",
                    color = GoldPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Adjust these values to fine-tune buying markup (مصنعية) and cashback (كاش باك) refunds relative to live index gold prices.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // BULLION SETTINGS
                Text("Bullions / سبيكة", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bullionFee,
                        onValueChange = { bullionFee = it },
                        label = { Text("Making Fee (EGP/g)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = bullionCashback,
                        onValueChange = { bullionCashback = it },
                        label = { Text("Cashback (%)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                // COIN SETTINGS
                Text("Gold Coins / جنيه ذهب", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = coinFee,
                        onValueChange = { coinFee = it },
                        label = { Text("Making Fee (EGP/g)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = coinCashback,
                        onValueChange = { coinCashback = it },
                        label = { Text("Cashback (%)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                // JEWELRY SETTINGS
                Text("Jewelry / مشغولات (Strict 0% Cashback)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = jewelryFee,
                    onValueChange = { jewelryFee = it },
                    label = { Text("Making Fee (EGP/g)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Close", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val bf = bullionFee.toDoubleOrNull() ?: 120.0
                            val bc = (bullionCashback.toDoubleOrNull() ?: 50.0) / 100.0

                            val cf = coinFee.toDoubleOrNull() ?: 150.0
                            val cc = (coinCashback.toDoubleOrNull() ?: 45.0) / 100.0

                            val jf = jewelryFee.toDoubleOrNull() ?: 280.0

                            onSave("BULLION", bf, bc)
                            onSave("COIN", cf, cc)
                            onSave("JEWELRY", jf, 0.0)

                            Toast.makeText(context, "Calculations recalibrated!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = ObsidianBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Configurations", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualPriceOverrideDialog(
    price24: Double,
    price21: Double,
    price18: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double) -> Unit
) {
    var p24 by remember { mutableStateOf(price24.toString()) }
    var p21 by remember { mutableStateOf(price21.toString()) }
    var p18 by remember { mutableStateOf(price18.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Manual Base Price Adjustment (EGP / g)",
                    color = GoldPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = p24,
                    onValueChange = { p24 = it },
                    label = { Text("24K Gold Price (per gram)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = p21,
                    onValueChange = { p21 = it },
                    label = { Text("21K Gold Price (per gram)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = p18,
                    onValueChange = { p18 = it },
                    label = { Text("18K Gold Price (per gram)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val v24 = p24.toDoubleOrNull() ?: price24
                            val v21 = p21.toDoubleOrNull() ?: price21
                            val v18 = p18.toDoubleOrNull() ?: price18
                            onSave(v24, v21, v18)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBackground)
                    ) {
                        Text("Save Manual Index")
                    }
                }
            }
        }
    }
}

@Composable
fun InvoicePreviewDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBackground),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Linked Purchase Invoice",
                        color = GoldPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = GoldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Invoice receipt zoom",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Photo stored locally on device.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
