package org.example.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Set;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

public class GMLC implements EventListener<Request, Answer>{
	private static final String configFile = "org/example/client/client-jdiameter-config.xml";
	private static final String dictionaryFile = "org/example/client/dictionary.xml";
	// our destination
	private static final long applicationID_HSS = 16777216;
	private static final long applicationID_MME = 16777251;
	// private ApplicationId authAppId = ApplicationId.createByAuthAppId(applicationID);
	private ApplicationId authAppId_HSS = ApplicationId.createByAuthAppId(VENDOR_ID, applicationID_HSS);
	private ApplicationId authAppId_MME = ApplicationId.createByAuthAppId(VENDOR_ID, applicationID_MME);
	// our realm
	private static final String realmName = "localdomain";
	// vendor ID
	private static final long VENDOR_ID = 10415;
	// stack and session factory
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
		// 1 .. 1
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

		/************************************************************************************************************/
		// User-Name
		// 0 .. 1
		requestAvps.addAvp(Avp.USER_NAME, "452041234567813", true, false, false);

		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
		String number = "840987654321";
		String cd = "";
		for(int i=0; i < number.length()/2;i++){
			int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
			cd += Character.toString((char)temp);
		}
		requestAvps.addAvp(Avp.MSISDN, cd, VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// GMLC-Number
		// 0 .. 1
		requestAvps.addAvp(Avp.GMLC_NUMBER, "1", VENDOR_ID, true, false, true);
		
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
		// 1 .. 1
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);
		
		/************************************************************************************************************/
		// User-Name
		// 0 .. 1
		requestAvps.addAvp(Avp.USER_NAME, "452041234567813", true, false, false);
		
		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
		String number = "840987654321";
		String cd = "";
		for(int i=0; i < number.length()/2;i++){
			int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
			cd += Character.toString((char)temp);
		}
		requestAvps.addAvp(Avp.MSISDN, cd, VENDOR_ID, true, false, true);
		
		/************************************************************************************************************/
		// SLg-Location-Type
		// 1 .. 1
			/**
			 * 0 CURRENT_LOCATION
			 * 1 CURRENT_OR_LAST_KNOW_LOCATION
			 * 2 INITIAL_LOCATION
			 * 3 ACTIVATE_DEFERRED_LOCATION
			 * 4 CANCEL_DEFERRED_LOCATION
			 * 5 NOTIFICATION_VERIFICATION_ONLY
			 */
		requestAvps.addAvp(Avp.SLG_LOCATION_TYPE, 1, VENDOR_ID, true, false);
		
		/************************************************************************************************************/
		// IMEI
		// 0 .. 1
		requestAvps.addAvp(Avp.TGPP_IMEI, "123456789012345", VENDOR_ID, true, false, false);

