/**
 * CustomQueueBrowse.java
 * 
 * This class will browse messages from a queue based on given key-value pair or all the messages
 * Developed by Moumita Saha (msaha2@its.jnj.com)
 * 
 */

package com.solace.samples.jcsmp.custom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.solace.samples.jcsmp.features.common.ArgParser;
import com.solace.samples.jcsmp.features.common.SampleApp;
import com.solace.samples.jcsmp.features.common.SampleUtils;
import com.solace.samples.jcsmp.features.common.SessionConfiguration;
import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessageProducer;

import org.json.JSONArray;
import org.json.JSONObject;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.impl.TextMessageImpl;

public class CustomQueueBrowse extends SampleApp {
	XMLMessageProducer prod = null;
	SessionConfiguration conf = null;

	void createSession(String[] args) {
		ArgParser parser = new ArgParser();

		// Parse command-line arguments
		if (parser.parse(args) == 0)
			conf = parser.getConfig();
		else
			printUsage(parser.isSecure());

		session = SampleUtils.newSession(conf, new PrintingSessionEventHandler(),null);
	}

	void printUsage(boolean secure) {
		String strusage = ArgParser.getCommonUsage(secure);
		System.out.println(strusage);
		finish(1);
	}

	public static void main(String[] args) {
		CustomQueueBrowse qsample = new CustomQueueBrowse();
		qsample.run(args);
	}

	void checkCapability(final CapabilityType cap) {
		System.out.printf("Checking for capability %s...", cap);
		if (session.isCapable(cap)) {
			System.out.println("OK");
		} else {
			System.out.println("FAILED");
			finish(1);
		}
	}

	byte[] getBinaryData(int len) {
		final byte[] tmpdata = "the quick brown fox jumps over the lazy dog / flying spaghetti monster ".getBytes();
		final byte[] ret_data = new byte[len];
		for (int i = 0; i < len; i++)
			ret_data[i] = tmpdata[i % tmpdata.length];
		return ret_data;
	}

	void run(String[] args) {
		createSession(args);
		try {
			// Connects the Session and acquires a message producer.
	        session.connect();
			prod = session.getMessageProducer(new PrintingPubCallback());

			// Check capability to provision endpoints
			checkCapability(CapabilityType.ENDPOINT_MANAGEMENT);
			// Check capability to browse queues
			checkCapability(CapabilityType.BROWSER);


			String ep_qn = conf.getQueueName();
			final String virtRouterName = (String) session.getProperty(JCSMPProperties.VIRTUAL_ROUTER_NAME);
			System.out.printf("Router's virtual router name: '%s'\n", virtRouterName);
			Queue ep_queue = JCSMPFactory.onlyInstance().createQueue(ep_qn);			
			System.out.println("OK");

			/*
			 * Now browse messages on the Queue and selectively remove
			 * them.
			 */
			StringBuffer sb = new StringBuffer();
			String correlationValues = "";
			int count = 0;
			if((conf.getCorrelationKey() != null && !"".equals(conf.getCorrelationKey())) && 
				(conf.getCorrelationValue() != null &&!"".equals(conf.getCorrelationValue()))){
				correlationValues = conf.getCorrelationValue();
			}
			BrowserProperties br_prop = new BrowserProperties();				
			br_prop.setEndpoint(ep_queue);
			br_prop.setTransportWindowSize(1);
			br_prop.setWaitTimeout(1000);			
			if(!"".equals(correlationValues)){
				String[] elements = correlationValues.split(",\\s*");
				List<String> list = Arrays.asList(elements);
				Iterator<String> iterator = list.iterator();			
				while(iterator.hasNext()){
					String correlationValue = iterator.next();					
					br_prop.setSelector(conf.getCorrelationKey() + " = '" + correlationValue + "'");
					Browser myBrowser = session.createBrowser(br_prop);
					BytesXMLMessage rx_msg = null;
					do {
						rx_msg = myBrowser.getNext();
						if(rx_msg != null){
							//System.out.println("Browser got message... dumping: START");
							//JSONObject json = new JSONObject();
							System.out.println(rx_msg.dump(XMLMessage.MSGDUMP_BRIEF));
							sb.append(rx_msg.dump(XMLMessage.MSGDUMP_BRIEF));
							String queueData = "";
							if(rx_msg instanceof com.solacesystems.jcsmp.impl.TextMessageImpl){						
								System.out.println("Queue data: " + new String(((TextMessageImpl)rx_msg).getText()));						
								queueData = new String(((TextMessageImpl)rx_msg).getText());
							}else if(rx_msg instanceof com.solacesystems.jcsmp.BytesMessage){
								System.out.println("Queue data: " + new String(((BytesMessage)rx_msg).getData()));						
								queueData = new String(((BytesMessage)rx_msg).getData());
							}	
							sb.append("content: " + queueData);
							sb.append("\n-----------------------------------------------------------\n\n");
							count = count + 1;
						}
					} while (rx_msg != null);
					rx_msg = null;
					// Close the Browser.
					myBrowser.close();	
				}							
			}else{
				Browser myBrowser = session.createBrowser(br_prop);
				BytesXMLMessage rx_msg = null;
				do {
					rx_msg = myBrowser.getNext();
					if(rx_msg != null){
						//System.out.println("Browser got message... dumping: START");
						//JSONObject json = new JSONObject();
						System.out.println(rx_msg.dump(XMLMessage.MSGDUMP_BRIEF));
						sb.append(rx_msg.dump(XMLMessage.MSGDUMP_BRIEF));
						String queueData = "";
						if(rx_msg instanceof com.solacesystems.jcsmp.impl.TextMessageImpl){						
							System.out.println("Queue data: " + new String(((TextMessageImpl)rx_msg).getText()));						
							queueData = new String(((TextMessageImpl)rx_msg).getText());
						}else if(rx_msg instanceof com.solacesystems.jcsmp.BytesMessage){
							System.out.println("Queue data: " + new String(((BytesMessage)rx_msg).getData()));						
							queueData = new String(((BytesMessage)rx_msg).getData());
						}	
						sb.append("content: " + queueData);
						sb.append("\n-----------------------------------------------------------\n\n");
						count = count + 1;
					}
				} while (rx_msg != null);
				rx_msg = null;
				// Close the Browser.
				myBrowser.close();					
			}
			System.out.println("Finished browsing.");
			sb.append("\n\nTotal number of Messages browsed: " + String.valueOf(count));
			// Write to a file
			String filePath = "q_browse_content.dat";
			//String fileContent = jsonArray.toString(2);
			String fileContent = sb.toString();

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(fileContent);
				System.out.println("Successfully wrote to the file.");
			} catch (IOException e) {
				System.err.println("An error occurred while writing to the file: " + e.getMessage());
			}
			
					
			System.out.println("OK");

			finish(0);
							
			
		} catch (JCSMPTransportException ex) {
			System.err.println("Encountered a JCSMPTransportException, closing session... " + ex.getMessage());
			if (prod != null) {
				prod.close();
				// At this point the producer handle is unusable, a new one
				// may be created by the application.
			}
			finish(1);
		} catch (JCSMPException ex) {
			System.err.println("Encountered a JCSMPException, closing consumer channel... " + ex.getMessage());
			// Possible causes:
			// - Authentication error: invalid username/password
			// - Provisioning error: unable to add subscriptions from CSMP
			// - Invalid or unsupported properties specified
			if (prod != null) {
				prod.close();
				// At this point the producer handle is unusable, a new one
				// may be created by the application.
			}
			finish(1);
		} catch (Exception ex) {
			System.err.println("Encountered an Exception... " + ex.getMessage());
			finish(1);
		}

	}

}
