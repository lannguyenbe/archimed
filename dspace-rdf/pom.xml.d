<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.dspace</groupId>
    <artifactId>dspace-rdf</artifactId>
    <packaging>war</packaging>
    <name>DSpace RDF</name>
    <description>Parent project for the RDF API and Webapp</description>

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
    </build>

    <dependencies>
        <!-- Use jena to create, store and load rdf -->
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>apache-jena-libs</artifactId>
            <type>pom</type>
        </dependency>

        <dependency>
            <groupId>org.dspace</groupId>
            <artifactId>dspace-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dspace</groupId>
            <artifactId>dspace-services</artifactId>
        </dependency>

		<!-- Spring 3 dependencies -->
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-core</artifactId>
		</dependency>
 
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-context</artifactId>
		</dependency>
 
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-web</artifactId>
		</dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-lang</groupId>
            <artifactId>commons-lang</artifactId>
        </dependency>
    </dependencies>
</project>
