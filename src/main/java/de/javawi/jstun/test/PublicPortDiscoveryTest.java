/*
 * This file is part of JSTUN. 
 * 
 * Copyright (c) 2005 Thomas King <king@t-king.de> - All rights
 * reserved.
 * 
 * This software is licensed under either the GNU Public License (GPL),
 * or the Apache 2.0 license. Copies of both license agreements are
 * included in this distribution.
 */

package de.javawi.jstun.test;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.ErrorCode;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.util.UtilityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class PublicPortDiscoveryTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PublicPortDiscoveryTest.class);
	InetAddress sourceIaddress;
	int sourcePort;
	String stunServer;
	int stunServerPort;
	int timeoutInitValue = 300; //ms
	MappedAddress ma = null;
	ChangedAddress ca = null;
	boolean nodeNatted = true;
	DiscoveryInfo di = null;

	final static int UNINITIALIZED = -1;
	final static int ERROR = 0;
	final static int CONNECTION_ESTABLISHED_NO_ERROR = 1;
	final static int CONNECTION_TIMEOUT = 2;

	public PublicPortDiscoveryTest(InetAddress sourceIaddress, int sourcePort, String stunServer, int stunServerPort) {
		this.sourceIaddress = sourceIaddress;
		this.sourcePort = sourcePort;
		this.stunServer = stunServer;
		this.stunServerPort = stunServerPort;
	}
		
	public DiscoveryInfo test() throws UtilityException, SocketException, UnknownHostException, IOException, MessageAttributeParsingException, MessageAttributeException, MessageHeaderParsingException{
		LOGGER.debug("Using stun server '{}' for fast discovery", stunServer);
		ma = null;
		ca = null;
		nodeNatted = true;
		di = new DiscoveryInfo(sourceIaddress);
		
		Test1Thread t1t = new Test1Thread(this);
		t1t.start();
		try {
			t1t.join();
		} catch (InterruptedException e) {
			LOGGER.debug("Joining thread '{}' for public port discovery", t1t.getClass().getSimpleName());
		}

		return di;
	}
	
	private boolean test1() throws UtilityException, SocketException, UnknownHostException, IOException, MessageAttributeParsingException, MessageHeaderParsingException {
		int timeSinceFirstTransmission = 0;
		int timeout = timeoutInitValue;
		try (DatagramSocket socketTest1 = new DatagramSocket(new InetSocketAddress(sourceIaddress, sourcePort));) {
			while (true) {
				try {
					// Test 1 including response
//				socketTest1 = new DatagramSocket(new InetSocketAddress(sourceIaddress, sourcePort));
					socketTest1.setReuseAddress(true);
					socketTest1.connect(InetAddress.getByName(stunServer), stunServerPort);
					socketTest1.setSoTimeout(timeout);

					MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
					sendMH.generateTransactionID();

					ChangeRequest changeRequest = new ChangeRequest();
					sendMH.addMessageAttribute(changeRequest);

					byte[] data = sendMH.getBytes();
					DatagramPacket send = new DatagramPacket(data, data.length);
					socketTest1.send(send);
					LOGGER.debug("Test 1: Binding Request sent.");

					MessageHeader receiveMH = new MessageHeader();
					while (!(receiveMH.equalTransactionID(sendMH))) {
						DatagramPacket receive = new DatagramPacket(new byte[200], 200);
						socketTest1.receive(receive);
						receiveMH = MessageHeader.parseHeader(receive.getData());
						receiveMH.parseAttributes(receive.getData());
					}
					ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
					ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);
					ErrorCode ec = (ErrorCode) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ErrorCode);
					if (ec != null) {
						di.setError(ec.getResponseCode(), ec.getReason());
						LOGGER.debug("Message header contains an Errorcode message attribute.");
						return false;
					}
					if ((ma == null) || (ca == null)) {
						di.setError(700, "The server is sending an incomplete response (Mapped Address and Changed Address message attributes are missing). The client should not retry.");
						LOGGER.debug("Response does not contain a Mapped Address or Changed Address message attribute.");
						return false;
					} else {
						di.setPublicIP(ma.getAddress().getInetAddress());
						di.setPublicPort(ma.getPort());
						if ((ma.getPort() == socketTest1.getLocalPort()) && (ma.getAddress().getInetAddress().equals(socketTest1.getLocalAddress()))) {
							LOGGER.debug("Node is not natted.");
							nodeNatted = false;
						} else {
							LOGGER.debug("Node is natted.");
						}
						return true;
					}
				} catch (SocketTimeoutException ste) {
					if (timeSinceFirstTransmission < 300) {
						LOGGER.debug("Test 1: Socket timeout while receiving the response.");
						timeSinceFirstTransmission += timeout;
					} else {
						// node is not capable of udp communication
						LOGGER.debug("Test 1: Socket timeout while receiving the response. Maximum retry limit exceed. Give up.");
						di.setBlockedUDP();
						LOGGER.debug("Node is not capable of UDP communication.");
						return false;
					}
				}
			}
		}
	}
		

	public class Test1Thread extends Thread {
		private PublicPortDiscoveryTest fdt;
		private boolean returnTest1;
		
		public Test1Thread(PublicPortDiscoveryTest fdt) {
			this.fdt = fdt;
		}
		
		@Override
		public void run() {
			try {
				returnTest1 = fdt.test1();
			} catch (Exception e) {	
			}
		}
		
		public boolean getReturnTest1() {
			return returnTest1;
		}
	}
}
