package ch.admin.bit.jeap.governance.rules.dependency.versions;

import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
public class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern SEMVER_PATTERN = Pattern.compile("(?<major>\\d+)(\\.(?<minor>\\d+))?(\\.(?<patch>\\d+))?.*");

    int major;
    int minor;
    int patch;

    public static SemanticVersion parse(String version) {
        if (version.endsWith("-SNAPSHOT")) {
            throw InvalidDependencyVersionException.snapshot(version);
        }

        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (matcher.matches()) {
            try {
                int major = Integer.parseInt(matcher.group("major"));
                int minor = getIntGroupIfMatchedOtherwiseZero(matcher, "minor");
                int patch = getIntGroupIfMatchedOtherwiseZero(matcher, "patch");
                return new SemanticVersion(major, minor, patch);
            } catch (NumberFormatException _) {
                throw InvalidDependencyVersionException.unparsableVersion(version);
            }
        } else {
            throw InvalidDependencyVersionException.unparsableVersion(version);
        }
    }

    private static int getIntGroupIfMatchedOtherwiseZero(Matcher matcher, String groupName) {
        String strValue = matcher.group(groupName);
        if (strValue == null) {
            return 0;
        }
        return Integer.parseInt(strValue);
    }

    @Override
    public int compareTo(SemanticVersion semanticVersion) {
        int compare = Integer.compare(major, semanticVersion.major);
        if (compare != 0) {
            return compare;
        }
        compare = Integer.compare(minor, semanticVersion.minor);
        if (compare != 0) {
            return compare;
        }
        return Integer.compare(patch, semanticVersion.patch);
    }

    @Override
    public String toString() {
        return "%s.%s.%s".formatted(major, minor, patch);
    }
}
