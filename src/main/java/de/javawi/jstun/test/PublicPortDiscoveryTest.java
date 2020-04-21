package de.javawi.jstun.test;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.ErrorCode;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.exception.ClientException;
import de.javawi.jstun.exception.TimeoutException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.logging.Logger;
import de.javawi.jstun.logging.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PublicPortDiscoveryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicPortDiscoveryTest.class);
    InetAddress sourceIaddress;
    int sourcePort;
    String stunServer;
    int stunServerPort;
    int timeoutInitValue = 100; //ms

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

    public DiscoveryInfo test() throws ExecutionException, InterruptedException {
        LOGGER.debug("Using stun server '{}' for public port discovery", stunServer);

        try {
            LOGGER.debug("local host aadress {} interface name {}", sourceIaddress.getHostName(),
                    NetworkInterface.getByInetAddress(sourceIaddress).getName());
        } catch (SocketException e) {
            LOGGER.error("Error during getting Inet address", e);
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<DiscoveryInfo> future = executorService.submit(new PublicPortDiscoveryCallable(this));

        try {
            return future.get();
        } finally {
            executorService.shutdown();
        }
    }

    private DiscoveryInfo test1() throws TimeoutException, ClientException, SocketException {
        DiscoveryInfo discoveryInfo = new DiscoveryInfo(sourceIaddress);
        int timeout = timeoutInitValue;
        MappedAddress ma = null;
        ChangedAddress ca = null;
        boolean nodeNatted = true;
        long start = System.currentTimeMillis();
        try (DatagramSocket socketTest1 = new DatagramSocket(new InetSocketAddress(sourceIaddress, sourcePort))) {
            while (true) {
                try {
                    // Test 1 including response
                    socketTest1.setReuseAddress(true);
                    socketTest1.connect(InetAddress.getByName(stunServer), stunServerPort);

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
                        LOGGER.debug("Test 1: Socket timeout is in {} ms", timeout);
                        long startWhile = System.currentTimeMillis();
                        socketTest1.setSoTimeout(timeout);
                        socketTest1.receive(receive);
                        LOGGER.info("socketTest1 received in {} ms", System.currentTimeMillis() - startWhile);
                        receiveMH = MessageHeader.parseHeader(receive.getData());
                        receiveMH.parseAttributes(receive.getData());
                    }
                    ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
                    ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);
                    ErrorCode ec = (ErrorCode) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ErrorCode);
                    if (ec != null) {
                        discoveryInfo.setError(ec.getResponseCode(), ec.getReason());
                        LOGGER.debug("Message header contains an Errorcode message attribute.");
                        throw new ClientException("Message header contains an Errorcode message attribute.");
                    }
                    if ((ma == null) || (ca == null)) {
                        discoveryInfo.setError(700, "The server is sending an incomplete response (Mapped Address and Changed Address message attributes are missing). The client should not retry.");
                        LOGGER.debug("Response does not contain a Mapped Address or Changed Address message attribute.");
                        throw new ClientException("Response does not contain a Mapped Address or Changed Address message attribute.");
                    } else {
                        discoveryInfo.setPublicIP(ma.getAddress().getInetAddress());
                        discoveryInfo.setPublicPort(ma.getPort());
                        if ((ma.getPort() == socketTest1.getLocalPort()) && (ma.getAddress().getInetAddress().equals(socketTest1.getLocalAddress()))) {
                            LOGGER.debug("Node is not natted.");
                            nodeNatted = false;
                        } else {
                            LOGGER.debug("Node is natted.");
                        }
                        return discoveryInfo;
                    }
                } catch (SocketTimeoutException ste) {
                    long methodExecutionTime = System.currentTimeMillis() - start;
                    LOGGER.debug("Test 1: execution time so far {} ms", methodExecutionTime);
                    if (methodExecutionTime < 9501) {
                        int oldTimeout = timeout;
                        timeout = getNextTimeout(timeout);
                        LOGGER.debug("Test 1: Socket timeout in {} ms while receiving the response. New timout {} ms",
                                oldTimeout, timeout);
                    } else {
                        // node is not capable of udp communication
                        LOGGER.debug("Test 1: Socket maximum timeout encountered.");
                        discoveryInfo.setBlockedUDP();
                        LOGGER.debug("Node is not capable of UDP communication.");
                        throw new TimeoutException("Timeout exceeded");
                    }
                } catch (Exception ex) {
                    throw new ClientException(ex);
                }
            }
        }
    }

    private int getNextTimeout(int currentTimeout) {
        if (currentTimeout < 101) {
            return 200;
        } else if (currentTimeout < 301) {
            return 400;
        } else if (currentTimeout < 701) {
            return 800;
        } else {
            return 1600;
        }
    }

    public class PublicPortDiscoveryCallable implements Callable<DiscoveryInfo> {
        PublicPortDiscoveryTest publicPortDiscoveryTest;

        public PublicPortDiscoveryCallable(PublicPortDiscoveryTest publicPortDiscoveryTest) {
            this.publicPortDiscoveryTest = publicPortDiscoveryTest;
        }

        @Override
        public DiscoveryInfo call() throws Exception {
            return this.publicPortDiscoveryTest.test1();
        }
    }
}
