package dev.tarekakel.udi.common.security;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns XSUAA scopes such as {@code udi-assistant!t12345.Editor} into the authority {@code Editor}.
 * Scopes belonging to other applications (or generic ones like {@code openid}) are ignored.
 */
class XsuaaScopeAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String SCOPE_CLAIM = "scope";

    private final String prefix;

    XsuaaScopeAuthoritiesConverter(String xsappname) {
        this.prefix = xsappname + ".";
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> scopes = jwt.getClaimAsStringList(SCOPE_CLAIM);
        if (scopes == null) {
            return List.of();
        }
        return scopes.stream()
                .filter(scope -> scope.startsWith(prefix))
                .map(scope -> scope.substring(prefix.length()))
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }
}
