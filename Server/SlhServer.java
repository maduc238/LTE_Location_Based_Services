package org.example.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggerFactory;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.ResultCode;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Stack;
import org.jdiameter.api.URI;
import org.jdiameter.api.slh.ServerSLhSession;
import org.jdiameter.api.slh.events.LCSRoutingInfoAnswer;
import org.jdiameter.api.slh.events.LCSRoutingInfoRequest;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.common.impl.app.slh.LCSRoutingInfoAnswerImpl;
import org.jdiameter.common.impl.app.slh.SLhSessionFactoryImpl;
import org.jdiameter.server.impl.app.slh.SLhServerSessionImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.jdiameter.client.impl.parser.MessageParser;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.AvpDataException;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.api.Peer;
import org.jdiameter.api.IllegalDiameterStateException;


public class SlhServer extends SLhSessionFactoryImpl implements EventListener<Request, Answer>, NetworkReqListener {
    // tao log
    private static final Logger log = Logger.getLogger(SlhServer.class);
    static{

        configLog4j();
    }
    private ApplicationId slhAppId = ApplicationId.createByAuthAppId(10415L,16777291L);
    private Stack stack = null; // Application nao cung can co stack
    private static final Object[] EMPTY_ARRAY = new Object[]{};

    private HashMap<String, Long> accounts = new HashMap<String, Long>();
    private HashMap<String, Long> reserved = new HashMap<String, Long>();
    String imsi = "452041234567813";
    String mmeRealm = "mme.localdomain";

    public static void main(String[] args) throws Exception {
        new SlhServer();
    }

    //Constructor
    public SlhServer() throws Exception {
        super();
        // Get dictionary from file
        AvpDictionary.INSTANCE.parseDictionary(this.getClass().getClassLoader().getResourceAsStream("org/example/client/dictionary.xml"));

        // // lay thong tin subcriber tu file
        // Properties properties = new Properties();
        // try {
        //   InputStream is = this.getClass().getClassLoader().getResourceAsStream("accounts.properties");
        //   if (is == null) {
        //     throw new IOException("InputStream is null");
        //   }
        //   properties.load(is);
        //   for (Object property : properties.keySet()) {
        //     String imsi = (String) property;
        //     String info = properties.getProperty(imsi,  "0");
        //     if (log.isInfoEnabled()) {
        //       log.info("IMSI: '" + imsi + "' Info [" + info + "].");
        //     }
        //     accounts.put(imsi, Long.valueOf(info));
        //   }
        // }
        // catch (IOException e) {
        //   System.err.println("Failed to read 'accounts.properties' file. Aborting.");
        //   System.exit(-1);
        // }

        try {
            // Get stack config from file
            InputStream config = this.getClass().getClassLoader().getResourceAsStream("org/example/server/slh_config.xml");
            Configuration configuration = new XMLConfiguration(config);
            config.close();

            //Khai bao stack implement
            stack = new StackImpl();
            //Init stack
            this.stack.init(configuration);
            Thread.sleep(500);
            //Network create
            Network network = stack.unwrap(Network.class);
            // Add a listener to listen RIR from network
            network.addNetworkReqListener(this, this.slhAppId);
        } catch (Exception e) {
            e.printStackTrace();
            if (this.stack != null) {
              stack.destroy();
            }
          }

        // Start to running stack
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

        sessionFactory = (ISessionFactory)stack.getSessionFactory();
        this.init(sessionFactory);

        sessionFactory.registerAppFacory(ServerSLhSession.class, this);
    }

    //Cau hinh log4j de ghi log 
    private static void configLog4j() {
        InputStream inStreamLog4j = ExampleServer.class.getClassLoader().getResourceAsStream("log4j.properties");
        Properties propertiesLog4j = new Properties();
        try {
          propertiesLog4j.load(inStreamLog4j);
          PropertyConfigurator.configure(propertiesLog4j);
        } catch (Exception e) {
          e.printStackTrace();
        }
      
        log.debug("log4j configured");
      
    }
    
    //In logo
    private void printLogo() {
        if (log.isInfoEnabled()) {
          Properties sysProps = System.getProperties();
    
          String osLine = sysProps.getProperty("os.name") + "/" + sysProps.getProperty("os.arch");
          String javaLine = sysProps.getProperty("java.vm.vendor") + " " + sysProps.getProperty("java.vm.name") + " " + sysProps.getProperty("java.vm.version");
    
          Peer localPeer = stack.getMetaData().getLocalPeer();
    
          String diameterLine = localPeer.getProductName() + " (" +  localPeer.getUri() + " @ " + localPeer.getRealmName() + ")";
    
          log.info("===============================================================================");
          log.info("");
          log.info("== Test Slh server (" + osLine + ")" );
          log.info("== " + javaLine);
          log.info("");
          log.info("== " + diameterLine);
          log.info("");
          log.info("===============================================================================");
        }
    }

