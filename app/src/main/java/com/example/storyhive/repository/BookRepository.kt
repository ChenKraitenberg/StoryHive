package com.example.storyhive.repository

import com.example.storyhive.data.models.Book
import com.example.storyhive.data.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface BookRepository {
    suspend fun searchBooks(query: String): List<Book>
    suspend fun getBookDetails(bookId: String): Book
    suspend fun getBookReviews(bookId: String): List<Review>
}

class FirebaseBookRepository : BookRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val booksCollection = firestore.collection("books")

    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            booksCollection
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
                .get()
                .await()
                .toObjects(Book::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getBookDetails(bookId: String): Book {
        return booksCollection
            .document(bookId)
            .get()
            .await()
            .toObject(Book::class.java) ?: throw Exception("Book not found")
    }

    override suspend fun getBookReviews(bookId: String): List<Review> {
        return booksCollection
            .document(bookId)
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Review::class.java)
    }
}