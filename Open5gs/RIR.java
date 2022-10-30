package org.example.client;

import java.net.InetAddress;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.OverloadException;

public class RIR {
    private static long VENDOR_ID = Dataset.getVENDOR_ID();

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
		requestAvps.addAvp(Avp.USER_NAME, Dataset.getIMSI(), true, false, false);

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
		requestAvps.addAvp(Avp.MSISDN, Dataset.getMSISDN().getBytes(), VENDOR_ID, true, false);

		/************************************************************************************************************/
		// GMLC-Number
		// 0 .. 1
		requestAvps.addAvp(Avp.GMLC_NUMBER, Dataset.getGMLC_number().getBytes(), VENDOR_ID, true, false);
		
		return request;
	}

}
