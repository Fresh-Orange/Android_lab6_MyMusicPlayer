package com.lxc.mymusicplayer;

import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Created by LaiXiancheng on 2017/12/3.
 * Email: lxc.sysu@qq.com
 */

public class connectRunnable implements Runnable {
	private MusicService.MusicBinder musicBinder;
	private MainActivity.ProgressHandler progressHandler;

	public connectRunnable(MusicService.MusicBinder musicBinder, MainActivity.ProgressHandler progressHandler) {
		this.musicBinder = musicBinder;
		this.progressHandler = progressHandler;
	}

	/**
	 * 每一秒和service通信一次，获取届时数据，然后交给hander更新界面
	 */
	@Override
	public void run() {
		while(true){
			try {
				Parcel timeReply = Parcel.obtain();
				musicBinder.transact(5, Parcel.obtain(), timeReply, 0);
				int curTime = timeReply.readInt();
				int length = timeReply.readInt();
				Message message = new Message();
				message.what = 666;
				message.arg1 = ((int)(curTime*1.0/length*100));
				message.arg2 = curTime;
				progressHandler.sendMessage(message);

				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
