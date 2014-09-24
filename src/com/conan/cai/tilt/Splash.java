package com.conan.cai.tilt;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.VideoView;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		 VideoView myVideoView = (VideoView)findViewById(R.id.myvideoview);
		 Uri uri = Uri.parse("android.resource://com.conan.cai.tilt/raw/splash");
		 myVideoView.setVideoURI(uri);
		 myVideoView.start();
		 
		 myVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

	            @Override
	            public void onCompletion(MediaPlayer mp){
	            	Intent game = new Intent(Splash.this, Tilt.class);
	            	Splash.this.startActivity(game);
	            	finish();
	            }
	        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_splash, menu);
		return true;
	}

}
