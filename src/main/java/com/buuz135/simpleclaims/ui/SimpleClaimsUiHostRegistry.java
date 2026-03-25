package com.buuz135.simpleclaims.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SimpleClaimsUiHostRegistry {

    private static volatile SimpleClaimsUiHost host;

    private SimpleClaimsUiHostRegistry() {}

    public static void register(@Nonnull SimpleClaimsUiHost uiHost) {
        host = uiHost;
    }

    public static void clear(@Nonnull SimpleClaimsUiHost uiHost) {
        if (host == uiHost) {
            host = null;
        }
    }

    @Nullable
    public static SimpleClaimsUiHost get() {
        return host;
    }
}
