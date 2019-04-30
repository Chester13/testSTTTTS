package chess.teststttts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

    private TextView mTTS_status;
    private Locale mTTS_lang = null;
    private EditText mTTS_content;

    // STT service
    private SpeechRecognizer mSR = null;
    private STT_Listener mSTT_listener = new STT_Listener();

    // TTS service
    private TextToSpeech mTTS = null;
    private TTS_OnInitListener mTTS_onInitListener = new TTS_OnInitListener();
    private TTS_UtteranceProgressListener mTTS_progressListener = new TTS_UtteranceProgressListener();


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

                case R.id.navigation_hide:
                    findViewById(R.id.navigation).setVisibility(View.GONE);
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

        // Release TTS.
        if (null != mTTS) {
            mTTS.shutdown();
            mTTS = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(R.string.title_exit);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }


    // To setup views
    private void initViews() {
        // The Navigation Buttons
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        if (hasNavigationBar(this)) {
            // There has a native navigation bar, we hide ourselves one.
            navigation.setVisibility(View.GONE);
        }

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
        spinnerSTTlang.setOnItemSelectedListener(mSpinner_Listener);

        // Will update STT result on this EditText view
        mSTT_result = findViewById(R.id.stt_result);

        // -------------------------------------------------------------------
        // TTS language spinner
        Spinner spinnerTTSlang = findViewById(R.id.spinner_tts);
        String[] tts_langs = {
                getString(R.string.system_default),
                Locale.TAIWAN.toString(),
                Locale.US.toString(),
                Locale.JAPAN.toString(),
                Locale.GERMANY.toString()
        };
        ArrayAdapter<String> ttsLangList = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tts_langs);
        spinnerTTSlang.setAdapter(ttsLangList);
        spinnerTTSlang.setOnItemSelectedListener(mSpinner_Listener);

        // TTS status sequence:
        //   uninitiated -> initialed -> speaking
        //   -> stopped -> speaking -> stopped -> speaking -> stopped -> ...
        mTTS_status = findViewById(R.id.tts_status);
        mTTS_status.setText(R.string.uninitiated);

        mTTS_content = findViewById(R.id.tts_content);
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
                    findViewById(R.id.button_stt_cancel).setEnabled(true);
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
                    if(((CheckBox)findViewById(R.id.checkbox_enable_partial)).isChecked()) {
                        // Enable partial result.
                        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    }

                    mSR.setRecognitionListener(mSTT_listener);
                    mSR.startListening(intent);
                    mSTT_status.setText(R.string.listening);
                }
                break;

            case R.id.button_stt_cancel:
                if (DBG) Log.d(TAG, "onClick, stt cancel");
                if (null != mSR) {
                    mSR.stopListening();
                    mSR.cancel();
                    mSTT_status.setText(R.string.canceled);
                }
                break;

            case R.id.button_stt_stop:
                if (DBG) Log.d(TAG, "onClick, stt stop");
                if (null != mSR) {
                    mSR.destroy();
                    mSTT_status.setText(R.string.stopped);
                }
                break;

            case R.id.button_tts_init:
                if (DBG) Log.d(TAG, "onClick, tts init");
                if (null == mTTS) {
                    // Create and wait for OnInitListener callback.
                    mTTS = new TextToSpeech(this, mTTS_onInitListener);
                }
                break;

            case R.id.button_tts_speak:
                if (DBG) Log.d(TAG, "onClick, tts speak, mTTS_lang="+mTTS_lang);
                if (null != mTTS) {
                    // Set speech rate and pitch.
                    float speechRate =
                            Float.valueOf(((EditText)findViewById(R.id.speech_rate_value)).getText().toString());
                    float pitch =
                            Float.valueOf(((EditText)findViewById(R.id.pitch_value)).getText().toString());
                    if (DBG) Log.d(TAG, "onClick, tts speak, speechRate="+speechRate+", pitch="+pitch);
                    mTTS.setSpeechRate(speechRate);
                    mTTS.setPitch(pitch);

                    // Start speaking.
                    mTTS.speak(mTTS_content.getText(), TextToSpeech.QUEUE_ADD, null, TAG);

                    // Update TTS status.
                    mTTS_status.setText(R.string.speaking);
                } else {
                    Log.w(TAG, "TTS is null or still speaking!!");
                }
                break;

            case R.id.button_tts_stop:
                if (DBG) Log.d(TAG, "onClick, tts stop");
                if (null != mTTS) {
                    mTTS.stop();
                    mTTS_status.setText(R.string.stopped);
                }
                break;

            default:
                break;
        }
    }

    AdapterView.OnItemSelectedListener mSpinner_Listener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                                   View view, int pos, long id) {
            final String selected_str = parent.getItemAtPosition(pos).toString();
            switch (parent.getId()) {
                case R.id.spinner_stt:
                    {
                        // Update mSTT_lang.
                        if (getString(R.string.system_default).equals(selected_str)) {
                            mSTT_lang = null;
                        } else {
                            mSTT_lang = selected_str;
                        }
                        if (DBG) Log.d(TAG, "STT lang selected: "+selected_str+", mSTT_lang="+mSTT_lang);
                    }
                    break;

                case R.id.spinner_tts:
                    {
                        // Update mTTS_lang.
                        if (getString(R.string.system_default).equals(selected_str)) {
                            mTTS_lang = Locale.getDefault();
                        } else {
                            switch (selected_str) {
                                case "zh_TW":
                                    mTTS_lang = Locale.CHINESE;
                                    break;

                                case "en_US":
                                    mTTS_lang = Locale.ENGLISH;
                                    break;

                                case "ja_JP":
                                    mTTS_lang = Locale.JAPANESE;
                                    break;

                                case "de_DE":
                                    mTTS_lang = Locale.GERMAN;
                                    break;

                                default:
                                    Log.w(TAG, "Not found matched lang, use default.");
                                    mTTS_lang = Locale.getDefault();
                                    break;
                            }
                        }
                        if (DBG) Log.d(TAG, "TTS lang selected: "+selected_str+", mTTS_lang="+mTTS_lang);

                        if (null != mTTS_lang && null != mTTS) {
                            // Use desired TTS language if user selected one.
                            mTTS.setLanguage(mTTS_lang);
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    };


    // STT progress listener
    class STT_Listener implements RecognitionListener {
        public void onReadyForSpeech(Bundle params) {
            if (DBG) Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {
            if (DBG) Log.d(TAG, "onBeginningOfSpeech");
        }

        public void onRmsChanged(float rmsdB) {
            //if (DBG) Log.d(TAG, "onRmsChanged, rmsdB="+rmsdB);
        }

        public void onBufferReceived(byte[] buffer) {
            if (DBG) Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            if (DBG) Log.d(TAG, "onEndofSpeech");
            updateStatus(mSTT_status, R.string.stopped);
        }

        public void onError(int error) {
            Log.e(TAG, "onError: " + error);
            updateStatus(mSTT_status, "Error "+error);
        }

        public void onResults(Bundle results) {
            //if (DBG) Log.d(TAG, "onResults");
            try {
                // Get STT result and update it to UI.
                ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (null != data) {
                    String str = data.get(0).toString();
                    mSTT_result.setText(str);
                    if (DBG) Log.d(TAG, "onResults, str="+str);
                }
            } catch (Exception e) {
                Log.e(TAG, "got exception, e="+e);
            }
        }

        public void onPartialResults(Bundle partialResults) {
            //if (DBG) Log.d(TAG, "onPartialResults");
            try {
                // Get STT result and update it to UI.
                ArrayList data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (null != data) {
                    String str = data.get(0).toString();
                    mSTT_result.setText(str);
                    if (DBG) Log.d(TAG, "onPartialResults, str="+str);
                }
            } catch (Exception e) {
                Log.e(TAG, "got exception, e="+e);
            }
        }

        public void onEvent(int eventType, Bundle params) {
            if (DBG) Log.d(TAG, "onEvent " + eventType);
        }
    }

    // TTS initialization listener
    class TTS_OnInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            if (TextToSpeech.SUCCESS == status) {
                updateStatus(mTTS_status, R.string.initialed);

                // To tracking TTS progress.
                mTTS.setOnUtteranceProgressListener(mTTS_progressListener);

                // We disable init button because only need one initialization,
                // and then enable Start & Stop buttons.
                findViewById(R.id.button_tts_init).setEnabled(false);
                findViewById(R.id.button_tts_speak).setEnabled(true);
                findViewById(R.id.button_tts_stop).setEnabled(true);
            } else {
                Log.e(TAG, "Fail to init TTS!! status="+status);
                updateStatus(mTTS_status, "Init failed, status="+status);
            }
        }
    }

    // TTS progress listener
    class TTS_UtteranceProgressListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
            if (DBG) Log.d(TAG, "TTS_utterance, onStart, utteranceId="+utteranceId);
        }

        @Override
        public void onDone(String utteranceId) {
            if (DBG) Log.d(TAG, "TTS_utterance, onDone, utteranceId="+utteranceId);
            updateStatus(mTTS_status, R.string.stopped);
        }

        @Override
        public void onError(String utteranceId) {
            if (DBG) Log.d(TAG, "TTS_utterance, onError, utteranceId="+utteranceId);
            updateStatus(mTTS_status, "Error");
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            if (DBG) Log.d(TAG, "TTS_utterance, onError, utteranceId="+utteranceId+", errorCode="+errorCode);
            updateStatus(mTTS_status, "Error "+errorCode);
        }

        @Override
        public void onStop(String utteranceId, boolean interrupted) {
            if (DBG) Log.d(TAG, "TTS_utterance, onStop, utteranceId="+utteranceId+", interrupted="+interrupted);
        }
    }

    // To update status on UI thread.
    private void updateStatus(final TextView tv, final int resId) {
        updateStatus(tv, getString(resId));
    }

    // To update status on UI thread.
    private void updateStatus(final TextView tv, final String status) {
        runOnUiThread(new Runnable() {
            public void run() {
                tv.setText(status);
            }
        });
    }

    // Return true if there has native navigation bar.
    public boolean hasNavigationBar (Context context) {
        int id = context.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && context.getResources().getBoolean(id);
    }
}
