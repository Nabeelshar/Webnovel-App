package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.runBlocking
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityChaptersBinding
import my.noveldokusha.databinding.ActivityChaptersListHeaderBinding
import my.noveldokusha.databinding.ActivityChaptersListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsModel
import my.noveldokusha.ui.reader.ReaderActivity
import java.util.*

class ChaptersActivity : AppCompatActivity()
{
	class Extras(val bookUrl: String, val bookTitle: String)
	{
		fun intent(ctx: Context) = Intent(ctx, ChaptersActivity::class.java).also {
			it.putExtra(::bookUrl.name, bookUrl)
			it.putExtra(::bookTitle.name, bookTitle)
		}
	}
	
	private val extras = object
	{
		fun bookMetadata() = bookstore.BookMetadata(
			title = intent.extras!!.getString(Extras::bookTitle.name)!!,
			url = intent.extras!!.getString(Extras::bookUrl.name)!!
		)
	}
	
	private val viewModel by viewModels<ChaptersModel>()
	private val viewHolder by lazy { ActivityChaptersBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val chapters by lazy { ChaptersArrayAdapter(viewModel.chapters) }
		val header by lazy { ChaptersHeaderAdapter() }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(extras.bookMetadata())
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.header, viewAdapter.chapters)
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		viewHolder.swipeRefreshLayout.setOnRefreshListener { viewModel.loadChapters(false) }
		
		viewModel.refresh.observe(this) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		
		viewModel.chaptersItemsLiveData.observe(this) {
			viewAdapter.chapters.setList(it)
			viewModel.numberOfChapters.value = it.size
		}
		
		supportActionBar!!.let {
			it.title = "Chapters"
			it.setDisplayHomeAsUpEnabled(true)
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean
	{
		menuInflater.inflate(R.menu.chapters_list_menu__appbar, menu)
		
		val itemBookmark = menu!!.findItem(R.id.action_bookmarked)!!
		
		setBookmarkIconActive(runBlocking { bookstore.bookLibrary.exist(viewModel.bookMetadata.url) }, itemBookmark)
		bookstore.bookLibrary.existFlow(viewModel.bookMetadata.url).asLiveData().observe(this) { bookmarked ->
			setBookmarkIconActive(bookmarked, itemBookmark)
		}
		return true
	}
	
	private fun setBookmarkIconActive(active: Boolean, item: MenuItem)
	{
		val color = if (active) ContextCompat.getColor(this, R.color.dark_orange_red) else Color.DKGRAY
		item.icon.setTint(color)
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		R.id.action_set_all_chapters_read -> viewModel.setAsRead(viewAdapter.chapters.selected).let { true }
		R.id.action_download_all_chapters -> viewModel.downloadAllChapters().let { true }
		R.id.action_bookmarked -> viewModel.toggleBookmark().let { true }
		android.R.id.home -> this.onBackPressed().let { true }
		else -> super.onOptionsItemSelected(item)
	}
	
	private inner class ChaptersArrayAdapter(private val list: ArrayList<ChaptersModel.ChapterItem>) :
		RecyclerView.Adapter<ChaptersArrayAdapter.ViewBinder>()
	{
		private inner class Diff(private val new: List<ChaptersModel.ChapterItem>) : DiffUtil.Callback()
		{
			override fun getOldListSize(): Int = list.size
			override fun getNewListSize(): Int = new.size
			override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].chapter.url == new[newPos].chapter.url
			override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].chapter == new[newPos].chapter
		}
		
		fun setList(newList: List<ChaptersModel.ChapterItem>) = DiffUtil.calculateDiff(Diff(newList)).let {
			val isEmpty = list.isEmpty()
			list.clear()
			list.addAll(newList)
			if (isEmpty) notifyDataSetChanged() else it.dispatchUpdatesTo(this)
		}
		
		private var default_textColor = 0
		
