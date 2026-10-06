---
id: admin-publishing
title: Publishing
description: Publishing content and delivery targets in Percussion CMS 8.2
version: "8.2"
order: 43
tags: [admin, publishing]
---

# Publishing

Publishing is how Percussion turns assembled content into deliverables for websites and other
channels (static files, FTP, database, custom locations).

## Concepts

| Term | Meaning |
|------|---------|
| **Publish** | Run assembly for selected content and write results to configured destinations |
| **Edition / pub job** | Configured unit of work (what, where, when) |
| **Delivery location** | Filesystem path, FTP, or other target for assembled output |
| **Delivery Tier Service (DTS)** | Optional dynamic services (forms, comments, membership, metadata, polls, …) used by published sites |

## Operator workflow

1. Ensure content is in an **approved** (or otherwise publishable) workflow state.
2. Select the Site and publish scope (incremental vs full, as configured).
3. Run the publish job from the admin UI or scheduled task.
4. Verify logs for assembly errors and missing resources.
5. Spot-check delivered files or the live site.

Directory / Faculty Directory pages list people by **query** (organization and department on `percPerson`), not by unlinking members from the Directory asset. After a person is correctly non-matching, use full site publish or an explicit page publish, then purge CDN if disk HTML is clean but HTTP is stale. Procedure: [Faculty Directory membership and publish](id:admin-faculty-directory).

### Create a site (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), the **Sites** toolbar **Create site** button (and the
same action on the empty sites state) opens a name panel. **Create** posts `POST /services/sites`
with a `Site` body whose **name** is required. A successful create opens that site's workspace
so servers can be configured. **Cancel**, a blank name, and a name already on the list do not
call the server and do not open a workspace. HTTP **400** (invalid name), **403** (not Admin),
and **409** (name already exists) stay on the sites list with an error and do not open a
workspace. Copy of an existing site is a separate action.

### Rename the open site (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card, then **Rename site**.
**Rename** posts `POST /services/sites/{name}/rename` with a `RenameSiteRequest` body.
On success the workspace title shows the new name, and **Back** lists that name on the
sites cards. A site that does not yet have a site folder still renames the site record.
A site that already has a folder also updates that folder and navigation name. **Cancel**, a blank or unchanged name, and a name already used by another
site do not call the server and leave the title unchanged. HTTP **400** (invalid name),
**403** (not Admin), and **409** (name already exists) stay on the workspace with an
error. The title does not change. Create is a separate action.

### Copy the open site (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card, then **Copy site**.
**Copy** posts `POST /services/sitemanage/site/copy` with a `SiteCopyRequest` body
(`srcSite` is the open site, `copySite` is the new name). On success the workspace
title stays the source name. **Back** lists both the source and the new site.
**Cancel**, a blank or illegal name, the source name itself, and a name already on
the sites list do not call the server and do not add a card. HTTP **400** (invalid
name), **403** (not allowed), and **409** (name already exists or a copy is in
progress) stay on the workspace with an error. The source title does not change.
Copy of one delivery server, rename, and delete are separate actions.

### Edit the open site description (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Description** loaded from `GET /services/sites/{nameOrId}`. **Edit description**
opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with the site name and
the trimmed description only (base URL and other fields are not sent). The workspace
shows the saved text. **Cancel**, and **Save** when the text is unchanged (including
a blank description that was already blank), do not call PUT. HTTP **400**, **403**,
and **409** stay on the form with an error and do not show a saved notice. Rename,
copy, delete, and base URL edits are separate actions.

### Edit the open site protocol (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Protocol** (`http` or `https`) loaded from `GET /services/sites/{nameOrId}`.
**Edit protocol** opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with
the site name and `siteProtocol` only (description, base URL, and other fields are
not sent). The workspace shows the saved protocol, including after you leave and
reopen the site. **Cancel**, **Save** when the protocol is unchanged, and any value
other than `http` or `https` do not call PUT and do not show a saved notice. HTTP
**400**, **403**, and **409** stay on the form with an error and do not show a saved
notice. Description, base URL, rename, copy, and delete are separate actions.

### Edit the open site base URL (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Base URL** loaded from `GET /services/sites/{nameOrId}`. **Edit base URL**
opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with the site name and
the trimmed base URL only (description and other fields are not sent). The value must
be an absolute `http` or `https` URL. The workspace shows the saved URL, including
after you leave and reopen the site. **Cancel**, **Save** when the URL is unchanged,
and an empty or invalid URL do not call PUT and do not show a saved notice. HTTP
**400**, **403**, and **409** stay on the form with an error and do not show a saved
notice. Description, rename, copy, and delete are separate actions.

### Edit the open site default document (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Default document** loaded from `GET /services/sites/{nameOrId}`. **Edit default
document** opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with the site
name and the trimmed default document only (description, base URL, and other fields
are not sent). The workspace shows the saved value, including after you leave and
reopen the site. **Cancel**, **Save** when the value is unchanged, and an empty value
do not call PUT and do not show a saved notice. The site update contract ignores a
blank default document (it does not clear the stored value and does not return
**400** for blank). HTTP **400**, **403**, and **409** stay on the form with an error
and do not show a saved notice. Protocol, description, base
URL, rename, copy, and delete are separate actions.

### Edit the open site default file extension (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Default file extension** loaded from `GET /services/sites/{nameOrId}` (the wire
field is `defaultFileExtention`). **Edit default file extension** opens a form.
**Save** sends `PUT /services/sites/{nameOrId}` with the site name and the normalized
extension only (description, base URL, default document, and other fields are not
sent). A leading dot is dropped; the value must be 1–16 letters or digits (for
example `html`). The workspace shows the saved extension, including after you leave
and reopen the site. **Cancel**, **Save** when the extension is unchanged, and an
empty or invalid extension do not call PUT and do not show a saved notice. The site
update contract ignores a blank extension (it does not clear the stored value and
does not return **400** for blank). HTTP **400**, **403**, and **409** stay on the
form with an error and do not show a saved notice. Protocol, default document,
description, base URL, rename, copy, and delete are separate actions.

### Edit the open site folder root (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Folder root** loaded from `GET /services/sites/{nameOrId}`. **Edit folder root**
opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with the site name and
`folderRoot` only (description, base URL, and other fields are not sent). Backslashes
are stored as `/`. The path must start with `/` (a leading `//` is kept, as in
`//Sites/Name`), must include at least one folder name, and must not contain empty,
`.` , or `..` segments. The workspace shows the saved path, including after you leave
and reopen the site. This updates the path stored on the site record. It does not
create the folder and it does not move items. **Cancel**, **Save** when the path is
unchanged, and an empty or invalid path do not call PUT and do not show a saved
notice. A blank or unsafe path sent to the server is HTTP **400** and does not change
the stored path. A path already used by another site is HTTP **409**. HTTP **400**,
**403**, and **409** stay on the form with an error and do not show a saved notice.
Protocol, file extension, description, base URL, rename, copy, and delete are
separate actions.

### Set the open site default workflow (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows
the site **Default workflow** loaded from `GET /services/sites/{nameOrId}` (the
wire field is `workflowName`). **Edit default workflow** opens a list filled from
`GET /services/workflowmanagement/workflows/metadata`. **Save** sends
`PUT /services/sites/{nameOrId}` with the site name and that catalog
`workflowName` only (description, folder root, and other fields are not sent).
The workspace shows the saved workflow name, including after you leave and
reopen the site. The saved notice appears only after that PUT succeeds.
**Cancel**, **Save** when the name is unchanged, and an empty choice do not call
PUT and do not show a saved notice. A name that is not in the catalog is not
sent. An unknown workflow name on the server is HTTP **400** and does not change
the folder workflow. HTTP **400**, **403**, and **409** stay on the form with an
error and do not show a saved notice. The site must already have a site folder;
this action sets the folder workflow. It does not edit workflow transitions.
Folder root, the page-based flag, and site delete are separate actions.

### Edit the open site canonical distribution (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows the
site **Canonical distribution** (`pages` or `sections`) loaded from
`GET /services/sites/{nameOrId}` (the wire field is `canonicalDist`). **Edit canonical
distribution** opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with the
site name and `canonicalDist` only (description, base URL, protocol, and other fields
are not sent). The value must be `pages` or `sections`. The workspace shows the saved
value, including after you leave and reopen the site. **Cancel**, **Save** when the
value is unchanged, and a blank or unsupported value do not call PUT and do not show a
saved notice. When `canonicalDist` is present on update, a blank or unsupported value
is **400** and does not change the stored value. Omitting the field leaves the stored
value unchanged (the wire default of `pages` is not an implicit write). HTTP **400**,
**403**, and **409** stay on the form with an error and do not show a saved notice.
Protocol, default document, default file extension, description, base URL, rename,
copy, and delete are separate actions. The canonical replace flag is a separate
setting.

### Toggle canonical URL replace (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows
whether canonical URLs replace the rendered location (**yes** or **no**), loaded from
`GET /services/sites/{nameOrId}` (the wire field is `canonicalReplace`). **Edit canonical
URL replace** opens a checkbox. **Save** sends `PUT /services/sites/{nameOrId}` with the
site name and `canonicalReplace` only (description, base URL, `canonicalDist`, and other
fields are not sent). The workspace shows **yes** or **no** for the saved boolean,
including after you leave and reopen the site. The saved notice appears only after that
PUT succeeds. **Cancel** restores the previous value and does not call PUT. **Save** when
the checkbox matches the loaded value does not call PUT and does not show a saved notice.
Omitting `canonicalReplace` on update leaves the stored boolean unchanged (the wire
default of `true` is not an implicit write). HTTP **400**, **403**, and **409** stay on
the form with an error and do not show a saved notice. Canonical distribution, protocol,
default document, description, base URL, rename, copy, and delete are separate actions.

