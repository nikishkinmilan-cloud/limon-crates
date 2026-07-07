package com.limonanarchy.holograms;

import java.util.ArrayList;
import java.util.List;

public class HologramData {

    public enum Type {
        STATIC,   // обычный текст, который задаёшь сам (над шахтой, над кейсами)
        RICHEST,  // авто-обновляемый топ богачей
        PVP       // авто-обновляемый топ по PvP-убийствам
    }

    private final String name;
    private String world;
    private double x, y, z;
    private Type type;
    private List<String> lines;

    public HologramData(String name, String world, double x, double y, double z, Type type, List<String> lines) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Type getType() {
        return type;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public void setLocation(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
