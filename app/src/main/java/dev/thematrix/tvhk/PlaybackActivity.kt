package dev.thematrix.tvhk

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import kotlin.math.abs
import android.app.PictureInPictureParams
import android.util.Rational


class PlaybackActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.tv)
        super.onCreate(savedInstanceState)

        this.setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, PlaybackVideoFragment())
                .commit()
        }

        paused = false

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()

//        if (!(TVHandler.SDK_VER >= 24 && this.isInPictureInPictureMode)) {
//            PlaybackVideoFragment().pause()
            saveState()
//        }
    }

    override fun onResume() {
        super.onResume()
        restoreState()
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        adjustScreen(resources.configuration)
//    }

//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//
//        if (hasFocus) {
//            adjustScreen(resources.configuration)
//        }
//    }

//    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
//        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
//
//        if (!isInPictureInPictureMode) {
//            adjustScreen(newConfig)
//        }
//    }

//    override fun onUserLeaveHint() {
//        super.onUserLeaveHint()
//
//        pip()
//    }

    private fun saveState(){
        paused = true
    }

    private fun restoreState(){
        if(paused){
            val intent = Intent()
            intent.putExtra("action", "restore")
            this.setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lateinit var direction: String

        if(
            event.keyCode == KeyEvent.KEYCODE_CHANNEL_UP ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_REWIND ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD ||
            event.keyCode == KeyEvent.KEYCODE_NAVIGATE_PREVIOUS ||
            event.keyCode == KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT
        ){
            direction = "PREVIOUS"
        }else if(
            event.keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD ||
            event.keyCode == KeyEvent.KEYCODE_MEDIA_STEP_FORWARD ||
            event.keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT ||
            event.keyCode == KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT
        ){
            direction = "NEXT"
        }else{
            return super.dispatchKeyEvent(event)
        }

        if(event.action == KeyEvent.ACTION_UP){
            channelSwitch(direction, true)
        }

        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                lastCoordinateX = event.x
                lastCoordinateY = event.y
            }else if (event.action == MotionEvent.ACTION_UP) {
                val changeInX = (event.x - lastCoordinateX) / lastCoordinateX * 100
                val changeInY = (event.y - lastCoordinateY) / lastCoordinateY * 100
                val absChangeInX = abs(changeInX)
                val absChangeInY = abs(changeInY)

                if (absChangeInX > 50 || absChangeInY > 50){
                    if (abs(absChangeInX) > abs(absChangeInY)) {
                        if (changeInX < 0) { // left
                            channelSwitch("PREVIOUS", true)
                        }else if (changeInX > 0) { // right
                            channelSwitch("NEXT", true)
                        }
                    }else{
                        if (changeInY < 0) { // up
                        }else if (changeInY > 0) { // down
//                            pip()
                        }
                    }
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    fun channelSwitch(direction: String, showMessage: Boolean){
        val intent = Intent()
        intent.putExtra("action", "switch")
        intent.putExtra("direction", direction)
        intent.putExtra("showMessage", showMessage)
        this.setResult(Activity.RESULT_OK, intent)
        finish()
    }

//    fun pip(){
//        if (TVHandler.SDK_VER >= 26 && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
//            val params = PictureInPictureParams
//                .Builder()
//                .setAspectRatio(Rational(16, 9))
//                .build()
//            enterPictureInPictureMode(params)
//        }
//    }

//    fun adjustScreen(config: Configuration){
//        val decorView = window.decorView
//        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//
//        } else {
//            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//
//        }
//    }

    companion object{
        private var paused: Boolean = false
        private var lastCoordinateX: Float = 0f
        private var lastCoordinateY: Float = 0f
    }
}
