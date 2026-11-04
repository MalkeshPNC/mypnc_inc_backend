package com.pnc.masters.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tblemployee_group_users")
@IdClass(EmployeeGroupMember.Key.class)
public class EmployeeGroupMember {

    @Id
    @Column(name = "g_id")
    private Long groupId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public static class Key implements Serializable {
        private Long groupId;
        private Long userId;

        public Key() {
        }

        public Key(Long groupId, Long userId) {
            this.groupId = groupId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(groupId, key.groupId) && Objects.equals(userId, key.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, userId);
        }
    }
}
