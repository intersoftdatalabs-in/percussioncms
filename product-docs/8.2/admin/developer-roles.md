---
id: admin-developer-roles
title: Developer Roles
description: Browse CMS security roles, view the users on one role, add or remove one user, create a role, update one description, set or clear one home page, and delete one role
version: "8.2"
order: 46
tags: [admin, developer, roles, security]
---

# Developer Roles

**Developer → Roles** is the Security Design catalog of system roles.
It mirrors the classic Workbench **Security Design → Roles** navigator folders:

| Group | Meaning |
|-------|---------|
| **Community** | Role is assigned to at least one community |
| **Workflow** | Role is assigned to at least one workflow |
| **Unassigned** | Role is in neither community nor workflow membership |

A role that is both community- and workflow-assigned appears under **both** groups.
An Admin can **create** one role (name and description), **open** one role to see
the users who belong to it, **add one existing user** to that role, **remove one
user** from that role after confirming, **edit the description**
of one existing role, **set or clear the home page** of one existing role, and
**delete** one existing role from this catalog.
**System** and **Default** cannot be deleted.
Opening a role lists stored user names. A role with no users shows an empty
membership state, not an error. This chrome adds or removes one user at a time.
It does **not** replace the whole member list — use **Admin → Roles** for bulk
membership edits, and **Developer → Communities** detail for community role
association. Packaged roles such as Admin and Designer are not
created by this form. If the server rejects a description change, an add, a
remove, or a delete, the catalog keeps the previous row and the member list
stays as it was. A failed user read does not invent members. A remove that
would leave a user unable to log in stays a visible error; that user stays
on the role.

## Product path — browse

1. Sign in as **Admin** (the browse catalog requires the Admin role).
2. Open **Developer → Roles**, or deep-link
   `spa.jsp?entry=developer&section=roles` (aliases: `role`, `se03`).
3. The panel loads the full catalog and shows three expandable groups with
   role counts. Expand or collapse a group to show or hide its table.
4. Use the **All groups** / **Community** / **Workflow** / **Unassigned** filters
   to focus on one navigator folder.
5. Each row shows the role name, description (when known), home page (when
   stored), communities that include the role, and workflows that include the
   role. Select a row to edit that role's description or home page.

Non-Admin sessions receive **403** from the catalog API and the panel shows an
error. An empty catalog is a valid **200** with no rows.

## Product path — create a role

1. Sign in as **Admin**.
2. Open **Developer → Roles**.
3. Choose **Create role**.
4. Enter a role name. Description is optional. **Cancel** closes the form and
   does not call the server. A blank or whitespace-only name stays on the form;
   **Create role** stays disabled and no request is sent.
5. Choose **Create role** on the form. The catalog does not list the new role
   until the server returns success, then the catalog reloads.
6. The existing role service also adds the new role to workflows, so the row
   appears under **Workflow** (and under **Community** only if a community
   already includes that name). Member changes are not part of this form.

HTTP **400** (blank, invalid, or duplicate name) and **403** (not Admin) leave
the form in an error state. The catalog does not show the role as created.

## Product path — update a description

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. Select the row of an existing role. The description form shows that role's
   name (read-only) and its current description.
4. **Cancel** closes the form and does not call the server. The row keeps the
   previous description.
5. Change the description and choose **Save description**. The row does not
   show the new text until the server returns success and the catalog reloads.
   A blank description clears the stored description.
6. The save does not rename the role, change its home page, or change its
   members.

HTTP **400** (description longer than 255 characters, or the role service
rejects the update), **403** (not Admin), and **404** (the role no longer
exists) leave the form in an error state. The catalog row keeps the previous
description. `update=true` does not create a missing role.

## Product path — view users on one role

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. Select the row of an existing role. The detail loads that role
   (`GET /services/roles/{roleName}`) and lists each stored user name.
4. A role with no users shows **No users on this role.** That is not an error.
5. Use **Add user** below the list to add one existing user, or **Remove**
   beside a name to drop that one user (see the next sections).
6. **Cancel** closes the detail and does not change membership.

