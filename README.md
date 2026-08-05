# Morpheus Commvault Plugin

This plugin provides backup integration between [Commvault](https://www.commvault.com/) and [Morpheus](https://morpheusdata.com). It enables Commvault client, backup set, and storage policy discovery, subclient-based backup job management, VM and file backup protection for VMware and OpenStack workloads, and restore workflows from within the Morpheus platform.

## Requirements

| Component | Minimum Version |
|-----------|----------------|
| Morpheus | 9.1.0 |
| Commvault | CommServe with the CVWebService REST API enabled |

The Commvault Web Service (`/SearchSvc/CVWebService.svc`) must be reachable from the Morpheus appliance.

## Installation

1. Download the latest `.jar` from the [Releases](https://github.com/HewlettPackard/morpheus-commvault-plugin/releases) page, or [build it yourself](#building).
2. In Morpheus, navigate to **Administration → Integrations → Plugins**.
3. Click **Browse** and upload the `.jar` file.
4. The **Commvault** backup integration will appear after the plugin loads.

## Configuration

When adding a Commvault backup integration in Morpheus (**Backups → Integrations → Add Backup Integration**), provide the following:

| Field | Description |
|-------|-------------|
| **Host** | Commvault Web Service API URL, e.g. `https://commserve.example.com` |
| **Port** | Commvault Web Service port, typically `81` or `443` |
| **Credentials** | Username and password used to authenticate against the Commvault Web Service |

Credentials can also be stored as a Morpheus [Credential](https://docs.morpheusdata.com/en/latest/administration/credentials/credentials.html) of type `username-password` and selected at integration setup time.

When configuring an individual backup, the following options are required:

| Field | Description |
|-------|-------------|
| **Backup Server** | Commvault client (virtualization or file system client) that owns the backup |
| **Backup Set** | Commvault backup set on the selected client |
| **Storage Policy** | Commvault storage policy applied to the backup subclient |

## Features

### Backup Integration
The plugin registers a Commvault `BackupProvider` that connects Morpheus backup workflows to Commvault. Supported integration behavior includes:

- Validate connectivity and credentials against the Commvault Web Service
- Authenticate and manage API tokens via login and logout sessions
- Track provider health during refresh
- Clean up Commvault clients and associated reference data when the integration is removed

### Commvault Sync
The following Commvault resources are discovered and kept in sync:

- **Clients** — Commvault clients registered with the CommServe
- **Backup Sets** — backup sets available on each client
- **Storage Policies** — storage policies available as backup targets
- **Subclients** — Commvault subclients represented as Morpheus backup jobs

Additions, updates, and removals in Commvault are automatically reflected in Morpheus on the next sync cycle.

### Workload Backup Types
Backup types are registered as scoped providers, selected based on the workload being protected. The plugin ships three backup types:

| Backup Type | Code | Scope | Restore Mode |
|-------------|------|-------------|--------------|
| Commvault VMware Backup | `commvaultVMWareBackup` | `vmware` | `VM_RESTORE` |
| Commvault OpenStack Backup | `commvaultOpenstackBackup` | `openstack` | `VM_RESTORE` |
| Commvault File/Directory Backup | `commvaultFileBackup` | `file` | n/a |

All three types support online restores to both the existing workload and a new workload.

### Backup Job Management
Commvault subclients are managed as backup jobs through the Morpheus backup framework. Supported operations include:

- Create a new Commvault subclient for a Morpheus backup job
- Clone an existing subclient into a new backup job
- Add workloads to an existing backup job
- Execute backup jobs on demand
- Delete subclients when backup jobs are removed

### Backup and Restore Operations
Workload protection and restore workflows are available directly from Morpheus. Supported operations include:

- Create and configure Commvault subclients for individual workloads
- Run on-demand backups for a protected workload
- Cancel in-flight backups by killing the associated Commvault job
- Poll Commvault jobs and update Morpheus backup results
- Delete Commvault backup jobs and backup results
- Restore a backup in place over the existing workload
- Restore a backup to a new virtual machine
- Poll Commvault restore jobs and update Morpheus restore status

### Option Sources and Datasets
The plugin registers dataset providers in the `commvault` namespace so that Commvault-specific selections are populated from live inventory:

| Dataset Key | Purpose |
|-------------|---------|
| `commvaultClients` | Commvault clients available on the selected integration |
| `commvaultBackupSets` | Backup sets available on the selected client |
| `commvaultStoragePolicies` | Storage policies available on the selected integration |

## Repository structure

- `src/main/groovy/com/morpheusdata/commvault`
  - `CommvaultPlugin.groovy` — plugin entry point that registers all providers and builds API auth config
  - `backup/` — backup provider, job provider, and shared execution/restore providers
    - `vmware/`, `openstack/`, `file/` — scoped backup type, execution, and restore providers
  - `datasets/` — dataset providers for clients, backup sets, and storage policies
  - `sync/` — sync tasks for clients, backup sets, storage policies, and subclients
  - `utils/`
    - `CommvaultApiUtility.groovy` — Commvault Web Service REST API client
    - `CommvaultReferenceUtility.groovy` — reference data and code mapping helpers
- `src/assets` — plugin icon assets
- `src/main/resources/i18n` — localization message bundles
- `build.gradle` and `gradle.properties` — build configuration and dependency versions

## Building

```bash
./gradlew shadowJar
```

The plugin JAR will be written to `build/libs/`.

To run the tests:

```bash
./gradlew test
```

## API Endpoints

The plugin communicates with the Commvault Web Service under the base path `/SearchSvc/CVWebService.svc`. Key endpoints used include:

| Endpoint | Methods | Purpose |
|----------|---------|---------|
| `/Login` | `POST` | Authenticate and retrieve an API token |
| `/Logout` | `POST` | Terminate the API session |
| `/Client` | `GET` | List Commvault clients |
| `/Client/{clientId}` | `DELETE` | Remove a Commvault client |
| `/Backupset` | `GET` | List backup sets |
| `/StoragePolicy`, `/StoragePolicy/{id}` | `GET` | List and retrieve storage policies |
| `/Library`, `/Library/{id}` | `GET` | List and retrieve storage libraries |
| `/Subclient` | `GET`, `POST` | List and create subclients |
| `/Subclient/{subclientId}` | `GET`, `POST`, `DELETE` | Retrieve, update, and delete a subclient |
| `/Subclient/{subclientId}/action/backup` | `POST` | Start a backup for a subclient |
| `/Job`, `/Job/{jobId}` | `GET` | List and retrieve Commvault job status |
| `/Job/{jobId}/Action/pause` | `POST` | Pause a running job |
| `/Job/{jobId}/Action/kill` | `POST` | Cancel a running job |
| `/VM` | `GET` | Look up virtual machines known to Commvault |
| `/v2/vsa/vm/{vmGuid}/jobs` | `GET` | List backup jobs for a virtual machine |
| `/v2/vsa/vm/{vmGuid}/recover` | `POST` | Start a virtual machine restore |
| `/QCommand` | `POST` | Execute QCommand operations |

## License

Copyright 2022 Morpheus Data, LLC. Licensed under the [Apache License, Version 2.0](LICENSE).
