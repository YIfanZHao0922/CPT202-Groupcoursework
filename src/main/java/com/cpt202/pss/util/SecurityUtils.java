package com.cpt202.pss.util;

import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            throw new BusinessException(401, "Not authenticated");
        }
        return up;
    }

    public static Integer currentUserId() { return currentPrincipal().getUserId(); }

    public static User.Role currentRole()  { return currentPrincipal().getRole(); }
}
