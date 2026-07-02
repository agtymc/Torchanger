package org.agty.torchanger.torchanger.tor.profile;

import java.util.List;

public final class BuiltinProfiles {
    private static final List<TorProfileDefinition> VISIBLE_DEFINITIONS = List.of(
            new DirectProfileDefinition(),
            new VanillaBridgeProfileDefinition(),
            new Obfs4ProfileDefinition(),
            new SnowflakeProfileDefinition(),
            new WebTunnelProfileDefinition()
    );

    private BuiltinProfiles() {
    }

    public static List<TorInstanceSpec> defaults() {
        return VISIBLE_DEFINITIONS.stream()
                .map(TorProfileDefinition::create)
                .toList();
    }
}
