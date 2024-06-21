package com.guido.darkmovies

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.guido.darkmovies.ui.theme.DarkmoviesTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val movies = remember { mutableStateListOf<MoviesMainScreen>() }
    val moviesVistas = remember { mutableStateListOf<MovieWithTitleAndPortada>() }
    val searchTerm = remember { mutableStateOf(TextFieldValue()) }
    LaunchedEffect(Unit) {
        mainScreenPortadasTitulos(movies)
        fetchMoviesFromFirestoreByTitles(context, moviesVistas)
    }
    Column {
        TextField(
            value = searchTerm.value,
            onValueChange = { searchTerm.value = it },
            label = { Text("Search by movie title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Continue watching :)")
        LazyRow {
            items(moviesVistas) { movie ->
                Column(
                    modifier = Modifier.clickable {
                        navController.navigate("detail_screen/${movie.titulo}")
                    }
                ) {
                    GlideImage(
                        model = movie.portada,
                        contentDescription = "portada",
                        modifier = Modifier.width(80.dp)
                    )
                    Text(movie.titulo)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        LazyRow {
            val filteredMovies = movies.filter {
                it.titulo.contains(searchTerm.value.text, ignoreCase = true)
            }
            items(filteredMovies) { movie ->
                Column(
                    modifier = Modifier.clickable {
                        navController.navigate("detail_screen/${movie.titulo}")
                    }
                ) {
                    GlideImage(
                        model = movie.portada,
                        contentDescription = "portada",
                        modifier = Modifier.width(80.dp)
                    )
                    Text(movie.titulo)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DetailScreen(navController: NavHostController, titulo: String) {
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

    LazyColumn {
        when (val content = detail.value) {
            is MovieDetail -> {
                item {
                    content.apply {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                GlideImage(
                                    model = portada ?: "",
                                    contentDescription = "portada",
                                    modifier = Modifier.width(200.dp).height(300.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Text(text = titulo)
                                Text(text = descripcion ?: "")
                                videos?.forEach { (key, value) ->
                                    Button(onClick = {
                                        navController.navigate("video_screen/${Uri.encode(value)}/$titulo/false/0/0")
                                    }) {
                                        Text("Ver $key")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is SeriesDetail -> {
                item {
                    content.apply {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                        GlideImage(
                            model = portada ?: "",
                            contentDescription = "portada",
                            modifier = Modifier.width(200.dp).height(300.dp),
                            contentScale = ContentScale.Crop
                        )
                        Text(text = titulo)
                        Text(text = descripcion ?: "")
                        Text(text = "Temporadas: $temporadas")

                        // Selector de idioma
                        Row(Modifier.padding(vertical = 8.dp)) {
                            series?.keys?.forEach { language ->
                                Button(
                                    onClick = {
                                        selectedLanguage = language
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(text = language)
                                }
                            }
                        }
                    }
                }

                selectedLanguage?.let { language ->
                    content.series?.get(language)?.forEach { (seasonNumber, episodesMap) ->
                        this@LazyColumn.item {
                            Column(modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (seasonNumber == "1") {
                                    Text(text = "Idioma: $language")
                                }
                                Text(text = "Temporada $seasonNumber:")
                                episodesMap.forEach { (episodeNumber, episodeLink) ->
                                    val episodeKey = "$seasonNumber-$episodeNumber"
                                    Button(
                                        onClick = {
                                            navController.navigate(
                                                "video_screen/${Uri.encode(episodeLink)}/$titulo/true/$seasonNumber/$episodeNumber"
                                            )
                                            // Update last clicked episode
                                            lastClickedEpisode = episodeKey
                                            with(sharedPreferences.edit()) {
                                                putString("$titulo-lastClickedEpisode", episodeKey)
                                                apply()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (lastClickedEpisode == episodeKey) Color.Green else Color.Gray
                                        )
                                    ) {
                                        Text("Ver Episodio $episodeNumber")
                                    }
                                }
                            }
                        }
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VideoPlayer(context = context, videos = videos, titulo = titulo, videoPlayerState = videoPlayerState, isSeries = isSeries, episodeKey = episodeKey, seasonNumber = seasonNumber)
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

@Composable
fun VideoPlayer(
    context: Context,
    videos: String,
    titulo: String,
    videoPlayerState: VideoPlayerState,
    isSeries: Boolean,
    episodeKey: String,
    seasonNumber: String
) {
    val mediaController = remember { MediaController(context) }

    // A variable to store the VideoView instance
    var videoViewInstance: VideoView? by remember { mutableStateOf(null) }

    // LaunchedEffect to periodically save video position
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L) // 10 seconds delay
            videoViewInstance?.let { videoView ->
                videoPlayerState.currentPosition = videoView.currentPosition
                val sharedPreferences = context.getSharedPreferences("video_position", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putInt(getVideoPositionKey(titulo, isSeries, seasonNumber, episodeKey), videoPlayerState.currentPosition)
                    apply()
                    Log.i("sppp", "$titulo $episodeKey ${videoPlayerState.currentPosition}")
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(videos)
                setMediaController(mediaController)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.seekTo(videoPlayerState.currentPosition)
                    start()
                }

                // Assign the VideoView instance to the variable
                videoViewInstance = this

                /* setOnTouchListener { _, motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            videoPlayerState.currentPosition = currentPosition
                            val sharedPreferences = context.getSharedPreferences("video_position", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putInt(getVideoPositionKey(titulo, isSeries, seasonNumber, episodeKey), videoPlayerState.currentPosition)
                                apply()
                                Log.i("sppp", "$titulo $episodeKey ${videoPlayerState.currentPosition}")
                            }
                            false
                        }
                        else -> false
                    }
                } */

                setOnKeyListener { _, keyCode, _ ->
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            videoPlayerState.currentPosition = currentPosition
                            false
                        }
                        else -> false
                    }
                }
            }
        },
        update = { view ->
            view.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.seekTo(videoPlayerState.currentPosition)
                mediaPlayer.start()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            videoViewInstance?.stopPlayback()
        }
    }
}