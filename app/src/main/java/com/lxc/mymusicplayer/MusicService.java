package com.lxc.mymusicplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.IOException;

public class MusicService extends Service {
	private MusicBinder musicBinder = new MusicBinder();
	private MediaPlayer mp = new MediaPlayer();
	public enum StateEnum {PLAY,PAUSE,STOP}
	private StateEnum curState;
	public MusicService() {
	}
	@Override
	public IBinder onBind(Intent intent) {
		try {
			mp.setDataSource("/storage/emulated/0/mymusic/任然 - 你好陌生人.mp3");
			mp.prepare();
			mp.setLooping(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return musicBinder;
	}

	class MusicBinder extends Binder{
		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			switch (code){
				//暂停或者播放
				case 1:
					if (mp.isPlaying()){
						mp.pause();
						curState = StateEnum.PAUSE;
					}
					else{
						mp.start();
						curState = StateEnum.PLAY;
					}
					break;
				//停止音乐
				case 2:
					mp.seekTo(0);
					mp.pause();
					curState = StateEnum.STOP;
					break;
				//完全退出
				case 3:
					mp.stop();
					mp.release();
					stopSelf();
					break;
				//拖动滑动条
				case 4:
					float progress = data.readFloat();
					mp.seekTo(((int)(progress*mp.getDuration())));
					break;
				//获取现在的音乐位置以及音乐总长度
				case 5:
					int curTime = mp.getCurrentPosition();
					int length = mp.getDuration();
					reply.writeInt(curTime);
					reply.writeInt(length);
					reply.setDataPosition(0);//设置偏移量为0，待会读的时候就能从头读
					break;
				default:

			}
			return super.onTransact(code, data, reply, flags);
		}

		public StateEnum getState(){
			return curState;
		}

	}

}
