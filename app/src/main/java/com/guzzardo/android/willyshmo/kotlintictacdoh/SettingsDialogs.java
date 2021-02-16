/*
 * Copyright (C) 2007 The Android Open Source Project
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
 */

package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.appcompat.app.AlertDialog;

import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences;

//import android.support.v7.app.AlertDialog;

public class SettingsDialogs extends Activity {

    private static final int MAX_PROGRESS = 100;
    private ProgressDialog mProgressDialog;
    private int mProgress;
    private Handler mProgressHandler;
    private String mPlayer1Name, mPlayer2Name;
    private Button mButtonPlayer1, mButtonPlayer2;
    private static boolean mMoveModeTouch;  //false = drag move mode; true = touch move mode
    private static int mMoveModeChecked; // 0 = drag move mode; 1 = touch move mode
    private static int mSoundModeChecked; // 0 = sound on; 1 = sound off    
    private static boolean mSoundMode; //false = no sound; true = sound
    private SeekBar mSeekBar;
    private int mTokenSize, mTokenColor, mTokenColor1, mTokenColor2;

    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView(int)} to
     * describe what is to be displayed in the screen.
     */
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        mPlayer1Name = getIntent().getStringExtra(GameActivity.PLAYER1_NAME);
        mPlayer2Name = getIntent().getStringExtra(GameActivity.PLAYER2_NAME);

        setContentView(R.layout.settings_dialog);
        
        mPlayer1Name = mPlayer1Name == null ? " " : mPlayer1Name; 
        mPlayer2Name = mPlayer2Name == null ? " " : mPlayer2Name; 
        
        mButtonPlayer1 = (Button) findViewById(R.id.text_entry_button_player1_name);
        mButtonPlayer1.setText("Player 1 Name: "+mPlayer1Name);
        mButtonPlayer2 = (Button) findViewById(R.id.text_entry_button_player2_name);
        mButtonPlayer2.setText("Player 2 Name: "+mPlayer2Name);
        
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE);
        mMoveModeTouch = settings.getBoolean(GameActivity.MOVE_MODE, false);
        mTokenSize = settings.getInt(GameActivity.TOKEN_SIZE, 50);
        mTokenColor1 = settings.getInt(GameActivity.TOKEN_COLOR_1, Color.RED);
        mTokenColor2 = settings.getInt(GameActivity.TOKEN_COLOR_2, Color.BLUE);
        
        
        mMoveModeChecked = !mMoveModeTouch ? 0 : 1;
        mSoundModeChecked = mSoundMode ? 0 : 1;
        
        /* Display a text message with yes/no buttons and handle each message as well as the cancel action */
        Button twoButtonsTitle = (Button) findViewById(R.id.reset_scores);
        twoButtonsTitle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
//                showDialog(RESET_SCORES);
                AlertDialog resetScoresDialog = showResetScoresDialog();
                resetScoresDialog.show();
            }
        });
        

        /* Display a text entry dialog for entry of player 1 name */
//        Button textEntryPlayer1Name = (Button) findViewById(R.id.text_entry_button_player1_name);
//        textEntryPlayer1Name.setOnClickListener(new OnClickListener() {
        mButtonPlayer1.setOnClickListener(new OnClickListener() {        
            public void onClick(View v) {
                AlertDialog playerNameDialog = showPlayerNameDialog(1);
                playerNameDialog.show();
            }
        });

        /* Display a text entry dialog for entry of player 2 name */
//        Button textEntryPlayer2Name = (Button) findViewById(R.id.text_entry_button_player2_name);
//        textEntryPlayer2Name.setOnClickListener(new OnClickListener() {
        mButtonPlayer2.setOnClickListener(new OnClickListener() {         	
            public void onClick(View v) {
                AlertDialog playerNameDialog = showPlayerNameDialog(2);
                playerNameDialog.show();
                
            }
        });
        
        mProgressHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (mProgress >= MAX_PROGRESS) {
                    mProgressDialog.dismiss();
                } else {
                    mProgress++;
                    mProgressDialog.incrementProgressBy(1);
                    mProgressHandler.sendEmptyMessageDelayed(0, 100);
                }
            }
        };
        
        /* Display a radio button group */
      Button radioButton = (Button) findViewById(R.id.move_mode);
      radioButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
//              showDialog(DIALOG_MOVE_MODE);
              AlertDialog moveModeDialog = showMoveModeDialog();
              moveModeDialog.show();
          }
      });        
      
      /* Display a radio button group */
    radioButton = (Button) findViewById(R.id.sound_mode);
    radioButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
