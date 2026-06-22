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

## Re-opening & Additional Fix

### Cause of Re-opening

Even though the layout tag files were updated with `version="2.1"`, the JSP container by default treats implicit tag libraries (loaded via `tagdir` directive) as JSP version 2.0. Consequently, the container ignores the `version="2.1"` inside the `<jsp:root>` element and compiles the tags as JSP 2.0. As a result, deferred EL expressions (`#{}`) are treated as literal String values instead of JSF `ValueExpression` objects, which led to the same `ClassCastException` on `disclosedRowKeys`.

### Resolution

To force the JSP container to treat the implicit tag libraries as JSP version 2.1, we created `implicit.tld` files in the directories containing the tag files. These explicit implicit TLDs declare the tag library version as `2.1`, ensuring the container evaluates the deferred expressions correctly.

The following files were created:
1. **[system/ear/WEB-INF/tags/layout/implicit.tld](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/layout/implicit.tld)**: Declares version `2.1` for layout tag library.
2. **[system/ear/WEB-INF/tags/nav/implicit.tld](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/nav/implicit.tld)**: Declares version `2.1` for navigation tag library.
3. **[system/ear/WEB-INF/tags/banner/implicit.tld](file:///home/nate/projects/java8/percussioncms/system/ear/WEB-INF/tags/banner/implicit.tld)**: Declares version `2.1` for banner tag library.

