package energy.py.p2pconnect;

import androidx.annotation.NonNull;

public interface IP2pOnSuccessListener<TResult> {
    void onSuccess(@NonNull TResult result);
}
