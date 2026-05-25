package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class PermissionConfigurationTest {
    @Test
    void fallsBackToAllowWhenNoGroupsAreConfigured() {
        PermissionConfiguration configuration = PermissionConfiguration.empty();

        assertTrue(configuration.canRead(authentication("some-user")));
        assertTrue(configuration.canBuild(authentication("some-user")));
    }

    @Test
    void checksReadAndBuildGroups() {
        PermissionConfiguration configuration = new PermissionConfiguration(
                List.of("GA_Gretl_Datenportal_Read"),
                List.of("GA_Gretl_Datenportal_AFU"));

        assertTrue(configuration.canRead(authentication("GA_Gretl_Datenportal_Read")));
        assertFalse(configuration.canBuild(authentication("GA_Gretl_Datenportal_Read")));
        assertTrue(configuration.canBuild(authentication("GA_Gretl_Datenportal_AFU")));
    }

    @Test
    void parsesYamlPermissions() {
        PermissionConfiguration configuration = PermissionConfiguration.fromYaml(Map.of(
                "read", List.of("GA_Gretl_Datenportal_Read", "GA_Gretl_Datenportal_Read"),
                "build", List.of("GA_Gretl_Datenportal_AFU")));

        assertTrue(configuration.hasReadRestrictions());
        assertTrue(configuration.hasBuildRestrictions());
        assertTrue(configuration.getReadGroups().contains("GA_Gretl_Datenportal_Read"));
        assertTrue(configuration.getBuildGroups().contains("GA_Gretl_Datenportal_AFU"));
    }

    private Authentication authentication(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "user",
                "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
