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

package de.javawi.jstun.util;

import de.javawi.jstun.test.demo.DiscoveryTestDemo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Utility {
    public static void confLogging() {
		try {
			Handler fh = new FileHandler("logging.txt");
			fh.setFormatter(new SimpleFormatter());
			java.util.logging.Logger.getLogger("de.javawi.jstun").addHandler(fh);
			java.util.logging.Logger.getLogger("de.javawi.jstun").setLevel(Level.ALL);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getStunServerName() {
    	final String STUN_SERVER_KEY = "stun.server.name";
		String stunServerName = System.getProperty(STUN_SERVER_KEY, null);
		return (stunServerName != null)
				? stunServerName
				: loadPropertiesFromResource().getProperty(STUN_SERVER_KEY);
	}

	private static Properties loadPropertiesFromResource() {
		Properties properties = new Properties();
		try (InputStream stream = DiscoveryTestDemo.class.getClassLoader().
				getResourceAsStream("jstun.properties")) {
			properties.load(stream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
    }

	public static final byte integerToOneByte(int value) throws UtilityException {
		if ((value > Math.pow(2,15)) || (value < 0)) {
			throw new UtilityException("Integer value " + value + " is larger than 2^15");
		}
		return (byte)(value & 0xFF);
	}
	
	public static final byte[] integerToTwoBytes(int value) throws UtilityException {
		byte[] result = new byte[2];
		if ((value > Math.pow(2,31)) || (value < 0)) {
			throw new UtilityException("Integer value " + value + " is larger than 2^31");
		}
        result[0] = (byte)((value >>> 8) & 0xFF);
        result[1] = (byte)(value & 0xFF);
		return result; 
	}
	
	public static final byte[] integerToFourBytes(int value) throws UtilityException {
		byte[] result = new byte[4];
		if ((value > Math.pow(2,63)) || (value < 0)) {
			throw new UtilityException("Integer value " + value + " is larger than 2^63");
		}
        result[0] = (byte)((value >>> 24) & 0xFF);
		result[1] = (byte)((value >>> 16) & 0xFF);
		result[2] = (byte)((value >>> 8) & 0xFF);
        result[3] = (byte)(value & 0xFF);
		return result; 
	}
	
	public static final int oneByteToInteger(byte value) throws UtilityException {
		return (int)value & 0xFF;
	}
	
	public static final int twoBytesToInteger(byte[] value) throws UtilityException {
		if (value.length < 2) {
			throw new UtilityException("Byte array too short!");
		}
        int temp0 = value[0] & 0xFF;
        int temp1 = value[1] & 0xFF;
        return ((temp0 << 8) + temp1);
	}
	
	public static final long fourBytesToLong(byte[] value) throws UtilityException {
		if (value.length < 4) {
			throw new UtilityException("Byte array too short!");
		}
        int temp0 = value[0] & 0xFF;
        int temp1 = value[1] & 0xFF;
		int temp2 = value[2] & 0xFF;
		int temp3 = value[3] & 0xFF;
        return (((long)temp0 << 24) + (temp1 << 16) + (temp2 << 8) + temp3);
	}	                                      
}
