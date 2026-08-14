package com.cluecompanion.game;
public record Card(Type type, String name) { public enum Type { SUSPECT, WEAPON, ROOM } }
