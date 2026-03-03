package ch.admin.bit.jeap.governance.rules.dependency.versions;

class InvalidDependencyVersionException extends RuntimeException {
    private InvalidDependencyVersionException(String message) {
        super(message);
    }

    static InvalidDependencyVersionException unparsableVersion(String version) {
        return new InvalidDependencyVersionException("Component seems to use an un-parsable version number: " + version);
    }

    static InvalidDependencyVersionException snapshot(String version) {
        return new InvalidDependencyVersionException("Component uses Snapshot version: " + version);
    }
}
