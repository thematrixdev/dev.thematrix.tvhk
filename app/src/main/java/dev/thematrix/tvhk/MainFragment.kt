package dev.thematrix.tvhk

import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.BrowseFragment
import androidx.leanback.widget.*

class MainFragment : BrowseFragment() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupUIElements()
        loadRows()
        setupEventListeners()
    }

    private fun setupUIElements() {
        title = getString(R.string.app_name)
        badgeDrawable = activity.resources.getDrawable(R.drawable.transparentbanner)
    }

    private fun loadRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()

        lateinit var header: HeaderItem
        var listRowAdapter = ArrayObjectAdapter(cardPresenter)
        var lastCategoryId: Int = -1

        for (i in 0 until MovieList.list.count()) {
                if (MovieList.list[i].categoryId > lastCategoryId){
                    if(listRowAdapter.size() > 0){
                        rowsAdapter.add(ListRow(header, listRowAdapter))
                    }

                    header = HeaderItem(i.toLong(), MovieList.CATEGORY[ MovieList.list[i].categoryId ])
                    listRowAdapter = ArrayObjectAdapter(cardPresenter)
                }

                listRowAdapter.add(MovieList.list[i])

                lastCategoryId = MovieList.list[i].categoryId
        }

        if(listRowAdapter.size() > 0){
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }

        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = ItemViewClickedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is Movie) {
                TVHandler().prepareVideo(item)
            } else if (item is String) {
                Toast.makeText(activity, item, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
