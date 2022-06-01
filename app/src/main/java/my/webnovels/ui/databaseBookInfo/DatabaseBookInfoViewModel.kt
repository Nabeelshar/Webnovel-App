package my.webnovels.ui.databaseBookInfo

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import my.webnovels.data.BookMetadata
import my.webnovels.scraper.*
import my.webnovels.ui.BaseViewModel
import my.webnovels.uiUtils.StateExtra_String
import javax.inject.Inject

interface DatabaseBookInfoStateBundle {
    val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
    val database get() = scraper.getCompatibleDatabase(databaseUrlBase)!!

    var databaseUrlBase: String
    var bookUrl: String
    var bookTitle: String
}

@HiltViewModel
class DatabaseBookInfoViewModel @Inject constructor(
    state: SavedStateHandle
) : BaseViewModel(), DatabaseBookInfoStateBundle {
    override var databaseUrlBase: String by StateExtra_String(state)
    override var bookUrl: String by StateExtra_String(state)
    override var bookTitle: String by StateExtra_String(state)

    val bookData = flow {

        // Preview before loading
        emit(
            Response.Success(
                DatabaseInterface.BookData(
                    title = bookTitle,
                    description = "",
                    coverImageUrl = "",
                    alternativeTitles = listOf(),
                    authors = listOf(),
                    tags = listOf(),
                    genres = listOf(),
                    bookType = "",
                    relatedBooks = listOf(),
                    similarRecommended = listOf()
                )
            )
        )

        val res = tryConnect {
            val doc = fetchDoc(bookMetadata.url)
            Response.Success(database.getBookData(doc))
        }
        emit(res)
    }.flowOn(Dispatchers.IO)
}