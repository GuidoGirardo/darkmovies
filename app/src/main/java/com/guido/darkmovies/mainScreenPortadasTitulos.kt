package com.guido.darkmovies

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MoviesMainScreen(
    val titulo: String = "",
    val portada: String = ""
)

suspend fun mainScreenPortadasTitulos(movies: MutableList<MoviesMainScreen>) {
    val db = FirebaseFirestore.getInstance()
    try {
        val result = db.collection("movies").get().await()
        for (document in result) {
            val titulo = document.getString("titulo") ?: ""
            val portada = document.getString("portada") ?: ""
            movies.add(MoviesMainScreen(titulo, portada))
        }
    } catch (e: Exception) {
        // manejar error
    }
}