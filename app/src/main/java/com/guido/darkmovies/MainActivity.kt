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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
                        composable("main_screen") { MainScreen(navController) }
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
                            "video_screen/{videos}",
                            arguments = listOf(navArgument("videos") { defaultValue = "" })
                        ) { backStackEntry ->
                            VideoScreen(
                                navController,
                                backStackEntry.arguments?.getString("videos") ?: "",
                                context = context
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
fun MainScreen(navController: NavHostController) {
    val movies = remember { mutableStateListOf<MoviesMainScreen>() }
    LaunchedEffect(Unit) {
        mainScreenPortadasTitulos(movies)
    }
    LazyRow{
        items(movies) { movie ->
            Column(
                modifier = Modifier.clickable{
                    navController.navigate("detail_screen/${movie.titulo}")
                }
            ){
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
            Text(movie.videos ?: "")
            Button(onClick = {
                navController.navigate("video_screen/${Uri.encode(movie.videos)}")
            }) {
                Text("watch")
            }
        }
    }
}

@Composable
fun VideoScreen(navController: NavHostController, videos: String, context: Context) {
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VideoPlayer(context = context, videos = videos)
    }
}

@Composable
fun VideoPlayer(context: Context, videos: String) {
    val mediaController = remember { MediaController(context) }
    var position by rememberSaveable { mutableStateOf(0) }
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(videos)
                setMediaController(mediaController)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.seekTo(position)
                    start()
                }
            }
        },
        update = { view ->
            view.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.seekTo(position)
                mediaPlayer.start()
            }
        },
        onRelease = {
            it.pause()
            position = it.currentPosition
        }
    )
}