package de.javawi.jstun.test.demo;

import de.javawi.jstun.util.Utility;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class AllDiscoveryRunnerDemo {
    static {
        Utility.confLogging();
    }
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AllDiscoveryRunnerDemo.class);

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
                            runDiscovery(new FastDiscoveryTestDemo(iaddress));
                            runDiscovery(new DiscoveryTestDemo(iaddress));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception in main: ", e);
        }
    }

    private static void runDiscovery(Runnable discoveryRunnable) {
        Thread thread = new Thread(discoveryRunnable);
        LOGGER.info("================================================================================");
        LOGGER.info("Starting {} discovery", discoveryRunnable.getClass().getSimpleName());
        long start = System.currentTimeMillis();
        thread.start();
        try {
            thread.join();
            LOGGER.info("With {} discovery took {} ms", discoveryRunnable.getClass().getSimpleName(), System.currentTimeMillis()-start);
        } catch (InterruptedException e) {
            // skip
        }
    }
}
