
package org.example.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

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
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.ResultCode;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Stack;
import org.jdiameter.api.URI;
import org.jdiameter.api.sh.ServerShSession;
import org.jdiameter.api.sh.events.ProfileUpdateRequest;
import org.jdiameter.api.sh.events.PushNotificationAnswer;
import org.jdiameter.api.sh.events.PushNotificationRequest;
import org.jdiameter.api.sh.events.SubscribeNotificationsRequest;
import org.jdiameter.api.sh.events.UserDataAnswer;
import org.jdiameter.api.sh.events.UserDataRequest;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.common.impl.app.sh.UserDataAnswerImpl;
import org.jdiameter.common.impl.app.sh.UserDataRequestImpl;
import org.jdiameter.common.impl.app.sh.ProfileUpdateAnswerImpl;
import org.jdiameter.common.impl.app.sh.ProfileUpdateRequestImpl;
import org.jdiameter.common.impl.app.sh.PushNotificationAnswerImpl;
import org.jdiameter.common.impl.app.sh.PushNotificationRequestImpl;
import org.jdiameter.common.impl.app.sh.ShSessionFactoryImpl;
import org.jdiameter.common.impl.app.sh.SubscribeNotificationsAnswerImpl;
import org.jdiameter.common.impl.app.sh.SubscribeNotificationsRequestImpl;
import org.jdiameter.server.impl.app.sh.ShServerSessionImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.AvpDataException;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.api.Peer;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.sh.ServerShSessionListener;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.common.api.app.sh.IShMessageFactory;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;


public class ShServer implements ServerShSessionListener, IShMessageFactory, NetworkReqListener, EventListener<Request, Answer> {
    // tao log
    private static final Logger log = Logger.getLogger(ShServer.class);
    static{

        configLog4j();
    }
    private ApplicationId shAppId = ApplicationId.createByAuthAppId(10415, 16777217);
    private Stack stack = null; // Application nao cung can co stack
    private ShSessionFactoryImpl shSessionFactory;
    protected SessionFactory factory;
    private static final Object[] EMPTY_ARRAY = new Object[]{};

    String msisdn = "84976643224";
    String userData = null;

    public static void main(String[] args) throws Exception {
        ShServer server = new ShServer();
        server.stackRun();
    }

    //Constructor
    public ShServer() {

    }
    
    public void stackRun() throws Exception {
        // Get dictionary from file
        AvpDictionary.INSTANCE.parseDictionary(this.getClass().getClassLoader().getResourceAsStream("org/example/client/dictionary.xml"));

        try {
            // Get stack config from file
            InputStream config = this.getClass().getClassLoader().getResourceAsStream("org/example/server/LCS_server_config.xml");
            Configuration configuration = new XMLConfiguration(config);
            config.close();

            //Khai bao stack implement
            stack = new StackImpl();
            
            //Init stack
            factory = stack.init(configuration);
            
            Thread.sleep(500);

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

        this.shSessionFactory = new ShSessionFactoryImpl(this.factory);
        this.shSessionFactory.setServerShSessionListener(this);
        
        //Network create
        Network network = stack.unwrap(Network.class);
        // Add a listener to listen UDR from network
        network.addNetworkReqListener(this, shAppId);

        ((ISessionFactory) this.factory).registerAppFacory(ServerShSession.class, this.shSessionFactory);
    }

    //Cau hinh log4j de ghi log 
    private static void configLog4j() {
        InputStream inStreamLog4j = ShServer.class.getClassLoader().getResourceAsStream("log4j.properties");
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
          log.info("== Test Sh server (" + osLine + ")" );
          log.info("== " + javaLine);
          log.info("");
          log.info("== " + diameterLine);
          log.info("");
          log.info("===============================================================================");
        }
    }

    //Xu ly request
    
