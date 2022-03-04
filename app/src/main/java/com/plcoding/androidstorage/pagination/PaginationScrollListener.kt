package com.plcoding.androidstorage.pagination

import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Created By Dhruv Limbachiya on 04-03-2022 01:52 PM.
 */
abstract class PaginationScrollListener(private val layoutManager: RecyclerView.LayoutManager) :
    RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition =
            (layoutManager as GridLayoutManager).findFirstVisibleItemPosition()

        if (!isLoading() && !isLastPage()) {
            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0 && totalItemCount >= PAGE_SIZE) {
                loadMoreItems()
            }
        }
    }

    protected abstract fun loadMoreItems()

    abstract fun isLoading(): Boolean

    abstract fun isLastPage(): Boolean

    companion object {
        const val PAGE_START = 1

        /**
         * Set scrolling threshold here (for now i'm assuming 10 item in one page)
         */
        const val PAGE_SIZE = 10
    }
}