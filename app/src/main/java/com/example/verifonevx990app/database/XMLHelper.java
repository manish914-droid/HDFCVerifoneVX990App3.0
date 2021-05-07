package com.example.verifonevx990app.database;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class XMLHelper extends DefaultHandler {
    private final InputStream inputStream;
    // private ArrayList<XmlFieldModel> posts = new ArrayList<>();
    private final HashMap<String, XmlFieldModel> xmlFieldModels = new HashMap<>();
    String TAG = "XMLHelper";
    Boolean currTag = false;
    String currTagVal = "";
    private XmlFieldModel post = null;

    public XMLHelper(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public HashMap<String, XmlFieldModel> getISOList() {
        return this.xmlFieldModels;
    }


    public void get() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser mSaxParser = factory.newSAXParser();
            XMLReader mXmlReader = mSaxParser.getXMLReader();
            mXmlReader.setContentHandler(this);
            //InputStream mInputStream = new URL(URL_MAIN).openStream();
            mXmlReader.parse(new InputSource(inputStream));
        } catch (Exception e) {
            // Exceptions can be handled for different types
            // But, this is about XML Parsing not about Exception Handling

            //    CustomToast.printAppLog("" + e.getMessage());
        }
    }

    // This method receives notification character data inside an element
    // e.g. <post_title>Add EditText Pragmatically - Android</post_title>
    // It will be called to read "Add EditText Pragmatically - Android"
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (currTag) {
            currTagVal = currTagVal + new String(ch, start, length);
            currTag = false;
        }
    }

    // Receives notification of end of element
    // e.g. <post_title>Add EditText Pragmatically - Android</post_title>
    // It will be called when </post_title> is encountered
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        currTag = false;


//        else if (localName.equalsIgnoreCase("post_date"))
//            post.setDate(currTagVal);

        if (localName.equalsIgnoreCase("isofield")) {
            xmlFieldModels.put(post.getId(), post);
        }
    }

    // Receives notification of start of an element
    // e.g. <post_title>Add EditText Pragmatically - Android</post_title>
    // It will be called when <post_title> is encountered
    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        Log.i(TAG, "TAG: " + localName);

        currTag = true;
        currTagVal = "";
        // Whenever <post> element is encountered it will create new object of PostValue
        if (localName.equals("isofield")) {
            post = new XmlFieldModel();
            //if (qName.equalsIgnoreCase("id"))
            post.setId(attributes.getValue("id"));

            // else if (qName.equalsIgnoreCase("name"))
            post.setName(attributes.getValue("name"));
        }

    }
}
