package io.github.pelmenstar1.digiDict.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class JvmUtils {
    private JvmUtils() {
    }

    // Like String.format but args isn't declared as varargs.
    @NotNull
    public static String format(@NotNull Locale locale, @NotNull String format, @Nullable Object @NotNull [] args) {
        return String.format(locale, format, args);
    }
}
