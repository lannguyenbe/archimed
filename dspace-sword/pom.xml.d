<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.dspace</groupId>
   <artifactId>dspace-sword</artifactId>
   <packaging>war</packaging>
   <name>DSpace SWORD</name>
   <description>
      DSpace SWORD Deposit Service Provider Web Application
   </description>

   <!--
      A Parent POM that Maven inherits DSpace Default
      POM attributes from.
   -->
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

        <!-- Leave this out for the moment, as the source is in the tree -->
        <!--
        <dependency>
        <groupId>org.dspace</groupId>
        <artifactId>sword-common</artifactId>
        <version>1.0.0</version>
        </dependency>-->

        <dependency>
            <groupId>org.dspace</groupId>
            <artifactId>dspace-api</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.dspace</groupId>
            <artifactId>dspace-api-lang</artifactId>
        </dependency>

        <dependency>
            <groupId>jaxen</groupId>
            <artifactId>jaxen</artifactId>
        </dependency>

        <!-- additional dependencies for the sword-common code -->
        <dependency>
            <groupId>commons-fileupload</groupId>
            <artifactId>commons-fileupload</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>jmxtools</artifactId>
                    <groupId>com.sun.jdmk</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>jms</artifactId>
                    <groupId>javax.jms</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>jmxri</artifactId>
                    <groupId>com.sun.jmx</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>xom</groupId>
            <artifactId>xom</artifactId>
            <version>1.1</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
        </dependency>
    </dependencies>

</project>
