package my.webnovels.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import my.webnovels.AppPreferences
import my.webnovels.R
import my.webnovels.data.BookMetadata
import my.webnovels.ui.chaptersList.ChaptersActivity
import my.webnovels.ui.theme.Theme
import my.webnovels.ui.webView.WebViewActivity
import my.webnovels.uiToolbars.ToolbarModeSearch
import my.webnovels.uiUtils.Extra_String
import my.webnovels.uiUtils.copyToClipboard
import my.webnovels.uiViews.BooksVerticalView
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SourceCatalogActivity : ComponentActivity()
{
    class IntentData : Intent, SourceCatalogStateBundle
    {
        override var sourceBaseUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, sourceBaseUrl: String) : super(
            ctx,
            SourceCatalogActivity::class.java
        )
        {
            this.sourceBaseUrl = sourceBaseUrl
        }
    }

    private val viewModel by viewModels<SourceCatalogViewModel>()

    @Inject
    lateinit var appPreferences: AppPreferences

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContent {
            val title = stringResource(R.string.catalog)
            val subtitle = viewModel.source.name.capitalize(Locale.ROOT)

            val searchText = rememberSaveable { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
            val state = rememberLazyListState()
            var optionsExpanded by remember { mutableStateOf(false) }

            Theme(appPreferences = appPreferences) {
                Column {
                    when (toolbarMode.value)
                    {
                        ToolbarMode.MAIN -> ToolbarMain(
                            title = title,
                            subtitle = subtitle,
                            toolbarMode = toolbarMode,
                            onOpenSourceWebPage = ::openSourceWebPage,
                            onPressMoreOptions = { optionsExpanded = !optionsExpanded },
                            optionsDropDownView = {
                                OptionsDropDown(
                                    expanded = optionsExpanded,
                                    onDismiss = { optionsExpanded = !optionsExpanded },
                                    listLayoutMode = appPreferences.BOOKS_LIST_LAYOUT_MODE.value,
                                    onSelectListLayout = {
                                        appPreferences.BOOKS_LIST_LAYOUT_MODE.value = it
                                    }
                                )
                            }
                        )
                        ToolbarMode.SEARCH -> ToolbarModeSearch(
                            focusRequester = focusRequester,
                            searchText = searchText,
                            onClose = {
                                toolbarMode.value = ToolbarMode.MAIN
                                viewModel.startCatalogListMode()
                            },
                            onTextDone = { viewModel.startCatalogSearchMode(searchText.value) },
                            placeholderText = stringResource(R.string.search_by_title)
                        )
                    }
                    BooksVerticalView(
                        layoutMode = viewModel.listLayout,
                        list = viewModel.fetchIterator.list,
                        listState = state,
                        error = viewModel.fetchIterator.error,
                        loadState = viewModel.fetchIterator.state,
                        onLoadNext = { viewModel.fetchIterator.fetchNext() },
                        onBookClicked = ::openBookPage,
                        onBookLongClicked = ::addBookToLibrary,
                        onReload = { viewModel.fetchIterator.reloadFailedLastLoad() },
                        onCopyError = ::copyToClipboard
                    )
                }
            }
        }
    }

    fun openSourceWebPage()
    {
        WebViewActivity.IntentData(this, viewModel.sourceBaseUrl).let(::startActivity)
    }

    fun openBookPage(book: BookMetadata)
    {
        ChaptersActivity.IntentData(
            this,
            bookMetadata = book
        ).let(::startActivity)
    }

    fun addBookToLibrary(book: BookMetadata)
    {
        viewModel.addToLibraryToggle(book)
    }
}
