package fm.libro.wearos.library

import fm.libro.wearos.api.models.Audiobook
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookStore @Inject constructor() {
    private val books = mutableMapOf<String, Audiobook>()

    fun put(book: Audiobook) {
        books[book.isbn] = book
    }

    fun get(isbn: String): Audiobook? = books.remove(isbn)
}
