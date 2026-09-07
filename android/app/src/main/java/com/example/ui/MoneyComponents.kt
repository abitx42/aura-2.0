package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.ShimmerMoneyOverviewCard
import com.example.ui.anim.ShimmerTransactionRow
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Money sub-sections enum
enum class MoneySubSection {
    Overview, Transactions, FriendsSplits, Investments, SavingsGoals, Analytics, Reminders
}

sealed class MoneyDetailSection {
    object AvailableBalancePassbook : MoneyDetailSection()
    object SplitsToReceive : MoneyDetailSection()
    data class FriendSplitDetails(val friend: Friend) : MoneyDetailSection()
    object SplitsYouOwe : MoneyDetailSection()
    data class FriendOweDetails(val friend: Friend) : MoneyDetailSection()
    object PortfolioInvestments : MoneyDetailSection()
}

@Composable
fun MoneyTrackerScreen(
    viewModel: AppViewModel
) {
    var activeSubSection by remember { mutableStateOf(MoneySubSection.Overview) }

    androidx.activity.compose.BackHandler(enabled = activeSubSection != MoneySubSection.Overview) {
        activeSubSection = MoneySubSection.Overview
    }

    // Money related state flows
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val isMoneyLoading by viewModel.isMoneyLoading.collectAsState()
    val investments by viewModel.allInvestments.collectAsState()
    val friends by viewModel.allFriends.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()

    // Dialog & Form states
    var showQuickTransactionSheet by remember { mutableStateOf<String?>(null) } // "SENT", "RECEIVED", "INVESTED", "CASH_ADDED"
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showSplitBillDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showBalanceAdjustmentDialog by remember { mutableStateOf<Account?>(null) }

    // Compute aggregated dynamic overview metrics
    val totalAvailableBalance = accounts.sumOf { it.balance }
    val totalInvested = investments.sumOf { it.amount }
    
    // Splitwise metrics
    val totalToReceive = debts.filter { !it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val totalYouOwe = debts.filter { it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val netWorth = totalAvailableBalance + totalInvested + totalToReceive - totalYouOwe

    val detailBackStack = remember { mutableStateListOf<MoneyDetailSection>() }
    val currentDetail = detailBackStack.lastOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopStart
    ) {
        if (currentDetail != null) {
            when (currentDetail) {
                is MoneyDetailSection.AvailableBalancePassbook -> {
                    AvailableBalancePassbookView(
                        accounts = accounts,
                        transactions = transactions,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onAdjustBalance = { showBalanceAdjustmentDialog = it },
                        onDeleteTransaction = { transactionToDelete = it },
                        onEditTransaction = {
                            editingTransaction = it
                            showQuickTransactionSheet = it.type
                        }
                    )
                }
                is MoneyDetailSection.SplitsToReceive -> {
                    SplitsToReceiveView(
                        friends = friends,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onFriendClick = { detailBackStack.add(MoneyDetailSection.FriendSplitDetails(it)) }
                    )
                }
                is MoneyDetailSection.FriendSplitDetails -> {
                    FriendSplitDetailsView(
                        friend = currentDetail.friend,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onQuickSettle = { viewModel.quickSettleDebt(it) }
                    )
                }
                is MoneyDetailSection.SplitsYouOwe -> {
                    SplitsYouOweView(
                        friends = friends,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onFriendClick = { detailBackStack.add(MoneyDetailSection.FriendOweDetails(it)) }
                    )
                }
                is MoneyDetailSection.FriendOweDetails -> {
                    FriendSplitDetailsView(
                        friend = currentDetail.friend,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onQuickSettle = { viewModel.quickSettleDebt(it) }
                    )
                }
                is MoneyDetailSection.PortfolioInvestments -> {
                    PortfolioInvestmentDetailsView(
                        investments = investments,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onAddInvestmentClick = { showAddInvestmentDialog = true },
                        onDeleteInvestment = { viewModel.deleteInvestment(it) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. PREMIUM HEADER ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Accounts",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = AuraTheme.colors.textPrimary
                            )
                            Text(
                                text = "Manage budgets and track source balances",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTheme.colors.textSecondary
                            )
                        }
                        
                        AuraHeaderActions(
                            onProClick = { viewModel.navigateTo(Section.SecuritySettings) },
                            onProfileClick = { viewModel.navigateTo(Section.SecuritySettings) }
                        )
                    }
                }

                // --- 2. STATS OVERVIEW CARD MATRIX ---
                if (isMoneyLoading) {
                    item {
                        ShimmerMoneyOverviewCard()
                    }
                    items(4) {
                        ShimmerTransactionRow()
                    }
                } else {
                    item {
                        PremiumFinancialOverviewCard(
                            netWorth = netWorth,
                            available = totalAvailableBalance,
                            invested = totalInvested,
                            toReceive = totalToReceive,
                            youOwe = totalYouOwe,
                            onBalanceClick = { detailBackStack.add(MoneyDetailSection.AvailableBalancePassbook) },
                            onInvestedClick = { detailBackStack.add(MoneyDetailSection.PortfolioInvestments) },
                            onToReceiveClick = { detailBackStack.add(MoneyDetailSection.SplitsToReceive) },
                            onYouOweClick = { detailBackStack.add(MoneyDetailSection.SplitsYouOwe) },
                            onNetWorthClick = { activeSubSection = MoneySubSection.Analytics }
                        )
                    }

                    // --- 3. QUICK ENGAGEMENT TRANSIT TOOLBAR ---
                    item {
                        QuickEngagementToolbar(
                            onSentClick = { showQuickTransactionSheet = "SENT" },
                            onReceivedClick = { showQuickTransactionSheet = "RECEIVED" },
                            onInvestedClick = { showQuickTransactionSheet = "INVESTED" },
                            onAddedCashClick = { showQuickTransactionSheet = "CASH_ADDED" }
                        )
                    }

                    // --- 4. NAVIGATION PILL SLIDERS ---
                    item {
                        val subSections = MoneySubSection.values().toList()
                        val labels = subSections.map { sub ->
                            when (sub) {
                                MoneySubSection.Overview -> "Overview"
                                MoneySubSection.Transactions -> "Ledger"
                                MoneySubSection.FriendsSplits -> "Splits & Friends"
                                MoneySubSection.Investments -> "Investments"
                                MoneySubSection.SavingsGoals -> "Savings Goals"
                                MoneySubSection.Analytics -> "Analytics Graphs"
                                MoneySubSection.Reminders -> "Reminders"
                            }
                        }
                        AuraPeriodSelector(
                            items = labels,
                            selectedIndex = subSections.indexOf(activeSubSection),
                            onItemSelected = { index -> activeSubSection = subSections[index] },
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // --- 5. RENDER CHOSEN MODULE ACTIVE VIEW ---
                when (activeSubSection) {
                    MoneySubSection.Overview -> {
                        // Interactive Mini sections dashboard
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Accounts List Card (Tapping card opens Available Balance Passbook)
                                AccountsSectionView(
                                    accounts = accounts,
                                    onAdjustBalance = { showBalanceAdjustmentDialog = it },
                                    onNavigateToPassbook = { detailBackStack.add(MoneyDetailSection.AvailableBalancePassbook) },
                                    transactions = transactions
                                )

                                // Split Settlement overview block (Tapping card opens Splits To Receive module)
                                SplitSummarySectionView(
                                    toReceive = totalToReceive,
                                    youOwe = totalYouOwe,
                                    debts = debts,
                                    onQuickSettle = { viewModel.quickSettleDebt(it) },
                                    onNavigateToSplits = { detailBackStack.add(MoneyDetailSection.SplitsToReceive) }
                                )

                                // Reminders Section Alert Board (Tapping card opens Reminders Sub-section timeline)
                                MiniRemindersView(
                                    reminders = reminders.filter { !it.isCompleted }.take(3),
                                    onSettle = { viewModel.toggleReminderCompleted(it) },
                                    onNavigateToReminders = { activeSubSection = MoneySubSection.Reminders }
                                )
                            }
                        }
                    }
                    MoneySubSection.Transactions -> {
                        item {
                            TransactionLedgerView(
                                transactions = transactions,
                                onDeleteClick = { transactionToDelete = it },
                                onEditClick = {
                                    editingTransaction = it
                                    showQuickTransactionSheet = it.type
                                }
                            )
                        }
                    }
                    MoneySubSection.FriendsSplits -> {
                        item {
                            FriendsAndSplitsModule(
                                friends = friends,
                                debts = debts,
                                viewModel = viewModel,
                                onAddFriendClick = { showAddFriendDialog = true },
                                onSplitBillClick = { showSplitBillDialog = true },
                                onQuickSettle = { viewModel.quickSettleDebt(it) },
                                onDeleteDebt = { viewModel.deleteDebt(it) },
                                onDeleteFriend = { viewModel.deleteFriend(it) }
                            )
                        }
                    }
                    MoneySubSection.Investments -> {
                        item {
                            InvestmentsPortfolioView(
                                investments = investments,
                                onAddInvestmentClick = { showAddInvestmentDialog = true },
                                onDeleteInvestment = { viewModel.deleteInvestment(it) }
                            )
                        }
                    }
                    MoneySubSection.SavingsGoals -> {
                        item {
                            SavingsGoalsView(
                                goals = savingsGoals,
                                onAddGoalClick = { showAddGoalDialog = true },
                                onSettleProgress = { viewModel.updateSavingsGoal(it) },
                                onDeleteGoal = { viewModel.deleteSavingsGoal(it) }
                            )
                        }
                    }
                    MoneySubSection.Analytics -> {
                        item {
                            VisualAnalyticsDashboard(
                                transactions = transactions,
                                investments = investments,
                                debts = debts,
                                goals = savingsGoals,
                                onCategoryClick = { activeSubSection = MoneySubSection.Transactions },
                                onCashflowClick = { detailBackStack.add(MoneyDetailSection.AvailableBalancePassbook) }
                            )
                        }
                    }
                    MoneySubSection.Reminders -> {
                        item {
                            AccountRemindersTimeline(
                                reminders = reminders,
                                onAddReminderClick = { showAddReminderDialog = true },
                                onToggleDone = { viewModel.toggleReminderCompleted(it) },
                                onDeleteClick = { viewModel.deleteReminder(it) }
                            )
                        }
                    }
                }
                
                // Safety Bottom Space
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Radiant Orange FAB for fast transaction entry (inspired by Reference Screenshot 1 & 2)
            AuraFloatingActionButton(
                onClick = { showQuickTransactionSheet = "SENT" },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
            )
        }
    }

    // --- FORM SHEET & ALERT DIALOGS POP-UPS ---

    // Quick transaction sheet (SENT, RECEIVED, INVESTED, CASH_ADDED)
    if (showQuickTransactionSheet != null) {
        val txType = showQuickTransactionSheet!!
        val sentOptions by viewModel.sentOptions.collectAsState()
        QuickTransactionBottomSheet(
            type = txType,
            accounts = accounts,
            friends = friends,
            sentOptions = sentOptions,
            onDismiss = {
                showQuickTransactionSheet = null
                editingTransaction = null
            },
            onSubmit = { amount, recipient, category, note, loc, method, acctId, tickedFriends, includeMe ->
                val editTx = editingTransaction
                if (editTx != null) {
                    viewModel.updateTransaction(
                        transactionId = editTx.id,
                        type = txType,
                        amount = amount,
                        recipientOrSender = recipient,
                        category = category,
                        note = note,
                        location = loc,
                        paymentMethod = method,
                        accountId = acctId,
                        dateString = editTx.dateString
                    )
                } else {
                    viewModel.addTransaction(
                        type = txType,
                        amount = amount,
                        recipientOrSender = recipient,
                        category = category,
                        note = note,
                        location = loc,
                        paymentMethod = method,
                        accountId = acctId
                    )
                }
                if (category == "Friend" && tickedFriends.isNotEmpty()) {
                    val shareCount = (tickedFriends.size + (if (includeMe) 1 else 0)).coerceAtLeast(1)
                    val splitShare = amount / shareCount
                    tickedFriends.forEach { fri ->
                        viewModel.addDebt(
                            friendId = fri.id,
                            friendName = fri.name,
                            title = note.ifBlank { "Shared Quick Expense" },
                            totalAmount = splitShare,
                            amount = splitShare,
                            isYouOwe = (txType == "RECEIVED")
                        )
                    }
                }
                showQuickTransactionSheet = null
                editingTransaction = null
            },
            transactionToEdit = editingTransaction
        )
    }

    // Safety delete confirmation dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = "Delete Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this transaction? This will revert its impact on your account balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraTheme.colors.textSecondary
                )
            },
            confirmButton = {
                AuraPrimaryAction(
                    text = "DELETE",
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    containerColor = AuraTheme.colors.negativeRed
                )
            },
            dismissButton = {
                AuraSecondaryAction(
                    text = "CANCEL",
                    onClick = { transactionToDelete = null }
                )
            },
            containerColor = AuraTheme.colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Balance Manual adjustment Dialog
    if (showBalanceAdjustmentDialog != null) {
        val acct = showBalanceAdjustmentDialog!!
        var adjustVal by remember { mutableStateOf(acct.balance.toString()) }
        AlertDialog(
            onDismissRequest = { showBalanceAdjustmentDialog = null },
            title = {
                Text(
                    text = "Update Available Balance",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set current liquid balance for ${acct.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary
                    )
                    OutlinedTextField(
                        value = adjustVal,
                        onValueChange = { adjustVal = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        label = { Text("Available Amount (₹)", color = AuraTheme.colors.accentBrand) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AuraPrimaryAction(
                    text = "SAVE ADJUSTMENT",
                    onClick = {
                        val amt = adjustVal.toDoubleOrNull() ?: acct.balance
                        viewModel.updateAccountBalance(acct.id, amt)
                        showBalanceAdjustmentDialog = null
                    },
                    containerColor = AuraTheme.colors.accentBrand
                )
            },
            dismissButton = {
                AuraSecondaryAction(
                    text = "CANCEL",
                    onClick = { showBalanceAdjustmentDialog = null }
                )
            },
            containerColor = AuraTheme.colors.cardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Add Investment Dialog
    if (showAddInvestmentDialog) {
        AddInvestmentFormDialog(
            onDismiss = { showAddInvestmentDialog = false },
            onSubmit = { name, type, amt, date, note ->
                viewModel.addInvestment(name, type, amt, date, note)
                showAddInvestmentDialog = false
            }
        )
    }

    // Add Friend profiles Dialog
    if (showAddFriendDialog) {
        AddFriendProfileDialog(
            onDismiss = { showAddFriendDialog = false },
            onSubmit = { name, phone, note ->
                viewModel.addFriend(name, phone, note)
                showAddFriendDialog = false
            }
        )
    }

    // Bill Splitting System dialog window
    if (showSplitBillDialog) {
        BillSplittingFormDialog(
            friends = friends,
            onDismiss = { showSplitBillDialog = false },
            onSubmit = { title, amt, isYouOwe, selectedParticipants, splitType, customPays ->
                // Automatically calculate and register debt transactions under participants
                val participantCount = (selectedParticipants.size + 1).coerceAtLeast(1) // + Me
                when (splitType) {
                    "EQUAL" -> {
                        val chunk = amt / participantCount
                        selectedParticipants.forEach { fri ->
                            viewModel.addDebt(
                                friendId = fri.id,
                                friendName = fri.name,
                                title = title,
                                totalAmount = chunk,
                                amount = chunk,
                                isYouOwe = isYouOwe
                            )
                        }
                    }
                    "CUSTOM", "PERCENTAGE" -> {
                        customPays.forEach { (friendId, customAmt) ->
                            val fri = friends.find { it.id == friendId } ?: return@forEach
                            viewModel.addDebt(
                                friendId = friendId,
                                friendName = fri.name,
                                title = title,
                                totalAmount = customAmt,
                                amount = customAmt,
                                isYouOwe = isYouOwe
                            )
                        }
                    }
                }
                showSplitBillDialog = false
            }
        )
    }

    // Add Savings Goal
    if (showAddGoalDialog) {
        AddSavingsGoalFormDialog(
            onDismiss = { showAddGoalDialog = false },
            onSubmit = { name, target, saved, date, notes ->
                viewModel.addSavingsGoal(name, target, saved, date, notes)
                showAddGoalDialog = false
            }
        )
    }

    // Add Reminders Alert
    if (showAddReminderDialog) {
        AddReminderFormDialog(
            onDismiss = { showAddReminderDialog = false },
            onSubmit = { title, amt, date, isRec, rec ->
                viewModel.addReminder(title, amt, date, isRec, rec)
                showAddReminderDialog = false
            }
        )
    }
}

// ===================================================
// COMPONENT: PREMIUM OVERVIEW STATS BANNERS
// ===================================================
@Composable
fun PremiumFinancialOverviewCard(
    netWorth: Double,
    available: Double,
    invested: Double,
    toReceive: Double,
    youOwe: Double,
    onBalanceClick: () -> Unit,
    onInvestedClick: () -> Unit,
    onToReceiveClick: () -> Unit,
    onYouOweClick: () -> Unit,
    onNetWorthClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                AuraTheme.colors.cardBorder,
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Net Worth Top Row (Clickable: opens Analytics Graphs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .auraSpringPress(
                        cornerRadius = 24.dp,
                        onClick = onNetWorthClick
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC NET WORTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuraTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "₹${"%,.2f".format(netWorth)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = AuraTheme.colors.textPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Dynamic balance calculation",
                    tint = AuraTheme.colors.accentBrand,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Numbered Statistics 2x2 Grid using AuraNumberedStat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuraNumberedStat(
                    index = "01",
                    label = "Liquid Balance",
                    value = "₹${"%,.0f".format(available)}",
                    accentColor = AuraTheme.colors.accentBrand,
                    onClick = onBalanceClick,
                    modifier = Modifier.weight(1f)
                )
                AuraNumberedStat(
                    index = "02",
                    label = "Investments",
                    value = "₹${"%,.0f".format(invested)}",
                    accentColor = AuraTheme.colors.gold,
                    onClick = onInvestedClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuraNumberedStat(
                    index = "03",
                    label = "To Receive",
                    value = "₹${"%,.0f".format(toReceive)}",
                    accentColor = AuraTheme.colors.positiveGreen,
                    onClick = onToReceiveClick,
                    modifier = Modifier.weight(1f)
                )
                AuraNumberedStat(
                    index = "04",
                    label = "You Owe",
                    value = "₹${"%,.0f".format(youOwe)}",
                    accentColor = AuraTheme.colors.negativeRed,
                    onClick = onYouOweClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ===================================================
// COMPONENT: QUICK ENGAGEMENT TRANSIT BAR + BUTTONS
// ===================================================
@Composable
fun QuickEngagementToolbar(
    onSentClick: () -> Unit,
    onReceivedClick: () -> Unit,
    onInvestedClick: () -> Unit,
    onAddedCashClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val actions = listOf(
            Triple("+ Sent", AuraTheme.colors.negativeRed, onSentClick),
            Triple("+ Recv", AuraTheme.colors.positiveGreen, onReceivedClick),
            Triple("+ Portf", AuraTheme.colors.gold, onInvestedClick),
            Triple("+ Cash", AuraTheme.colors.accentBrand, onAddedCashClick)
        )

        actions.forEach { (label, actionColor, callback) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .auraSpringPress(cornerRadius = 50.dp, onClick = callback)
                    .background(AuraTheme.colors.cardBackground)
                    .border(BorderStroke(1.dp, actionColor.copy(alpha = 0.4f)), RoundedCornerShape(50.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = actionColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ===================================================
// SIDE-BY-SIDE INCOMING VS OUTGOING SUMMARY CARDS
// Directly matching Reference Screenshot 2
// ===================================================
@Composable
fun ModernTransactionSummaryCards(
    incoming: Double,
    outgoing: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val shape = RoundedCornerShape(AuraCornerRadius.Card)

        // INCOMING CARD
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(AuraTheme.colors.cardBackground)
                .border(width = 1.dp, color = AuraTheme.colors.cardBorder, shape = shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "INCOMING",
                style = MaterialTheme.typography.labelSmall,
                color = AuraTheme.colors.positiveGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            )
            Text(
                text = "₹${"%,.0f".format(incoming)}",
                style = MaterialTheme.typography.titleLarge,
                color = AuraTheme.colors.positiveGreen,
                fontWeight = FontWeight.Black
            )
        }

        // OUTGOING CARD
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(AuraTheme.colors.cardBackground)
                .border(width = 1.dp, color = AuraTheme.colors.cardBorder, shape = shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "OUTGOING",
                style = MaterialTheme.typography.labelSmall,
                color = AuraTheme.colors.negativeRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            )
            Text(
                text = "₹${"%,.0f".format(outgoing)}",
                style = MaterialTheme.typography.titleLarge,
                color = AuraTheme.colors.negativeRed,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ===================================================
// SUB-MODULE: TRANSACTION JOURNAL LEDGER
// ===================================================
@Composable
fun TransactionLedgerView(
    transactions: List<Transaction>,
    onDeleteClick: (Transaction) -> Unit,
    onEditClick: (Transaction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("All") }
    var filterType by remember { mutableStateOf("All") }

    val totalIncoming = transactions.filter { it.type == "RECEIVED" || it.type == "CASH_ADDED" }.sumOf { it.amount }
    val totalOutgoing = transactions.filter { it.type == "SENT" || it.type == "INVESTED" }.sumOf { it.amount }

    val filteredList = transactions.filter { tx ->
        val matchesQuery = tx.recipientOrSender.contains(searchQuery, true) ||
                tx.category.contains(searchQuery, true) ||
                tx.note.contains(searchQuery, true) ||
                tx.amount.toString().contains(searchQuery)
        
        val matchesCategory = filterCategory == "All" || tx.category == filterCategory
        val matchesType = filterType == "All" || tx.type == filterType
        
        matchesQuery && matchesCategory && matchesType
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Summary Cards (INCOMING vs OUTGOING)
        ModernTransactionSummaryCards(
            incoming = totalIncoming,
            outgoing = totalOutgoing
        )

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TRANSACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...", color = AuraTheme.colors.textMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AuraTheme.colors.textMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category & Type sliders
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "SENT", "RECEIVED", "INVESTED").forEach { t ->
                        val isSel = filterType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .auraSpringPress(cornerRadius = 8.dp, onClick = { filterType = t })
                                .background(if (isSel) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t,
                                fontSize = 9.sp,
                                color = if (isSel) Color.White else AuraTheme.colors.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredList.isEmpty()) {
                    AuraEmptyState(
                        title = "No Transactions",
                        description = "No transactions found matching your criteria.",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = AuraTheme.colors.accentBrand
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredList.forEach { tx ->
                            val isIncome = tx.type == "RECEIVED" || tx.type == "CASH_ADDED"
                            val amountColor = if (isIncome) AuraTheme.colors.positiveGreen else AuraTheme.colors.negativeRed
                            val prefix = if (isIncome) "+" else "-"
                            val arrowIcon = if (isIncome) Icons.Default.SouthEast else Icons.Default.NorthEast

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AuraTheme.colors.bottomNavBackground.copy(alpha = 0.6f))
                                    .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .clickable { onEditClick(tx) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Squircle direction arrow container
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(amountColor.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = arrowIcon,
                                            contentDescription = tx.type,
                                            tint = amountColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = tx.recipientOrSender.ifBlank { "Transaction" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AuraTheme.colors.textPrimary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "👤 ${tx.category} · ${tx.paymentMethod.ifBlank { "Other" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AuraTheme.colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$prefix₹${"%,.0f".format(tx.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = amountColor,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = tx.dateString,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AuraTheme.colors.textMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteClick(tx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = AuraTheme.colors.negativeRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: PORTFOLIO INVESTMENTS TRACKER
// ===================================================
@Composable
fun InvestmentsPortfolioView(
    investments: List<Investment>,
    onAddInvestmentClick: () -> Unit,
    onDeleteInvestment: (Investment) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INVESTMENT PORTFOLIO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand, letterSpacing = 1.sp)
                Button(
                    onClick = onAddInvestmentClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("+ ADD PORTF", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (investments.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = "No portfolio", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(48.dp))
                    Text("No investments logged. Track stocks, mutual funds, or gold.", color = AuraTheme.colors.textMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            } else {
                investments.forEach { inv ->
                    val daysHeld = try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val parseDate = sdf.parse(inv.date) ?: Date()
                        val diffMs = System.currentTimeMillis() - parseDate.time
                        (diffMs / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    } catch (e: Exception) {
                        0L
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(AuraTheme.colors.gold.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                    Text(inv.type.uppercase(), fontSize = 8.sp, color = AuraTheme.colors.gold, fontWeight = FontWeight.Bold)
                                }
                                Text("Held: $daysHeld Days", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("₹${"%,.0f".format(inv.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.gold)
                            IconButton(onClick = { onDeleteInvestment(inv) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete investment", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: LIGHTWEIGHT SPLITWISE & BILL COMPOSERS
// ===================================================
@Composable
fun FriendsAndSplitsModule(
    friends: List<Friend>,
    debts: List<Debt>,
    viewModel: AppViewModel,
    onAddFriendClick: () -> Unit,
    onSplitBillClick: () -> Unit,
    onQuickSettle: (Debt) -> Unit,
    onDeleteDebt: (Debt) -> Unit,
    onDeleteFriend: (Friend) -> Unit
) {
    // Cloud Sync States
    val isSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val userEmail by viewModel.cloudUserEmail.collectAsState()
    val isSyncing by viewModel.isCurrentlySyncing.collectAsState()
    val lastSync by viewModel.lastSyncedTime.collectAsState()
    val backups by viewModel.mockCloudBackups.collectAsState()

    // Live Splits & Group Expense Rooms States
    val groupRooms by viewModel.groupRooms.collectAsState()
    val roomExpenses by viewModel.roomExpenses.collectAsState()
    val socialActivities by viewModel.socialActivities.collectAsState()

    // Interactive Dialog States
    var activeLedgerTab by remember { mutableStateOf("1-ON-1") } // "1-ON-1", "ROOMS", "SOCIAL"
    var showGoogleSignDialog by remember { mutableStateOf(false) }
    var inviteFriendItem by remember { mutableStateOf<Friend?>(null) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var showAddRoomExpenseDialog by remember { mutableStateOf(false) }
    var activeReceiptPath by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        
        // ==========================================
        // SECTION 1: AURA CLINICAL CLOUD SYNC & BACKUP
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, if (isSyncEnabled) AuraTheme.colors.accentBrand.copy(alpha = 0.5f) else AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue, 
                            contentDescription = "Cloud Icon", 
                            tint = if (isSyncEnabled) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("AURA CLOUD BACKUP & SYNCHRONIZER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary, letterSpacing = 0.5.sp)
                            Text(
                                text = if (isSyncEnabled) "Cloud Sync: Connected to Google" else "Offline Local Storage Only",
                                fontSize = 9.sp,
                                color = if (isSyncEnabled) AuraTheme.colors.positiveGreen else AuraTheme.colors.textMuted
                            )
                        }
                    }

                    // Syncing Spinner or Connected Indicator
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AuraTheme.colors.accentBrand, strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isSyncEnabled) AuraTheme.colors.positiveGreen else AuraTheme.colors.gold, 
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (userEmail == null) {
                    // Sign-In Visual Call-to-Action
                    Text(
                        text = "Sign in to activate encrypted Drive backups & synchronize shared transaction splits with friends in real time.",
                        fontSize = 10.sp,
                        color = AuraTheme.colors.textSecondary,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showGoogleSignDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("google_signin_trigger"),
                        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("G", color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("Connect Google Workspace Account", color = AuraTheme.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Google Signed-In Visual Interface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Avatar Icon representing G-Profile
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(AuraTheme.colors.accentBrand.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = AuraTheme.colors.accentBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(userEmail ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                Text("Mirror Last Sync: $lastSync", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                            }
                        }

                        // Disconnect link
                        Text(
                            text = "Disconnect",
                            color = AuraTheme.colors.negativeRed.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.signOut() }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerSyncNow() },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync icon", tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SYNC NOW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.createGoogleDriveBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = "Backup icon", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BACKUP DRIVE", color = AuraTheme.colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Collapsible Backup List
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("GOOGLE DRIVE RESTORE POINTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    backups.take(2).forEach { bkp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bkp, fontSize = 10.sp, color = AuraTheme.colors.textSecondary)
                            Text(
                                text = "RESTORE",
                                color = AuraTheme.colors.accentBrand,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.triggerSyncNow()
                                            android.widget.Toast.makeText(context, "Data restoration index synced safely!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: CHROME NAVIGATION DIAL TABS
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .background(AuraTheme.colors.cardBackground, RoundedCornerShape(12.dp))
                .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("1-ON-1" to "INDIVIDUAL SPLITS", "ROOMS" to "GROUP ROOMS 🏖️", "SOCIAL" to "SOCIAL FEED 💬")
            tabs.forEach { (key, title) ->
                val active = activeLedgerTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) AuraTheme.colors.bottomNavBackground else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (active) AuraTheme.colors.cardBorder else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeLedgerTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // TAB CONTROLLER ACTIONS
        // ==========================================
        when (activeLedgerTab) {
            "1-ON-1" -> {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1-ON-1 SPLITS LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = onAddFriendClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    border = BorderStroke(1.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.5f))
                                ) {
                                    Text("+ FRIEND", color = AuraTheme.colors.accentBrand, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onSplitBillClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("SPLIT BILL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("FRIENDS & CLOUD PROFILE CONNECTIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (friends.isEmpty()) {
                            Text("No friends available.", color = AuraTheme.colors.textMuted, fontSize = 11.sp)
                        } else {
                            friends.forEach { fri ->
                                val friendDebts = debts.filter { it.friendId == fri.id && it.status == "PENDING" }
                                val toRecVal = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
                                val oweVal = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
                                val netBal = toRecVal - oweVal

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                                        .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(fri.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                            // Tiny glowing connected indicator representing real-time state
                                            Box(modifier = Modifier.size(6.dp).background(AuraTheme.colors.positiveGreen, CircleShape))
                                        }
                                        if (fri.phone.isNotEmpty()) {
                                            Text("Link ID: ${fri.phone}", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val netColor = if (netBal > 0) AuraTheme.colors.positiveGreen else if (netBal < 0) AuraTheme.colors.negativeRed else AuraTheme.colors.textPrimary
                                        val netLabel = if (netBal > 0) "Owed: ₹${netBal.toInt()}" else if (netBal < 0) "Owe: ₹${(-netBal).toInt()}" else "Settled"
                                        
                                        Text(netLabel, fontSize = 11.sp, color = netColor, fontWeight = FontWeight.Bold)
                                        
                                        // Invite button
                                        IconButton(
                                            onClick = { inviteFriendItem = fri }, 
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Invite friend code", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(13.dp))
                                        }

                                        IconButton(onClick = { onDeleteFriend(fri) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.PersonRemove, contentDescription = "Delete friend", tint = AuraTheme.colors.textMuted.copy(alpha=0.6f), modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("OPEN BILL SPLITS (DEBTS)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        val pendingDebts = debts.filter { it.status == "PENDING" }
                        if (pendingDebts.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("All individual splits settled! Great.", color = AuraTheme.colors.positiveGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            pendingDebts.forEach { dbt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                        .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dbt.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "${if (dbt.isYouOwe) "You owe" else "Owes you"} ${dbt.friendName}",
                                                fontSize = 9.sp,
                                                color = AuraTheme.colors.textMuted
                                            )
                                            if (dbt.isSynced) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Synced", tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "₹${dbt.remainingAmount.toInt()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (dbt.isYouOwe) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen
                                        )

                                        Button(
                                            onClick = { onQuickSettle(dbt) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (dbt.isYouOwe) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("SETTLE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ROOMS" -> {
                // ==========================================
                // TAB 2: GROUP EXPENSE SPLITWISE ROOMS (PHASE 3)
                // ==========================================
                if (selectedRoomId == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("COGNITIVE EXPENSE ROOMS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand)
                                Button(
                                    onClick = { showCreateRoomDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ CREATE ROOM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (groupRooms.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = AuraTheme.colors.textMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "No shared expense rooms configured. Create a room to manage Splitwise-style travel or room bills.",
                                        color = AuraTheme.colors.textMuted,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                groupRooms.forEach { room ->
                                    val expenses = roomExpenses.filter { it.roomId == room.id }
                                    val totalAmt = expenses.sumOf { it.amount }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                            .clickable { selectedRoomId = room.id }
                                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(AuraTheme.colors.cardBackground, RoundedCornerShape(8.dp))
                                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(room.emoji, fontSize = 18.sp)
                                            }

                                            Column {
                                                Text(room.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                                Text(
                                                    text = "${room.memberNames.size} members • ${room.memberNames.joinToString(", ")}",
                                                    fontSize = 8.sp,
                                                    color = AuraTheme.colors.textMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${totalAmt.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand)
                                            Text("Total Spent", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Room Details Pane
                    val roomId = selectedRoomId!!
                    val roomObj = groupRooms.find { it.id == roomId }
                    if (roomObj == null) {
                        selectedRoomId = null
                    } else {
                        val expenses = roomExpenses.filter { it.roomId == roomId }
                        val totalAmt = expenses.sumOf { it.amount }
                        val minSettlements = viewModel.getMinimizeTransactionsForRoom(roomId)

                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Room Header Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(roomObj.emoji, fontSize = 20.sp)
                                        Column {
                                            Text(roomObj.name.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary)
                                            Text("${roomObj.memberNames.size} connected members", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                                        }
                                    }

                                    Text(
                                        "BACK TO LIST",
                                        color = AuraTheme.colors.accentBrand,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { selectedRoomId = null }
                                            .padding(6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                                        .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("TOTAL ROOM SPEND", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                                        Text("₹${totalAmt.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand)
                                    }
                                    Button(
                                        onClick = { showAddRoomExpenseDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ ADD EXPENSE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 1. SMART SETTLEMENT ALGORITHM SOLVER
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("SMART DEBT-MINIMIZATION RECOMMENDATIONS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.gold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (minSettlements.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, AuraTheme.colors.positiveGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("🎉 Group is fully settled up! Zero balances outstanding.", color = AuraTheme.colors.positiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    minSettlements.forEach { (debtor, creditor, amt) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                                                .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(debtor, fontWeight = FontWeight.Bold, color = AuraTheme.colors.negativeRed, fontSize = 11.sp)
                                                Text("owes", color = AuraTheme.colors.textMuted, fontSize = 10.sp)
                                                Text(creditor, fontWeight = FontWeight.Bold, color = AuraTheme.colors.positiveGreen, fontSize = 11.sp)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("₹${amt.toInt()}", fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary, fontSize = 12.sp)
                                                Button(
                                                    onClick = { 
                                                        viewModel.settleGroupDebt(roomId, debtor, creditor, amt)
                                                        android.widget.Toast.makeText(context, "Settlement registered!", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.cardBackground),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.5f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("SETTLE DIRECT", color = AuraTheme.colors.accentBrand, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Room Expense history
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("EXPENSES CHRONIC TIMELINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (expenses.isEmpty()) {
                                    Text("No expenses logged in this room yet.", color = AuraTheme.colors.textMuted, fontSize = 10.sp)
                                } else {
                                    expenses.forEach { exp ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                                .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(exp.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                                Text("Paid by ${exp.paidByName}", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                                            }
                                            Text("₹${exp.amount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "SOCIAL" -> {
                // ==========================================
                // TAB 3: SOCIAL REACTION HUB (PHASE 4)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("REAL-TIME ACTIVITIES & FEED", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand)
                        Spacer(modifier = Modifier.height(10.dp))

                        socialActivities.forEach { act ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Initials Avatar
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(AuraTheme.colors.accentBrand.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(act.userName.take(2).uppercase(), color = AuraTheme.colors.accentBrand, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(act.userName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.US).format(Date(act.timestamp)),
                                            fontSize = 8.sp,
                                            color = AuraTheme.colors.textMuted
                                        )
                                    }

                                    Text(act.text, fontSize = 11.sp, color = AuraTheme.colors.textSecondary)

                                    // Attachment Display (UPI Payment Screenshot simulation)
                                    if (act.receiptPath != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { activeReceiptPath = act.receiptPath },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.cardBackground),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.AttachFile, contentDescription = "attachment", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("UPI_RECEIPT_PROOF.PNG", fontSize = 8.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Predefined Emoji Reactions Row
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val reactions = listOf("👍", "👀", "😮", "✅")
                                        reactions.forEach { emoji ->
                                            val isReacted = act.emojiReaction == emoji
                                            Box(
                                                modifier = Modifier
                                                    .border(
                                                        1.dp, 
                                                        if (isReacted) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder.copy(alpha = 0.5f), 
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .background(
                                                        if (isReacted) AuraTheme.colors.accentBrand.copy(alpha = 0.2f) else AuraTheme.colors.cardBackground, 
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.addSocialActivity(
                                                            userName = "Me",
                                                            text = "reacted with ${emoji} on \"${act.userName}\" split activity",
                                                            type = "REACTION",
                                                            emojiReaction = emoji
                                                        )
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(emoji, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG POPUPS & SHEETS CONTROLLERS
    // ==========================================

    // 1. Google Account picker Simulation dialog window
    if (showGoogleSignDialog) {
        var customEmailInput by remember { mutableStateOf("moreaboutastram@gmail.com") }

        AlertDialog(
            onDismissRequest = { showGoogleSignDialog = false },
            title = { Text("GOOGLE CLOUD ACCESS LINK", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select or enter a Google account credentials to link for secure sync mirroring:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = customEmailInput,
                        onValueChange = { customEmailInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        placeholder = { Text("example@gmail.com", color = AuraTheme.colors.textMuted) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signInWithGoogle(customEmailInput)
                        showGoogleSignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("AUTHORIZE & SYNC", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showGoogleSignDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                ) {
                    Text("CANCEL", color = AuraTheme.colors.textSecondary)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }

    // 2. Peer Friend Invitation Link Dialog Drawer
    if (inviteFriendItem != null) {
        val friObj = inviteFriendItem!!
        val linkText = "https://auranotes.app/invite?from=usr_${friObj.id}"

        AlertDialog(
            onDismissRequest = { inviteFriendItem = null },
            title = { Text("PEER CLOUD CONNECT INVITE", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Share this secure entry connect URL with ${friObj.name} to sync all your split balances in real time", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    
                    // Web URL link text field box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(linkText, color = AuraTheme.colors.accentBrand, fontSize = 11.sp, maxLines = 2)
                    }

                    // Graphical matrix representation of QR Code matching specifications
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(1.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        // Drawing QR grid squares
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(6) { indexRow ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(6) { indexCol ->
                                        val fillSquare = (indexRow + indexCol) % 2 == 0 || (indexRow < 2 && indexCol < 2) || (indexRow > 3 && indexCol > 3)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(if (fillSquare) Color.Black else Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Copy to clipboard
                        inviteFriendItem = null
                        android.widget.Toast.makeText(context, "Copied connection invite link!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("COPY URL & CONNECTIONS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }

    // 3. Group Room Creator Dialog Setup
    if (showCreateRoomDialog) {
        var roomNameInput by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("🏖️") }
        val emojis = listOf("🏖️", "🏡", "🍕", "🚗", "💼", "🍿")
        val checkedFriends = remember { mutableStateMapOf<String, Boolean>() }

        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("NEW GROUP ROOM SETUP", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Enter Group Room title details:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        placeholder = { Text("e.g. Flatmates Bills, Goa 2026", color = AuraTheme.colors.textMuted) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose Room Visual Emoji:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        emojis.forEach { emoji ->
                            val isSel = selectedEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(if (isSel) AuraTheme.colors.accentBrand.copy(alpha = 0.2f) else AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedEmoji = emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select Group Members Checklist:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    
                    if (friends.isEmpty()) {
                        Text("Please create Friend entry items before setting group rooms.", color = AuraTheme.colors.textMuted, fontSize = 10.sp)
                    } else {
                        friends.forEach { fri ->
                            val isChecked = checkedFriends[fri.name] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkedFriends[fri.name] = !isChecked }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked, 
                                    onCheckedChange = { checkedFriends[fri.name] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = AuraTheme.colors.accentBrand)
                                )
                                Text(fri.name, color = AuraTheme.colors.textPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeMembers = checkedFriends.filter { it.value }.map { it.key }
                        if (roomNameInput.isNotBlank()) {
                            viewModel.createGroupRoom(roomNameInput, selectedEmoji, activeMembers)
                        }
                        showCreateRoomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("CREATE ROOM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateRoomDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                ) {
                    Text("CANCEL", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }

    // 4. Create Group Expense Dialog Form
    if (showAddRoomExpenseDialog && selectedRoomId != null) {
        val activeRoom = groupRooms.find { it.id == selectedRoomId }
        if (activeRoom != null) {
            var expenseTitleInput by remember { mutableStateOf("") }
            var expenseAmountInput by remember { mutableStateOf("") }
            var expensePaidBy by remember { mutableStateOf("Me") }
            var customSplitStrategy by remember { mutableStateOf("EQUAL") }

            AlertDialog(
                onDismissRequest = { showAddRoomExpenseDialog = false },
                title = { Text("ADD GROUP ROOM EXPENSE", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text("Expense Title:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                        OutlinedTextField(
                            value = expenseTitleInput,
                            onValueChange = { expenseTitleInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            ),
                            placeholder = { Text("e.g. Resort Booking / Lunch", color = AuraTheme.colors.textMuted) }
                        )

                        Text("Total Bill Amount (₹):", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                        OutlinedTextField(
                            value = expenseAmountInput,
                            onValueChange = { expenseAmountInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            ),
                            placeholder = { Text("e.g. 1800", color = AuraTheme.colors.textMuted) }
                        )

                        Text("Who Paid Initially:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            activeRoom.memberNames.forEach { mbr ->
                                val isPaid = expensePaidBy == mbr
                                Box(
                                    modifier = Modifier
                                        .background(if (isPaid) AuraTheme.colors.accentBrand.copy(alpha = 0.2f) else AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isPaid) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                        .clickable { expensePaidBy = mbr }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(mbr, color = if (isPaid) AuraTheme.colors.accentBrand else AuraTheme.colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text("Split Proportion strategy:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val strategies = listOf("EQUAL", "CUSTOM")
                            strategies.forEach { strat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { customSplitStrategy = strat }
                                ) {
                                    RadioButton(
                                        selected = customSplitStrategy == strat,
                                        onClick = { customSplitStrategy = strat },
                                        colors = RadioButtonDefaults.colors(selectedColor = AuraTheme.colors.accentBrand)
                                    )
                                    Text(strat, color = AuraTheme.colors.textPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val billTotalValue = expenseAmountInput.toDoubleOrNull() ?: 0.0
                            if (billTotalValue > 0.0 && expenseTitleInput.isNotBlank()) {
                                // EQUAL Chunk calculators
                                val partitionCount = activeRoom.memberNames.size.coerceAtLeast(1)
                                val mapSplits = activeRoom.memberNames.associateWith { billTotalValue / partitionCount }
                                viewModel.addRoomExpense(
                                    roomId = activeRoom.id,
                                    title = expenseTitleInput,
                                    amount = billTotalValue,
                                    paidByName = expensePaidBy,
                                    splits = mapSplits
                                )
                            }
                            showAddRoomExpenseDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                    ) {
                        Text("RECORD EXPENSE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showAddRoomExpenseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                    ) {
                        Text("CANCEL", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    }
                },
                containerColor = AuraTheme.colors.cardBackground
            )
        }
    }

    // 5. Beautiful UPI screenshot billing proof confirmation Dialog overlay
    if (activeReceiptPath != null) {
        val receipt = activeReceiptPath!!

        AlertDialog(
            onDismissRequest = { activeReceiptPath = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "verified security", tint = MoodHappy, modifier = Modifier.size(18.dp))
                    Text("VERIFIED UPI RECEIPT PROOF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.bottomNavBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MoodHappy.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₹", color = MoodHappy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Text("TRANSFER TRANSACTION SUCCESSFUL", color = AuraTheme.colors.positiveGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)

                        HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recipient Name:", color = AuraTheme.colors.textMuted, fontSize = 9.sp)
                            Text("Sahil (Roommate)", color = AuraTheme.colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount Settled:", color = AuraTheme.colors.textMuted, fontSize = 9.sp)
                            Text("₹450.00", color = AuraTheme.colors.accentBrand, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UPI Reference No:", color = AuraTheme.colors.textMuted, fontSize = 9.sp)
                            Text("618295039203", color = AuraTheme.colors.textPrimary, fontSize = 10.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Timestamp:", color = AuraTheme.colors.textMuted, fontSize = 9.sp)
                            Text("Jun 5, 2026, 23:18:57 UTC", color = AuraTheme.colors.textPrimary, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓ SIGNED SECURE & AUDITED VIA F-SECURE", color = AuraTheme.colors.accentBrand, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeReceiptPath = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("DONE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }
}

// ===================================================
// SUB-MODULE: SAVINGS PLANNER GOALS
// ===================================================
@Composable
fun SavingsGoalsView(
    goals: List<SavingsGoal>,
    onAddGoalClick: () -> Unit,
    onSettleProgress: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SAVINGS GOALS & PLANS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand, letterSpacing = 1.sp)
                Button(
                    onClick = onAddGoalClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("+ ADD GOAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (goals.isEmpty()) {
                AuraEmptyState(
                    title = "No Savings Goals Set",
                    description = "Create target plans and track milestones for gadgets, trips, or emergency funds.",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = AuraTheme.colors.gold
                )
            } else {
                goals.forEach { gol ->
                    val percentage = if (gol.targetAmount > 0) {
                        (gol.savedAmount / gol.targetAmount * 100).coerceAtMost(100.0)
                    } else 0.0

                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(shape)
                            .background(AuraTheme.colors.bottomNavBackground)
                            .border(1.dp, AuraTheme.colors.cardBorder, shape)
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(gol.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                                Text("Target: ${gol.targetDate}", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Progress bar
                            val progressFrac = (percentage / 100.0).toFloat().coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AuraTheme.colors.cardBackground)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFrac.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (progressFrac >= 1f) AuraTheme.colors.positiveGreen else AuraTheme.colors.accentBrand)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Saved: ₹${gol.savedAmount.roundToInt()} of ₹${gol.targetAmount.roundToInt()} (${percentage.roundToInt()}%)",
                                    fontSize = 11.sp,
                                    color = AuraTheme.colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Settle/Add savings increment button
                                    IconButton(
                                        onClick = {
                                            val increment = 1000.0
                                            onSettleProgress(gol.copy(savedAmount = (gol.savedAmount + increment).coerceAtMost(gol.targetAmount)))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add savings", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    IconButton(
                                        onClick = { onDeleteGoal(gol) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete target", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: RECURRING ALARMS & REMINDERS
// ===================================================
@Composable
fun AccountRemindersTimeline(
    reminders: List<MoneyReminder>,
    onAddReminderClick: () -> Unit,
    onToggleDone: (MoneyReminder) -> Unit,
    onDeleteClick: (MoneyReminder) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECURRING REMINDERS & EMIs", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand, letterSpacing = 1.sp)
                Button(
                    onClick = onAddReminderClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("+ EMIs / BILLS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (reminders.isEmpty()) {
                Text("No EMIs, rent, subscriptions, or split schedules set.", color = AuraTheme.colors.textMuted, fontSize = 11.sp)
            } else {
                reminders.forEach { rem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = rem.isCompleted,
                                onCheckedChange = { onToggleDone(rem) },
                                colors = CheckboxDefaults.colors(checkedColor = AuraTheme.colors.accentBrand)
                            )
                            Column {
                                Text(
                                    text = rem.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rem.isCompleted) AuraTheme.colors.textMuted else AuraTheme.colors.textPrimary,
                                    textDecoration = if (rem.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = "${if (rem.isRecurring) "Recurring ${rem.recurrence}" else "One-time"} • Due: ${rem.dueDate}",
                                    fontSize = 10.sp,
                                    color = AuraTheme.colors.textMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "₹${"%,.0f".format(rem.amount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (rem.isCompleted) AuraTheme.colors.textMuted else AuraTheme.colors.gold
                            )
                            IconButton(onClick = { onDeleteClick(rem) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete alarm", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: NATIVE JETPACK COMPOSE ANALYTICS GRAPHS
// ===================================================
@Composable
fun VisualAnalyticsDashboard(
    transactions: List<Transaction>,
    investments: List<Investment>,
    debts: List<Debt>,
    goals: List<SavingsGoal>,
    onCategoryClick: ((String) -> Unit)? = null,
    onCashflowClick: (() -> Unit)? = null
) {
    // Math category totals
    val categoryTotals = transactions.groupBy { it.category }
        .mapValues { (_, txList) -> txList.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalSentimentExpenses = categoryTotals.sumOf { it.second }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("FINTECH VISUAL ANALYTICS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.accentBrand, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (categoryTotals.isEmpty()) {
                AuraEmptyState(
                    title = "No Analytics Data Yet",
                    description = "Add more transactions or investments to visualize spending share and portfolio distribution.",
                    icon = Icons.Default.Analytics,
                    iconTint = AuraTheme.colors.accentBrand
                )
            } else {
                Text("COGNITIVE SPENDING CATEGORY SHARE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Drawing Pie Category Share Chart (Clicking opens Category Ledger search)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (onCategoryClick != null) it.clickable { onCategoryClick("ALL") } else it },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val colors = listOf(
                        AuraTheme.colors.accentBrand,
                        AuraTheme.colors.gold,
                        AuraTheme.colors.positiveGreen,
                        AuraTheme.colors.negativeRed,
                        Color(0xFF7C4DFF),
                        Color(0xFF29B6F6),
                        Color(0xFFBA68C8)
                    )
                    
                    Canvas(
                        modifier = Modifier
                            .size(120.dp)
                            .clearAndSetSemantics { 
                                contentDescription = "Pie chart displaying expense category breakdown." 
                            }
                    ) {
                        var startAngle = 0f
                        categoryTotals.forEachIndexed { idx, pair ->
                            val sweepAngle = if (totalSentimentExpenses > 0) {
                                ((pair.second / totalSentimentExpenses) * 360f).toFloat()
                            } else 0f
                            drawArc(
                                color = colors[idx % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Legend values
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        categoryTotals.take(5).forEachIndexed { idx, pair ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(colors[idx % colors.size], RoundedCornerShape(2.dp)))
                                Text(
                                    text = "${pair.first}: ${if (totalSentimentExpenses > 0) ((pair.second / totalSentimentExpenses) * 100).roundToInt() else 0}%",
                                    fontSize = 11.sp,
                                    color = AuraTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Income vs Expenses custom layout (Clicking opens Bank Passbook details)
                val sentAmt = transactions.filter { it.type == "SENT" || it.type == "INVESTED" }.sumOf { it.amount }
                val recvAmt = transactions.filter { it.type == "RECEIVED" || it.type == "CASH_ADDED" }.sumOf { it.amount }
                val savingsRate = if (recvAmt > 0) ((recvAmt - sentAmt) / recvAmt * 100).coerceIn(0.0..100.0) else 0.0

                Text("NET CASHFLOW INSIGHTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (onCashflowClick != null) it.clickable { onCashflowClick() } else it },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, AuraTheme.colors.positiveGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL RECORDED INCOME", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                        Text("₹${"%,.0f".format(recvAmt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.positiveGreen)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, AuraTheme.colors.negativeRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL RECORDED DEBITS", fontSize = 8.sp, color = AuraTheme.colors.textMuted)
                        Text("₹${"%,.0f".format(sentAmt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.negativeRed)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your net savings rate this month is ${savingsRate.roundToInt()}%. Keep it up!",
                    fontSize = 11.sp,
                    color = AuraTheme.colors.accentBrand,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Modern Account Card directly inspired by Reference Screenshot 1
@Composable
fun ModernAccountCard(
    account: Account,
    onAdjustBalance: (Account) -> Unit,
    modifier: Modifier = Modifier,
    transactions: List<Transaction> = emptyList()
) {
    val shape = RoundedCornerShape(AuraCornerRadius.Hero)
    
    // Dynamically compute real spent & income values for this account
    val accountTxns = remember(transactions, account.id) {
        transactions.filter { it.accountId == account.id }
    }
    val usedAmount = remember(accountTxns) {
        accountTxns.filter { it.type == "SENT" || it.type == "INVESTED" }.sumOf { it.amount }
    }
    val incomeAmount = remember(accountTxns) {
        accountTxns.filter { it.type == "RECEIVED" || it.type == "CASH_ADDED" }.sumOf { it.amount }
    }
    val personalSpent = remember(accountTxns) {
        accountTxns.filter { it.type == "SENT" && it.category != "Shared" }.sumOf { it.amount }
    }
    val sharedSpent = remember(accountTxns) {
        accountTxns.filter { it.type == "SENT" && it.category == "Shared" }.sumOf { it.amount }
    }

    val availableAmount = account.balance
    val totalDenom = usedAmount + availableAmount
    val progressFraction = if (totalDenom > 0.0) (usedAmount / totalDenom).toFloat().coerceIn(0f, 1f) else 0f
    val progressPercent = (progressFraction * 100).toInt()

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "budgetUsageProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AuraTheme.colors.cardBackground)
            .border(width = 1.dp, color = AuraTheme.colors.cardBorder, shape = shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Row: Icon + Title + Default Badge + Edit Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wallet squircle icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraTheme.colors.accentBrand.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary
                        )
                        if (account.isDefault) {
                            AuraDefaultBadge()
                        }
                    }
                    Text(
                        text = "BANK",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuraTheme.colors.textMuted,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    )
                }
            }

            // Edit button in circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .auraSpringPress(
                        cornerRadius = 50.dp,
                        onClick = { onAdjustBalance(account) }
                    )
                    .background(AuraTheme.colors.bottomNavBackground)
                    .border(width = 1.dp, color = AuraTheme.colors.cardBorder, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Balance",
                    tint = AuraTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Middle Row: 2-Column Split (USED vs AVAILABLE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "USED",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTheme.colors.negativeRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
                Text(
                    text = "₹${"%,.0f".format(usedAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraTheme.colors.negativeRed,
                    fontWeight = FontWeight.Black
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "AVAILABLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTheme.colors.positiveGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
                Text(
                    text = "₹${"%,.0f".format(availableAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraTheme.colors.positiveGreen,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Budget usage progress track
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BUDGET USAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTheme.colors.textMuted,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                )
                Text(
                    text = "$progressPercent% Used",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progressPercent > 80) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AuraTheme.colors.bottomNavBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.01f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (progressPercent > 80) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen)
                )
            }
        }

        // Footer: INCOME · PERSONAL · SHARED
        Text(
            text = "INCOME ₹${"%,.0f".format(incomeAmount.ifZero(availableAmount))} · PERSONAL ₹${"%,.0f".format(personalSpent)} · SHARED ₹${"%,.0f".format(sharedSpent)}",
            style = MaterialTheme.typography.labelSmall,
            color = AuraTheme.colors.textMuted,
            letterSpacing = 1.sp,
            fontSize = 9.sp
        )
    }
}

private fun Double.ifZero(default: Double): Double = if (this == 0.0) default else this

// Mini view helper: Accounts summary
@Composable
fun AccountsSectionView(
    accounts: List<Account>,
    onAdjustBalance: (Account) -> Unit,
    onNavigateToPassbook: (() -> Unit)? = null,
    transactions: List<Transaction> = emptyList()
) {
    if (accounts.isEmpty()) {
        AuraEmptyState(
            title = "No Accounts Found",
            description = "Tap '+' to add your first bank account or wallet.",
            icon = Icons.Default.AccountBalanceWallet,
            iconTint = AuraTheme.colors.accentBrand
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            accounts.forEach { acct ->
                ModernAccountCard(
                    account = acct,
                    onAdjustBalance = onAdjustBalance,
                    transactions = transactions
                )
            }
        }
    }
}

// Mini view helper: Splits settlements Overview
@Composable
fun SplitSummarySectionView(
    toReceive: Double,
    youOwe: Double,
    debts: List<Debt>,
    onQuickSettle: (Debt) -> Unit,
    onNavigateToSplits: (() -> Unit)? = null
) {
    val pending = debts.filter { d -> d.status == "PENDING" }
    if (pending.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                .let { if (onNavigateToSplits != null) it.clickable { onNavigateToSplits() } else it },
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PENDING SPLITS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary, letterSpacing = 1.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Net: ₹${(toReceive - youOwe).roundToInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.accentBrand)
                        if (onNavigateToSplits != null) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Open Splits Module",
                                tint = AuraTheme.colors.accentBrand,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                pending.take(3).forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(d.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Text("${if (d.isYouOwe) "Owe" else "Owes you"} ${d.friendName}", fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${d.remainingAmount.roundToInt()}", fontSize = 11.sp, color = if (d.isYouOwe) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen)
                            TextButton(
                                onClick = { onQuickSettle(d) },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("SETTLE", fontSize = 9.sp, color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Mini reminders widget helper
@Composable
fun MiniRemindersView(
    reminders: List<MoneyReminder>,
    onSettle: (MoneyReminder) -> Unit,
    onNavigateToReminders: (() -> Unit)? = null
) {
    if (reminders.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                .let { if (onNavigateToReminders != null) it.clickable { onNavigateToReminders() } else it },
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UPCOMING SUBS/BILLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textSecondary, letterSpacing = 1.sp)
                    if (onNavigateToReminders != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Reminders",
                            tint = AuraTheme.colors.accentBrand,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                reminders.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(r.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Text("Due: ${r.dueDate}", fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${r.amount.roundToInt()}", fontSize = 11.sp, color = AuraTheme.colors.gold, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onSettle(r) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Done, contentDescription = "Mark done", tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// DETAILED COMPOSE SHEETS AND MODAL CONSTRUCTORS
// ===================================================

@Composable
fun QuickTransactionBottomSheet(
    type: String,
    accounts: List<Account>,
    friends: List<Friend>,
    sentOptions: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String, String, String, String, Int, List<Friend>, Boolean) -> Unit,
    transactionToEdit: Transaction? = null
) {
    var rawAmount by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedCategory by remember(sentOptions, transactionToEdit) { mutableStateOf(transactionToEdit?.category ?: sentOptions.firstOrNull() ?: "Friend") }
    var rawNote by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.note ?: "") }
    var rawLocation by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.location ?: "") }
    var selectedAccount by remember(accounts, transactionToEdit) {
        mutableStateOf(accounts.find { it.id == transactionToEdit?.accountId } ?: accounts.find { it.isDefault } ?: accounts.firstOrNull())
    }
    
    // Track selected friends for splits (active if selectedCategory == "Friend")
    var tickedFriends by remember { mutableStateOf(setOf<Friend>()) }
    var includeMe by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record $type Money", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    label = { Text("Transaction Amount (₹)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                val limitWarn = (type == "SENT" || type == "INVESTED") && (rawAmount.toDoubleOrNull() ?: 0.0) > (selectedAccount?.balance ?: 0.0)
                if (limitWarn) {
                    Text(
                        text = "⚠️ Warning: This exceeds ${selectedAccount?.name ?: "Account"}'s balance (Available: ₹${"%,.0f".format(selectedAccount?.balance ?: 0.0)})",
                        color = AuraTheme.colors.gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Destination / Source Selector instead of recipient text field + category horizontal slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (type == "SENT") "Select Destination Option" else "Select Source Option", fontSize = 10.sp, color = AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sentOptions.forEach { opt ->
                            val s = selectedCategory == opt
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = opt }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(opt, fontSize = 10.sp, color = if (s) Color.White else AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Friends Toggles Sub-Section if category is Friend
                if (selectedCategory == "Friend") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "SPLIT WITH FRIENDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = AuraTheme.colors.accentBrand,
                            letterSpacing = 1.sp
                        )
                        
                        if (friends.isEmpty()) {
                            Text(
                                "No friends directory found. Add friends in Settings first!",
                                fontSize = 11.sp,
                                color = AuraTheme.colors.textMuted
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { includeMe = !includeMe }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(if (includeMe) AuraTheme.colors.accentBrand else Color.Transparent, RoundedCornerShape(4.dp))
                                            .border(1.5.dp, if (includeMe) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (includeMe) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text("Include Me", fontSize = 12.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                                Text("You part of the split", fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                            }

                            friends.forEach { friend ->
                                val isTicked = tickedFriends.contains(friend)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tickedFriends = if (isTicked) {
                                                tickedFriends - friend
                                            } else {
                                                tickedFriends + friend
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(if (isTicked) AuraTheme.colors.accentBrand else Color.Transparent, RoundedCornerShape(4.dp))
                                                .border(1.5.dp, if (isTicked) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isTicked) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(friend.name, fontSize = 12.sp, color = AuraTheme.colors.textPrimary)
                                    }
                                    Text(
                                        if (friend.notes.isNotBlank()) friend.notes else "Friend",
                                        fontSize = 9.sp,
                                        color = AuraTheme.colors.textMuted
                                    )
                                }
                            }

                            // Interactive share calculator
                            val amtVal = rawAmount.toDoubleOrNull() ?: 0.0
                            val shareCount = tickedFriends.size + (if (includeMe) 1 else 0)
                            if (amtVal > 0.0 && shareCount > 0 && tickedFriends.isNotEmpty()) {
                                val share = amtVal / shareCount
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))
                                Text(
                                    text = "Equally split between $shareCount ticks:",
                                    fontSize = 9.sp,
                                    color = AuraTheme.colors.textSecondary
                                )
                                Text(
                                    text = "₹${"%,.2f".format(share)} each",
                                    fontSize = 13.sp,
                                    color = AuraTheme.colors.positiveGreen,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Account Selector
                Column {
                    Text(if (type == "SENT") "Debit Account" else "Credit Account", fontSize = 10.sp, color = AuraTheme.colors.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { ac ->
                            val s = selectedAccount?.id == ac.id
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedAccount = ac }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(ac.name, fontSize = 9.sp, color = if (s) Color.White else AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = rawNote,
                    onValueChange = { rawNote = it },
                    placeholder = { Text("Notes (optional)", color = AuraTheme.colors.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = rawLocation,
                    onValueChange = { rawLocation = it },
                    placeholder = { Text("Location (optional)", color = AuraTheme.colors.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rawAmount.toDoubleOrNull() ?: 0.0
                    val acId = selectedAccount?.id ?: 0
                    val payMethod = selectedAccount?.name ?: "Cash"
                    if (amt > 0.0) {
                        val recName = when (selectedCategory) {
                            "Friend" -> {
                                if (tickedFriends.isEmpty()) if (type == "SENT") "Friends Split" else "Received from Friend"
                                else tickedFriends.joinToString { it.name }
                            }
                            else -> selectedCategory
                        }
                        onSubmit(amt, recName, selectedCategory, rawNote, rawLocation, payMethod, acId, tickedFriends.toList(), includeMe)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
            ) {
                Text("RECORD ENTRY", color = Color.White, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraTheme.colors.cardBackground
    )
}

@Composable
fun AddInvestmentFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double, String, String) -> Unit
) {
    var inName by remember { mutableStateOf("") }
    var inType by remember { mutableStateOf("Stocks") }
    var inAmount by remember { mutableStateOf("") }
    var inNotes by remember { mutableStateOf("") }

    val supportedTypes = listOf("Stocks", "Mutual Funds", "Fixed Deposit", "Gold", "Crypto", "Real Estate", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Investment Asset", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inName,
                    onValueChange = { inName = it },
                    label = { Text("Investment Asset Name (Stocks, ETF etc.)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = inAmount,
                    onValueChange = { inAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Investment Value (₹)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                Column {
                    Text("Investment Type", fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        supportedTypes.forEach { t ->
                            val s = inType == t
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { inType = t }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(t, fontSize = 9.sp, color = if (s) Color.White else AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inNotes,
                    onValueChange = { inNotes = it },
                    placeholder = { Text("Notes (optional)", color = AuraTheme.colors.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = inAmount.toDoubleOrNull() ?: 0.0
                    if (inName.isNotBlank() && amt > 0.0) {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        onSubmit(inName, inType, amt, todayStr, inNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
            ) {
                Text("ADD PORTFOLIO ASSET", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AuraTheme.colors.textSecondary)
            }
        },
        containerColor = AuraTheme.colors.cardBackground
    )
}

@Composable
fun AddFriendProfileDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var frName by remember { mutableStateOf("") }
    var frPhone by remember { mutableStateOf("") }
    var frNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Splitwise Friend Profile", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = frName,
                    onValueChange = { frName = it },
                    label = { Text("Friend Name", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = frPhone,
                    onValueChange = { frPhone = it },
                    label = { Text("Phone Number (optional)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = frNotes,
                    onValueChange = { frNotes = it },
                    label = { Text("Context/Notes (e.g. Roommate)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (frName.isNotBlank()) {
                        onSubmit(frName, frPhone, frNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
            ) {
                Text("CREATE PROFILE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AuraTheme.colors.textSecondary)
            }
        },
        containerColor = AuraTheme.colors.cardBackground
    )
}

@Composable
fun BillSplittingFormDialog(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Boolean, List<Friend>, String, Map<Int, Double>) -> Unit
) {
    var billTitle by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var isYouOwe by remember { mutableStateOf(false) } // False if they owe you, True if you owe them
    var selectedParticipants by remember { mutableStateOf(emptyList<Friend>()) }
    var splitType by remember { mutableStateOf("EQUAL") } // EQUAL, CUSTOM, PERCENTAGE

    // Custom Split state maps
    val customSplits = remember { mutableStateMapOf<Int, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unified Splitwise Bill Composer", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = billTitle,
                    onValueChange = { billTitle = it },
                    label = { Text("Bill Title (e.g., Dinner Split)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Total Bill Amount (₹)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                // Who Paid selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Debtor Direction:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (!isYouOwe) AuraTheme.colors.accentBrand else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { isYouOwe = false }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("They Owe Me", fontSize = 9.sp, color = if (!isYouOwe) Color.White else AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isYouOwe) AuraTheme.colors.negativeRed else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { isYouOwe = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("I Owe Them", fontSize = 9.sp, color = if (isYouOwe) Color.White else AuraTheme.colors.textMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                // Participants multi-select
                Text("Select Friends sharing the bill:", fontSize = 10.sp, color = AuraTheme.colors.textSecondary)
                if (friends.isEmpty()) {
                    Text("No friends available to split. Register some first!", fontSize = 10.sp, color = AuraTheme.colors.negativeRed)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        friends.forEach { f ->
                            val s = selectedParticipants.contains(f)
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                                    .border(1.dp, if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedParticipants = if (s) {
                                            selectedParticipants - f
                                        } else {
                                            selectedParticipants + f
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(f.name, fontSize = 9.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = AuraSlateLight.copy(alpha=0.3f))

                // Split method
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split options", fontSize = 11.sp, color = AuraTheme.colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EQUAL", "CUSTOM", "PERCENTAGE").forEach { st ->
                            val s = splitType == st
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBackground)
                                    .border(1.dp, if (s) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { splitType = st }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(st, fontSize = 10.sp, color = if (s) Color.White else AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // If CUSTOM or PERCENTAGE shows individual inputs
                if (splitType != "EQUAL" && selectedParticipants.isNotEmpty()) {
                    Text("Assign Custom Amounts for each participant:", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                    selectedParticipants.forEach { fri ->
                        var v by remember { mutableStateOf(customSplits[fri.id] ?: "") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(fri.name, color = AuraTheme.colors.textPrimary, fontSize = 12.sp, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = v,
                                onValueChange = {
                                    v = it
                                    customSplits[fri.id] = it
                                },
                                label = { Text(if (splitType == "CUSTOM") "Amount (₹)" else "Percent (%)", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuraTheme.colors.accentBrand,
                                    unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                    focusedTextColor = AuraTheme.colors.textPrimary,
                                    unfocusedTextColor = AuraTheme.colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ttl = billTitle.ifBlank { "Split Bill Ledger" }
                    val totalAmt = billAmount.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    if (totalAmt > 0.0 && selectedParticipants.isNotEmpty()) {
                        // Build custom pay split matrices
                        val pays = mutableMapOf<Int, Double>()
                        when (splitType) {
                            "CUSTOM" -> {
                                selectedParticipants.forEach { fri ->
                                    val indVal = (customSplits[fri.id]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                    pays[fri.id] = indVal
                                }
                            }
                            "PERCENTAGE" -> {
                                selectedParticipants.forEach { fri ->
                                    val percent = (customSplits[fri.id]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                    pays[fri.id] = (percent / 100.0) * totalAmt
                                }
                            }
                            else -> {}
                        }
                        onSubmit(ttl, totalAmt, isYouOwe, selectedParticipants, splitType, pays)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
            ) {
                Text("PERFORM SPLIT", color = Color.White, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AuraTheme.colors.textSecondary)
            }
        },
        containerColor = AuraTheme.colors.cardBackground
    )
}

@Composable
fun AddSavingsGoalFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Double, String, String) -> Unit
) {
    var glName by remember { mutableStateOf("") }
    var glTarget by remember { mutableStateOf("") }
    var glSaved by remember { mutableStateOf("0") }
    var glNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Savings Plan Target", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = glName,
                    onValueChange = { glName = it },
                    label = { Text("Goal Name (e.g., Emergency Fund)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = glTarget,
                    onValueChange = { glTarget = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Target Amount (₹)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = glSaved,
                    onValueChange = { glSaved = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Initial Saved Balance (₹)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = glNotes,
                    onValueChange = { glNotes = it },
                    label = { Text("Notes/Details", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goalTgt = (glTarget.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val goalSaved = (glSaved.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    if (glName.isNotBlank() && goalTgt > 0.0) {
                        val formatSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 90 * 24 * 60 * 60 * 1000L)) // 90 days default
                        onSubmit(glName, goalTgt, goalSaved, formatSdf, glNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
            ) {
                Text("ESTABLISH GOAL", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AuraTheme.colors.textSecondary)
            }
        },
        containerColor = AuraTheme.colors.cardBackground
    )
}

@Composable
fun AddReminderFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, String, Boolean, String) -> Unit
) {
    var rTitle by remember { mutableStateOf("") }
    var rAmount by remember { mutableStateOf("") }
    var rRecurName by remember { mutableStateOf("Monthly") }
    var isRec by remember { mutableStateOf(true) }

    val periodicLabels = listOf("Daily", "Weekly", "Monthly", "Yearly")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register EMI / Billing Alert", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = rTitle,
                    onValueChange = { rTitle = it },
                    label = { Text("Billing Title (e.g., Netflix)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = rAmount,
                    onValueChange = { rAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Amount (₹)", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = (rAmount.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    if (rTitle.isNotBlank() && amountVal > 0.0) {
                        val formatSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L))
                        onSubmit(rTitle, amountVal, formatSdf, isRec, rRecurName)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
            ) {
                Text("ADD REMINDER", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AuraTheme.colors.textSecondary)
            }
        },
        containerColor = AuraTheme.colors.cardBackground
    )
}

@Composable
fun DetailScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuraTheme.colors.cardBackground)
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Go Back",
                tint = AuraTheme.colors.textPrimary
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = AuraTheme.colors.textPrimary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun AvailableBalancePassbookView(
    accounts: List<Account>,
    transactions: List<Transaction>,
    onBack: () -> Unit,
    onAdjustBalance: (Account) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        DetailScreenHeader(title = "Account Passbook", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Title: Accounts List Horizontal Scroller
            item {
                Text(
                    text = "WALLETS & GENERAL LEDGERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { acct ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onAdjustBalance(acct) }
                                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(acct.name.uppercase(), fontSize = 10.sp, color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("₹${"%,.2f".format(acct.balance)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap to adjust manually", fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                            }
                        }
                    }
                }
            }

            // Filters Section Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(16.dp))
                            Text("ADVANCED FILTERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search description, recipient, category...", fontSize = 11.sp, color = AuraTheme.colors.textMuted) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = AuraTheme.colors.textPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            )
                        )

                        // Transaction Flow Filters
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("All", "CASH_ADDED", "RECEIVED", "SENT", "INVESTED").forEach { t ->
                                val active = selectedType == t
                                Box(
                                    modifier = Modifier
                                        .background(if (active) AuraTheme.colors.accentBrand else AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (active) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedType = t }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when(t) {
                                            "CASH_ADDED" -> "Cash In"
                                            "RECEIVED" -> "Received"
                                            "SENT" -> "Sent"
                                            "INVESTED" -> "Invested"
                                            else -> "All Flows"
                                        },
                                        fontSize = 9.sp,
                                        color = if (active) Color.White else AuraTheme.colors.textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ledger Entries Header
            item {
                Text(
                    text = "PASSBOOK LEDGER ENTRIES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            // Filter lists based on match criteria
            val filteredList = transactions.filter { tx ->
                val matchesQuery = tx.recipientOrSender.contains(searchQuery, true) ||
                        tx.category.contains(searchQuery, true) ||
                        tx.note.contains(searchQuery, true) ||
                        tx.amount.toString().contains(searchQuery)
                val matchesType = selectedType == "All" || tx.type == selectedType
                matchesQuery && matchesType
            }.sortedByDescending { it.timestamp }

            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No passbook entries match current filter.", color = AuraTheme.colors.textMuted, fontSize = 11.sp)
                    }
                }
            } else {
                items(filteredList) { tx ->
                    val isIncome = tx.type == "RECEIVED" || tx.type == "CASH_ADDED"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isIncome) AuraTheme.colors.positiveGreen.copy(alpha = 0.15f) else AuraTheme.colors.accentBrand.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = "Flow direction",
                                    tint = if (isIncome) AuraTheme.colors.positiveGreen else AuraTheme.colors.accentBrand,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = tx.recipientOrSender.ifBlank { "Cash Box" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(AuraTheme.colors.cardBackground, RoundedCornerShape(4.dp))
                                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(tx.category.uppercase(), fontSize = 7.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Text(tx.dateString, fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                                }
                                if (tx.note.isNotBlank()) {
                                    Text(tx.note, fontSize = 10.sp, color = AuraTheme.colors.textMuted, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${if (isIncome) "+" else "-"}₹${tx.amount.roundToInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isIncome) AuraTheme.colors.positiveGreen else AuraTheme.colors.textPrimary
                            )

                            IconButton(onClick = { onEditTransaction(tx) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Tx", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                            }

                            IconButton(onClick = { onDeleteTransaction(tx) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Tx", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SplitsToReceiveView(
    friends: List<Friend>,
    debts: List<Debt>,
    onBack: () -> Unit,
    onFriendClick: (Friend) -> Unit
) {
    val pendingToReceive = debts.filter { !it.isYouOwe && it.status == "PENDING" }
    val totalToReceive = pendingToReceive.sumOf { it.remainingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        DetailScreenHeader(title = "Splits To Receive", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Aggregate metrics card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraTheme.colors.positiveGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL EXPECTED RECEIVABLES", fontSize = 9.sp, color = AuraTheme.colors.positiveGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${"%,.2f".format(totalToReceive)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary)
                            }
                            Icon(Icons.Default.TrendingUp, contentDescription = "Receivables stream", tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Friend list heading
            item {
                Text(
                    text = "PENDING SPLITS BY FRIEND",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            // List of friends who currently owe money to the user (i.e. net receivable > 0)
            val friendsWithOwes = friends.map { fri ->
                val friendDebts = debts.filter { it.friendId == fri.id && it.status == "PENDING" }
                val toRec = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
                val toPay = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
                val net = toRec - toPay
                Triple(fri, net, friendDebts.size)
            }.filter { it.second > 0 }

            if (friendsWithOwes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No outstanding friend splits to make! You are fully squared up.", color = AuraTheme.colors.positiveGreen, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(friendsWithOwes) { (friend, netAmount, splitCount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onFriendClick(friend) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(friend.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Text("Owing splits: $splitCount bills", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            if (friend.notes.isNotBlank()) {
                                Text(friend.notes, fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${netAmount.roundToInt()}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.positiveGreen)
                                Text("Owes you net", fontSize = 8.sp, color = AuraTheme.colors.positiveGreen)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = "View details", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SplitsYouOweView(
    friends: List<Friend>,
    debts: List<Debt>,
    onBack: () -> Unit,
    onFriendClick: (Friend) -> Unit
) {
    val pendingYouOwe = debts.filter { it.isYouOwe && it.status == "PENDING" }
    val totalYouOwe = pendingYouOwe.sumOf { it.remainingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        DetailScreenHeader(title = "Splits You Owe", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Aggregate metrics card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraTheme.colors.negativeRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL OUTSTANDING LIABILITIES", fontSize = 9.sp, color = AuraTheme.colors.negativeRed, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${"%,.2f".format(totalYouOwe)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary)
                            }
                            Icon(Icons.Default.TrendingDown, contentDescription = "Liabilities curve", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Friend list heading
            item {
                Text(
                    text = "DEBTS BY CREDITOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            // List of friends whom the user owes money to (i.e. net owe > 0)
            val friendsWithOwes = friends.map { fri ->
                val friendDebts = debts.filter { it.friendId == fri.id && it.status == "PENDING" }
                val toRec = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
                val toPay = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
                val net = toPay - toRec
                Triple(fri, net, friendDebts.size)
            }.filter { it.second > 0 }

            if (friendsWithOwes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No debts! You are completely debt-free. Outstanding job.", color = AuraTheme.colors.positiveGreen, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(friendsWithOwes) { (friend, netAmount, splitCount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onFriendClick(friend) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(friend.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Text("Owed splits: $splitCount bills", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            if (friend.notes.isNotBlank()) {
                                Text(friend.notes, fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${netAmount.roundToInt()}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.negativeRed)
                                Text("You owe net", fontSize = 8.sp, color = AuraTheme.colors.negativeRed)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = "View details", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun FriendSplitDetailsView(
    friend: Friend,
    debts: List<Debt>,
    onBack: () -> Unit,
    onQuickSettle: (Debt) -> Unit
) {
    val friendDebts = debts.filter { it.friendId == friend.id && it.status == "PENDING" }
    val toRec = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
    val toPay = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
    val netBalance = toRec - toPay

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        DetailScreenHeader(title = "Splits with ${friend.name}", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Summary balance block
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (netBalance > 0) AuraTheme.colors.positiveGreen.copy(alpha = 0.5f) else if (netBalance < 0) AuraTheme.colors.negativeRed.copy(alpha = 0.5f) else AuraTheme.colors.cardBorder,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NET SETTLEMENT WITH ${friend.name.uppercase()}",
                            fontSize = 9.sp,
                            color = AuraTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val netColor = if (netBalance > 0) AuraTheme.colors.positiveGreen else if (netBalance < 0) AuraTheme.colors.negativeRed else AuraTheme.colors.textPrimary
                        val netLabel = if (netBalance > 0) "OWES YOU ₹${"%,.2f".format(netBalance)}" else if (netBalance < 0) "YOU OWE ₹${"%,.2f".format(-netBalance)}" else "SETTLED SQUARED UP"

                        Text(
                            text = netLabel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = netColor
                        )
                    }
                }
            }

            // Transaction items list header
            item {
                Text(
                    text = "ACTIVE SPLITS JOURNAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            if (friendDebts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No pending splits with this friend.", color = AuraTheme.colors.textMuted, fontSize = 11.sp)
                    }
                }
            } else {
                items(friendDebts) { dbt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(14.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dbt.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Text(
                                text = "Total ₹${dbt.totalAmount.roundToInt()} • ${dbt.date}",
                                fontSize = 10.sp,
                                color = AuraTheme.colors.textMuted
                            )
                            if (dbt.remainingAmount < dbt.totalAmount) {
                                Text("Partial Settle Paid: ₹${(dbt.totalAmount - dbt.remainingAmount).roundToInt()}", fontSize = 9.sp, color = AuraTheme.colors.accentBrand)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${dbt.remainingAmount.roundToInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dbt.isYouOwe) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen
                                )
                                Text(
                                    text = if (dbt.isYouOwe) "You owe" else "Owes you",
                                    fontSize = 8.sp,
                                    color = AuraTheme.colors.textMuted
                                )
                            }

                            Button(
                                onClick = { onQuickSettle(dbt) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (dbt.isYouOwe) AuraTheme.colors.negativeRed else AuraTheme.colors.positiveGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("SETTLE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PortfolioInvestmentDetailsView(
    investments: List<Investment>,
    onBack: () -> Unit,
    onAddInvestmentClick: () -> Unit,
    onDeleteInvestment: (Investment) -> Unit
) {
    val totalInvested = investments.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        DetailScreenHeader(title = "Portfolio Holdings", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Aggregate Portfolio Holdings card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraTheme.colors.gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL PORTFOLIO VALUE", fontSize = 9.sp, color = AuraTheme.colors.gold, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${"%,.2f".format(totalInvested)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary)
                            }
                            Icon(Icons.Default.Timeline, contentDescription = "Holdings graph", tint = AuraTheme.colors.gold, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Quick add button bar inline inside the holding detail view
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSETS ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onAddInvestmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.gold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("+ ADD ASSET", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            if (investments.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No active holdings discovered. Tap '+ ADD ASSET' above to log Stocks, Crypto, Mutual Funds or Gold.", color = AuraTheme.colors.textMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(investments) { inv ->
                    val daysHeld = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val parseDate = sdf.parse(inv.date) ?: java.util.Date()
                        val diffMs = System.currentTimeMillis() - parseDate.time
                        (diffMs / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    } catch (e: Exception) {
                        0L
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(AuraTheme.colors.gold, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(inv.type.uppercase(), fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black)
                                }
                                Text("Held: $daysHeld Days", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            }
                            if (inv.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(inv.notes, fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("₹${"%,.0f".format(inv.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.gold)
                            IconButton(onClick = { onDeleteInvestment(inv) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete asset", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