### Set the open site page-based flag (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows
whether the site is page-based (**yes** or **no**), loaded from
`GET /services/sites/{nameOrId}` (the wire field is `pageBasedSite`). **Edit
page-based** opens a checkbox. **Save** sends `PUT /services/sites/{nameOrId}` with
the site name and `pageBasedSite` only (description, base URL, canonical settings,
and other fields are not sent). The workspace shows **yes** or **no** for the saved
boolean, including after you leave and reopen the site. The saved notice appears
only after that PUT succeeds. **Cancel** restores the previous value and does not
call PUT. **Save** when the checkbox matches the loaded value does not call PUT and
does not show a saved notice. Omitting `pageBasedSite` on update leaves the stored
boolean unchanged (the wire default of `false` is not an implicit write). HTTP
**400**, **403**, and **409** stay on the form with an error and do not show a saved
notice. Folder root, default workflow, site create, and site delete are separate
actions.

### Edit additional head content (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card. The workspace shows
**Additional head content** loaded from `GET /services/sites/{nameOrId}` as plain
text. The shell does not insert or run that markup. **Edit additional head content**
opens a form. **Save** sends `PUT /services/sites/{nameOrId}` with the site name and
`siteAdditionalHeadContent` only (description, base URL, canonical settings, and
other fields are not sent). A blank value clears the stored content. The workspace
shows the saved text, including after you leave and reopen the site. **Cancel**, and
**Save** when the text is unchanged (including content that was already blank), do
not call PUT. HTTP **400**, **403**, and **409** stay on the form with an error and
do not show a saved notice. Before-body and after-body snippets are not edited here.

### Delete the open site (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card, then **Delete site**.
**Delete** sends `DELETE /services/sites/{nameOrId}` only after you confirm.
On success the shell leaves the site workspace and refreshes the sites list so
the deleted site is gone. **Cancel** closes the confirm panel and does not call
DELETE. HTTP **403** (not Admin), **404** (site already gone), and **409**
(the site is still in use) stay on the workspace with an error and do not
claim the site was deleted. Rename, copy, and delivery-server delete are
separate actions.

### Search the Sites list (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), the **Sites** section lists sites as cards (or a list).
Use **Filter Sites** to narrow the list by **name** or **id** (case-insensitive substring). Clearing
the box restores the full list. A search with no matches shows an empty state; it does not hide the
filter. Open a remaining card to configure servers and run Incremental / Full publish. Logs search
is a separate **Logs** filter (not this Sites box).

### Save a site delivery server (Sites)

From **Publish** (`spa.jsp?entry=publish`), open a **site** card, then **Add** (or **Edit Server**).
Enter a **server name** (required), production vs staging, File vs Database, driver fields, then
**Save**. Unsaved field edits mark the form dirty; leaving the editor prompts to discard. The shell
posts `POST …/publishmanagement/servers/{siteId}/{serverName}` (create) or
`PUT …/publishmanagement/servers/{siteId}/{serverId}` (update).

HTTP **403** (not Admin or Designer) and **409** (publish server name already exists on the site)
are shown in the server editor error region — not as a successful save. Incremental / Full publish
and Design delivery-type save are separate actions.

### Copy a site delivery server (Sites)

From **Publish** (`spa.jsp?entry=publish`), open a site card, select a publish server, then
**Copy Server**. Enter a **new server name** for that same site. Confirm copies the source
driver and non-secret connection settings (`POST …/publishmanagement/servers/{siteId}/{serverName}`,
the same create used by **Add**). The new server appears in the site list and is not marked as
the publish-now default. Cancel the name prompt does not call the server.

A blank name is refused in the workspace (**Server name is required**) and is not a successful
copy. A name already used on the site is HTTP **409** and stays in the workspace error region;
the list does not gain a second server. Passwords and other secret driver properties are not
copied and are not shown in this help. Cross-site copy and delete are separate actions.

### Delete a site delivery server (Sites)

From **Publish** (`spa.jsp?entry=publish`), open a site card, select a publish server, then
**Edit Server**. **Delete Server** asks for confirmation (**Confirm Delete Server**). Confirm calls
`DELETE …/publishmanagement/servers/{siteId}/{serverId}`. On success the editor closes and that
server is gone from the site list. Cancel does not call the server and leaves the list unchanged.

The server cannot be deleted when it is the site default or when a publish job is using it. Those
cases, and HTTP **403** (not Admin or Designer) or **404**, stay in the server editor error region.
The list is not refreshed as a successful delete. Edition delete is a separate Design action.

### Retry a failed status job (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open **Status**. A job whose status is **Failed** or **Completed with failures**, and that names both a site and a delivery server, shows **Retry**. Running, completed, and failed rows that omit the site or server do not. **Retry** asks for confirmation (**Retry this failed publish for the same site and server?**). Confirm starts a new publish for that site and server: incremental when the status payload's publish kind or edition name says incremental (`GET …/sitemanage/publish/incremental/publish/{site}/{server}`), otherwise a full site publish (`GET …/sitemanage/publish/{site}/{server}`). Status reloads after a job starts. Cancel does not call publish. Application-level `FORBIDDEN` and `BADCONFIG` (including HTTP 200 bodies) stay in the Status error region and are not treated as a started job. Stop remains a separate action for a running job.

### Incremental site publish (Publishing shell)

From **Publish** (`spa.jsp?entry=publish`), open a site card, select a publish server, then choose **Incremental**. Confirm the dialog (**Confirm Incremental Publish**). The shell calls the incremental site publish API (`GET …/sitemanage/publish/incremental/publish/{site}/{server}`), optionally with related-item approval after **Incremental preview**. Success shows **Publish Job Started** plus the job id and refreshes the site **Status** list (active jobs). Dismissing confirm does not start a job. Application-level `FORBIDDEN` / `BADCONFIG` responses are failures in the workspace error region, not success. Full site publish remains a separate **Full** action.

**Incremental preview** loads the queued items for the selected site and server (`GET …/sitemanage/publish/incremental/content/{site}/{server}`). Each queued item is a row with its **content id** and **title or name**. An empty queue shows the empty-queue message and no rows. If the list request fails, the workspace shows the error and does not invent rows.

**Approve** on a row asks for confirmation (**Approve this queued item for incremental publish?**), then calls `POST …/sitemanage/publish/incremental/content/{site}/{server}/{contentId}/approve`. Confirm runs the approve workflow transition for that content id. The item stays on the incremental queue. The list reloads and the row shows **Approved**. Cancel does not call the server. HTTP **400** (the id is not a content id, or the workflow transition was rejected), **403** (publish not allowed), and **404** (the id is not queued, or the site or server was not found) stay on the workspace as errors and do not mark the row approved. This is not incremental publish, not remove-one-row, and not clear-queue.

**Unapprove** appears on a row that is already **Approved**. It asks for confirmation (**Remove approval from this queued item?**), then calls `POST …/sitemanage/publish/incremental/content/{site}/{server}/{contentId}/unapprove`. Confirm runs the Reject workflow transition for that content id only. The item stays on the incremental queue. The list reloads and that row no longer shows **Approved**. Other approved rows stay approved. Cancel does not call the server. HTTP **400** (the id is not a content id, or the workflow transition was rejected), **403** (publish not allowed, or the transition is forbidden), and **404** (the id is not queued, or the site or server was not found) stay on the workspace as errors and leave the **Approved** mark in place. This is not incremental publish, not approve, not remove-one-row, and not clear-queue.

**Remove from queue** on a row asks for confirmation, then calls `DELETE …/sitemanage/publish/incremental/content/{site}/{server}/{contentId}`. Confirm removes that content id from the site incremental queue for the selected server (live or staging) and the row disappears. Cancel leaves the queue unchanged. HTTP **403** (publish not allowed) and **404** (the id is not queued, or the site or server was not found) stay on the workspace as errors; the row is not removed. Job-status detail is a separate action.

**Clear queue** (shown when the preview has at least one row) asks for confirmation, then calls `DELETE …/sitemanage/publish/incremental/content/{site}/{server}` with no content id. Confirm deletes the incremental queue for that site and server (live or staging) and reloads the list. An empty reload shows the empty-queue message. If the server still returns rows, those rows stay and the workspace says some items are still queued. Cancel does not call the server. HTTP **403** and **404** (site or server not found) stay on the workspace as errors; this is not incremental publish and not remove-one-row.

### Create a publish edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
The site selector is the open site. **Add edition** starts a new edition for that site.
Enter a **name** (required, at most 100 characters) and optional comment and priority (1–5),
then **Save**. The shell posts `POST …/sitemanage/publishingdesign/editions` with the open
site id. On success the editor closes and the new edition appears in that site's edition list.

A blank name stays in the editor (**Name is required**) and is not posted. A name longer than
100 characters stays in the editor and is not posted. HTTP **400** (name or site missing, or
the name is too long), **403** (not Admin or Designer), and **409** (edition name already
exists) stay in the edition editor error region. The list does not gain a row for those
failures. Saving an edited **comment** together with other fields on an existing edition is
still that update (`PUT …/sitemanage/publishingdesign/editions/{editionId}`). Changing only the
name is described under **Rename a publish edition** and does not rewrite stored priority,
comment, or content-list order. Changing only the priority is described under **Set an edition
priority** and does not rewrite the name, comment, or content-list order. Changing only the
comment is described under **Set an edition comment** and does not rewrite the name, priority,
or content-list order. Associating a content list is described below. Copying an edition and
Runtime start/stop are separate actions.

### Rename a publish edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Open one existing edition (not **Add edition**). Change **Name** and **Save**.

That sends `PUT …/sitemanage/publishingdesign/editions/{editionId}` with the new name, the
edition id, and the open site id. When **Comment** and **Priority** were not edited, those
fields are omitted so the stored comment and priority stay as they are. The call does not
associate, remove, or reorder content lists, so that order stays as stored. **Editions**
keeps the previous name until the save succeeds, then shows the new name. **Back** does not
call the server.

A blank name stays in the editor (**Name is required**) and is not sent. A name longer than
100 characters stays in the editor (**Edition name must be 100 characters or fewer**) and is
not sent. HTTP **400** (the name is rejected), **403** (not Admin or Designer), and **409**
(edition name already exists) stay in the edition editor error region. Those responses leave
the previous name on **Editions**. Stored priority, comment, and content-list order stay as
they were. Creating an edition, copying an edition, deleting an edition, setting only the
priority, and reordering content lists are separate actions.

