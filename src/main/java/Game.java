public class Game {
    String owner;
    int tourCount;
    int userCount;
    int loserCount;
    boolean started;
    String delimiter = "#d#";

    Game(String owner, int tourCount, int playerSize, boolean started) {
        this.owner = owner;
        this.tourCount = tourCount;
        this.userCount = playerSize;
        this.started = started;
        this.loserCount = 0;
    }

    Game(String token) {
        System.out.println("game token: " + token);
        String[] fields = token.split(delimiter);
        this.owner = fields[0];
        this.tourCount = Integer.parseInt(fields[1]);
        this.userCount = Integer.parseInt(fields[2]);
        this.started = Boolean.parseBoolean(fields[3]);
    }

    String Token() {
        return this.owner + this.delimiter +
                this.tourCount + this.delimiter +
                this.userCount + this.delimiter +
                this.started;
    }
}