package com.percussion.webservices.systemdesign;

/**
 * Minimal DTO representing the permissions response for saveAcls webservice.
 * This is a tiny compatibility shim to aid compilation during stabilization.
 */
public class SaveAclsResponsePermissions
{
    private long id;
    private int[] permission;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int[] getPermission()
    {
        return permission;
    }

    public void setPermission(int[] permission)
    {
        this.permission = permission;
    }
}
