package my.noveldokusha

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object bookstore
{
	data class BookMetadata(var title: String, var url: String)
	{
		override fun equals(other: Any?): Boolean = if (other is BookMetadata) (url == other.url) else false
		override fun hashCode(): Int = url.hashCode()
	}
	
	data class ChapterMetadata(var title: String, var url: String)
	{
		override fun equals(other: Any?): Boolean = if (other is ChapterMetadata) (url == other.url) else false
		override fun hashCode(): Int = url.hashCode()
	}
	
	data class LastReadChapter(var url: String, var position: Int, var offset: Int)
	
	private lateinit var db_context: Context
	fun setContext(context: Context) = ::db_context.set(context)
	
	@Entity
	data class Book(
		val title: String,
		@PrimaryKey val url: String,
		val completed: Boolean = false,
		val lastReadChapter: String? = null
	)
	
	@Entity
	data class Chapter(
		val title: String,
		@PrimaryKey val url: String,
		val bookUrl: String,
		val read: Boolean = false,
		val lastReadPosition: Int = 0,
		val lastReadOffset: Int = 0
	)
	
	@Entity
	data class ChapterBody(@PrimaryKey val url: String, val body: String)
	
	@Dao
	interface LibraryDao
	{
		@Query("SELECT * FROM Book")
		fun booksFlow(): Flow<List<Book>>
		
		@Query("SELECT lastReadChapter FROM Book WHERE url == :bookUrl")
		fun bookLastReadChapterFlow(bookUrl: String): Flow<String?>
		
		@Query("SELECT * FROM Book WHERE completed = 0")
		fun booksReadingFlow(): Flow<List<Book>>
		
		@Query("SELECT * FROM Book WHERE completed = 1")
		fun booksCompletedFlow(): Flow<List<Book>>
		
		@Insert(onConflict = OnConflictStrategy.IGNORE)
		suspend fun insert(book: Book)
		
		@Delete
		suspend fun remove(book: Book)
		
		@Update
		suspend fun update(book: Book)
		
		@Query("SELECT * FROM Book WHERE url = :url")
		suspend fun get(url: String): Book?
		
		@Query("SELECT EXISTS(SELECT * FROM Book WHERE url == :url)")
		suspend fun exist(url: String): Boolean
		
		@Query("SELECT EXISTS(SELECT * FROM Book WHERE url == :url)")
		fun existFlow(url: String): Flow<Boolean>
	}
	
	@Dao
	interface ChapterDao
	{
		@Query("SELECT * FROM Chapter WHERE bookUrl = :bookUrl")
		fun chaptersFlow(bookUrl: String): Flow<List<Chapter>>
		
		@Query("SELECT * FROM Chapter WHERE bookUrl = :bookUrl")
		suspend fun chapters(bookUrl: String): List<Chapter>
		
		@Query("SELECT EXISTS(SELECT * FROM Chapter WHERE bookUrl = :bookUrl)")
		suspend fun existBookChapters(bookUrl: String): Boolean
		
		@Update
		suspend fun update(chapter: Chapter)
		
		@Update
		suspend fun update(chapters: List<Chapter>)
		
		@Query("UPDATE Chapter SET read = 1 WHERE url in (:chaptersUrl)")
		suspend fun setAsRead(chaptersUrl: List<String>)
		
		@Insert(onConflict = OnConflictStrategy.IGNORE)
		suspend fun insert(chapters: List<Chapter>)
		
		@Query("SELECT * FROM Chapter WHERE url = :url")
		suspend fun get(url: String): Chapter?
		
		@Query("SELECT COUNT(*) FROM Chapter WHERE bookUrl = :bookUrl AND read = 0")
		fun numberOfUnreadChaptersFlow(bookUrl: String): Flow<Int>
		
		@Query("SELECT lastReadPosition FROM Chapter WHERE bookUrl = :bookUrl AND url =:chapterUrl")
		fun lastReadPositionChapterFlow(bookUrl: String, chapterUrl: String): Flow<Int>
		
		@Query("DELETE FROM Chapter WHERE Chapter.bookUrl NOT IN (SELECT Book.url FROM Book)")
		suspend fun removeAllNonLibraryRows(): Unit
	}
	
	@Dao
	interface ChapterBodyDao
	{
		@Query("SELECT EXISTS(SELECT * FROM ChapterBody WHERE url == :url)")
		suspend fun exists(url: String): Boolean
		
		@Insert(onConflict = OnConflictStrategy.REPLACE)
		suspend fun insert(chapterBody: ChapterBody)
		
		@Query("SELECT EXISTS(SELECT * FROM ChapterBody WHERE url == :url)")
		fun existsFlow(url: String): Flow<Boolean>
		
		@Query("SELECT * FROM ChapterBody WHERE url = :url")
		suspend fun get(url: String): ChapterBody?
		
		@Query("SELECT url FROM ChapterBody WHERE url IN (SELECT url FROM Chapter	WHERE bookUrl == :bookUrl)")
		fun getExistBodyChapterUrlsFlow(bookUrl: String): Flow<List<String>>
		
		@Query("DELETE FROM ChapterBody WHERE ChapterBody.url NOT IN (SELECT Chapter.url FROM Chapter)")
		suspend fun removeAllNonChapterRows(): Unit
	}
	
	@Database(entities = [Book::class, Chapter::class, ChapterBody::class], version = 1, exportSchema = false)
	abstract class LibraryDatabase : RoomDatabase()
	{
		abstract fun libraryDao(): LibraryDao
		abstract fun chapterDao(): ChapterDao
		abstract fun chapterBodyDao(): ChapterBodyDao
	}
	
	private const val db_name = "bookEntry"
	
	private val db by lazy {
		Room.databaseBuilder(
			db_context.applicationContext,
			LibraryDatabase::class.java,
			db_name
		).build()
	}
	
	fun getDatabaseSizeBytes() = db_context.getDatabasePath(db_name).length()
	
	object settings
	{
		suspend fun clearNonLibraryData()
		{
			db.chapterDao().removeAllNonLibraryRows()
			db.chapterBodyDao().removeAllNonChapterRows()
		}
	}
	
	object bookLibrary
	{
		val booksFlow by lazy { db.libraryDao().booksFlow() }
		val booksReadingFlow by lazy { db.libraryDao().booksReadingFlow() }
		val booksCompletedFlow by lazy { db.libraryDao().booksCompletedFlow() }
		fun bookLastReadChapterFlow(bookUrl: String) = db.libraryDao().bookLastReadChapterFlow(bookUrl)
		fun existFlow(url: String) = db.libraryDao().existFlow(url)
		suspend fun insert(book: Book) = if (isValid(book)) db.libraryDao().insert(book) else Unit
		suspend fun remove(book: Book) = db.libraryDao().remove(book)
		suspend fun update(book: Book) = db.libraryDao().update(book)
		suspend fun get(url: String) = db.libraryDao().get(url)
		suspend fun exist(url: String) = db.libraryDao().exist(url)
	}
	
	object bookChapter
	{
		fun numberOfUnreadChaptersFlow(bookUrl: String) = db.chapterDao().numberOfUnreadChaptersFlow(bookUrl)
		suspend fun update(chapter: Chapter) = db.chapterDao().update(chapter)
		suspend fun update(chapters: List<Chapter>) = db.chapterDao().update(chapters)
		suspend fun get(url: String) = db.chapterDao().get(url)
		suspend fun setAsRead(chaptersUrl: List<String>) = chaptersUrl.chunked(500).forEach { db.chapterDao().setAsRead(it) }
		suspend fun insert(chapters: List<Chapter>) = db.chapterDao().insert(chapters.filter(::isValid))
		suspend fun chapters(bookUrl: String) = db.chapterDao().chapters(bookUrl)
		fun chaptersFlow(bookUrl: String) = db.chapterDao().chaptersFlow(bookUrl)
		suspend fun existBookChapters(bookUrl: String) = db.chapterDao().existBookChapters(bookUrl)
	}
	
	object bookChapterBody
	{
		fun existsFlow(url: String) = db.chapterBodyDao().existsFlow(url)
		suspend fun exists(url: String) = db.chapterBodyDao().exists(url)
		suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String>
		{
			if (tryCache) db.chapterBodyDao().get(urlChapter)?.let {
				return@fetchBody Response.Success(it.body)
			}
			
			val res = downloadChapter(urlChapter)
			if (res is Response.Success)
				db.chapterBodyDao().insert(ChapterBody(url = urlChapter, body = res.data))
			return res
		}
		
		fun getExistBodyChapterUrlsFlow(bookUrl: String): Flow<Set<String>> =
			db.chapterBodyDao().getExistBodyChapterUrlsFlow(bookUrl).map { it.toSet() }
	}
	
	fun isValid(book: Book): Boolean = book.url.matches("""^https?://.*""".toRegex())
	fun isValid(chapter: Chapter): Boolean = chapter.url.matches("""^https?://.*""".toRegex())
}
