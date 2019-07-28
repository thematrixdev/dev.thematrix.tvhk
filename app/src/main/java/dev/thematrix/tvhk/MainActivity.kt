package dev.thematrix.tvhk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpSSL()
        restoreState()
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

    private fun restoreState(){
        val currentVideoID = SharedPreference(this).getInt("currentVideoID")

        if(currentVideoID > -1) {
            val intent = Intent(this, PlaybackActivity::class.java)
            intent.putExtra(DetailsActivity.MOVIE, MovieList.list[currentVideoID])
            startActivity(intent)
        }
    }
}
