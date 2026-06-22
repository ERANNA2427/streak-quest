package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.Habit
import com.example.data.UserStats
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StreakQuestApp() {
    val navController = rememberNavController()
    val viewModel: HabitViewModel = viewModel()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateBg
    ) {
        NavHost(
            navController = navController,
            startDestination = "checklist"
        ) {
            composable("checklist") {
                DailyChecklistScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { habitId ->
                        navController.navigate("details/$habitId")
                    }
                )
            }
            composable(
                route = "details/{habitId}",
                arguments = listOf(navArgument("habitId") { type = NavType.IntType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getInt("habitId") ?: 0
                HabitDetailsScreen(
                    habitId = habitId,
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChecklistScreen(
    viewModel: HabitViewModel,
    onNavigateToDetails: (Int) -> Unit
) {
    val habits by viewModel.habitsState.collectAsStateWithLifecycle()
    val stats by viewModel.userStatsState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val nextLvlXp = viewModel.getXpThreshold(stats.level)
    val progressFraction = if (nextLvlXp > 0) stats.xp.toFloat() / nextLvlXp else 0f

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("checklist_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "StreakQuest Active Icon",
                            tint = PastelTeal,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "STREAK QUEST",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateHeader,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    // Quick Stats Badge
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(XPBarBg, RoundedCornerShape(16.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Active Streaks",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(16.dp)
                        )
                        val activeStreakSum = habits.filter { it.isCompletedToday }.sumOf { it.streakCount }
                        Text(
                            text = "$activeStreakSum Ticks",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PastelPurple,
                contentColor = SlateBg,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("add_habit_fab"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = SlateBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gamified User Banner (Level & XP Progress bar)
            item {
                GamifiedHeroBanner(
                    stats = stats,
                    progressFraction = progressFraction,
                    nextLvlXp = nextLvlXp,
                    habits = habits
                )
            }

            // Checklist Header Label
            item {
                Text(
                    text = "DAILY QUEST CHECKLIST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (habits.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No quests configured yet!",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the '+' button to form your first habit quest.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(
                    items = habits,
                    key = { it.id }
                ) { habit ->
                    HabitChecklistItem(
                        habit = habit,
                        onToggle = { viewModel.toggleHabit(habit) },
                        onNavigate = { onNavigateToDetails(habit.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, difficulty, icon ->
                viewModel.addNewHabit(title, desc, difficulty, icon)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun GamifiedHeroBanner(
    stats: UserStats,
    progressFraction: Float,
    nextLvlXp: Int,
    habits: List<Habit>
) {
    // Elegant layered card decoration with smooth dark slate elevation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(2.dp, Brush.linearGradient(listOf(BorderColor, Color(0xFF3B4A6F))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AVATAR STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelTeal,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Streak Alchemist",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Level badge representation
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(DeepPurple, RoundedCornerShape(16.dp))
                        .border(2.dp, PastelPurple, RoundedCornerShape(16.dp))
                        .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = PastelPurpleGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LVL",
                            fontSize = 11.sp,
                            color = PastelPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stats.level.toString(),
                            fontSize = 20.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // XP level up meter
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Experience Progress",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${stats.xp} / ${nextLvlXp} XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelPurple
                    )
                }

                // Custom XP Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(XPBarBg)
                ) {
                    val progressAnimation by animateFloatAsState(
                        targetValue = progressFraction.coerceIn(0f, 1f),
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "XP Progressive Fill"
                    )

                    // Filled XP progress in linear gradient
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnimation)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PastelTealGlow, PastelPurpleGlow)
                                )
                            )
                    )
                }
            }

            // Quick Quest completions overview
            val completedCount = habits.count { it.isCompletedToday }
            val totalCount = habits.size
            val completionMsg = if (totalCount > 0 && completedCount == totalCount) {
                "✨ All Daily Quests Accomplished! (+50 XP bonus Active)"
            } else {
                "$completedCount of $totalCount Quests completed"
            }

            Text(
                text = completionMsg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (totalCount > 0 && completedCount == totalCount) PastelTeal else TextSecondary
            )
        }
    }
}

@Composable
fun HabitChecklistItem(
    habit: Habit,
    onToggle: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() }
            .testTag("habit_card_${habit.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (habit.isCompletedToday) PastelTealGlow.copy(alpha = 0.4f) else BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen 1: Custom Circular Checkbox node that fills with gradient color when tapped
            CircularGradientCheckbox(
                isChecked = habit.isCompletedToday,
                onCheckedChange = { onToggle() }
            )

            // Habit Information Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getIconByName(habit.iconName),
                        contentDescription = habit.title,
                        tint = if (habit.isCompletedToday) PastelTeal else PastelPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = habit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (habit.isCompletedToday) TextSecondary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = habit.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Difficulty chip Badge
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            when (habit.difficulty) {
                                "Easy" -> DeepTeal.copy(alpha = 0.3f)
                                "Hard" -> Color(0xFF7F1D1D).copy(alpha = 0.3f)
                                else -> DeepPurple.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = habit.difficulty,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (habit.difficulty) {
                            "Easy" -> PastelTeal
                            "Hard" -> Color(0xFFFCA5A5)
                            else -> PastelPurple
                        }
                    )
                }
            }

            // Streak counts and XP rewards
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Day Streak",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${habit.streakCount}d",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "+${habit.xpReward} XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelTeal
                )
            }
        }
    }
}