    public Answer processRequest(Request request) {
      if (log.isInfoEnabled()) {
        log.info("<< Received Request [" + request + "]");
      }
      try {
        // tao sh session tu sessionfactory
        ShServerSessionImpl session =
            ( (ISessionFactory) factory).getNewAppSession(request.getSessionId(), shAppId, ServerShSession.class, null);
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

    
    public void timeoutExpired(Request request) {
      if (log.isInfoEnabled()) {
        log.info("<< Received Timeout for Request [" + request + "]");
      }
    }

    // Xac dinh cac hanh dong can thuc hien khi gap ban tin UDR 
    
    public void doUserDataRequestEvent(ServerShSession appSession, UserDataRequest request)  
         throws InternalException {
        //Dau tien lay ra cac AVP cua request
        AvpSet udrAvpSet = request.getMessage().getAvps();
        System.out.println("****************************"+ udrAvpSet);
        
        if (log.isInfoEnabled()) {
            log.info("<< Received User Data Request >>");
        }

        //Lay ra msisdn cua subcriber
        try {
            String userID = udrAvpSet.getAvp(Avp.USER_IDENTITY,10415).getUTF8String();
            System.out.println(udrAvpSet.getAvps(Avp.USER_IDENTITY,10415)+"*****************" +userID + "*********************************************************" + this.msisdn);
            
            UserDataAnswer uda ;
            if(userID != null && userID.equals(magicConvert(this.msisdn))) {
                uda = createUserDataAnswer(request, ResultCode.SUCCESS);
                AvpSet udaAvps = uda.getMessage().getAvps();

                // Lay user data tu file
                try {
                getUserData("org/example/server/user-data.xml");
                udaAvps.addAvp(702,userData, 10415L, true, false, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                uda = createUserDataAnswer(request, 5001);
                uda.getMessage().setError(true);
            }

            //gui UDA di
            appSession.sendUserDataAnswer(uda);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //tao ban tin UDA theo UDR nhan duoc va resultcode
    private UserDataAnswer createUserDataAnswer(UserDataRequest request, long resultCode) throws InternalException, AvpDataException{
        
        UserDataAnswerImpl answer = new UserDataAnswerImpl((Request) request.getMessage(), resultCode);

        if (log.isInfoEnabled()) {
            log.info(">> Created User-Data-Answer.");
        }
      
        return answer;
    }

    //LAY USER DATA TU XML FILE
    private void getUserData(String filePath) throws IOException {
           
            File xmlFile = new File(filePath);
        
            Reader fileReader = new FileReader(xmlFile);
            BufferedReader bufReader = new BufferedReader(fileReader);
            
            StringBuilder sb = new StringBuilder();
            String line = bufReader.readLine();
            while( line != null){
                sb.append(line).append("\n");
                line = bufReader.readLine();
            }
            userData = sb.toString();
            bufReader.close();
    
    }

    public void doOtherEvent(AppSession arg0, AppRequestEvent arg1, AppAnswerEvent arg2)
    throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
  // TODO Auto-generated method stub
    }

    public void doProfileUpdateRequestEvent(ServerShSession arg0, ProfileUpdateRequest arg1)
    throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
  // TODO Auto-generated method stub
    }

    
    public void doPushNotificationAnswerEvent(ServerShSession arg0, PushNotificationRequest arg1,
      PushNotificationAnswer arg2)
          throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
            
    }
    
    public void doSubscribeNotificationsRequestEvent(ServerShSession appSession, SubscribeNotificationsRequest request)
    throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    }

    public void stateChanged(Enum arg0, Enum arg1) {
        if (log.isDebugEnabled()) {
          log.debug("State changed from[" + arg0 + "] to[" + arg1 + "]");
    
        }
    
      }
    
    public void stateChanged(AppSession source, Enum arg0, Enum arg1) {
    this.stateChanged(arg0, arg1);
    }

    public AppAnswerEvent createProfileUpdateAnswer(Answer answer) {
    return new ProfileUpdateAnswerImpl(answer);
    }

    public AppRequestEvent createProfileUpdateRequest(Request request) {
    return new ProfileUpdateRequestImpl(request);
    }

    public AppAnswerEvent createPushNotificationAnswer(Answer answer) {
    return new PushNotificationAnswerImpl(answer);
    }

    public AppRequestEvent createPushNotificationRequest(Request request) {
    return new PushNotificationRequestImpl(request);
    }

    public AppAnswerEvent createSubscribeNotificationsAnswer(Answer answer) {
    return new SubscribeNotificationsAnswerImpl(answer);
    }

    public AppRequestEvent createSubscribeNotificationsRequest(Request request) {
    return new SubscribeNotificationsRequestImpl(request);
    }

    public AppAnswerEvent createUserDataAnswer(Answer answer) {
    return new UserDataAnswerImpl(answer);
    }

    public AppRequestEvent createUserDataRequest(Request request) {
    return new UserDataRequestImpl(request);
    }
    public long getMessageTimeout() {
        // TODO Auto-generated method stub
        return 5000;
    }

    public long getApplicationId() {
        return this.shAppId.getAuthAppId();
      }

      private String magicConvert(String a) {
        String cd = "";
        int len = a.length();
        if (a.length() % 2 != 0) {
            a = a + "f";
            len ++;
        }
        for(int i=0; i < len/2;i++){
            int temp = Integer.decode("0x"+a.charAt(2*i+1)+a.charAt(2*i));
            cd += Character.toString((char)temp);
        }
        return cd;
    }
}
