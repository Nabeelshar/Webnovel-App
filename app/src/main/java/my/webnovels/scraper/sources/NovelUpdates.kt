package my.webnovels.scraper.sources

import android.net.Uri
import my.webnovels.data.BookMetadata
import my.webnovels.data.ChapterMetadata
import my.webnovels.scraper.*
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.novelupdates.com/series/mushoku-tensei-old-dragons-tale/
 * Chapter url example:
 * (redirected url) Doesn't have chapters, assume it redirects to different website
 */
class NovelUpdates : SourceInterface.catalog
{
    override val name = "Novel Updates"
    override val baseUrl = "https://www.novelupdates.com/"
    override val catalogUrl = "https://www.novelupdates.com/novelslisting/?sort=7&order=1&status=1"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String
    {
        // Given novel updates only host the chapters list (they redirect to a diferent webpage) and
        // not the content, it will return nothing.
        return ""
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst("div.seriesimg > img[src]")
            ?.attr("src")
            ?.let { if (it == "https://www.novelupdates.com/img/noimagefound.jpg") null else it }
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst("#editdescription")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        return connect("https://www.novelupdates.com/wp-admin/admin-ajax.php")
            .addHeaderRequest()
            .data("action", "nd_getchapters")
            .data("mygrr", doc.selectFirst("#grr_groups")!!.attr("value"))
            .data("mygroupfilter", "")
            .data("mypostid", doc.selectFirst("#mypostid")!!.attr("value"))
            .postIO()
            .select("a[href]")
            .asSequence()
            .filter { it.hasAttr("data-id") }
            .map {
                val title = it.selectFirst("span")!!.attr("title")
                val url = "https:" + it.attr("href")
                ChapterMetadata(title = title, url = url)
            }.toList().reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = catalogUrl.toUrlBuilderSafe().apply {
            add("st", 1)
            if (page > 1) add("pg", page)
        }
        return getSearchList(page, url)
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = baseUrl.toUrlBuilderSafe().apply {
            if (page > 1) appendPath("page").appendPath(page.toString())
            add("s", input)
            add("post_type", "seriesplans")
        }
        return getSearchList(page, url)
    }

    suspend fun getSearchList(page: Int, url: Uri.Builder) = tryConnect(
        extraErrorInfo = "page: $page\nurl: $url"
    ) {
        fetchDoc(url)
            .select(".search_main_box_nu")
            .mapNotNull {
                val title = it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                BookMetadata(
                    title = title.text(),
                    url = title.attr("href"),
                    coverImageUrl = image
                )
            }
            .let { Response.Success(it) }
    }
}
