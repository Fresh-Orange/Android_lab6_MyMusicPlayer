package com.lxc.mymusicplayer;

import android.os.Parcel;
import android.os.RemoteException;
import android.widget.SeekBar;

/**
 * Created by LaiXiancheng on 2017/12/2.
 * Email: lxc.sysu@qq.com
 */

public class progressChangeListener implements SeekBar.OnSeekBarChangeListener {
	MusicService.MusicBinder musicBinder;
	public progressChangeListener(MusicService.MusicBinder musicBinder) {
		this.musicBinder = musicBinder;
	}

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
}
