package de.javawi.jstun.test.demo;

import de.javawi.jstun.logging.Logger;
import de.javawi.jstun.logging.LoggerFactory;
import de.javawi.jstun.util.Utility;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AllDiscoveryRunnerDemo {
    static {
        Utility.confLogging();
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(AllDiscoveryRunnerDemo.class);

    public static void main(String args[]) {
        while (true) {
            discover();
            waitInMs(500);
        }
    }

    private static void discover() {
        try {
            for (InetAddress iaddress : getInetAddressList()) {
                runDiscovery(new PublicPortDiscoveryTestDemo(iaddress));
            }
        } catch (Exception e) {
            LOGGER.error("Exception in main: ", e);
        }
    }

    private static List<InetAddress> getInetAddressList() throws SocketException, ClassNotFoundException {
        List<InetAddress> inetAddressList = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if (Class.forName("java.net.Inet4Address").isInstance(iaddress)) {
                    String localHostAddress = iaddress.getHostAddress();
                    LOGGER.debug("local host aadress {} interface name {}", localHostAddress,
                            NetworkInterface.getByInetAddress(iaddress).getName());
                    if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress()) &&
                            NetworkInterface.getByInetAddress(iaddress).getName().toLowerCase().startsWith("en")) {
                        inetAddressList.add(iaddress);
                    }
                }
            }
        }
        return inetAddressList;
    }

    private static void waitInMs(long waitInMs) {
        LOGGER.debug("wait {} ms", waitInMs);
        try {
            Thread.sleep(waitInMs);
        } catch (InterruptedException e) {
            // skip
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
