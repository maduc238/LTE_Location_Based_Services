package com.example.LCS;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class XmlDeliver {
    public static Document loadXMLFromString(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static String ReadXML(Document doc) {
        doc.getDocumentElement().normalize();
        
        String E_UTRANCellGlobalId = doc.getElementsByTagName("E-UTRANCellGlobalId").item(0).getTextContent();
        String TrackingAreaId = doc.getElementsByTagName("TrackingAreaId").item(0).getTextContent();
        String MMEName = doc.getElementsByTagName("MMEName").item(0).getTextContent();
        String AgeOfLocationInformation = doc.getElementsByTagName("AgeOfLocationInformation").item(0).getTextContent();
        String VisitedPLMNID = doc.getElementsByTagName("VisitedPLMNID").item(0).getTextContent();
        return "E-UTRAN Cell Global Id: "+E_UTRANCellGlobalId+" . Tracking Area Id: "+TrackingAreaId+" . MMEName: "+MMEName+" . Age Of Location Information: "+AgeOfLocationInformation+" . Visited PLMN ID: "+VisitedPLMNID;
    }
}
