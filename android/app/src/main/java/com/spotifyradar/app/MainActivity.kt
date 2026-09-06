package com.spotifyradar.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpotifyRadarApp()
        }
    }
}

// Spotify Brand Colors
val SpotifyBlack = Color(0xFF121212)
val SpotifyCard = Color(0xFF181818)
val SpotifyBorder = Color(0xFF282828)
val SpotifyGreen = Color(0xFF1ED760)
val SpotifyTextSecondary = Color(0xFFB3B3B3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyRadarApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<RadarAnalyzer.AnalysisResult?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isMatchedExpanded by remember { mutableStateOf(false) }
    var isIndieExpanded by remember { mutableStateOf(false) }
    var matchedLimit by remember { mutableStateOf(25) }
    var indieLimit by remember { mutableStateOf(25) }
    var selectedArtistForDetails by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var storyBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    fun haptic() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    // Auto-check for GitHub updates on app launch
    LaunchedEffect(Unit) {
        val info = UpdateChecker.checkForUpdates(context)
        if (info != null) {
            updateInfo = info
        }
    }

    // Memoized filtered lists
    val result = analysisResult
    val filteredMatched = remember(result?.matchedArtists, searchQuery) {
        val list = result?.matchedArtists ?: emptyList()
        if (searchQuery.isBlank()) list
        else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredIndie = remember(result?.indieArtists, searchQuery) {
        val list = result?.indieArtists ?: emptyList()
        if (searchQuery.isBlank()) list
        else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Reset pagination limits when search or result changes
    LaunchedEffect(searchQuery, result) {
        matchedLimit = 25
        indieLimit = 25
    }

    // CSV File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            haptic()
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val content = reader.readText()
                    val tracks = RadarAnalyzer.parseCsv(content)
                    if (tracks.isNotEmpty()) {
                        analysisResult = RadarAnalyzer.analyze(tracks)
                        Toast.makeText(context, "Импортировано треков: ${tracks.size}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Не найдены треки в файле", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка чтения CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_spotify),
                            contentDescription = "Spotify Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Spotify Radar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Топ-500",
                            color = SpotifyGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    // GitHub Link Button
                    IconButton(
                        onClick = {
                            haptic()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kouqqu/spotify-radar"))
                            context.startActivity(intent)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222))
                                .border(1.dp, SpotifyBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = "GitHub",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyBlack)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero info
            item {
                Text(
                    text = "Узнай, сколько твоих треков и артистов входят в мировой Топ-500 Spotify.",
                    color = SpotifyTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Animated Sliding Tabs Selector
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpotifyCard, RoundedCornerShape(14.dp))
                        .border(1.dp, SpotifyBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    val tabWidth = maxWidth / 2
                    val indicatorOffset by animateDpAsState(
                        targetValue = if (selectedTab == 0) 0.dp else tabWidth,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
                        label = "tabIndicatorOffset"
                    )

                    // Sliding pill highlight
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2C2C2C))
                    )

                    // Tab buttons
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TabButton(
                            title = "Файл .CSV",
                            icon = Icons.Default.Add,
                            isSelected = selectedTab == 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptic()
                            selectedTab = 0
                        }
                        TabButton(
                            title = "Текстом",
                            icon = Icons.Default.Edit,
                            isSelected = selectedTab == 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            haptic()
                            selectedTab = 1
                        }
                    }
                }
            }

            // Tab 0: CSV Upload Card
            if (selectedTab == 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyCard, RoundedCornerShape(16.dp))
                            .border(1.dp, SpotifyBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Загрузить экспорт плейлиста",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Экспортируй свой Spotify-плейлист через сервис Exportify в формате .csv",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                haptic()
                                filePickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выбрать .csv файл", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Demo Button
                        OutlinedButton(
                            onClick = {
                                haptic()
                                val demoText = "The Weeknd\nTaylor Swift\nBillie Eilish\nPost Malone\nDua Lipa\nKanye West\nLana Del Rey\nKendrick Lamar\nShortparis\nХаски\nДайте Танк (!)\nMolchat Doma\nBones\nIC3PEAK\nSaluki\nBoulevard Depo"
                                val demoTracks = RadarAnalyzer.parseText(demoText)
                                analysisResult = RadarAnalyzer.analyze(demoTracks)
                                Toast.makeText(context, "Загружен демо-плейлист", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, SpotifyBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Загрузить демо-плейлист", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Tab 1: Manual Text Input
            if (selectedTab == 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyCard, RoundedCornerShape(16.dp))
                            .border(1.dp, SpotifyBorder, RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Введи исполнителей",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Каждого с новой строки или через запятую",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            placeholder = {
                                Text("The Weeknd, Billie Eilish, Lana Del Rey...", color = Color.DarkGray, fontSize = 13.sp)
                            },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = SpotifyGreen,
                                focusedBorderColor = SpotifyGreen,
                                unfocusedBorderColor = SpotifyBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                haptic()
                                if (textInput.isNotBlank()) {
                                    val tracks = RadarAnalyzer.parseText(textInput)
                                    analysisResult = RadarAnalyzer.analyze(tracks)
                                    Toast.makeText(context, "Проанализировано артистов: ${tracks.size}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Введи хотя бы одного артиста", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Анализировать", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Analysis Result Section
            if (result != null) {
                // Verdict Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyCard, RoundedCornerShape(18.dp))
                            .border(1.dp, SpotifyGreen.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = result.verdictEmoji, fontSize = 34.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ТВОЙ МУЗЫКАЛЬНЫЙ ТИПАЖ",
                                    color = SpotifyGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = result.verdictTitle,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = result.verdictDesc,
                            color = SpotifyTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // 3 Metric Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "В Топ-500",
                            value = "${result.matchedCount}",
                            sub = "артистов",
                            color = SpotifyGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "% Чартов",
                            value = "${result.chartPercentage}%",
                            sub = "плейлиста",
                            color = SpotifyGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Андеграунд",
                            value = "${result.indieCount}",
                            sub = "артистов",
                            color = Color(0xFFB388FF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Top 1 Artist Card
                if (result.highestArtist != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SpotifyCard, RoundedCornerShape(16.dp))
                                .border(1.dp, SpotifyBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF242424)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "САМЫЙ ПОПУЛЯРНЫЙ",
                                    color = SpotifyTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = result.highestArtist.name,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "#${result.highestArtist.rank}",
                                color = SpotifyGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Action Buttons: Generate Story Card & Share Text
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic()
                                val card = StoryCardGenerator.generateCard(result)
                                if (card != null) {
                                    storyBitmap = card
                                } else {
                                    Toast.makeText(context, "Не удалось создать карточку", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stories карточка", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                haptic()
                                val shareText = "${result.verdictEmoji} Мой Spotify Top 500 Radar:\n" +
                                        "🏆 Вердикт: ${result.verdictTitle}\n" +
                                        "📊 В Топ-500: ${result.matchedCount} артистов\n" +
                                        "👑 Топ-1: ${result.highestArtist?.name ?: "—"} (#${result.highestArtist?.rank ?: "—"})\n" +
                                        "🎧 Процент чартов: ${result.chartPercentage}%\n\n" +
                                        "kouqqu.github.io/spotify-radar"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Поделиться результатом"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = SpotifyGreen)
                        }
                    }
                }

                // Search Filter - permanently placed above accordions so opening accordions does not cause layout shifts
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        placeholder = { Text("Поиск артиста в списках...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = SpotifyGreen,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyBorder
                        )
                    )
                }

                // 1. Accordion Section: Matched Artists List
                item {
                    AccordionSection(
                        title = "Найдено в Топ-500",
                        count = result.matchedArtists.size,
                        indicatorColor = SpotifyGreen,
                        isExpanded = isMatchedExpanded,
                        onToggle = {
                            haptic()
                            isMatchedExpanded = !isMatchedExpanded
                        }
                    ) {
                        if (filteredMatched.isEmpty()) {
                            Text(
                                text = "Ничего не найдено",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            val displayList = filteredMatched.take(matchedLimit)
                            displayList.forEach { artist ->
                                ArtistRow(
                                    rankText = "#${artist.rank}",
                                    rankColor = SpotifyGreen,
                                    artistName = artist.name,
                                    tracksCount = artist.tracks.size,
                                    onClick = {
                                        haptic()
                                        selectedArtistForDetails = Pair(artist.name, artist.tracks)
                                    }
                                )
                            }
                            if (filteredMatched.size > matchedLimit) {
                                TextButton(
                                    onClick = {
                                        haptic()
                                        matchedLimit += 35
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Показать ещё (+35)",
                                        color = SpotifyGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Accordion Section: Indie Artists List
                item {
                    AccordionSection(
                        title = "Андеграунд (вне чартов)",
                        count = result.indieArtists.size,
                        indicatorColor = Color(0xFFB388FF),
                        isExpanded = isIndieExpanded,
                        onToggle = {
                            haptic()
                            isIndieExpanded = !isIndieExpanded
                        }
                    ) {
                        if (filteredIndie.isEmpty()) {
                            Text(
                                text = "Ничего не найдено",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            val displayList = filteredIndie.take(indieLimit)
                            displayList.forEach { artist ->
                                ArtistRow(
                                    rankText = "●",
                                    rankColor = Color(0xFFB388FF),
                                    artistName = artist.name,
                                    tracksCount = artist.tracks.size,
                                    onClick = {
                                        haptic()
                                        selectedArtistForDetails = Pair(artist.name, artist.tracks)
                                    }
                                )
                            }
                            if (filteredIndie.size > indieLimit) {
                                TextButton(
                                    onClick = {
                                        haptic()
                                        indieLimit += 35
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Показать ещё (+35)",
                                        color = Color(0xFFB388FF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer with Version & Tap to Check Updates
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic()
                            coroutineScope.launch {
                                Toast.makeText(context, "Проверка обновлений...", Toast.LENGTH_SHORT).show()
                                val info = UpdateChecker.checkForUpdates(context)
                                if (info != null) {
                                    updateInfo = info
                                } else {
                                    Toast.makeText(context, "У вас установлена последняя версия (v1.2.${AppVersion.CURRENT_RUN_NUMBER})!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Spotify Radar • v1.2.${AppVersion.CURRENT_RUN_NUMBER}",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Нажмите, чтобы проверить обновления",
                        color = Color(0xFF555555),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal: Update Available Dialog
    val update = updateInfo
    if (update != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            icon = { Text("🚀", fontSize = 32.sp) },
            title = {
                Text(
                    text = "Доступно обновление!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = update.title,
                        color = SpotifyGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (update.body.isNotBlank()) {
                        Text(
                            text = update.body.lines().take(6).joinToString("\n"),
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Text(
                        text = "Желаете скачать новую версию?",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                        context.startActivity(intent)
                        updateInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Скачать", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            UpdateChecker.ignoreVersion(context, update.tag)
                            updateInfo = null
                            Toast.makeText(context, "Больше не напоминать для этой версии", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Не напоминать", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(onClick = { updateInfo = null }) {
                        Text("Позже", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Modal: Song Details with direct Spotify Launch
    val artistDetails = selectedArtistForDetails
    if (artistDetails != null) {
        AlertDialog(
            onDismissRequest = { selectedArtistForDetails = null },
            confirmButton = {
                Button(
                    onClick = {
                        haptic()
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(artistDetails.first)}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/${Uri.encode(artistDetails.first)}"))
                            context.startActivity(webIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_spotify),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("В Spotify", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedArtistForDetails = null }) {
                    Text("Закрыть", color = Color.Gray)
                }
            },
            title = {
                Text(
                    text = artistDetails.first,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    Text(
                        text = "Треки в плейлисте: ${artistDetails.second.size}",
                        color = SpotifyTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(artistDetails.second) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic()
                                        val query = "${artistDetails.first} $track"
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}"))
                                            context.startActivity(webIntent)
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• $track",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_spotify),
                                    contentDescription = "Слушать в Spotify",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal: Story Card Preview & Sharing
    val story = storyBitmap
    if (story != null) {
        Dialog(onDismissRequest = { storyBitmap = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181818), RoundedCornerShape(20.dp))
                    .border(1.dp, SpotifyBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stories Карточка", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { storyBitmap = null }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Image(
                    bitmap = story.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic()
                            val uri = StoryCardGenerator.saveToGallery(context, story)
                            if (uri != null) {
                                Toast.makeText(context, "Сохранено в Галерею!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("В галерею", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            haptic()
                            StoryCardGenerator.shareImage(context, story)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = SpotifyGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun AccordionSection(
    title: String,
    count: Int,
    indicatorColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "chevronRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpotifyCard)
            .border(1.dp, SpotifyBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count",
                    color = indicatorColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(indicatorColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.rotate(rotation)
            )
        }

        // Visible Animated Collapse/Expand with clipping and fading
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                    fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                   fadeOut(animationSpec = tween(180))
        ) {
            Column {
                HorizontalDivider(
                    color = SpotifyBorder,
                    thickness = 1.dp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Gray,
        animationSpec = tween(220),
        label = "tabTextColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) SpotifyGreen else Color.Gray,
        animationSpec = tween(220),
        label = "tabIconColor"
    )

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ArtistRow(
    rankText: String,
    rankColor: Color,
    artistName: String,
    tracksCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF222222))
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rankText,
            color = rankColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(42.dp)
        )
        Text(
            text = artistName,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (tracksCount > 0) {
            Text(
                text = "$tracksCount трек.",
                color = SpotifyTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SpotifyCard, RoundedCornerShape(14.dp))
            .border(1.dp, SpotifyBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = sub,
            color = SpotifyTextSecondary,
            fontSize = 10.sp
        )
    }
}
