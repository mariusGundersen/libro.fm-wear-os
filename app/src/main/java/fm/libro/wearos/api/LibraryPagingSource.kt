package fm.libro.wearos.api

import androidx.paging.PagingSource
import androidx.paging.PagingState
import fm.libro.wearos.api.models.Audiobook
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LibraryPagingSource(
    private val token: String,
) : PagingSource<Int, Audiobook>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Audiobook> {
        val page = params.key ?: 1
        return try {
            val response = LibroFmClient.getLibrary(token, page)
            val books = coroutineScope {
                response.audiobooks.map { book ->
                    async {
                        try {
                            val manifest = LibroFmClient.getDownloadManifest(token, book.isbn)
                            book.manifest = manifest
                        } catch (_: Exception) {
                        }
                        book
                    }
                }.map { it.await() }
            }
            LoadResult.Page(
                data = books,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Audiobook>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }
}
