package dev.hiki.lostcities;

import net.neoforged.fml.common.Mod;

@Mod(HikiLostCitiesAddon.MOD_ID)
public final class HikiLostCitiesAddon {
    public static final String MOD_ID = "hiki_lostcities";

    public HikiLostCitiesAddon() {
        System.getLogger(MOD_ID).log(System.Logger.Level.INFO,
                "Hiki Lost Cities Addon 0.1.0: correções para Lost Cities oficial 8.4.5.");
    }
}
