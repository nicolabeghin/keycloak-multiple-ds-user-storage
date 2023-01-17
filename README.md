# multiple-ds-user-storage
## Keycloak User Storage Provider with multiple JPA datasources support
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
![CI](https://github.com/nicolabeghin/keycloak-multiple-ds-user-storage/actions/workflows/maven.yml/badge.svg)

* tested on Keycloak 19.0.3 and later 
* Java SDK 11.0 or later 
* * Maven 3.3.3 or later

### Pre-requisite - create the WildFly datasources

You must first create the Quarkus datasource that will be used. You can add them in `<keycloak>/conf/quarkus.properties`:

    quarkus.datasource.user-store.db-kind=h2
    quarkus.datasource.user-store.username=sa
    quarkus.datasource.user-store.jdbc.url=jdbc:h2:mem:user-store;DB_CLOSE_DELAY=-1`

    quarkus.datasource.user-store2.db-kind=h2
    quarkus.datasource.user-store2.username=sa
    quarkus.datasource.user-store2.jdbc.url=jdbc:h2:mem:user-store;DB_CLOSE_DELAY=-1`

**Please note: `quarkus.properties` is not the one in the extension, but in the Keycloak server `conf` folder**

Differently from the previously JBoss-based Keycloak, in Quarkus 
you also need to explicitly maintain datasources in [`src/main/resources/META-INF/persistence.xml`](https://github.com/nicolabeghin/keycloak-multiple-ds-user-storage/blob/master/src/main/resources/META-INF/persistence.xml) 
by adding a corresponding  `<persistence-unit>` for each datasource.

    <persistence-unit name="user-store" transaction-type="JTA">
        <class>org.keycloak.multipleds.storage.user.entities.UserEntity</class>
        <properties>
            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQLDialect"/>
            <!-- Sets the name of the datasource to be the same as the datasource name in quarkus.properties-->
            <property name="hibernate.connection.datasource" value="user-store"/>
            <property name="javax.persistence.transactionType" value="JTA"/>
            <property name="hibernate.hbm2ddl.auto" value="none"/>
            <property name="hibernate.show_sql" value="false"/>
        </properties>
    </persistence-unit>
    <persistence-unit name="user-store2" transaction-type="JTA">
        <class>org.keycloak.multipleds.storage.user.entities.UserEntity</class>
        <properties>
            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQLDialect"/>
            <!-- Sets the name of the datasource to be the same as the datasource name in quarkus.properties-->
            <property name="hibernate.connection.datasource" value="user-store2"/>
            <property name="javax.persistence.transactionType" value="JTA"/>
            <property name="hibernate.hbm2ddl.auto" value="none"/>
            <property name="hibernate.show_sql" value="false"/>
        </properties>
    </persistence-unit>

### Build and Deploy

Compile with `mvn package` or, if you have Docker `make package`. Then copy `target/multiple-ds-user-storage.jar` to `<keycloak>/providers/` folder

### Enable the Provider instances for a Realm

1. Login to the <span>Keycloak</span> Admin Console
2. access User Federation tab
3. add a new User Storage provider instance selecting `multiple-ds-user-storage` from the list box
 
![image](https://user-images.githubusercontent.com/2743637/108629459-41874a80-7460-11eb-9b28-4b930f554ae0.png)

![image](https://user-images.githubusercontent.com/2743637/108629352-bc039a80-745f-11eb-9445-3fc6f7eb91f3.png)

5. save and repeat for as many datasources you need

![image](https://user-images.githubusercontent.com/2743637/108629322-937ba080-745f-11eb-8a89-63e530a352cf.png)
### More Information

* password validation can be customized in `MultipleDSUserStorageProvider.isValid` (by default: salted SHA-1)
* password update can be customized in `MultipleDSUserStorageProvider.updateCredential`
* once password is changed in Keycloak, the legacy password is not used anymore
* once password is changed in Keycloak, it's persisted in the legacy DB as well - this to provide a fallback if Keycloak implementation must be rolled-back
