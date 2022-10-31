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

public class GMLC implements EventListener<Request, Answer>{
	private static final String configFile = "org/example/client/GMLC_config.xml";
	private static final String dictionaryFile = "org/example/client/dictionary.xml";
	// our destination
	private static final long applicationID_HSS = 16777291;
	private static final long applicationID_MME = 16777255;
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
			this.session = this.factory.getNewSession("gmlc.localdomain;" + System.currentTimeMillis() + ";app_lcs");
			Request r = this.session.createRequest(8388622, this.authAppId_HSS, "localdomain", "hss.localdomain");
			this.session.send(RIR.processRIR(r), this);

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

	/* (non-Javadoc)
	 * @see org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter.api.Message, org.jdiameter.api.Message)
	 */
	@Override
	public void receivedSuccessMessage(Request request, Answer answer) {
        int answerCommandCode = answer.getCommandCode();
		Avp resultAvp = answer.getResultCode();
        
        // Just LCS Application
        if (answerCommandCode != 8388620 && answerCommandCode != 8388621 && answerCommandCode != 8388622) {
            System.out.println("Received bad command code answer: " + answerCommandCode);
            return;
        }
		System.out.println("************************************************************************************");
        System.out.println("Received command code answer: " + answerCommandCode);

        try {
			long resultCode = resultAvp.getUnsigned32();
			// Catch error
            if (resultCode == 5001){
				this.session.release();
                this.session = null;
				System.out.println("User identified by the IMSI or the MSISDN is unknown!");
                finished = true;
            }
            else if (resultCode == 5005 || resultCode == 5004) {
                this.session.release();
                this.session = null;
                System.out.println("Something wrong happened at server side!");
                finished = true;
            }
			else if (resultCode == 5490) {
                this.session.release();
                this.session = null;
                System.out.println("The requesting GMLC's network is not authorized to request UE location information!");
                finished = true;
            }
			else if (resultCode == 4201) {
                this.session.release();
                this.session = null;
                System.out.println("The location of the targeted user is not known at this time to satisfy the requested operation!");
                finished = true;
            }
			if (answerCommandCode == 8388622) {
				Request r = this.session.createRequest(8388620, this.authAppId_MME, "localdomain", "mme.localdomain");
				this.session.send(PLR.processPLR(r), this);

				r = this.session.createRequest(8388621, this.authAppId_MME, "localdomain", "mme.localdomain");
				this.session.send(LRR.processLRR(r), this);

				this.session.release();
                this.session = null;
				finished = true;
			}
			
        } catch (AvpDataException e) {
			e.printStackTrace();
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
