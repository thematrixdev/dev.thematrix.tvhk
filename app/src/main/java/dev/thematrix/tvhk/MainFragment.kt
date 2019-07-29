package dev.thematrix.tvhk

import android.content.Intent
import android.net.Uri
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
//        headersState = BrowseFragment.HEADERS_HIDDEN
//        isHeadersTransitionOnBackEnabled = false
    }

    private fun loadRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()

        lateinit var header: HeaderItem
        var listRowAdapter = ArrayObjectAdapter(cardPresenter)
        for (i in 0 until MovieList.list.count()) {
            if (i % 3 == 0) {
                if(listRowAdapter.size() > 0){
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }

                header = HeaderItem(i.toLong(), MovieList.CATEGORY[i / 3])
                listRowAdapter = ArrayObjectAdapter(cardPresenter)
            }

            listRowAdapter.add(MovieList.list[i])
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
                play(item)
            } else if (item is String) {
                Toast.makeText(activity, item, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun play(item: Movie){
        try {
            var currentVideoId: Int

            if(MainActivity.playerType == MainActivity.playerUseExternal){
                currentVideoId = item.id

                val playIntent: Intent = Uri.parse(item.videoUrl).let { uri->
                    Intent(Intent.ACTION_VIEW, uri)
                }

                startActivity(playIntent)
            }else{
                currentVideoId = -1

                val intent = Intent(activity, PlaybackActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)
                startActivity(intent)
            }

            SharedPreference(activity).saveInt("currentVideoID", currentVideoId)
        }catch (e: Exception){
            if(e.message.toString().indexOf("No Activity found to handle Intent") > -1){
                Toast.makeText(activity, "請先安裝媒體播放器，建議使用 MX Player", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