HTTP **403** (not allowed to read the role) and **404** (the role no longer
exists) show an error on the detail and do not list members, even when the
error body contains names. The catalog row stays. Saving a description or a
home page still does not add or drop users.

## Product path — add one existing user

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. Select the row of an existing role. The detail lists that role's users.
4. **Cancel** closes the detail and does not call the server. Membership stays
   as it was.
5. Enter the name of one user who already exists and choose **Add user**. A
   blank or whitespace-only name keeps **Add user** disabled and is not sent.
6. The name is not shown in the user list until the server returns success.
   The list then shows the stored members, including that user. The role name,
   description, and home page stay the same. Other members stay.
7. An unknown user is **400**. The previous list stays. A user who is already
   a member is **409**. The previous list stays. **403** (not Admin) also
   leaves the previous list.

`addUser=true` does not create a missing role (**404**) and does not create a
user. Do not send `addUser=true` together with `create=true`, `update=true`,
`homePage=true`, or `removeUser=true` (**400**). Sending more than one user name is **400** and
does not replace the member list. A description save still ignores a client
user list.

## Product path — remove one user

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. Select the row of an existing role. The detail lists that role's users.
4. Choose **Remove** beside one user. **Cancel** closes the confirm dialog and
   does not call the server. That user stays on the list. Other members stay.
5. Choose **Remove** in the dialog. The name stays in the list until the server
   returns success. The list then shows the stored members without that user.
   The role name, description, and home page stay the same. Other members stay.
6. Closing the detail with **Cancel** does not remove a user.

HTTP **400** (blank name, more than one name, unknown user, or a user who is
not a member), **403** (not Admin), and **409** (the user would be left unable
to log in, or you would remove yourself from **Admin**) leave that user on the
role. The panel shows the error. It does not say the user was removed. A **409**
body that omits the user is still an error, not a success.

`removeUser=true` does not create a missing role (**404**) and does not delete
the role. Do not send `removeUser=true` together with `create=true`,
`update=true`, `homePage=true`, or `addUser=true` (**400**). Sending more than
one user name is **400** and does not replace the member list. A description
save still ignores a client user list.

## Product path — set or clear a home page

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. Select the row of an existing role. The detail shows that role's name
   (read-only), its description, and its stored home page (blank when none is
   stored).
4. **Cancel** closes the detail and does not call the server. The row keeps
   the previous home page.
5. Enter a landing-page type and choose **Save home page**. The row does not
   show the new home page until the server returns success and the catalog
   reloads. Known values and common aliases are stored in canonical form
   (`Home`, `Explorer`, `Architecture`, `Developer`, `Publish`, `Workflow`,
   and the older `Dashboard`, `Editor`, and `Design` values). `navigation`
   is stored as `Architecture`. `admin` is stored as `Design`.
6. Clear the field and choose **Save home page**. The stored home page is
   removed only after success. The row is blank after reload. Sign-in still
   uses **Home** when a role has no stored home page. Saving the word `Home`
   stores Home; that is not the same as clearing the field.
7. The save does not rename the role, change its description, or change its
   members.

HTTP **400** (the value is not a known landing page, or the role service
rejects the update), **403** (not Admin), and **404** (the role no longer
exists) leave the detail in an error state. The catalog row keeps the previous
home page. `homePage=true` does not create a missing role. Do not send
`homePage=true` together with `create=true` or `update=true` (**400**).

## Product path — delete a role

1. Sign in as **Admin**.
2. Open **Developer → Roles** and wait for the catalog.
3. On a role row, choose **Delete**. **System** and **Default** do not offer
   delete (the control stays disabled) and are not sent to the server.
4. **Cancel** closes the confirm dialog and does not call the server. The row
   stays.
5. Choose **Delete** in the dialog. The catalog does not drop the row until
   the server returns success, then the catalog reloads.
6. Delete removes the CMS role only. If the role is a directory group, the CMS
   link is removed and the group stays in the remote directory. Members of the
   CMS role are removed from that role. The role is also removed from workflows.