### Set an edition priority (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
On an edition row, **Priority** opens **Edition priority** (it does not call the server).
The form shows the current **name** and **comment** and does not let you change them.
Change **Priority** to a whole number from 1 (lowest) to 5 (highest). **Save priority** sends
`PUT …/sitemanage/publishingdesign/editions/{editionId}` with an `edition` object that contains
only `priority`. Name, comment, and site id are omitted, so the server leaves the stored name
and comment as they are. The call does not associate, remove, or reorder content lists, so that
order stays as stored.

**Editions** keeps the previous priority until that update succeeds, then shows the new priority
with the same name and comment. **Cancel** closes the form and does not call the server.
A value outside 1–5 (blank, 0, 6, or not a whole number) stays in the form (**Edition priority
must be from 1 to 5**) and does not call the server.

HTTP **400** (priority outside 1–5), **403** (not Admin or Designer), and **409** stay in the
priority form error region. Those responses do not change the priority on **Editions**. Renaming
an edition, setting only the comment, creating an edition, and reordering content lists are
separate actions.

### Set an edition comment (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
On an edition row, **Comment** opens **Edition comment** (it does not call the server).
The form shows the current **name** and **priority** and does not let you change them.
Change **Comment**, or clear it. **Save comment** sends
`PUT …/sitemanage/publishingdesign/editions/{editionId}` with an `edition` object that contains
only `comment`. Name, priority, and site id are omitted, so the server leaves the stored name
and priority as they are. The call does not associate, remove, or reorder content lists, so that
order stays as stored.

A blank comment, including spaces only, is trimmed and sent as an empty `comment`, which clears
the stored comment. **Editions** keeps the previous comment until that update succeeds, then
shows the new comment (or nothing, when it was cleared) with the same name and priority.
**Cancel** closes the form and does not call the server.

HTTP **400**, **403** (not Admin or Designer), and **409** stay in the comment form error region.
Those responses do not change the comment on **Editions**. Renaming an edition, setting only the
priority, creating an edition, and reordering content lists are separate actions.

### Associate a content list with an edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Open an existing edition (not **Add edition**). Under **Associated content lists**, choose
one **content list** and one **delivery context**, then **Associate**.

The shell posts `POST …/sitemanage/publishingdesign/editions/{editionId}/contentlists` with an
`editionContentList` body (`contentListId` and `deliveryContextId`). The content list name
appears under **Associated content lists** only after that call succeeds. Leaving either the
content list or the delivery context blank shows **Select content list and delivery context**
and does not call the server. The associated list stays unchanged.

HTTP **400** (content list or delivery context missing), **403** (not Admin or Designer), and
**409** (that content list is already associated with this edition) stay in the edition editor
error region. Those responses do not add the content list to the associated list. A successful
associate stores the next sequence, so the new row is last among numbered associations.
Removing an association and changing that order are described below. Creating an edition and
creating a content list are separate actions.

### Remove a content list from an edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Open an existing edition (not **Add edition**). Under **Associated content lists**, **Remove**
on one row asks for confirmation (**Remove this content list from the edition? The content
list itself is not deleted.**).

Confirm calls
`DELETE …/sitemanage/publishingdesign/editions/{editionId}/contentlists/{contentListId}`.
That content list disappears from **Associated content lists** only after the call succeeds.
Other associated lists on the same edition stay. Cancel does not call the server and leaves
the association in place. This does not delete the content list definition.

An edition with a running publish job does not lose the association. A publish job that has
already finished does not block removal. HTTP **400** (edition or content list id missing),
**403** (not Admin or Designer), and **409** (**Edition is in use**) stay in the edition
editor error region. Those responses do not remove the row. Associating a content list,
deleting the content list definition, and deleting the edition are separate actions.

### Reorder a content list on an edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Open an existing edition (not **Add edition**). Under **Associated content lists**, each row
has **Move up** and **Move down**. The list is shown in association sequence (a missing
sequence sorts after numbered rows, then by content-list id).

**Move up** on the first row and **Move down** on the last row stay disabled. On any other
row the button asks for confirmation (**Move this content list up in the edition?** or
**Move this content list down in the edition?**). Confirm calls
`PUT …/sitemanage/publishingdesign/editions/{editionId}/contentlists/{contentListId}/sequence`
with an `editionContentList` body. `sequence` in that body is the 0-based target position and
must be the previous or next row. The rendered order changes only after that call succeeds.
The server then rewrites each association's stored sequence to 1, 2, 3… in the new order
(`PSEditionContentList.sequence` / `RXEDITIONCLIST.SEQUENCE`). Cancel does not call the server
and leaves the order unchanged.

An edition with a running publish job does not change order. HTTP **400** (the position is
missing or not adjacent, including moving the first row up or the last row down), **403**
(not Admin or Designer), and **409** (**Edition is in use**) stay in the edition editor error
region. Those responses keep the previous order. Associating or removing a content list,
setting an edition priority (1–5), and setting an edition comment are separate actions.

### Copy a publish edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Open an existing edition (not **Add edition**). Under **Copy to site**, choose the **target
site** — the site you are already viewing, or another site — and an optional **new name**.
**Copy edition** posts `POST …/sitemanage/publishingdesign/editions/copy` with a
`copyEditionRequest` body (source edition id, target site id, optional new name, and
whether content-list associations are copied).

On success the editor closes. The Design edition list for the **target** site shows the new
edition (the site selector switches when the target is not the current site). Open that row
to edit the copy. A blank new name is stored as the source name plus `_copy`.

HTTP **400** (source edition or target site missing) and **403** (not Admin or Designer) stay
in the edition editor error region. The list is not refreshed as a successful copy. HTTP
**409** (the new name already exists) stays in that same error region. Creating an edition
from scratch is a separate action. Deleting an edition is described below.

### Delete a publish edition (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Editions**.
Open an existing edition (not **Add edition**). **Delete** asks for confirmation
(**Delete this design object? This cannot be undone.**). Confirm calls
`DELETE …/sitemanage/publishingdesign/editions/{editionId}`. On success the editor
closes and that edition is gone from the site's edition list. Cancel does not call
the server and leaves the list unchanged.

An edition with a running publish job is not deleted. A publish job that has already
finished does not block delete, even while the server still remembers that job.
HTTP **409** (**Edition is in use**),
**403** (not Admin or Designer), and **400** (edition id missing) stay in the edition
editor error region. The list is not refreshed as a successful delete. Creating an
edition, copying an edition, and stopping a running job are separate actions.

### Save a content list (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
**Add content list** (or open an existing list). Enter a **name** (required), optional description,
and, on create, a type (modern vs legacy), plus a generator or legacy URL, then **Save**.
On an existing list the **name** stays editable; **type** cannot be changed. Unsaved field edits
mark the form dirty; leaving the editor prompts to discard. The shell posts
`POST …/sitemanage/publishingdesign/contentlists` (create) or
`PUT …/sitemanage/publishingdesign/contentlists/{contentListId}` (update).

HTTP **403** (not Admin or Designer) and **409** (content list name already exists) are shown in the
content-list editor error region — not as a successful save. Setting only the description is
described below and does not change the name, type, generator or legacy URL, or item filter.
Setting only the generator on a modern list is also described below and does not change the name,
description, type, item filter, or a legacy list URL.
Edition save and delivery-type save are separate Design actions.

### Rename a content list (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
Open one existing content list (not **Add content list**). Change **Name** and **Save**.
That sends `PUT …/sitemanage/publishingdesign/contentlists/{contentListId}`. **Content lists**
keeps the previous name until the save succeeds, then shows the new name. **Back** does not call
the server. A blank name is rejected in the editor and does not call the server. HTTP **409**
(that name already exists) stays in the editor and leaves the previous name on **Content lists**.
Changing only the description still saves and does not rename the list. The list type is unchanged.
Copying a content list is described below.

### Copy a content list (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
**Copy** on one row opens a form with a **new name** (required, at most 100 characters,
`RXCONTENTLIST.NAME`). The field starts as the source name plus ` copy` when that still fits.
**Copy content list** sends
`POST …/sitemanage/publishingdesign/contentlists/copy` with a `copyContentListRequest` body
(`sourceContentListId` and `newName`). The server allocates a new content list id and copies the
description, generator, legacy URL, expander, item filter, edition type, content-list type, and
generator and expander parameters. The source name and id stay the same. The new name appears
under **Content lists** only after that call succeeds. **Cancel** does not call the server.

A blank name, or a name longer than 100 characters, is rejected in the form and does not call the
server. HTTP **400** (name missing or too long, or the source id is missing), **403** (not Admin
or Designer), **404** (source list not found), and **409** (that name already exists) stay in the
copy form. Those responses do not add a row and do not change the source list. This action does
not associate the copy with an edition. Renaming a content list, deleting one, setting its item
filter, and copying an edition (which may link existing lists) are separate actions.

### Set the item filter on a content list (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
Open one existing **modern** content list (not **Add content list**, and not a legacy list).
**Item filter** lists item filters that already exist (the same catalog as Developer item filters).
Choose one, or **No item filter** to clear it, then **Save**. That sends
`PUT …/sitemanage/publishingdesign/contentlists/{contentListId}` with `itemFilterId` set to the
filter's **name** (numeric uuids are not unique across hosts), or to an empty string when clearing.
The **Saved item filter** line and the text next
to the content list stay on the previous filter until that save succeeds and the list reloads.
**Back** does not call the server.

A blank name is rejected in the editor and does not call the server. An unknown filter is
**400** (**Unknown item filter**). HTTP **400**, **403** (not Admin or Designer), and **409**
(content list name already exists) stay in the editor. Those responses do not change the filter
shown for that content list. This action does not create item filters, and it does not change the
list type, expander, edition type, or generator. Renaming a content list, copying one, and
setting only the description are separate actions. A legacy content list does not use an item
filter. A list with no generator, no expander, and no item filter is legacy, so keep a generator
on a modern list when clearing the filter.

