package org.example.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Stack;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

public class GMLC implements EventListener<Request, Answer>{
	private static final String configFile = "org/example/client/client-jdiameter-config.xml";
	private static final String dictionaryFile = "org/example/client/dictionary.xml";
	//our destination
	private static final long applicationID_HSS = 16777216;
	private static final long applicationID_MME = 16777251;
	// private ApplicationId authAppId = ApplicationId.createByAuthAppId(applicationID);
	private ApplicationId authAppId_HSS = ApplicationId.createByAuthAppId(10415, applicationID_HSS);
	private ApplicationId authAppId_MME = ApplicationId.createByAuthAppId(10415, applicationID_MME);
	//our realm
	private static final String realmName = "localdomain";
	//stack and session factory
	private Stack stack;
	private SessionFactory factory;
	private Session session;
	private boolean finished = false;
	
	private void initStack() {
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
			network.addNetworkReqListener(new NetworkReqListener() {
				@Override
				public Answer processRequest(Request request) {
					//this wontbe called.
					return null;
				}
			}, this.authAppId_MME);
			

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

	private boolean finished() {
		return this.finished;
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
			this.session = this.factory.getNewSession("gmlc.localdomain;" + System.currentTimeMillis() + ";1;app_lcs");
			sendRIR();
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

	/**
	 * Send LCS-Routing_Info Request
	 * @throws InternalException
	 * @throws IllegalDiameterStateException
	 * @throws RouteException
	 * @throws OverloadException
	 */
	private void sendRIR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		Request r = this.session.createRequest(8388622, this.authAppId_HSS, "localdomain", "hss.localdomain");
		AvpSet requestAvps = r.getAvps();
	
		// Auth-Session-State
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

		/************************************************************************************************************/
		// User-Name
		requestAvps.addAvp(Avp.USER_NAME, "452041234567813", true, false, false);

		/************************************************************************************************************/
		// MSISDN
		String number = "840987654321";
		String cd = "";
		for(int i=0; i < number.length()/2;i++){
			int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
			cd += Character.toString((char)temp);
		}
		requestAvps.addAvp(Avp.MSISDN, cd, 10415, true, false, true);

		/************************************************************************************************************/
		// GMLC-Number
		requestAvps.addAvp(Avp.GMLC_NUMBER, "1", 10415, true, false, true);
		
		// requestAvps.removeAvp(258);
		
		// send
		this.session.send(r, this);
		// Ok
	}

	/**
	 * Send Provide-Location Request
	 * @throws InternalException
	 * @throws IllegalDiameterStateException
	 * @throws RouteException
	 * @throws OverloadException
	 */
	private void sendPLR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		Request r = this.session.createRequest(8388620, this.authAppId_MME, "localdomain", "mme.localdomain");
		AvpSet requestAvps = r.getAvps();
	
		// Auth-Session-State
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);
		
		/************************************************************************************************************/
		// User-Name
		requestAvps.addAvp(Avp.USER_NAME, "452041234567813", true, false, false);
		
		/************************************************************************************************************/
		// MSISDN
		String number = "840987654321";
		String cd = "";
		for(int i=0; i < number.length()/2;i++){
			int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
			cd += Character.toString((char)temp);
		}
		requestAvps.addAvp(Avp.MSISDN, cd, 10415, true, false, true);
		
		/************************************************************************************************************/
		// SLg-Location-Type
			/**
			 * 0 CURRENT_LOCATION
			 * 1 CURRENT_OR_LAST_KNOW_LOCATION
			 * 2 INITIAL_LOCATION
			 * 3 ACTIVATE_DEFERRED_LOCATION
			 * 4 CANCEL_DEFERRED_LOCATION
			 * 5 NOTIFICATION_VERIFICATION_ONLY
			 */
		requestAvps.addAvp(Avp.SLG_LOCATION_TYPE, 1, 10415, true, false);
		
		/************************************************************************************************************/
		// IMEI
		requestAvps.addAvp(Avp.TGPP_IMEI, "123456789012345", 10415, true, false, false);

		/************************************************************************************************************/
		// LCS-EPS-Client-Name
		AvpSet client_name = requestAvps.addGroupedAvp(Avp.LCS_EPS_CLIENT_NAME, 10415, true, false);
			client_name.addAvp(Avp.LCS_NAME_STRING, "https://aovl.com.vn", 10415, true, false, false);
				/**
				 * Format of the LCS Client name
				 * 0 LOGICAL_NAME
				 * 1 EMAIL_ADDRESS
				 * 2 MSISDN
				 * 3 URL
				 * 4 SIP_URL
				 */
			client_name.addAvp(Avp.LCS_FORMAT_INDICATOR, 3, 10415, true, false);

		/************************************************************************************************************/
		// LCS-Client-Type
			/**
			 * Type of services requested by the LCS Client
			 * 0 EMERGENCY_SERVICES
			 * 1 VALUE_ADDED_SERVICES
			 * 2 PLMN_OPERATOR_SERVICES
			 * 3 LAWFUL_INTERCEPT_SERVICES
			 */
		requestAvps.addAvp(Avp.LCS_CLIENT_TYPE, 1, 10415, true, false);

		/************************************************************************************************************/
		// LCS-Requestor-Name
		AvpSet requestor_name = requestAvps.addGroupedAvp(Avp.LCS_REQUESTOR_NAME, 10415, true, false);
			// LCS_Requestor-Id-String
				/**
				 * Contain the identification of the Requestor and can be e.g. MSISDN or logical name
				 */
			requestor_name.addAvp(Avp.LCS_REQUESTOR_ID_STRING, "https://aothatday.com", 10415, true, false, false);
			
