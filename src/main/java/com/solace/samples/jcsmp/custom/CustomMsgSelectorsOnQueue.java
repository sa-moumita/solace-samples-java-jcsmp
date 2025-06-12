/**
 * CustomMsgSelectorsOnQueue.java
 * 
 * This class will read message from a queue based on given key-value pair
 * Developed by Moumita Saha (msaha2@its.jnj.com)
 * 
 */

package com.solace.samples.jcsmp.custom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.solace.samples.jcsmp.features.common.ArgParser;
import com.solace.samples.jcsmp.features.common.SampleApp;
import com.solace.samples.jcsmp.features.common.SampleUtils;
import com.solace.samples.jcsmp.features.common.SessionConfiguration;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.Consumer;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;
import com.solacesystems.jcsmp.impl.TextMessageImpl;

public class CustomMsgSelectorsOnQueue extends SampleApp {
	Consumer cons = null;
    XMLMessageProducer prod = null;
	SessionConfiguration conf = null;

	void createSession(String[] args) {
		ArgParser parser = new ArgParser();
		
		// Setup defaults
		SessionConfiguration sc = new SessionConfiguration();
		sc.setDeliveryMode(DeliveryMode.PERSISTENT);
		parser.setConfig(sc);
		
		// Parse command-line arguments
		if (parser.parse(args) == 0)
			conf = parser.getConfig();
		else
			printUsage(parser.isSecure());

		session = SampleUtils.newSession(conf, new PrintingSessionEventHandler(),null);
	}
	
	static class MessageDumpListener implements XMLMessageListener {
		public void onException(JCSMPException exception) {
			exception.printStackTrace();
		}

		public void onReceive(BytesXMLMessage message) {
			//System.out.println("\n======== Received message ======== \n" + message.dump());
			StringBuffer sb = new StringBuffer();
			System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
			sb.append(message.dump(XMLMessage.MSGDUMP_BRIEF));
			String queueData = "";
			if(message instanceof com.solacesystems.jcsmp.impl.TextMessageImpl){						
				System.out.println("Queue data: " + new String(((TextMessageImpl)message).getText()));						
				queueData = new String(((TextMessageImpl)message).getText());
			}else if(message instanceof com.solacesystems.jcsmp.BytesMessage){
				System.out.println("Queue data: " + new String(((BytesMessage)message).getData()));						
				queueData = new String(((BytesMessage)message).getData());
			}
			sb.append("content: " + queueData);
			sb.append("\n-----------------------------------------------------------\n\n");
			// Write to a file
			String filePath = "q_read_content.dat";			
			String fileContent = sb.toString();
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(fileContent);
				System.out.println("Successfully wrote to the file.");
			} catch (IOException e) {
				System.err.println("An error occurred while writing to the file: " + e.getMessage());
			}			
		}
	}
	
	void printUsage(boolean secure) {
		String strusage = ArgParser.getCommonUsage(secure);
		System.out.println(strusage);
		finish(1);
	}
	
	public CustomMsgSelectorsOnQueue() {
	}

	public static void main(String[] args) {
		CustomMsgSelectorsOnQueue msg_sel_sample = new CustomMsgSelectorsOnQueue();
		msg_sel_sample.run(args);
	}
	
	void run(String[] args) {
		createSession(args);

		try {
			// Acquire a blocking message consumer and open the data channel to
			// the appliance.
			System.out.println("About to connect to appliance.");
	        session.connect();
			prod = session.getMessageProducer(new PrintingPubCallback());
			printRouterInfo();
			// Check the capabilities after the connection is established 
			//(verify selectors are supported).
			if (!session.isCapable(CapabilityType.SELECTOR)) {
				System.out.println("Requires a Solace appliance supporting message selectors.");
				finish(1);
			}

			/*
			 * Creation of the Queue object. Temporary destinations must be
			 * acquired from a connected Session, as they require knowledge
			 * about the connected appliance.
			 */
			String ep_qn = conf.getQueueName();
			Queue myqueue = JCSMPFactory.onlyInstance().createQueue(ep_qn);			
			System.out.println("OK");
			//Queue myqueue = session.createTemporaryQueue();

			/*
			 * Creation of a Flow: a FlowReceiver is acquired for consuming
			 * messages from a specified endpoint.
			 * 
			 * The selector "pasta = 'rotini' OR pasta = 'farfalle'" is used to
			 * select only messages matching those pasta types in their user
			 * property map.
			 */
			ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
			flow_prop.setEndpoint(myqueue);
			//flow_prop.setSelector("pasta = 'rotini' OR pasta = 'farfalle' OR SAP_MplCorrelationId = 'AGgYp9ussDxRtrKhDPHZhIbMO_V3'");
			flow_prop.setSelector(conf.getCorrelationKey() + " = '" + conf.getCorrelationValue() + "'");
			System.out.println("Binding to endpoint (queue): " + myqueue);
			cons = session.createFlow(new MessageDumpListener(), flow_prop);
			System.out.println("Connected!");
			

			cons.start();
			// Will receive and print any messages.
			Thread.sleep(2000);

			// Close our consumer flow and release temporary endpoints
			cons.close();
			
			finish(0);
		} catch (JCSMPTransportException ex) {
			System.err.println("Encountered a JCSMPTransportException, closing consumer channel... " + ex.getMessage());
			if (cons != null) {
				cons.close();
				// At this point the consumer handle is unusable; a new one should be created 
				// by calling cons = session.getMessageConsumer(...) if the application 
				// logic requires the consumer channel to remain open.
			}
			finish(1);
		} catch (JCSMPException ex) {
			System.err.println("Encountered a JCSMPException, closing consumer channel... " + ex.getMessage());
			// Possible causes: 
			// - Authentication error: invalid username/password 
			// - Provisioning error: unable to add subscriptions from CSMP
			// - Invalid or unsupported properties specified
			// - Endpoint not created on the appliance
			if (cons != null) {
				cons.close();
				// At this point the consumer handle is unusable; a new one should be created 
				// by calling cons = session.getMessageConsumer(...) if the application 
				// logic requires the consumer channel to remain open.
			}
			finish(1);
		} catch (Exception ex) {
			System.err.println("Encountered an Exception... " + ex.getMessage());
			finish(1);
		}
	}

}
