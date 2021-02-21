# multiple-ds-user-storage: Keycloak User Storage Provider with multiple JPA datasources support

When migrating multiple legacy auth systems to Keycloak it's useful to read credentials from an existing database 
(that being MySQL, Oracle, PostgreSQL, etc) in an on-demand way.

Keycloak quick-start [user-storage-jpa](https://github.com/keycloak/keycloak-quickstarts/tree/latest/user-storage-jpa) 
provides a great starting point for this, but no indications on how to read from
multiple/dynamic JPA datasources.

This projects aims to provide a Keycloak User Storage Provider able to use different databases.
As an example:
* User Storage Provider A => reading from DB-A
* User Storage Provider B => reading from DB-B
* User Storage Provider C => reading from DB-C

### System Requirements

You need to have <span>Keycloak</span> running.

All you need to build this project is Java SDK 11.0 (Java SDK 1) or later and Maven 3.3.3 or later.

### Pre-requisite - create the WildFly datasources

You must first create the WildFly datasource that will be used. You can do this from WildFly admin interface (http://localhost:9990/console) 
or programmatically like below. Remember to replace required variables (in uppercase, like `DATASOURCENAME`)

    /subsystem=datasources/data-source=DATASOURCENAME: add(jndi-name=java:jboss/datasources/DATASOURCENAME,enabled=true,jta=false,use-java-context=true,use-ccm=true, connection-url=jdbc:mysql://${env.DB_ADDR:mysql}:${env.DB_PORT:3306}/DATASOURCEDB${env.JDBC_PARAMS:}, driver-name=mysql)
    /subsystem=datasources/data-source=DATASOURCENAME: write-attribute(name=user-name, value=${env.DB_USER:keycloak})
    /subsystem=datasources/data-source=DATASOURCENAME: write-attribute(name=password, value=${env.DB_PASSWORD:password})
    /subsystem=datasources/data-source=DATASOURCENAME: write-attribute(name=check-valid-connection-sql, value="SELECT 1")
    /subsystem=datasources/data-source=DATASOURCENAME: write-attribute(name=background-validation-millis, value=60000)
    /subsystem=datasources/data-source=DATASOURCENAME: write-attribute(name=flush-strategy, value=IdleConnections)

### Build and Deploy

1. compile with `mvn package`
2. copy `target/multiple-ds-user-storage.jar` to `<keycloak>/deployments/` folder

### Enable the Provider for a Realm

Login to the <span>Keycloak</span> Admin Console and got to the User Federation tab.   You should now see your deployed provider in the add-provider list box.
Add the provider, save it.  This will now enable the provider for the 'master' realm.

### More Information

* password validation can be customized in `MultipleDSUserStorageProvider.isValid` (by default: salted SHA-1)
* password update can be customized in `MultipleDSUserStorageProvider.updateCredential`
* once password is changed in Keycloak, the legacy password is not used anymore
* once password is changed in Keycloak, it's persisted in the legacy DB as well - this to provide a fallback if Keycloak implementation must be rolled-back
