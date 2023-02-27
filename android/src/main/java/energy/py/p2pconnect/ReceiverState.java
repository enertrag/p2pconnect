package energy.py.p2pconnect;

public enum ReceiverState {

    NONE,

    WAITING_FOR_CONNECT,
    CONNECTING,
    WAITING_FOR_VERSION,
    WAITING_FOR_TRANSFER_ID,
    WAITING_FOR_TRANSFER_ACCEPT,

    WAITING_FOR_COUNT,
    WAITING_FOR_ID,

    RECEIVING

}