### Set a content list description (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
On a content list row, **Description** opens **Content list description** (it does not call the
server). The form shows the current **name**, **type**, **generator** or **legacy URL**, and
**item filter**, and does not let you change them. Change **Description**. **Save description**
sends `PUT …/sitemanage/publishingdesign/contentlists/{contentListId}` with a `contentList`
object that contains only the description. Name, type, generator, legacy URL, and item filter
are omitted, so the server leaves those stored. A blank description clears the stored
description.

**Content lists** keeps the previous description until that update succeeds, then shows the new
description (or none, when cleared) with the same name, type, generator or legacy URL, and item
filter. **Cancel** closes the form and does not call the server. A description longer than 255
characters (`RXCONTENTLIST.DESCRIPTION`) is rejected in the form and does not call the server.

HTTP **400** (description longer than 255 characters), **403** (not Admin or Designer), and
**409** (for example a content list name conflict on the same update) stay in the description
form error region. Those responses do not change the description on the list. Saving a content
list (which can also change the name, generator or legacy URL, and item filter), renaming a
content list, setting the item filter, and setting only the generator are separate actions.

### Set a content list generator (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
On a **modern** content list row, **Generator** opens **Content list generator** (it does not call
the server). A legacy row has no **Generator** action, so this does not change a legacy URL.
The form shows the current **name**, **description**, **type**, and **item filter**, and does not
let you change them. Change **Generator**. **Save generator** sends
`PUT …/sitemanage/publishingdesign/contentlists/{contentListId}` with a `contentList` object that
contains only the generator. Name, description, type, legacy URL, and item filter are omitted, so
the server leaves those stored.

**Content lists** keeps the previous generator until that update succeeds, then shows the new
generator with the same name, description, type, and item filter. **Cancel** closes the form and
does not call the server. A blank generator is rejected in the form and does not call the server
(clearing the generator would make a list with no expander and no item filter look legacy). A
generator longer than 256 characters (`RXCONTENTLIST.GENERATOR`) is also rejected in the form and
does not call the server.

HTTP **400** (generator blank, longer than 256 characters, or sent for a legacy list), **403**
(not Admin or Designer), and **409** (for example a content list name conflict on the same update)
stay in the generator form error region. Those responses do not change the generator on the list.
Saving a content list (which can also change the name, description, legacy URL, and item filter),
renaming a content list, setting the item filter, and setting only the description are separate
actions. The content list type stays disabled after create; this action does not switch a saved
list between modern and legacy.

### Delete a content list (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Content lists**.
Open an existing content list (not **Add content list**). **Delete** asks for confirmation
(**Delete this design object? This cannot be undone.**). Confirm calls
`DELETE …/sitemanage/publishingdesign/contentlists/{contentListId}`. On success the editor
closes and that content list is gone from **Content lists**. Cancel does not call the server
and leaves the list unchanged.

A content list that is still associated with an edition is not deleted. Remove that association
first. This action does not remove an edition association and does not delete the edition.
HTTP **409** (**Content list is in use**), **403** (not Admin or Designer), and **400** (content
list id missing) stay in the content-list editor error region. The list is not refreshed as a
successful delete. Saving a content list and removing it from an edition are separate actions.

### Save a delivery type (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
**Add** (or open an existing type). Enter a **name** and **bean name** (required), optional
description, then **Save**. Unsaved field edits mark the form dirty; leaving the editor prompts to
discard. The shell posts `POST …/sitemanage/publishingdesign/deliverytypes` (create) or
`PUT …/sitemanage/publishingdesign/deliverytypes/{deliveryTypeId}` (update).

HTTP **403** (not Admin or Designer) and **409** (delivery type name already exists) are shown in the
delivery-type editor error region — not as a successful save. Content-list save and edition save
are separate Design actions. Renaming a delivery type is described below and does not change the
bean name or description. Setting a delivery type description is also described below and does
not change the name or bean name. Setting a delivery type bean name is also described below and
does not change the name or description.

### Rename a delivery type (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
On a delivery type row, **Rename** opens **Rename delivery type** (it does not call the server).
The form shows the current **bean name** and **description** and does not let you change them.
Change **Name**. **Rename delivery type** sends
`PUT …/sitemanage/publishingdesign/deliverytypes/{deliveryTypeId}` with a `deliveryType` object
that contains only the new name. Bean name, description, and whether unpublishing requires
assembly are omitted, so the server leaves those stored.

**Delivery types** keeps the previous name until that update succeeds, then shows the new name
and the same bean name. **Cancel** closes the form and does not call the server. A blank name,
or a name longer than 50 characters (`PSX_DELIVERY_TYPE.NAME`), is rejected in the form and
does not call the server.

HTTP **400** (name invalid), **403** (not Admin or Designer), and **409** (delivery type name
already exists) stay in the rename form error region. Those responses do not change the name
on the list. Saving a delivery type (which can also change the bean name and description),
setting only the description, copying a delivery type, and renaming an edition are separate
actions.

### Set a delivery type description (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
On a delivery type row, **Description** opens **Delivery type description** (it does not call
the server). The form shows the current **name** and **bean name** and does not let you change
them. Change **Description**. **Save description** sends
`PUT …/sitemanage/publishingdesign/deliverytypes/{deliveryTypeId}` with a `deliveryType` object
that contains only the description. Name, bean name, and whether unpublishing requires assembly
are omitted, so the server leaves those stored. A blank description clears the stored
description.

**Delivery types** keeps the previous description until that update succeeds, then shows the
new description (or none, when cleared) with the same name and bean name. **Cancel** closes
the form and does not call the server. A description longer than 255 characters
(`PSX_DELIVERY_TYPE.DESCRIPTION`) is rejected in the form and does not call the server.

HTTP **400** (description longer than 255 characters), **403** (not Admin or Designer), and
**409** (for example a delivery type name conflict on the same update) stay in the description
form error region. Those responses do not change the description on the list. Saving a delivery
type (which can also change the name and bean name), renaming a delivery type, setting only the
bean name, and copying a delivery type are separate actions.

### Set a delivery type bean name (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
On a delivery type row, **Bean name** opens **Delivery type bean name** (it does not call the
server). The form shows the current **name** and **description** and does not let you change
them. Change **Bean name**. **Save bean name** sends
`PUT …/sitemanage/publishingdesign/deliverytypes/{deliveryTypeId}` with a `deliveryType` object
that contains only the bean name. Name, description, and whether unpublishing requires assembly
are omitted, so the server leaves those stored. A blank bean name does not clear the stored
bean name and does not call the server.

**Delivery types** keeps the previous bean name until that update succeeds, then shows the new
bean name with the same name and description. **Cancel** closes the form and does not call the
server. A bean name longer than 255 characters (`PSX_DELIVERY_TYPE.BEAN_NAME`) is rejected in
the form and does not call the server.

HTTP **400** (bean name blank or longer than 255 characters), **403** (not Admin or Designer),
and **409** (for example a delivery type name conflict on the same update) stay in the bean-name
form error region. Those responses do not change the bean name on the list. Saving a delivery
type (which can also change the name and description), renaming a delivery type, setting only
the description, and copying a delivery type are separate actions.

### Copy a delivery type (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
On a delivery type row, **Copy** opens **Copy delivery type** (it does not call the server).
The form shows the source **bean name** and **description** and does not let you change them.
Enter a **new name**. **Copy delivery type** posts
`POST …/sitemanage/publishingdesign/deliverytypes` with a `deliveryType` object: the new name
plus the source bean name, description, and whether unpublishing requires assembly. The source
id is not sent. There is no separate copy resource. The source row is not changed.

The new name appears in **Delivery types** only after that create succeeds.
**Cancel** closes the form and does not call the server. A blank name, or a name longer than
50 characters (`PSX_DELIVERY_TYPE.NAME`), is rejected in the form and does not call the server.

HTTP **400** (name invalid or bean missing), **403** (not Admin or Designer), and **409**
(delivery type name already exists) stay in the copy form error region. Those responses do
not add the new name to the list. Saving a delivery type, and copying an edition, a content
list, or a location scheme, are separate actions. Deleting a delivery type is described below.

### Delete a delivery type (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then **Delivery types**.
On a delivery type row, **Delete** asks **Delete this design object? This cannot be undone.**
Dismissing that confirm does not call the server, and the row stays. Confirm sends
`DELETE …/sitemanage/publishingdesign/deliverytypes/{deliveryTypeId}` (the same delete already
used by Design; there is no second delete resource). The row leaves **Delivery types** only
after the delete succeeds. If the list refresh fails after a successful delete, that delivery
type is still removed. Other delivery types stay.

A delivery type that a content list still names is not deleted. The content list URL parameter
`sys_deliverytype` must no longer equal that delivery type's name (change or delete the content
list first). This action does not change content lists, editions, or publishing contexts, and
it does not rename the type or edit its bean name. HTTP **409** (**Delivery type is in use**),
**403** (not Admin or Designer), and **400** (delivery type id missing) stay in the delivery-types
error region. Those responses do not remove the row and do not claim it was deleted. Saving,
renaming, and copying a delivery type are separate actions.

### Save a publishing context (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. **Add context** (or **Edit context** for the selected context).
Enter a **name** (required) and optional description, then **Save**. Unsaved field edits
mark the form dirty; leaving the editor prompts to discard. The shell posts
`POST …/sitemanage/publishingdesign/contexts` (create) or
`PUT …/sitemanage/publishingdesign/contexts/{contextId}` (update).

HTTP **403** (not Admin or Designer) and **409** (publishing context name already exists)
are shown in the context editor error region — not as a successful save. Create and update
post a `context` object (`name`, optional `description`). A flat JSON body without that
root is not saved. Location-scheme save, delivery-type save, and edition save are separate
Design actions. Renaming a publishing context is described below and does not change the
description or move location schemes. Setting a publishing context description is also
described below and does not change the name or move location schemes. Copying a publishing
context is also described below.

