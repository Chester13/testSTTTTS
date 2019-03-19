package chess.teststttts;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "testSTTTTS";
    private boolean DBG = true;

    private TextView mSTT_status;
    private String mSTT_lang = null;
    private EditText mSTT_result;
    private STT_Listener mSTT_listener = new STT_Listener();

    // STT service
    private SpeechRecognizer mSR = null;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_back:
                    onBackPressed();
                    return true;

                case R.id.navigation_home:
                    onHomePressed();
                    return true;

                case R.id.navigation_exit:
                    finish();
                    return true;
            }
            return false;
        }
    };

    public void onHomePressed() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure to release STT.
        if (null != mSR) {
            mSR.destroy();
            mSR = null;
        }
    }

    // To setup views
    private void initViews() {
        // The Navigation Buttons
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // STT status sequence:
        //   uninitiated -> initialed -> listening
        //   -> stopped -> listening -> stopped -> listening -> stopped -> ...
        mSTT_status = findViewById(R.id.stt_status);
        mSTT_status.setText(R.string.uninitiated);

        // STT language spinner
        Spinner spinnerSTTlang = findViewById(R.id.spinner_stt);
        String[] stt_langs = {
                getString(R.string.system_default),
                Locale.TAIWAN.toString(),
                Locale.US.toString(),
                Locale.JAPAN.toString(),
                Locale.GERMANY.toString()
        };
        ArrayAdapter<String> sttLangList = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, stt_langs);
        spinnerSTTlang.setAdapter(sttLangList);
        spinnerSTTlang.setOnItemSelectedListener(mSpinnerSTTlang_Listener);

        // Will update STT result on this EditText view
        mSTT_result = findViewById(R.id.stt_result);
    }

    // All buttons onClick listener
    public void onButtonClick(View v) {
        switch (v.getId()) {
            case R.id.button_stt_init:
                if (DBG) Log.d(TAG, "onClick, stt init");
                if (null == mSR) {
                    mSR = SpeechRecognizer.createSpeechRecognizer(this);
                    mSTT_status.setText(R.string.initialed);

                    // We disable init button because only need one initialization,
                    // and then enable Start & Stop buttons.
                    v.setEnabled(false);
                    findViewById(R.id.button_stt_start).setEnabled(true);
                    findViewById(R.id.button_stt_stop).setEnabled(true);
                }
                break;

            case R.id.button_stt_start:
                if (DBG) Log.d(TAG, "onClick, stt start, mSTT_lang="+mSTT_lang);
                if (null != mSR) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    // Change STT language if user selected.
                    if (null != mSTT_lang) {
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mSTT_lang);
                    }
                    mSR.setRecognitionListener(mSTT_listener);
                    mSR.startListening(intent);
                    mSTT_status.setText(R.string.listening);
                }
                break;

            case R.id.button_stt_stop:
                if (DBG) Log.d(TAG, "onClick, stt stop");
                if (null != mSR) {
                    mSR.destroy();
                    mSTT_status.setText(R.string.stopped);
                }
                break;

            default:
                break;
        }
    }

    AdapterView.OnItemSelectedListener mSpinnerSTTlang_Listener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                                   View view, int pos, long id) {
            final String selected_str = parent.getItemAtPosition(pos).toString();
            if (DBG) Log.d(TAG, "onSpinnerSTTlang selected: " + selected_str);

            // Update mSTT_lang.
            if (getString(R.string.system_default).equals(selected_str)) {
                mSTT_lang = null;
            } else {
                mSTT_lang = selected_str;
            }
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    };


    class STT_Listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params) {
            if (DBG) Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {
            if (DBG) Log.d(TAG, "onBeginningOfSpeech");
        }

        public void onRmsChanged(float rmsdB) {
            if (DBG) Log.d(TAG, "onRmsChanged");
        }

        public void onBufferReceived(byte[] buffer) {
            if (DBG) Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            if (DBG) Log.d(TAG, "onEndofSpeech");
            mSTT_status.setText(R.string.stopped);
        }

        public void onError(int error) {
            Log.e(TAG, "onError: " + error);
            mSTT_result.setText(error);
        }

        public void onResults(Bundle results)
        {
            try {
                // Get STT result and update it to UI.
                ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (null != data) {
                    String str = data.get(0).toString();
                    mSTT_result.setText(str);
                }
            } catch (Exception e) {
                Log.e(TAG, "got exception, e="+e);
            }
        }

        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }
    }
}
