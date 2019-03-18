package chess.teststttts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "testSTTTTS";
    private boolean DBG = true;

    private Spinner mSpinnerSTTlang;

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

    // To setup views
    private void initViews() {
        // The Navigation Buttons
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // STT language spinner
        mSpinnerSTTlang = findViewById(R.id.spinner_stt);
        ArrayAdapter<CharSequence> sttLangList = ArrayAdapter.createFromResource(MainActivity.this,
                R.array.stt_language,
                android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSTTlang.setAdapter(sttLangList);
        mSpinnerSTTlang.setOnItemSelectedListener(mSpinnerSTTlang_Listener);
    }

    // All buttons onClick listener
    public void onButtonClick(View v) {
        switch (v.getId()) {
            case R.id.button_stt_init:
                if (DBG) Log.d(TAG, "onClick, stt init");
                break;

            case R.id.button_stt_start:
                if (DBG) Log.d(TAG, "onClick, stt start");
                break;

            case R.id.button_stt_stop:
                if (DBG) Log.d(TAG, "onClick, stt stop");
                break;

            default:
                break;
        }
        Log.d(TAG, "onClick, mSpinnerSTTlang.getPrompt()="+mSpinnerSTTlang.getAutofillValue());
    }

    AdapterView.OnItemSelectedListener mSpinnerSTTlang_Listener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                                   View view, int pos, long id) {
            if (DBG) Log.d(TAG, "onSpinnerSTTlang selected: "+parent.getItemAtPosition(pos).toString());
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    };
}
