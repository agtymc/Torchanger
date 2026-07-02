package org.agty.torchanger.torchanger;

import java.nio.file.Path;

public record BridgeCatalogEntry(
        String displayName,
        TorLaunchMode mode,
        String fileName,
        String sourceUrl,
        Path importPath
) {
}
