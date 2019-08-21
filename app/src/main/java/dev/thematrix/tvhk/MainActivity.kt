package dev.thematrix.tvhk

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.UiModeManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.*
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import kotlinx.android.synthetic.main.layout_phone.*
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        isTV = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        if (isTV) {
            setTheme(R.style.tv)
        } else {
            setTheme(R.style.phone)
        }

        super.onCreate(savedInstanceState)

        this.setTheme(android.R.style.Theme_Black_NoTitleBar)

        ctx = this
        TVHandler.activity = this
        TVHandler.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)

        clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        lastInitializationStep = -1
        lastShownDialog = -1
        initialized = false
        initialize()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        TVHandler().unScheduleUrlUpdate()

        if(requestCode == 0 && resultCode == RESULT_OK){
            val action = data!!.getStringExtra("action")

            if (action == "switch") {
                val direction = data.getStringExtra("direction")
                val showMessage = data.getBooleanExtra("showMessage", false)
                TVHandler().channelSwitch(direction, showMessage)
            } else if (action == "restore") {
                TVHandler().prepareVideo(MovieList.list[TVHandler.currentVideoID])
            } else {
                showLayout()
            }
        }
    }

    private fun initialize(){
        lastInitializationStep++

        if (lastInitializationStep == 0) {
            detectPlayStore()
            initialize()
        } else if (lastInitializationStep == 1) {
            setupNetwork()
            initialize()
        } else if (lastInitializationStep == 2) {
            detectLocation()
        } else if (lastInitializationStep == 3) {
            showLayout()
            initialize()
        } else if (lastInitializationStep == 4) {
            showUserInteraction()
        } else {
            restoreState()
            initialized = true
        }
    }

    private fun detectPlayStore(){
        try {
            packageManager.getPackageInfo("com.android.vending", 0)
        } catch (e: PackageManager.NameNotFoundException) {
            hasPlaystore = false
        }
    }

    private fun setupNetwork(){
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
            var sslContext: SSLContext? = null
            sslContext = SSLContext.getInstance("TLSv1.2")
            try {
                sslContext!!.init(null, null, null)
                val engine = sslContext.createSSLEngine()
                engine.enabledCipherSuites

                tlsVersionSet = true
            } catch (e: KeyManagementException) {
            }
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: GooglePlayServicesNotAvailableException) {
            hasPlaystore = false
        } catch (e: GooglePlayServicesRepairableException) {
            hasPlaystore = false
        }

        requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
            start()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionRequestCode -> {
                if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkUpdate()
                }
            }
        }
    }

    private fun checkUpdate(){
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            "https://thematrix.dev/tvhk/release.json",
            null,
            Response.Listener { response ->
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionCode = packageInfo.versionCode

                if(response.getInt("version") > versionCode){
                    val builder = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                    builder.setTitle("更新到 v" + response.getString("version") + " ?")
                    builder.setMessage(response.getString("note"))

                    builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                        downloadUpdate()
                    }

                    builder.setNegativeButton(android.R.string.no) { dialog, which ->
                    }

                    builder.show()
                }else{
                    showUserInteraction()
                }
            },
            Response.ErrorListener{ error ->
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun downloadUpdate() {
        registerReceiver(onDownloadComplete(), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        val request = DownloadManager
            .Request(Uri.parse("https://thematrix.dev/tvhk/app-release.apk"))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "tvhk.apk")

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
    }

    private class onDownloadComplete: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val c = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            if(c != null){
                c.moveToFirst()
                val fileUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                val mFile = File(Uri.parse(fileUri).path!!)
                val fileName = mFile.absolutePath

                context.unregisterReceiver(this)

                val intent = Intent(Intent.ACTION_VIEW)
                var contentUri: Uri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", File(fileName))

                }else{
                    contentUri = Uri.fromFile(File(fileName))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
                startActivity(ctx, intent, null)
            }
        }
    }

    private fun detectLocation(){
        Toast.makeText(ctx, "偵測IP地址所屬地區中...", Toast.LENGTH_LONG).show()

        val stringRequest = object: StringRequest(
            Method.GET,
            "https://ifconfig.co/country-iso",
            Response.Listener { response ->
                location = response.trim()

                if (location != "HK") {
                    Toast.makeText(ctx, "偵測所屬地區不在香港\n部分頻道因無法播放已被隱藏", Toast.LENGTH_LONG).show()
                }

                initialize()
            },
            Response.ErrorListener{ error ->
                location = ""
                initialize()
            }
        ){}

        requestQueue.add(stringRequest)
    }

    private fun showUserInteraction() {
        lastShownDialog++

        if (lastShownDialog == 0) {
            val readme = SharedPreference(this).getInt("readme")
            if (readme == -1){
                val builder = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                builder.setTitle("請注意")
                builder.setMessage("- 只可用係非謀利用途\n" +
                        "- 香港地區以外可能睇唔到部份台\n" +
                        "- 呢個app係設計俾Android-TV嘅電視或機頂盒使用。Android嘅手機及機頂盒，有可能出現各種問題。\n" +
                        "- 高清廣播，請自行注意網絡用量！\n" +
                        "- 呢個app只提供公眾免費頻道\n" +
                        "- 所有出現嘅商標都屬於各公司所有\n" +
                        "- 我地同各公司無任何關係\n" +
                        "- 如果各公司認為我地有侵權，請搵我剷返走你地個台"
                )

                builder.setPositiveButton("好") { dialog, which ->
                    SharedPreference(this).saveInt("readme", 1)
                    showUserInteraction()
                }

                builder.setNegativeButton("不了") { dialog, which ->
                    bye9bye()
                }

                builder.setCancelable(false)

                builder.show()
            }else{
                showUserInteraction()
            }
        }else if (lastShownDialog == 1) {
            if(hasPlaystore) {
                showUserInteraction()
            }else{
                val PERM_READ_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                val PERM_WRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if (PERM_READ_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED && PERM_WRITE_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED) {
                    hasPermission = true
                    checkUpdate()
                }else{
                    val builder = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                    builder.setTitle("TVHK")
                    builder.setMessage("由於系統沒有Google Play Service，需要額外權限下載更新")

                    builder.setPositiveButton("授權下載更新") { dialog, which ->
                        ActivityCompat.requestPermissions(this, arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ), permissionRequestCode)
                    }

                    builder.setNegativeButton("不了") { dialog, which ->
                        showUserInteraction()
                    }

                    builder.setCancelable(false)

                    builder.show()
                }
            }
        }else if (lastShownDialog == 2) {
            playerType = SharedPreference(this).getInt("playerType")
            if (playerType == -1) {
                val builder = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                builder.setTitle("選擇播放器")
                builder.setMessage("內置播放器：可用方向掣轉台、載入速度較快\n" +
                        "\n" +
                        "外置播放器：可用幾乎任何嘅播放器。建議使用MX Player\n" +
                        "\n" +
                        "注意：Android 4用家如內置播放器唔正常運作，請揀外置播放器\n" +
                        "\n" +
                        "建議：先唔選擇，使用預設內置播放器，用唔到先再揀外置播放器"
                )

                builder.setPositiveButton("內置播放器") { dialog, which ->
                    playerType = playerUseInternal
                    SharedPreference(this).saveInt("playerType", playerType)
                    showUserInteraction()
                }

                builder.setNegativeButton("外置播放器") { dialog, which ->
                    playerType = playerUseExternal
                    SharedPreference(this).saveInt("playerType", playerType)
                    showUserInteraction()
                }

                builder.setNeutralButton("遲下再講") { dialog, which ->
                    playerType = playerUseUnknown
                    SharedPreference(this).saveInt("playerType", playerType)
                    showUserInteraction()
                }

                builder.setCancelable(false)

                builder.show()
            } else {
                showUserInteraction()
            }
        } else if (lastShownDialog == 3) {
            copyUrlToClipboard = SharedPreference(this).getInt("copyUrlToClipboard")
            if (copyUrlToClipboard == -1) {
                val builder = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                builder.setTitle("複製播放網址")
                builder.setMessage("你想每次播放都複製播放網址到剪貼簿嗎？")

                builder.setPositiveButton("好") { dialog, which ->
                    copyUrlToClipboard = doCopyUrlToClipboard
                    SharedPreference(this).saveInt("copyUrlToClipboard", copyUrlToClipboard)
                    showUserInteraction()
                }

                builder.setNegativeButton("不了") { dialog, which ->
                    copyUrlToClipboard = dontCopyUrlToClipboard
                    SharedPreference(this).saveInt("copyUrlToClipboard", copyUrlToClipboard)
                    showUserInteraction()
                }

                builder.setCancelable(false)

                builder.show()
            } else {
                showUserInteraction()
            }
        } else {
            initialize()
        }
    }

    private fun bye9bye() {
        if (SDK_VER >= 21) {
            finishAndRemoveTask()
        } else {
            finishAffinity()
        }
    }

    private fun showLayout() {
        if (isTV) {
            if (!initialized) {
                setContentView(R.layout.layout_tv)
            }
        } else {
            setContentView(R.layout.layout_phone)

            var numColumn: Int = 1
            if (this@MainActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                numColumn = 3
            }else if (this@MainActivity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                numColumn = 2
            }
            gridviewMovie.numColumns = numColumn

            gridviewMovie.adapter = MovieAdapter(this@MainActivity)
            gridviewMovie.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
                TVHandler().prepareVideo(MovieList.list[i])
            }
        }
    }

    private fun restoreState() {
        if (isTV && !restored && playerType > -1) {
            val currentVideoID = SharedPreference(this).getInt("currentVideoID")

            if (currentVideoID > -1) {
                SharedPreference(MainActivity.ctx).saveInt("currentVideoID", -1)
                TVHandler().prepareVideo(MovieList.list[currentVideoID])
            }

            restored = true
        }
    }

    companion object {
        lateinit var ctx: Context
        private val SDK_VER = Build.VERSION.SDK_INT
        var isTV: Boolean = false
        private var initialized: Boolean = false
        private var restored: Boolean = false

        lateinit var clipboardManager: ClipboardManager

        lateinit var requestQueue: RequestQueue
        lateinit var location: String

        private var hasPlaystore: Boolean = true
        var tlsVersionSet: Boolean = false

        private val permissionRequestCode = 1
        private var hasPermission: Boolean = false
        private lateinit var downloadManager: DownloadManager
        private var downloadId: Long = -1

        var lastInitializationStep: Int = -1
        var lastShownDialog: Int = -1

        var playerType: Int = -1
        val playerUseUnknown: Int = -1
        val playerUseInternal: Int = 0
        val playerUseExternal: Int = 1

        var copyUrlToClipboard: Int = -1
        val doCopyUrlToClipboard: Int = 0
        val dontCopyUrlToClipboard: Int = 1
    }
}