### Rename a publishing context (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**. **Rename context** opens **Rename context**
(it does not call the server). The form shows the current **description** and does not
let you change it. Location schemes are not edited. Change **Name**. **Rename context**
sends `PUT …/sitemanage/publishingdesign/contexts/{contextId}` with a `context` object
that contains only the new name. Description and the default scheme are omitted, so the
server leaves those stored. The context id does not change, so location schemes that
were on the context stay on it.

The **Context** list keeps the previous name until that update succeeds, then shows the
new name. The location schemes for that context stay listed.
**Cancel** closes the form and does not call the server. A blank name, or a name longer
than 50 characters (`RXCONTEXT.CONTEXTNAME`), is rejected in the form and does not call
the server.

HTTP **400** (name invalid), **403** (not Admin or Designer), and **409** (publishing
context name already exists) stay in the rename form error region. Those responses do
not change the name on the list and do not move location schemes. Saving a publishing
context (which can also change the description), setting only the description, copying
one, and deleting one are separate actions.

### Set a publishing context description (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**. **Description** opens **Context description**
(it does not call the server). The form shows the current **name** and the location scheme
names and does not let you change them. Change **Description**. **Save description** sends
`PUT …/sitemanage/publishingdesign/contexts/{contextId}` with a `context` object that
contains only the description. Name and the default scheme are omitted, so the server leaves
those stored. The context id does not change, so location schemes that were on the context
stay on it. A blank description clears the stored description.

The **Context** list keeps the previous description until that update succeeds, then shows
the new description (or none, when cleared) with the same name. The location schemes for
that context stay listed. **Cancel** closes the form and does not call the server. A
description longer than 255 characters (`RXCONTEXT.CONTEXTDESC`) is rejected in the form
and does not call the server.

HTTP **400** (description longer than 255 characters), **403** (not Admin or Designer), and
**409** (for example a publishing context name conflict on the same update) stay in the
description form error region. Those responses do not change the description on the list
and do not move location schemes. Saving a publishing context (which can also change the
name), renaming a publishing context, and copying one are separate actions.

### Copy a publishing context (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**. **Copy context** opens **Copy context**
(it does not call the server). The form shows the source **description** and does not
let you change it. Location schemes are not copied. Enter a **new name**. **Copy context**
posts `POST …/sitemanage/publishingdesign/contexts` with a `context` object: the new name
plus the source description. The source id and default scheme are not sent. There is no
separate copy resource. The source context is not changed, and its location schemes stay
on that context.

The new name appears in the **Context** list only after that create succeeds.
**Cancel** closes the form and does not call the server. A blank name, or a name longer than
50 characters (`RXCONTEXT.CONTEXTNAME`), is rejected in the form and does not call the server.

HTTP **400** (name missing or too long), **403** (not Admin or Designer), and **409**
(publishing context name already exists) stay in the copy form error region. Those responses
do not add the new name to the list and do not move location schemes. Saving a publishing
context, renaming one, deleting one, and copying a location scheme are separate actions.

### Delete a publishing context (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context** that has no location schemes.
**Delete context** asks **Delete this design object? This cannot be undone.**
Dismissing that confirm does not call the server, and the context stays in the
list. Confirm sends `DELETE …/sitemanage/publishingdesign/contexts/{contextId}`
(the same delete already used by Design; there is no second delete resource).
The context leaves the list only after the delete succeeds. If the list refresh
fails after a successful delete, that context is still removed. Other contexts stay.

A context that still has location schemes is not deleted. Remove those schemes
first. This action does not delete the schemes. HTTP **409** (**Publishing context
has location schemes**), **403** (not Admin or Designer), and **400** (context id
missing) stay in the contexts error region. Those responses do not remove the
context and do not claim it was deleted. Saving a publishing context, and deleting
a location scheme, are separate actions.

### Save a location scheme (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**, then **Add scheme** (or open an existing scheme).
Enter a **name** and **generator** (required), optional description, content type id, template id,
and parameters, then **Save**. Unsaved field edits mark the form dirty; leaving the editor prompts
to discard. The shell posts
`POST …/sitemanage/publishingdesign/contexts/{contextId}/schemes` (create) or
`PUT …/sitemanage/publishingdesign/schemes/{schemeId}` (update).

HTTP **403** (not Admin or Designer) and **409** (location scheme name already exists in that
context) are shown in the scheme editor error region — not as a successful save. Context save,
delivery-type save, and edition save are separate Design actions.

### Copy a location scheme (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**. On a location scheme row, **Copy** opens
**Copy location scheme** (it does not call the server). Enter a **new name**. **Copy scheme**
loads the source scheme and posts
`POST …/sitemanage/publishingdesign/contexts/{contextId}/schemes` with a `locationScheme`
object: the new name plus the source generator, description, content type, template, and
`schemeParameter` entries (the location path is one of those parameters), with `copy` set
so the server can tell this create from **Add scheme**. A flat JSON body without the
`locationScheme` root is not saved. There is no separate copy resource.

The database allows one location scheme for each context, template, and content type
(`UIX_RXLOCSCHEME`). The copy keeps the source scheme's assignment in place, so publishing
that template is unchanged. The new row is still saved on the same context: when that
assignment is already used, the copy is stored with a different template id and the same
content type, generator, and parameters. It appears in the context's location-scheme list
only after that create succeeds.
**Cancel** closes the form and does not call the server. A blank name, or a name longer than
50 characters (`RXLOCATIONSCHEME.SCHEMENAME`), is rejected in the form and does not call the
server.

HTTP **400** (name or generator invalid), **403** (not Admin or Designer), and **409** (that
name already exists in the context) stay in the copy form error region. Those responses do
not add the new name to the list. Editing a location scheme, deleting one, and copying an
edition are separate actions.

### Delete a location scheme (Design)

From **Publish** (`spa.jsp?entry=publish&section=design`), open **Design** then
**Contexts / schemes**. Choose a **context**. On a location scheme row, **Delete** asks
**Delete this design object? This cannot be undone.** Dismissing that confirm does not
call the server, and the row stays on that context. Confirm sends
`DELETE …/sitemanage/publishingdesign/schemes/{schemeId}` (the same delete already used
by Design; there is no second delete resource). The row leaves that context's list only
after the delete succeeds. If the list refresh fails after a successful delete, that row
is still removed. Other schemes on the context stay.

HTTP **400** (scheme id missing or invalid), **403** (not allowed), and **409** (the scheme
cannot be deleted, for example it is still in use) stay in the contexts error region.
Those responses do not remove the row and do not claim the scheme was deleted. Saving or
copying a location scheme, and deleting an edition, a content list, or a publishing
context, are separate actions.

### Start or stop a publish job (Runtime)

From **Publish** (`spa.jsp?entry=publish&section=runtime`), choose a **site** and
**publish server**. The Runtime list shows editions for that server. **Start** queues
`POST …/sitemanage/publishingdesign/runtime/editions/{editionId}/start`. When a job is
running, **Stop** posts `POST …/sitemanage/publishingdesign/runtime/jobs/{jobId}/stop`
(falls back to ops `stopPublishing` if needed). An idle edition (no running job id)
does not show **Stop**. If both stop calls fail, the Runtime error region shows the
server message (not a successful **Last result**). A running edition also shows **Open job**,
which switches to **Status** and opens the job-detail panel for that job id (the same panel
as choosing the job on Status). An idle edition (no running job id) does not show **Open job**.
**Demand publish** (same Runtime section) uses the selected edition. Enter one or
more **content ids** (comma, space, or semicolon separated) and choose **Queue demand**.
The shell posts
`POST …/sitemanage/publishingdesign/runtime/editions/{editionId}/demand` with
`{ "contentIds": ["…"] }` for the parsed ids. An empty id list does not call the
server; the section shows **Select an edition and enter at least one content id**.
That validation message stays if the publish-server list finishes loading after
**Queue demand** (the edition reload does not clear it).
HTTP errors (for example a missing folder parent) stay in the Runtime error region
and do not switch to Status. Success shows **Last result** with status `queued`,
the request id, and a job id when the publisher has already assigned one.
The **Last result** status region shows
started/cancelled (or the job state) plus job id. Listing may pass `pubServerId` so only
editions on the selected server appear. **Filter editions by name** narrows that
loaded list (case-insensitive substring). Clearing the filter shows every edition
again. No name match is an empty state, not an error; start and stop errors stay
in the alert. **Start** and **Stop** still act only on the edition whose button
you use. Design edition save and Sites list filter are separate sections. Runtime chrome (site, publish server, refresh, idle, start,
demand publish, content ids, queue demand, clear site record, purge log, and the
related prompts) is loaded from the publishing message catalog. The default
**en-us** pack supplies those labels; other locales fall back to that English
until they are translated.

### Search and filter publish logs

From **Publish** (`spa.jsp?entry=publish&section=logs`), the **Logs** section lists historical
publish jobs. Choose a **site**, optional **server id**, **days** window, and **max count**, then
click **Logs** to load rows (`POST …/sitemanage/pubstatus/logs`). The **days** value
(3, 5, or 10) is the server query window: the publish-status lookup includes a start-date
bound for that many days and a max-count limit. It does not load every historical job and
drop older rows in memory. Use **Status** (All / Failed /
Success) and **Search** to filter the loaded table by site, server, job id, or status without
another round trip. **Failures only (server)** sets `showOnlyFailures` on the logs request so the
server query returns failed jobs only (aborted, canceled, completed with failures, or restart
needed), still inside the same day window. **Export** downloads a CSV of the rows currently on screen
(job, site, server, status), including after **Status** and **Search**. An empty filter still
downloads a header-only file; if the file cannot be built, the section shows
**Could not export the filtered publish logs.** Open **details** on a row for item-level log lines (those details
have their own text filter). From an item row, **view** then **Open in editor** probes
`GET …/itemmanagement/item/fields/{contentId}` and, on success, opens the React content editor
for that content id (`spa.jsp?entry=editor&contentId=…&mode=edit`). HTTP **403** (not allowed)
and **404** (item not found) stay on the log item detail as an error — they do not open a blank
editor window and the log list stays on screen. A row with no content id has no **Open in editor**
action. **Copy location** on an item row writes the published file location
(or the file name when the location is blank) to the clipboard and shows
**Location copied**. **Dismiss** clears that confirmation and does not change
the log list or close the details. A row with neither location nor file name
shows **No location to copy** and does not claim success. If the clipboard
write fails, **Could not copy location** stays on the details panel.

