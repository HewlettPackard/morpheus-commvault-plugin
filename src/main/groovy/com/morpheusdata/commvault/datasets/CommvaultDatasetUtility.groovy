package com.morpheusdata.commvault.datasets

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.data.DataFilter
import com.morpheusdata.core.data.DataQuery
import com.morpheusdata.core.data.DatasetQuery
import com.morpheusdata.model.BackupProvider
import com.morpheusdata.model.Cloud
import groovy.util.logging.Slf4j

/**
 * Shared context resolution for the commvault dataset providers. The backup wizard and the provisioning
 * wizard post different context, and a commvault integration is not necessarily attached to a cloud, so
 * every dataset provider resolves its cloud and backup provider the same way here.
 */
@Slf4j
class CommvaultDatasetUtility {

    static final String PROVIDER_CODE = 'commvault'

    /**
     * Resolve the cloud from either the container (workload) or the zone id supplied in the query params. The
     * backup wizard posts a bare {@code zoneId} while the instance context posts a {@code containerId}.
     * @return the resolved cloud or null when the form carries no cloud context
     */
    static Cloud resolveCloud(MorpheusContext morpheus, DatasetQuery query, account) {
        Cloud cloud = null
        Long containerId = query.get("containerId")?.toLong()
        Long cloudId = query.get("zoneId")?.toLong()
        if (containerId && !cloudId) {
            def workload = morpheus.services.workload.find(new DataQuery()
                    .withFilter("account", account).withFilter("containerId", containerId))
            cloud = workload?.server?.cloud
        }
        if (!cloud && cloudId) {
            cloud = morpheus.services.cloud.get(cloudId)
        }
        return cloud
    }

    /**
     * Resolve the commvault backup provider for the given cloud: prefer a commvault provider attached to the
     * cloud, then an enabled commvault provider owned by the account, then a master/public one. Backup providers
     * are account scoped and are frequently not linked to any cloud, so restricting the lookup to the cloud's
     * providers leaves every commvault option source empty.
     */
    static BackupProvider resolveBackupProvider(MorpheusContext morpheus, Cloud cloud, account) {
        BackupProvider backupProvider = null
        List<Long> cloudProviderIds = []
        if (cloud?.backupProvider?.id) {
            cloudProviderIds << cloud.backupProvider.id
        }
        if (cloud?.backupProviders) {
            cloudProviderIds += cloud.backupProviders.collect { it.id }
        }
        if (cloudProviderIds) {
            backupProvider = morpheus.services.backupProvider.listById(cloudProviderIds.unique())
                    .find { it.type?.code == PROVIDER_CODE }
        }
        if (!backupProvider && account) {
            backupProvider = morpheus.services.backupProvider.find(new DataQuery().withFilters([
                    new DataFilter('enabled', true),
                    new DataFilter('type.code', PROVIDER_CODE),
                    new DataFilter('account.id', account.id)
            ]))
        }
        if (!backupProvider) {
            backupProvider = morpheus.services.backupProvider.find(new DataQuery().withFilters([
                    new DataFilter('enabled', true),
                    new DataFilter('type.code', PROVIDER_CODE),
                    new DataFilter('account.masterAccount', true),
                    new DataFilter('visibility', 'public')
            ]))
        }
        return backupProvider
    }
}
