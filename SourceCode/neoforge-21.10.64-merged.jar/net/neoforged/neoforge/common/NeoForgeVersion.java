/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class NeoForgeVersion {
    private NeoForgeVersion() {}

    private static final NeoForgeBuildType BUILD_TYPE = NeoForgeBuildType.valueOf("STABLE");

    public static String getVersion() {
        return "21.10.64";
    }

    public static NeoForgeBuildType getBuildType() {
        return BUILD_TYPE;
    }
}