### Filter current jobs by site (Status)

From **Publish** → **Status** (`spa.jsp?entry=publish&section=status`), the current-jobs
table lists jobs returned by `GET …/sitemanage/pubstatus/current`. That list is running jobs
plus jobs that finished as **Completed with failures**, **Failed** (aborted), or publish-server
database reconfigure (**Failed**) during the last day — even after the publisher drops them from
its in-memory active ids. A clean **Completed** job and a user-cancelled job do not stay on this
list (use **Logs** for that history). **Filter Sites**
narrows that table by site name or site id (case-insensitive; no extra publish request).
Clear the field to restore the full list. When jobs are loaded but none match, Status shows
**No jobs match this site filter**. **Filter editions** narrows the same loaded list by
edition name (case-insensitive contains; no extra publish request). Jobs that have no edition
name stay hidden while that box is not empty. Clear **Filter editions** to restore the list
that **Filter Sites** still shows. When jobs are loaded but none match the edition text, Status
shows **No jobs match this edition filter** — that is an empty match, not an error. A later
reload that returns HTTP **403** keeps Status on screen with the error; it does not replace the
panel with a blank page. When the server returns no jobs at all, Status still shows
**No active publishing jobs**. **Stop** is unchanged and applies only to visible running jobs.

### Job detail (Status)

From **Publish** → **Status**, choose a job’s site name. A detail panel shows the job id, site, edition name when the status API includes one, and status. Optional fields that the API omits are left blank. When the job failed (including **Completed with failures**) and the API includes error text, the panel shows that text. The same panel lists that job’s content items from `POST …/sitemanage/pubstatus/details` (the log-details payload). Each row shows the content id and a name or title when the payload includes one (`title`, `name`, or the item file name). An empty item list is **No content items for this job**, not an error. **Open in editor** on a row probes `GET …/itemmanagement/item/fields/{contentId}` and, on success, opens the React content editor (`spa.jsp?entry=editor&contentId=…&mode=edit`). A row with no content id has no **Open in editor** control. HTTP **403** and **404** on the item list stay in the detail panel (the job fields remain; the list is not treated as empty success). The same 403/404 on the editor probe stay in the panel and do not open a blank editor window. A failed job stays on Status for the last day after it leaves the active id set; while the publisher still has the live job, the panel includes that publisher message. **Close** (Back) hides the panel and does **not** stop the job. **Stop** stays on the row for running jobs and still requires confirm.

### Cancel or stop an in-flight publish job

From **Publish** → **Status**, or from a site workspace **Status** list, **Stop** is shown only for jobs whose status is running (not completed, failed, or already stopping). Confirm the dialog. The shell posts `POST …/publishmanagement/servers/stopPublishing/{jobId}`. Success refreshes the Status list. Dismissing the confirm does not call the server. Jobs that are not running have no Stop control. HTTP **403** (forbidden), **404** (unknown job), and **409** (job cannot be stopped — already finished or already stopping) are shown as errors in Status; they are **not** treated as success.

### Item Publish Now and Take Down (Content Explorer)

From **Content Explorer** (`spa.jsp?entry=explorer`), select a **page**, **asset**, or **folder**. **Publish Now** demand-publishes a page or asset. For a folder, one confirm publishes the pages and assets directly in that folder (nested folders are named and skipped). **Take Down** unpublishes a page or asset from its site (not a folder). **Schedule** sets or clears item publish and removal dates (`GET …/getitemdates/{id}`, `POST …/setitemdates`). **Clear scheduled dates** clears both dates after one confirm: one page or asset uses a dialog (HTTP **400** / **403** / **409** stay on that dialog); two or more checked pages or assets use one confirm, skip and name folders, and do not treat a per-item **400** / **403** / **409** as full success. Cancel does not write. **Publishing History** shows item-level publish/takedown rows (`GET …/item/pubhistory/{id}`). Publish Now, Take Down, Stage, and Schedule confirm first. Application-level `FORBIDDEN` / `BADCONFIG` / `INVALID` responses are failures (the Server actions error region), not success. HTTP **404** / **403** on publishing history are errors in the history dialog, not empty success. See [Content Explorer](id:admin-content-explorer) for the exact URLs, linked-page confirm, schedule fields, and history dialog. Stage from Explorer is a separate action.

From the **React Content Editor** (`spa.jsp?entry=editor`) in **Edit** mode, **Publish now** demand-publishes the already-open page or asset after confirm (same sitemanage `publish/page/{id}` or `publish/resource/{id}` GETs). **View** mode stays read-only. `FORBIDDEN` / `BADCONFIG` is a failure on the editor host, not success.

**Schedule** on that same editor host loads and saves start/end publish dates for the already-open page or asset (`GET /services/itemmanagement/item/getitemdates/{id}`, `POST /services/itemmanagement/item/setitemdates`). Cancel does not write. Invalid ranges stay on the form. HTTP **400** / **403** and application-level `FORBIDDEN` / `INVALID` / `BADCONFIG` are not success. **View** mode has no **Schedule** control. Explorer and the Publishing site workspace keep their own schedule editors.

**Clear schedule** on that same editor host clears the start and removal dates for the already-open page or asset after one confirm. It loads `GET /services/itemmanagement/item/getitemdates/{id}`, then posts empty dates (`POST /services/itemmanagement/item/setitemdates` with empty `startDate`, `endDate`, and `comments`). Success reloads those dates and shows them empty only when the reload has neither date. **Cancel** does not call the server and does not reload. HTTP **400**, **403**, and **409**, and HTTP 200 with application-level `FORBIDDEN` / `INVALID` / `BADCONFIG`, stay on the confirm — they are not success, and the dates on that confirm stay as loaded. **View** mode has no **Clear schedule** control. Setting a non-empty schedule stays on **Schedule**. Explorer **Clear scheduled dates** is a separate action.

**Approve for incremental** on that same editor host approves the already-open page or asset onto the incremental publish queue after one confirm. Confirm posts `POST /services/sitemanage/publish/incremental/explorer/{contentId}/approve` (the same Explorer incremental-queue client, not a second queue API). **Approved onto the incremental queue** is shown only after HTTP 204. **Cancel** does not call the server. A template or other item that is not a page or asset is not queued and is not marked approved. HTTP **400**, **403**, and **409** stay on the confirm and do not claim a new approval. **View** mode has no **Approve for incremental** control. This is not remove from the queue, not **Publish now**, not **Stage**, and not **Schedule**. **Unapprove from incremental** is a separate control. Folders are not edited in this host.

**Unapprove from incremental** on that same editor host removes approval from the already-open page or asset on the incremental publish queue after one confirm. Confirm posts `POST /services/sitemanage/publish/incremental/explorer/{contentId}/unapprove` (the same Explorer incremental-queue client, not a second queue API). **Unapproved from the incremental queue** is shown, and **Approved onto the incremental queue** is cleared, only after that call succeeds. **Cancel** does not call the server. A template, folder, or other item that is not a page or asset is not sent to the server and is not marked unapproved. HTTP **400**, **403**, and **409** stay on the confirm, leave **Approved onto the incremental queue** in place, and do not claim the item was unapproved. **View** mode has no **Unapprove from incremental** control. This is not approve, not **Remove from incremental**, not **Publish now**, not **Stage**, and not **Schedule**.

**Remove from incremental** on that same editor host removes the already-open page or asset from the incremental publish queue after one confirm. Confirm posts `POST /services/sitemanage/publish/incremental/explorer/{contentId}/remove` (the same Explorer incremental-queue client, not a second queue API). **Removed from the incremental queue** is shown, and any **Approved onto the incremental queue** or **Unapproved from the incremental queue** mark is cleared, only after that call succeeds. **Cancel** does not call the server. A template, folder, or other item that is not a page or asset is not sent to the server and is not marked removed. HTTP **400**, **403**, and **409** stay on the confirm and do not claim the item was removed. **View** mode has no **Remove from incremental** control. This is not approve, not unapprove, not **Publish now**, not **Stage**, and not **Schedule**. Explorer **Remove from incremental queue** stays the Explorer action.

**Stage** on that same editor host copies the already-open page or asset to staging after confirm (`GET /services/sitemanage/publish/page/staging/{id}` or `/resource/staging/{id}`). Cancel does not call staging. **View** mode, promote mode, templates, folders, and a new unsaved item have no **Stage** control. HTTP **400** and **403**, and HTTP 200 with application-level `FORBIDDEN` / `BADCONFIG` / `INVALID`, stay on the editor host as a failure — they are not success. Stage does not publish live and does not take the item down.

**Remove from staging** on that same editor host removes the already-open page or asset from staging after confirm (`GET /services/sitemanage/publish/takedown/page/staging/{id}` or `/takedown/resource/staging/{id}`). Cancel does not call the service. **View** mode, promote mode, templates, folders, and a new unsaved item have no **Remove from staging** control. HTTP **400** and **403**, and HTTP 200 with application-level `FORBIDDEN` / `BADCONFIG` / `INVALID`, stay on the editor host as a failure — they are not success. Remove from staging does not unpublish the live site and does not delete the CMS item. Explorer and the Publishing site workspace still have their own remove-from-staging actions.

**Publishing history** on that same editor host (Edit or View) lists item-level publish and takedown rows for the already-open page or asset (`GET /services/itemmanagement/item/pubhistory/{id}`). It does not publish or take down. An item that has never been published shows an empty-history message. HTTP **404** and **403** are errors in the history dialog, not empty success. Promote mode, folders, templates, and a new unsaved item have no **Publishing history** control.

