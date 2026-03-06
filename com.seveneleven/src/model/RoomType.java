package model;

public enum RoomType {
    SINGLE,
    DOUBLE,
    SUITE;

    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}