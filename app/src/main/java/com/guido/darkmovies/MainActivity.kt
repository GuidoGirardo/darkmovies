package com.guido.darkmovies

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
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
                            "video_screen/{videos}/{titulo}",
                            arguments = listOf(
                                navArgument("videos") { defaultValue = "" },
                                navArgument("titulo") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            VideoScreen(
                                navController,
                                backStackEntry.arguments?.getString("videos") ?: "",
                                context = context,
                                backStackEntry.arguments?.getString("titulo") ?: "",
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
    Column() {
        TextField(
            value = searchTerm.value,
            onValueChange = { searchTerm.value = it },
            label = { Text("Search by movie title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Continue wacthing :)")
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
    val movieDetail = remember { mutableStateOf<MovieDetail?>(null) }
    LaunchedEffect(titulo) {
        movieDetail.value = detailScreenMovies(titulo)
    }

    Column {
        movieDetail.value?.let { movie ->
            GlideImage(
                model = movie.portada ?: "",
                contentDescription = "portada",
                modifier = Modifier.width(200.dp)
            )
            Text(text = movie.titulo)
            Text(text = movie.descripcion ?: "")
            movie.videos?.forEach { (key, value) ->
                Button(onClick = {
                    navController.navigate("video_screen/${Uri.encode(value)}/${movie.titulo}")
                }) {
                    Text("watch $key")
                }
            }
        }
    }
}

@Composable
fun VideoScreen(navController: NavHostController, videos: String, context: Context, titulo: String) {
    val activity = LocalContext.current as ComponentActivity
    val sharedPreferences = activity.getSharedPreferences("video_position", Context.MODE_PRIVATE)

    val videoPlayerState = remember {
        VideoPlayerState(
            sharedPreferences.getInt(titulo, 0)
        )
    }

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            // Guardar el tiempo actual de reproducción del video al salir
            with(sharedPreferences.edit()) {
                putInt(titulo, videoPlayerState.currentPosition)
                Log.i("sp", videoPlayerState.currentPosition.toString())
                apply()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VideoPlayer(context = context, videos = videos, titulo = titulo, videoPlayerState = videoPlayerState)
    }
}


class VideoPlayerState(initialPosition: Int) {
    var currentPosition by mutableStateOf(initialPosition)
}

@Composable
fun VideoPlayer(context: Context, videos: String, titulo: String, videoPlayerState: VideoPlayerState) {
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

                setOnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        videoPlayerState.currentPosition = currentPosition
                    }
                    false
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