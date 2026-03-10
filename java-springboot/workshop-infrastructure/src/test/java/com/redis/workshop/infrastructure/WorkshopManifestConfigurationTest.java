package com.redis.workshop.infrastructure;

import com.redis.workshop.autoconfigure.WorkshopManifestAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class WorkshopManifestConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WorkshopManifestAutoConfiguration.class));

    @TempDir
    Path tempDir;

    @Test
    void createsWorkshopConfigFromManifestWhenNoExplicitBeanExists() throws IOException {
        Path manifestPath = writeManifest(tempDir);

        contextRunner
            .withPropertyValues("workshop.manifest.location=" + manifestPath)
            .run(context -> {
                assertThat(context).hasSingleBean(WorkshopConfig.class);

                WorkshopConfig workshopConfig = context.getBean(WorkshopConfig.class);
                assertThat(workshopConfig.getEditableFiles()).containsExactly(
                    entry("application.properties", "src/main/resources/application.properties"),
                    entry("Dockerfile", "Dockerfile")
                );
                assertThat(workshopConfig.getOriginalContent("application.properties"))
                    .isEqualTo("spring.session.store-type=none\n");
                assertThat(workshopConfig.getOriginalContent("Dockerfile"))
                    .isEqualTo("FROM eclipse-temurin:21\n");
                assertThat(workshopConfig.getLanguage("Dockerfile")).isEqualTo("dockerfile");
                assertThat(workshopConfig.getBasePath()).isEqualTo(tempDir.resolve("workshop-root").toString());
                assertThat(workshopConfig.getWorkshopTitle()).isEqualTo("Manifest Workshop");
                assertThat(workshopConfig.getWorkshopDescription()).isEqualTo("Loaded from manifest data");
            });
    }

    @Test
    void keepsExplicitWorkshopConfigBeanWhenManifestAlsoExists() throws IOException {
        Path manifestPath = writeManifest(tempDir);

        contextRunner
            .withUserConfiguration(ExplicitWorkshopConfigConfiguration.class)
            .withPropertyValues("workshop.manifest.location=" + manifestPath)
            .run(context -> {
                assertThat(context).hasSingleBean(WorkshopConfig.class);
                assertThat(context.getBean(WorkshopConfig.class)).isSameAs(context.getBean("explicitWorkshopConfig"));
                assertThat(context.getBean(WorkshopConfig.class).getWorkshopTitle()).isEqualTo("Explicit Workshop");
            });
    }

    @Test
    void createsWorkshopConfigFromClasspathManifestWithRelativeResources() throws IOException {
        contextRunner
            .withPropertyValues("workshop.manifest.location=classpath:classpath-manifests/nested/workshop-manifest.yaml")
            .run(context -> {
                assertThat(context).hasSingleBean(WorkshopConfig.class);

                WorkshopConfig workshopConfig = context.getBean(WorkshopConfig.class);
                assertThat(workshopConfig.getEditableFiles()).containsExactly(
                    entry("application.properties", "src/main/resources/application.properties"),
                    entry("Dockerfile", "Dockerfile")
                );
                assertThat(workshopConfig.getOriginalContent("application.properties"))
                    .isEqualTo("spring.session.store-type=classpath\n");
                assertThat(workshopConfig.getOriginalContent("Dockerfile"))
                    .isEqualTo("FROM eclipse-temurin:21\n");
                assertThat(workshopConfig.getLanguage("Dockerfile")).isEqualTo("dockerfile");

                Path expectedBasePath = classpathPath("classpath-manifests/workshop-root/module-marker.txt").getParent();
                assertThat(workshopConfig.getBasePath()).isEqualTo(expectedBasePath.toString());
                assertThat(expectedBasePath.resolve("module-marker.txt")).exists();
                assertThat(workshopConfig.getWorkshopTitle()).isEqualTo("Classpath Manifest Workshop");
                assertThat(workshopConfig.getWorkshopDescription()).isEqualTo("Loaded from classpath manifest data");
            });
    }

    @Test
    void createsWorkshopConfigFromPackagedClasspathManifestWhenBasePathIsAbsolute() {
        Path absoluteBasePath = tempDir.resolve("packaged-workshop-root");
        ResourceLoader resourceLoader = packagedClasspathResourceLoader(
            """
                moduleName: packaged-classpath-manifest
                basePath: %s
                title: Packaged Classpath Manifest Workshop
                description: Loaded from packaged classpath manifest data
                editableFiles:
                  - name: application.properties
                    path: src/main/resources/application.properties
                    resetContentLocation: ../shared-reset/application.properties
                """.formatted(absoluteBasePath)
        );

        WorkshopConfig workshopConfig = new WorkshopManifestLoader(
            new MockEnvironment().withProperty(
                "workshop.manifest.location",
                "classpath:classpath-manifests/packaged/workshop-manifest.yaml"
            ),
            resourceLoader
        ).loadWorkshopConfig().orElseThrow();

        assertThat(workshopConfig.getOriginalContent("application.properties"))
            .isEqualTo("spring.session.store-type=packaged-classpath\n");
        assertThat(workshopConfig.getBasePath()).isEqualTo(absoluteBasePath.toString());
        assertThat(workshopConfig.getWorkshopTitle()).isEqualTo("Packaged Classpath Manifest Workshop");
    }

    @Test
    void rejectsRelativeBasePathForPackagedClasspathManifest() {
        ResourceLoader resourceLoader = packagedClasspathResourceLoader(
            """
                moduleName: packaged-classpath-manifest
                basePath: ../workshop-root
                title: Packaged Classpath Manifest Workshop
                description: Loaded from packaged classpath manifest data
                editableFiles:
                  - name: application.properties
                    path: src/main/resources/application.properties
                    resetContentLocation: ../shared-reset/application.properties
                """
        );

        WorkshopManifestLoader loader = new WorkshopManifestLoader(
            new MockEnvironment().withProperty(
                "workshop.manifest.location",
                "classpath:classpath-manifests/packaged/workshop-manifest.yaml"
            ),
            resourceLoader
        );

        assertThatThrownBy(loader::loadWorkshopConfig)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Relative basePath ../workshop-root")
            .hasMessageContaining("classpath:classpath-manifests/packaged/workshop-manifest.yaml")
            .hasMessageContaining("WORKSHOP_SOURCE_PATH");
    }

    private static Path writeManifest(Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("reset"));
        Files.writeString(tempDir.resolve("reset/application.properties"), "spring.session.store-type=none\n");

        Path manifestPath = tempDir.resolve("workshop-manifest.yaml");
        Files.writeString(
            manifestPath,
            """
                moduleName: manifest-workshop
                basePath: workshop-root
                title: Manifest Workshop
                description: Loaded from manifest data
                editableFiles:
                  - name: application.properties
                    path: src/main/resources/application.properties
                    resetContentLocation: reset/application.properties
                  - name: Dockerfile
                    path: Dockerfile
                    language: dockerfile
                    resetContent: |
                      FROM eclipse-temurin:21
                """
        );
        return manifestPath;
    }

    private static Path classpathPath(String location) throws IOException {
        return new ClassPathResource(location).getFile().toPath();
    }

    private static ResourceLoader packagedClasspathResourceLoader(String manifestContent) {
        return new MapBackedClasspathResourceLoader(Map.of(
            "classpath:classpath-manifests/packaged/workshop-manifest.yaml", manifestContent,
            "classpath:classpath-manifests/shared-reset/application.properties", "spring.session.store-type=packaged-classpath\n"
        ));
    }

    private static final class MapBackedClasspathResourceLoader implements ResourceLoader {
        private final Map<String, String> resources;

        private MapBackedClasspathResourceLoader(Map<String, String> resources) {
            this.resources = resources;
        }

        @Override
        public Resource getResource(String location) {
            String normalizedLocation = normalizeLocation(location);
            String content = resources.get(normalizedLocation);
            if (content == null) {
                return new MissingResource(normalizedLocation);
            }
            return new MapBackedClasspathResource(normalizedLocation, content, this);
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }

        private static String normalizeLocation(String location) {
            if (!location.startsWith("classpath:")) {
                return location;
            }

            String classpathLocation = location.substring("classpath:".length());
            return "classpath:" + Path.of(classpathLocation).normalize().toString().replace('\\', '/');
        }
    }

    private static final class MapBackedClasspathResource extends AbstractResource {
        private final String location;
        private final byte[] content;
        private final ResourceLoader resourceLoader;

        private MapBackedClasspathResource(String location, String content, ResourceLoader resourceLoader) {
            this.location = location;
            this.content = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            this.resourceLoader = resourceLoader;
        }

        @Override
        public String getDescription() {
            return location;
        }

        @Override
        public String getFilename() {
            int separatorIndex = location.lastIndexOf('/');
            return separatorIndex >= 0 ? location.substring(separatorIndex + 1) : location;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isFile() {
            return false;
        }

        @Override
        public Resource createRelative(String relativePath) {
            String manifestPath = location.substring("classpath:".length());
            Path resolvedPath = Path.of(manifestPath).getParent().resolve(relativePath).normalize();
            return resourceLoader.getResource("classpath:" + resolvedPath.toString().replace('\\', '/'));
        }
    }

    private static final class MissingResource extends AbstractResource {
        private final String location;

        private MissingResource(String location) {
            this.location = location;
        }

        @Override
        public String getDescription() {
            return location;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new FileNotFoundException(location);
        }

        @Override
        public boolean exists() {
            return false;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ExplicitWorkshopConfigConfiguration {

        @Bean
        WorkshopConfig explicitWorkshopConfig() {
            return new WorkshopConfig() {
                @Override
                public Map<String, String> getEditableFiles() {
                    return Map.of("custom.txt", "custom.txt");
                }

                @Override
                public String getOriginalContent(String fileName) {
                    return "custom";
                }

                @Override
                public String getModuleName() {
                    return "explicit-workshop";
                }

                @Override
                public String getWorkshopTitle() {
                    return "Explicit Workshop";
                }
            };
        }
    }
}
