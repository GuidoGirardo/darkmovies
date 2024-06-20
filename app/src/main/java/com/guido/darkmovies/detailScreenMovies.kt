package com.guido.darkmovies

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MovieDetail(
    val titulo: String,
    val descripcion: String?,
    val portada: String?,
    val videos: Map<String, String>?
)

data class SeriesDetail(
    val titulo: String,
    val descripcion: String?,
    val portada: String?,
    val series: Map<String, Map<String, Map<String, String>>>?,
    val temporadas: Int?
)

suspend fun detailScreenMovies(titulo: String): Any? {
    val db = FirebaseFirestore.getInstance()
    val moviesCollection = db.collection("movies")

    return try {
        val querySnapshot = moviesCollection.whereEqualTo("titulo", titulo).get().await()

        if (!querySnapshot.isEmpty) {
            val document = querySnapshot.documents.first()
            val descripcion = document.getString("descripcion")
            val portada = document.getString("portada")
            val temporadas = document.getLong("temporadas")?.toInt()

            if (temporadas != null && temporadas > 0) {
                // Es una serie
                val videos = document.get("videos") as? Map<String, Map<String, Map<String, String>>>
                SeriesDetail(titulo, descripcion, portada, videos, temporadas)
            } else {
                // Es una película
                val videos = document.get("videos") as? Map<String, String>
                MovieDetail(titulo, descripcion, portada, videos)
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
