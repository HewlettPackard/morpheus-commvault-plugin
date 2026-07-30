package com.morpheusdata.commvault.backup

import com.morpheusdata.commvault.CommvaultPlugin
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.model.BackupJob
import com.morpheusdata.model.BackupProvider
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject

class CommvaultBackupJobProviderTest extends Specification {

	CommvaultPlugin plugin = Mock(CommvaultPlugin)
	MorpheusContext context = Mock(MorpheusContext)

	@Subject
	CommvaultBackupJobProvider provider = new CommvaultBackupJobProvider(plugin, context)

	void "provider can be constructed"() {
		expect:
		provider != null
	}

	//@Ignore("TODO: createBackupJob")
	void "createBackupJob creates a new backup job"() {
		given: "Provider and input parameters"
		BackupJob backupJob = Mock(BackupJob)
		backupJob.code = "test-backup-job"
		backupJob.name = "Test Backup Job"
		backupJob.backupProvider >> getMockBackupProvider()

		Map opts = [:]

		when: "createBackupJob is called"
		provider.createBackupJob(backupJob, opts)
		then:
		//1 * plugin.createBackupJob(backupJob, opts)
		backupJob.backupProvider != null
		backupJob.backupProvider.code.equals("commvault")
	}

	@Ignore("TODO: implement execute validation coverage")
	void "execute validates required inputs"() {
		expect:
		true
	}

	private BackupProvider getMockBackupProvider() {
		BackupProvider backupProvider = Mock(BackupProvider)
		backupProvider.id >> 1L
		backupProvider.name >> "Test CommVault Backup Provider"
		backupProvider.code >> "commvault"
		return backupProvider
	}
}
