# Plugin Mechanism

The governance service provides several extension points, which are explained in this chapter.

Plugin beans must be registered in the Spring context using autoconfiguration. Create a configuration class
annotated with `@AutoConfiguration` that declares your plugin beans, and register it in
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Data Import

Instances of the governance service can collect their own data. To do so, the governance service offers an interface:

```java
public interface DataSourceImporter {

    /**
     * Imports data from the external data source into the governance system.
     */
    void importData();

}
```

**Implementation:**
1. Implement the `DataSourceImporter` interface
2. Add the implementation as a Spring bean
3. It will be automatically included in the data import process

**Example:**

```java
@Component
public class CustomDataImporter implements DataSourceImporter {
    
    @Override
    public void importData() {
        // Your custom import logic here
    }
}
```

## Data Deletion

If you hold data in instances of the governance service that reference SystemComponents, you can be notified before a
SystemComponent is deleted. This is necessary to perform certain data cleanup operations. You implement an interface
for this:

```java
/**
 * Should be annotated with {@code org.springframework.core.annotation.Order} to define the sort order
 */
public interface ComponentDeletionListener {

    /**
     * This method is called before a component identified by {@code systemComponentId} is deleted.
     * Implementations should perform any necessary cleanup or data removal related to the component.
     *
     * @param systemComponentId the unique identifier of the component to be deleted
     */
    void preComponentDeletion(long systemComponentId);

}
```

**Implementation:**
1. Implement the `ComponentDeletionListener` interface
2. Optionally annotate with `@Order` to control execution sequence
3. Add the implementation as a Spring bean
4. It will be automatically included in the deletion process

**Example:**

```java
@Component
public class CustomCleanupListener implements ComponentDeletionListener {

  @Override
  public void preComponentDeletion(long systemComponentId) {
    // Cleanup logic before component deletion
  }
}
```

If you hold data in instances of the governance service that reference Systems, you can be notified before a System is
deleted. This is necessary to perform certain data cleanup operations. You implement an interface for this:

```java
public interface SystemDeletionListener {

    /**
     * This method is called before a system identified by {@code systemId} is deleted.
     * Implementations should perform any necessary cleanup or data removal related to the system.
     *
     * @param systemId the unique identifier of the system to be deleted
     */
    void preSystemDeletion(long systemId);

}
```

**Implementation:**
1. Implement the `SystemDeletionListener` interface
2. Optionally annotate with `@Order` to control execution sequence
3. Add the implementation as a Spring bean
4. It will be automatically included in the deletion process

## See also

- [Rules](rules.md) — implementing custom governance rules.
- [Architecture](architecture.md) — module overview.
