package com.guido.darkmovies

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.guido.darkmovies.ui.theme.Blanco
import com.guido.darkmovies.ui.theme.DarkmoviesTheme
import com.guido.darkmovies.ui.theme.Gris
import com.guido.darkmovies.ui.theme.GrisOscuro
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }*/

        setContent {
            DarkmoviesTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "main_screen") {
                        composable("main_screen") { MainScreen(navController, context) }
                        composable(
                            "detail_screen/{titulo}",
                            arguments = listOf(navArgument("titulo") { defaultValue = "" })
                        ) { backStackEntry ->
                            DetailScreen(
                                context,
                                navController,
                                backStackEntry.arguments?.getString("titulo") ?: ""
                            )
                        }
                        composable(
                            "video_screen/{videos}/{titulo}/{isSeries}/{seasonNumber}/{episodeKey}",
                            arguments = listOf(
                                navArgument("videos") { defaultValue = "" },
                                navArgument("titulo") { defaultValue = "" },
                                navArgument("isSeries") { defaultValue = false },
                                navArgument("seasonNumber") { defaultValue = "" },
                                navArgument("episodeKey") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            VideoScreen(
                                navController,
                                backStackEntry.arguments?.getString("videos") ?: "",
                                context = context,
                                backStackEntry.arguments?.getString("titulo") ?: "",
                                isSeries = backStackEntry.arguments?.getBoolean("isSeries") ?: false,
                                seasonNumber = (backStackEntry.arguments?.getBoolean("seasonNumber") ?: "").toString(),
                                episodeKey = backStackEntry.arguments?.getString("episodeKey") ?: ""
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainScreen(navController: NavHostController, context: Context) {
    val moviesVistas = remember { mutableStateListOf<MovieWithTitleAndPortada>() }
    val searchTerm = remember { mutableStateOf(TextFieldValue()) }
    val movies = remember { mutableStateListOf<MoviesMainScreen>() }

    LaunchedEffect(Unit) {
        mainScreenPortadasTitulos(movies)
        fetchMoviesFromFirestoreByTitles(context, moviesVistas)
    }

    // Ordenar las películas vistas por última vez
    moviesVistas.sortByDescending {
        context.getSharedPreferences("last_watched_prefs", Context.MODE_PRIVATE)
            .getLong("${it.titulo}-lastWatched", 0L)
    }

    Column(modifier = Modifier.background(Gris)) {
        TextField(
            value = searchTerm.value,
            onValueChange = { searchTerm.value = it },
            label = { Text("Search by movie title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (moviesVistas.isNotEmpty()) {
            Text(
                "Continue watching :)",
                modifier = Modifier.padding(start = 8.dp),
                color = Blanco
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(modifier = Modifier.padding(start = 8.dp)) {
            items(moviesVistas) { movie ->
                Column(
                    modifier = Modifier.clickable {
                        incrementGlobalCounter(context)
                        navController.navigate("detail_screen/${movie.titulo}")
                    }
                ) {
                    GlideImage(
                        model = movie.portada,
                        contentDescription = "portada",
                        modifier = Modifier
                            .width(100.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        val groupedMovies = movies.filter {
            it.titulo.contains(searchTerm.value.text, ignoreCase = true)
        }.groupBy { it.categoria }

        LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Red)) {
            groupedMovies.forEach { (categoria, moviesInCategory) ->
                item {
                    Text(
                        text = categoria,
                        modifier = Modifier.padding(start = 8.dp),
                        color = Blanco
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(moviesInCategory.chunked(4)) { rowItems ->
                    Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                        rowItems.forEach { movie ->
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        incrementGlobalCounter(context)
                                        navController.navigate("detail_screen/${movie.titulo}")
                                    }
                            ) {
                                GlideImage(
                                    model = movie.portada,
                                    contentDescription = "portada",
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun incrementGlobalCounter(context: Context) {
    val currentCount = context.getSharedPreferences("global_counterr", Context.MODE_PRIVATE)
        .getInt("global_count", 0)
    Log.i("Contador", currentCount.toString())
    if (currentCount < 4) {
        with(context.getSharedPreferences("global_counterr", Context.MODE_PRIVATE).edit()) {
            putInt("global_count", currentCount + 1)
            apply()
        }
        if (currentCount + 1 == 4) {
            Log.i("Contador", "El contador ha alcanzado 4 en MainScreen")
            with(context.getSharedPreferences("global_counterr", Context.MODE_PRIVATE).edit()) {
                putInt("global_count", 0)
                apply()
            }
            val url = "https://www.highrevenuenetwork.com/ef379y53x?key=33fea0a311f5a616b4844d9304fb1f1d"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DetailScreen(context: Context, navController: NavHostController, titulo: String) {
    val detail = remember { mutableStateOf<Any?>(null) }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("episode_prefs", Context.MODE_PRIVATE)

    // Load last clicked episode from shared preferences
    var lastClickedEpisode by remember {
        mutableStateOf(sharedPreferences.getString("$titulo-lastClickedEpisode", ""))
    }

    LaunchedEffect(titulo) {
        val fetchedDetail = detailScreenMovies(titulo)
        detail.value = fetchedDetail
        selectedLanguage = (fetchedDetail as? SeriesDetail)?.series?.keys?.firstOrNull()
    }

    LazyColumn(modifier = Modifier.background(Gris)) {
        when (val content = detail.value) {
            is MovieDetail -> {
                item {
                    content.apply {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(32.dp))
                                GlideImage(
                                    model = portada ?: "",
                                    contentDescription = "portada",
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = titulo, color = Blanco, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                videos?.forEach { (key, value) ->
                                    Button(
                                        onClick = {
                                            incrementGlobalCounter(context)
                                            navController.navigate("video_screen/${Uri.encode(value)}/$titulo/false/0/0")
                                            val sharedPreferences = context.getSharedPreferences("last_watched_prefs", Context.MODE_PRIVATE)
                                            with(sharedPreferences.edit()) {
                                                putLong("${titulo}-lastWatched", System.currentTimeMillis())
                                                apply()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Blanco),
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(40.dp),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play icon",
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .weight(1f) // Ocupa 1/4 del ancho
                                                    .wrapContentWidth(Alignment.CenterHorizontally), // Centrado horizontalmente
                                                tint = Gris
                                            )
                                            Spacer(modifier = Modifier.width(4.dp)) // Espacio entre el icono y el texto
                                            Text(
                                                text = key,
                                                color = Gris,
                                                textAlign = TextAlign.Start, // Alinear el texto a la izquierda
                                                modifier = Modifier.weight(3f) // Ocupa 3/4 del ancho
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = descripcion ?: "", color = Blanco, modifier = Modifier.padding(horizontal = 35.dp))
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
            is SeriesDetail -> {
                item {
                    content.apply {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(32.dp))
                                GlideImage(
                                    model = portada ?: "",
                                    contentDescription = "portada",
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = titulo, color = Blanco, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = descripcion ?: "", color = Blanco, modifier = Modifier.padding(horizontal = 35.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Seasons: $temporadas", color = Blanco)
                                Spacer(modifier = Modifier.height(8.dp))
                                // Language selector
                                LazyRow(Modifier.padding(vertical = 6.dp)) {
                                    items(series?.keys?.toList() ?: emptyList()) { language ->
                                        Button(
                                            onClick = {
                                                incrementGlobalCounter(context)
                                                selectedLanguage = language
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Blanco),
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .wrapContentWidth()
                                        ) {
                                            Text(text = language, color = Gris)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Show selected language's episodes
                // Show selected language's episodes
                selectedLanguage?.let { language ->
                    content.series?.get(language)?.forEach { (seasonNumber, episodesMap) ->
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (seasonNumber == "1") {
                                    Text(text = "Language: $language", color = Blanco)
                                }
                                Text(text = "Season $seasonNumber:", color = Blanco)
                                Spacer(modifier = Modifier.height(4.dp))
                                // Ordenar los episodios por número de episodio
                                episodesMap.entries.sortedBy { it.key.toInt() }.forEach { (episodeNumber, episodeLink) ->
                                    val episodeKey = "$seasonNumber-$episodeNumber"
                                    Button(
                                        onClick = {
                                            incrementGlobalCounter(context)
                                            navController.navigate(
                                                "video_screen/${Uri.encode(episodeLink)}/$titulo/true/$seasonNumber/$episodeNumber"
                                            )
                                            val sharedPreferences = context.getSharedPreferences("last_watched_prefs", Context.MODE_PRIVATE)
                                            with(sharedPreferences.edit()) {
                                                putLong("${titulo}-lastWatched", System.currentTimeMillis())
                                                apply()
                                            }
                                            // Update last clicked episode
                                            lastClickedEpisode = episodeKey
                                            with(sharedPreferences.edit()) {
                                                putString("$titulo-lastClickedEpisode", episodeKey)
                                                apply()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (lastClickedEpisode == episodeKey) Color.Green else Blanco
                                        ),
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(40.dp),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play icon",
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .weight(1f) // Ocupa 1/4 del ancho
                                                    .wrapContentWidth(Alignment.CenterHorizontally), // Centrado horizontalmente
                                                tint = Gris
                                            )
                                            Spacer(modifier = Modifier.width(16.dp)) // Espacio entre el icono y el texto
                                            Text(
                                                text = episodeNumber,
                                                color = Gris,
                                                textAlign = TextAlign.Start, // Alinear el texto a la izquierda
                                                modifier = Modifier.weight(1f) // Ocupa 3/4 del ancho
                                            )
                                        }
                                        // Text("Watch chapter $episodeNumber", color = Gris)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Text("Cargando...")
                }
            }
        }
    }
}

@Composable
fun VideoScreen(
    navController: NavHostController,
    videos: String,
    context: Context,
    titulo: String,
    isSeries: Boolean,
    seasonNumber: String,
    episodeKey: String
) {
    val activity = LocalContext.current as ComponentActivity
    val sharedPreferences = activity.getSharedPreferences("video_position", Context.MODE_PRIVATE)

    val sharedPreferences2 = activity.getSharedPreferences("videos", Context.MODE_PRIVATE)
    val existingTitle = sharedPreferences2.getString(titulo, null)
    if (existingTitle == null) {
        with(sharedPreferences2.edit()) {
            putString(titulo, titulo)
            apply()
        }
    }

    // Creamos una clave única para guardar y recuperar la posición del video
    val videoPositionKey = getVideoPositionKey(titulo, isSeries, seasonNumber, episodeKey)

    val videoPlayerState = remember {
        VideoPlayerState(
            sharedPreferences.getInt(videoPositionKey, 0)
        )
    }

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            // Guardar el tiempo actual de reproducción del video al salir
            with(sharedPreferences.edit()) {
                putInt(videoPositionKey, videoPlayerState.currentPosition)
                apply()
                Log.i("sppp", "$titulo $seasonNumber $episodeKey ${videoPlayerState.currentPosition}")
            }
        }
    }

    // Manejar la visibilidad de los botones de navegación
    val windowInsetsController = remember {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrisOscuro),
        contentAlignment = Alignment.Center,
    ) {
        VideoPlayer(
            context = context,
            videos = videos,
            titulo = titulo,
            videoPlayerState = videoPlayerState,
            isSeries = isSeries,
            episodeKey = episodeKey,
            seasonNumber = seasonNumber,
            windowInsetsController = windowInsetsController // Pasamos el controlador de insets al VideoPlayer
        )
    }
}

@Composable
fun VideoPlayer(
    context: Context,
    videos: String,
    titulo: String,
    videoPlayerState: VideoPlayerState,
    isSeries: Boolean,
    episodeKey: String,
    seasonNumber: String,
    windowInsetsController: WindowInsetsControllerCompat?
) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videos)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            seekTo(videoPlayerState.currentPosition.toLong())
        }
    }

    LaunchedEffect(Unit) {
        // Hide the system bars initially
        windowInsetsController?.run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setOnTouchListener { _, motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // Hide the system bars when the player is touched
                                windowInsetsController?.run {
                                    hide(WindowInsetsCompat.Type.systemBars())
                                }
                                false
                            }
                            else -> false
                        }
                    }
                }
            },
            update = {
                it.player = exoPlayer
            }
        )
    ) {
        onDispose {
            videoPlayerState.currentPosition = exoPlayer.currentPosition.toInt()
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L)
            videoPlayerState.currentPosition = exoPlayer.currentPosition.toInt()
            val sharedPreferences = context.getSharedPreferences("video_position", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt(getVideoPositionKey(titulo, isSeries, seasonNumber, episodeKey), videoPlayerState.currentPosition)
                apply()
                Log.i("sppp", "$titulo $seasonNumber $episodeKey ${videoPlayerState.currentPosition}")
            }
        }
    }
}

fun getVideoPositionKey(titulo: String, isSeries: Boolean, seasonNumber: String, episodeKey: String): String {
    return if (isSeries) {
        "$titulo-season$seasonNumber-episode$episodeKey-video"
    } else {
        "$titulo-video"
    }
}

class VideoPlayerState(initialPosition: Int) {
    var currentPosition by mutableStateOf(initialPosition)
}