		var selected: ChaptersModel.ChapterItem? = null
			private set
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
		{
			val holder = ViewBinder(ActivityChaptersListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
			default_textColor = holder.viewHolder.title.currentTextColor
			return holder
		}
		
		override fun getItemCount() = this@ChaptersArrayAdapter.list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			
			itemData.currentlyLastReadChapterLiveData.removeObservers(this@ChaptersActivity)
			itemData.currentlyLastReadChapterLiveData.observe(this@ChaptersActivity) { currentlyReading ->
				itemView.currentlyReading.visibility = if (currentlyReading) View.VISIBLE else View.INVISIBLE
			}
			
			itemData.downloadedLiveData.removeObservers(this@ChaptersActivity)
			itemData.downloadedLiveData.observe(this@ChaptersActivity) { downloaded ->
				itemView.downloaded.visibility = if (downloaded) View.VISIBLE else View.INVISIBLE
			}
			
			itemView.title.text = itemData.chapter.title
			if (itemData.chapter.read) itemView.title.setTextColor(Color.GRAY)
			else itemView.title.setTextColor(default_textColor)
			
			itemView.selected.visibility = if (selected == itemData) View.VISIBLE else View.INVISIBLE
			itemView.position.visibility = if (itemData.chapter.run { lastReadOffset == 0 || read }) View.INVISIBLE else View.VISIBLE
			itemView.position.text = itemData.chapter.lastReadPosition.toString()
			
			itemView.root.setOnClickListener {
				
				selected?.let {
					val lastIndex = this.list.indexOf(it)
					selected = null
					this.notifyItemChanged(lastIndex)
					this.notifyItemChanged(this.list.indexOf(itemData))
					return@setOnClickListener
				}
				
				val intent = ReaderActivity
					.Extras(bookUrl = viewModel.bookMetadata.url, bookSelectedChapterUrl = itemData.chapter.url)
					.intent(this@ChaptersActivity)
				this@ChaptersActivity.startActivity(intent)
			}
			
			itemView.root.setOnLongClickListener {
				val lastIndex = this.list.indexOf(selected)
				selected = if (selected == itemData) null else itemData
				this.notifyItemChanged(lastIndex)
				this.notifyItemChanged(this.list.indexOf(itemData))
				true
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityChaptersListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
	
	inner class ChaptersHeaderAdapter : RecyclerView.Adapter<ChaptersHeaderAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
		{
			return ViewBinder(ActivityChaptersListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		}
		
		override fun getItemCount() = 1
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			binder.viewHolder.bookTitle.text = viewModel.bookTitle
			binder.viewHolder.sourceName.text = viewModel.sourceName
			
			viewModel.numberOfChapters.observe(this@ChaptersActivity) {
				binder.viewHolder.numberOfChapters.text = it.toString()
				binder.viewHolder.numberOfChapters.visibility = if (it.toString().isEmpty()) View.INVISIBLE else View.VISIBLE
			}
			viewModel.errorMessage.observe(this@ChaptersActivity) { binder.viewHolder.errorMessage.text = it }
			viewModel.errorMessageVisibility.observe(this@ChaptersActivity) { binder.viewHolder.errorMessage.visibility = it }
			viewModel.errorMessageMaxLines.observe(this@ChaptersActivity) { binder.viewHolder.errorMessage.maxLines = it }
			binder.viewHolder.errorMessage.setOnLongClickListener(object : View.OnLongClickListener
			{
				private var expand: Boolean = false
				override fun onLongClick(v: View?): Boolean
				{
					expand = !expand
					when (expand)
					{
						true -> viewModel.errorMessageMaxLines.postValue(100)
						false -> viewModel.errorMessageMaxLines.postValue(10)
					}
					return true
				}
			})
			binder.viewHolder.databaseSearchButton.setOnClickListener {
				DatabaseSearchResultsActivity
					.Extras(scrubber.database.NovelUpdates.baseUrl, DatabaseSearchResultsModel.SearchMode.Text(viewModel.bookMetadata.title))
					.intent(this@ChaptersActivity).let { intent -> startActivity(intent) }
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityChaptersListHeaderBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
}