		/************************************************************************************************************/
		// LCS-EPS-Client-Name
		// 1 .. 1
		AvpSet client_name = requestAvps.addGroupedAvp(Avp.LCS_EPS_CLIENT_NAME, VENDOR_ID, true, false);
			client_name.addAvp(Avp.LCS_NAME_STRING, "https://aovl.com.vn", VENDOR_ID, true, false, false);
				/**
				 * Format of the LCS Client name
				 * 0 LOGICAL_NAME
				 * 1 EMAIL_ADDRESS
				 * 2 MSISDN
				 * 3 URL
				 * 4 SIP_URL
				 */
			client_name.addAvp(Avp.LCS_FORMAT_INDICATOR, 3, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-Client-Type
		// 1 .. 1
			/**
			 * Type of services requested by the LCS Client
			 * 0 EMERGENCY_SERVICES
			 * 1 VALUE_ADDED_SERVICES
			 * 2 PLMN_OPERATOR_SERVICES
			 * 3 LAWFUL_INTERCEPT_SERVICES
			 */
		requestAvps.addAvp(Avp.LCS_CLIENT_TYPE, 1, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-Requestor-Name
		// 0 .. 1
		AvpSet requestor_name = requestAvps.addGroupedAvp(Avp.LCS_REQUESTOR_NAME, VENDOR_ID, true, false);
			// LCS_Requestor-Id-String
				/**
				 * Contain the identification of the Requestor and can be e.g. MSISDN or logical name
				 */
			requestor_name.addAvp(Avp.LCS_REQUESTOR_ID_STRING, "https://aothatday.com", VENDOR_ID, true, false, false);
			
			// LCS-Format-Indicator
				/**
				 * 0 LOGICAL_NAME
				 * 1 EMAIL_ADDRESS
				 * 2 MSISDN
				 * 3 URL
				 * 4 SIP_URL
				 */
			requestor_name.addAvp(Avp.LCS_FORMAT_INDICATOR, 3, VENDOR_ID, true, false);
		
		/************************************************************************************************************/
		// LCS-Priority
		// 0 .. 1
		/**
		 * Indicate the priority of the location request.
		 * 0 shall indicate the highest priority, 1 shall indicate normal priority
		 * All other values shall be treated as 1
		 */
		requestAvps.addAvp(Avp.LCS_PRIORITY, 1, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-QoS
		// 0 .. 1
		AvpSet qos = requestAvps.addGroupedAvp(Avp.LCS_QOS, VENDOR_ID, true, false);
			// LCS-QoS-Class
			/**
			 * 0 ASSURED
			 * 1 BEST_EFFORT
			 */
			qos.addAvp(Avp.LCS_QOS_CLASS, 1, VENDOR_ID, true, false);
			// Horizontal-Accuracy
			/**
			 * Bits 6-0 corresponds to Uncertainty Code in 3GPP TS 23.032
			 * Error should be less than the error indicated by the uncertainty code with 67% confidence
			 * Bit 7 to 31 shall be ignored
			 */
			qos.addAvp(Avp.HORIZONTAL_ACCURACY, 1, VENDOR_ID, true, false);
			// Vertical-Accuracy
			qos.addAvp(Avp.VERTICAL_ACCURACY, 1, VENDOR_ID, true, false);
			// Vertical-Requested
			/**
			 * 0 VERTICAL_COORDINATE_IS_NOT_REQUESTED
			 * 1 VERTICAL_COORDINATE_IS_REQUESTED
			 */
			qos.addAvp(Avp.VERTICAL_REQUESTED, 0, VENDOR_ID, true, false);
			// Response-Time
			/**
			 * 0 LOW_DELAY
			 * 1 DELAY_TOLERANT
			 */
			qos.addAvp(Avp.RESPONSE_TIME, 0, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Velocity-Requested
		// 0 .. 1
		/**
		 * 0 VELOCITY_IS_NOT_REQUESTED
		 * 1 VELOCITY_IS_REQUESTED
		 */
		requestAvps.addAvp(Avp.VELOCITY_REQUESTED, 0, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-Supported-GAD-Shapes
		// 0 .. 1
		/**
		 * It shall contain bitmask.
		 * A node shall mark in the BIT STRING all Shaped
		 * Bit 8-0 in shall indicate the supported. Bits 9 to 31 shall be igored
		 */
		requestAvps.addAvp(Avp.LCS_SUPPORTED_GAD_SHAPES, 1, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-Service-Type-ID
		// 0 .. 1
		/**
		 * Define the identifier associated to one of the Service Type for which the LCS client
		 * is allowed to locate the particular UE
		 */
		requestAvps.addAvp(Avp.LCS_SERVICE_TYPE_ID, 1234, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-Codeword
		// 0 .. 1
		/**
		 * Indicates the potential codeword string to send in a notification message to UE
		 */
		requestAvps.addAvp(Avp.LCS_CODEWORD, "hello", VENDOR_ID, true, false, false);

		/************************************************************************************************************/
		// LCS-Privacy-Check-Non-Session
		// 0 .. 1
		AvpSet privacy_non_session = requestAvps.addGroupedAvp(Avp.LCS_PRIVACY_CHECK_NON_SESSION, VENDOR_ID, true, false);
			/**
			 * 0 ALLOWED_WITHOUT_NOTIFICATION
			 */
		privacy_non_session.addAvp(Avp.LCS_PRIVACY_CHECK, 0, VENDOR_ID, true, false);
			

		/************************************************************************************************************/
		// LCS-Privacy-Check-Session
		// 0 .. 1
		AvpSet privacy_session = requestAvps.addGroupedAvp(Avp.LCS_PRIVACY_CHECK_SESSION, VENDOR_ID, true, false);
			/**
			 * 4 NOT_ALLOWED
			 */
			privacy_session.addAvp(Avp.LCS_PRIVACY_CHECK, 4, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Service-Selection
		// 0 .. 1
		/**
		 * It is used to define the APN which the mobility service should be associated
		 */
		requestAvps.addAvp(Avp.SERVICE_SELECTION, "Service", VENDOR_ID, true, false, false);

		/************************************************************************************************************/
		// Deferred-Location-Type
		// 0 .. 1
		/**
		 * it shall contain a bit mask. Each bit indicates a type of 
		 * event, until when the location estimation is deferred
		 */
		requestAvps.addAvp(Avp.DEFERRED_LOCATION_TYPE, 1, VENDOR_ID, false, false);

		/************************************************************************************************************/
		// LCS-Reference-Number
		// 0 .. 1
		/**
		 * The reference number identifying the deferred location request
		 */
		requestAvps.addAvp(Avp.LCS_REFERENCE_NUMBER, "123456", VENDOR_ID, false, false, true);

		/************************************************************************************************************/
		// Area-Event-Info
		// 0 .. 1
		// AvpSet area_event = requestAvps.addGroupedAvp(Avp.AREA_EVENT_INFO, VENDOR_ID, false, false);
			// area_event.addAvp(Avp.OCCURRENCE_INFO);
			// area_event.addAvp(Avp.INTERVAL_TIME);
			// area_event.addAvp(Avp.MAXIMUM_INTERVAL);
			// area_event.addAvp(Avp.SAMPLING_INTERVAL);
			// area_event.addAvp(Avp.REPORTING_DURATION);
			// area_event.addAvp(Avp.REPORTING_LOCATION_REQUIREMENTS);

		/************************************************************************************************************/
		// GMLC-Address
		// 0 .. 1
		/**
		 * Contains the IPv4 or IPv6 address od H-GMLC or the V-GMLC associated with the serving node
		 */
		// requestAvps.addAvp(Avp.GMLC_ADDRESS, InetAddress.getByName("127.0.0.1"), VENDOR_ID, true, false);

		/************************************************************************************************************/
		// PLR-Flags
		// 0 .. 1
		/**
		 * It shall contain a bit mask
		 * 0 MO-LR-ShortCircuit-Indicator
		 * 1 Optimized-LCS-Proc-Req
		 * 2 Delayed-Location-Reporting-Support-Indicator
		 */
		// requestAvps.addAvp(Avp.PLR_FLAGS, 0, VENDOR_ID, false, false, false);

		/************************************************************************************************************/
		// Periodic-LDR-Info
		// 0 .. 1
		AvpSet periodic_ldr = requestAvps.addGroupedAvp(Avp.PERIODIC_LDR_INFORMATION, VENDOR_ID, false, false);
			/**
			 * Contains reporting frequency. Its minimum value shall be 1 and maximun value shall be 8639999
			 */
			periodic_ldr.addAvp(Avp.REPORTING_AMOUNT, 123, VENDOR_ID, false, false);
			/**
			 * Contains reporting interval in seconds. Its minimum value shall be 1 and maximun value shall be 8639999
			 */
			periodic_ldr.addAvp(Avp.REPORTING_INTERVAL, 123, VENDOR_ID, false, false);


		/************************************************************************************************************/
		// Reporting-PLMN-List
		// 0 .. 1
		AvpSet reporting_plmn = requestAvps.addGroupedAvp(Avp.REPORTING_PLMN_LIST, VENDOR_ID, false, false);
			AvpSet plmn_id_list = reporting_plmn.addGroupedAvp(Avp.PLMN_ID_LIST, VENDOR_ID, false, false);
				/**
				 * Concatenation of MCC and MNC
				 */
				String plmn_id = "452400";
				String plmn_id_byte = "";
				for(int i=0; i < plmn_id.length()/2;i++){
					int temp = Integer.decode("0x"+plmn_id.charAt(2*i+1)+plmn_id.charAt(2*i));
					plmn_id_byte += Character.toString((char)temp);
				}
				plmn_id_list.addAvp(Avp.VISITED_PLMN_ID, plmn_id_byte, VENDOR_ID, true, false, true);
				/**
				 * Indicated if the given PLMN-ID (indicated by Visited-PLMN-ID) supports periodic location or not
				 * 0 NOT_SUPPORTED
				 * 1 SUPPORTED
				 */
				plmn_id_list.addAvp(Avp.PERIODIC_LOCATION_SUPPORT_INDICATOR, 0, VENDOR_ID, false, true);

			/**
			 * It indicates if the PLMN-ID-List is provided in prioritized order or not
			 * 0 NOT_PRIORITIZED
			 * 1 PRIORITIZED
			 */
			reporting_plmn.addAvp(Avp.PRIORITIZED_LIST_INDICATOR, 0, VENDOR_ID, false, false);

		/************************************************************************************************************/
		// Motion-Event-Info
		// 0 .. 1
		// AvpSet motion_event = requestAvps.addGroupedAvp(Avp.MOTION_EVENT_INFO, VENDOR_ID, false, false);
			// motion_event.addAvp(Avp.LINEAR_DISTANCE);
			// motion_event.addAvp(Avp.OCCURRENCE_INFO);
			// motion_event.addAvp(Avp.INTERVAL_TIME);
			// motion_event.addAvp(Avp.MAXIMUM_INTERVAL);
			// motion_event.addAvp(Avp.SAMPLING_INTERVAL);
			// motion_event.addAvp(Avp.REPORTING_DURATION);
			// motion_event.addAvp(Avp.REPORTING_LOCATION_REQUIREMENTS);

		// send
		this.session.send(r, this);
		// Ok
	}

	/**
	 * Send Location Report Request
	 * @throws InternalException
	 * @throws IllegalDiameterStateException
	 * @throws RouteException
	 * @throws OverloadException
	 */
	private void sendLRR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		Request r = this.session.createRequest(8388621, this.authAppId_MME, "localdomain", "mme.localdomain");
		AvpSet requestAvps = r.getAvps();
	
		// Auth-Session-State
		// 1 .. 1
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

		/************************************************************************************************************/
		// User-Name
		// 0 .. 1
		requestAvps.addAvp(Avp.USER_NAME, "452041234567813", true, false, false);

		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
		String number = "840987654321";
		String cd = "";
		for(int i=0; i < number.length()/2;i++){
			int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
			cd += Character.toString((char)temp);
		}
		requestAvps.addAvp(Avp.MSISDN, cd, VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// Location-Event
		// 1 .. 1
		/**
		 * Contain the type of event that caused the location procedure to be initiated
		 * 0 EMERGENCY_CALL_ORIGINATION
		 * 1 EMERGENCY_CALL_RELEASE
		 * 2 MO_LR
		 * 3 EMERGENCY_CALL_HANDOVER
		 * 4 DEFERRED_MT_LR_RESPONSE
		 * 5 DEFERRED_MO_LR_TTTP_INITIATION
		 * 6 DELAYED_LOCATION_REPORTING
		 */
		requestAvps.addAvp(Avp.LOCATION_EVENT, 2, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-EPS-Client-Name
		// 1 .. 1
		AvpSet client_name = requestAvps.addGroupedAvp(Avp.LCS_EPS_CLIENT_NAME, VENDOR_ID, true, false);
			client_name.addAvp(Avp.LCS_NAME_STRING, "https://aovl.com.vn", VENDOR_ID, true, false, false);
				/**
				 * Format of the LCS Client name
				 * 0 LOGICAL_NAME
				 * 1 EMAIL_ADDRESS
				 * 2 MSISDN
				 * 3 URL
				 * 4 SIP_URL
				 */
			client_name.addAvp(Avp.LCS_FORMAT_INDICATOR, 3, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// IMEI
		// 0 .. 1
		requestAvps.addAvp(Avp.TGPP_IMEI, "123456789012345", VENDOR_ID, true, false, false);

		/************************************************************************************************************/
		// Location-Estimate
		// 0 .. 1
		/**
		 * Contain an estimate of the location of an MS in universal coordinates and the accuracy of the estimate
		 */
		requestAvps.addAvp(Avp.LOCATION_ESTIMATE, "123456", VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// Accuracy-Fulfilment-Indicator
		// 0 .. 1
		/**
		 * 0 REQUESTED_ACCURACY_FULFILLED
		 * 1 REQUESTED_ACCURACY_NOT_FULFILLED
		 */
		requestAvps.addAvp(Avp.ACCURACY_FULFILMENT_INDICATOR, 1, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Age-Of-Location-Estimate
		// 0 .. 1
		/**
		 * Indicates how long ago the location estimate was obtained in minutes
		 */
		requestAvps.addAvp(Avp.AGE_OF_LOCATION_ESTIMATE, 123456, VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// Velocity-Estimate
		// 0 .. 1
		/**
		 * It is composed of 4 or more octets with an internal structure
		 */
		requestAvps.addAvp(Avp.VELOCITY_ESTIMATE, "ABCD", VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// EUTRAN-Positioning-Data
		// 0 .. 1
		/**
		 * Contain the encoded content of the "Positioning-Data" Information Element
		 */
		requestAvps.addAvp(Avp.EUTRAN_POSITIONING_DATA, "ABCD", VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// ECGI
		// 0 .. 1
		/**
		 * Indicates the E-UTRAN Cell Global Identifier
		 */
		requestAvps.addAvp(Avp.ECGI, "ABCD", VENDOR_ID, true, false, true);

		/************************************************************************************************************/
		// GERAN-Positioning-Info
		// 0 .. 1
		AvpSet geran_positioning = requestAvps.addGroupedAvp(Avp.GERAN_POSITIONING_INFO, VENDOR_ID, false, false);
			/**
			 * Contain the encoded content of the "Positioning Data" Information Element as defined in 3GPP TS 49.031
			 */
			geran_positioning.addAvp(Avp.GERAN_POSITIONING_DATA, "ABCDE", VENDOR_ID, false, false, true);
			/**
			 * Contain the encoded content of the "GANSS Positioning Data" Information Element as defined in 3GPP TS 49.031
			 */
			geran_positioning.addAvp(Avp.GERAN_GANSS_POSITIONING_DATA, "ABCDE", VENDOR_ID, false, false, true);

		/************************************************************************************************************/
		// Cell-Global-Identity
		// 0 .. 1
		/**
		 * Contain the Cell Global Identification of the user which identifies the cell the user equipment is registered
		 */
		requestAvps.addAvp(Avp.CELL_GLOBAL_IDENTITY, "ABCD", VENDOR_ID, false, false, true);

		/************************************************************************************************************/
		// UTRAN-Positioning-Info
		// 0 .. 1
		// AvpSet utran_positioning = requestAvps.addGroupedAvp(Avp.UTRAN_POSITIONING_INFO, VENDOR_ID, false, false);
			// utran_positioning.addAvp(Avp.UTRAN_POSITIONING_DATA);
			// utran_positioning.addAvp(Avp.UTRAN_GANSS_POSITIONING_DATA);
			// utran_positioning.addAvp(Avp.UTRAN_ADDITIONAL_POSITIONING_DATA);

		/************************************************************************************************************/
		// Service-Area-Identity
		// 0 .. 1
		requestAvps.addAvp(Avp.CELL_GLOBAL_IDENTITY, "ABCD", VENDOR_ID, false, false, true);

		/************************************************************************************************************/
		// LCS-Service-Type-ID
		// 0 .. 1
		/**
		 * Define the identifier associated to one of the Service Type for which the LCS client
		 * is allowed to locate the particular UE
		 */
		requestAvps.addAvp(Avp.LCS_SERVICE_TYPE_ID, 1234, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Pseudonym-Indicator

		/************************************************************************************************************/
		// LCS-QoS-Class
		// 0 .. 1
		AvpSet qos = requestAvps.addGroupedAvp(Avp.LCS_QOS, VENDOR_ID, true, false);
			// LCS-QoS-Class
			/**
			 * 0 ASSURED
			 * 1 BEST_EFFORT
			 */
			qos.addAvp(Avp.LCS_QOS_CLASS, 1, VENDOR_ID, true, false);
			// Horizontal-Accuracy
			/**
			 * Bits 6-0 corresponds to Uncertainty Code in 3GPP TS 23.032
			 * Error should be less than the error indicated by the uncertainty code with 67% confidence
			 * Bit 7 to 31 shall be ignored
			 */
			qos.addAvp(Avp.HORIZONTAL_ACCURACY, 1, VENDOR_ID, true, false);
			// Vertical-Accuracy
			qos.addAvp(Avp.VERTICAL_ACCURACY, 1, VENDOR_ID, true, false);
			// Vertical-Requested
			/**
			 * 0 VERTICAL_COORDINATE_IS_NOT_REQUESTED
			 * 1 VERTICAL_COORDINATE_IS_REQUESTED
			 */
			qos.addAvp(Avp.VERTICAL_REQUESTED, 0, VENDOR_ID, true, false);
			// Response-Time
			/**
			 * 0 LOW_DELAY
			 * 1 DELAY_TOLERANT
			 */
			qos.addAvp(Avp.RESPONSE_TIME, 0, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Serving-Node
		// 0 .. 1
		// AvpSet serving_node = requestAvps.addGroupedAvp(Avp.SERVING_NODE, VENDOR_ID, true, false);
			// serving_node.addAvp(Avp.SGSN_NUMBER);
			// serving_node.addAvp(Avp.SGSN_NAME);
			// serving_node.addAvp(Avp.SGSN_REALM);
			// serving_node.addAvp(Avp.MME_NAME);
			// serving_node.addAvp(Avp.MME_REALM);
			// serving_node.addAvp(Avp.MSC_NUMBER);
			// serving_node.addAvp(Avp.TGPP_AAA_SERVER_NAME);
			// serving_node.addAvp(Avp.LCS_CAPABILITIES_SETS);
			// serving_node.addAvp(Avp.GMLC_ADDRESS, InetAddress.getByName("127.0.0.1"), VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LRR-Flags
		// 0 .. 1
		/**
		 * It shall contain a bit mask
		 * 0 Lgd/SLg-Indicator
		 * 1 MO-LR-ShortCircuit-Indicator
		 * 2 MO-LR-ShortCircuit-Requested
		 */
		requestAvps.addAvp(Avp.LRR_FLAGS, 1, VENDOR_ID, false, false, false);

		/************************************************************************************************************/
		// LCS-Reference-Number
		// 0 .. 1
		/**
		 * The reference number identifying the deferred location request
		 */
		requestAvps.addAvp(Avp.LCS_REFERENCE_NUMBER, "123456", VENDOR_ID, false, false, true);

		/************************************************************************************************************/
		// Deferred-MT-LR-Data
		// 0 .. 1
		AvpSet deferred_mt_lr = requestAvps.addGroupedAvp(Avp.DEFERRED_MT_LR_DATA, VENDOR_ID, false, false);
			// deferred_mt_lr.addAvp(Avp.DEFERRED_LOCATION_TYPE);
			// deferred_mt_lr.addAvp(Avp.TERMINATION_CAUSE_LCS);
			// AvpSet def_serving_node = requestAvps.addGroupedAvp(Avp.SERVING_NODE, VENDOR_ID, true, false);
				// def_serving_node.addAvp(Avp.SGSN_NUMBER);
				// def_serving_node.addAvp(Avp.SGSN_NAME);
				// def_serving_node.addAvp(Avp.SGSN_REALM);
				// def_serving_node.addAvp(Avp.MME_NAME);
				// def_serving_node.addAvp(Avp.MME_REALM);
				// def_serving_node.addAvp(Avp.MSC_NUMBER);
				// def_serving_node.addAvp(Avp.TGPP_AAA_SERVER_NAME);
				// def_serving_node.addAvp(Avp.LCS_CAPABILITIES_SETS);
				// def_serving_node.addAvp(Avp.GMLC_ADDRESS, InetAddress.getByName("127.0.0.1"), VENDOR_ID, true, false);

		/************************************************************************************************************/
		// GMLC-Address
		// 0 .. 1
		/**
		 * Contains the IPv4 or IPv6 address od H-GMLC or the V-GMLC associated with the serving node
		 */
		// requestAvps.addAvp(Avp.GMLC_ADDRESS, InetAddress.getByName("127.0.0.1"), VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Reporting-Amount
		// 0 .. 1
		/**
		 * Contains reporting frequency. Its minimum value shall be 1 and maximum value shall be 8639999
		 */
		requestAvps.addAvp(Avp.REPORTING_AMOUNT, 123, VENDOR_ID, false, false);

		/************************************************************************************************************/
		// Periodic-LDR-Information
		// 0 .. 1
		// AvpSet periodic_ldr = requestAvps.addGroupedAvp(Avp.PERIODIC_LDR_INFORMATION, VENDOR_ID, false, false);
			// periodic_ldr.addAvp(Avp.REPORTING_AMOUNT);
			// periodic_ldr.addAvp(Avp.REPORTING_INTERVAL);

		/************************************************************************************************************/
		// ESMLC-Cell-Info

		/************************************************************************************************************/
		// 1xRTT-RCID

		/************************************************************************************************************/
		// Delayed-Location-Reporting-Data

		/************************************************************************************************************/
		// Civic-Address

		/************************************************************************************************************/
		// Barometric-Pressure

		
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
				sendLRR();
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
