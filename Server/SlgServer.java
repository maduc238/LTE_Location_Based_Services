package org.example.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.StackType;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.diameter.dictionary.AvpRepresentation;

public class SlgServer implements NetworkReqListener {
	private static final Logger log = Logger.getLogger(SlgServer.class);
	static{
		configLog4j();
	}

private static void configLog4j() {
	InputStream inStreamLog4j = SlgServer.class.getClassLoader().getResourceAsStream("log4j.properties");
	Properties propertiesLog4j = new Properties();
	try {
		propertiesLog4j.load(inStreamLog4j);
		PropertyConfigurator.configure(propertiesLog4j);
	} catch (Exception e) {
		e.printStackTrace();
	}

	log.debug("log4j configured");

}
	private static final String configFile = "org/example/server/slg_config.xml";
	private static final String dictionaryFile = "org/example/client/dictionary.xml";
	private static final long vendorID = 10415;
	private static final long applicationID = 16777255;
	private ApplicationId authAppId = ApplicationId.createByAuthAppId(vendorID, applicationID);

	private AvpDictionary dictionary = AvpDictionary.INSTANCE;
	private Stack stack;
	private SessionFactory factory;
	private Session session;
	private int toReceiveIndex = 0;
	private boolean finished = false;

	private void initStack() {
		if (log.isInfoEnabled()) {
			log.info("Initializing Stack...");
		}
		InputStream is = null;
		try {
			this.stack = new StackImpl();
			is = this.getClass().getClassLoader().getResourceAsStream(configFile);

			Configuration config = new XMLConfiguration(is);
			factory = stack.init(config);
			if (log.isInfoEnabled()) {
				log.info("Stack Configuration successfully loaded.");
			}

			Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

			log.info("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
			for (org.jdiameter.api.ApplicationId x : appIds) {
				log.info("Diameter Stack  :: Common :: " + x);
			}
			is.close();
			Network network = stack.unwrap(Network.class);
			network.addNetworkReqListener(this, this.authAppId);
		} catch (Exception e) {
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

		MetaData metaData = stack.getMetaData();
		if (metaData.getStackType() != StackType.TYPE_SERVER || metaData.getMinorVersion() <= 0) {
			stack.destroy();
			if (log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
				log.error("Incorrect driver");
			}
			return;
		}

		try {
			if (log.isInfoEnabled()) {
				log.info("Starting stack");
			}
			stack.start();
			if (log.isInfoEnabled()) {
				log.info("Stack is running.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			stack.destroy();
			return;
		}
		if (log.isInfoEnabled()) {
			log.info("Stack initialization successfully completed.");
		}
	}

	/**
	 * @return
	 */
	private boolean finished() {
		return this.finished;
	}

	public static void main(String[] args) {
		SlgServer es = new SlgServer();
		es.initStack();

		while (!es.finished()) {
			try {
				Thread.currentThread();
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.jdiameter.api.NetworkReqListener#processRequest(org.jdiameter.api
	 * .Request)
	 */
	@Override
	public Answer processRequest(Request request) {
		int requestCommandCode = request.getCommandCode();
		if (requestCommandCode != 8388620 && requestCommandCode != 8388621) {
			log.error("Received bad answer: " + requestCommandCode);
			return null;
		}
		try {
			/*
				PLA
				[ Vendor-Specific-Application-Id ]
				[ Result-Code ]
				[ Experimental-Result ]
				{ Auth-Session-State }
				{ Origin-Host }
				{ Origin-Realm }
				[ Location-Estimate ]
				[ Accuracy-Fulfilment-Indicator ]
				[ Age-Of-Location-Estimate]
				[ Velocity-Estimate ]
				[ EUTRAN-Positioning-Data]
				[ ECGI ]
				[ GERAN-Positioning-Info ]
				[ Cell-Global-Identity ]
				[ UTRAN-Positioning-Info ]
				[ Service-Area-Identity ]
				[ Serving-Node ]
				[ PLA-Flags ]
				[ ESMLC-Cell-Info ]
				[ Civic-Address ]
				[ Barometric-Pressure 
			 */

			/*
				LRA
				Vendor-Specific-Application-Id ]
				[ Result-Code ]
				[ Experimental-Result ]
				{ Auth-Session-State }
				{ Origin-Host }
				{ Origin-Realm }
				[ GMLC-Address ]
				[ LRA-Flags ]
				[ Reporting-PLMN-List ]
				[ LCS-Reference-Number ]
			 */

			this.session = this.factory.getNewSession(request.getSessionId());
			System.out.println("hello");
			Answer answer = request.createAnswer(2001);
			this.session.release();
        	finished = true;
        	this.session = null;
			return answer;
		} catch (InternalException e) {
			e.printStackTrace();
		}
		finished = true;
		return null;
	}
}
