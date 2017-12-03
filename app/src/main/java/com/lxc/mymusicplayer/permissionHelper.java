package com.lxc.mymusicplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by LaiXiancheng on 2017/12/2.
 * Email: lxc.sysu@qq.com
 */

public class permissionHelper {
	MainActivity mainActivity;
	public permissionHelper(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	public void requestPermission() {
		if (ContextCompat.checkSelfPermission(mainActivity,
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
			ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
		}
		else{
			startAndBingService();
		}
	}


	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
			startAndBingService();
		}
		else{
			Toast.makeText(mainActivity, "拒绝权限，无法使用程序",Toast.LENGTH_SHORT).show();
			mainActivity.finish();
		}
	}

	private void startAndBingService() {
		Intent serviceIntent =  new Intent(mainActivity, MusicService.class);
		mainActivity.startService(serviceIntent);
		mainActivity.bindService(serviceIntent, mainActivity.getServiceConnection(), BIND_AUTO_CREATE);
	}
}
