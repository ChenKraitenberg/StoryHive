package com.example.storyhive.repository

import com.example.storyhive.data.models.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Interface defining repository functions for fetching book-related data.
 */
interface BookRepository {
    /**
     * Searches for books based on a query string.
     * @param query The search term to match book titles.
     * @return A list of books matching the search criteria.
     */
    suspend fun searchBooks(query: String): List<Book>

    /**
     * Retrieves detailed information about a specific book.
     * @param bookId The unique ID of the book.
     * @return A Book object containing the book details.
     */
    suspend fun getBookDetails(bookId: String): Book
}

//Implementation of BookRepository using Firebase Firestore as the data source
class FirebaseBookRepository : BookRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val booksCollection = firestore.collection("books")

    /**
     * Searches for books in Firestore by title using a range query.
     * @param query The search term.
     * @return A list of books that match the query.
     */
    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            booksCollection
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')  // Unicode trick for Firestore range queries
                .get()
                .await()
                .toObjects(Book::class.java)
        } catch (e: Exception) {
            emptyList() // Returns an empty list in case of an error
        }
    }

    /**
     * Retrieves details of a book from Firestore by its document ID.
     * @param bookId The ID of the book document.
     * @return The Book object.
     * @throws Exception if the book is not found.
     */
    override suspend fun getBookDetails(bookId: String): Book {
        return booksCollection
            .document(bookId)
            .get()
            .await()
            .toObject(Book::class.java) ?: throw Exception("Book not found")
    }

}