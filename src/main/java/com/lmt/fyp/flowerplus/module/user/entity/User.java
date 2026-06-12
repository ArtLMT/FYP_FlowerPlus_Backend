package com.lmt.fyp.flowerplus.module.user.entity;

import com.lmt.fyp.flowerplus.common.entity.TimestampEntity;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entity mapping to the user_account database table.
 * Inherits UUID primary key and timestamp columns from TimestampEntity.
 */
@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends TimestampEntity implements UserDetails {

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * We use email as the security principal (username) for Spring Security.
     */
    @Override
    public String getUsername() {
        return email;
    }

    // Returning user username from DB if needed
    public String getActualUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserAccountStatus.INACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserAccountStatus.SUSPENDED && status != UserAccountStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserAccountStatus.ACTIVE;
    }
}