**Take down** on that same editor host unpublishes the already-open page or asset (same meaning as Explorer **Take Down**). Confirm first. Linked pages (`GET /services/itemmanagement/item/findLinkedItems/{id}`) are listed on the confirm (up to ten). Confirm then calls `GET /services/sitemanage/publish/takedown/page/{id}` or `/takedown/resource/{id}`, or `PUT` of the linked-page list when that list is non-empty. Cancel does not call takedown. **View** mode, folders, templates, and a new unsaved item (no content id yet) have no **Take down** control. HTTP 200 with application-level `FORBIDDEN` / `BADCONFIG` / `INVALID` is a failure on the editor host, not success. Take down does not delete the CMS item.

## Virtual Sites and docs builds

For Git/filesystem, CSV/filesystem, SQL/database, or HTTP JSON Virtual Sites such as product documentation:

- Offline / CI builds use `scripts/build-cms-docs.bat` / `scripts/build-cms-docs.sh` to emit static HTML without a full CMS UI session. CSV trees can use `PSVirtualSiteBuildMain … csv-filesystem`. SQL trees use `PSVirtualSiteBuildMain … sql-database` (in-memory H2 only). Local object-key directories use `PSVirtualSiteBuildMain … object-storage` (no cloud credentials). Local iCalendar fixtures use `PSVirtualSiteBuildMain … icalendar` (`calendar.ics` or `_config.yaml` `icalendar.file`; no CalDAV).
- **Build** (`POST /sites/{nameOrId}/virtual/build`) writes a staging tree under
  `{install}/tmp/virtual-sites/{siteKey}` (or an optional `outputRoot`). Each build re-reads the
  current Git/filesystem, CSV tree (`csv-filesystem`), H2 `SELECT` (`sql-database`), HTTP
  JSON catalog (`http-json`: local fixture / loopback `http.url`), object-storage keys
  (`object-storage`: Markdown / HTML / JSON under a local `rootPath`), or rss-atom feeds
  (`rss-atom`: local `feed.xml` / `atom.xml` / `rss.file` or loopback `rss.url`), or
  iCalendar fixtures (`icalendar`: local `calendar.ics` / `icalendar.file`; no CalDAV). After
  `git pull`, a local Markdown edit, a CSV change, a `_config.yaml` change, a SQL
  `queryFile` / `sql.query` change, an H2 row change, a JSON catalog / `_config.yaml`
  edit, an object-storage Markdown / HTML / JSON key or `_config.yaml`
  (`objects.keys`) edit, an RSS/Atom fixture / `_config.yaml` edit, or an iCalendar
  fixture (`calendar.ics` / `icalendar.file`) / `_config.yaml` edit, run Build (or
  Publish) again — no CMS restart. File watchers are not used. `sql-database` requires `_config.yaml` with a `sql:` mapping
  (`jdbc:h2:mem:`; Oracle / MySQL / SQL Server URLs return **400**). `http-json` requires
  `_config.yaml` (versions plus `http.url` or `http.file`); `virtual.remoteUrl` is **400**.
  `object-storage` requires `_config.yaml` and a portable-safe local `rootPath`; leftover
  `virtual.remoteUrl` is **400**.
- **Publish** (`POST /sites/{nameOrId}/virtual/publish`) runs that build, then copies the
  assembled HTML/assets to the Site **filesystem publish location** (`IPSSite.root` / Site
  publishing root). Staging `_meta` files are not copied. Redirect HTML and `redirects.json`
  from optional `_redirects.yaml` are copied with the site. `sql-database` Publish is
  in-memory H2 only (`jdbc:h2:mem:`); Oracle / MySQL / SQL Server URLs return **400**.
  `http-json` Publish uses a local JSON fixture or loopback catalog (`http.url` /
  `http.file` in `_config.yaml`); leftover `virtual.remoteUrl` is **400** (no secrets on
  the envelope). `object-storage` Publish uses a portable-safe local object-key `rootPath`
  (Markdown / HTML / JSON keys; no cloud URLs, IAM, or access keys); leftover
  `virtual.remoteUrl` is **400**. `rss-atom` Publish uses a local RSS 2.0 / Atom
  fixture or loopback feed (`feed.xml` / `atom.xml` or `_config.yaml` `rss.file`); leftover
  `virtual.remoteUrl` and credential properties are **400** (no live feeds). `icalendar`
  Publish uses a local RFC 5545 fixture (`calendar.ics` or `_config.yaml` `icalendar.file`);
  leftover `virtual.remoteUrl` and credential properties are **400** (no CalDAV). REST **GET/PUT**
  can persist `virtual.sourceKind=object-storage` with that local `rootPath` (cloud URLs and
  credentials are **400**); REST **Build** runs that local bucket. REST **Preview**
  streams last-build HTML for that kind after a successful Build (`available=true`;
  missing build is `available=false` HTTP 200). REST **Preview** also streams last-build
  HTML for `rss-atom` (local RSS 2.0 / Atom fixture or loopback feed; no live remote
  feeds) and for `icalendar` (local RFC 5545 `calendar.ics` / `icalendar.file`; no CalDAV).
  REST **Publish** copies that last-build HTML to `IPSSite.root` for `rss-atom` and
  `icalendar` (`filesCopied > 0`; `_meta` skipped). Developer Sites **Preview assembled site** is
  shown for **RSS / Atom**, **iCalendar**, and **Sitemap XML** after a successful Build. Developer Sites can save **Object
  storage** (GET round-trips the kind), then **Build Virtual Site**, **Preview
  assembled site**, and **Publish Virtual Site**. Developer Sites can save **RSS / Atom**
  the same way, then **Build Virtual Site**, **Preview assembled site**, and
  **Publish Virtual Site** (local RSS/Atom fixture; leftover `virtual.remoteUrl` and
  credentials are **400**; no live feeds). Developer Sites can save **iCalendar**
  the same way, then **Build Virtual Site**, **Preview assembled site**, and
  **Publish Virtual Site** (local RFC 5545 fixture; leftover `virtual.remoteUrl` and
  credentials are **400**; no CalDAV). Developer Sites can save **Sitemap XML**
  the same way, then **Build Virtual Site**, **Preview assembled site**, and
  **Publish Virtual Site** (local `sitemap.xml` fixture; leftover `virtual.remoteUrl`
  and credentials are **400**; no live crawl). The endpoint does not accept an
  `outputRoot` body (always the default staging root).

### Publish a Virtual Site to the Site filesystem target

1. Sign in as **Admin**.
2. Configure the Site as a Git-filesystem, CSV-filesystem, SQL-database, HTTP JSON, object-storage, RSS / Atom, iCalendar, or Sitemap XML Virtual Site (see [Sites](id:admin-sites)). After you save **SQL database**, **Build Virtual Site** on Developer Sites runs the same in-memory H2 REST Build as Git/CSV. After you save **HTTP JSON**, **Build Virtual Site** then **Publish Virtual Site** copies assembled HTML to the Site filesystem root. After you save **Object storage**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local object-key tree (`POST …/virtual/publish`; leftover `virtual.remoteUrl` is **400**; no cloud URLs or credentials). After you save **RSS / Atom**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local RSS/Atom fixture (`POST …/virtual/publish`; leftover `virtual.remoteUrl` and credentials are **400**; no live feeds). After you save **iCalendar**, **Build Virtual Site** then **Preview assembled site** and **Publish Virtual Site** run against that local RFC 5545 fixture (`POST …/virtual/publish`; leftover `virtual.remoteUrl` and credentials are **400**; no CalDAV).
3. Set the Site **publishing filesystem root** (Site root) to a dedicated directory on the CMS
   host. Relative roots (legacy values such as `../CI_Home`) are resolved against the CMS
   install directory. Do **not** point it at `virtual.rootPath` (the Markdown or CSV source tree).
4. Confirm the source root exists on the host and that the publish directory is writable.
5. From **Developer → Sites → Site detail**, choose **Publish Virtual Site** (visible for
   **Git filesystem**, **CSV filesystem**, **SQL database**, **HTTP JSON**,
   **Object storage**, **RSS / Atom**, **iCalendar**, **Sitemap XML**, **Robots.txt**, **llms.txt**, **OpenAPI YAML**, **AsyncAPI YAML**, and **GraphQL SDL**; hidden for repository Sites). For **SQL database**, **HTTP JSON**,
   **Object storage**, **RSS / Atom**, and **iCalendar**, save the source, run **Build Virtual Site**, then
   **Publish Virtual Site**. For **Sitemap XML**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; no live crawl). For **Robots.txt**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live crawl). For **llms.txt**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live HTTP fetch). For **OpenAPI YAML**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live spec fetch). For **AsyncAPI YAML**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl` and credentials
   fail closed; missing assemble is **400**; no live spec fetch). For **GraphQL SDL**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl`, credentials, and `graphql.url`
   fail closed; missing assemble is **400**; no live GraphQL HTTP). For **JSON Schema**, save the source, run **Build Virtual Site**, then **Publish Virtual Site**
   (builds then copies last-build local HTML; leftover `virtual.remoteUrl`, credentials, `jsonschema.url`, and remote `$ref`/`$id` HTTP
   fail closed; missing assemble is **400**; no live HTTP schema fetch). The panel reports files copied and the destination path, or a
   clear error. Integrators can call `POST /services/sites/{nameOrId}/virtual/publish`
   instead (Git, CSV, SQL, HTTP JSON, object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, llms-txt, openapi-yaml, asyncapi-yaml, graphql-sdl, and json-schema).
   `sitemap-xml` REST Publish copies assembled HTML from a local `sitemap.xml` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl).
   `robots-txt` REST Publish copies assembled HTML from a local `robots.txt` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live crawl).
   `llms-txt` REST Publish copies assembled HTML from a local `llms.txt` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live HTTP fetch).
   `openapi-yaml` REST Publish copies assembled HTML from a local `openapi.yaml` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live spec fetch).
   `asyncapi-yaml` REST Publish copies assembled HTML from a local `asyncapi.yaml` fixture (leftover
   `virtual.remoteUrl`, credentials, and cloud URL `rootPath` are **400**; no live spec fetch).
   `graphql-sdl` REST Publish copies assembled HTML from a local `schema.graphql` fixture (leftover
   `virtual.remoteUrl`, credentials, cloud URL `rootPath`, and `graphql.url` are **400**; no live GraphQL HTTP).
   `json-schema` REST Publish copies assembled HTML from a local `schema.json` fixture (leftover
   `virtual.remoteUrl`, credentials, cloud URL `rootPath`, `jsonschema.url`, and remote `$ref`/`$id` HTTP are **400**; no live HTTP schema fetch).
   Developer Sites **Publish Virtual Site** is shown for **Sitemap XML**, **Robots.txt**, **llms.txt**, **OpenAPI YAML**, **AsyncAPI YAML**, **GraphQL SDL**, and **JSON Schema**. Run **Build Virtual Site** first
   if you only want staging output.
