import com.rabbitmq.client.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

public class Server {
    public Server() {
        mMonitor = new Object();
    }

    public static void main(String[] args) {
        new Server().start("localhost");
    }

    private Connection mConnection;
    private Channel mChannel;
    private final Object mMonitor;

    private final List<Client> clients = new CopyOnWriteArrayList<>();
    private final List<Game> games = new CopyOnWriteArrayList<>();

    private void start(String host) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);

        try {
            mConnection = factory.newConnection();
            mChannel = mConnection.createChannel();

            initInputOutput();

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        waitForConnections();
    }

    private void initInputOutput() throws IOException {
        mChannel.queueDeclare(String.valueOf(MsgType.LOGIN_REQUEST), false, false, false, null);
        mChannel.basicConsume(String.valueOf(MsgType.LOGIN_REQUEST), false,
                this::onLoginRequest,
                consumerTag -> {
                });

        mChannel.exchangeDeclare(String.valueOf(MsgType.NEW_USER), "fanout");
        mChannel.exchangeDeclare(String.valueOf(MsgType.CHAT_NEW_MESSAGE), "fanout");


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_NEW), "fanout");
        String gameNew = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameNew, String.valueOf(MsgType.GAME_NEW), "");
        mChannel.basicConsume(gameNew, true,
                this::onGameNew,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_JOIN_ROOM), "fanout");
        String gameJoinRoom = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameJoinRoom, String.valueOf(MsgType.GAME_JOIN_ROOM), "");
        mChannel.basicConsume(gameJoinRoom, true,
                this::onGameJoinRoom,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_LEAVE_ROOM), "fanout");
        String gameLeaveRoom = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameLeaveRoom, String.valueOf(MsgType.GAME_LEAVE_ROOM), "");
        mChannel.basicConsume(gameLeaveRoom, true,
                this::onGameLeaveRoom,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_CANCEL_ROOM), "fanout");
        String gameCancelRoom = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameCancelRoom, String.valueOf(MsgType.GAME_CANCEL_ROOM), "");
        mChannel.basicConsume(gameCancelRoom, true,
                this::onGameCancelRoom,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_START), "fanout");
        String gameStart = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameStart, String.valueOf(MsgType.GAME_START), "");
        mChannel.basicConsume(gameStart, true,
                this::onGameStart,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.TANK_NEW), "fanout");
        String tankNew = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(tankNew, String.valueOf(MsgType.TANK_NEW), "");
        mChannel.basicConsume(tankNew, true,
                this::onTankNew,
                consumerTag -> {
                });

        mChannel.exchangeDeclare(String.valueOf(MsgType.TANK_MOVE), "fanout");
        String tankMove = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(tankMove, String.valueOf(MsgType.TANK_MOVE), "");
        mChannel.basicConsume(tankMove, true,
                this::onTankMove,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.MISSILE_NEW), "fanout");
        String missileNew = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(missileNew, String.valueOf(MsgType.MISSILE_NEW), "");
        mChannel.basicConsume(missileNew, true,
                this::onMissileNew,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.MISSILE_DEAD), "fanout");
        String missileDead = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(missileDead, String.valueOf(MsgType.MISSILE_DEAD), "");
        mChannel.basicConsume(missileDead, true,
                this::onMissileDead,
                consumerTag -> {
                });

        mChannel.exchangeDeclare(String.valueOf(MsgType.TANK_DEAD), "fanout");
        String tankDead = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameNew, String.valueOf(MsgType.TANK_DEAD), "");
        mChannel.basicConsume(tankDead, true,
                this::onTankDead,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_QUIT), "fanout");
        String gameQuit = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameQuit, String.valueOf(MsgType.GAME_QUIT), "");
        mChannel.basicConsume(gameQuit, true,
                this::onGameQuit,
                consumerTag -> {
                });


        mChannel.exchangeDeclare(String.valueOf(MsgType.GAME_LOSER), "fanout");
        String gameLoser = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(gameLoser, String.valueOf(MsgType.GAME_LOSER), "");
        mChannel.basicConsume(gameLoser,true,
                this::onGameLoser,
                consumerTag -> {
                });

        mChannel.exchangeDeclare(String.valueOf(MsgType.CLOSE_APP), "fanout");
        String closeApp = mChannel.queueDeclare().getQueue();
        mChannel.queueBind(closeApp, String.valueOf(MsgType.CLOSE_APP), "");
        mChannel.basicConsume(closeApp, true,
                this::onCloseApp,
                consumerTag -> {
                });
    }

    String readDelivery(Delivery delivery) {
        return new String(delivery.getBody(), StandardCharsets.UTF_8);
    }

    private void onLoginRequest(String consumerTag, Delivery delivery) throws IOException {

        for (Client c : clients) {
            System.out.println(c.username);
        }

        LoginRequest lreq = new LoginRequest(readDelivery(delivery));
        System.out.println("Server: login attempt with username " + lreq.username);

        boolean usernameExists = false;

        for (Client c : clients) {
            if (c.username.equals(lreq.username)) {
                usernameExists = true;
                break;
            }
        }

        LoginResponse lr;
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(delivery.getProperties().getCorrelationId())
                .build();

        if (usernameExists) {
            System.out.println("username " + lreq.username + " exists!");

            lr = new LoginResponse(false);

        } else {
            System.out.println("Adding client with name " + lreq.username);
            Client c = new Client(lreq.username);
            clients.add(c);
            lr = new LoginResponse(true);
        }

        mChannel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
                lr.Token().getBytes(StandardCharsets.UTF_8));

        mChannel.basicPublish("", delivery.getProperties().getReplyTo(),
                replyProps, tokenizeGameList().getBytes(StandardCharsets.UTF_8));

        mChannel.basicPublish("", delivery.getProperties().getReplyTo(),
                replyProps, tokenizeUserList().getBytes(StandardCharsets.UTF_8));

        mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        // RabbitMq consumer worker thread notifies the RPC server owner thread.
        synchronized (mMonitor) {
            mMonitor.notify();
        }
    }


    private void onGameNew(String consumerTag, Delivery delivery) throws IOException {
        Game gm = new Game(readDelivery(delivery));
        games.add(gm);
    }

    private void onGameJoinRoom(String consumerTag, Delivery delivery) throws IOException {
        GameJoinRoom jg = new GameJoinRoom(readDelivery(delivery));
        for (Game g : games) {
            if (jg.gameOwner.equals(g.owner)) {
                g.userCount++;
                break;
            }
        }

        for (Client c : clients) {
            if (c.username.equals(jg.username)) {
                c.currentGame = jg.gameOwner;
                break;
            }
        }
    }

    private void onGameLeaveRoom(String consumerTag, Delivery delivery) throws IOException {
        GameLeaveRoom wul = new GameLeaveRoom(readDelivery(delivery));
        for (Game g : games) {
            if (wul.gameOwner.equals(g.owner)) {
                g.userCount--;
                break;
            }
        }

        for (Client c : clients) {
            if (c.username.equals(wul.username)) {
                c.currentGame = "";
                break;
            }
        }
    }


    private void onGameCancelRoom(String consumerTag, Delivery delivery) throws IOException {
        GameCancelRoom wuc = new GameCancelRoom(readDelivery(delivery));
        games.removeIf(g -> g.owner.equals(wuc.gameOwner));

        for (Client c : clients) {
            if (c.currentGame.equals(wuc.gameOwner)) {
                c.currentGame = "";
            }
        }
    }

    private void onGameStart(String consumerTag, Delivery delivery) throws IOException {
        GameStart gs = new GameStart(readDelivery(delivery));
        for (Game g : games) {
            if (g.owner.equals(gs.gameOwner)) {
                g.started = true;
                break;
            }
        }
    }

    private void onTankNew(String consumerTag, Delivery delivery) throws IOException {
        Tank t = new Tank(readDelivery(delivery));
        // TODO only notify players
        //notifyPlayers(datagramPacket, datagramSocket, t.gameId);
    }

    private void onTankMove(String consumerTag, Delivery delivery) throws IOException {
        Tank t = new Tank(readDelivery(delivery));
        // TODO only notify players
        //notifyPlayers(datagramPacket, datagramSocket, t.gameId);
    }

    private void onTankDead(String consumerTag, Delivery delivery) throws IOException {
        TankDead td = new TankDead(readDelivery(delivery));
        // TODO only notify players
        //notifyPlayers(datagramPacket, datagramSocket, td.gameId);
    }


    private void onMissileNew(String consumerTag, Delivery delivery) throws IOException {
        Missile missile = new Missile(readDelivery(delivery));
        // TODO only notify players
        //notifyPlayers(datagramPacket, datagramSocket, td.gameId);

    }


    private void onMissileDead(String consumerTag, Delivery delivery) throws IOException {
        MissileDead md = new MissileDead(readDelivery(delivery));
        // TODO only notify players
        //notifyPlayers(datagramPacket, datagramSocket, td.gameId);
    }

    private void onGameLoser(String consumerTag, Delivery delivery) throws IOException {

        GameLoser gl = new GameLoser(readDelivery(delivery));
        for (Game g : games) {
            if (g.owner.equals(gl.gameID)) {
                g.loserCount++;
                break;
            }
        }

        for (Client c : clients) {
            if (c.currentGame.equals(gl.gameID) && c.username.equals(gl.username)) {
                c.loser = true;
                break;
            }
        }

        if (checkFinish(gl.gameID)) {
            finishGame(gl.gameID);
        }
    }

    private void onGameQuit(String consumerTag, Delivery delivery) throws IOException {
        GameQuit gq = new GameQuit(readDelivery(delivery));
        games.forEach(g -> {
            if (g.owner.equals(gq.gameID)) {
                g.userCount--;
            }
        });

        for (Client c : clients) {
            if (c.username.equals(gq.username)) {
                c.currentGame = "";
                break;
            }
        }

        if (checkFinish(gq.gameID)) {
            finishGame(gq.gameID);
        } else {
            // TODO NOTIFY need ?
        }
    }

    private void onCloseApp(String consumerTag, Delivery delivery) throws IOException {
        CloseApp ca = new CloseApp(readDelivery(delivery));
        clients.removeIf(client -> client.username.equals(ca.username));
    }

    public void finishGame(String gameID) {
        String winner = "";
        for (Client c : clients) {
            if (c.currentGame.equals(gameID)) {
                c.currentGame = "";
                if (!c.loser) {
                    winner = c.username;
                }
            }
        }

        games.removeIf(g -> g.owner.equals(gameID));

        GameEnd ge = new GameEnd(gameID, winner);

        send(MsgType.GAME_END, ge.Token());

    }


    boolean checkFinish(String gameID) {
        boolean finish = false;
        for (Game g : games) {
            if (g.owner.equals(gameID)) {
                if (g.loserCount + 1 == g.userCount) {
                    finish = true;
                }
                break;
            }
        }
        return finish;
    }

    public void send(int type, String token) {
        try {
            switch (type) {
                default -> mChannel.basicPublish(String.valueOf(type), "", null,
                        token.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void waitForConnections() {
        while (true) {
            synchronized (mMonitor) {
                try {
                    mMonitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String tokenizeUserList() {
        String delimiter = "%ddd%";
        StringBuilder list = new StringBuilder();
        for (Client c : clients) {
            User u = new User(c.username);
            list.append(delimiter).append(u.Token());
        }

        return list.toString();
    }

    String tokenizeGameList() {
        String delimiter = "%ddd%";
        StringBuilder list = new StringBuilder();
        for (Game g : games) {
            list.append(delimiter).append(g.Token());
        }

        return list.toString();
    }

    public static class Client {
        int udpPort;
        int connectionID;
        String IP;
        String currentGame;
        String username;
        boolean loser;

        Client(String username) {
            this.username = username;
        }

        Client(String IP, int udpPort, int connectionID) {
            this.IP = IP;
            this.udpPort = udpPort;
            this.connectionID = connectionID;
            this.username = "";
            this.currentGame = "";
            loser = false;
        }
    }




    /*
    private static int connectionID = 0;
    static final int UDP_SERVER_PORT = 4243;
    private static final int TCP_SERVER_PORT = 4242;
    private final List<Client> clients = new CopyOnWriteArrayList<>();
    private final List<Game> games = new CopyOnWriteArrayList<>();

    private void start() {
        new Thread(new UDPThread()).start();
        try {
            ServerSocket serverSocket = new ServerSocket(TCP_SERVER_PORT);
            do {
                Socket socket = serverSocket.accept();
                String socketIP = socket.getInetAddress().getHostAddress();

                DataInputStream input = new DataInputStream(socket.getInputStream());

                int socketUdpPort = input.readInt();

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeInt(++connectionID);

                clients.add(new Client(socketIP, socketUdpPort, connectionID));
                System.out.println("new client with connection id " + connectionID);

                dataOutputStream.flush();
                dataOutputStream.close();
                socket.close();

            } while (!serverSocket.isClosed());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }

    private class UDPThread implements Runnable {
        byte[] buffer = new byte[1024];

        private void sendText(DataOutputStream out, String txt) throws IOException {
            byte[] by = txt.getBytes(StandardCharsets.UTF_8);
            out.writeInt(by.length);
            out.write(by);
        }

        String readToken(DataInputStream in) throws IOException {
            int length = in.readInt();
            byte[] array = new byte[length];
            in.read(array);
            return new String(array, StandardCharsets.UTF_8);
        }

        public void send(DatagramSocket datagramSocket, String IP, int udpPort, int msgType, String token) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            try {
                dataOutputStream.writeInt(msgType);
                sendText(dataOutputStream, token);

                dataOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            try {
                datagramSocket.send(new DatagramPacket(byteArray, byteArray.length, new InetSocketAddress(IP, udpPort)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                DatagramSocket datagramSocket = new DatagramSocket(UDP_SERVER_PORT);
                do {
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(datagramPacket);

                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer, 0, datagramPacket.getLength()));

                    int msgType = dataInputStream.readInt();
                    switch (msgType) {

                        case MsgType.NEW_USER:
                            User u = new User(readToken(dataInputStream));
                            notifyAll(datagramPacket, datagramSocket);
                            break;

                        case MsgType.GAME_NEW:
                            Game gm = new Game(readToken(dataInputStream));
                            games.add(gm);
                            notifyAll(datagramPacket, datagramSocket);
                            break;

                        case MsgType.GAME_JOIN_ROOM:
                            GameJoinRoom jg = new GameJoinRoom(readToken(dataInputStream));
                            for (Game g : games) {
                                if (jg.gameOwner.equals(g.owner)) {
                                    g.userCount++;
                                    break;
                                }
                            }

                            for (Client c : clients) {
                                if (c.username.equals(jg.username)) {
                                    c.currentGame = jg.gameOwner;
                                    break;
                                }
                            }

                            notifyAll(datagramPacket, datagramSocket);
                            break;


                        case MsgType.GAME_LEAVE_ROOM:
                            GameLeaveRoom wul = new GameLeaveRoom(readToken(dataInputStream));
                            for (Game g : games) {
                                if (wul.gameOwner.equals(g.owner)) {
                                    g.userCount--;
                                    break;
                                }
                            }

                            for (Client c : clients) {
                                if (c.username.equals(wul.username)) {
                                    c.currentGame = "";
                                    break;
                                }
                            }

                            notifyAll(datagramPacket, datagramSocket);
                            break;

                        case MsgType.GAME_CANCEL_ROOM:
                            GameCancelRoom wuc = new GameCancelRoom(readToken(dataInputStream));
                            games.removeIf(g -> g.owner.equals(wuc.gameOwner));

                            for (Client c : clients) {
                                if (c.currentGame.equals(wuc.gameOwner)) {
                                    c.currentGame = "";
                                }
                            }

                            notifyAll(datagramPacket, datagramSocket);
                            break;

                        case MsgType.GAME_START:
                            GameStart gs = new GameStart(readToken(dataInputStream));
                            for (Game g : games) {
                                if (g.owner.equals(gs.gameOwner)) {
                                    g.started = true;
                                    break;
                                }
                            }

                            notifyAll(datagramPacket, datagramSocket);
                            break;

                        case MsgType.TANK_NEW:

                        case MsgType.TANK_MOVE:
                            Tank t = new Tank(readToken(dataInputStream));
                            notifyPlayers(datagramPacket, datagramSocket, t.gameId);
                            break;
                        case MsgType.TANK_DEAD:
                            TankDead td = new TankDead(readToken(dataInputStream));
                            notifyPlayers(datagramPacket, datagramSocket, td.gameID);
                            break;

                        case MsgType.MISSILE_NEW:
                            Missile missile = new Missile(readToken(dataInputStream));
                            notifyPlayers(datagramPacket, datagramSocket, missile.gameID);
                            break;

                        case MsgType.MISSILE_DEAD:
                            MissileDead md = new MissileDead(readToken(dataInputStream));

                            notifyPlayers(datagramPacket, datagramSocket, md.gameId);
                            break;

                        case MsgType.GAME_LOSER:
                            GameLoser gl = new GameLoser(readToken(dataInputStream));
                            for (Game g : games) {
                                if (g.owner.equals(gl.gameID)) {
                                    g.loserCount++;
                                    break;
                                }
                            }

                            for (Client c : clients) {
                                if (c.currentGame.equals(gl.gameID) && c.username.equals(gl.username)) {
                                    c.loser = true;
                                    break;
                                }
                            }

                            if (checkFinish(gl.gameID)) {
                                finishGame(datagramSocket, gl.gameID);
                            }

                            break;

                        case MsgType.GAME_QUIT:
                            GameQuit gq = new GameQuit(readToken(dataInputStream));
                            games.forEach(g -> {
                                if (g.owner.equals(gq.gameID)) {
                                    g.userCount--;
                                }
                            });

                            for (Client c : clients) {
                                if (c.username.equals(gq.username)) {
                                    c.currentGame = "";
                                    break;
                                }
                            }

                            if (checkFinish(gq.gameID)) {
                                finishGame(datagramSocket, gq.gameID);
                            } else {
                                notifyAll(datagramPacket, datagramSocket);
                            }

                            break;

                        case MsgType.CLOSE_APP:
                            CloseApp ca = new CloseApp(readToken(dataInputStream));
                            clients.removeIf(client -> client.connectionID == ca.connectionID);
                            notifyAll(datagramPacket, datagramSocket);
                            break;

                        default:
                            notifyAll(datagramPacket, datagramSocket);
                    }

                } while (!datagramSocket.isClosed());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String tokenizeUserList() {
            String delimiter = "%ddd%";
            StringBuilder list = new StringBuilder();
            for (Client c : clients) {
                User u = new User(c.username);
                list.append(delimiter).append(u.Token());
            }

            return list.toString();
        }

        String tokenizeGameList() {
            String delimiter = "%ddd%";
            StringBuilder list = new StringBuilder();
            for (Game g : games) {
                list.append(delimiter).append(g.Token());
            }

            return list.toString();
        }

        boolean checkFinish(String gameID) {
            boolean finish = false;
            for (Game g : games) {
                if (g.owner.equals(gameID)) {
                    if (g.loserCount + 1 == g.userCount) {
                        finish = true;
                    }
                    break;
                }
            }
            return finish;
        }

        public void finishGame(DatagramSocket datagramSocket, String gameID) {
            String winner = "";
            for (Client c : clients) {
                if (c.currentGame.equals(gameID)) {
                    c.currentGame = "";
                    if (!c.loser) {
                        winner = c.username;
                    }
                }
            }

            games.removeIf(g -> g.owner.equals(gameID));

            GameEnd ge = new GameEnd(gameID, winner);

            for (Client c : clients) {
                send(datagramSocket, c.IP, c.udpPort, MsgType.GAME_END, ge.Token());
            }
        }

        public void notifyAll(DatagramPacket datagramPacket, DatagramSocket datagramSocket) throws IOException {
            for (Client client : clients) {
                datagramPacket.setSocketAddress(new InetSocketAddress(client.IP, client.udpPort));
                datagramSocket.send(datagramPacket);
            }
        }

        public void notifyPlayers(DatagramPacket datagramPacket, DatagramSocket datagramSocket, String gameID) throws IOException {
            for (Client client : clients) {
                if (client.currentGame.equals(gameID)) {
                    datagramPacket.setSocketAddress(new InetSocketAddress(client.IP, client.udpPort));
                    datagramSocket.send(datagramPacket);
                }
            }
        }
    }

    public static class Client {
        int udpPort;
        int connectionID;
        String IP;
        String currentGame;
        String username;
        boolean loser;

        Client(String IP, int udpPort, int connectionID) {
            this.IP = IP;
            this.udpPort = udpPort;
            this.connectionID = connectionID;
            this.username = "";
            this.currentGame = "";
            loser = false;
        }
    }

     */
}