			// LCS-Format-Indicator
				/**
				 * 0 LOGICAL_NAME
				 * 1 EMAIL_ADDRESS
				 * 2 MSISDN
				 * 3 URL
				 * 4 SIP_URL
				 */
			requestor_name.addAvp(Avp.LCS_FORMAT_INDICATOR, 3, 10415, true, false);
		
		/************************************************************************************************************/
		// LCS-Priority
		/**
		 * Indicate the priority of the location request.
		 * 0 shall indicate the highest priority, 1 shall indicate normal priority
		 * All other values shall be treated as 1
		 */
		requestAvps.addAvp(Avp.LCS_PRIORITY, 1, 10415, true, false);

		/************************************************************************************************************/
		// LCS-QoS
		AvpSet qos = requestAvps.addGroupedAvp(Avp.LCS_QOS, 10415, true, false);
			// LCS-QoS-Class
			/**
			 * 0 ASSURED
			 * 1 BEST_EFFORT
			 */
			qos.addAvp(Avp.LCS_QOS_CLASS, 1, 10415, true, false);
			// Horizontal-Accuracy
			/**
			 * Bits 6-0 corresponds to Uncertainty Code in 3GPP TS 23.032
			 * Error should be less than the error indicated by the uncertainty code with 67% confidence
			 * Bit 7 to 31 shall be ignored
			 */
			qos.addAvp(Avp.HORIZONTAL_ACCURACY, 1, 10415, true, false);
			// Vertical-Accuracy
			qos.addAvp(Avp.VERTICAL_ACCURACY, 1, 10415, true, false);
			// Vertical-Requested
			/**
			 * 0 VERTICAL_COORDINATE_IS_NOT_REQUESTED
			 * 1 VERTICAL_COORDINATE_IS_REQUESTED
			 */
			qos.addAvp(Avp.VERTICAL_REQUESTED, 0, 10415, true, false);
			// Response-Time
			/**
			 * 0 LOW_DELAY
			 * 1 DELAY_TOLERANT
			 */
			qos.addAvp(Avp.RESPONSE_TIME, 0, 10415, true, false);

		/************************************************************************************************************/
		// Velocity-Requested
		/**
		 * 0 VELOCITY_IS_NOT_REQUESTED
		 * 1 VELOCITY_IS_REQUESTED
		 */
		requestAvps.addAvp(Avp.VELOCITY_REQUESTED, 0, 10415, true, false);

		/************************************************************************************************************/
		// LCS-Supported-GAD-Shapes
		/**
		 * It shall contain bitmask.
		 * A node shall mark in the BIT STRING all Shaped
		 * Bit 8-0 in shall indicate the supported. Bits 9 to 31 shall be igored
		 */
		requestAvps.addAvp(Avp.LCS_SUPPORTED_GAD_SHAPES, 1, 10415, true, false);

		/************************************************************************************************************/
		// LCS-Service-Type-Id

		/************************************************************************************************************/
		// LCS-Codeword
		/**
		 * Indicates the potential codeword string to send in a notification message to UE
		 */
		requestAvps.addAvp(Avp.LCS_CODEWORD, "hello", 10415, true, false, false);

		/************************************************************************************************************/
		// LCS-Privacy-Check-Non-Session
		AvpSet privacy_non_session = requestAvps.addGroupedAvp(Avp.LCS_PRIVACY_CHECK_NON_SESSION, 10415, true, false);
			/**
			 * 0 ALLOWED_WITHOUT_NOTIFICATION
			 */
		privacy_non_session.addAvp(Avp.LCS_PRIVACY_CHECK, 0, 10415, true, false);
			

		/************************************************************************************************************/
		// LCS-Privacy-Check-Session
		AvpSet privacy_session = requestAvps.addGroupedAvp(Avp.LCS_PRIVACY_CHECK_SESSION, 10415, true, false);
			/**
			 * 4 NOT_ALLOWED
			 */
		privacy_session.addAvp(Avp.LCS_PRIVACY_CHECK, 4, 10415, true, false);

		/************************************************************************************************************/
		// Service-Selection

		/************************************************************************************************************/
		// Deferred-Location-Type

		/************************************************************************************************************/
		// LCS-Reference-Number

		/************************************************************************************************************/
		// Area-Event-Info

		/************************************************************************************************************/
		// GMLC-Address

		/************************************************************************************************************/
		// PLR-Flags

		/************************************************************************************************************/
		// Periodic-LDR-Information

		/************************************************************************************************************/
		// Reporting-PLMN-List

		/************************************************************************************************************/
		// Motion-Event-Info


		// send
		this.session.send(r, this);
		// Ok
	}
	
	int count = 0;

	@Override
	public void receivedSuccessMessage(Request request, Answer answer) {
		if (count < 1) {
			try {
				sendPLR();
			} catch (InternalException e) {
				e.printStackTrace();
			} catch (IllegalDiameterStateException e) {
				e.printStackTrace();
			} catch (RouteException e) {
				e.printStackTrace();
			} catch (OverloadException e) {
				e.printStackTrace();
			}
			count++;
		}
	}

	@Override
	public void timeoutExpired(Request request) {

	}

	public static void main(String[] args) {
		GMLC ec = new GMLC();
		ec.initStack();
		ec.start();

		while (!ec.finished()) {
			try {
				Thread.currentThread();
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
