package my.webnovels.scraper.sources

import my.webnovels.data.BookMetadata
import my.webnovels.data.ChapterMetadata
import my.webnovels.scraper.*
import org.jsoup.nodes.Document

class _1stKissNovel : SourceInterface.catalog
{
    override val catalogUrl = "https://1stkissnovel.love/novel/?m_orderby=alphabet"
    override val name = "1stKissNovel"
    override val baseUrl = "https://1stkissnovel.love/"
    override val language = "English"


    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst("div.summary_image")
            ?.selectFirst("img[data-src]")
            ?.attr("data-src")
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        val url = doc
            .location()
            .toUrlBuilder()
            ?.addPath("ajax")
            ?.addPath("chapters")

        return connect(url.toString())
            .addHeaderRequest()
            .postIO()
            .select(".wp-manga-chapter > a[href]")
            .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        return tryConnect {

            val url = baseUrl
                .toUrlBuilderSafe()
                .ifCase(page == 1){ addPath("page", page.toString())}

            fetchDoc(url, 20000)
                .select(".page-item-detail")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[data-src]")?.attr("data-src")
                    BookMetadata(
                        title = it.attr("title"),
                        url = it.attr("href"),
                        coverImageUrl = coverImageUrl ?: "",
                    )
                }
                .let { Response.Success(it) }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        val page = index + 1
        return tryConnect {

            val url = baseUrl
                .toUrlBuilderSafe()
                .ifCase(page == 1){ addPath("page", page.toString())}
                .add("s",input)
                .add("post_type","wp-manga")
                .toString()
                .plus("&op&author&artist&release&adult&m_orderby")

            fetchDoc(url, 20000)
                .select(".row.c-tabs-item__content")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[data-src]")?.attr("data-src")
                    BookMetadata(
                        title = it.attr("title"),
                        url = it.attr("href"),
                        coverImageUrl = coverImageUrl ?: "",
                    )
                }
                .let { Response.Success(it) }
        }
    }
}