package io.github.barmoury.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class UserDetails<T> implements org.springframework.security.core.userdetails.UserDetails {

    T data;
    String id;
    @Setter String authorityPrefix = "ROLE_";
    List<String> authoritiesValues = new ArrayList<>();

    public UserDetails(String id, List<String> authoritiesValues, T data) {
        this.id = id;
        this.data = data;
        this.authoritiesValues = authoritiesValues;
    }

    public UserDetails(String id, String authoritiesValue, T data) {
        this.id = id;
        this.data = data;
        this.authoritiesValues.add(authoritiesValue);
    }

    public UserDetails(String id, List<String> authoritiesValues) {
        this.id = id;
        this.authoritiesValues = authoritiesValues;
    }

    public UserDetails(String id, T data) {
        this.id = id;
        this.data = data;
    }

    public UserDetails(String id) {
        this(id, new ArrayList<>(), null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        authoritiesValues.forEach(authoritiesValue ->
                list.add(new SimpleGrantedAuthority(authorityPrefix + authoritiesValue)));
        return list;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

}
