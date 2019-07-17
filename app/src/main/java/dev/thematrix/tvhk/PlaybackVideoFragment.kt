package dev.thematrix.tvhk

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class PlaybackVideoFragment : VideoSupportFragment() {
    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (_, title, _, videoUrl, func) = activity?.intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie

        if(videoUrl.equals("")){
            getVideoUrl(title, func)
        }else{
            playVideo(title, videoUrl)
        }
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }

    fun playVideo(title: String, videoUrl: String) {
        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        val playerAdapter = MediaPlayerAdapter(activity)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = title
        mTransportControlGlue.playWhenPrepared()

        playerAdapter.setDataSource(Uri.parse(videoUrl))
    }

    fun getVideoUrl(title: String, ch: String) {
        if(ch.equals("viutv99") or ch.equals("nowtv332") or ch.equals("nowtv331")){
            var url = ""
            val params = JSONObject()

            if(ch.equals("viutv99")){
                url = "https://api.viu.now.com/p8/2/getLiveURL"

                params.put("channelno", "099")

                params.put("deviceId", "7849989bff631f5888")
                params.put("deviceType", "5")
            }else{
                url = "https://hkt-mobile-api.nowtv.now.com/09/1/getLiveURL"

                if(ch.equals("nowtv332")){
                    params.put("channelno", "332")
                }else if(ch.equals("nowtv331")){
                    params.put("channelno", "331")
                }

                params.put("audioCode", "")
            }

            params.put("callerReferenceNo", "20190625160500")
            params.put("format", "HLS")
            params.put("mode", "prod")

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                Response.Listener { response ->
                    val video_url = JSONArray(JSONObject(JSONObject(response.get("asset").toString()).get("hls").toString()).get("adaptive").toString()).get(0).toString()
                    Log.d("adaptive", video_url)
                    playVideo(title, video_url)
                },
                Response.ErrorListener{
                }
            )

            jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f
            )

            val requestQueue = Volley.newRequestQueue(activity)
            requestQueue.add(jsonObjectRequest)
        }
    }
}