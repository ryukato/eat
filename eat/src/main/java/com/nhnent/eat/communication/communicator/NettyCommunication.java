package com.nhnent.eat.communication.communicator;

import co.paralleluniverse.fibers.SuspendExecution;
import com.nhnent.eat.common.Config.Config;
import com.nhnent.eat.common.PacketClassPool;
import com.nhnent.eat.communication.netty.NettyClient;
import com.nhnent.eat.entity.PacketReceiver;
import com.nhnent.eat.entity.ScenarioUnit;
import com.nhnent.eat.entity.ScenarioUnitType;
import com.nhnent.eat.handler.PacketJsonHandler;
import com.nhnent.eat.handler.ReportHandler;
import com.nhnent.eat.packets.StreamPacket;
import javafx.util.Pair;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.netty.handler.timeout.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nhnent.eat.Main.userInfo;


/**
 * Communication for Netty socket
 */
public class NettyCommunication implements IBaseCommunication {

    private NettyClient nettyClient;
    private PacketReceiver packetReceiver;
    private final PacketJsonHandler packetJsonHandler;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReportHandler reportHandler;

    private final String userId;
    private final int actorIndex;
    
    private Config config;

    public NettyCommunication(String userId, int actorIndex, Config config) {
        this.config = config;
        this.userId = userId;
        this.actorIndex = actorIndex;

        nettyClient = new NettyClient(config);
        packetReceiver = new PacketReceiver(userId);
        packetJsonHandler = new PacketJsonHandler(config);
        reportHandler  = new ReportHandler(config);
        nettyClient.setPacketListener(packetReceiver);


        if (config.getDisplay().isDisplayTransferredPacket()) {
            logger.info("Connect to server");
        }
    }

    private void loadConfig() {
        this.config = Config.builder().create();
    }

    @Override
    public Boolean isRegisteredScenarioType(String scenarioType) {

        if (scenarioType.equals(ScenarioUnitType.Request)
                || scenarioType.equals(ScenarioUnitType.Response))
            return Boolean.TRUE;
        else
            return Boolean.FALSE;

    }

    @Override
    public void execute(ScenarioUnit scenarioUnit) throws SuspendExecution {
        if (scenarioUnit.type.equals(ScenarioUnitType.Disconnect)) {
            nettyClient.disconnect();
            return;
        }

        if (scenarioUnit.type.equals(ScenarioUnitType.Connect)) {
            nettyClient.startUp(actorIndex);
            return;
        }

        try {
            byte[] packet;

            if (scenarioUnit.name.contains(".")) {
                packet = StreamPacket.obj().jsonToPacket(userId, scenarioUnit.type,
                        scenarioUnit.packageName, scenarioUnit.name.split("\\.")[1], scenarioUnit.json);
            } else {
                packet = StreamPacket.obj().jsonToPacket(userId, scenarioUnit.type,
                        scenarioUnit.packageName, scenarioUnit.name, scenarioUnit.json);
            }


            if (!nettyClient.getConnected()) nettyClient.startUp(actorIndex);

            nettyClient.transferPacket(packet);

            reportHandler.displayTransferredPacket(scenarioUnit.type, scenarioUnit.name,
                    scenarioUnit.json);
        } catch (Exception e) {
            logger.error("Exception is raised : {}\n", ExceptionUtils.getStackTrace(e));
        }

    }

