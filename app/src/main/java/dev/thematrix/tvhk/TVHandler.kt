package dev.thematrix.tvhk

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.timerTask

class TVHandler{
    fun prepareVideo(item: Movie){
        currentVideoID = item.id

        if(item.videoUrl == ""){
            getVideoUrl(item, ::play, ::showPlaybackErrorMessage)
        }else{
            play(item)
        }
    }

    fun play(item: Movie, extra_url: String = ""){
        try {
            var url: String
            if(extra_url != ""){
                url = extra_url
            }else{
                url = item.videoUrl
            }

            lastItem = item
            lastUrl = url

            if (MainActivity.copyUrlToClipboard == MainActivity.doCopyUrlToClipboard){
                MainActivity.clipboardManager.setPrimaryClip(ClipData.newPlainText(null, url))

//                toast.setText("已複製播放網址")
//                toast.show()
            }

            if(MainActivity.playerType == MainActivity.playerUseExternal){
                val intent: Intent = Uri.parse(url).let { uri->
                    Intent(Intent.ACTION_VIEW, uri)
                }

                startActivityForResult(activity, intent, 0, null)
            }else{
                val intent = Intent(activity, PlaybackActivity::class.java)
                intent.putExtra("item", item)
                intent.putExtra("url", url)
                startActivityForResult(activity, intent, 0, null)
            }
        }catch (e: Exception){
            if(e.message.toString().indexOf("No Activity found to handle Intent") > -1){
                toast.setText("請先安裝媒體播放器，建議使用 MX Player")
                toast.show()
            }
        }
    }

    fun getVideoUrl(item: Movie, successfulCallback: (item: Movie, extra_url: String) -> Unit, errorCallback: (title: String) -> Unit) {
        MainActivity.requestQueue.cancelAll(this)

        lateinit var url: String

        if(item.func.equals("viutv99") || item.func.equals("nowtv332") || item.func.equals("nowtv331")) {
            val params = JSONObject()

            if (item.func.equals("viutv99")) {
                url = "https://api.viu.now.com/p8/2/getLiveURL"

                params.put("channelno", "099")

                params.put("deviceId", getRandomString(18))
                params.put("deviceType", "5")
            } else {
                url = "https://hkt-mobile-api.nowtv.now.com/09/1/getLiveURL"

                if (item.func.equals("nowtv332")) {
                    params.put("channelno", "332")
                } else if (item.func.equals("nowtv331")) {
                    params.put("channelno", "331")
                }

                params.put("audioCode", "")
            }

            params.put("callerReferenceNo", "")
            params.put("format", "HLS")
            params.put("mode", "prod")

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST,
                handleUrl(url),
                params,
                Response.Listener { response ->
                    try {
                        var u = JSONArray(JSONObject(JSONObject(response.getString("asset")).getString("hls")).getString("adaptive")).getString(0)

                        if (item.func.equals("viutv99")) {
                            u = handleUrl(u, true)
                            Thread.sleep(3000)
                        }

                        successfulCallback(
                            item,
                            u
                        )

                        scheduleUrlUpdate(item)
                    } catch (exception: Exception) {
                        errorCallback(item.title)
                    }
                },
                Response.ErrorListener { error ->
                    errorCallback(item.title)
                }
            )

            jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            MainActivity.requestQueue.add(jsonObjectRequest)
        }else if(item.func.equals("nowtv630")){
            url = handleUrl("https://sports.now.com/VideoCheckOut?pid=webch630_4&service=NOW360&type=channel")

            val stringRequest = object: StringRequest(
                Method.GET,
                url,
                Response.Listener { response ->
                    try {
                        var url: String = ""

                        val factory = DocumentBuilderFactory.newInstance()
                        val builder = factory.newDocumentBuilder()
                        val source = InputSource(StringReader(response))
                        val document = builder.parse(source)
                        val nodes = document.getElementsByTagName("RESULT").item(0).childNodes

                        for (i in 0 until nodes.length) {
                            if (nodes.item(i).nodeName == "html5streamurlhq") {
                                url = nodes.item(i).textContent
                                break
                            }
                        }

                        if (url != "") {
                            successfulCallback(item, url)
                            scheduleUrlUpdate(item)
                        } else {
                            errorCallback(item.title)
                        }
                    }catch (exception: Exception){
                        errorCallback(item.title)
                    }
                },
                Response.ErrorListener{ error ->
                    errorCallback(item.title)
                }
            ){
                override fun getRetryPolicy(): RetryPolicy {
                    return DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params =  mutableMapOf<String, String>()

//                    params.put("Referer", "https://sports.now.com/home/630")
                    params.put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; TVHK Build/35.0.A.1.282)")

                    return params
                }
            }

