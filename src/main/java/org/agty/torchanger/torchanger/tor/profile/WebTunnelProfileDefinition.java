package org.agty.torchanger.torchanger.tor.profile;

import org.agty.torchanger.torchanger.bridge.BridgeCatalog;
import org.agty.torchanger.torchanger.config.AppDefaults;

public final class WebTunnelProfileDefinition implements TorProfileDefinition {
    @Override
    public TorInstanceSpec create() {
        return new TorInstanceSpec(
                "webtunnel",
                "WebTunnel",
                AppDefaults.intValue("webtunnelSocksPort", 9065),
                AppDefaults.intValue("webtunnelHttpPort", 19065),
                "lyrebird",
                TorLaunchMode.WEBTUNNEL,
                BridgeCatalog.webTunnelTested()
        );
    }
}
