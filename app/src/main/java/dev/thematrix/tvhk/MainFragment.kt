package dev.thematrix.tvhk

import android.content.Intent
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
        title = getString(R.string.browse_title)
//        headersState = BrowseFragment.HEADERS_HIDDEN
//        isHeadersTransitionOnBackEnabled = false
    }

      private fun loadRows() {
        val list = MovieList.list

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()
//listRowAdapter mean pccw channels
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)

        listRowAdapter.add(list[0])
        listRowAdapter.add(list[1])
        listRowAdapter.add(list[2])
//listRowAdapter2 mean i-cable channels
        val listRowAdapter2 = ArrayObjectAdapter(cardPresenter)

        listRowAdapter2.add(list[3])
        listRowAdapter2.add(list[4])
        listRowAdapter2.add(list[5])
//listRowAdapter3 mean RTHK channels
        val listRowAdapter3 = ArrayObjectAdapter(cardPresenter)

        listRowAdapter3.add(list[6])
        listRowAdapter3.add(list[7])


        val header = HeaderItem(0, "PCCW")
        val header2 = HeaderItem(1, "i-Cable")
        val header3 = HeaderItem(2, "RTHK")

        rowsAdapter.add(ListRow(header, listRowAdapter))
        rowsAdapter.add(ListRow(header2, listRowAdapter2))
        rowsAdapter.add(ListRow(header3, listRowAdapter3))

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
                val intent = Intent(activity, PlaybackActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)
                startActivity(intent)
            } else if (item is String) {
                Toast.makeText(activity, item, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
