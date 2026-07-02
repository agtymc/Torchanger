package org.agty.torchanger.torchanger.tor.profile;

import org.agty.torchanger.torchanger.bridge.BridgeCatalog;
import org.agty.torchanger.torchanger.config.AppDefaults;

public final class VanillaBridgeProfileDefinition implements TorProfileDefinition {
    @Override
    public TorInstanceSpec create() {
        return new TorInstanceSpec(
                "vanilla-bridges",
                "Vanilla Bridges",
                AppDefaults.intValue("vanillaSocksPort", 9061),
                AppDefaults.intValue("vanillaHttpPort", 19061),
                "bridge relay",
                TorLaunchMode.VANILLA_BRIDGE,
                BridgeCatalog.vanillaTested()
        );
    }
}
