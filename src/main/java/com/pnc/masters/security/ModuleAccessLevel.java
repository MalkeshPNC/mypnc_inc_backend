package com.pnc.masters.security;

public enum ModuleAccessLevel {
    NONE,
    READ,
    WRITE;

    public boolean allows(ModuleAccessLevel required) {
        return ordinal() >= required.ordinal();
    }

    public static ModuleAccessLevel higher(ModuleAccessLevel left, ModuleAccessLevel right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
