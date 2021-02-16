package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.os.Bundle
import android.view.View

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */   class TokenSelectorActivity : Activity() {
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