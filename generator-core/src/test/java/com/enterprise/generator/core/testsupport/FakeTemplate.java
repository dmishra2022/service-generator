package com.enterprise.generator.core.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds a tiny but realistic replica of the DDD starter on disk, for use by scanner, planner, and
 * copier tests. The structure deliberately includes:
 * <ul>
 *   <li>A parent {@code pom.xml} with {@code order-management-parent} artifact id,
 *       {@code <base.package>} property, and two {@code <module>} entries that start with
 *       {@code order-}, plus one that does not ({@code architecture-tests}).</li>
 *   <li>Context-prefixed modules with their own poms referencing the parent and each other.</li>
 *   <li>Java source files that import {@code com.enterprise.starter.order.*}.</li>
 *   <li>A {@code Packages.java} file in {@code architecture-tests} with the sentinel
 *       {@code BASE} constant.</li>
 *   <li>A {@code target/} directory and a {@code .git/} directory that must be skipped.</li>
 *   <li>A binary file to verify byte-for-byte copying.</li>
 * </ul>
 */
public final class FakeTemplate {

    private FakeTemplate() {
    }

    public static Path writeDefault(Path parent) throws IOException {
        Path root = Files.createDirectory(parent.resolve("ddd"));

        Files.writeString(root.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.enterprise.starter</groupId>
                    <artifactId>order-management-parent</artifactId>
                    <version>0.1.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>order-domain</module>
                        <module>order-application</module>
                        <module>architecture-tests</module>
                    </modules>
                    <properties>
                        <base.package>com.enterprise.starter.order</base.package>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.enterprise.starter</groupId>
                                <artifactId>order-domain</artifactId>
                                <version>${project.version}</version>
                            </dependency>
                            <dependency>
                                <groupId>com.enterprise.starter</groupId>
                                <artifactId>order-application</artifactId>
                                <version>${project.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """);

        // ArchGuard policy carried over to generated services. Base-package tokens get rewritten
        // to the new context by the content-rule layer; module artifact ids inside this file would
        // likewise be rewritten if any were present.
        Files.writeString(root.resolve("architecture.yml"), """
                schemaVersion: 1
                roots: ["com.enterprise.starter.order.."]
                profiles: [hexagonal, ddd]
                roles:
                  DOMAIN: ["com.enterprise.starter.order.domain.."]
                  APPLICATION: ["com.enterprise.starter.order.application.."]
                requireClassification: true
                failOn: ERROR
                """);

        Path domain = Files.createDirectories(root.resolve("order-domain"));
        Files.writeString(domain.resolve("pom.xml"), """
                <project>
                    <parent>
                        <groupId>com.enterprise.starter</groupId>
                        <artifactId>order-management-parent</artifactId>
                        <version>0.1.0</version>
                    </parent>
                    <artifactId>order-domain</artifactId>
                </project>
                """);
        Path domainPkg = Files.createDirectories(domain.resolve("src/main/java/com/enterprise/starter/order/domain"));
        Files.writeString(domainPkg.resolve("Order.java"), """
                package com.enterprise.starter.order.domain;

                public final class Order {
                    private final String id;
                    public Order(String id) { this.id = id; }
                    public String id() { return id; }
                }
                """);

        Path app = Files.createDirectories(root.resolve("order-application"));
        Files.writeString(app.resolve("pom.xml"), """
                <project>
                    <parent>
                        <groupId>com.enterprise.starter</groupId>
                        <artifactId>order-management-parent</artifactId>
                        <version>0.1.0</version>
                    </parent>
                    <artifactId>order-application</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.enterprise.starter</groupId>
                            <artifactId>order-domain</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);
        Path appPkg = Files.createDirectories(app.resolve("src/main/java/com/enterprise/starter/order/application"));
        Files.writeString(appPkg.resolve("PlaceOrderService.java"), """
                package com.enterprise.starter.order.application;

                import com.enterprise.starter.order.domain.Order;

                public final class PlaceOrderService {
                    public Order place() { return new Order("x"); }
                }
                """);

        Path arch = Files.createDirectories(root.resolve("architecture-tests"));
        Files.writeString(arch.resolve("pom.xml"), """
                <project>
                    <parent>
                        <groupId>com.enterprise.starter</groupId>
                        <artifactId>order-management-parent</artifactId>
                        <version>0.1.0</version>
                    </parent>
                    <artifactId>architecture-tests</artifactId>
                </project>
                """);
        Path archPkg = Files.createDirectories(arch.resolve("src/test/java/com/enterprise/starter/order/architecture"));
        Files.writeString(archPkg.resolve("Packages.java"), """
                package com.enterprise.starter.order.architecture;

                final class Packages {
                    static final String BASE = "com.enterprise.starter.order";
                    private Packages() {}
                }
                """);

        // Directories that must be excluded.
        Files.createDirectories(root.resolve("target/classes"));
        Files.writeString(root.resolve("target/classes/Dummy.class"), "junk");
        Files.createDirectories(root.resolve(".git/objects"));
        Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/main");

        // A binary file to prove non-text handling.
        Files.write(root.resolve("logo.png"), new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 1, 2, 3});

        // A text file that contains no renamable tokens at all.
        Files.writeString(root.resolve("LICENSE"), "Apache License 2.0\n");

        return root;
    }
}
