package dev.thematrix.tvhk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_phone_grid.view.*

class MovieAdapter : BaseAdapter {
    var context: Context? = null

    constructor(context: Context) : super() {
        this.context = context
    }

    override fun getCount(): Int {
        return MovieList.list.count()
    }

    override fun getItem(position: Int): Any {
        return MovieList.list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val movie = MovieList.list[position]

        var inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var movieView = inflator.inflate(R.layout.layout_phone_grid, null)
        Picasso.get().load(movie.cardImageUrl).into(movieView.logo)
        movieView.title.text = movie.title

        return movieView
    }
}