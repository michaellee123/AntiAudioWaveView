package dog.abcd.demo;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import dog.abcd.antilib.widget.AntiAudioWaveSurfaceView;

public class MainActivity extends Activity {

    AntiAudioWaveSurfaceView antiAudioWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        antiAudioWaveView = findViewById(R.id.antiAudioWaveView);
        thread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        antiAudioWaveView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        antiAudioWaveView.stop();
    }

    Thread thread = new Thread() {
        @Override
        public void run() {
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    10 * AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            );
            audioRecord.startRecording();
            while (!isInterrupted()) {
                short[] data = new short[1600];
                int length = audioRecord.read(data, 0, data.length);
                if (length > 0) {
                    antiAudioWaveView.putAudioData(data);
                }
            }
        }
    };

}
