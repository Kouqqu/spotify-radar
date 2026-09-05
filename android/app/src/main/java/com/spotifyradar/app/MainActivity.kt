package com.spotifyradar.app

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    var selectedTab by remember { mutableStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<RadarAnalyzer.AnalysisResult?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedArtistForDetails by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var storyBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun haptic() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
                        Toast.makeText(context, "Найдено треков: ${tracks.size}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Не удалось распознать треки в файле", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = SpotifyBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Spotify Radar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kworb 500",
                            color = SpotifyGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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
                    text = "Узнай, сколько твоих любимых артистов входят в мировой чарт Топ-500 Spotify.",
                    color = SpotifyTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Tabs Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpotifyCard, RoundedCornerShape(14.dp))
                        .border(1.dp, SpotifyBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    TabButton(
                        title = "Файл .CSV",
                        icon = Icons.Default.UploadFile,
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic()
                        selectedTab = 0
                    }
                    TabButton(
                        title = "Список текстом",
                        icon = Icons.Default.Edit,
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic()
                        selectedTab = 1
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
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Загрузи .csv из Exportify",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Экспортируй плейлист на exportify.net",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    haptic()
                                    filePickerLauncher.launch("*/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Выбрать файл", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic()
                                    val demoData = listOf(
                                        Pair("Starboy", "The Weeknd, Daft Punk"),
                                        Pair("Blinding Lights", "The Weeknd"),
                                        Pair("Shape of You", "Ed Sheeran"),
                                        Pair("bad guy", "Billie Eilish"),
                                        Pair("Levitating", "Dua Lipa"),
                                        Pair("Stay", "The Kid LAROI, Justin Bieber"),
                                        Pair("Flowers", "Miley Cyrus"),
                                        Pair("Goth", "Sidewalks and Skeletons"),
                                        Pair("Resonance", "HOME"),
                                        Pair("After Dark", "Mr.Kitty")
                                    )
                                    analysisResult = RadarAnalyzer.analyze(demoData)
                                    Toast.makeText(context, "Демо-плейлист загружен!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SpotifyBorder))
                            ) {
                                Text("Демо")
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Text input
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyCard, RoundedCornerShape(16.dp))
                            .border(1.dp, SpotifyBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Введи артистов через запятую или строку:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("The Weeknd, Taylor Swift, Lady Gaga...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen,
                                unfocusedBorderColor = SpotifyBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                haptic()
                                val parsed = RadarAnalyzer.parseText(textInput)
                                if (parsed.isNotEmpty()) {
                                    analysisResult = RadarAnalyzer.analyze(parsed)
                                } else {
                                    Toast.makeText(context, "Введи хотя бы одного исполнителя", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Анализировать", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Analysis Result Section
            val result = analysisResult
            if (result != null) {
                // Verdict Banner
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
                                storyBitmap = StoryCardGenerator.generateCard(result)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stories карточка", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                haptic()
                                val shareText = "${result.verdictEmoji} Мой Spotify Top 500 Radar:\n" +
                                        "• Вердикт: ${result.verdictTitle}\n" +
                                        "• В Топ-500: ${result.matchedCount} артистов\n" +
                                        "• Топ-1: ${result.highestArtist?.name ?: "—"} (#${result.highestArtist?.rank ?: "—"})\n" +
                                        "• Вхождение в чарты: ${result.chartPercentage}%\n\n" +
                                        "Проверь свой плейлист: https://saaanek.github.io/spotify-radar/"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
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

                // Search Filter for lists
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск артиста...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Matched Artists List
                val filteredMatched = result.matchedArtists.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Найдено в Топ-500",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredMatched.size}",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                items(filteredMatched) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyCard, RoundedCornerShape(12.dp))
                            .border(1.dp, SpotifyBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                haptic()
                                selectedArtistForDetails = Pair(artist.name, artist.tracks)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${artist.rank}",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = artist.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (artist.tracks.isNotEmpty()) {
                            Text(
                                text = "${artist.tracks.size} трек.",
                                color = SpotifyTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Indie Artists List
                val filteredIndie = result.indieArtists.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Андеграунд (вне чартов)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredIndie.size}",
                            color = Color(0xFFB388FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFB388FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                items(filteredIndie) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotifyCard, RoundedCornerShape(12.dp))
                            .border(1.dp, SpotifyBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                haptic()
                                selectedArtistForDetails = Pair(artist.name, artist.tracks)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "●",
                            color = Color(0xFFB388FF),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = artist.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (artist.tracks.isNotEmpty()) {
                            Text(
                                text = "${artist.tracks.size} трек.",
                                color = SpotifyTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Spotify Radar Native • v1.2.0\n100% Pure Kotlin & Compose",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }
        }
    }

    // Modal: Song Details
    val artistDetails = selectedArtistForDetails
    if (artistDetails != null) {
        AlertDialog(
            onDismissRequest = { selectedArtistForDetails = null },
            confirmButton = {
                TextButton(onClick = { selectedArtistForDetails = null }) {
                    Text("Закрыть", color = SpotifyGreen)
                }
            },
            title = {
                Text(text = artistDetails.first, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Треков в плейлисте: ${artistDetails.second.size}",
                        color = SpotifyTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (artistDetails.second.isEmpty()) {
                        Text("Информация о треках отсутствует", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        artistDetails.second.forEach { track ->
                            Text(
                                text = "• $track",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
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
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
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
fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF282828) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) SpotifyGreen else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = sub,
            color = SpotifyTextSecondary,
            fontSize = 10.sp
        )
    }
}
