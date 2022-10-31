package org.example.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Set;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.AvpDataException;
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

public class ShLCS implements EventListener<Request, Answer>{
	private static final String configFile = "org/example/client/LCS_config.xml";
	private static final String dictionaryFile = "org/example/client/dictionary.xml";
	// our destination
	private static final long applicationID_HSS = 16777217;
	private ApplicationId authAppId_HSS = ApplicationId.createByAuthAppId(VENDOR_ID, applicationID_HSS);
	// our realm
	private static final String realmName = "ims.mnc004.mcc452.3gppnetwork.org";

	private static final long VENDOR_ID = 10415;
	private static final String MSISDN = "84976643224";
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
			this.session = this.factory.getNewSession("lbsanm." + realmName + ";" + System.currentTimeMillis());
			Request r = this.session.createRequest(306, this.authAppId_HSS, realmName, "HSPD01.ims.mnc004.mcc452.3gppnetwork.org");
			r.setProxiable(true);
			AvpSet requestAvps = r.getAvps();
			sendRequest(r);

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
	 * Just use for MSISDN
	 * @param a Number of MSISDN String
	 * @return Encoded String value
	 */
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

	/* (non-Javadoc)
	 * @see org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter.api.Message, org.jdiameter.api.Message)
	 */
	@Override
	public void receivedSuccessMessage(Request request, Answer answer) {
        int answerCommandCode = answer.getCommandCode();
		Avp resultAvp = answer.getResultCode();
        
        // Just LCS Application
        if (answerCommandCode != 306) {
            System.out.println("Received bad command code answer: " + answerCommandCode);
            return;
        }
        System.out.println("Received command code answer: " + answerCommandCode);
		try {
			long resultCode = resultAvp.getUnsigned32();
			// Catch error
			if (resultCode == 5001){
				System.out.println("User identified by the IMSI or the MSISDN is unknown!");
			}
			else if (resultCode == 5005 || resultCode == 5004) {
				System.out.println("Something wrong happened at server side!");
			}
			else if (resultCode == 5490) {
				System.out.println("The requesting GMLC's network is not authorized to request UE location information!");
			}
			else if (resultCode == 4201) {
				System.out.println("The location of the targeted user is not known at this time to satisfy the requested operation!");
			}
			this.session.release();
			this.session = null;
			finished = true;
		} catch (AvpDataException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void timeoutExpired(Request request) {

	}

	public static void main(String[] args) {
		ShLCS ec = new ShLCS();
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

	/**
	 * Send request to HSS
	 * @param request message from LCS client
	 */
	public void sendRequest(Request request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		AvpSet requestAvps = request.getAvps();
		
		requestAvps.removeAvp(Avp.DESTINATION_HOST);

		requestAvps.addAvp(Avp.AUTH_SESSION_STATE, 1, true, false);

		AvpSet userid = requestAvps.addGroupedAvp(Avp.USER_IDENTITY, VENDOR_ID, true, false);
		userid.addAvp(Avp.MSISDN, magicConvert(MSISDN), VENDOR_ID, true, false, true);

		requestAvps.addAvp(Avp.REQUESTED_DOMAIN, 1, VENDOR_ID, true, false);

		requestAvps.addAvp(713, 1, VENDOR_ID, false, false);

		requestAvps.addAvp(Avp.CURRENT_LOCATION, 0, VENDOR_ID, true, false);

		requestAvps.addAvp(Avp.DATA_REFERENCE, 14, VENDOR_ID, true, false, true);

		this.session.send(request, this);
	}
}
