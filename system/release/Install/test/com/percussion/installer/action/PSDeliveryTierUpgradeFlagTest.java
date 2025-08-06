package com.percussion.installer.action;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

public class PSDeliveryTierUpgradeFlagTest {

    /**
     * Verify the property parsing / sorting logic
     */
    @Test
    @Disabled("Failing on Windows")
    public void testGetValidDTSInstallations(){
        String dirs = this.getClass().getResource("New Folder").getPath().replace("%20"," ") +
                ";" + this.getClass().getResource("Percussion").getPath();

        SortedSet<String> ret = PSDeliveryTierUpgradeFlag.getValidDTSInstallations(dirs);

        assertEquals(this.getClass().getResource("New Folder").getPath().replace("%20"," "), ret.first());
        assertEquals(this.getClass().getResource("Percussion").getPath().replace("%20"," "), ret.last());

    }
}