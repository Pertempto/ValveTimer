package models;

public class Timer {
    private String id;
    private String name;
    private int end;
    private int lastSeen;

    public Timer(String id, String name, int end, int lastSeen) {
        this.id = id;
        this.name = name;
        this.end = end;
        this.lastSeen = lastSeen;
    }

    public int getLength() {
        int now = (int) (System.currentTimeMillis() / 1000);
        if (end > now) {
            return end - now;
        }
        return 0;
    }

    public int getTimeSinceLastSeen() {
        int now = (int) (System.currentTimeMillis() / 1000);
        return now - lastSeen;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getEnd() {
        return end;
    }

    public int getLastSeen() {
        return lastSeen;
    }


}
