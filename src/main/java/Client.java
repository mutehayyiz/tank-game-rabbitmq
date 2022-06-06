import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Client {

    TankGame tankGame;
    private Connection mConnection;
    private Channel mChannel;

    String connectionID = "id";
    private String IP;


    Client(TankGame tankGame) {
        this.tankGame = tankGame;
    }

    boolean connect(String IP) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP);

        try {
            mConnection = factory.newConnection();
            mChannel = mConnection.createChannel();

            initInputOutput();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    void initInputOutput() throws IOException {

        String newUser = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(newUser, String.valueOf(MsgType.NEW_USER), "");
        mChannel.basicConsume(newUser, true,
                this::onNewUser,
                consumerTag -> {
                });


        // chat message
        String chatMessage = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(chatMessage, String.valueOf(MsgType.CHAT_NEW_MESSAGE), "");
        mChannel.basicConsume(chatMessage, true,
                this::onChatNewMessage,
                consumerTag -> {
                });

        // gameNew
        String gameNew = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameNew, String.valueOf(MsgType.GAME_NEW), "");
        mChannel.basicConsume(gameNew, true,
                this::onGameNew,
                consumerTag -> {
                });


        //gameJoinRoom
        String gameJoinRoom = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameJoinRoom, String.valueOf(MsgType.GAME_JOIN_ROOM), "");
        mChannel.basicConsume(gameJoinRoom, true,
                this::onGameJoinRoom,
                consumerTag -> {
                });

        //gameLeaveRoom
        String gameLeaveRoom = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameJoinRoom, String.valueOf(MsgType.GAME_LEAVE_ROOM), "");
        mChannel.basicConsume(gameLeaveRoom, true,
                this::onGameLeaveRoom,
                consumerTag -> {
                });

        //gameCancelRoom
        String gameCancelRoom = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameCancelRoom, String.valueOf(MsgType.GAME_CANCEL_ROOM), "");
        mChannel.basicConsume(gameCancelRoom, true,
                this::onGameCancelRoom,
                consumerTag -> {
                });

        // gameStart
        String gameStart = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameStart, String.valueOf(MsgType.GAME_START), "");
        mChannel.basicConsume(gameStart, true,
                this::onGameStart,
                consumerTag -> {
                });

        //tank new
        String tankNew = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(tankNew, String.valueOf(MsgType.TANK_NEW), "");
        mChannel.basicConsume(tankNew, true,
                this::onTankNew,
                consumerTag -> {
                });

        // tank move
        String tankMove = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(tankMove, String.valueOf(MsgType.TANK_MOVE), "");
        mChannel.basicConsume(tankMove, true,
                this::onTankMove,
                consumerTag -> {
                });

        // missile new
        String missileNew = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(missileNew, String.valueOf(MsgType.MISSILE_NEW), "");
        mChannel.basicConsume(missileNew, true,
                this::onMissileNew,
                consumerTag -> {
                });

        // missile dead
        String missileDead = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(missileDead, String.valueOf(MsgType.MISSILE_DEAD), "");
        mChannel.basicConsume(missileDead, true,
                this::onMissileDead,
                consumerTag -> {
                });

        // tank dead
        String tankDead = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(tankDead, String.valueOf(MsgType.TANK_DEAD), "");
        mChannel.basicConsume(tankDead, true,
                this::onTankDead,
                consumerTag -> {
                });

        // game quit
        String gameQuit = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameQuit, String.valueOf(MsgType.GAME_QUIT), "");
        mChannel.basicConsume(gameQuit, true,
                this::onGameQuit,
                consumerTag -> {
                });

        // game end
        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_END), "topic");
        String gameEnd = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameEnd, String.valueOf(MsgType.GAME_END), "");
        mChannel.basicConsume(gameEnd, true,
                this::onGameEnd,
                consumerTag -> {
                });

        // cloae app
        String closeApp = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(closeApp, String.valueOf(MsgType.CLOSE_APP), "");
        mChannel.basicConsume(closeApp, true,
                this::onCloseApp,
                consumerTag -> {
                });
    }

    private void onNewUser(String consumerTag, Delivery delivery) {
        User u = new User(readDelivery(delivery));
        System.out.println("new user " + u.username);
        tankGame.homePanel.handleNewUser(u);

    }

    private void onChatNewMessage(String consumerTag, Delivery delivery) {
        ChatMessage ch = new ChatMessage(readDelivery(delivery));
        tankGame.homePanel.handleChatMessage(ch);
    }

    private void onGameNew(String consumerTag, Delivery delivery) {
        Game game = new Game(readDelivery(delivery));
        tankGame.homePanel.handleGameNew(game);
    }

    private void onGameJoinRoom(String consumerTag, Delivery delivery) {
        GameJoinRoom jg = new GameJoinRoom(readDelivery(delivery));
        tankGame.homePanel.handleGameJoinRoom(jg.username, jg.gameOwner);
    }

    private void onGameLeaveRoom(String consumerTag, Delivery delivery) {
        GameLeaveRoom lg = new GameLeaveRoom(readDelivery(delivery));
        tankGame.homePanel.handleGameLeaveRoom(lg.username, lg.gameOwner);
    }

    private void onGameCancelRoom(String consumerTag, Delivery delivery) {
        GameCancelRoom cg = new GameCancelRoom(readDelivery(delivery));
        tankGame.homePanel.handleGameCancelRoom(cg.gameOwner);
    }

    private void onGameStart(String consumerTag, Delivery delivery) {
        GameStart sg = new GameStart(readDelivery(delivery));
        tankGame.homePanel.handleGameStart(sg.gameOwner);
    }

    private void onTankNew(String consumerTag, Delivery delivery) {
        Tank tank = new Tank(readDelivery(delivery));
        tankGame.warPanel.handleNewTank(tank);
    }

    private void onTankMove(String consumerTag, Delivery delivery) {
        Tank tankMove = new Tank(readDelivery(delivery));
        tankGame.warPanel.handleTankMove(tankMove);
    }

    private void onMissileNew(String consumerTag, Delivery delivery) {
        Missile missile = new Missile(readDelivery(delivery));
        tankGame.warPanel.handleNewMissile(missile);
    }

    private void onMissileDead(String consumerTag, Delivery delivery) {
        MissileDead md = new MissileDead(readDelivery(delivery));
        tankGame.warPanel.handleMissileDead(md);
    }

    private void onTankDead(String consumerTag, Delivery delivery) {
        TankDead td = new TankDead(readDelivery(delivery));
        tankGame.warPanel.handleTankDead(td);
    }

    private void onGameQuit(String consumerTag, Delivery delivery) {
        GameQuit gq = new GameQuit(readDelivery(delivery));
        tankGame.homePanel.handleGameQuit(gq);
    }

    private void onGameEnd(String consumerTag, Delivery delivery) {
        GameEnd ge = new GameEnd(readDelivery(delivery));
        tankGame.homePanel.handleGameEnd(ge);
    }

    private void onCloseApp(String consumerTag, Delivery delivery) {
        CloseApp ce = new CloseApp(readDelivery(delivery));
        tankGame.homePanel.handleCloseApp(ce);
    }


    private BlockingQueue<String> connectRPC(String token) throws IOException {

        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = mChannel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        // Publish the user with the pseudo on the server side.
        mChannel.basicPublish("", String.valueOf(MsgType.LOGIN_REQUEST), props,
                token.getBytes(StandardCharsets.UTF_8));


        // Get the response.
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(3);

        mChannel.basicConsume(replyQueueName, true,
                (consumerTag, delivery) ->
                {
                    if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                        response.offer(readDelivery(delivery));
                    }
                },
                consumerTag -> {
                }
        );

        return response;
    }

    String readDelivery(Delivery delivery) {
        return new String(delivery.getBody(), StandardCharsets.UTF_8);
    }

    void sendLoginRequest(String token) {
        try {
            BlockingQueue<String> response = connectRPC(token);

            String resp1 = response.take();
            System.out.println("response is ");
            System.out.println(resp1);
            LoginResponse loginResponse = new LoginResponse(resp1);

            tankGame.handleLoginResponse(loginResponse.loggedIn);

            if (loginResponse.loggedIn) {
                System.out.println("success");
                tankGame.homePanel.handleGameList(response.take());
                tankGame.homePanel.handleUserList(response.take());
            }

            System.out.println("handling login response");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(int type, String token) {
        try {
            switch (type) {
                case MsgType.LOGIN_REQUEST -> sendLoginRequest(token);
                default -> mChannel.basicPublish(String.valueOf(type), "", null,
                        token.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /*
    private class UDPReceiveThread implements Runnable {
        byte[] buffer = new byte[1024];

        public void run() {
            while (datagramSocket != null) {
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    datagramSocket.receive(datagramPacket);
                    parse(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void parse(DatagramPacket datagramPacket) {
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer, 0, datagramPacket.getLength()));
            try {
                int msgType = dataInputStream.readInt();
                switch (msgType) {



                    case MsgType.NEW_USER:
                        User u = new User(readToken(dataInputStream));
                        tankGame.homePanel.handleNewUser(u);
                        break;
                    case MsgType.CHAT_NEW_MESSAGE:
                        ChatMessage ch = new ChatMessage(readToken(dataInputStream));
                        tankGame.homePanel.handleChatMessage(ch);
                        break;

                    case MsgType.GAME_NEW:
                        Game game = new Game(readToken(dataInputStream));
                        tankGame.homePanel.handleGameNew(game);
                        break;

                    case MsgType.GAME_JOIN_ROOM:
                        GameJoinRoom jg = new GameJoinRoom(readToken(dataInputStream));
                        tankGame.homePanel.handleGameJoinRoom(jg.username, jg.gameOwner);
                        break;

                    case MsgType.GAME_LEAVE_ROOM:
                        GameLeaveRoom lg = new GameLeaveRoom(readToken(dataInputStream));
                        tankGame.homePanel.handleGameLeaveRoom(lg.username, lg.gameOwner);
                        break;

                    case MsgType.GAME_CANCEL_ROOM:
                        GameCancelRoom cg = new GameCancelRoom(readToken(dataInputStream));
                        tankGame.homePanel.handleGameCancelRoom(cg.gameOwner);
                        break;

                    case MsgType.GAME_START:
                        GameStart sg = new GameStart(readToken(dataInputStream));
                        tankGame.homePanel.handleGameStart(sg.gameOwner);
                        break;

                    case MsgType.TANK_NEW:
                        Tank tank = new Tank(readToken(dataInputStream));
                        tankGame.warPanel.handleNewTank(tank);
                        break;

                    case MsgType.TANK_MOVE:
                        Tank tankMove = new Tank(readToken(dataInputStream));
                        tankGame.warPanel.handleTankMove(tankMove);
                        break;

                    case MsgType.MISSILE_NEW:
                        Missile missile = new Missile(readToken(dataInputStream));
                        tankGame.warPanel.handleNewMissile(missile);
                        break;

                    case MsgType.MISSILE_DEAD:
                        MissileDead md = new MissileDead(readToken(dataInputStream));
                        tankGame.warPanel.handleMissileDead(md);
                        break;

                    case MsgType.TANK_DEAD:
                        TankDead td = new TankDead(readToken(dataInputStream));
                        tankGame.warPanel.handleTankDead(td);
                        break;

                    case MsgType.GAME_QUIT:
                        GameQuit gq = new GameQuit(readToken(dataInputStream));
                        tankGame.homePanel.handleGameQuit(gq);
                        break;

                    case MsgType.GAME_END:
                        GameEnd ge = new GameEnd(readToken(dataInputStream));
                        tankGame.homePanel.handleGameEnd(ge);
                        break;

                    case MsgType.CLOSE_APP:
                        CloseApp ce = new CloseApp(readToken(dataInputStream));
                        tankGame.homePanel.handleCloseApp(ce);
                        break;
                }
            } catch (
                    IOException e) {
                e.printStackTrace();
            }
        }
    }



     */
}