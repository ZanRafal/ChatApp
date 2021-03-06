package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Input server port:");
        int port = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port)){
            ConsoleHelper.writeMessage("The server is currently running...");

            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("An error has occurred while starting or running the server..");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("New connection established with " + socket.getRemoteSocketAddress());

            String userName = null;
            try (Connection connection = new Connection(socket)) {
                //store new Username
                userName = serverHandshake(connection);

                //Inform all chat members that new participant has joined
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));

                //inform all chat members about other existing participants
                notifyUsers(connection, userName);

                //Process participant's messages
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("An error occurred while communicating with remote address.");
            }

            if(userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }

            ConsoleHelper.writeMessage("Closed the connection with User " + socket.getRemoteSocketAddress());
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));

                Message message = connection.receive();
                if(message.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("server.Message received from: " + socket.getRemoteSocketAddress() + ". The message type does not match the protocol.");
                    continue;
                }

                String userName = message.getData();

                if(userName.isEmpty()) {
                    ConsoleHelper.writeMessage("Error! Attempting to connect to the server using an empty name from: " + socket.getRemoteSocketAddress());
                    continue;
                }

                if(connectionMap.containsKey(userName)) {
                    ConsoleHelper.writeMessage("There was an attempt to connect to the server using a name that is already being used from: " + socket.getRemoteSocketAddress());
                    continue;
                }
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));

                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for(String name : connectionMap.keySet()) {
                if(name.equals(userName))
                    continue;
                connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if(message.getType() == MessageType.TEXT) {
                    String textMessage = message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + textMessage));
                } else {
                    ConsoleHelper.writeMessage("server.Message received from " + socket.getRemoteSocketAddress() + ". The message does not match the protocol.");
                }
            }
        }

        public static void sendBroadcastMessage(Message message) {
            for(Connection connection : connectionMap.values()) {
                try {
                    connection.send(message);
                } catch (IOException e) {
                    ConsoleHelper.writeMessage("server.Message couldn't be sent to" + connection.getRemoteSocketAddress());
                }
            }
        }
    }
}