package energy.py.p2pconnect;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;


public class SendActivity extends AppCompatActivity {

    private static final String TAG = "P2pConnect/SendActivity";

    private TextView _progressTitle;
    private TextView _infoTitle;
    private ProgressBar _progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_progress);


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

        Sender.getInstance().startTransfer(this, (title, progress, info) -> {

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

    /*
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback { }
    }*/

}