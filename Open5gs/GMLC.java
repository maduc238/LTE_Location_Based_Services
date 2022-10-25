package org.example.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.StackType;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.diameter.dictionary.AvpRepresentation;
// import org.jdiameter.api.slh.events.LCSRoutingInfoRequest;

public class ExampleClient implements EventListener<Request, Answer> {

  private static final Logger log = Logger.getLogger(ExampleClient.class);
  static{
      //configure logging.
      configLog4j();
  }

  private static void configLog4j() {
    InputStream inStreamLog4j = ExampleClient.class.getClassLoader().getResourceAsStream("log4j.properties");
    Properties propertiesLog4j = new Properties();
    try {
      propertiesLog4j.load(inStreamLog4j);
      PropertyConfigurator.configure(propertiesLog4j);
    } catch (Exception e) {
      e.printStackTrace();
    }finally
    {
      if(inStreamLog4j!=null)
      {
        try {
          inStreamLog4j.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    log.debug("log4j configured");
  }

  //configuration files
  private static final String configFile = "org/example/client/client-jdiameter-config.xml";
  private static final String dictionaryFile = "org/example/client/dictionary.xml";
  //our destination
  private static final String serverHost = "127.0.0.8";
  private static final String serverPort = "3868";
  private static final String serverURI = "aaa://" + serverHost + ":" + serverPort;
  //our realm
  private static final String realmName = "localdomain";
  // definition of codes, IDs
  private static final int commandCode = 686;
  private static final long vendorID = 66666;
  private static final long applicationID = 16777216;
  // private ApplicationId authAppId = ApplicationId.createByAuthAppId(applicationID);
  private ApplicationId authAppId = ApplicationId.createByAuthAppId(10415, applicationID);
  private static final int exchangeTypeCode = 888;
  private static final int exchangeDataCode = 999;
  // enum values for Exchange-Type AVP
  private static final int EXCHANGE_TYPE_INITIAL = 0;
  private static final int EXCHANGE_TYPE_INTERMEDIATE = 1;
  private static final int EXCHANGE_TYPE_TERMINATING = 2;
  //list of data we want to exchange.
  private static final String[] TO_SEND = new String[] { "I want to get 3 answers", "This is second message", "Bye bye" };
  //Dictionary, for informational purposes.
  private AvpDictionary dictionary = AvpDictionary.INSTANCE;
  //stack and session factory
  private Stack stack;
  private SessionFactory factory;

  // ////////////////////////////////////////
  // Objects which will be used in action //
  // ////////////////////////////////////////
  private Session session;  // session used as handle for communication
  private int toSendIndex = 0;  //index in TO_SEND table
  private boolean finished = false;  //boolean telling if we finished our interaction

  private void initStack() {
    if (log.isInfoEnabled()) {
      log.info("Initializing Stack...");
    }
    InputStream is = null;
    try {
      //Parse dictionary, it is used for user friendly info.
      dictionary.parseDictionary(this.getClass().getClassLoader().getResourceAsStream(dictionaryFile));
      log.info("AVP Dictionary successfully parsed.");

      this.stack = new StackImpl();
      //Parse stack configuration
      is = this.getClass().getClassLoader().getResourceAsStream(configFile);
      Configuration config = new XMLConfiguration(is);
      factory = stack.init(config);
      if (log.isInfoEnabled()) {
        log.info("Stack Configuration successfully loaded.");
      }
      //Print info about applicatio
      Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

      log.info("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
      for (org.jdiameter.api.ApplicationId x : appIds) {
        log.info("Diameter Stack  :: Common :: " + x);
      }
      is.close();
      //Register network req listener, even though we wont receive requests
      //this has to be done to inform stack that we support application
      Network network = stack.unwrap(Network.class);
      network.addNetworkReqListener(new NetworkReqListener() {

        @Override
        public Answer processRequest(Request request) {
          //this wontbe called.
          return null;
        }
      }, this.authAppId); //passing our example app id.

    } catch (Exception e) {
      e.printStackTrace();
      if (this.stack != null) {
        this.stack.destroy();
      }

      if (is != null) {
        try {
          is.close();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }
      return;
    }

    MetaData metaData = stack.getMetaData();
    //ignore for now.
    if (metaData.getStackType() != StackType.TYPE_SERVER || metaData.getMinorVersion() <= 0) {
      stack.destroy();
      if (log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        log.error("Incorrect driver");
      }
      return;
    }

    try {
      if (log.isInfoEnabled()) {
        log.info("Starting stack");
      }
      stack.start();
      if (log.isInfoEnabled()) {
        log.info("Stack is running.");
      }
    } catch (Exception e) {
      e.printStackTrace();
      stack.destroy();
      return;
    }
    if (log.isInfoEnabled()) {
      log.info("Stack initialization successfully completed.");
    }
  }

  /**
   * @return
   */
  private boolean finished() {
    return this.finished;
  }

  /**
   *
   */
  private void start() {
    try {
      //wait for connection to peer
      try {
        Thread.currentThread();
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      //do send
      this.session = this.factory.getNewSession("gmlc.localdomain;" + System.currentTimeMillis() + ";1;app_s6a");
      sendNextRequest(EXCHANGE_TYPE_INITIAL);
    } catch (InternalException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalDiameterStateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RouteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (OverloadException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private void sendNextRequest(int enumType) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    Request r = this.session.createRequest(8388622, this.authAppId, "localdomain", "hss.localdomain");
    AvpSet requestAvps = r.getAvps();

    // Auth-Session-State
    requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);
    // User-Name
    requestAvps.addAvp(Avp.USER_NAME, "452041234567813", true, false, false);
    // MSISDN
    String number = "840987654321";
    String cd = "";
    for(int i=0; i < number.length()/2;i++){
        // System.out.println(Integer.decode("0x4d2"));
        int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
        cd += Character.toString((char)temp);
        // System.out.println(str);
    }
    requestAvps.addAvp(Avp.MSISDN, cd, 10415, true, false, true);
    // GMLC-Number
    requestAvps.addAvp(Avp.GMLC_NUMBER, "1", 10415, true, false, true);

    // Vendor-Specific-Application-Id
    AvpSet vendor_spec = requestAvps.addGroupedAvp(Avp.VENDOR_SPECIFIC_APPLICATION_ID, true, false);
    // Auth-Application-Id
    vendor_spec.addAvp(requestAvps.getAvp(Avp.AUTH_APPLICATION_ID));
    vendor_spec.addAvp(Avp.VENDOR_ID, 10415, true, false);
    

    // requestAvps.removeAvp(258);

    // send
    this.session.send(r, this);
    dumpMessage(r,true); //dump info on console
  }

  /*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter
   * .api.Message, org.jdiameter.api.Message)
   */
  @Override
  public void receivedSuccessMessage(Request request, Answer answer) {
    dumpMessage(answer,false);
    if (answer.getCommandCode() != commandCode) {
      log.error("Received bad answer: " + answer.getCommandCode());
      return;
    }
    AvpSet answerAvpSet = answer.getAvps();

    Avp exchangeTypeAvp = answerAvpSet.getAvp(exchangeTypeCode, vendorID);
    Avp exchangeDataAvp = answerAvpSet.getAvp(exchangeDataCode, vendorID);
    Avp resultAvp = answer.getResultCode();


    try {
      //for bad formatted request.
      if (resultAvp.getUnsigned32() == 5005 || resultAvp.getUnsigned32() == 5004) {
        // missing || bad value of avp
        this.session.release();
        this.session = null;
        log.error("Something wrong happened at server side!");
        finished = true;
      }
      switch ((int) exchangeTypeAvp.getUnsigned32()) {
      case EXCHANGE_TYPE_INITIAL:
        // JIC check;
        String data = exchangeDataAvp.getUTF8String();
        if (data.equals(TO_SEND[toSendIndex - 1])) {
          // ok :) send next;
          sendNextRequest(EXCHANGE_TYPE_INTERMEDIATE);
        } else {
          log.error("Received wrong Exchange-Data: " + data);
        }
        break;
      case EXCHANGE_TYPE_INTERMEDIATE:
        // JIC check;
        data = exchangeDataAvp.getUTF8String();
        if (data.equals(TO_SEND[toSendIndex - 1])) {
          // ok :) send next;
          sendNextRequest(EXCHANGE_TYPE_TERMINATING);
        } else {
          log.error("Received wrong Exchange-Data: " + data);
        }
        break;
      case EXCHANGE_TYPE_TERMINATING:
        data = exchangeDataAvp.getUTF8String();
        if (data.equals(TO_SEND[toSendIndex - 1])) {
          // good, we reached end of FSM.
          finished = true;
          // release session and its resources.
          this.session.release();
          this.session = null;
        } else {
          log.error("Received wrong Exchange-Data: " + data);
        }
        break;
      default:
        log.error("Bad value of Exchange-Type avp: " + exchangeTypeAvp.getUnsigned32());
        break;
      }
    } catch (AvpDataException e) {
      // thrown when interpretation of byte[] fails
      e.printStackTrace();
    } catch (InternalException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalDiameterStateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RouteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (OverloadException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.EventListener#timeoutExpired(org.jdiameter.api.
   * Message)
   */
  @Override
  public void timeoutExpired(Request request) {


  }

  private void dumpMessage(Message message, boolean sending) {
    if (log.isInfoEnabled()) {
      log.info((sending?"Sending ":"Received ") + (message.isRequest() ? "Request: " : "Answer: ") + message.getCommandCode() + "\nE2E:"
          + message.getEndToEndIdentifier() + "\nHBH:" + message.getHopByHopIdentifier() + "\nAppID:" + message.getApplicationId());
      log.info("AVPS["+message.getAvps().size()+"]: \n");
      try {
        printAvps(message.getAvps());
      } catch (AvpDataException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void printAvps(AvpSet avpSet) throws AvpDataException {
    printAvpsAux(avpSet, 0);
  }

  /**
   * Prints the AVPs present in an AvpSet with a specified 'tab' level
   *
   * @param avpSet
   *            the AvpSet containing the AVPs to be printed
   * @param level
   *            an int representing the number of 'tabs' to make a pretty
   *            print
   * @throws AvpDataException
   */
  private void printAvpsAux(AvpSet avpSet, int level) throws AvpDataException {
    String prefix = "                      ".substring(0, level * 2);

    for (Avp avp : avpSet) {
      AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(avp.getCode(), avp.getVendorId());

      if (avpRep != null && avpRep.getType().equals("Grouped")) {
        log.info(prefix + "<avp name=\"" + avpRep.getName() + "\" code=\"" + avp.getCode() + "\" vendor=\"" + avp.getVendorId() + "\">");
        printAvpsAux(avp.getGrouped(), level + 1);
        log.info(prefix + "</avp>");
      } else if (avpRep != null) {
        String value = "";

        if (avpRep.getType().equals("Integer32"))
          value = String.valueOf(avp.getInteger32());
        else if (avpRep.getType().equals("Integer64") || avpRep.getType().equals("Unsigned64"))
          value = String.valueOf(avp.getInteger64());
        else if (avpRep.getType().equals("Unsigned32"))
          value = String.valueOf(avp.getUnsigned32());
        else if (avpRep.getType().equals("Float32"))
          value = String.valueOf(avp.getFloat32());
        else
          //value = avp.getOctetString();
          value = new String(avp.getOctetString(), StandardCharsets.UTF_8);

        log.info(prefix + "<avp name=\"" + avpRep.getName() + "\" code=\"" + avp.getCode() + "\" vendor=\"" + avp.getVendorId()
            + "\" value=\"" + value + "\" />");
      }
    }
  }

  public static void main(String[] args) {
    ExampleClient ec = new ExampleClient();
    ec.initStack();
    ec.start();

    while (!ec.finished()) {
      try {
        Thread.currentThread();
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

}
