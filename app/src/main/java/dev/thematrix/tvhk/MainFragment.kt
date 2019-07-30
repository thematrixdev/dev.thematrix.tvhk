package dev.thematrix.tvhk

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.BrowseFragment
import androidx.leanback.widget.*
import com.android.volley.*
import com.android.volley.toolbox.*
import org.json.JSONArray
import org.json.JSONObject

class MainFragment : BrowseFragment() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupUIElements()
        setupNetwork()
        detectLocation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 0 && resultCode == Activity.RESULT_OK){
            val action = data!!.getStringExtra("action")

            if (action == "switch") {
                val direction = data.getStringExtra("direction")
                val showMessage = data.getBooleanExtra("showMessage", false)
                channelSwitch(direction, showMessage)
            } else if (action == "restore") {
                prepareVideo(MovieList.list[currentVideoID])
            }
        }
    }

    private fun continueInitialization(){
        loadRows()
        setupEventListeners()

        restoreState()

        clipboardManager = activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
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
            if (
                !MovieList.list[i].hongkongonly ||
                (MovieList.list[i].hongkongonly && (location == "HK" || location == ""))
            ) {
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
                prepareVideo(item)
            } else if (item is String) {
                Toast.makeText(activity, item, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNetwork(){
        requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }
    }

    private fun detectLocation(){
        val stringRequest = object: StringRequest(
            Method.GET,
            "https://ifconfig.co/country-iso",
            Response.Listener { response ->
                location = response.trim()

                if (location != "HK") {
                    Toast.makeText(activity, "偵測位置不在香港\n部分頻道因無法播放已被隱藏", Toast.LENGTH_LONG).show()
                }

                continueInitialization()
            },
            Response.ErrorListener{ error ->
                location = ""
                continueInitialization()
            }
        ){}

        requestQueue.add(stringRequest)
    }

    private fun restoreState() {
        if(MainActivity.playerType > -1){
            val currentVideoID = SharedPreference(activity).getInt("currentVideoID")

            if (currentVideoID > -1) {
                prepareVideo(MovieList.list[currentVideoID])
            }
        }
    }

    fun prepareVideo(item: Movie){
        currentVideoID = item.id

        if(MainActivity.playerType != MainActivity.playerUseInternal){
            SharedPreference(activity).saveInt("currentVideoID", currentVideoID)
        }

        if(item.videoUrl == ""){
            getVideoUrl(item)
        }else{
            play(item)
        }
    }

    private fun getVideoUrl(item: Movie) {
        requestQueue.cancelAll(this)

        lateinit var url: String

        if(item.func.equals("viutv99") || item.func.equals("nowtv332") || item.func.equals("nowtv331")){
            val params = JSONObject()

            if(item.func.equals("viutv99")){
                url = "https://api.viu.now.com/p8/2/getLiveURL"

                params.put("channelno", "099")

                params.put("deviceId", "AndroidTV")
                params.put("deviceType", "5")
            }else{
                url = "https://hkt-mobile-api.nowtv.now.com/09/1/getLiveURL"

                if(item.func.equals("nowtv332")){
                    params.put("channelno", "332")
                }else if(item.func.equals("nowtv331")){
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
                        play(item, JSONArray(JSONObject(JSONObject(response.getString("asset")).getString("hls")).getString("adaptive")).getString(0))
                    }catch (exception: Exception){
                        showPlaybackErrorMessage(item.title)
                    }
                },
                Response.ErrorListener{ error ->
                    showPlaybackErrorMessage(item.title)
                }
            )

            jsonObjectRequest.retryPolicy = DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 5, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

            requestQueue.add(jsonObjectRequest)
        }else if(item.func.equals("cabletv109") || item.func.equals("cabletv110")){
            url = "https://mobileapp.i-cable.com/iCableMobile/API/api.php"

            val stringRequest = object: StringRequest(
                Method.POST,
                url,
                Response.Listener { response ->
                    try {
                        play(item, JSONObject(JSONObject(response).getString("result")).getString("stream"))
                    }catch (exception: Exception){
                        showPlaybackErrorMessage(item.title)
                    }
                },
                Response.ErrorListener{ error ->
                    showPlaybackErrorMessage(item.title)
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
                    params.put("deviceModel", "AndroidTV")
                    params.put("type", "live")

                    return params
                }
            }

            requestQueue.add(stringRequest)
        }
    }

    private fun showPlaybackErrorMessage(title: String){
        Toast.makeText(activity, title + " 暫時未能播放，請稍候再試。", Toast.LENGTH_SHORT).show()

        if(MainActivity.playerType != MainActivity.playerUseExternal) {
            channelSwitch(lastDirection, false)
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

            if (MainActivity.copyUrlToClipboard == MainActivity.doCopyUrlToClipboard){
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, url))
                Toast.makeText(activity, "已複製播放網址", Toast.LENGTH_SHORT).show()
            }

            if(MainActivity.playerType == MainActivity.playerUseExternal){
                val intent: Intent = Uri.parse(url).let { uri->
                    Intent(Intent.ACTION_VIEW, uri)
                }

                startActivityForResult(intent, 0)
            }else{
                val intent = Intent(activity, PlaybackActivity::class.java)
                intent.putExtra("item", item)
                intent.putExtra("url", url)
                startActivityForResult(intent, 0)
            }
        }catch (e: Exception){
            if(e.message.toString().indexOf("No Activity found to handle Intent") > -1){
                Toast.makeText(activity, "請先安裝媒體播放器，建議使用 MX Player", Toast.LENGTH_SHORT).show()
            }
        }
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

        Toast.makeText(activity, "正在轉台到 " + MovieList.list[id].title, Toast.LENGTH_SHORT).show()

        prepareVideo(MovieList.list[id])
    }

    companion object {
        private lateinit var requestQueue: RequestQueue
        private lateinit var location: String
        var currentVideoID = -1
        private lateinit var clipboardManager: ClipboardManager
        private var lastDirection = "NEXT"
    }
}
