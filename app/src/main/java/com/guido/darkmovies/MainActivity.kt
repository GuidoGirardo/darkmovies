package com.guido.darkmovies

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
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
                            "video_screen/{videos}/{titulo}/{isSeries}/{episodeKey}",
                            arguments = listOf(
                                navArgument("videos") { defaultValue = "" },
                                navArgument("titulo") { defaultValue = "" },
                                navArgument("isSeries") { defaultValue = false },
                                navArgument("episodeKey") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            VideoScreen(
                                navController,
                                backStackEntry.arguments?.getString("videos") ?: "",
                                context = context,
                                backStackEntry.arguments?.getString("titulo") ?: "",
                                isSeries = backStackEntry.arguments?.getBoolean("isSeries") ?: false,
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
    LaunchedEffect(titulo) {
        detail.value = detailScreenMovies(titulo)
    }

    Column {
        when (val content = detail.value) {
            is MovieDetail -> {
                content.apply {
                    GlideImage(
                        model = portada ?: "",
                        contentDescription = "portada",
                        modifier = Modifier.width(200.dp)
                    )
                    Text(text = titulo)
                    Text(text = descripcion ?: "")
                    videos?.forEach { (key, value) ->
                        Button(onClick = {
                            navController.navigate("video_screen/${Uri.encode(value)}/$titulo/false/0")
                        }) {
                            Text("watch $key")
                        }
                    }
                }
            }
            is SeriesDetail -> {
                content.apply {
                    GlideImage(
                        model = portada ?: "",
                        contentDescription = "portada",
                        modifier = Modifier.width(200.dp)
                    )
                    Text(text = titulo)
                    Text(text = descripcion ?: "")
                    series?.forEach { (key, value) ->
                        Column {
                            value.forEach { (chapterKey, chapterLink) ->
                                Button(onClick = {
                                    navController.navigate("video_screen/${Uri.encode(chapterLink.toString())}/$titulo/true/$chapterKey")
                                }) {
                                    Text("Watch Episode $chapterKey")
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Text("Loading...")
            }
        }
    }
}

@Composable
fun VideoScreen(navController: NavHostController, videos: String, context: Context, titulo: String, isSeries: Boolean, episodeKey: String) {
    val activity = LocalContext.current as ComponentActivity
    val sharedPreferences = activity.getSharedPreferences("video_position", Context.MODE_PRIVATE)

    val videoPlayerState = remember {
        VideoPlayerState(
            sharedPreferences.getInt(getVideoPositionKey(titulo, isSeries, episodeKey), 0)
        )
    }

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            // Guardar el tiempo actual de reproducción del video al salir
            with(sharedPreferences.edit()) {
                putInt(getVideoPositionKey(titulo, isSeries, episodeKey), videoPlayerState.currentPosition)
                apply()
                Log.i("sp", "$titulo $episodeKey ${videoPlayerState.currentPosition}")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VideoPlayer(context = context, videos = videos, titulo = titulo, videoPlayerState = videoPlayerState, isSeries = isSeries, episodeKey = episodeKey)
    }
}

fun getVideoPositionKey(titulo: String, isSeries: Boolean, episodeKey: String = ""): String {
    return if (isSeries) {
        "$titulo-$episodeKey-episode"
    } else {
        "$titulo-video"
    }
}

class VideoPlayerState(initialPosition: Int) {
    var currentPosition by mutableStateOf(initialPosition)
}

@Composable
fun VideoPlayer(context: Context, videos: String, titulo: String, videoPlayerState: VideoPlayerState, isSeries: Boolean, episodeKey: String) {
    val mediaController = remember { MediaController(context) }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(videos)
                setMediaController(mediaController)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.seekTo(videoPlayerState.currentPosition)
                    start()
                }

                setOnTouchListener { _, motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            videoPlayerState.currentPosition = currentPosition
                            val sharedPreferences = context.getSharedPreferences("video_position", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putInt(getVideoPositionKey(titulo, isSeries, episodeKey), videoPlayerState.currentPosition)
                                apply()
                                Log.i("sp", "$titulo $episodeKey ${videoPlayerState.currentPosition}")
                            }
                            false
                        }
                        else -> false
                    }
                }

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
        val videoView = VideoView(context)
        videoView.setVideoPath(videos)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.seekTo(videoPlayerState.currentPosition)
            mediaPlayer.start()
        }

        onDispose {
            videoView.stopPlayback()
        }
    }
}