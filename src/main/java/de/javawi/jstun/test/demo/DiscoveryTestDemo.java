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

package de.javawi.jstun.test.demo;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import de.javawi.jstun.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class DiscoveryTestDemo implements Runnable {
	static {
		Utility.confLogging();
	}
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryTestDemo.class);
	InetAddress iaddress;
	int port;

	public DiscoveryTestDemo(InetAddress iaddress, int port) {
		this.iaddress = iaddress;
		this.port = port;
	}
	
	public DiscoveryTestDemo(InetAddress iaddress) {
		this.iaddress = iaddress;
		this.port = 0;
	}
	
	public void run() {		
		try {
			DiscoveryTest test = new DiscoveryTest(iaddress, port, Utility.getStunServerName(), 3478);
			DiscoveryInfo discoveryInfo = test.test();
			LOGGER.info("Result of discovery {}", discoveryInfo);
			LOGGER.info("Discovered public port is {}", discoveryInfo.getPublicPort());
		} catch (BindException be) {
			LOGGER.error("BindException with " + iaddress.toString(), be);
		} catch (Exception e) {
			LOGGER.error("Exception in run: ", e);
		}
	}
	
	public static void main(String args[]) {
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
				while (iaddresses.hasMoreElements()) {
					InetAddress iaddress = iaddresses.nextElement();
					if (Class.forName("java.net.Inet4Address").isInstance(iaddress)) {
						if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress())) {
							Thread thread = new Thread(new DiscoveryTestDemo(iaddress));
							thread.start();
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception in main: ", e);
		}
	}
}
