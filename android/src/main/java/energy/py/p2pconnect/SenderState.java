package energy.py.p2pconnect;

public enum SenderState {

    /** Not yet started */
    NONE,
    /** Opening browser activity */
    START_BROWSING,
    /** Browsing: waiting for peers to detect */
    BROWSING,
    /** Opening transfer progress dialog */
    START_CONNECTING,
    /** Connecting to peer */
    CONNECTING,
    /** Connected: Sent version and waiting for answer */
    WAITING_FOR_VERSION,
    /** Connected: Sent transfer id and waiting for answer */
    WAITING_FOR_TRANSFER_ID,
    /** Connected: Sent number of resources and waiting for answer */
    WAITING_FOR_COUNT,
    /** Connected: Sent ids of resources and waiting for answer */
    WAITING_FOR_ID,
    /** Connected: Sending resources */
    TRANSFERRING_RESOURCES,
    /**
     * Done: waiting for receiver to commit.
     *
     * I observed an error when the transmitter
     * disconnected right after the transmission was completed.
     * In this case, the transmission hung at 99% at
     * the receiver and was not completed.
     */
    WAITING_FOR_RECEIVER


}
