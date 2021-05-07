package com.example.verifonevx990app.database;


/*called while get Responce from server */
public interface ServerResponce {
    void onSucees(byte[] responce);

    void onError(String throwable);

    void onResponseTimeOut();

    void onNoInternet();

    void onConnectionFailed();

    void saveReversal();
}
