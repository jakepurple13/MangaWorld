package com.programmersbox.mangaworld

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.gsonutils.putExtra
import com.programmersbox.mangaworld.databinding.HistoryListItemBinding
import com.programmersbox.mangaworld.utils.ChapterHistory
import com.programmersbox.mangaworld.utils.chapterHistory
import com.programmersbox.mangaworld.utils.usePalette
import com.programmersbox.thirdpartyutils.into
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.history_list_item.view.*

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyRV.adapter = HistoryAdapter(chapterHistory.toMutableList())

    }

    inner class HistoryAdapter(dataList: MutableList<ChapterHistory>) : DragSwipeAdapter<ChapterHistory, HistoryHolder>(dataList) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder =
            HistoryHolder(HistoryListItemBinding.inflate(layoutInflater, parent, false))

        override fun HistoryHolder.onBind(item: ChapterHistory, position: Int) = bind(item)
    }

    class HistoryHolder(private val binding: HistoryListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val cover = itemView.historyListCover!!
        private val layout = itemView.historyLayout!!
        private val title = itemView.historyListTitle!!
        private val description = itemView.historyListDescription!!

        fun bind(item: ChapterHistory) {
            binding.model = item
            binding.executePendingBindings()
            itemView.setOnClickListener {
                it.context.startActivity(
                    Intent(it.context, ReadActivity::class.java).apply {
                        putExtra("mangaTitle", item.title)
                        putExtra("currentChapter", item.chapterModel)
                        putExtra("mangaUrl", item.chapterModel.url)
                        //putExtra("allChapters", "")
                        putExtra("mangaInfoUrl", item.mangaUrl)
                    }
                )
            }
            Glide.with(cover)
                .asBitmap()
                .load(item.imageUrl)
                .override(360, 480)
                .transform(RoundedCorners(30))
                .fallback(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into<Bitmap> {
                    resourceReady { image, _ ->
                        cover.setImageBitmap(image)
                        if (itemView.context.usePalette) {
                            val p = Palette.from(image).generate()

                            val dom = p.vibrantSwatch
                            dom?.rgb?.let { layout.setCardBackgroundColor(it) }
                            dom?.titleTextColor?.let { title.setTextColor(it) }
                            dom?.bodyTextColor?.let { description.setTextColor(it) }
                        }
                    }
                }
        }
    }
}