package com.percussion.maven;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * Mojo to generate OpenAPI specification from JAX-RS annotations in REST module. Scans REST
 * resource classes for @Path and HTTP method annotations.
 */
@Mojo(name = "generate-spec", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class OpenAPIGeneratorMojo extends AbstractMojo {

  /** No-op constructor. */
  public OpenAPIGeneratorMojo() {}

  /** Maven project being built, injected by the plugin framework. */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  /** Output file location for generated OpenAPI spec. */
  @Parameter(
      defaultValue = "${project.build.directory}/../src/main/webapp/openapi.json",
      property = "openapi.output.file",
      required = true)
  private String outputFile;

  /** API title for OpenAPI specification. */
  @Parameter(defaultValue = "Percussion CMS REST API", property = "api.title", required = true)
  private String apiTitle;

  /** API version. */
  @Parameter(defaultValue = "${project.version}", property = "api.version", required = true)
  private String apiVersion;

  /** API description. */
  @Parameter(
      defaultValue = "Public REST API for Percussion CMS content management and delivery",
      property = "api.description",
      required = true)
  private String apiDescription;

  /**
   * Path to the REST module JAR file to scan for JAX-RS annotations. Can be a file path or Maven
   * artifact reference.
   */
  @Parameter(property = "restModuleJar", required = false)
  private String restModuleJar;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Generating OpenAPI specification from REST module...");

      // Create OpenAPI object with basic info
      OpenAPI openapi = createOpenAPISpec();

      // Scan REST module for JAX-RS resources
      scanRESTResources(openapi);

      // Ensure output directory exists
      Path outputPath = Paths.get(outputFile).toAbsolutePath();
      Files.createDirectories(outputPath.getParent());

      // Write spec to file
      String jsonSpec = Json.pretty(openapi);
      Files.write(outputPath, jsonSpec.getBytes());

      getLog().info("OpenAPI spec generated at: " + outputPath);

    } catch (Exception e) {
      throw new MojoExecutionException("Failed to generate OpenAPI specification", e);
    }
  }

  /** Creates a skeleton OpenAPI specification with metadata. */
  private OpenAPI createOpenAPISpec() {
    OpenAPI openapi = new OpenAPI();

    Info info = new Info();
    info.setTitle(apiTitle);
    info.setVersion(apiVersion);
    info.setDescription(apiDescription);
    openapi.setInfo(info);

    return openapi;
  }

  /** Scans the REST module for JAX-RS resource classes and populates OpenAPI paths. */
  private void scanRESTResources(OpenAPI openapi) throws Exception {
    // Get REST module JAR from parameter or find it in artifacts
    File restModuleFile = getRESTModuleFile();

    if (restModuleFile == null || !restModuleFile.exists()) {
      getLog().warn("REST module JAR not found. OpenAPI paths will not be generated.");
      return;
    }

    getLog().info("Scanning REST module: " + restModuleFile.getAbsolutePath());

    // Create classloader with REST module JAR
    URLClassLoader classLoader =
        new URLClassLoader(
            new URL[] {restModuleFile.toURI().toURL()},
            Thread.currentThread().getContextClassLoader());

    try {
      // Scan for classes with @Path annotation
      Reflections reflections =
          new Reflections(
              new org.reflections.util.ConfigurationBuilder()
                  .setUrls(restModuleFile.toURI().toURL())
                  .setClassLoaders(new ClassLoader[] {classLoader})
                  .setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated));

      Set<Class<?>> resourceClasses =
          reflections.getTypesAnnotatedWith(jakarta.ws.rs.Path.class, true);
      getLog().info("Found " + resourceClasses.size() + " REST resource classes");

      // Process each resource class
      for (Class<?> resourceClass : resourceClasses) {
        processResourceClass(openapi, resourceClass);
      }

    } finally {
      classLoader.close();
    }
  }

  /** Processes a single REST resource class and extracts endpoint metadata. */
  private void processResourceClass(OpenAPI openapi, Class<?> resourceClass) {
    jakarta.ws.rs.Path classPath = resourceClass.getAnnotation(jakarta.ws.rs.Path.class);
    if (classPath == null) {
      return;
    }

    String basePath = classPath.value();
    getLog()
        .debug("Processing resource class: " + resourceClass.getName() + " at path: " + basePath);

    // Process all methods in the resource class
    for (Method method : resourceClass.getDeclaredMethods()) {
      procesResourceMethod(openapi, basePath, method);
    }
  }

  /** Processes a single REST endpoint method and add it to OpenAPI paths. */
  private void procesResourceMethod(OpenAPI openapi, String basePath, Method method) {
    // Determine HTTP method
    String httpMethod = getHttpMethod(method);
    if (httpMethod == null) {
      return;
    }

    // Get path from @Path annotation if present
    jakarta.ws.rs.Path pathAnnotation = method.getAnnotation(jakarta.ws.rs.Path.class);
    String methodPath = (pathAnnotation != null) ? pathAnnotation.value() : "";
    String fullPath = combinePathSegments(basePath, methodPath);

    getLog().debug("Found endpoint: " + httpMethod.toUpperCase() + " " + fullPath);

    // Create or get PathItem for this path
    PathItem pathItem = openapi.getPaths() != null ? openapi.getPaths().get(fullPath) : null;
    if (pathItem == null) {
      pathItem = new PathItem();
    }

    // Create Operation for this method
    Operation operation = createOperation(method);

    // Add operation to PathItem based on HTTP method
    switch (httpMethod.toLowerCase()) {
      case "get":
        pathItem.setGet(operation);
        break;
      case "post":
        pathItem.setPost(operation);
        break;
      case "put":
        pathItem.setPut(operation);
        break;
      case "delete":
        pathItem.setDelete(operation);
        break;
      case "head":
        pathItem.setHead(operation);
        break;
      case "options":
        pathItem.setOptions(operation);
        break;
      case "patch":
        pathItem.setPatch(operation);
        break;
    }

    // Add pathItem to spec
    if (openapi.getPaths() == null) {
      openapi.paths(new io.swagger.v3.oas.models.Paths());
    }
    openapi.getPaths().put(fullPath, pathItem);
  }

  /** Creates an Operation object from a method's metadata. */
  private Operation createOperation(Method method) {
    Operation operation = new Operation();

    // Set operation ID from method name
    operation.setOperationId(method.getName());

    // Set description from JavaDoc if available
    operation.setDescription(
        "REST endpoint: " + method.getDeclaringClass().getSimpleName() + "." + method.getName());

    // Extract parameters
    List<io.swagger.v3.oas.models.parameters.Parameter> params = extractParameters(method);
    if (!params.isEmpty()) {
      operation.setParameters(params);
    }

    // Create default 200 response
    ApiResponses responses = new ApiResponses();
    ApiResponse response200 = new ApiResponse().description("Successful response");

    // Check @Produces annotation for content type
    Produces produces = method.getAnnotation(Produces.class);
    if (produces != null && produces.value().length > 0) {
      Content content = new Content();
      for (String mediaType : produces.value()) {
        content.addMediaType(mediaType, new MediaType().schema(new Schema<>().type("object")));
      }
      response200.setContent(content);
    }

    responses.addApiResponse("200", response200);
    operation.setResponses(responses);

    return operation;
  }

  /** Extracts parameters from method annotations (@PathParam, @QueryParam, etc). */
  private List<io.swagger.v3.oas.models.parameters.Parameter> extractParameters(Method method) {
    List<io.swagger.v3.oas.models.parameters.Parameter> parameters = new ArrayList<>();

    for (java.lang.reflect.Parameter param : method.getParameters()) {
      PathParam pathParam = param.getAnnotation(PathParam.class);
      if (pathParam != null) {
        parameters.add(
            new PathParameter()
                .name(pathParam.value())
                .description("Path parameter: " + pathParam.value())
                .required(true)
                .schema(new Schema<>().type("string")));
      }

      QueryParam queryParam = param.getAnnotation(QueryParam.class);
      if (queryParam != null) {
        parameters.add(
            new QueryParameter()
                .name(queryParam.value())
                .description("Query parameter: " + queryParam.value())
                .schema(new Schema<>().type("string")));
      }
    }

    return parameters;
  }

  /** Determines the HTTP method from method annotations. */
  private String getHttpMethod(Method method) {
    if (method.isAnnotationPresent(GET.class)) return "GET";
    if (method.isAnnotationPresent(POST.class)) return "POST";
    if (method.isAnnotationPresent(PUT.class)) return "PUT";
    if (method.isAnnotationPresent(DELETE.class)) return "DELETE";
    if (method.isAnnotationPresent(HEAD.class)) return "HEAD";
    if (method.isAnnotationPresent(OPTIONS.class)) return "OPTIONS";
    return null;
  }

  /** Gets the REST module JAR file from parameter or project artifacts. */
  private File getRESTModuleFile() {
    // If restModuleJar parameter is provided, use it
    if (restModuleJar != null && !restModuleJar.trim().isEmpty()) {
      File file = new File(restModuleJar);
      if (file.exists()) {
        getLog().info("Using REST module from parameter: " + restModuleJar);
        return file;
      }
      getLog().warn("REST module parameter points to non-existent file: " + restModuleJar);
    }

    // Fall back to finding REST module in project artifacts
    getLog()
        .debug("Searching for REST module in " + project.getArtifacts().size() + " artifacts...");
    for (Object artifact : project.getArtifacts()) {
      org.apache.maven.artifact.Artifact mvnArtifact =
          (org.apache.maven.artifact.Artifact) artifact;
      getLog().debug("  - " + mvnArtifact.getArtifactId() + ":" + mvnArtifact.getType());
      if ("rest".equals(mvnArtifact.getArtifactId()) && "jar".equals(mvnArtifact.getType())) {
        getLog().info("Found REST module JAR in artifacts: " + mvnArtifact.getFile());
        return mvnArtifact.getFile();
      }
    }
    getLog().warn("REST module not found in parameter or project artifacts");
    return null;
  }

  /** Finds the REST module JAR file from project dependencies. */
  private File findRESTModuleJar() {
    getLog()
        .info("Searching for REST module in " + project.getArtifacts().size() + " artifacts...");
    for (Object artifact : project.getArtifacts()) {
      org.apache.maven.artifact.Artifact mvnArtifact =
          (org.apache.maven.artifact.Artifact) artifact;
      getLog().debug("  - " + mvnArtifact.getArtifactId() + ":" + mvnArtifact.getType());
      if ("rest".equals(mvnArtifact.getArtifactId()) && "jar".equals(mvnArtifact.getType())) {
        getLog().info("Found REST module JAR: " + mvnArtifact.getFile());
        return mvnArtifact.getFile();
      }
    }
    getLog().warn("REST module not found in project artifacts");
    return null;
  }

  /** Combines base path and method-level path segments. */
  private String combinePathSegments(String basePath, String methodPath) {
    if (methodPath == null || methodPath.isEmpty()) {
      return basePath;
    }
    if (basePath.endsWith("/") || methodPath.startsWith("/")) {
      return basePath + methodPath;
    }
    return basePath + "/" + methodPath;
  }
}
