package com.example.verifonevx990app.database;


public interface ResponseMessage {
    void onResponseError(ISOPackageReader data);

    void onSuccessMessage(ISOPackageReader data);//
}
