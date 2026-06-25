# Task: 8.1.7 Navigation error message displays duplicate "folder not found" validation when converting folder to section (Issue #866)

## Objective

Fix a bug where attempting to create/convert a Section from a folder that does not exist in Percussion CMS displays duplicate validation errors in the error dialog (e.g. "folder with that path cannot be found" displayed twice). When the source folder does not exist, the validation should stop early and return only the primary root cause error rather than continuing to validate parent path and dependent landing page existence.

## Changes Made

1. **[PSSiteSectionService (`projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java`)](file:///home/nate/projects/java8/percussioncms/projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java)**:
   - Modified `PSCreateSectionFromFolderValidator.doValidation` method to add `return;` statements after rejecting the request when:
     - The source path category is not a `Category.FOLDER`
     - An exception occurs while looking up the source folder path (folder does not exist)
   - This prevents execution of subsequent validation steps (parent folder existence and landing page existence checks) when the primary folder itself is invalid or missing, thereby avoiding duplicate/dependent validation messages.
2. **Server-side Validation Exception Mappers (`projects/sitemanage/src/main/java/com/percussion/share/web/service/`)**:
   - Updated `PSValidationExceptionMapper`, `PSBeanValidationExceptionMapper`, and `PSSpringValidationExceptionMapper` to return `Response.Status.BAD_REQUEST` (400) status codes instead of `500 INTERNAL_SERVER_ERROR`.
3. **Client-side Error Handling (`WebUI/war/services/PercServiceUtils.js`)**:
   - Modified `extractDefaultErrorMessage` to use `else if` for `globalError` checking. Since `globalError` is just an alias for the first element in `globalErrors` list, this prevents extracting and showing the same global error twice in the UI error dialog.
4. **Unit Tests (`projects/sitemanage/src/test/java/com/percussion/share/web/service/PSValidationExceptionMapperTest.java`)**:
   - Added new test cases to verify status mappings for all three validation exception mappers.

## Verification

- Formatted the codebase using `./mvn-env.sh spotless:apply`.
- Built the `sitemanage` module successfully.
- Ran new `PSValidationExceptionMapperTest` suite successfully.
- Ran all unit tests in the `projects/sitemanage` module successfully.

