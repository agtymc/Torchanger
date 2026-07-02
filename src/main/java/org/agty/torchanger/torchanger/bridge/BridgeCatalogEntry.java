package org.agty.torchanger.torchanger.bridge;

import java.nio.file.Path;
import org.agty.torchanger.torchanger.tor.profile.TorLaunchMode;

public record BridgeCatalogEntry(
        String displayName,
        TorLaunchMode mode,
        String fileName,
        String sourceUrl,
        Path importPath
) {
}
