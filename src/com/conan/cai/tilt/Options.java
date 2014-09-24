package com.conan.cai.tilt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class Options extends Activity {
	Switch s;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_options);
		
		 s = (Switch) findViewById(R.id.hardcore);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_options, menu);
		return true;
	}
	
	@Override
	public void onBackPressed(){
    	Intent i = new Intent();
    	if(s.isChecked()){
	    	i.putExtra("hc", true);
	    	setResult(RESULT_OK, i);
	    	finish();
    	}
    	else{
    		i.putExtra("hc", false);
	    	setResult(RESULT_OK, i);
	    	finish();
    	}
    		
	}
}
