package com.bitmaster.obdii_wifi_collect.obdwifi.googleapis;

import android.os.AsyncTask;
import android.util.Log;

import com.bitmaster.obdii_wifi_collect.obdwifi.MainActivity;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by renet on 5/7/14.
 */
class RequestGoogleDirections extends AsyncTask<String, String, String> {

    private List<RouteStep> routeSteps = new ArrayList<RouteStep>();
    private MainActivity activity;

    public RequestGoogleDirections(MainActivity a) {
        this.activity = a;
    }

    protected List<RouteStep> getRouteSteps() {
        return routeSteps;
    }

    @Override
    protected String doInBackground(String... uri) {

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            this.activity.googleServiceRequestCompleted(e.getLocalizedMessage());
        } catch (IOException e) {
            this.activity.googleServiceRequestCompleted(e.getLocalizedMessage());
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        Document doc = getDomElement(result);
        Node status = doc.getElementsByTagName("status").item(0);
        if(!status.getTextContent().equalsIgnoreCase("OK")) {
            //status: FAIL
            this.activity.googleServiceRequestCompleted(status.getTextContent());
            return;
        }
        NodeList nl = doc.getElementsByTagName("step");
        // looping through all item nodes <item>
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            Element child;
            RouteStep routeStep = new RouteStep();

            child = (Element) e.getElementsByTagName("start_location").item(0);
            routeStep.setStartLat(getValue(child, "lat"));
            routeStep.setStartLng(getValue(child, "lng"));
            child = (Element) e.getElementsByTagName("end_location").item(0);
            routeStep.setEndLat(getValue(child, "lat"));
            routeStep.setEndLng(getValue(child, "lng"));
            child = (Element) e.getElementsByTagName("duration").item(0);
            routeStep.setDuration(getValue(child, "value"));
            routeStep.setInstructions(getValue(e, "html_instructions"));
            child = (Element) e.getElementsByTagName("distance").item(0);
            routeStep.setDistance(getValue(child, "value"));

            routeSteps.add(routeStep);
        }
        //status: OK
        this.activity.googleServiceRequestCompleted(status.getTextContent());
    }

    private Document getDomElement(String xml){
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {

            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);

        } catch (ParserConfigurationException e) {
            this.activity.googleServiceRequestCompleted(e.getLocalizedMessage());
            return null;
        } catch (SAXException e) {
            this.activity.googleServiceRequestCompleted(e.getLocalizedMessage());
            return null;
        } catch (IOException e) {
            this.activity.googleServiceRequestCompleted(e.getLocalizedMessage());
            return null;
        }
        // return DOM
        return doc;
    }
    private String getValue(Element item, String str) {
        NodeList n = item.getElementsByTagName(str);
        return this.getElementValue(n.item(0));
    }

    private final String getElementValue( Node elem ) {
        Node child;
        if( elem != null){
            if (elem.hasChildNodes()){
                for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
                    if( child.getNodeType() == Node.TEXT_NODE  ){
                        return child.getNodeValue();
                    }
                }
            }
        }
        return "";
    }
}
