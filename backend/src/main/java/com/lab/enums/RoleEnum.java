package com.lab.enums;

public enum RoleEnum {

    STUDENT(1L, "STUDENT", "学生"),
    LAB_ADMIN(2L, "LAB_ADMIN", "实验室管理员"),
    SUPER_ADMIN(3L, "SUPER_ADMIN", "超级管理员");

    private final Long id;
    private final String code;
    private final String name;

    RoleEnum(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static RoleEnum fromId(Long id) {
        if (id == null) {
            return null;
        }
        for (RoleEnum role : values()) {
            if (role.id.equals(id)) {
                return role;
            }
        }
        return null;
    }
}
