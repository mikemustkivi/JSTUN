package de.javawi.jstun.test.demo;

import de.javawi.jstun.logging.Logger;
import de.javawi.jstun.logging.LoggerFactory;
import de.javawi.jstun.util.Utility;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class AllDiscoveryRunnerDemo {
    static {
        Utility.confLogging();
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(AllDiscoveryRunnerDemo.class);

    public static void main(String args[]) {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
                while (iaddresses.hasMoreElements()) {
                    InetAddress iaddress = iaddresses.nextElement();
                    if (Class.forName("java.net.Inet4Address").isInstance(iaddress)) {
                        String localHostAddress = iaddress.getHostAddress();
                        if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress()
                            && !localHostAddress.startsWith("10."))) {
                            runDiscovery(new PublicPortDiscoveryTestDemo(iaddress));
//                            runDiscovery(new FastDiscoveryTestDemo(iaddress));
//                            runDiscovery(new DiscoveryTestDemo(iaddress));
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
