package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.os.Bundle
import android.view.View

class TokenSelectorActivity : Activity() {
    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.token_selector)
        findViewById<View>(R.id.start_x).setOnClickListener {
            setResult(GameView.BoardSpaceValues.CROSS)
            finish()
        }
        findViewById<View>(R.id.start_o).setOnClickListener {
            setResult(GameView.BoardSpaceValues.CIRCLE)
            finish()
        }
    }
}