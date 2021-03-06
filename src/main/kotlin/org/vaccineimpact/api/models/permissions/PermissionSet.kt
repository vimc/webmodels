package org.vaccineimpact.api.models.permissions

class PermissionSet(val permissions: Set<ReifiedPermission>): Set<ReifiedPermission> by permissions
{
    constructor(vararg rawPermissions: String)
            : this(rawPermissions.map { ReifiedPermission.parse(it) }.toSet())
    constructor(rawPermissions: Iterable<String>)
            : this(rawPermissions.map { ReifiedPermission.parse(it) }.toSet())

    val names by lazy { permissions.map { it.name }.distinct() }

    infix operator fun plus(other: PermissionSet) = PermissionSet(this.permissions + other.permissions)
    infix operator fun plus(other: ReifiedPermission) = PermissionSet(this.permissions + other)
    infix operator fun plus(other: String) = this + ReifiedPermission.parse(other)

    override fun toString(): String
    {
        return "[${permissions.joinToString()}]"
    }
}