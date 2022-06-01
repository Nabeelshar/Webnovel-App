package my.webnovels.ui.globalSourceSearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import my.webnovels.AppPreferences
import my.webnovels.scraper.FetchIteratorState
import my.webnovels.scraper.scraper
import my.webnovels.scraper.SourceInterface
import my.webnovels.ui.BaseViewModel
import my.webnovels.uiUtils.StateExtra_String
import javax.inject.Inject

interface GlobalSourceSearchStateBundle
{
    val input: String
}

@HiltViewModel
class GlobalSourceSearchViewModel @Inject constructor(
    state: SavedStateHandle,
    val appPreferences: AppPreferences
) : BaseViewModel(), GlobalSourceSearchStateBundle
{
    override val input by StateExtra_String(state)

    val list = appPreferences.SOURCES_LANGUAGES.value.let { activeLangs ->
        scraper.sourcesListCatalog
            .filter { it.language in activeLangs }
            .map { SourceResults(it, input, viewModelScope) }
    }
}

data class SourceResults(val source: SourceInterface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
    val fetchIterator = FetchIteratorState(coroutineScope) { source.getCatalogSearch(it, searchInput) }

    init
    {
        fetchIterator.fetchNext()
    }
}
