// cũ
package org.example.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Set;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.OverloadException;

public class ProcessRequest {
	private static long VENDOR_ID = 10415;
	private static String GMLC_Address = "127.0.0.10";
	private static String IMSI = "452041234567813";
	private static String MSISDN = "840987654321";
	private static String GMLC_number = "1";
	private static String IMEI = "123456789012345";
	private static String PLMN_ID = "45204";

	/**
	 * Process LCS-Routing_Info Request message
	 * @param request Request message created by this session
	 * @return Request to send new request to HSS
	 * @throws InternalException
	 * @throws IllegalDiameterStateException
	 * @throws RouteException
	 * @throws OverloadException
	 */
	public static Request processRIR(Request request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		AvpSet requestAvps = request.getAvps();
		// Auth-Session-State
		// 1 .. 1
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

		/************************************************************************************************************/
		// User-Name
		// 0 .. 1
		requestAvps.addAvp(Avp.USER_NAME, IMSI, true, false, false);

		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
			/*
			String number = "840987654321";
			String cd = "";
			for(int i=0; i < number.length()/2;i++){
				int temp = Integer.decode("0x"+number.charAt(2*i+1)+number.charAt(2*i));
				cd += Character.toString((char)temp);
			}
			*/
		requestAvps.addAvp(Avp.MSISDN, MSISDN.getBytes(), VENDOR_ID, true, false);

		/************************************************************************************************************/
		// GMLC-Number
		// 0 .. 1
		requestAvps.addAvp(Avp.GMLC_NUMBER, GMLC_number.getBytes(), VENDOR_ID, true, false);
		
		return request;
	}

	/**
	 * Process Provide-Location Request message
	 * @param request Request message created by this session
	 * @param answerAvps Set of answer Avps
	 * @return Request to send new request to MME
	 * @throws InternalException
	 * @throws IllegalDiameterStateException
	 * @throws RouteException
	 * @throws OverloadException
	 */
	public static Request processPLR(Request request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		AvpSet requestAvps = request.getAvps();
	
		// Auth-Session-State
		// 1 .. 1
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);
		
		/************************************************************************************************************/
		// User-Name
		// 0 .. 1
		requestAvps.addAvp(Avp.USER_NAME, IMSI, true, false, false);
		
		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
		requestAvps.addAvp(Avp.MSISDN, MSISDN.getBytes(), VENDOR_ID, true, false);
		
		/************************************************************************************************************/
		// SLg-Location-Type
		// 1 .. 1
			/**
			 * This Information Element shall contain the type of location
			 * measurement requested, such as current location, initial location, last
			 * known location
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
		/**
		 * Shall contain the IMEI of the UE to be positioned
		 */
		requestAvps.addAvp(Avp.TGPP_IMEI, IMEI, VENDOR_ID, true, false, false);

