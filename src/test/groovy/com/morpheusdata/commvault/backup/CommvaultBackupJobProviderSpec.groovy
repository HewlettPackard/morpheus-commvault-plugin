package com.morpheusdata.commvault.backup

import com.morpheusdata.commvault.CommvaultPlugin
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.model.BackupJob
import com.morpheusdata.model.BackupProvider
import com.morpheusdata.response.ServiceResponse
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class CommvaultBackupJobProviderSpec extends Specification {
	@Shared MorpheusContext context
	@Shared CommvaultPlugin plugin
	@Subject @Shared CommvaultBackupJobProvider provider

	void setup() {
		// Create a Mocks of the Morpheus contexts you will use
		context = Mock(MorpheusContext)
		plugin = Mock(CommvaultPlugin)

		// Create the actual provider to unit test
		provider = new CommvaultBackupJobProvider(plugin, context)
	}

	//@Ignore("TODO: createBackupJob")
	void "createBackupJob should return a successful response"() {
		given: "Provider and input parameters"
		BackupJob backupJob = getMockBackupJob()
		backupJob.backupProvider >> getMockBackupProvider()


		Map opts = [:]

		when: "createBackupJob is called"
		def response = provider.createBackupJob(backupJob, opts)
		then:
		//1 * plugin.createBackupJob(backupJob, opts)
		backupJob.backupProvider != null
		backupJob.backupProvider.code.equals("commvault")
		//response == ServiceResponse.success(backupJob)
	}




	@Ignore("TODO: implement")
	void "createBackupExecution should return a successful response"() {
		given:
		def backup = Mock(BackupJob)
		def opts = [:]

		when: "createBackupExecution is called"
		ServiceResponse response = provider.createBackupJob(backup, opts)
		then:
		response.success == true
	}



	private BackupProvider getMockBackupProvider() {
		BackupProvider backupProvider = Mock(BackupProvider)
		backupProvider.id >> 1L
		backupProvider.name >> "Test CommVault Backup Provider"
		backupProvider.code >> "commvault"
		return backupProvider
	}

	private BackupJob getMockBackupJob() {
		BackupJob backupJob = Mock(BackupJob)
		backupJob.id >> 1L
		backupJob.name >> "Test Backup Job"
		backupJob.code >> "test-backup-job"
		return backupJob
	}

	private MorpheusContext setMockMorpheusContext( BackupProvider backupProvider, BackupJob backupJob) {
		MorpheusContext morpheusContext = Mock(MorpheusContext)
		morpheusContext.getServices() >> Mock(MorpheusContext.Services) {
			1 * backupProvider.get(backupProvider.id) >> backupProvider
			1 * backupJob.get(backupJob.id) >> backupJob
		}
		return morpheusContext
	}
}
