package com.example.verifonevx990app.database;


import java.util.HashMap;

public interface OnXmlDataParsed {
    void onXmlSuccess(HashMap<String, XmlFieldModel> xmlFieldModels);

    void onXmlError(String message);
}
