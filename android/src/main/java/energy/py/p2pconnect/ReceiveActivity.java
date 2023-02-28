package energy.py.p2pconnect;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;


public class ReceiveActivity extends AppCompatActivity {

    private static final String TAG = "P2pConnect/ReceiveAcvty";

    private TextView _progressTitle;
    private TextView _infoTitle;
    private ProgressBar _progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_progress);

        String endpointId = getIntent().getExtras().getString("endpointId");

        _progressTitle = findViewById(R.id.progressTitle);
        _progressBar = findViewById(R.id.progressBar);
        _infoTitle = findViewById(R.id.infoTitle);

        _progressBar.setIndeterminate(true);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i("SendActivity", "back pressed");
            }
        });

        Receiver.getInstance().acceptEndpoint(this, endpointId, (title, progress, info) -> {

            runOnUiThread(() -> {
                if (progress < 0) {

                    Log.i(TAG, "Finishing activity (progress < 0)");
                    finish();
                    return;
                }

                if (_progressBar.isIndeterminate()) {
                    _progressBar.setIndeterminate(false);
                }

                if(info != null) {
                    _infoTitle.setText(info);
                }

                if (title != null) {
                    _progressTitle.setText(title);
                }
                _progressBar.setProgress(progress);
            });
        });
    }

}