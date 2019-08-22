package dev.thematrix.tvhk

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.layout_exoplayer.view.*


class PlaybackExoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(
            R.layout.layout_exoplayer,
            container,
            false
        )

        val item = activity?.intent?.getSerializableExtra("item") as Movie

        var url: String
        if(item.videoUrl != ""){
            url = item.videoUrl
        }else{
            url = activity?.intent?.getStringExtra("url").toString()
        }

        setUpPlayer(view)

        play(item.title, url)

        return view
    }

    override fun onPause() {
        super.onPause()

        player.release()
    }

    private fun setUpPlayer(view: View) {
        player = ExoPlayerFactory.newSimpleInstance(activity)
        player.playWhenReady = true

        playerView = view.simpleExoPlayerView
        playerView.player = player
        playerView.useController = false
    }

    fun play(title: String, url: String) {
        dataSourceFactory = DefaultHttpDataSourceFactory(Util.getUserAgent(context, "TVHK"))

        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))

        player.prepare(hlsMediaSource)
    }

    companion object {
        private lateinit var player: SimpleExoPlayer
        private lateinit var playerView: PlayerView
        private lateinit var dataSourceFactory: DefaultHttpDataSourceFactory
    }
}