HTTP **400** (blank name or a system role), **403** (not Admin), and **409**
(the role would leave a user unable to log in, or a workflow still assigns the
role) leave an error on the catalog. The row stays. The panel does not say the
role was deleted.

## Limits

- Create is name and description only. Opening a role shows its users.
  **Add user** adds one existing user. **Remove** drops one member after
  confirm and does not replace the rest of the list. Description
  save changes the description only. Home-page save changes the home page only
  (a blank value clears it). Delete removes one CMS role. Bulk membership edits
  remain on **Admin → Roles**. Community **Save roles** still assigns roles
  to a community; it is not this user list.
- **System** and **Default** cannot be deleted.
- A duplicate name is rejected. It does not update the existing role.
- LocalContent (internal) workflow assignments are excluded from the workflow
  column, matching Workbench.
- Session community switch in the header is a separate membership list; it is
  not this catalog.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| Full catalog | `GET /services/roles/catalog` |
| One role, including users | `GET /services/roles/{roleName}` |
| Filtered | `GET /services/roles/catalog?group=community\|workflow\|unassigned` |
| Create | `PUT /services/roles/?create=true` with a `Role` object: `name` (required) and optional `description` |
| Update description | `PUT /services/roles/?update=true` with a `Role` object: `name` (required) and `description` (blank clears) |
| Set or clear home page | `PUT /services/roles/?homePage=true` with a `Role` object: `name` (required) and `homePage` (blank clears). Description and users on the body are ignored |
| Add one user | `PUT /services/roles/?addUser=true` with a `Role` object: `name` (required) and `users` containing exactly one existing user name. Description and home page on the body are ignored |
| Remove one user | `PUT /services/roles/?removeUser=true` with a `Role` object: `name` (required) and `users` containing exactly one current member. Description and home page on the body are ignored |
| Delete | `DELETE /services/roles/{roleName}` |

`GET /services/roles/{roleName}` returns the role's `users` list. An empty or
missing list means the role has no users. HTTP **403** and **404** are errors
and are not membership. Description and home-page saves do not send `users`;
the server keeps the stored members.

Create always uses the role create service (`create=true`). It does not update
an existing role. A blank name is **400**. A caller who is not Admin is **403**.
A duplicate or otherwise invalid name is **400**. `update=true` changes the
description of an existing role and does not create a missing name (**404**).
It does not change members, the home page, or the role name. A description
longer than 255 characters is **400**. `homePage=true` changes only the home
page of an existing role and does not create a missing name (**404**). A blank
`homePage` clears the stored value. A value that is not a known landing page
is **400**. It does not change the description, members, or the role name.
`addUser=true` adds one existing user to an existing role and does not create
a missing name (**404**). A blank user name, more than one user name, or an
unknown user is **400** and does not change membership. A user who is already
a member is **409** and does not change membership. The save does not change
the description, home page, or role name. Description update still ignores a
client `users` list, including an empty list, so it cannot clear members.
`removeUser=true` removes one current member of an existing role and does not
create a missing name (**404**) or delete the role. A blank user name, more
than one user name, an unknown user, or a user who is not a member is **400**
and does not change membership. A removal that would leave the user unable to
log in, or that would remove the caller from **Admin**, is **409** and does
not change membership. The save does not change the description, home page,
role name, or the other members. Description update still ignores a client
`users` list.
Without `create=true`, `update=true`, `homePage=true`, `addUser=true`, or
`removeUser=true`, `PUT` still creates a role that is not already defined and
updates the description of a role that is. Do not combine `create=true`,
`update=true`, `homePage=true`, `addUser=true`, and `removeUser=true` (**400**).
Delete is **Admin** only (**403** otherwise). **System** and **Default** are
**400**. A missing name is **404**. A role that would strand a user, or that a
workflow still assigns beyond reader, is **409** and is not deleted. A
directory group loses only the CMS link.

Integrator notes: [REST API](id:developer-rest) (Roles browse catalog). Related
chrome: [Developer Communities](id:admin-developer-communities),
[Users, roles & security](id:admin-users-roles).
