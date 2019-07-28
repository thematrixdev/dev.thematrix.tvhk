package dev.thematrix.tvhk

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow
import com.android.volley.*
import com.android.volley.toolbox.*
import org.json.JSONArray
import org.json.JSONObject

class PlaybackVideoFragment : VideoSupportFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (id, title, _, _, videoUrl, func) = activity?.intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie

        setUpPlayer()
        setUpNetwork()

        prepareVideo(id, title, videoUrl, func)
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }

    private fun setUpPlayer(){
        playerAdapter = MediaPlayerAdapter(activity)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
        mTransportControlGlue = PlaybackTransportControlGlue(activity, playerAdapter)

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        mTransportControlGlue.host = glueHost

        mTransportControlGlue.isControlsOverlayAutoHideEnabled = true
        hideControlsOverlay(false)
        mTransportControlGlue.isSeekEnabled = false

        toast = Toast.makeText(context, "", Toast.LENGTH_LONG)
    }

    private fun setUpNetwork(){
        requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }
    }

    fun channelSwitch(direction: String, showMessage: Boolean){
        lastDirection = direction

        val list = MovieList.list

        var videoId = currentVideoID

        if(direction.equals("PREVIOUS")){
            videoId--
        }else if(direction.equals("NEXT")) {
            videoId++
        }

        val channelCount = list.count()
        if(videoId < 0){
            videoId = channelCount - 1
        }else if(videoId >= channelCount){
            videoId = 0
        }

        val item = list[videoId]

        if(showMessage){
            toast.setText("正在轉台到 " + item.title)
            toast.show()
        }

        prepareVideo(item.id, item.title, item.videoUrl, item.func)
    }

    fun prepareVideo(id: Int, title: String, videoUrl: String, func: String){
        if(id != currentVideoID){
            currentVideoID = id

            if(videoUrl.equals("")){
                getVideoUrl(title, func)
            }else{
                playVideo(title, videoUrl)
            }
        }
    }

    fun playVideo(title: String, videoUrl: String) {
        mTransportControlGlue.title = title
        playerAdapter.setDataSource(Uri.parse(handleUrl(videoUrl)))
        mTransportControlGlue.playWhenPrepared()
    }

    private fun getVideoUrl(title: String, ch: String) {
        requestQueue.cancelAll(this)

        lateinit var url: String

        if(ch.equals("viutv99") || ch.equals("nowtv332") || ch.equals("nowtv331")){
            val params = JSONObject()

            if(ch.equals("viutv99")){
                url = handleUrl("https://api.viu.now.com/p8/2/getLiveURL")

                params.put("channelno", "099")

                params.put("deviceId", "AndroidTV")
                params.put("deviceType", "5")
            }else{
                url = handleUrl("https://hkt-mobile-api.nowtv.now.com/09/1/getLiveURL")

                if(ch.equals("nowtv332")){
                    params.put("channelno", "332")
                }else if(ch.equals("nowtv331")){
                    params.put("channelno", "331")
                }

                params.put("audioCode", "")
            }

            params.put("callerReferenceNo", "")
            params.put("format", "HLS")
            params.put("mode", "prod")

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                Response.Listener { response ->
                    try {
                        url = JSONArray(JSONObject(JSONObject(response.get("asset").toString()).get("hls").toString()).get("adaptive").toString()).get(0).toString()

//                        val stringRequest = object: StringRequest(
//                            Method.GET,
//                            url,
//                            Response.Listener { response ->
//                                var urlParts: MutableList<String> = mutableListOf()
//                                var s = url.split("/")
//                                for (i in 0 until s.count() - 1) {
//                                    urlParts.add(s[i])
//                                }
//                                url = urlParts.joinToString("/")
//
//                                val res = response.toString().split("\n")
//
//                                var u: MutableList<String> = mutableListOf()
//                                var highestDefinitionBandwidth: Int = 0
//                                var highestDefinitionStreamMeta: String = ""
//                                var highestDefinitionStreamUrl: String = ""
//
//                                for (i in 0 until res.count()) {
//                                    var strlen = res[i].length
//                                    if (strlen > 13 && res[i].substring(0, 13) == "#EXT-X-MEDIA:") {
//                                        if (res[i].indexOf("DEFAULT=YES", ignoreCase = true) > -1) {
//                                            u.add(res[i].replace("URI=\"", "URI=\"" + url + "/"))
//                                        }
//                                    } else if (strlen > 18 && res[i].substring(0, 18) == "#EXT-X-STREAM-INF:") {
//                                        var params = res[i].substring(18).split(",")
//
//                                        params.forEach{
//                                            var param = it.split("=")
//                                            if (param[0] == "BANDWIDTH") {
//                                                val bandwidth = param[1].toInt()
//                                                if (bandwidth > highestDefinitionBandwidth) {
//                                                    highestDefinitionBandwidth = bandwidth
//                                                    highestDefinitionStreamMeta = res[i]
//                                                    highestDefinitionStreamUrl = url + "/" + res[i + 1]
//                                                }
//
//                                                return@forEach
//                                            }
//                                        }
//                                    }else if (strlen > 1 && res[i].substring(0, 1) == "#") {
//                                        u.add(res[i])
//                                    }
//                                }
//
//                                u.add(highestDefinitionStreamMeta)
//                                u.add(highestDefinitionStreamUrl)
//
//                                val m3u8 = u.joinToString("\n")
//                            },
//                            Response.ErrorListener{ error ->
//                            }
//                        ){}
//
//                        requestQueue.add(stringRequest)

                        playVideo(title, url)
                    }catch (exception: Exception){
                        showPlaybackErrorMessage(title)
                    }
                },
                Response.ErrorListener{ error ->
                    showPlaybackErrorMessage(title)
                }
            )

            jsonObjectRequest.retryPolicy = DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

            requestQueue.add(jsonObjectRequest)
        }else if(ch.equals("cabletv109") || ch.equals("cabletv110")){
            url = handleUrl("https://mobileapp.i-cable.com/iCableMobile/API/api.php")

            val stringRequest = object: StringRequest(
                Method.POST,
                url,
                Response.Listener { response ->
                    try {
                        playVideo(title, JSONObject(JSONObject(response).get("result").toString()).get("stream").toString())
                    }catch (exception: Exception){
                        showPlaybackErrorMessage(title)
                    }
                },
                Response.ErrorListener{ error ->
                    showPlaybackErrorMessage(title)
                }
            ){
                override fun getRetryPolicy(): RetryPolicy {
                    return DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params =  mutableMapOf<String, String>()

                    params.put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; AndroidTV Build/35.0.A.1.282)")

                    return params
                }

                override fun getParams(): MutableMap<String, String> {
                    val params =  mutableMapOf<String, String>()

                    if(ch.equals("cabletv109")){
                        params.put("channel_no", "_9")
                        params.put("vlink", "_9")
                    }else if(ch.equals("cabletv110")){
                        params.put("channel_no", "_10")
                        params.put("vlink", "_10")
                    }

                    params.put("device", "aos_mobile")
                    params.put("method", "streamingGenerator2")
                    params.put("quality", "h")
                    params.put("uuid", "")
                    params.put("is_premium", "0")
                    params.put("network", "wifi")
                    params.put("platform", "1")
                    params.put("deviceToken", "")
                    params.put("appVersion", "6.3.4")
                    params.put("market", "G")
                    params.put("lang", "zh_TW")
                    params.put("version", "6.3.4")
                    params.put("osVersion", "23")
                    params.put("channel_id", "106")
                    params.put("deviceModel", "AndroidTV")
                    params.put("type", "live")

                    return params
                }
            }

            requestQueue.add(stringRequest)
        }
    }

    private fun handleUrl(url: String): String{
        if(SDK_VER < 21){
            return url.replace("https://", "http://")
        }else{
            return url
        }
    }

    private fun showPlaybackErrorMessage(title: String){
        toast.setText(title + " 暫時未能播放，請稍候再試。")
        toast.show()
        channelSwitch(lastDirection, false)
    }

    companion object {
        private val SDK_VER = android.os.Build.VERSION.SDK_INT
        var currentVideoID = -1
        private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>
        private lateinit var playerAdapter: MediaPlayerAdapter
        private lateinit var requestQueue: RequestQueue
        private var lastDirection = "NEXT"
        private lateinit var toast: Toast
    }
}