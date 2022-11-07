package com.example.test.config;

import java.util.concurrent.TimeUnit;

import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Network;
import org.jdiameter.api.sh.ServerShSession;
import org.jdiameter.client.api.ISessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.test.ShServer;
import com.example.test.utilis.DatabaseManager;
import com.example.test.utilis.Printor;
import com.example.test.utilis.StackCreator;

@Configuration
public class ServerConfig {

    private String configFile = "config-server.xml";
    private String mongodbHost = "localhost";
    private int mongodbPort = 27017;
    private ApplicationId shAppId = ApplicationId.createByAuthAppId(10415L, 16777217L);

    @Bean("stackCreator")
    StackCreator stackConfig() throws Exception{
        String config = Printor.readFile(this.getClass().getClassLoader().getResourceAsStream(configFile));
        StackCreator stackCreator = new StackCreator(config,"Server");
        return stackCreator;
    }

    @Bean("databaseMng")
    DatabaseManager databaseMngConfig() {
        DatabaseManager databaseMng = new DatabaseManager(mongodbHost,mongodbPort);
        return databaseMng;
    }

    @Bean("shServer")
    ShServer shServerConfig(DatabaseManager databaseMng, StackCreator stackCreator) throws Exception{
      ShServer shServer = new ShServer(databaseMng, stackCreator);

      //config listener request
      Network network = stackCreator.unwrap(Network.class);
      network.addNetworkReqListener(shServer, shAppId);

      //run stack
      stackCreator.start(Mode.ALL_PEERS, 30000, TimeUnit.MILLISECONDS);

      ((ISessionFactory) stackCreator.getSessionFactory()).registerAppFacory(ServerShSession.class, shServer);
      return shServer;
    }

}
