package org.agty.torchanger.torchanger.tor.profile;

import org.agty.torchanger.torchanger.config.AppDefaults;

public final class SnowflakeProfileDefinition implements TorProfileDefinition {
    @Override
    public TorInstanceSpec create() {
        return new TorInstanceSpec(
                "snowflake",
                "Snowflake",
                AppDefaults.intValue("snowflakeSocksPort", 9063),
                AppDefaults.intValue("snowflakeHttpPort", 19063),
                "snowflake-client",
                TorLaunchMode.SNOWFLAKE,
                null
        );
    }
}
