package dog.abcd.demo;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import dog.abcd.antilib.widget.AntiAudioWaveView;

public class MainActivity extends Activity {

    AntiAudioWaveView antiAudioWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        antiAudioWaveView = findViewById(R.id.antiAudioWaveView);
        thread.start();
        antiAudioWaveView.startShow();
    }

    Thread thread = new Thread() {
        @Override
        public void run() {
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    10 * AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
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
