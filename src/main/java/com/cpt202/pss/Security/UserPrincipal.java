package com.cpt202.pss.security;

import com.cpt202.pss.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Integer userId;
    private final String username;
    private final String password;
    private final User.Role role;
    private final boolean active;

    public UserPrincipal(User u) {
        this.userId   = u.getUserId();
        this.username = u.getUsername();
        this.password = u.getPassword();
        this.role     = u.getRole();
        this.active   = u.getStatus() == User.Status.ACTIVE;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }
}
