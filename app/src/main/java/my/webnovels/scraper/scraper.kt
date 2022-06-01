package my.webnovels.scraper


import my.webnovels.scraper.databases.NovelUpdates
import my.webnovels.scraper.sources.*

object scraper
{
    val databasesList = setOf<DatabaseInterface>(
        NovelUpdates(),
      //  BakaUpdates()
    )

    val sourcesList = setOf<SourceInterface>(
      //  LightNovelsTranslations(),
        MTLNovel(),
        WuxiaWorld(),
        BoxNovel(),
        NovelHall(),
        ReadLightNovel(),
        ReadNovelFull(),
        my.webnovels.scraper.sources.NovelUpdates(),
        Reddit(),
        AT(),

        BestLightNovel(),
        _1stKissNovel(),
//        Sousetsuka(),
//        Saikai(),
        Wuxia(),


//        LightNovelWorld(),


    )

    val sourcesListCatalog = sourcesList.filterIsInstance<SourceInterface.catalog>().toSet()
    val sourcesLanguages = sourcesList.filterIsInstance<SourceInterface.catalog>().map { it.language }.toSortedSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean
    {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? = sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
    fun getCompatibleSourceCatalog(url: String): SourceInterface.catalog? = sourcesListCatalog.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
    fun getCompatibleDatabase(url: String): DatabaseInterface? = databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl)}
}
