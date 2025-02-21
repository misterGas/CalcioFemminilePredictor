package com.embeddedproject.calciofemminileitaliano.helpers

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView

class SlowListView(context: Context, attrs: AttributeSet?) : ListView(context, attrs) {

    private var customScroller: Scroller? = null

    init {
        customScroller = SlowScroller(context)
        setCustomScroller()
    }

    private fun setCustomScroller() {
        try {
            val scrollerField = ListView::class.java.getDeclaredField("mScroller")
            scrollerField.isAccessible = true
            scrollerField.set(this, customScroller)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class SlowScroller(context: Context) : Scroller(context) {

        override fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) {
            super.fling(startX, startY, (velocityX * 0.1).toInt(), (velocityY * 0.1).toInt(), minX, maxX, minY, maxY)
        }
    }
}