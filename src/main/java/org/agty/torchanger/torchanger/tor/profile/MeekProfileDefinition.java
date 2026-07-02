package org.agty.torchanger.torchanger.tor.profile;

import org.agty.torchanger.torchanger.config.AppDefaults;

public final class MeekProfileDefinition implements TorProfileDefinition {
    @Override
    public TorInstanceSpec create() {
        return new TorInstanceSpec(
                "meek",
                "Meek",
                AppDefaults.intValue("meekSocksPort", 9064),
                AppDefaults.intValue("meekHttpPort", 19064),
                "lyrebird",
                TorLaunchMode.MEEK,
                null
        );
    }
}
