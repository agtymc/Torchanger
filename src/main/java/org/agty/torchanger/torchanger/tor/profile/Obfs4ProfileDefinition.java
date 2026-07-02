package org.agty.torchanger.torchanger.tor.profile;

import org.agty.torchanger.torchanger.bridge.BridgeCatalog;
import org.agty.torchanger.torchanger.config.AppDefaults;

public final class Obfs4ProfileDefinition implements TorProfileDefinition {
    @Override
    public TorInstanceSpec create() {
        return new TorInstanceSpec(
                "obfs4-bridges",
                "obfs4 Bridges",
                AppDefaults.intValue("obfs4SocksPort", 9062),
                AppDefaults.intValue("obfs4HttpPort", 19062),
                "obfs4proxy",
                TorLaunchMode.OBFS4,
                BridgeCatalog.obfs4Tested()
        );
    }
}
