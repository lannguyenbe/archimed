<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.dspace</groupId>
   <artifactId>dspace-lni</artifactId>
   <packaging>war</packaging>
   <name>DSpace LNI</name>
   <description>
      DSpace Lightweight Network Interface Build Modules
   </description>

   <parent>
      <groupId>org.dspace</groupId>
      <artifactId>dspace-parent</artifactId>
      <version>5.0</version>
      <relativePath>..</relativePath>
   </parent>

    <properties>
        <!-- This is the path to the root [dspace-src] directory. -->
        <root.basedir>${basedir}/..</root.basedir>
    </properties>

    <build>
        <filters>
            <!-- Filter using the properties file defined by dspace-parent POM -->
            <filter>${filters.file}</filter>
        </filters>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <configuration>
                    <attachClasses>true</attachClasses>
                    <!-- In version 2.1-alpha-1, this was incorrectly named warSourceExcludes -->
                    <packagingExcludes>WEB-INF/lib/*.jar</packagingExcludes>
                    <warSourceExcludes>WEB-INF/lib/*.jar</warSourceExcludes>
                    <webResources>
                        <resource>
                            <filtering>true</filtering>
                            <directory>${basedir}/src/main/webapp</directory>
                            <includes>
                                <include>WEB-INF/web.xml</include>
                            </includes>
                        </resource>
                    </webResources>
                </configuration>
                <executions>
                    <execution>
                        <phase>prepare-package</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>oracle-support</id>
            <activation>
                <property>
                    <name>db.name</name>
                    <value>oracle</value>
                </property>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>com.oracle</groupId>
                    <artifactId>ojdbc6</artifactId>
                </dependency>
            </dependencies>
        </profile>
    </profiles>

    <dependencies>
        <dependency>
            <groupId>org.dspace</groupId>
            <artifactId>dspace-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dspace</groupId>
            <artifactId>dspace-api-lang</artifactId>
        </dependency>
        <dependency>
            <groupId>wsdl4j</groupId>
            <artifactId>wsdl4j</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-discovery</groupId>
            <artifactId>commons-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </dependency>
        <dependency>
            <groupId>javax.xml</groupId>
            <artifactId>jaxrpc-api</artifactId>
        </dependency>
        <dependency>
            <groupId>axis</groupId>
            <artifactId>axis</artifactId>
        </dependency>
        <dependency>
            <groupId>axis</groupId>
            <artifactId>axis-ant</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>axis</groupId>
            <artifactId>axis-saaj</artifactId>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

</project>