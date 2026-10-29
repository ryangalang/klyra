package com.music.bitchord.ui.theme

import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

/** Every Material role is explicit, including tonal surfaces and selected controls. */
fun klyraColors(dark: Boolean, oled: Boolean = false): ColorScheme {
    val ink = if (dark) Color(0xFFF5F5F7) else Color(0xFF202022)
    val base = if (dark) if (oled) Color.Black else Color(0xFF111113) else Color(0xFFFAFAFA)
    val surface = if (dark) Color(0xFF1C1C1E) else Color.White
    val container = if (dark) Color(0xFF29292C) else Color(0xFFF0F0F2)
    val muted = if (dark) Color(0xFFADADB3) else Color(0xFF68686E)
    val outline = if (dark) Color(0xFF64646A) else Color(0xFF929298)
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary=ink, onPrimary=base, primaryContainer=container, onPrimaryContainer=ink,
        inversePrimary=base, secondary=muted, onSecondary=base, secondaryContainer=container,
        onSecondaryContainer=ink, tertiary=ink, onTertiary=base, tertiaryContainer=container,
        onTertiaryContainer=ink, background=base, onBackground=ink, surface=surface,
        onSurface=ink, surfaceVariant=container, onSurfaceVariant=muted, surfaceTint=Color.Transparent,
        inverseSurface=ink, inverseOnSurface=base, outline=outline, outlineVariant=outline.copy(alpha=.24f),
        scrim=Color.Black, surfaceBright=container, surfaceDim=base, surfaceContainerLowest=base,
        surfaceContainerLow=surface, surfaceContainer=container, surfaceContainerHigh=container,
        surfaceContainerHighest=if(dark) Color(0xFF363639) else Color(0xFFE5E5E8),
        error=if(dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError=if(dark) Color(0xFF690005) else Color.White,
        errorContainer=if(dark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer=if(dark) Color(0xFFFFDAD6) else Color(0xFF410002)
    )
}

