package com.example.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Peer;
import org.jdiameter.api.Request;
import org.jdiameter.api.ResultCode;
import org.jdiameter.api.sh.ServerShSession;
import org.jdiameter.api.sh.events.UserDataAnswer;
import org.jdiameter.api.sh.events.UserDataRequest;
import org.jdiameter.common.impl.app.sh.ShSessionFactoryImpl;
import org.jdiameter.common.impl.app.sh.UserDataAnswerImpl;
import org.jdiameter.server.impl.app.sh.ShServerSessionImpl;
import org.mobicents.diameter.dictionary.AvpDictionary;

import com.example.test.utilis.DatabaseManager;
import com.example.test.utilis.Printor;
import com.example.test.utilis.StackCreator;

public class ShServer extends ShSessionFactoryImpl implements NetworkReqListener{
    private DatabaseManager databaseMng;

    private StackCreator stackCreator;

    private AvpDictionary dictionary = AvpDictionary.INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(ShServer.class);

    private ApplicationId shAppId = ApplicationId.createByAuthAppId(10415L, 16777217L);


    public ShServer(DatabaseManager databaseMng, StackCreator stackCreator) throws Exception{
        super(stackCreator.getSessionFactory());

        try {
            dictionary.parseDictionary(this.getClass().getClassLoader().getResourceAsStream("dictionary.xml"));

            setDatabaseMng(databaseMng);
            setStackCreator(stackCreator);

            printLogo();
        } catch (Exception e) {
            logger.info("Fail to create Sh Server!");
        }
    }



    public void setDatabaseMng(DatabaseManager databaseMng) {
        this.databaseMng = databaseMng;
    }

    public void setStackCreator(StackCreator stackCreator) {
        this.stackCreator = stackCreator;
    }

    @Override
    public Answer processRequest(Request request) {
        if (logger.isInfoEnabled()) {
          logger.info("<< Received Request [" + request + "]");
        }
        try {
          // tao sh session tu sessionfactory
          ShServerSessionImpl session =
              (sessionFactory).getNewAppSession(request.getSessionId(), shAppId, ServerShSession.class,  (Object) null);
          // session nay se chiu trach nhien xu ly request
          session.processRequest(request);
        }
        catch (InternalException e) {
          logger.error(">< Failure handling received request.", e);
        }
    
        return null;
    }

    // Xac dinh cac hanh dong can thuc hien khi gap ban tin UDR 
    public void doUserDataRequestEvent(ServerShSession appSession, UserDataRequest request) throws InternalException {
        //Dau tien lay ra cac AVP cua request
        AvpSet udrAvpSet = request.getMessage().getAvps();
    
        if (logger.isInfoEnabled()) {
            logger.info("<< Received User Data Request >>");
        }

        //Lay ra msisdn cua subcriber
        try {
            String userID = udrAvpSet.getAvp(Avp.USER_IDENTITY,10415).getGrouped().getAvp(Avp.MSISDN, 10415).getDiameterIdentity();
            
            UserDataAnswer uda ;
            /** < User-Data-Answer > ::= < Diameter Header: 306, PXY, 16777217 >
                < Session-Id >
                { Vendor-Specific-Application-Id }
                [ Result-Code ]
                [ Experimental-Result ]
                { Auth-Session-State }
                { Origin-Host }
                { Origin-Realm }
                *[ Supported-Features ]
                [ Wildcarded-Public-Identity ]
                [ Wildcarded-IMPU ]
                [ User-Data ]
                *[ AVP ]
                *[ Failed-AVP ]
                *[ Proxy-Info ]
                *[ Route-Record ] 
            */
            if(userID != null && checkUserId(userID)) {
                uda = createUserDataAnswer(request, ResultCode.SUCCESS);
                AvpSet udaAvps = uda.getMessage().getAvps();

                // Lay user data tu file
                try {
                String userData = getUserData("/home/aothatday/jdiameter/examples/guide1/src/main/resources/org/example/server/user-data.xml");
                udaAvps.addAvp(Avp.USER_DATA_SH,userData, 10415, true, false, false);
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

        AvpSet ansAvp = answer.getMessage().getAvps();

        Avp authState = request.getMessage().getAvps().getAvp(Avp.AUTH_SESSION_STATE);
        if (authState != null) {
          ansAvp.addAvp(authState);
        }
        if (logger.isInfoEnabled()) {
            logger.info(">> Created User-Data-Answer.");
            Printor.printMessage(answer.getMessage());
          }
      
        return answer;
    }

    //LAY USER DATA TU XML FILE
    private String getUserData(String filePath) throws IOException {
           
            String userData;
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
            return userData;
    }


    private void printLogo() {
        if (logger.isInfoEnabled()) {
          Properties sysProps = System.getProperties();
    
          String osLine = sysProps.getProperty("os.name") + "/" + sysProps.getProperty("os.arch");
          String javaLine = sysProps.getProperty("java.vm.vendor") + " " + sysProps.getProperty("java.vm.name") + " " + sysProps.getProperty("java.vm.version");
    
          Peer localPeer = stackCreator.getMetaData().getLocalPeer();
    
          String diameterLine = localPeer.getProductName() + " (" +  localPeer.getUri() + " @ " + localPeer.getRealmName() + ")";
    
          logger.info("===============================================================================");
          logger.info("");
          logger.info("== Sh server testing (" + osLine + ")" );
          logger.info("");
          logger.info("== " + javaLine);
          logger.info("");
          logger.info("== " + diameterLine);
          logger.info("");
          logger.info("===============================================================================");
        }
      }

    private String magicConvert(String input) {
        StringBuffer output = new StringBuffer("");
        for(int i=0; i<input.length(); i++) {
        String temp = String.format("%02x ", (int)input.charAt(i));
        if (temp.charAt(0) == 'f') {
        output.append(temp.charAt(1));
        } else {
        output.append(temp.charAt(1));
        output.append(temp.charAt(0));
        }
        }
        return output.toString();
    }

    private boolean checkUserId(String reqUserId) {
        return databaseMng.checkValue(magicConvert(reqUserId));
    }
}