    //Xu ly request
    @Override
    public Answer processRequest(Request request) {
      if (log.isInfoEnabled()) {
        log.info("<< Received Request [" + request + "]");
      }
      try {
        // tao slh session tu sessionfactory
        SLhServerSessionImpl session =
            (sessionFactory).getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(10415L, 16777291L ), ServerSLhSession.class, EMPTY_ARRAY);
        // session nay se chiu trach nhien xu ly request
        session.processRequest(request);
      }
      catch (InternalException e) {
        log.error(">< Failure handling received request.", e);
      }
  
      return null;
    }

    public void receivedSuccessMessage(Request request, Answer answer) {
        if (log.isInfoEnabled()) {
          log.info("<< Received Success Message for Request [" + request + "] and Answer [" + answer + "]");
        }
    }

    @Override
    public void timeoutExpired(Request request) {
      if (log.isInfoEnabled()) {
        log.info("<< Received Timeout for Request [" + request + "]");
      }
    }

    // Xac dinh cac hanh dong can thuc hien khi gap ban tin RIR 
    @Override
    public void doLCSRoutingInfoRequestEvent(ServerSLhSession appSession, LCSRoutingInfoRequest request)  
         throws InternalException {
        //Dau tien lay ra cac AVP cua request
        AvpSet rirAvpSet = request.getMessage().getAvps();
        
        if (log.isInfoEnabled()) {
            log.info("<< Received Routing Info Request >>");
        }
        // khai bao RIA
        LCSRoutingInfoAnswer ria = null;

        //Lay ra imsi cua subcriber
        try {
            String imsi = rirAvpSet.getAvp(Avp.USER_NAME).getUTF8String();
            System.out.println(imsi + "*********************************************************" + this.imsi);
            
            if(imsi != null && imsi.equals(this.imsi)) {
                ria = createLCSRoutingInfoAnswer(request, ResultCode.SUCCESS);

                AvpSet servingNode = ria.getMessage().getAvps().addGroupedAvp(Avp.SERVING_NODE, 10415, true, false);
                // them URI cua MME ung voi imsi cua subcriber
                servingNode.addAvp(Avp.MME_NAME,this.mmeRealm,10415, true, false, false);
            }
            else {
                // 6.3.3.1 DIAMETER_ERROR_USER_UNKNOWN (5001) 
                // This result code shall be sent by the HSS to indicate that 
                // the user identified by the IMSI or the MSISDN is unknown. 
                // This error code is defined in 3GPP TS 29.229 [8] 
                ria = createLCSRoutingInfoAnswer(request, 5001);
                ria.getMessage().setError(true);
            }
        } catch (AvpDataException e) {
            e.printStackTrace();
        }
        try {
        appSession.sendLCSRoutingInfoAnswer(ria);
        } catch (Exception e) {
            log.error(e);
        }
    }
    //tao ban tin RIA theo RIR nhan duoc va resultcode
    private LCSRoutingInfoAnswer createLCSRoutingInfoAnswer(LCSRoutingInfoRequest request, long resultCode) throws InternalException, AvpDataException{
        
        LCSRoutingInfoAnswerImpl answer = new LCSRoutingInfoAnswerImpl((Request) request.getMessage(), resultCode);

        // < LCS-Routing-Info-Answer> ::= < Diameter Header: 8388622, PXY, 16777291 > 
        //  < Session-Id > 
        //  [ Result-Code ] 
        // [ Experimental-Result ] 
        // { Auth-Session-State } 
        // { Origin-Host } 
        // { Origin-Realm } 
        // *[ Supported-Features ] 
        // [ User-Name ] 
        // [ MSISDN ] 
        // [ LMSI ] 
        // [ Serving-Node ] 
        // *[ Additional-Serving-Node ] 
        // [ GMLC-Address ] 
        // [ PPR-Address ] 
        // *[ AVP ] 
        // *[ Failed-AVP ] 
        //  *[ Proxy-Info ] 
        // *[ Route-Record ]

        if (log.isInfoEnabled()) {
            log.info(">> Created Credit-Control-Answer.");
        }
      
        return answer;
    }
}
