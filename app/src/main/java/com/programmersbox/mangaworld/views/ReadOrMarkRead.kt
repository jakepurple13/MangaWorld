package com.programmersbox.mangaworld.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeManageAdapter
import com.programmersbox.manga_sources.mangasources.ChapterModel
import com.programmersbox.mangaworld.adapters.ChapterHolder

class ReadOrMarkRead(
    dragSwipeAdapter: DragSwipeAdapter<ChapterModel, ChapterHolder>,
    dragDirs: Int,
    swipeDirs: Int,
    context: Context,
    private val colorSwatch: Palette.Swatch?
) : DragSwipeManageAdapter<ChapterModel, ChapterHolder>(dragSwipeAdapter, dragDirs, swipeDirs) {

    private class ReadInfo(context: Context, iconId: Int, backgroundColor: Int?) {
        val icon = context.getDrawable(iconId)
        val intrinsicWidth = icon!!.intrinsicWidth
        val intrinsicHeight = icon!!.intrinsicHeight
        val background = ColorDrawable()

        init {
            background.color = backgroundColor ?: Color.BLACK
        }
    }

    private val readChapterInfo = ReadInfo(context, android.R.drawable.ic_media_play, colorSwatch?.titleTextColor)
    private val addChapterInfo = ReadInfo(context, android.R.drawable.ic_input_add, colorSwatch?.titleTextColor)
    private val removeChapterInfo = ReadInfo(context, android.R.drawable.ic_delete, colorSwatch?.titleTextColor)

    private val clearPaint =
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top
            val isCanceled = dX == 0f && !isCurrentlyActive

            if (isCanceled) {
                clearCanvas(
                    c,
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                return
            }

            if (dX < 0) {
                // Draw the red delete background
                readChapterInfo.background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                readChapterInfo.background.draw(c)

                // Calculate position of delete icon
                val deleteIconTop = itemView.top + (itemHeight - readChapterInfo.intrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - readChapterInfo.intrinsicHeight) / 2
                val deleteIconLeft = itemView.right - deleteIconMargin - readChapterInfo.intrinsicWidth
                val deleteIconRight = itemView.right - deleteIconMargin
                val deleteIconBottom = deleteIconTop + readChapterInfo.intrinsicHeight

                colorSwatch?.rgb?.let { readChapterInfo.icon?.setTint(it) }

                // Draw the delete icon
                readChapterInfo.icon!!.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                readChapterInfo.icon.draw(c)
            } else {

                val info = if ((viewHolder as? ChapterHolder)?.readChapter?.isChecked == true) removeChapterInfo else addChapterInfo

                info.background.setBounds(
                    itemView.left - dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                info.background.draw(c)

                // Calculate position of delete icon
                val deleteIconTop2 = itemView.top + (itemHeight - info.intrinsicHeight) / 2
                val deleteIconMargin2 = (itemHeight - info.intrinsicHeight) / 2
                val deleteIconLeft2 = itemView.left + deleteIconMargin2
                val deleteIconRight2 = itemView.left + deleteIconMargin2 + info.intrinsicWidth
                val deleteIconBottom2 = deleteIconTop2 + info.intrinsicHeight

                colorSwatch?.rgb?.let { info.icon?.setTint(it) }

                // Draw the delete icon
                info.icon!!.setBounds(deleteIconLeft2, deleteIconTop2, deleteIconRight2, deleteIconBottom2)
                info.icon.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

}