@Composable
fun CircularGradientCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Custom Checkbox built using Canvas for pristine compliance with:
    // "circular checkbox nodes that fill with gradient color when tapped"
    val animProgress by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Circular Checkbox Animation"
    )

    Box(
        modifier = Modifier
            .size(48.dp) // Accessibility touch target compliance
            .testTag("circular_checkbox_container")
            .clip(CircleShape)
            .clickable { onCheckedChange(!isChecked) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val canvasSize = size.width
            val center = Offset(x = canvasSize / 2f, y = canvasSize / 2f)
            val radius = canvasSize / 2f

            // 1. Draw outer stroke border
            drawCircle(
                color = if (isChecked) PastelTealGlow else BorderColor,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Draw interior filled gradient circle
            if (animProgress > 0f) {
                val brush = Brush.linearGradient(
                    colors = listOf(PastelTealGlow, PastelPurpleGlow),
                    start = Offset(0f, 0f),
                    end = Offset(canvasSize, canvasSize)
                )
                // Draw circle that scales up with animation
                drawCircle(
                    brush = brush,
                    radius = radius * animProgress
                )

                // 3. Draw mini checklist mark
                val checkColor = SlateBg
                val strokeWidth = 2.5.dp.toPx()

                // Checkmark coordinate math
                val p1 = Offset(canvasSize * 0.3f, canvasSize * 0.5f)
                val p2 = Offset(canvasSize * 0.45f, canvasSize * 0.65f)
                val p3 = Offset(canvasSize * 0.72f, canvasSize * 0.38f)

                // Render lines if completed
                drawLine(
                    color = checkColor,
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = checkColor,
                    start = p2,
                    end = p3,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsScreen(
    habitId: Int,
    viewModel: HabitViewModel,
    onBack: () -> Unit
) {
    val habits by viewModel.habitsState.collectAsStateWithLifecycle()
    val habit = habits.find { it.id == habitId }

    // Selected habit history details from VM
    val historyDays by viewModel.selectedHabitHistory.collectAsStateWithLifecycle()

    LaunchedEffect(habitId) {
        viewModel.selectHabitById(habitId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("details_screen"),
        topBar = {
            TopAppBar(
                title = { Text(text = habit?.title ?: "Quest Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateHeader,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    if (habit != null) {
                        IconButton(onClick = {
                            viewModel.deleteHabit(habit)
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Quest",
                                tint = Color(0xFFF87171)
                            )
                        }
                    }
                }
            )
        },
        containerColor = SlateBg
    ) { paddingValues ->
        if (habit == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading Quest details...", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Intro Header Card
                item {
                    QuestDetailHeader(habit = habit, onToggle = { viewModel.toggleHabit(habit) })
                }

                // Grid Title & Instruction Label
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "30-DAY STREAK GRAPH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelTeal,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Vibrant purple nodes represent successful days. Tap any square to toggle past logs manually!",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Screen 2: 30-Day streak glowing rounded square grid pattern
                item {
                    Streak30DayCalendarGrid(
                        habit = habit,
                        historyDays = historyDays,
                        onToggleDate = { clickedDate ->
                            // Simple toggle in background
                            viewModel.toggleHabit(habit) 
                        }
                    )
                }

                // Legend Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PastelPurpleGlow)
                            )
                            Text(text = "Complete", color = TextPrimary, fontSize = 12.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SlateCard)
                                    .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                            )
                            Text(text = "Incomplete", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                // Analytical Metrics Summary Grid
                item {
                    StatisticsRow(habit = habit, completedDaysCount = historyDays.size)
                }
            }
        }
    }
}

@Composable
fun QuestDetailHeader(habit: Habit, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DeepPurple.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(habit.iconName),
                            contentDescription = habit.title,
                            tint = PastelPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = habit.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${habit.difficulty} level, ${habit.xpReward} XP per tap",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Quick Checklist checklist switch
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (habit.isCompletedToday) DeepTeal else PastelPurple,
                        contentColor = SlateBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (habit.isCompletedToday) "COMPLETED" else "MARK OUT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (habit.isCompletedToday) PastelTeal else SlateBg
                    )
                }
            }

            Divider(color = BorderColor)

            Text(
                text = habit.description,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun Streak30DayCalendarGrid(
    habit: Habit,
    historyDays: List<String>,
    onToggleDate: (String) -> Unit
) {
    // Generate consecutive 30 days backward from today
    val daysList = remember(historyDays) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val list = mutableListOf<CalendarDayInfo>()
        val cal = Calendar.getInstance()

        for (i in 29 downTo 0) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(dateCal.time)
            val dayNumString = SimpleDateFormat("d", Locale.getDefault()).format(dateCal.time)
            
            // Determine if completed
            val isCompleted = historyDays.contains(dateStr) || (i == 0 && habit.isCompletedToday)
            list.add(
                CalendarDayInfo(
                    dateString = dateStr,
                    dayLabel = dayNumString,
                    isCompleted = isCompleted
                )
            )
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_calendar"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Month Header
            val monthLabel = remember {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
            }
            Text(
                text = monthLabel.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 30 rounded square nodes grid (e.g. 6 elements per row, 5 rows)
            val rowSize = 6
            val chunkedDays = daysList.chunked(rowSize)

            chunkedDays.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { dayInfo ->
                        // Screen 2 Rounded node: glowing vibrant purple for successful days
                        StreakNode(
                            dayInfo = dayInfo,
                            onNodeClick = { onToggleDate(dayInfo.dateString) }
                        )
                    }
                    // Handle trailing rows elegantly to keep uniform row width
                    if (row.size < rowSize) {
                        repeat(rowSize - row.size) {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakNode(
    dayInfo: CalendarDayInfo,
    onNodeClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Glowing vibrant purple shadow configuration if active
    val nodeGlowModifier = if (dayInfo.isCompleted) {
        Modifier
            .shadow(
                elevation = 6.dp, 
                shape = RoundedCornerShape(10.dp),
                ambientColor = PastelPurpleGlow,
                spotColor = PastelPurpleGlow
            )
            .border(2.dp, PastelPurple, RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(PastelPurpleGlow, DeepPurple)
                )
            )
    } else {
        Modifier
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .background(SlateBg)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(nodeGlowModifier)
            .clickable {
                onNodeClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayInfo.dayLabel,
            color = if (dayInfo.isCompleted) SlateBg else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatisticsRow(habit: Habit, completedDaysCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Metric A: Streak
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "ACTIVE STREAK", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                    Text(text = "${habit.streakCount} Days", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
            }
        }

        // Metric B: Total completions
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "TOTAL LOGS", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PastelTeal, modifier = Modifier.size(16.dp))
                    Text(text = "$completedDaysCount Ticks", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var selectedIcon by remember { mutableStateOf("star") }

    val difficulties = listOf("Easy", "Medium", "Hard")
    val iconsList = listOf(
        Pair("star", Icons.Default.Star),
        Pair("water_drop", Icons.Default.WaterDrop),
        Pair("fitness_center", Icons.Default.FitnessCenter),
        Pair("book", Icons.Default.Book),
        Pair("bedtime", Icons.Default.Bedtime)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_habit_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(2.dp, PastelPurple)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "INITIATE DAILY QUEST",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PastelPurple,
                    letterSpacing = 1.5.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Name (e.g. Morning Meds)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PastelPurple,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_title_field"),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Quest objective details") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PastelPurple,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_desc_field"),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Select Difficulty
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Difficulty multiplier", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        difficulties.forEach { diff ->
                            val isSelected = diff == selectedDifficulty
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DeepPurple else SlateBg)
                                    .border(1.dp, if (isSelected) PastelPurple else BorderColor, RoundedCornerShape(10.dp))
                                    .clickable { selectedDifficulty = diff }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Select Icon
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Emblem Icon", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        iconsList.forEach { (name, iconVector) ->
                            val isSelected = name == selectedIcon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) DeepPurple else SlateBg)
                                    .border(1.dp, if (isSelected) PastelPurple else BorderColor, CircleShape)
                                    .clickable { selectedIcon = name },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = name,
                                    tint = if (isSelected) PastelTeal else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abate", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, desc, selectedDifficulty, selectedIcon)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelPurple, contentColor = SlateBg),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_button"),
                        enabled = title.isNotBlank()
                    ) {
                        Text("INITIATE", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

data class CalendarDayInfo(
    val dateString: String,
    val dayLabel: String,
    val isCompleted: Boolean
)

// Helper icon parser mapping saved names to vector drawables
fun getIconByName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "water_drop" -> Icons.Default.WaterDrop
        "fitness_center" -> Icons.Default.FitnessCenter
        "book" -> Icons.Default.Book
        "bedtime" -> Icons.Default.Bedtime
        "star" -> Icons.Default.Star
        else -> Icons.Default.Star
    }
}
