package com.embeddedproject.calciofemminileitaliano.helpers

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class SlowRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val slowVelocityX = (velocityX * 0.4).toInt()
        val slowVelocityY = (velocityY * 0.4).toInt()
        return super.fling(slowVelocityX, slowVelocityY)
    }
}