## Overview

The perc-toolkit module contains a set of extensions and utilities that have been contributed by professional services team members, customers, and implementors of Percussion that are usefull in implementations. 

The toolkit was historically called the PSO Toolkit in Percussion implementations.

This package is where experimental extensions / features can and should be implemented / contributed.

## Upgrade
On upgrade, the core CMS will remove all legacy toolkit filenames that it finds.  They will be replaced by the perc-toolkit-[version].jar package.

## Contributions Java Package

The com.percussion.contrib package is intended for any new toolkit code.  There is a contrib.experimental package that can be used for extensions that you are trying out / testing but that might not be ready for prime time.

Pull requests to experimental will almost always be accepted without a code review.

## Exploded Percussion Packages

There are a number of Percussion Packages, created with the Percussion Package Builder tool, that are "exploded"/unzipped in the packages folder of the toolkit.

This allows changes to Package files to be made without having to re-run the package builder tool. The build will re-package the files at build time.  If you are adding new files, or want to redo a package.  The package builder/manager tools needs to be used to Convert the Package to Source and then re-package with package builder.  After that the package needs re-exploded into it's location in the packages directory.

For packages under the packages folder, and the -Dcontrib=true parameter will need added to the install command line to install these packages.
There is an experimental folder under the packages folder.  Any packages that you would like to incubate can be added here.  There will be minimal code review on experimental packages, and the -Dexperimental=true parameter will need added to the install command line to install these packages.

## Module Map

https://www.github.com/percussion/PSOToolkit -> https://www.github.com/percussion/percussioncms/modules/perc-toolkit

## API Changes
- PSServerFolderProcessor
-- This is now a singleton  PSServerFolderProcessor.getInstance() should be used 
  
## Editor Custom Controls
Editor custom controls are XSL stylesheets that provide a control for editing specific field types in the Content Editor.

### Image Cropping Control
The image cropping control stores based x,y,width,height (as well as resize width / height) in a serialized JSON string in a field value.

To utilize the control, name a field based on the name of the image you will be cropping.  Example,

activeimg_hash may be the field value of the image field.

To create a image cropping control, create a field nammed

activeimg_crop_thumb

Then select rx_ImageCroppingControl as the control.  For the field size, set the size to "MAX", (where the default would be 50)

When editing a content item where an image has been uploaded, you should see a croppable image control.

If the image has not yet rendered, you may have to hit save / update to actually upload the image and make it available.