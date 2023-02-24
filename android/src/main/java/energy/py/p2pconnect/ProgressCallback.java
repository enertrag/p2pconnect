package energy.py.p2pconnect;

public interface ProgressCallback {

    /**
     * Informs the progress action about changes.
     *
     * @param title the progress title to set. Null requires the listener to keep the current value.
     * @param progress the progress in the range from 0 to 100. A negative value (<0) requires the listening action to finish.
     */
    void updateProgress(String title, int progress);

}
