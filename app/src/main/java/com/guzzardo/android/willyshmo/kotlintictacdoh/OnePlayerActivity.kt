package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class OnePlayerActivity : Activity() {
    private var mPlayer1Name = "Player 1"
    private val mPlayer2Name = "Willy"
    private var mButtonPlayer1MoveFirst: Button? = null
    private var mButtonPlayer2MoveFirst: Button? = null
    private var mAdView: AdView? = null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.one_player)
        val player1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        if (player1Name != null) mPlayer1Name = player1Name

        mButtonPlayer1MoveFirst = findViewById<View>(R.id.start_player) as Button
        mButtonPlayer1MoveFirst!!.text = "$mPlayer1Name moves first?"
        mButtonPlayer2MoveFirst = findViewById<View>(R.id.start_comp) as Button
        mButtonPlayer2MoveFirst!!.text = "$mPlayer2Name moves first?"

        findViewById<View>(R.id.start_player).setOnClickListener { startGame(true) }
        findViewById<View>(R.id.start_comp).setOnClickListener { startGame(false) }

        mAdView = findViewById<View>(R.id.ad_one_player) as AdView
        val adRequest = AdRequest.Builder().build()
        mAdView!!.loadAd(adRequest)
    }

    private fun startGame(startWithHuman: Boolean) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra(
            GameActivity.EXTRA_START_PLAYER,
            if (startWithHuman) GameView.State.PLAYER1.value else GameView.State.PLAYER2.value
        )
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
        i.putExtra(GameActivity.PLAY_AGAINST_WILLY, "true")
        startActivity(i)
    }

    override fun onResume() {
        super.onResume()
    }
}