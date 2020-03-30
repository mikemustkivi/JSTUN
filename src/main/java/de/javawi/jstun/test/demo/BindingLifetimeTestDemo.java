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

import de.javawi.jstun.test.BindingLifetimeTest;
import de.javawi.jstun.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingLifetimeTestDemo {
    static {
		Utility.confLogging();
	}
	private static Logger LOGGER = LoggerFactory.getLogger(BindingLifetimeTestDemo.class);

	public static void main(String args[]) {
		try {
			BindingLifetimeTest test = new BindingLifetimeTest(Utility.getStunServerName(), 3478);
			test.test();
			boolean continueWhile = true;
			while(continueWhile) {
				Thread.sleep(5000);
				if (test.getLifetime() != -1) {
				    String msg = "Lifetime: " + test.getLifetime() + " Finished: " + test.isCompleted();
				    LOGGER.info(msg);
					if (test.isCompleted()) continueWhile = false;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in main", e);
		}
	}
}
