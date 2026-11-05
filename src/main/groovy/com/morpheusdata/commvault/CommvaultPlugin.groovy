/*
* Copyright 2022 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.morpheusdata.commvault

import com.morpheusdata.commvault.backup.CommvaultBackupProvider
import com.morpheusdata.commvault.datasets.CommvaultBackupSetsDatasetProvider
import com.morpheusdata.commvault.datasets.CommvaultClientsDatasetProvider
import com.morpheusdata.commvault.datasets.CommvaultStoragePoliciesDatasetProvider
import com.morpheusdata.commvault.utils.CommvaultApiUtility
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.AccountCredential
import com.morpheusdata.model.BackupProvider

class CommvaultPlugin extends Plugin {

    static apiBasePath = '/SearchSvc/CVWebService.svc'

    private static final Map<Long, Map> authTokenCache = new java.util.concurrent.ConcurrentHashMap<>()
    private static final long TOKEN_TTL_MS = 10 * 60 * 1000 // conservative; well under Commvault's session timeout

    @Override
    String getCode() {
        return 'commvault'
    }

    @Override
    void initialize() {
        this.setName("Commvault")
        this.registerProvider(new CommvaultBackupProvider(this,this.morpheus))
        this.registerProvider(new CommvaultClientsDatasetProvider(this,this.morpheus))
        this.registerProvider(new CommvaultBackupSetsDatasetProvider(this,this.morpheus))
        this.registerProvider(new CommvaultStoragePoliciesDatasetProvider(this,this.morpheus))
    }

    /**
     * Called when a plugin is being removed from the plugin manager (aka Uninstalled)
     */
    @Override
    void onDestroy() {
        //nothing to do for now
    }

    def getAuthConfig(BackupProvider backupProvider) {
        if(!backupProvider) {
            return [apiUrl: null, username: null, password: null, basePath: apiBasePath]
        }
        //credentials
        backupProvider = loadCredentials(backupProvider)
        def rtn = [
                apiUrl: CommvaultApiUtility.getApiUrl(backupProvider),
                username:backupProvider.credentialData?.username ?: backupProvider.username,
                password:backupProvider.credentialData?.password ?: backupProvider.password,
                basePath:apiBasePath
        ]
        def cached = authTokenCache[backupProvider.id]
        if(cached && (System.currentTimeMillis() - cached.fetchedAt) < TOKEN_TTL_MS) {
            rtn.token = cached.token
        }
        return rtn
    }

    // caches the token fetched for a backupProvider so subsequent calls in the same flow can reuse it instead of logging in again
    def cacheAuthToken(BackupProvider backupProvider, String token) {
        if(backupProvider?.id && token) {
            authTokenCache[backupProvider.id] = [token: token, fetchedAt: System.currentTimeMillis()]
        }
    }

    // drops a cached token, e.g. after a 401, so the next call fetches a fresh one
    def evictAuthToken(BackupProvider backupProvider) {
        if(backupProvider?.id) {
            authTokenCache.remove(backupProvider.id)
        }
    }

    MorpheusContext getMorpheusContext() {
        this.morpheus
    }

    BackupProvider loadCredentials(BackupProvider backupProvider) {
        if(backupProvider && !backupProvider.credentialLoaded) {
            AccountCredential accountCredential
            accountCredential = this.morpheus.services.accountCredential.loadCredentials(backupProvider)
            backupProvider.credentialLoaded = true
            backupProvider.credentialData = accountCredential?.data
        }
        return backupProvider
    }
    
}
