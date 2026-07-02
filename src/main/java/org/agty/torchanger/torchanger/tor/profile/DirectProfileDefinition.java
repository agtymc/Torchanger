package org.agty.torchanger.torchanger.tor.profile;

import org.agty.torchanger.torchanger.config.AppDefaults;

public final class DirectProfileDefinition implements TorProfileDefinition {
    @Override
    public TorInstanceSpec create() {
        return new TorInstanceSpec(
                "direct",
                "Direct",
                AppDefaults.intValue("directSocksPort", 9060),
                AppDefaults.intValue("directHttpPort", 19060),
                "vanilla",
                TorLaunchMode.DIRECT,
                null
        );
    }
}