    @Override
    public Boolean compareWithRealResponse(ScenarioUnit scenarioUnit) throws SuspendExecution, InterruptedException {
        //All packet classes are inner class of 'com.nhnent.tardis.protocol.Base'
        //To declare inner(or nested) class, we should use '$' instead of '.'
        //ex)'com.nhnent.tardis.protocol.Base$AuthenticationReq'

        String fullClassNameOfPck = scenarioUnit.packageName + "."
                + scenarioUnit.name.replace(".", "$");

        Class<?> clsPck = PacketClassPool.obj().findClassByName(fullClassNameOfPck);

        Object responsePck;
        Boolean compareResult = Boolean.FALSE;

        try {
            responsePck = receivePacket(clsPck);
        } catch (Exception e) {
            logger.error("Exception is raised : {}\n", ExceptionUtils.getStackTrace(e));
            return Boolean.FALSE;
        }

        if (responsePck == null)
            return Boolean.FALSE;

        try {
            if (config.getDisplay().isDisplayUnitTestResult()) {
                compareResult = packetJsonHandler.matchPacket(userId, scenarioUnit.json, responsePck);
            } else {
                //To display statistics information, set to true
                compareResult = Boolean.TRUE;
            }

            reportHandler.displayReceivedPacket(scenarioUnit.type, scenarioUnit.json, scenarioUnit.name,
                    responsePck);

            reportHandler.writeLogForUnitResult(userInfo.get(), scenarioUnit.name, scenarioUnit.json,
                    responsePck, compareResult);
        } catch (Exception e) {
            logger.error("Exception is raised : {}\n", ExceptionUtils.getStackTrace(e));
        }


        return compareResult;
    }

    //------------------------- for Custom API -----------------------------------------------
    @Override
    public Pair<String, byte[]> readPacket() throws SuspendExecution, InterruptedException {
        return readRealResponse();
    }

    @Override
    public void transferPacket(byte[] sendPck) throws SuspendExecution, InterruptedException {
        nettyClient.transferPacket(sendPck);
    }
    //----------------------------------------------------------------------------------------

    public Pair<String, byte[]> readRealResponse() throws SuspendExecution, InterruptedException {
        Pair<String, byte[]> realResponsePacket = null;

        try {
            realResponsePacket = packetReceiver.readPacket();
            if (config.getDisplay().isDisplayTransferredPacket()) {
                logger.info("realResponsePacket : {}", realResponsePacket.getKey());
            }
        } catch (Exception e) {
            logger.error("Exception is raised : {}\n", ExceptionUtils.getStackTrace(e));
        }

        return realResponsePacket;
    }


    /**
     * Receive packet from server
     *
     * @param clsPck Class type of receiving packet
     * @return Received object which formatted given class type
     */
    private Object receivePacket(final Class<?> clsPck) throws SuspendExecution, InterruptedException {
        Object objReceivePacket;
        Pair<String, byte[]> packet = null;

        try {
            packet = readRealResponse();

            PacketClassPool classes = PacketClassPool.obj();

            String receivedPacketName = packet.getKey();
            String clsPckGetName = clsPck.getName();

            //Value of clsPck.getName() is like 'com.nhnent.tardis.common.protocol.Base$AuthenticationRes',
            //So remove prefix till '$'

            String expectedPacketName;
            if (clsPckGetName.contains("$")) {
                expectedPacketName = clsPckGetName.split("\\$")[1];
            } else {
                String[] splitString = clsPckGetName.split("\\.");
                expectedPacketName = splitString[splitString.length - 1];
            }

            if (!receivedPacketName.equals(expectedPacketName)) {
                if (config.getCommon().isIgnoreUnnecessaryPacket()) {
                    logger.debug("[userId:{}]ignore received packet: {}", userInfo.get(), receivedPacketName);
                    objReceivePacket = receivePacket(clsPck);
                } else {
                    throw new Exception("Received packet is different with expectation.\nExpect:" + expectedPacketName
                            + "\nReceived:" + receivedPacketName);
                }
            } else {
                objReceivePacket = StreamPacket.obj().decodePacket(userId, clsPck, packet.getValue());
            }
        } catch (TimeoutException e) {
            logger.error("Timeout is raised. Receive Packet for [{}]", clsPck.getName());
            return null;
        } catch (Exception e) {
            logger.error("Exception is raised. Receive Packet\nName:{}\nValue{}\n{}", clsPck.getName(),
                    packet.getValue(), ExceptionUtils.getStackTrace(e));
            return null;
        }
        return objReceivePacket;
    }

}