            MainActivity.requestQueue.add(stringRequest)
        }else if(item.func.equals("cabletv109") || item.func.equals("cabletv110")){
            url = handleUrl("https://mobileapp.i-cable.com/iCableMobile/API/api.php")

            val stringRequest = object: StringRequest(
                Method.POST,
                url,
                Response.Listener { response ->
                    try {
                        successfulCallback(item, JSONObject(JSONObject(response).getString("result")).getString("stream"))
                        scheduleUrlUpdate(item)
                    }catch (exception: Exception){
                        errorCallback(item.title)
                    }
                },
                Response.ErrorListener{ error ->
                    errorCallback(item.title)
                }
            ){
                override fun getRetryPolicy(): RetryPolicy {
                    return DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params =  mutableMapOf<String, String>()

                    params.put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; TVHK Build/35.0.A.1.282)")

                    return params
                }

                override fun getParams(): MutableMap<String, String> {
                    val params =  mutableMapOf<String, String>()

                    if(item.func.equals("cabletv109")){
                        params.put("channel_no", "_9")
                        params.put("vlink", "_9")
                    }else if(item.func.equals("cabletv110")){
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
                    params.put("deviceModel", "TVHK")
                    params.put("type", "live")

                    return params
                }
            }

            MainActivity.requestQueue.add(stringRequest)
        }
    }

    private fun handleUrl(url: String, force: Boolean = false): String{
        if (force || !MainActivity.tlsVersionSet && SDK_VER < 21) {
            return url.replace("https://", "http://")
        } else {
            return url
        }
    }

    private fun showPlaybackErrorMessage(title: String){
        toast.setText(title + " 暫時未能播放，請稍候再試。")
        toast.show()

//        if(MainActivity.isTV && MainActivity.playerType != MainActivity.playerUseExternal) {
//            channelSwitch(lastDirection, false)
//        }
    }

    private fun scheduleUrlUpdate(item: Movie){
        unScheduleUrlUpdate()

        urlUpdateScheduler = Timer()
        urlUpdateScheduler.schedule(timerTask {
            toast.setText("UPDATING " + item.title)
            toast.show()

            getVideoUrl(item, ::play, ::showPlaybackErrorMessage)
        }, 14400000)
    }

    fun unScheduleUrlUpdate(){
        urlUpdateScheduler.cancel()
        urlUpdateScheduler.purge()
    }

    fun channelSwitch(direction: String, showMessage: Boolean){
        lastDirection = direction

        var id = currentVideoID

        if (direction == "PREVIOUS") {
            id--
        } else if (direction == "NEXT") {
            id++
        }

        val channelCount = MovieList.list.count()
        if (id < 0) {
            id = channelCount - 1
        } else if (id >= channelCount) {
            id = 0
        }

        toast.setText("正在轉台到 " + MovieList.list[id].title)
        toast.show()

        prepareVideo(MovieList.list[id])
    }

    fun restart(){
        play(lastItem, lastUrl)
    }

    fun getRandomString(len: Int): String{
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val randomString = (1..len)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

        return randomString
    }

    companion object {
        val SDK_VER = android.os.Build.VERSION.SDK_INT

        lateinit var activity: Activity
        lateinit var toast: Toast

        var urlUpdateScheduler: Timer = Timer()

        var currentVideoID = -1
        lateinit var lastItem: Movie
        lateinit var lastUrl: String
        var lastDirection = "NEXT"
    }
}