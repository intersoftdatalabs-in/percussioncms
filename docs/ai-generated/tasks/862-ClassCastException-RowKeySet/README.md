# Task: 8.1.7 Publishing Design, Publishing Runtime, and Admin tabs fail to load and redirect back to CMS due to ClassCastException: java.lang.String cannot be cast to RowKeySet (Issue #862)

## Objective

Fix the `ClassCastException` that occurs when users click on the "Publishing Design", "Publishing Runtime", or "Admin" tabs. The underlying exception happens during the JSF state-saving phase when `UIXNavigationTree` attempts to cast the value of `disclosedRowKeys` to `RowKeySet`. Because the layout tag files were declared with JSP version `"1.2"`, deferred EL expressions (`#{}`) were not parsed natively by the JSP container and were instead passed as literal String values, leading to a `ClassCastException`.

## Changes Made

Updated the `<jsp:root>` version attribute from `"1.2"` to `"2.1"` in the following layout and navigation JSP tag files:

1. **[admin.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/layout/admin.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.
2. **[publishing.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/layout/publishing.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.
3. **[pubruntime.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/layout/pubruntime.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.
4. **[adminbreadcrumbs.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/nav/adminbreadcrumbs.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.
5. **[editorbreadcrumbs.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/nav/editorbreadcrumbs.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.
6. **[listbreadcrumbs.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/nav/listbreadcrumbs.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.
7. **[runtimebreadcrumbs.tag](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/nav/runtimebreadcrumbs.tag)**:
   - Changed `version="1.2"` to `version="2.1"`.

Upgrading the tag file declarations to JSP version `2.1` forces the container to correctly compile and resolve the deferred EL expressions (`#{}`) as JSF `ValueExpression` objects, which evaluate to the actual backing bean's `RowKeySet` properties rather than literal String objects.

## Verification

- Built the `system` module and its dependencies via `./mvn-env.sh clean install -pl system -am -DskipTests` successfully.
- Verified compilation and layout tag validation success.

