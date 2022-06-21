package com.example.tdyaudio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * AudioTrack 可以静态/流式传输，可以调整音量，循环，单双通道播放
 * 1.Invalid audio buffer size.以帧为单位，否则异常
 * 2.要在子线程中播放，否则不能调音量，不能暂停和停止
 * 3.static模式循环要在play设置，并且以帧为单位，不是以字节为单位，需要计算帧数
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = MainActivity.class.getSimpleName();
    private Button mPlayBtn;
    private Button mPauseBtn;
    private Button mStopBtn;
    private SwitchCompat mLoopSw;
    private SwitchCompat mModeSw;
    private SwitchCompat mChannelSw;
    private TextView mVolTv;
    private TextView mVolLeftTv;
    private TextView mVolRightTv;
    private SeekBar mVolSb;
    private SeekBar mVolLeftSb;
    private SeekBar mVolRightSb;
    private boolean mIsLoop = false;//是否循环播放
    private boolean mIsStatic = false;//是否是静态缓存模式
    private boolean mIsStereo = false;//是否是双通道
    private byte[] mAudioData;
    private byte[] mAudioDataLeft;
    private byte[] mAudioDataRight;
    private AudioTrack mAudioTrack;
    private AudioTrack mAudioTrackLeft;
    private AudioTrack mAudioTrackRight;
    private boolean mPlaying = false;
    private boolean mPlayingLeft = false;
    private boolean mPlayingRight = false;
    private static final int STREAM_BUFFER_SIZE = 1024 * 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mPlayBtn = findViewById(R.id.play_btn);
        mPauseBtn = findViewById(R.id.pause_btn);
        mStopBtn = findViewById(R.id.stop_btn);
        mLoopSw = findViewById(R.id.loop_sw);
        mModeSw = findViewById(R.id.mode_sw);
        mChannelSw = findViewById(R.id.channel_sw);
        mVolTv = findViewById(R.id.vol_tv);
        mVolLeftTv = findViewById(R.id.vol_left_tv);
        mVolRightTv = findViewById(R.id.vol_right_tv);
        mVolSb = findViewById(R.id.vol_sb);
        mVolLeftSb = findViewById(R.id.vol_left_sb);
        mVolRightSb = findViewById(R.id.vol_right_sb);
    }

    private void initListener() {
        mPlayBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mLoopSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean loop) {
                mIsLoop = loop;
                Log.e(TAG, "是否循环播放=" + mIsLoop);
            }
        });
        mModeSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isStatic) {
                mIsStatic = isStatic;
                if (mIsStatic) {
                    mAudioData = getAudioDate("priority1.wav");
                    mAudioDataLeft = getAudioDate("priority2.wav");
                    mAudioDataRight = getAudioDate("priority3.wav");
                } else {
                    mAudioData = new byte[STREAM_BUFFER_SIZE];
                    mAudioDataLeft = new byte[STREAM_BUFFER_SIZE];
                    mAudioDataRight = new byte[STREAM_BUFFER_SIZE];
                }
                Log.e(TAG, "是否静态缓存=" + mIsStatic);
//                Log.e(TAG, "audioData length=" + mAudioData.length);
//                Log.e(TAG, "audioData_l length=" + mAudioDataLeft.length);
//                Log.e(TAG, "audioData_r length=" + mAudioDataRight.length);
            }
        });
        mChannelSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isStereo) {
                mIsStereo = isStereo;
                if (!isStereo) {
                    mVolTv.setVisibility(View.VISIBLE);
                    mVolSb.setVisibility(View.VISIBLE);
                    mVolLeftTv.setVisibility(View.GONE);
                    mVolLeftSb.setVisibility(View.GONE);
                    mVolRightTv.setVisibility(View.GONE);
                    mVolRightSb.setVisibility(View.GONE);
                } else {
                    mVolTv.setVisibility(View.GONE);
                    mVolSb.setVisibility(View.GONE);
                    mVolLeftTv.setVisibility(View.VISIBLE);
                    mVolLeftSb.setVisibility(View.VISIBLE);
                    mVolRightTv.setVisibility(View.VISIBLE);
                    mVolRightSb.setVisibility(View.VISIBLE);
                }
                Log.e(TAG, "选择单双通道=" + isStereo);
            }
        });
        float maxVol = AudioTrack.getMaxVolume();
        Log.e(TAG, "最大音量=" + maxVol);
        mVolSb.setMax(10);
        mVolSb.setProgress(10);
        mVolSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float vol = (float) progress / seekBar.getMax() * maxVol;
                if (fromUser) {
                    if (mAudioTrack != null) {
//                        mAudioTrack.setStereoVolume(vol,vol);
                        mAudioTrack.setVolume(vol);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mVolLeftSb.setMax(10);
        mVolLeftSb.setProgress(10);
        mVolLeftSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float vol = (float) progress / seekBar.getMax() * maxVol;
                if (fromUser) {
                    if (mAudioTrackLeft != null) {
                        mAudioTrackLeft.setStereoVolume(vol, 0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mVolRightSb.setMax(10);
        mVolRightSb.setProgress(10);
        mVolRightSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float vol = (float) progress / seekBar.getMax() * maxVol;
                if (fromUser) {
                    if (mAudioTrackRight != null) {
                        mAudioTrackRight.setStereoVolume(0, vol);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initData() {
        mAudioData = new byte[STREAM_BUFFER_SIZE];
        mAudioDataLeft = new byte[STREAM_BUFFER_SIZE];
        mAudioDataRight = new byte[STREAM_BUFFER_SIZE];
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_btn:
                playAudio();
                break;
            case R.id.pause_btn:
                pauseAudio();
                break;
            case R.id.stop_btn:
                stopAudio();
                break;
        }
    }

    private byte[] getAudioDate(String name) {
        try {
            InputStream in = getAssets().open(name);
            try {
                int length = in.available();
                ByteArrayOutputStream out = new ByteArrayOutputStream(length);
                for (int b_l; (b_l = in.read()) != -1; ) {
                    out.write(b_l);
                }
                return out.toByteArray();
            } finally {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void playAudio() {
        stopAudio();
        int streamType = AudioManager.STREAM_MUSIC;//音频流的类型音乐
        int simpleRate = 44100;//采样率
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;//通道配置，立体声
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;//音频格式
        int minBufferSize = AudioTrack.getMinBufferSize(simpleRate, channelConfig, audioFormat);
        int dataBufferSize = mAudioData.length / 8 * 8;
        int dataBufferSizeLeft = mAudioDataLeft.length / 8 * 8;// Invalid audio buffer size.以帧为单位，否则异常
        int dataBufferSizeRight = mAudioDataRight.length / 8 * 8;
        int mode;//模式
        if (mIsStatic) {
            mode = AudioTrack.MODE_STATIC;//静态缓存模式
            if (mIsStereo) {
                mAudioTrackLeft = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat, dataBufferSizeLeft, mode);
                Log.e(TAG, "AudioTrack Static left create");

                mAudioTrackRight = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat, dataBufferSizeRight, mode);
                Log.e(TAG, "AudioTrack Static right create");

                mAudioTrackLeft.write(mAudioDataLeft, 0, dataBufferSizeLeft);
                Log.e(TAG, "AudioTrack Static left write length=" + dataBufferSizeLeft);

                mAudioTrackRight.write(mAudioDataRight, 0, dataBufferSizeRight);
                Log.e(TAG, "AudioTrack Static right write length=" + dataBufferSizeRight);

                new Thread(() -> {
                    mPlayingLeft = true;
                    if (mIsLoop) {
                        mAudioTrackLeft.setLoopPoints(0, dataBufferSizeLeft / 8, -1);
                    }
                    mAudioTrackLeft.play();
                    mAudioTrackLeft.setStereoVolume(1, 0);
                    Log.e(TAG, "AudioTrack Static left play");
                }).start();

                new Thread(() -> {
                    mPlayingRight = true;
                    if (mIsLoop) {
                        mAudioTrackRight.setLoopPoints(0, dataBufferSizeRight / 8, -1);
                    }
                    mAudioTrackRight.play();
                    mAudioTrackRight.setStereoVolume(0, 1);
                    Log.e(TAG, "AudioTrack Static right play");
                }).start();
            } else {
                mAudioTrack = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat, dataBufferSize, mode);
                Log.e(TAG, "AudioTrack Static create");

                mAudioTrack.write(mAudioData, 0, dataBufferSize);
                Log.e(TAG, "AudioTrack Static write length=" + dataBufferSize);

                new Thread(() -> {
                    mPlaying = true;
                    if (mIsLoop) {
                        mAudioTrack.setLoopPoints(0, dataBufferSize / 8, 3);
                    }
                    mAudioTrack.play();
                    mAudioTrack.setStereoVolume(1, 1);
                    Log.e(TAG, "AudioTrack Static play ");
                }).start();
            }
        } else {
            mode = AudioTrack.MODE_STREAM;//流模式
            if (mIsStereo) {
                mAudioTrackLeft = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat, Math.max(minBufferSize, STREAM_BUFFER_SIZE), mode);
                Log.e(TAG, "AudioTrack Stream left create");

                mAudioTrackRight = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat, Math.max(minBufferSize, STREAM_BUFFER_SIZE), mode);
                Log.e(TAG, "AudioTrack Stream right create");

                new Thread(() -> {
                    mPlayingLeft = true;
                    mAudioTrackLeft.play();
                    mAudioTrackLeft.setStereoVolume(1, 0);
                    if (mIsLoop) {
                        mAudioTrackLeft.setLoopPoints(0, mAudioDataLeft.length/8, -1);
                    }
                    Log.e(TAG, "AudioTrack Stream left play");
                    InputStream inputStream = null;
                    try {
                        inputStream = getAssets().open("priority3.wav");
                        int read;
                        while (mPlayingLeft) {
                            read = inputStream.read(mAudioDataLeft);
                            if (read > 0) {
                                mAudioTrackLeft.write(mAudioDataLeft, 0, read);
                            } else {
                                if (mIsLoop) {
                                    inputStream.reset();
                                }
                            }
                        }
                    } catch (RuntimeException | IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                new Thread(() -> {
                    mPlayingRight = true;
                    mAudioTrackRight.play();
                    mAudioTrackRight.setStereoVolume(0, 1);
                    if (mIsLoop) {
                        mAudioTrackRight.setLoopPoints(0, mAudioDataRight.length/8, -1);
                    }
                    Log.e(TAG, "AudioTrack Stream right play");
                    InputStream inputStream = null;
                    try {
                        inputStream = getAssets().open("priority2.wav");
                        int read;
                        while (mPlayingRight) {
                            read = inputStream.read(mAudioDataRight);
                            if (read > 0) {
                                mAudioTrackRight.write(mAudioDataRight, 0, read);
                            } else {
                                if (mIsLoop) {
                                    inputStream.reset();
                                }
                            }
                        }
                    } catch (RuntimeException | IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                mAudioTrack = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat, Math.max(minBufferSize, STREAM_BUFFER_SIZE), mode);
                Log.e(TAG, "AudioTrack Stream create");
                new Thread(() -> {
                    mPlaying = true;
                    mAudioTrack.play();
                    mAudioTrack.setStereoVolume(1, 1);
                    if (mIsLoop) {
                        mAudioTrack.setLoopPoints(0, mAudioData.length/8, -1);
                    }
                    InputStream inputStream = null;
                    try {
                        inputStream = getAssets().open("priority3.wav");
                        int read;
                        while (mPlaying) {
                            read = inputStream.read(mAudioData);
                            if (read > 0) {
                                mAudioTrack.write(mAudioData, 0, read);
                            } else {
                                if (mIsLoop) {
                                    inputStream.reset();
                                }
                            }
                        }
                    } catch (RuntimeException | IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private void stopAudio() {
        if (mIsStereo) {
            if (mAudioTrackLeft != null) {
                mPlayingLeft = false;
                mAudioTrackLeft.stop();
                Log.e(TAG, "AudioTrack left stop");
            }
            if (mAudioTrackRight != null) {
                mPlayingRight = false;
                mAudioTrackRight.stop();
                Log.e(TAG, "AudioTrack right stop");
            }
        } else {
            if (mAudioTrack != null) {
                mPlaying = false;
                mAudioTrack.stop();
                Log.e(TAG, "AudioTrack stop");
            }
        }
    }

    private void pauseAudio() {
        if (mIsStereo) {
            if (mAudioTrackLeft != null) {
                mPlayingLeft = false;
                mAudioTrackLeft.pause();
                Log.e(TAG, "AudioTrack left pause");
            }
            if (mAudioTrackRight != null) {
                mPlayingRight = false;
                mAudioTrackRight.pause();
                Log.e(TAG, "AudioTrack right pause");
            }
        } else {
            if (mAudioTrack != null) {
                mPlaying = false;
                mAudioTrack.pause();
                Log.e(TAG, "AudioTrack pause");
            }
        }
    }

    private void releaseAudio() {
        if (mIsStereo) {
            if (mAudioTrackLeft != null) {
                mPlayingLeft = false;
                mAudioTrackLeft.stop();
                mAudioTrackLeft.release();
                Log.e(TAG, "AudioTrack left release");
            }
            if (mAudioTrackRight != null) {
                mPlayingRight = false;
                mAudioTrackRight.stop();
                mAudioTrackRight.release();
                Log.e(TAG, "AudioTrack right release");
            }
        } else {
            if (mAudioTrack != null) {
                mPlaying = false;
                mAudioTrack.stop();
                mAudioTrack.release();
                Log.e(TAG, "AudioTrack release");
            }
        }
    }


}