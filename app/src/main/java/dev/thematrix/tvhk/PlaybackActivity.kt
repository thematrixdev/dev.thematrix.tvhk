package dev.thematrix.tvhk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import kotlin.math.abs

class PlaybackActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        saveState()
    }

    override fun onResume() {
        super.onResume()
        restoreState()
    }

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
//                        if (changeInY < 0) { // up
//                        }else if (changeInY > 0) { // down
//                        }
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

    companion object{
        private var paused: Boolean = false
        private var lastCoordinateX: Float = 0f
        private var lastCoordinateY: Float = 0f
    }
}
