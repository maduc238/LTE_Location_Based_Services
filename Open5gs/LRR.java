package org.example.client;

import java.net.InetAddress;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.OverloadException;

public class LRR {
	private static long VENDOR_ID = Dataset.getVENDOR_ID();

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
		requestAvps.addAvp(Avp.USER_NAME, Dataset.getIMSI(), true, false, false);

		/************************************************************************************************************/
		// MSISDN
		// 0 .. 1
		requestAvps.addAvp(Avp.MSISDN, Dataset.getMSISDN().getBytes(), VENDOR_ID, true, false);

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
			client_name.addAvp(Avp.LCS_NAME_STRING, Dataset.getLCS_NAME_STRING(), VENDOR_ID, true, false, false);
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
		requestAvps.addAvp(Avp.TGPP_IMEI, Dataset.getIMEI(), VENDOR_ID, true, false, false);

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