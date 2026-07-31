package com.morpheusdata.commvault.datasets

import com.morpheusdata.commvault.utils.CommvaultReferenceUtility
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.data.*
import com.morpheusdata.core.providers.AbstractDatasetProvider
import com.morpheusdata.model.ReferenceData
import com.morpheusdata.model.ResourcePermission
import groovy.util.logging.Slf4j
import io.reactivex.rxjava3.core.Observable

/**
 * @author rahul.ray
 */

@Slf4j
class CommvaultClientsDatasetProvider extends AbstractDatasetProvider<ReferenceData, Long> {

    public static final providerName = 'Commvault Clients Dataset Provider'
    public static final providerNamespace = 'commvault'
    public static final providerKey = 'commvaultClients'
    public static final providerDescription = 'A set of data from commvault client'

    CommvaultClientsDatasetProvider(Plugin plugin, MorpheusContext morpheus) {
        this.plugin = plugin
        this.morpheusContext = morpheus
    }

    @Override
    DatasetInfo getInfo() {
        new DatasetInfo(
                name: providerName,
                namespace: providerNamespace,
                key: providerKey,
                description: providerDescription
        )
    }

    /**
     * the class type of the data for this provider
     * @return the class this provider operates on
     */
    @Override
    Class<ReferenceData> getItemType() {
        ReferenceData.class
    }

    /**
     * list the values in the dataset
     * @param query the user and map of query params or options to apply to the list
     * @return a list of objects
     */
    @Override
    Observable<ReferenceData> list(DatasetQuery query) {
        log.debug("clients list: ${query.parameters}")

        def account = query.user.account
        def cloud = CommvaultDatasetUtility.resolveCloud(morpheus, query, account)
        def backupProvider = CommvaultDatasetUtility.resolveBackupProvider(morpheus, cloud, account)
        if (backupProvider) {
            def accessibleResourceIds = morpheus.services.resourcePermission.listAccessibleResources(account.id, ResourcePermission.ResourceType.BackupServer, null, null)
            def dataQuery = new DataQuery().withFilters([
                    new DataFilter("account.id", backupProvider.account.id),
                    new DataFilter("category", "${backupProvider.type.code}.backup.backupServer.${backupProvider.id}"),
                    new DataFilter("enabled", true)
            ])
            // filter on account.id rather than the account object: mixing an object valued "account" filter with
            // the nested "account.masterAccount" filter inside the same or-clause produces a query that matches
            // nothing, which silently emptied this option source.
            def dataOrFilter = new DataOrFilter(
                    new DataFilter("account.id", account.id),
                    new DataAndFilter(
                            new DataFilter("account.masterAccount", true),
                            new DataFilter("visibility", "public")
                    )
            )
            if (accessibleResourceIds) {
                dataOrFilter.withFilter(new DataFilter("id", "in", accessibleResourceIds))
            }
            dataQuery.withFilter(dataOrFilter)
            // NOTE: services.referenceData.list() is the synchronous facade and returns a blocking List,
            // not an Observable - use the async accessor here to match this method's declared return type.
            return morpheus.async.referenceData.list(dataQuery)
        }
        return Observable.empty()
    }

    /**
     * list the values in teh dataset in a common format of a name value pair. (example: [[name: "blue", value: 1]])
     * @param query a DatasetQuery containing the user and map of query params or options to apply to the list
     * @return a list of maps that have name value pairs of the items
     */
    @Override
    Observable<Map> listOptions(DatasetQuery query) {
        log.debug("clients: ${query.parameters}")
        List clients = []
        def account = query.user.account
        def cloud = CommvaultDatasetUtility.resolveCloud(morpheus, query, account)
        def backupProvider = CommvaultDatasetUtility.resolveBackupProvider(morpheus, cloud, account)
        if (backupProvider) {
            def clientResults = list(query).toList().blockingGet()
            if (clientResults.size() > 0) {
                clientResults.each { client ->
                    def clientInstanceType = client.getConfigProperty('vsInstanceType')
                    def clientInstanceTypeCode = clientInstanceType ? CommvaultReferenceUtility.getvsInstanceType(clientInstanceType?.toString()) : null
                    if (clientMatchesCloud(cloud, clientInstanceTypeCode)) {
                        clients << [name: client.name, id: client.id, value: client.id]
                    }
                }
            } else {
                clients << [name: "No clients setup in Commvault", id: '']
            }
        } else {
            clients << [name: "No Commvault backup provider found.", id: '']
        }
        return Observable.fromIterable(clients)
    }

    /**
     * Only offer clients whose commvault virtualization type matches the cloud being backed up. The cloud model
     * handed to a dataset provider does not carry its cloud type's provision types, so fall back to matching on
     * the cloud type code and keep the client when neither side gives us anything to compare.
     */
    private static boolean clientMatchesCloud(cloud, String clientInstanceTypeCode) {
        if (!cloud || !clientInstanceTypeCode) {
            return true
        }
        def provisionTypeCodes = cloud.cloudType?.provisionTypes?.collect { it.code }
        if (provisionTypeCodes) {
            return provisionTypeCodes.contains(clientInstanceTypeCode)
        }
        String cloudTypeCode = cloud.cloudType?.code
        return cloudTypeCode ? cloudTypeCode.toLowerCase().contains(clientInstanceTypeCode.toLowerCase()) : true
    }

    /**
     * returns the matching item from the list with the value as a string or object - since option values
     *   are often stored or passed as strings or unknown types. lets the provider do its own conversions to call
     *   item with the proper type. did object for flexibility but probably is usually a string
     * @param value the value to match the item in the list
     * @return the item
     */
    @Override
    ReferenceData fetchItem(Object value) {
        def rtn = null
        if (value instanceof Long) {
            rtn = item((Long) value)
        } else if (value instanceof CharSequence) {
            def longValue = value.isNumber() ? value.toLong() : null
            if (longValue) {
                rtn = item(longValue)
            }
        }
        return rtn
    }

    /**
     * returns the item from the list with the matching value
     * @param value the value to match the item in the list
     * @return the
     */
    @Override
    ReferenceData item(Long value) {
        return morpheus.services.referenceData.get(value)
    }

    /**
     * gets the name for an item
     * @param item an item
     * @return the corresponding name for the name/value pair list
     */
    @Override
    String itemName(ReferenceData item) {
        return item.name
    }

    /**
     * gets the value for an item
     * @param item an item
     * @return the corresponding value for the name/value pair list
     */
    @Override
    Long itemValue(ReferenceData item) {
        return item.id
    }

    /**
     * Returns true if the Provider is a plugin. Always true for plugin but null or false for Morpheus internal providers.
     * @return provider is plugin
     */
}