		/************************************************************************************************************/
		// LCS-EPS-Client-Name
		// 1 .. 1
		/**
		 * This Information Element shall contain the name of the LCS client
		 * issuing the positioning request.
		 */
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
		/**
		 * Contains the identity of the
		 * originating entity which has requested the location of the target UE
		 * from the LCS Client
		 */
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
		/**
		 * Shall contain the quality of service
		 * requested, such as the accuracy of the positioning measurement and
		 * the response time of the positioning operation. 
		 */
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
		 * Shall contain an indication of
		 * whether or not the Velocity of the target UE is requested
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
		/**
		 * Shall contain an indication of how
		 * the positioning operation should proceed in the relation to the
		 * checking of the non-session-related privacy settings of the user
		 */
		AvpSet privacy_non_session = requestAvps.addGroupedAvp(Avp.LCS_PRIVACY_CHECK_NON_SESSION, VENDOR_ID, true, false);
			/**
			 * 0 ALLOWED_WITHOUT_NOTIFICATION
			 */
		privacy_non_session.addAvp(Avp.LCS_PRIVACY_CHECK, 0, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-Privacy-Check-Session
		// 0 .. 1
		/**
		 * Shall contain an indication of how
		 * the positioning operation should proceed in the relation to the
		 * checking of the session-related privacy settings of the user
		 */
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
		 * It shall contain a bit mask. Each bit indicates a type of 
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
		/**
		 * Shall contain area definition, type
		 * of area event, occurrence info and minimum interval time. For a
		 * deferred EPC-MT-LR, this Information Element may also contain the
		 * duration of event reporting, the maximum time interval between event
		 * reports, the maximum event sampling interval, and whether location
		 * estimates shall be included in event reports. This Information
		 * Element is applicable only when the deferred MT-LR is initiated for
		 * the area event (UE entering or leaving or being in a pre-defined
		 * geographical area)
		 */
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
		try{
			requestAvps.addAvp(Avp.GMLC_ADDRESS, InetAddress.getByName(GMLC_Address), VENDOR_ID, true, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
				plmn_id_list.addAvp(Avp.VISITED_PLMN_ID, PLMN_ID.getBytes(), VENDOR_ID, true, false);
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

		return request;
	}

	/**
	 * Process Location Report Request message
	 * @param request Request message created by this session
	 * @return Request to send new request to MME
	 * @throws InternalException
	 * @throws IllegalDiameterStateException
	 * @throws RouteException
	 * @throws OverloadException
	 */
	public static Request processLRR(Request request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		AvpSet requestAvps = request.getAvps();
	
		// Auth-Session-State
		// 1 .. 1
		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

		/************************************************************************************************************/
		// User-Name
		// 0 .. 1
		requestAvps.addAvp(Avp.USER_NAME, IMSI, true, false, false);

		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
		requestAvps.addAvp(Avp.MSISDN, MSISDN.getBytes(), VENDOR_ID, true, false);

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
		/**
		 * This Information Element shall contain the name of the LCS client
		 * issuing the positioning request.
		 */
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
		/**
		 * Shall contain the IMEI of the UE to be positioned
		 */
		requestAvps.addAvp(Avp.TGPP_IMEI, IMEI, VENDOR_ID, true, false, false);

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
		// 0 .. 1
		/**
		 * It defines if a pseudonym is requested
		 * 0 PSEUDONYM_NOT_REQUESTED
		 * 1 PSEUDONYM_REQUESTED
		 */
		requestAvps.addAvp(Avp.PSEUDONYM_INDICATOR, 0, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// LCS-QoS-Class
		// 0 .. 1
		/**
		 * 0 ASSURED
		 * 1 BEST_EFFORT
		 */
		requestAvps.addAvp(Avp.LCS_QOS_CLASS, 1, VENDOR_ID, true, false);

		/************************************************************************************************************/
		// Serving-Node
		// 0 .. 1
		// AvpSet serving_node = createServingNodeAvp(requestAvps);

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
			// AvpSet def_serving_node = createServingNodeAvp(requestAvps);

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
		// 0 .. 1
		// AvpSet esmlc_cell = requestAvps.addGroupedAvp(Avp.ESMLC_CELL_INFO, VENDOR_ID, false, false);
			// esmlc_cell.addAvp(Avp.ECGI, "ABCD", VENDOR_ID, true, false, true);
			// esmlc_cell.addAvp(Avp.CELL_PORTION_ID);

		/************************************************************************************************************/
		// 1xRTT-RCID
		// 0 .. 1
		/**
		 * OctetString
		 * It indicates the 1xRTT Reference Cell Id that consists of a Cell
		 * Identification Discriminator and a Cell Identification and shall be formatted according to octets 3 through the end of the
		 * Cell Identifier element defined in clause 4.2.17 in 3GPP2 A.S0014-D [22]. The allowable cell discriminator values are
		 * "0000 0010", and "0000 0111"
		 */

		/************************************************************************************************************/
		// Delayed-Location-Reporting-Data
		// 0 .. 1
		// AvpSet delayed_location_reporting = requestAvps.addGroupedAvp(Avp.DELAYED_LOCATION_REPORTING, VENDOR_ID, false, true);
			// delayed_location_reporting.addAvp(Avp.TERMINATION_CAUSE_LCS);
			// AvpSet delay_serving_node = createServingNodeAvp(delayed_location_reporting);

		/************************************************************************************************************/
		// Civic-Address
		// 0 .. 1
		/**
		 * Contains the XML document carried in the "Civic Address" Information Element as defined in 3GPP TS 29.171
		 */
		requestAvps.addAvp(Avp.CIVIC_ADDRESS, "hello.xml", VENDOR_ID, false, false, false);

		/************************************************************************************************************/
		// Barometric-Pressure
		// 0 .. 1
		/**
		 * It contains the "Barometric Pressure" Information Element as defined in 3GPP TS 29.171
		 */
		requestAvps.addAvp(Avp.BAROMETRIC_PRESSURE, 123, VENDOR_ID, false, false, true);
		
		return request;
	}
	
	private static AvpSet createServingNodeAvp(AvpSet root) {
		AvpSet serving_node = root.addGroupedAvp(Avp.SERVING_NODE, VENDOR_ID, true, false);
		// serving_node.addAvp(Avp.SGSN_NUMBER);
		// serving_node.addAvp(Avp.SGSN_NAME);
		// serving_node.addAvp(Avp.SGSN_REALM);
		// serving_node.addAvp(Avp.MME_NAME);
		// serving_node.addAvp(Avp.MME_REALM);
		// serving_node.addAvp(Avp.MSC_NUMBER);
		// serving_node.addAvp(Avp.TGPP_AAA_SERVER_NAME);
		// serving_node.addAvp(Avp.LCS_CAPABILITIES_SETS);
		// serving_node.addAvp(Avp.GMLC_ADDRESS, InetAddress.getByName("127.0.0.1"), VENDOR_ID, true, false);
		return serving_node;
	}
}
