package client;

public class BotClient extends Client {

    @Override
    protected BotSocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        int x = (int) (Math.random() * 100);
        return "date_bot_" + x;
    }

    public class BotSocketThread extends SocketThread {
    }

    public static void main(String[] args) {
        Client client = new BotClient();
        client.run();
    }
}
