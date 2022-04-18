package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.reader.ReaderActivity
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiToolbars.ToolbarModeSearch
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.toast
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersActivity : ComponentActivity()
{
    class IntentData : Intent, ChapterStateBundle
    {
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(
            ctx,
            ChaptersActivity::class.java
        )
        {
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    lateinit var appPreferences: AppPreferences
    val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val focusRequester = remember { FocusRequester() }
            val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
            val listState = rememberLazyListState()
            val sourceName = remember(viewModel.book.url) {
                scraper.getCompatibleSource(viewModel.bookUrl)?.name ?: ""
            }

            Theme(appPreferences = appPreferences) {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colors.isLight
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                var isRefreshingDelayed by remember {
                    mutableStateOf(viewModel.isRefreshing)
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { viewModel.isRefreshing }
                        .distinctUntilChanged()
                        .collectLatest {
                            if (it) delay(200)
                            isRefreshingDelayed = it
                        }
                }

                Box {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshingDelayed),
                        onRefresh = {
                            isRefreshingDelayed = true
                            toast(getString(R.string.updating_book_info))
                            viewModel.reloadAll()
                        }
                    ) {
                        ChaptersListView(
                            header = {
                                HeaderView(
                                    bookTitle = viewModel.bookTitle,
                                    sourceName = sourceName,
                                    numberOfChapters = 34,
                                    bookCover = viewModel.book.coverImageUrl,
                                    description = viewModel.book.description,
                                    onSearchBookInDatabase = ::searchBookInDatabase,
                                    onOpenInBrowser = ::openInBrowser,
                                )
                            },
                            list = viewModel.chaptersWithContext,
                            selectedChapters = viewModel.selectedChaptersUrl,
                            error = viewModel.error,
                            listState = listState,
                            onClick = ::onClickChapter,
                            onLongClick = ::onLongClickChapter,
                        )
                    }

                    when (toolbarMode.value)
                    {
                        ToolbarMode.MAIN -> MainToolbar(
                            bookTitle = viewModel.book.title,
                            isBookmarked = viewModel.book.inLibrary,
                            listState = listState,
                            onClickBookmark = ::bookmarkToggle,
                            onClickSortChapters = viewModel::toggleChapterSort,
                            onClickChapterTitleSearch = { toolbarMode.value = ToolbarMode.SEARCH },

                            )
                        ToolbarMode.SEARCH -> ToolbarModeSearch(
                            focusRequester = focusRequester,
                            searchText = viewModel.textSearch,
                            onClose = { toolbarMode.value = ToolbarMode.MAIN },
                            onTextDone = {},
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        1f to MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(
                                        bottomStart = 32.dp,
                                        bottomEnd = 32.dp
                                    )
                                )
                                .background(MaterialTheme.colors.primary)
                                .padding(top = 16.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = ::onOpenLastActiveChapter,
                        backgroundColor = ColorAccent,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 30.dp, bottom = 100.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_play_arrow_24),
                            contentDescription = stringResource(id = R.string.open_last_read_chapter),
                            tint = MaterialTheme.colors.primary
                        )
                    }

                    AnimatedVisibility(
                        visible = viewModel.selectedChaptersUrl.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 160.dp),
                    ) {
                        BackHandler(onBack = viewModel::closeSelectionMode)
                        SelectionToolsBar(
                            onDeleteDownload = viewModel::deleteDownloadSelected,
                            onDownload = viewModel::downloadSelected,
                            onSetRead = viewModel::setSelectedAsRead,
                            onSetUnread = viewModel::setSelectedAsUnread,
                            onSelectAllChapters = viewModel::selectAll,
                            onSelectAllChaptersAfterSelectedOnes = viewModel::selectAllAfterSelectedOnes,
                            onCloseSelectionbar = viewModel::closeSelectionMode,
                        )
                    }
                }
            }
        }
    }

    fun onClickChapter(chapter: ChapterWithContext)
    {
        when
        {
            viewModel.selectedChaptersUrl.containsKey(chapter.chapter.url) ->
                viewModel.selectedChaptersUrl.remove(chapter.chapter.url)
            viewModel.selectedChaptersUrl.isNotEmpty() ->
                viewModel.selectedChaptersUrl[chapter.chapter.url] = Unit
            else -> openBookAtChapter(chapter.chapter.url)
        }
    }

    fun onLongClickChapter(chapter: ChapterWithContext)
    {
        when
        {
            viewModel.selectedChaptersUrl.containsKey(chapter.chapter.url) ->
                viewModel.selectedChaptersUrl.remove(chapter.chapter.url)
            else ->
                viewModel.selectedChaptersUrl[chapter.chapter.url] = Unit
        }
    }

    fun onOpenLastActiveChapter()
    {
        val bookUrl = viewModel.bookMetadata.url
        lifecycleScope.launch(Dispatchers.IO) {
            val lastReadChapter = viewModel.getLastReadChapter()
            if (lastReadChapter == null)
            {
                toast(getString(R.string.no_chapters))
                return@launch
            }

            withContext(Dispatchers.Main) {
                ReaderActivity
                    .IntentData(
                        this@ChaptersActivity,
                        bookUrl = bookUrl,
                        chapterUrl = lastReadChapter
                    )
                    .let(this@ChaptersActivity::startActivity)
            }
        }
    }


    fun bookmarkToggle()
    {
        lifecycleScope.launch {
            val isBookmarked = viewModel.toggleBookmark()
            val msg = if (isBookmarked) R.string.added_to_library else R.string.removed_from_library
            withContext(Dispatchers.Main) {
                toast(getString(msg))
            }
        }
    }

    fun searchBookInDatabase()
    {
        DatabaseSearchResultsActivity
            .IntentData(
                this,
                "https://www.novelupdates.com/",
                DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookTitle)
            )
            .let(this::startActivity)
    }

    fun openInBrowser()
    {
        Intent(Intent.ACTION_VIEW).also {
            it.data = Uri.parse(viewModel.bookMetadata.url)
        }.let(this::startActivity)
    }


    fun openBookAtChapter(chapterUrl: String)
    {
        ReaderActivity
            .IntentData(
                this,
                bookUrl = viewModel.bookMetadata.url,
                chapterUrl = chapterUrl
            )
            .let(::startActivity)
    }
}
