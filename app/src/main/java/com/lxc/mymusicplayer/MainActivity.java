package com.lxc.mymusicplayer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{
	private TextView tvCurTime;
	private TextView tvWholeTime;
	private Button btPlay, btStop, btQuit;
	private TextView tvHint;
	private SeekBar sbMusic;
	private ImageView imageView;
	private MusicService.MusicBinder musicBinder;
	private boolean isPause = true;
	ObjectAnimator rotationAnimator;
	SimpleDateFormat formater = new SimpleDateFormat("mm:ss");
	enum StateEnum {PLAY,PAUSE,STOP}
	//final String CUR_TIME_KEY = "CURRENT_TIME";


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		try {
			Parcel parcel = Parcel.obtain();
			parcel.writeFloat(((float)seekBar.getProgress())/100);
			musicBinder.transact(4,parcel , Parcel.obtain(), 0);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	class  ProgressHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 666){
				 sbMusic.setProgress(msg.arg1);
				 int curTime = msg.arg2;
				 tvCurTime.setText(formater.format(new Date(curTime)));
			}
		}
	}

	final ProgressHandler progressHandler = new ProgressHandler();

	Thread updateThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true){
				try {
					int curTime = musicBinder.getCurTime();
					Message message = new Message();
					message.what = 666;
					message.arg1 = ((int)(curTime*1.0/musicBinder.getMusicLength()*100));
					message.arg2 = curTime;
					progressHandler.sendMessage(message);
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tvCurTime = findViewById(R.id.tv_current_time);
		tvWholeTime = findViewById(R.id.tv_whole_time);
		tvHint = findViewById(R.id.tv_hint);
		btPlay = findViewById(R.id.bt_play);
		btStop = findViewById(R.id.bt_stop);
		btQuit = findViewById(R.id.bt_quit);
		btPlay.setOnClickListener(this);
		btStop.setOnClickListener(this);
		btQuit.setOnClickListener(this);
		sbMusic = findViewById(R.id.sb_music);
		sbMusic.setOnSeekBarChangeListener(this);
		imageView = findViewById(R.id.iv_image);

		requestPermission();
	}

	private void requestPermission() {
		if (ContextCompat.checkSelfPermission(MainActivity.this,
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
			ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
		}
		else{
			startAndBingService();
		}
	}

	private void startAndBingService() {
		Intent serviceIntent =  new Intent(this, MusicService.class);
		startService(serviceIntent);
		bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
			startAndBingService();
		}
		else{
			Toast.makeText(this, "拒绝权限，无法使用程序",Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.bt_play:
				try {
					musicBinder.transact(1, Parcel.obtain(), Parcel.obtain(), 0);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				updateViews((Button) v, StateEnum.PLAY);

				break;
			case R.id.bt_stop:
				try {
					musicBinder.transact(2, Parcel.obtain(), Parcel.obtain(), 0);
					updateViews((Button) v, StateEnum.STOP);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case R.id.bt_quit:
				unbindService(serviceConnection);
				try {
					musicBinder.transact(3, Parcel.obtain(), Parcel.obtain(), 0);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				finish();
				System.exit(0);
				break;
		}
	}

	private void updateViews(Button v, StateEnum state) {
		if (state == StateEnum.PLAY){
			if (isPause){
				isPause = false;
				v.setText("Pause");
				tvHint.setText("Playing...");
				startRotationAnim();
			}
			else{
				isPause = true;
				v.setText("Play");
				tvHint.setText("Paused!");
				if (rotationAnimator != null && rotationAnimator.isRunning()){
					float cur_rotation = (Float) rotationAnimator.getAnimatedValue();
					rotationAnimator.end();
					imageView.setRotation(cur_rotation);//让角度停在停的时候
				}
			}
		}
		else if (state == StateEnum.STOP){
			tvHint.setText("Stopped!");
			isPause = true;
			btPlay.setText("Play");
			if (rotationAnimator != null && rotationAnimator.isRunning())
				rotationAnimator.end();
		}
	}

	private void startRotationAnim() {
		//这里的360如果不加上imageView.getRotation()的话动画重复的时候会产生跳跃
		rotationAnimator = ObjectAnimator.ofFloat(imageView,"rotation",
				imageView.getRotation(),360f+imageView.getRotation());
		rotationAnimator.setDuration(5000);
		rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
		rotationAnimator.setInterpolator(new LinearInterpolator());
		rotationAnimator.start();
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			musicBinder = (MusicService.MusicBinder) service;
			updateThread.start();
			int musicLength = musicBinder.getMusicLength();
			tvWholeTime.setText(formater.format(new Date(musicLength)));
			if (musicBinder.getState()==MusicService.StateEnum.PLAY){
				isPause = true;
				updateViews(btPlay, StateEnum.PLAY);
			}
			else if (musicBinder.getState()==MusicService.StateEnum.PAUSE){
				isPause = false;
				updateViews(btPlay, StateEnum.PLAY);
			}
			else{
				updateViews(btStop, StateEnum.STOP);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};
}