6. On success, the result includes `publishPath`, `filesCopied`, `pagesWritten`, and any
   link problems (`hasLinkProblems` can be true with HTTP 200).
7. Spot-check `index.html` (and version folders such as `8.2/`) under the Site root. If the
   source tree includes `_redirects.yaml`, also spot-check a redirect HTML path and
   `redirects.json`.

**Clear operator errors (HTTP 400, not a silent no-op):**

- Site is not Virtual (`virtual.sourceKind` blank or `repository`)
- Site filesystem publish root is not configured
- Publish root is unsafe (`..` after normalize) or is a file rather than a directory
- Publish root overlaps `virtual.rootPath` or the build staging tree
- Caller is not Admin (403)

See [Virtual Sites](id:developer-virtual-sites) and [Build product docs](id:developer-build-source#product-docs-build).

## Item publishing history

From **Publish** (`spa.jsp?entry=publish`), open **Status** or **Logs**. The **Item publishing history** panel looks up a page or asset by item id using the existing item-management API (`GET /services/itemmanagement/item/pubhistory/{id}`). It replaces the classic jQuery publishing-history dialog for this shell. **Content Explorer** uses the same API and panel: select a page or asset and choose **Publishing History**.

1. Sign in as an operator who can open Publish.
2. Open **Publish → Status** or **Publish → Logs**.
3. Enter the item id (content GUID such as `16777215-101-9`) and choose **View History**.
4. Rows show server, location, revision, date, operation, and status (newest first). A **FAILURE** row keeps the server error on the status cell title.

**Empty and error states:** If the item has never been published, the panel says there is no publishing history. A failed lookup is an error, not empty history and not success:

- HTTP **400** — the item id is blank or not a content id
- HTTP **403** — the caller is not allowed to read that item
- HTTP **404** — the item id is unknown
- Any other HTTP error keeps the server message

**Deep links** (same panel on Status or Logs):

- `spa.jsp?entry=publish&section=status&itemId={id}`
- `spa.jsp?entry=publish&section=logs&itemId={id}`
- Path form: `/cm/app/publish/status?itemId={id}` and `/cm/app/publish/logs?itemId={id}`

Use **Open Logs** / **Open Status** in the panel to switch those sections without leaving the lookup. Site-level job status and publish logs stay on the same tabs. Schedule dates and site-workspace takedown (below) are separate Publishing steps.

## Schedule publish dates (Publishing site workspace)

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Schedule** panel gets and sets item publish and removal dates using the existing item-management APIs (`GET /services/itemmanagement/item/getitemdates/{id}`, `POST /services/itemmanagement/item/setitemdates`). It replaces the classic jQuery `PercScheduleDialog` for this shell. Explorer still has its own Schedule action for a selected page or asset, including one dialog for every checked page or asset (folders skipped; a partial save is an error).

1. Sign in as an operator who can open Publish.
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`) and choose **Load dates**.
4. Set or clear **Publish date** and **Removal date** (optional comments, 500 characters). Removal must be after publish when both are set. **Save dates** posts the `ItemDates` envelope.

**Empty and error states:** Blank item id is not a lookup. Invalid dates return **HTTP 400** (the panel shows the server validation message — past dates, unparseable values, or removal before publish). Forbidden updates return **HTTP 403** (reader or no assignment, or an application-level `FORBIDDEN` body). If another user has the item checked out, save returns **HTTP 409** and the panel shows that conflict. None of those responses are treated as success. After a successful save the panel reloads the stored publish and removal dates. The optional comment is the workflow note sent with the save; it is not returned by the dates lookup, so it stays in the form until the next lookup. Clearing both dates and saving removes the schedule.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

Item publishing history stays on Status / Logs. Site-workspace **Publish now** and takedown are below.

## Publish now from the Publishing site workspace

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Publish Now** panel demand-publishes a selected **page** or **asset** using the same sitemanage paths as Content Explorer and the React editor (`GET /services/sitemanage/publish/page/{id}` or `/resource/{id}`). It replaces the classic jQuery `PercItemPublisherService.publishItem` action for this shell. Explorer still has its own **Publish Now** action for a Finder row.

1. Sign in as an **Admin** (or an operator who can demand-publish).
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, and choose **Review publish now**.
4. Confirm **Publish this item now?** then **Publish Now**. The site workspace job list refreshes after a successful start.

**Empty and error states:** Blank or invalid item id is not a publish. HTTP **403** (non-Admin or locked/forbidden item) and HTTP **400** (validation) are shown in the panel — they are **not** treated as success. HTTP **404** (unknown id) shows **Item not found**. HTTP 200 with application-level `FORBIDDEN` or `BADCONFIG` is also a failure (same as Explorer). Stage / unstage have their own panels; the **Available publishing actions** menu above them jumps to each panel for the same item.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

## Take down from the Publishing site workspace

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Remove From Site** panel takes down (unpublishes) a page or asset using the same sitemanage paths as Content Explorer and classic Finder (`GET /services/sitemanage/publish/takedown/page/{id}` or `/resource/{id}`; `PUT` of the linked-page list when that list is non-empty). Take down does **not** delete the CMS content item. Linked pages come from `GET /services/itemmanagement/item/findLinkedItems/{id}` and are listed on confirm (up to ten). A failed linked-item lookup still proceeds with the confirm (classic Finder).

1. Sign in as an operator who can open Publish.
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, and choose **Review take down**.
4. Confirm lists linked page paths when present. Choose **Take down** to unpublish.

**Empty and error states:** Blank item id is not a takedown. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is a failure — the panel shows the server warning and does **not** treat the item as unpublished. HTTP **400** (validation), **403** (forbidden), **404** (item not found), and **409** (someone else is editing the item) are shown in the panel and are **not** success. The content item remains in the CMS. Content Explorer Take Down remains the selection-based action for a Finder row.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}`

Item publishing history stays on Status / Logs. Schedule dates stay on the same site workspace.

## Stage and Remove from Staging (Publishing site workspace)

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Stage / Remove From Staging** panel stages or removes-from-staging a selected **page** or **asset** using the same sitemanage paths as Content Explorer and classic Finder (`GET /services/sitemanage/publish/page/staging/{id}` or `/resource/staging/{id}` to stage; `GET /services/sitemanage/publish/takedown/page/staging/{id}` or `/takedown/resource/staging/{id}` to remove from staging). It replaces the classic jQuery `PercItemPublisherService.stageItem` / `removeItemFromStaging` actions for this shell. Explorer still has its own **Stage** and **Remove from Staging** actions for a Finder row.

1. Sign in as an **Admin** (or an operator who can stage).
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId`).
3. Enter the item id (content GUID such as `16777215-101-9`), choose **Page** or **Asset**, choose **Stage** or **Remove from staging**, and choose **Review stage**.
4. Confirm then submit. The site workspace job list refreshes after a successful stage / unstage.

**Empty and error states:** Blank or invalid item id is not a stage. HTTP **403** (non-Admin or locked item) and HTTP **400** (validation) are shown in the panel — they are **not** treated as success. HTTP **404** (unknown id) shows **Item not found**. HTTP 200 with application-level `FORBIDDEN`, `BADCONFIG`, `NOSTAGING_SERVERS`, or `INVALID` is also a failure (same as Explorer and classic Finder). Take down and publish-now are separate panels on the same workspace.

**Deep links:**

- `spa.jsp?entry=publish&section=sites&siteId={siteId}&itemId={id}&action={stage|unstage}`
- Path form: `/cm/app/publish/sites?siteId={siteId}&itemId={id}&action={stage|unstage}`

## Available publishing actions menu (Publishing site workspace)

From **Publish** (`spa.jsp?entry=publish`), open a **site workspace** (Sites, then a site). The **Available publishing actions** menu calls the existing sitemanage endpoint (`GET /services/sitemanage/publish/publishingActions/{id}`) and renders one button per server row — **Publish**, **Schedule...**, **Remove from Site**, **Stage**, **Remove from Staging**. It replaces the classic jQuery `PercItemPublisherService` get-publishing-actions menu for this shell; it adds no new REST and reuses the publish-now, schedule, takedown, and stage panels below it.

1. Sign in as an **Admin** (or an operator with publish rights on the item).
2. Open **Publish → Sites** and select a site (or deep-link `section=sites` with `siteId` and `itemId`).
3. Enter the item id (or follow the deep-linked one) and choose **Load actions**.
4. Rows the server marks unavailable render **disabled** — they cannot be clicked. Unknown action names are skipped.
5. Choose an enabled action to jump to its panel (publish now, schedule, takedown, stage) for the same item.

**Empty and error states:** Blank or invalid item id is not a lookup. HTTP **403** (non-Admin / no publish rights) shows **Publish Forbidden** — it is **not** treated as success. HTTP **404** (unknown id) shows **Item not found**. An item with no applicable actions shows **No publishing actions for this item**.

## Failure modes to watch

- Missing template/variant or broken relationship links
- File permission errors on delivery paths
- Partial publish after mid-job failure (re-run after fixing root cause)
- DTS connectivity issues that surface as broken dynamic widgets on an otherwise static site

## Related

- [Sites & content structure](id:admin-sites)
- [Server operations](id:admin-server-ops)
