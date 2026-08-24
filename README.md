# Morpheus Commvault Plugin

The Morpheus Commvault Plugin integrates [Morpheus](https://morpheusdata.com) with [Commvault](https://www.commvault.com/). It discovers Commvault clients, backup sets, storage policies, and subclients; manages backup jobs for supported workloads; and provides backup and restore workflows from the Morpheus platform.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Repository structure](#repository-structure)
- [Building the plugin](#building-the-plugin)
- [License](#license)
- [Installing](#installing)
- [Detailed Usage Steps](#detailed-usage-steps)
- [API Endpoints](#api-endpoints)

---

## Features

### Backup Integration

The plugin registers a Commvault backup provider that supports:

- Connectivity and credential validation against the Commvault Web Service
- API token management through login and logout sessions
- Provider health monitoring during refresh
- Cleanup of Commvault clients and associated reference data when an integration is removed

### Commvault Inventory Sync

The following Commvault resources are discovered and kept in sync:

- Clients
- Backup sets
- Storage policies
- Subclients represented as Morpheus backup jobs

Additions, updates, and removals in Commvault are reflected in Morpheus on the next integration refresh.

### Supported Workloads

The plugin provides these scoped backup types:

| Backup Type | Code | Scope | Restore Mode |
|-------------|------|-------|--------------|
| Commvault VMware Backup | `commvaultVMWareBackup` | `vmware` | VM restore |
| Commvault OpenStack Backup | `commvaultOpenstackBackup` | `openstack` | VM restore |
| Commvault File/Directory Backup | `commvaultFileBackup` | `file` | File backup |

The virtual machine backup types support restoring an existing workload or restoring to a new virtual machine.

### Backup Job Management

Commvault subclients are managed as backup jobs through the Morpheus backup framework. Supported operations include:

- Create a Commvault subclient for a Morpheus backup job
- Clone an existing subclient into a new backup job
- Add workloads to an existing backup job
- Execute backup jobs on demand
- Delete subclients when backup jobs are removed

### Backup and Restore Operations

Supported workload operations include:

- Configure Commvault protection for individual workloads
- Run on-demand backups
- Cancel in-progress backups by stopping the associated Commvault job
- Poll Commvault jobs and update Morpheus backup results
- Delete backup jobs and backup results
- Restore a backup over an existing virtual machine
- Restore a backup to a new virtual machine
- Poll restore jobs and update Morpheus restore status

### Option Sources and Datasets

The plugin registers dataset providers in the `commvault` namespace:

| Dataset Key | Purpose |
|-------------|---------|
| `commvaultClients` | Clients available on the selected integration |
| `commvaultBackupSets` | Backup sets available on the selected client |
| `commvaultStoragePolicies` | Storage policies available on the selected integration |

---

## Requirements

| Requirement | Version or Details |
|-------------|--------------------|
| Morpheus | 9.1.0 or later |
| Java | 25 |
| Gradle | Use the included Gradle wrapper (`./gradlew`) |
| Commvault | CommServe with the CVWebService REST API enabled |

Additional prerequisites:

- The Commvault Web Service base path, `/SearchSvc/CVWebService.svc`, reachable from the Morpheus appliance
- Network access from Morpheus to the configured Commvault port, commonly `81` or `443`
- A Commvault account with permission to view clients, backup sets, storage policies, subclients, and jobs and to run backup and restore operations
- VMware, OpenStack, or file workloads represented in both Morpheus and Commvault, as applicable

---

## Repository structure

```text
src/main/groovy/com/morpheusdata/commvault/
├── CommvaultPlugin.groovy                    - Plugin entry point, provider registration, and API authentication
├── backup/                                   - Backup provider, job provider, and shared execution and restore logic
│   ├── vmware/                               - VMware backup, execution, and restore providers
│   ├── openstack/                            - OpenStack backup, execution, and restore providers
│   └── file/                                 - File and directory backup providers
├── datasets/                                 - Client, backup set, and storage policy dataset providers
├── sync/                                     - Client, backup set, storage policy, and subclient sync tasks
└── utils/
    ├── CommvaultApiUtility.groovy            - Commvault Web Service REST API client
    └── CommvaultReferenceUtility.groovy      - Reference data and code mapping helpers
src/assets/                                   - Plugin icon assets
src/main/resources/i18n/                      - Localization bundles
build.gradle, gradle.properties               - Build configuration, versions, and plugin metadata
```

---

## Building the plugin

Run the following command to compile and package the plugin jar:

```bash
./gradlew clean shadowJar
```

The packaged jar will be written to `build/libs/`.

To execute tests, use the following command:

```bash
./gradlew test
```

---

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details.

---

## Installing

1. Build the plugin as described in [Building the plugin](#building-the-plugin), or download a released jar from the repository's [Releases](https://github.com/HewlettPackard/morpheus-commvault-plugin/releases) page.
2. In Morpheus, navigate to **Administration > Integrations > Plugins**.
3. Click **Add** and upload the `morpheus-commvault-plugin-<version>-all.jar` from `build/libs/`.
4. Wait for the plugin to load. **Commvault** will then be available as a backup integration.

---

## Detailed Usage Steps

### Adding a Commvault Backup Integration

1. In Morpheus, navigate to **Backups > Integrations**.
2. Click **Add Backup Integration** and select **Commvault**.
3. Enter the Commvault Web Service **Host** and **Port**.
4. Select a stored username/password credential or enter a Commvault username and password.
5. Save the integration. Morpheus validates the connection and synchronizes clients, backup sets, storage policies, and subclients.

### Configuring a Workload Backup

1. Open a supported VMware, OpenStack, or file workload in Morpheus.
2. Add a backup and select the Commvault integration.
3. Select the **Backup Server** that owns the workload.
4. Select the **Backup Set** on that server.
5. Select the **Storage Policy** to apply to the subclient.
6. Save the backup configuration. Morpheus creates or updates the corresponding Commvault subclient.

### Running and Monitoring a Backup

Run the configured backup from the workload's **Backups** tab or execute its backup job. Morpheus starts the Commvault subclient backup and polls the associated job until it succeeds, fails, or is canceled.

### Restoring a Virtual Machine

Select a successful VMware or OpenStack backup result and choose **Restore**. Restore the protected workload in place or restore it to a new virtual machine. Morpheus submits the recovery to Commvault and monitors the restore job to completion.

---

## API Endpoints

The plugin communicates with the Commvault Web Service under the `/SearchSvc/CVWebService.svc` base path.

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/Login` | `POST` | Authenticate and retrieve an API token |
| `/Logout` | `POST` | End the API session |
| `/Client` | `GET` | List Commvault clients |
| `/Client/{clientId}` | `DELETE` | Remove a Commvault client |
| `/Backupset` | `GET` | List backup sets |
| `/StoragePolicy` | `GET` | List storage policies |
| `/StoragePolicy/{id}` | `GET` | Retrieve a storage policy |
| `/Library` | `GET` | List storage libraries |
| `/Library/{id}` | `GET` | Retrieve a storage library |
| `/Subclient` | `GET`, `POST` | List or create subclients |
| `/Subclient/{subclientId}` | `GET`, `POST`, `DELETE` | Retrieve, update, or delete a subclient |
| `/Subclient/{subclientId}/action/backup` | `POST` | Start a subclient backup |
| `/Job` | `GET` | List Commvault jobs |
| `/Job/{jobId}` | `GET` | Retrieve job status |
| `/Job/{jobId}/Action/pause` | `POST` | Pause a running job |
| `/Job/{jobId}/Action/kill` | `POST` | Cancel a running job |
| `/VM` | `GET` | Look up virtual machines known to Commvault |
| `/v2/vsa/vm/{vmGuid}/jobs` | `GET` | List backup jobs for a virtual machine |
| `/v2/vsa/vm/{vmGuid}/recover` | `POST` | Start a virtual machine restore |
| `/QCommand` | `POST` | Execute a Commvault QCommand operation |
