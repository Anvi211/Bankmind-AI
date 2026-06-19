package com.bankmind.util;

public class TenantContext {

    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        currentTenant.set(tenantId);
    }

    public static Long getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
