package com.example.LCS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.ParserConfigurationException;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class Diameter implements org.jdiameter.api.EventListener<Request, Answer> {
    private static final String configFile = "config.xml";
	private static final long applicationID_HSS = 16777217;
	private ApplicationId authAppId_HSS = ApplicationId.createByAuthAppId(VENDOR_ID, applicationID_HSS);
	private static final String realmName = "ims.mnc004.mcc452.3gppnetwork.org";

	private static final long VENDOR_ID = 10415;
	// private static String MSISDN = "84976643224";
	Stack stack;
	private SessionFactory factory;

	org.jdiameter.api.Session session;
	// private boolean finished = false;

    @Autowired
    private WebController controller;

    @PostConstruct
    public void postConstruct() {
        initStack();
        start();
    }

    @PreDestroy
    public void preDestroy() {
        this.stack.destroy();
        this.stack = null;
        this.factory = null;
        this.session.release();
        this.session = null;
    }

	void initStack() {
		InputStream is = null;
		System.out.println("Initializing Stack...");
		try{
			this.stack = new StackImpl();
			// Them config file
			is = this.getClass().getClassLoader().getResourceAsStream(configFile);
			Configuration config = new XMLConfiguration(is);
      		factory = stack.init(config);
			System.out.println("Stack Configuration successfully loaded.");

			Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();
			System.out.println("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
			for (org.jdiameter.api.ApplicationId x : appIds) {
				System.out.println("Diameter Stack  :: Common :: " + x);
			}
			is.close();

			//Register network req listener
			Network network = stack.unwrap(Network.class);
			network.addNetworkReqListener(new NetworkReqListener() {
				@Override
				public Answer processRequest(Request request) {
					//this wontbe called.
					return null;
				}
      		}, this.authAppId_HSS);

		} catch (Exception e){
			e.printStackTrace();
			if (this.stack != null) {
				this.stack.destroy();
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e1) {
                    LogMessage.addLogging(controller.loggingList, e1.getMessage());
					e1.printStackTrace();
				}
			}
			return;
		}
		try {
			System.out.println("Starting stack");
			stack.start();
			System.out.println("Stack is running.");
		} catch (Exception e) {
			e.printStackTrace();
			stack.destroy();
			return;
		}
		System.out.println("Stack initialization successfully completed.");

	}

	private void start() {
		try {
			//wait for connection to peer
			try {
			  	Thread.currentThread();
			  	Thread.sleep(5000);
			} catch (InterruptedException e) {
			  	e.printStackTrace();
			}
			//do send
			this.session = this.factory.getNewSession("lbsanm." + realmName + ";" + System.currentTimeMillis());
			// sendRequest();

		} catch (InternalException e) {
            LogMessage.addLogging(controller.loggingList, e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Just use for MSISDN
	 * @param a Number of MSISDN String
	 * @return Encoded String value
	 */
	public static String magicConvert(String a) {
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

	/* (non-Javadoc)
	 * @see org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter.api.Message, org.jdiameter.api.Message)
	 */
	@Override
	public void receivedSuccessMessage(Request request, Answer answer) {
        int answerCommandCode = answer.getCommandCode();
		AvpSet resultAvps = answer.getAvps();
		Avp resultAvp = answer.getResultCode();
		// answer.getSessionId();
        
        // Just LCS Application
        if (answerCommandCode != 306) {
            System.out.println("Received bad command code answer: " + answerCommandCode);
			LogMessage.addLogging(controller.loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Nhận được answer với Command Code sai! Command Code: "+answerCommandCode);
        } else {
			LogMessage.addLogging(controller.loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Nhận được bản tin answer. Command Code: "+answerCommandCode);
		}

		try {
			long resultCode = resultAvp.getUnsigned32();
			if (LogMessage.logResultCode(controller.loggingList, resultCode, answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier())) {
				Avp ShUserData = resultAvps.getAvp(702, 10415);	// Lấy AVP 702
				if(ShUserData != null) {
					// System.out.println(ShUserData.getDiameterIdentity());
					try {
						LogMessage.addLogging(controller.loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Nhận được dữ liệu XML. "+ XmlDeliver.ReadXML(XmlDeliver.loadXMLFromString(ShUserData.getDiameterIdentity())));
					} catch (ParserConfigurationException | SAXException | IOException e) {
						LogMessage.addLogging(controller.loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Nhận được dữ liệu XML nhưng không xử lý được");
						e.printStackTrace();
					}
				} else {
					LogMessage.addLogging(controller.loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Không nhận được AVP Code 702 Sh-User-Data");
				}
			}

		} catch (AvpDataException e) {
            LogMessage.addLogging(controller.loggingList, e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public void timeoutExpired(Request request) {

	}

    // @Autowired
	void sending(String MSISDN){
		try{
			Request request = this.session.createRequest(306, this.authAppId_HSS, realmName, "HSPD01.ims.mnc004.mcc452.3gppnetwork.org");
            request.setProxiable(true);
            AvpSet requestAvps = request.getAvps();
            
            requestAvps.removeAvp(Avp.DESTINATION_HOST);

            requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

            AvpSet userid = requestAvps.addGroupedAvp(Avp.USER_IDENTITY, VENDOR_ID, true, false);
            userid.addAvp(Avp.MSISDN, magicConvert(MSISDN), VENDOR_ID, true, false, true);

            /**
             * Requested-Domain AVP: type Enumerated. 
             * Indicates the access domain for which certain data (e.g. user state) are requested.
             *  0	CS-Domain		The requested data apply to the CS domain
             *  1	PS-Domain		The requested data apply to the PS domain
             */
            requestAvps.addAvp(Avp.REQUESTED_DOMAIN, 1, VENDOR_ID, true, false);

            /**
             * Requested-Nodes Avp: type Unsigned32 and it shall contain a bit mask
             * Bit	Name
             *  0	MME
             *  1	SGSN
             *  2	3GPP-AAA-SERVER_TWAN
             *  3	AMF
             * 
             */
            requestAvps.addAvp(713, 1, VENDOR_ID, false, false, true);
            
            /**
             * Current-Location Avp: type Enumerated
             *  0	DoNotNeedInitiateActiveLocationRetrieval
             *  1 	InitiateActiveLocationRetrieval
             */
            requestAvps.addAvp(Avp.CURRENT_LOCATION, 0, VENDOR_ID, true, false);

            /**
             * Data-Reference AVP: type Enumerated
             *  0	RepositoryData
             *  10	IMSPublicIdentity
             *  11	IMSUserState
             *  12	S-CSCFName
             *  13	InitialFilterCriteria
             * 	14 	LocationInformation ...
             */
            requestAvps.addAvp(Avp.DATA_REFERENCE, 14, VENDOR_ID, true, false, true);

            // Send request
			
            this.session.send(request, this);
			LogMessage.addLogRequest(controller.loggingList, request.getHopByHopIdentifier(), request.getEndToEndIdentifier(), MSISDN);
		} catch (InternalException e) {
            LogMessage.addLogging(controller.loggingList, e.getMessage());
			e.printStackTrace();
		} catch (IllegalDiameterStateException e) {
            LogMessage.addLogging(controller.loggingList, e.getMessage());
			e.printStackTrace();
		} catch (RouteException e) {
            LogMessage.addLogging(controller.loggingList, e.getMessage());
			e.printStackTrace();
		} catch (OverloadException e) {
            LogMessage.addLogging(controller.loggingList, e.getMessage());
			e.printStackTrace();
		}
	}

}
