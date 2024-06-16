package com.guido.darkmovies

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MovieDetail(
    val titulo: String,
    val descripcion: String?,
    val portada: String?,
    val video: String?
)

suspend fun detailScreenMovies(titulo: String): MovieDetail? {
    val db = FirebaseFirestore.getInstance()
    val moviesCollection = db.collection("movies")

    return try {
        val querySnapshot = moviesCollection.whereEqualTo("titulo", titulo).get().await()

        if (!querySnapshot.isEmpty) {
            val document = querySnapshot.documents.first()
            val descripcion = document.getString("descripcion")
            val portada = document.getString("portada")
            val video = document.getString("video")
            MovieDetail(titulo, descripcion, portada, video)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
