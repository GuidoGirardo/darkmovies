package com.guido.darkmovies

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun fetchMoviesFromFirestoreByTitles(context: Context, movieVistas: MutableList<MovieWithTitleAndPortada>) {
    val sharedPreferences = context.getSharedPreferences("video_position", Context.MODE_PRIVATE)
    val keys = sharedPreferences.all.keys

    val db = FirebaseFirestore.getInstance()
    try {
        for (key in keys) {
            val querySnapshot = db.collection("movies")
                .whereEqualTo("titulo", key)
                .get()
                .await()

            for (document in querySnapshot.documents) {
                val titulo = document.getString("titulo") ?: ""
                val portada = document.getString("portada") ?: ""
                movieVistas.add(MovieWithTitleAndPortada(titulo, portada))
            }
        }
    } catch (e: Exception) { }
}

data class MovieWithTitleAndPortada(
    val titulo: String,
    val portada: String
)