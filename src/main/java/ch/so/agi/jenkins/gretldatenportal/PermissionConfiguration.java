package ch.so.agi.jenkins.gretldatenportal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;

public final class PermissionConfiguration {
    private final List<String> readGroups;
    private final List<String> buildGroups;

    public PermissionConfiguration(List<String> readGroups, List<String> buildGroups) {
        this.readGroups = normalize(readGroups);
        this.buildGroups = normalize(buildGroups);
    }

    public static PermissionConfiguration empty() {
        return new PermissionConfiguration(List.of(), List.of());
    }

    public static PermissionConfiguration fromYaml(Map<String, Object> root) {
        if (root == null || root.isEmpty()) {
            return empty();
        }
        return new PermissionConfiguration(
                YamlSupport.asList(root.get("read")).stream().map(Object::toString).toList(),
                YamlSupport.asList(root.get("build")).stream().map(Object::toString).toList());
    }

    public boolean hasReadRestrictions() {
        return !readGroups.isEmpty();
    }

    public boolean hasBuildRestrictions() {
        return !buildGroups.isEmpty();
    }

    public boolean canRead(Authentication authentication) {
        return !hasReadRestrictions() || hasAny(authentication, readGroups) || hasAny(authentication, buildGroups);
    }

    public boolean canBuild(Authentication authentication) {
        return !hasBuildRestrictions() || hasAny(authentication, buildGroups);
    }

    public List<String> getReadGroups() {
        return readGroups;
    }

    public List<String> getBuildGroups() {
        return buildGroups;
    }

    private static boolean hasAny(Authentication authentication, List<String> groups) {
        if (authentication == null || groups.isEmpty()) {
            return false;
        }
        Set<String> authorities = new LinkedHashSet<>();
        authentication.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));
        return groups.stream().anyMatch(authorities::contains);
    }

    private static List<String> normalize(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String group : groups) {
            if (group != null && !group.isBlank()) {
                normalized.add(group.trim());
            }
        }
        return List.copyOf(normalized);
    }
}
