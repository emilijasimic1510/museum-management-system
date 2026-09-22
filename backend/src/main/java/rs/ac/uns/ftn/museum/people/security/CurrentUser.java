package rs.ac.uns.ftn.museum.people.security;

import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.*;

public final class CurrentUser {
    private CurrentUser() {}
    public static Long id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) throw new AuthenticationCredentialsNotFoundException("Authentication required");
        try { return Long.valueOf(a.getName()); } catch (NumberFormatException ex) { throw new BadCredentialsException("Invalid identity"); }
    }
    public static boolean hasRole(String role) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_"+role));
    }
}
