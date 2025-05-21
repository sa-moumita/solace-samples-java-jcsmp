/**
 * CustomQueueBrowse.java
 *
 * This sample shows how to provision an endpoint (in this case a 
 * Queue) on the appliance, then how to use a Browser to browse the Queue's contents. 
 * Every message received through the Browser interface is dumped to screen 
 * using the XMLMessage.dump() convenience function for review.
 * 
 * On the publishing side, this sample shows the use of 
 * Producer-Independent Messages, which can be reused and resent multiple times 
 * with different data payloads.
 * 
 * Copyright 2009-2022 Solace Corporation. All rights reserved.
 */

package com.solace.samples.jcsmp.custom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

			/*
			 * Provision a new Queue on the appliance, ignoring if it already
			 * exists. Set permissions, access type, and provisioning flags.
			 */
			//EndpointProperties ep_provision = new EndpointProperties();
			// Set permissions to allow all.
			//ep_provision.setPermission(EndpointProperties.PERMISSION_CONSUME);
			// Set access type to exclusive.
			//ep_provision.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);
			// Set Quota to 100 MB.
			//ep_provision.setQuota(100);
			//SA//String ep_qn = "sample_queue_ProvisionAndBrowse_" + String.valueOf(System.currentTimeMillis() % 10000);
			//String ep_qn = "Q/poc/input";
			String ep_qn = conf.getQueueName();
			final String virtRouterName = (String) session.getProperty(JCSMPProperties.VIRTUAL_ROUTER_NAME);
			System.out.printf("Router's virtual router name: '%s'\n", virtRouterName);
			Queue ep_queue = JCSMPFactory.onlyInstance().createQueue(ep_qn);			
			System.out.println("OK");

			/*
			 * Now browse messages on the Queue and selectively remove
			 * them.
			 */
			BrowserProperties br_prop = new BrowserProperties();
			br_prop.setEndpoint(ep_queue);
			br_prop.setTransportWindowSize(1);
			br_prop.setWaitTimeout(1000);
			if(conf.getCorrelationKey() != null && !"".equals(conf.getCorrelationKey())){
				br_prop.setSelector(conf.getCorrelationKey() + " = '" + conf.getCorrelationValue() + "'");
			}		
			Browser myBrowser = session.createBrowser(br_prop);
			BytesXMLMessage rx_msg = null;
			//JSONArray jsonArray = new JSONArray();
			StringBuffer sb = new StringBuffer();
			do {
				rx_msg = myBrowser.getNext();
				if(rx_msg != null){
					//System.out.println("Browser got message... dumping: START");
					JSONObject json = new JSONObject();
					System.out.println(rx_msg.dump(XMLMessage.MSGDUMP_BRIEF));
					sb.append(rx_msg.dump(XMLMessage.MSGDUMP_BRIEF));
					
					//System.out.println(rx_msg.getMessageId());					
					//System.out.println(rx_msg.getCorrelationId());
					//System.out.println(rx_msg.getAttachmentContentLength());
					String queueData = "";
					if(rx_msg instanceof com.solacesystems.jcsmp.impl.TextMessageImpl){						
						System.out.println("Queue data: " + new String(((TextMessageImpl)rx_msg).getText()));						
						queueData = new String(((TextMessageImpl)rx_msg).getText());
					}else if(rx_msg instanceof com.solacesystems.jcsmp.BytesMessage){
						System.out.println("Queue data: " + new String(((BytesMessage)rx_msg).getData()));						
						queueData = new String(((BytesMessage)rx_msg).getData());
					}	
					
					//json.put("messageId", rx_msg.getMessageId());
					//json.put("correlationId", rx_msg.getCorrelationId());
					//json.put("attachmentLength", rx_msg.getAttachmentContentLength());
					//json.put("queueData", queueData);
					sb.append("content: " + queueData);
					sb.append("\n-----------------------------------------------------------\n\n");
					//jsonArray.put(json);
							
					//System.out.println("Browser got message... dumping: END");
				}
			} while (rx_msg != null);
			System.out.println("Finished browsing.");
			
			//System.out.println( jsonArray.toString(2));
			// Write to a file
			String filePath = "q_content.data";
			//String fileContent = jsonArray.toString(2);
			String fileContent = sb.toString();

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(fileContent);
				System.out.println("Successfully wrote to the file.");
			} catch (IOException e) {
				System.err.println("An error occurred while writing to the file: " + e.getMessage());
			}
			
			// Close the Browser.
			myBrowser.close();			
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
