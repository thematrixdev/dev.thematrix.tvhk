package dev.thematrix.tvhk

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity

class PlaybackActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, PlaybackVideoFragment())
                .commit()
        }

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
        SharedPreference(this).saveInt("currentVideoID", PlaybackVideoFragment.currentVideoID)
        PlaybackVideoFragment.currentVideoID = -1
    }

    private fun restoreState(){
        val currentVideoID = SharedPreference(this).getInt("currentVideoID")

        if(currentVideoID > -1){
            SharedPreference(this).saveInt("currentVideoID", -1)

            PlaybackVideoFragment().prepareVideo(
                MovieList.list[currentVideoID].id,
                MovieList.list[currentVideoID].title,
                MovieList.list[currentVideoID].videoUrl,
                MovieList.list[currentVideoID].func
            )
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
            PlaybackVideoFragment().channelSwitch(direction, true)
        }

        return true
    }
}
