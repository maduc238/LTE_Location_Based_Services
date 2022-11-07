package com.example.LCS;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

@Controller
public class AppController implements org.jdiameter.api.EventListener<Request, Answer> {
    List<Todo> todoList = new CopyOnWriteArrayList<>();
    List<Logging> loggingList = new CopyOnWriteArrayList<>();
    
    private static final String configFile = "config.xml";
	private static final long applicationID_HSS = 16777217;
	private ApplicationId authAppId_HSS = ApplicationId.createByAuthAppId(VENDOR_ID, applicationID_HSS);
	private static final String realmName = "ims.mnc004.mcc452.3gppnetwork.org";

	private static final long VENDOR_ID = 10415;
	// private static String MSISDN = "84976643224";
	private Stack stack;
	private SessionFactory factory;

	public org.jdiameter.api.Session session;
	// private boolean finished = false;

    @PostConstruct
    public void postConstruct() {
        initStack();
        start();
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
			LogMessage.addLogging(loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Nhận được answer với Command Code sai! Command Code: "+answerCommandCode);
        } else {
			LogMessage.addLogging(loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Nhận được bản tin answer. Command Code: "+answerCommandCode);
		}

		try {
			long resultCode = resultAvp.getUnsigned32();
			switch ((int) resultCode) {
				case 2001:
					LogMessage.addLogging(loggingList, "DIAMETER_SUCCESS", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 4100:
					LogMessage.addLogging(loggingList, "DIAMETER_USER_DATA_NOT_AVAILABLE", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 4101:
					LogMessage.addLogging(loggingList, "DIAMETER_PRIOR_UPDATE_IN_PROGRESS", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 4201:
					LogMessage.addLogging(loggingList, "The location of the targeted user is not known at this time to satisfy the requested operation!", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5001:
					LogMessage.addLogging(loggingList, "Không xác định được IMSI hoặc MSISDN của người dùng!", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5002:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_IDENTITIES_DONT_MATCH", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;	
				case 5004:
					LogMessage.addLogging(loggingList, "Có vấn đề với HSS!", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5005:
					LogMessage.addLogging(loggingList, "Có vấn đề với HSS!", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5006:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_SUBS_DATA_ABSENT", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5007:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_NO_SUBSCRIPTION_TO_DATA", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5008:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_TOO_MUCH_DATA", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5011:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_FEATURE_UNSUPPORTED", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5100:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_NOT_RECOGNIZED", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5101:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_OPERATION_NOT_ALLOWED", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5102:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_READ", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5103:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_MODIFIED", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5104:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_NOTIFIED", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5105:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_TRANSPARENT_DATA_OUT_OF_SYNC", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5108:
					LogMessage.addLogging(loggingList, "DIAMETER_ERROR_DSAI_NOT_AVAILABLE", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				case 5490:
					LogMessage.addLogging(loggingList, "The requesting GMLC's network is not authorized to request UE location information!", answer.getHopByHopIdentifier(), answer.getEndToEndIdentifier(), resultCode);
					break;
				
			}
			
			Avp ShUserData = resultAvps.getAvp(702, 10415);
			if(ShUserData != null) {
				System.out.println(ShUserData.getUTF8String());
				LogMessage.addLogging(loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Dữ liệu XML nhận được: "+ShUserData.getUTF8String());
			} else {
				LogMessage.addLogging(loggingList, "Hop by Hop Identifier: "+answer.getHopByHopIdentifier()+". End to End Identifier: "+answer.getEndToEndIdentifier()+". Không nhận được AVP Code 702 Sh-User-Data");
			}
			// this.session.release();
			// this.session = null;
			// finished = true;

		} catch (AvpDataException e) {
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
			LogMessage.addLogRequest(loggingList, request.getHopByHopIdentifier(), request.getEndToEndIdentifier(), MSISDN);
		} catch (InternalException e) {
			e.printStackTrace();
		} catch (IllegalDiameterStateException e) {
			e.printStackTrace();
		} catch (RouteException e) {
			e.printStackTrace();
		} catch (OverloadException e) {
			e.printStackTrace();
		}
	}

	// Web *************************************************************

    @GetMapping("/listTodo")
    public String index(Model model, @RequestParam(value = "limit", required = false) Integer limit) {
        model.addAttribute("todoList", limit != null ? todoList.subList(0, limit) : todoList);
        return "listTodo";
    }

    @GetMapping("/addrequest")
    public String addrequest(Model model) {
        model.addAttribute("todo", new Todo());
        return "addrequest";
    }

    @PostMapping("/addrequest")
    public String addrequest(@ModelAttribute Todo todo) {
        todoList.add(todo);
		sending(todo.getMSISDN());
        return "success";
    }

    @GetMapping("/logging")
    public String logging(Model model) {
		model.addAttribute("loggingList", loggingList);
        return "logging";
    }
}