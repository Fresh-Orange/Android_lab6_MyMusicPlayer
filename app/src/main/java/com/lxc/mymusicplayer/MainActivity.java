package com.lxc.mymusicplayer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
	private TextView tvCurTime;
	private TextView tvWholeTime;
	private Button btPlay, btStop, btQuit;
	private TextView tvHint;
	private SeekBar sbMusic;
	private ImageView imageView;
	private MusicService.MusicBinder musicBinder;
	private boolean isPause = true;
	ObjectAnimator rotationAnimator;
	SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
	enum StateEnum {PLAY,PAUSE,STOP}
	permissionHelper permissionHelper = new permissionHelper(this);
	Thread updateThread;

	/**
	 * 线程获得数据之后，由Hander更新界面
	 */
	class  ProgressHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 666){
				 sbMusic.setProgress(msg.arg1);
				 int curTime = msg.arg2;
				 tvCurTime.setText(formatter.format(new Date(curTime)));
			}
		}
	}
	final ProgressHandler progressHandler = new ProgressHandler();

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
		imageView = findViewById(R.id.iv_image);

		permissionHelper.requestPermission();
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
				if (isPause)
					updateViews(StateEnum.PLAY);
				else
					updateViews(StateEnum.PAUSE);
				break;
			case R.id.bt_stop:
				//只有正在播放的时候才stop
				if (musicBinder.getState() != MusicService.StateEnum.PLAY)
					return;
				try {
					musicBinder.transact(2, Parcel.obtain(), Parcel.obtain(), 0);
					updateViews(StateEnum.STOP);
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

	/**
	 * 根据不同状态更新界面
	 * @param state 新的状态
	 */
	private void updateViews(StateEnum state) {
		if (state == StateEnum.PLAY){
			isPause = false;
			btPlay.setText("Pause");
			tvHint.setText("Playing...");
			startRotationAnim();
		}
		else if (state == StateEnum.PAUSE){
			isPause = true;
			btPlay.setText("Play");
			tvHint.setText("Paused!");
			if (rotationAnimator != null && rotationAnimator.isRunning()){
				float cur_rotation = (Float) rotationAnimator.getAnimatedValue();
				rotationAnimator.end();
				imageView.setRotation(cur_rotation);//让角度停在停的时候
			}
		}
		else if (state == StateEnum.STOP){
			tvHint.setText("Stopped!");
			isPause = true;
			btPlay.setText("Play");
			if (rotationAnimator != null && rotationAnimator.isRunning())
				rotationAnimator.end();
				imageView.setRotation(0);//让角度恢复0度
		}
	}

	/**
	 * 开启旋转动画
	 */
	private void startRotationAnim() {
		//这里的360如果不加上imageView.getRotation()的话动画重复的时候会产生跳跃
		rotationAnimator = ObjectAnimator.ofFloat(imageView,"rotation",
				imageView.getRotation(),360f+imageView.getRotation());
		rotationAnimator.setDuration(10000);
		rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
		rotationAnimator.setInterpolator(new LinearInterpolator());
		rotationAnimator.start();
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			musicBinder = (MusicService.MusicBinder) service;

			//因为progressChangeListener里面要和service通信，所以绑定之后开始监听
			progressChangeListener progressListener = new progressChangeListener(musicBinder);
			sbMusic.setOnSeekBarChangeListener(progressListener);

			//因为线程里面要和service通信，所以绑定之后才开启线程
			updateThread = new Thread(new connectRunnable(musicBinder, progressHandler));
			updateThread.start();

			//获得音乐总时间
			Parcel timeReply = Parcel.obtain();
			try {
				musicBinder.transact(5, Parcel.obtain(), timeReply, 0);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			timeReply.setDataPosition(4);//第二个数（length）的位置，因为int是4字节
			int musicLength = timeReply.readInt();
			tvWholeTime.setText(formatter.format(new Date(musicLength)));

			//获取当前状态，根据当前状态更新界面
			//原因：有可能app关闭之后重新打开，因为音乐是能后台播放的，那么这时候音乐的状态有这三种可能
			if (musicBinder.getState()==MusicService.StateEnum.PLAY){
				updateViews(StateEnum.PLAY);
			}
			else if (musicBinder.getState()==MusicService.StateEnum.PAUSE){
				updateViews(StateEnum.PAUSE);
			}
			else{
				updateViews(StateEnum.STOP);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};

	public ServiceConnection getServiceConnection() {
		return serviceConnection;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}
}
