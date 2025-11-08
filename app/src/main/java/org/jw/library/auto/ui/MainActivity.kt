package org.jw.library.auto.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.jw.library.auto.R

/**
 * Main activity for phone interface
 * This is optional - the app primarily works through Android Auto
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple layout programmatically for now
        val textView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setPadding(32, 32, 32, 32)
        }

        setContentView(textView)

        // TODO: Add phone UI for browsing content, managing downloads, settings
        // For now, this is a placeholder - the app works through Android Auto
    }
}
