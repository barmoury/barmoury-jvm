package io.github.barmoury.api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

@Getter
public class UserDetails<T> implements org.springframework.security.core.userdetails.UserDetails {

    T data;
    String id;
    String language = "en";
    @Setter String authorityPrefix = "ROLE_";
    List<String> authoritiesValues = new ArrayList<>();

    public UserDetails(String id, List<String> authoritiesValues, T data, String language) {
        this.id = id;
        this.data = data;
        this.language = language;
        this.authoritiesValues = authoritiesValues;
    }

    public UserDetails(String id, List<String> authoritiesValues, T data) {
        this.id = id;
        this.data = data;
        this.authoritiesValues = authoritiesValues;
    }

    public UserDetails(String id, List<String> authoritiesValues) {
        this.id = id;
        this.authoritiesValues = authoritiesValues;
    }

    public UserDetails(String id, T data) {
        this.id = id;
        this.data = data;
    }

    public UserDetails(String id, T data, String language) {
        this.id = id;
        this.data = data;
        this.language = language;
    }

    public UserDetails(String id) {
        this(id, new ArrayList<>(), null);
    }

    public Locale getLocale() {
        if (this.language == null) return Locale.ENGLISH;
        return Locale.forLanguageTag(this.language);
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

    @SuppressWarnings("unchecked")
    public static <T> UserDetails<T> fromPrincipal(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return (UserDetails<T>) userDetails;
        }
        return null;
    }

    public static <T> UserDetails<T> fromAuthentication(Authentication authentication) {
        return fromPrincipal(Objects.requireNonNull(authentication).getPrincipal());
    }

    public static <T> T getUserDetailsDataFromAuthentication(Authentication authentication) {
        UserDetails<T> userDetails = fromAuthentication(authentication);
        return userDetails.getData();
    }

}
