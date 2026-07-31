package com.morpheusdata.commvault.backup

import com.morpheusdata.commvault.CommvaultPlugin
import com.morpheusdata.core.MorpheusContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class CommvaultBackupProviderSpec extends Specification {

    // define a mock CommvaultPlugin and MorpheusContext
    @Shared MorpheusContext context
    @Shared CommvaultPlugin plugin
    @Subject @Shared CommvaultBackupProvider provider

    void setup() {
        // Create a Mocks of the Morpheus contexts you will use
        context = Mock(MorpheusContextImpl)
        plugin = Mock(CommvaultPlugin)

        // Create the actual provider to unit test
        provider = new CommvaultBackupProvider(plugin, context)
    }


    //TODO: This test is for the plugin tests for CommvaultClient
}
