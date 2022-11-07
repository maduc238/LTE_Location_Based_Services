package com.example.test.utilis;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Message;
import org.jdiameter.api.validation.AvpRepresentation;
import org.jdiameter.api.validation.Dictionary;
import org.jdiameter.common.impl.validation.DictionaryImpl;
import org.springframework.stereotype.Component;

@Component
public class Printor {
    
    private static Logger logger = Logger.getLogger(Printor.class);

    public static Dictionary AVP_DICTIONARY = DictionaryImpl.INSTANCE;

    public static String readFile(InputStream is) throws IOException {
      BufferedInputStream bin = new BufferedInputStream(is);
  
      byte[] contents = new byte[1024];
  
      int bytesRead = 0;
      String strFileContents;
      StringBuilder sb = new StringBuilder();
  
      while ( (bytesRead = bin.read(contents)) != -1) {
        strFileContents = new String(contents, 0, bytesRead);
        sb.append(strFileContents);
      }
  
      return sb.toString();
    }

    public static void printMessage(Message message) {
      String reqFlag = message.isRequest() ? "R" : "A";
      String flags = reqFlag += message.isError() ? " | E" : "";
  
      if (logger.isInfoEnabled()) {
        logger.info("Message [" + flags + "] Command-Code: " + message.getCommandCode() + " / E2E("
            + message.getEndToEndIdentifier() + ") / HbH(" + message.getHopByHopIdentifier() + ")");
        logger.info("- - - - - - - - - - - - - - - - AVPs - - - - - - - - - - - - - - - -");
        printAvps(message.getAvps());
      }
    }
  
    public static void printAvps(AvpSet avps) {
      printAvps(avps, "");
    }
  
    public static void printAvps(AvpSet avps, String indentation) {
      for (Avp avp : avps) {
        AvpRepresentation avpRep = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId());
        Object avpValue = null;
        boolean isGrouped = false;
  
        try {
          String avpType = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId()).getType();
  
          if ("Integer32".equals(avpType) || "AppId".equals(avpType)) {
            avpValue = avp.getInteger32();
          }
          else if ("Unsigned32".equals(avpType) || "VendorId".equals(avpType)) {
            avpValue = avp.getUnsigned32();
          }
          else if ("Float64".equals(avpType)) {
            avpValue = avp.getFloat64();
          }
          else if ("Integer64".equals(avpType)) {
            avpValue = avp.getInteger64();
          }
          else if ("Time".equals(avpType)) {
            avpValue = avp.getTime();
          }
          else if ("Unsigned64".equals(avpType)) {
            avpValue = avp.getUnsigned64();
          }
          else if ("Grouped".equals(avpType)) {
            avpValue = "<Grouped>";
            isGrouped = true;
          }
          else {
            avpValue = avp.getUTF8String().replaceAll("\r", "").replaceAll("\n", "");
          }
        }
        catch (Exception ignore) {
          try {
            avpValue = avp.getUTF8String().replaceAll("\r", "").replaceAll("\n", "");
          }
          catch (AvpDataException e) {
            avpValue = avp.toString();
          }
        }
  
        String avpLine = indentation + avp.getCode() + ": " + avpRep.getName();
        while (avpLine.length() < 50) {
          avpLine += avpLine.length() % 2 == 0 ? "." : " ";
        }
        avpLine += avpValue;
  
        logger.info(avpLine);
  
        if (isGrouped) {
          try {
            printAvps(avp.getGrouped(), indentation + "  ");
          }
          catch (AvpDataException e) {
            // Failed to ungroup... ignore then...
          }
        }
      }
    }
}
