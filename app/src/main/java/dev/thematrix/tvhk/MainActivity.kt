package dev.thematrix.tvhk

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import org.json.JSONObject
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext
import java.io.File



class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpSSL()
        checkUpdate()
//        downloadUpdate()
//        restoreState()
    }

    private fun setUpSSL() {
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
            var sslContext: SSLContext? = null
            sslContext = SSLContext.getInstance("TLSv1.2")
            try {
                sslContext!!.init(null, null, null)
                val engine = sslContext.createSSLEngine()
                engine.enabledCipherSuites

                Toast.makeText(this, "強制使用 TLSv1.2", Toast.LENGTH_SHORT).show()
            } catch (e: KeyManagementException) {
                Toast.makeText(this, "強制使用 TLSv1.2 失敗", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NoSuchAlgorithmException) {
            Toast.makeText(this, "系統沒有 TLSv1.2", Toast.LENGTH_SHORT).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, "系統沒有 Google Play Service", Toast.LENGTH_SHORT).show()
        } catch (e: GooglePlayServicesRepairableException) {
            Toast.makeText(this, "Google Play Service 錯誤", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUpdate(){
        val stringRequest = object: StringRequest(
            Method.GET,
            "https://thematrix.dev/tvhk/release.htm",
            Response.Listener { response ->
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionCode = packageInfo.versionCode

                if(response.trim().toInt() > versionCode){
                    Log.d("__DEBUG__", "Update is available")
                }
            },
            Response.ErrorListener{ error ->
            }
        ){}

        val requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }

        requestQueue.add(stringRequest)
    }

    private fun downloadUpdate() {
        registerReceiver(onDownloadComplete(), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        val file = File(getExternalFilesDir(null), "app-release.apk")
        val request = DownloadManager.Request(Uri.parse("https://thematrix.dev/tvhk/app-release.apk"))
            .setTitle("Updating TVHK")
            .setDescription("Just a moment...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        Log.d("__DEBUG__", "Download started")
    }

    private class onDownloadComplete : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (downloadId == id) {
                Log.d("__DEBUG__", "Download completed")
            }
        }
    }

    private fun restoreState() {
        val currentVideoID = SharedPreference(this).getInt("currentVideoID")

        if (currentVideoID > -1) {
            val intent = Intent(this, PlaybackActivity::class.java)
            intent.putExtra(DetailsActivity.MOVIE, MovieList.list[currentVideoID])
            startActivity(intent)
        }
    }

    companion object {
        private var downloadId: Long = -1
    }
}