//            showDialog(DIALOG_MOVE_MODE);
            AlertDialog soundModeDialog = showSoundModeDialog();
            soundModeDialog.show();
        }
    });
    
    Button tokenSizeTitle = (Button) findViewById(R.id.seeker_entry_token_size);
    tokenSizeTitle.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            AlertDialog tokenSizeDialog = showTokenSizeDialog();
            
            tokenSizeDialog.show();
            
        }
    });

    
    
    Button tokenColorTitle = (Button) findViewById(R.id.seeker_entry_token_color_1);
    tokenColorTitle.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            AlertDialog tokenColorDialog = showTokenColorDialog(1);
            
            tokenColorDialog.show();
            
        }
    });
    
    Button tokenColorTitle2 = (Button) findViewById(R.id.seeker_entry_token_color_2);
    tokenColorTitle2.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            AlertDialog tokenColorDialog2 = showTokenColorDialog(2);
            
            tokenColorDialog2.show();
            
        }
    });
    
    
    
    }
    
	private AlertDialog showMoveModeDialog() {
		return new AlertDialog.Builder(SettingsDialogs.this)
		.setIcon(R.drawable.willy_shmo_small_icon)
		.setTitle(R.string.move_mode)
		.setSingleChoiceItems(R.array.select_move_mode, mMoveModeChecked, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				setMoveModeSelection(whichButton);
			}
		})
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				setMoveMode();
			}
		})
		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		})
		.create();
	}

	private AlertDialog showSoundModeDialog() {
		return new AlertDialog.Builder(SettingsDialogs.this)
		.setIcon(R.drawable.willy_shmo_small_icon)
		.setTitle(R.string.sound_mode)
		.setSingleChoiceItems(R.array.select_sound_mode, mSoundModeChecked, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				setSoundModeSelection(whichButton);
			}
		})
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				setSoundMode();
			}
		})
		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		})
		.create();
	}
	
	private AlertDialog showResetScoresDialog() {
        return new AlertDialog.Builder(SettingsDialogs.this)
        .setIcon(R.drawable.willy_shmo_small_icon)
        .setTitle(R.string.alert_dialog_reset_scores)
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked OK so do some stuff */
            	resetScores();
            }
        })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked Cancel so do some stuff */
            }
        })
        .create();
    }
        
	private AlertDialog showPlayerNameDialog(final int playerId) {
	    // This example shows how to add a custom layout to an AlertDialog
		
		int titleId = R.string.alert_dialog_text_entry_player1_name;
		if (playerId == 2) {
			titleId = R.string.alert_dialog_text_entry_player2_name;
		}
		
	    LayoutInflater factory = LayoutInflater.from(this);        
	    final View textEntryViewPlayer = factory.inflate(R.layout.name_dialog_text_entry, null);
	    return new AlertDialog.Builder(SettingsDialogs.this)
	        .setIcon(R.drawable.willy_shmo_small_icon)
	        .setTitle(titleId)
	        .setView(textEntryViewPlayer)
	        .setCancelable(false)
	        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                /* User clicked OK so do some stuff */
	            	EditText userName = (EditText)textEntryViewPlayer.findViewById(R.id.username_edit);
	            	Editable userNameText = userName.getText();
	            	int userNameLength = userNameText.length() > 15 ? 15 : userNameText.length();
	            	char[] userNameChars = new char[userNameLength];
	            	userNameText.getChars(0, userNameLength, userNameChars, 0);
	            	Intent intent = new Intent(getApplicationContext(), SettingsDialogs.class);
	            	if (playerId == 1) {
	            		mPlayer1Name = new String(userNameChars);
	            		intent.putExtra(GameActivity.PLAYER1_ID, 0); 
	            	} else {
	            		mPlayer2Name = new String(userNameChars);
	            	}
	            	intent.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);                    	
	            	intent.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name);
	            	startActivityForResult(intent, 1);
	            	finish();
	            }
	        })
	        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                /* User clicked cancel so do some stuff */
	            }
	        })
	        .create();
		}

	
	private AlertDialog showTokenSizeDialog() {
		
		AlertDialog.Builder tokenSizeDialog = new AlertDialog.Builder(this);
		int titleId = R.string.alert_dialog_seeker_entry_token_size;
	    LayoutInflater factory = LayoutInflater.from(this);        
	    final View tokenSizeEntryView = factory.inflate(R.layout.token_size_dialog_entry, null); 
	    tokenSizeDialog.setView(tokenSizeEntryView);
	    mSeekBar = (SeekBar) tokenSizeEntryView.findViewById(R.id.seekBar); 
	    mSeekBar.setProgress(mTokenSize);
	    mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		public void onStopTrackingTouch(SeekBar seekBar) {
		}

		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			mTokenSize = progress;
		}
	
	    });
	    
	    tokenSizeDialog.setIcon(R.drawable.willy_shmo_small_icon);
        tokenSizeDialog.setTitle(titleId);
        tokenSizeDialog.setCancelable(false);

        tokenSizeDialog.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked OK so do some stuff */
            	setTokenSize();
            	Intent intent = new Intent(getApplicationContext(), SettingsDialogs.class);
            	startActivityForResult(intent, 1);
            	finish();
            }
        });
        
        tokenSizeDialog.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked cancel so do some stuff */
            }
        });
	    return tokenSizeDialog.create();
	}
	
	public void setTokenColorFromDialog(int newTokenColor) {
		mTokenColor = newTokenColor;
	}

	
	private AlertDialog showTokenColorDialog(final int playerNumber) {
		
		AlertDialog.Builder tokenColorDialog = new AlertDialog.Builder(this);
		int titleId = playerNumber == 1 ? R.string.alert_dialog_seeker_entry_token_color_1 : R.string.alert_dialog_seeker_entry_token_color_2;
		
        int newColor = playerNumber == 1 ? mTokenColor1 : mTokenColor2;
        TokenColorPickerView tokenColorPickerView = new TokenColorPickerView(this, SettingsDialogs.this, newColor);
		
	    tokenColorDialog.setView(tokenColorPickerView);
	    
	    tokenColorDialog.setIcon(R.drawable.willy_shmo_small_icon);
	    tokenColorDialog.setTitle(titleId);
	    tokenColorDialog.setCancelable(false);

	    tokenColorDialog.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked OK so do some stuff */
            	setTokenColor(playerNumber);
            	Intent intent = new Intent(getApplicationContext(), SettingsDialogs.class);
            	startActivityForResult(intent, 1);
            	finish();
            }
        });
        
	    tokenColorDialog.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked cancel so do some stuff */
            }
        });
	    
	    return tokenColorDialog.create();
	}
	
    @Override
    protected void onResume() {
        super.onResume();  
        mPlayer1Name = getIntent().getStringExtra(GameActivity.PLAYER1_NAME);
        mPlayer2Name = getIntent().getStringExtra(GameActivity.PLAYER2_NAME);        
    }
    
    @Override
    protected void onPause() {
        super.onPause();    
        getIntent().putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);
        getIntent().putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name);
    }    
    
    @Override
    protected void onStop() {
        super.onStop();
        
     // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(GameActivity.PLAYER1_NAME, mPlayer1Name);
        editor.putString(GameActivity.PLAYER2_NAME, mPlayer2Name);

        // Commit the edits!
        editor.commit();        
    }
    
	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
		super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("gDialog_move_mode", mMoveModeTouch);
        savedInstanceState.putBoolean("gDialog_sound_mode", mSoundMode);
        savedInstanceState.putInt("gDialog_token_size", mTokenSize);        
    }        
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.	
		mMoveModeTouch = savedInstanceState.getBoolean("gDialog_move_mode");
		mSoundMode = savedInstanceState.getBoolean("gDialog_sound_mode");	
		mTokenSize = savedInstanceState.getInt("gDialog_token_size");
	}
    
	private void resetScores() {
	    // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(GameActivity.PLAYER1_SCORE, 0);
        editor.putInt(GameActivity.PLAYER2_SCORE, 0);
        editor.putInt(GameActivity.WILLY_SCORE, 0);
        // Commit the edits!
        editor.commit();  
	}
	
	private void setMoveModeSelection(int moveMode) {
        mMoveModeTouch = moveMode == 0 ? false : true;		
	}
	
	private void setMoveMode() {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(GameActivity.MOVE_MODE, mMoveModeTouch);
        // Commit the edits!
        editor.commit();  
	}
	
	private void setTokenSize() {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(GameActivity.TOKEN_SIZE, mTokenSize);
        // Commit the edits!
        editor.commit();  
	}

	private void setTokenColor(int playerNumber) {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        if (playerNumber == 1) {
        	editor.putInt(GameActivity.TOKEN_COLOR_1, mTokenColor);
        } else {
        	editor.putInt(GameActivity.TOKEN_COLOR_2, mTokenColor);
        }
        // Commit the edits!
        editor.commit();  
	}
	
	
	private void setSoundModeSelection(int soundMode) {
        mSoundMode = soundMode == 0 ? true : false;		
	}

	private void setSoundMode() {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(GameActivity.SOUND_MODE, mSoundMode);
        // Commit the edits!
        editor.commit();  
	}
	
}
