# Task: Folder Creation Succeeds but Displays "Path not found" Error popup to User (Issue #867)

## Objective

Fix a bug where creating a new folder in the CMS UI displays a "Path not found" error popup even though the folder is actually created successfully in the database. This occurs due to:
1. A database/cache transaction commit delay. The client immediately requests the new folder's path details before the database transaction commits and the cache is fully updated, throwing `PSPathNotFoundServiceException`.
2. A missing JAX-RS Exception Mapper registration. The JAX-RS bean `pathServiceExceptionMapper` is scanned by Spring, but was not registered as a provider for the `/pathmanagement` endpoint. As a result, when the path service throws a checked exception, CXF falls back to a generic HTTP 500 error instead of a graceful JSON error message.

## Changes Made

1. **JAX-RS Configuration (`projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml`)**:
   - Added `<ref bean="pathServiceExceptionMapper"/>` to the `<jaxrs:providers>` section of `<jaxrs:server id="pathmanagement-jax-rs">` to register the exception mapper.
2. **Exception Mapper (`projects/sitemanage/src/main/java/com/percussion/share/web/service/PSPathServiceExceptionMapper.java`)**:
   - Imported `com.percussion.share.service.exception.IPSNotFoundException`.
   - Overrode `getStatus` to map `PSPathNotFoundServiceException` or any exception whose cause implements `IPSNotFoundException` to `Response.Status.NOT_FOUND` (404) instead of `Response.Status.INTERNAL_SERVER_ERROR` (500).
3. **Path Management Service (`projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSPathItemService.java`)**:
   - Updated `addFolder` to catch `PSPathNotFoundServiceException` during path lookup (`find(path)`) and retry up to 3 times, sleeping 100ms between attempts, to allow the database transaction to commit and the caches to sync.
4. **Unit Tests (`projects/sitemanage/src/test/java/com/percussion/share/web/service/PSPathServiceExceptionMapperTest.java`)**:
   - Created a new unit test class to verify correct HTTP status mapping for `PSPathNotFoundServiceException`, generic path exceptions, and exceptions with custom `IPSNotFoundException` causes.

## Verification

- Ran spotless check successfully using `./mvn-env.sh spotless:check -pl projects/sitemanage`.
- Ran unit tests successfully using `./mvn-env.sh test -pl projects/sitemanage` (298 passing tests).
- Ran the new `PSPathServiceExceptionMapperTest` suite successfully.
