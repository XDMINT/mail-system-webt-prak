package de.thm.mni.backend.mail.imap

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ImapSyncStateRepository : CrudRepository<ImapSyncState, UUID> {
    fun findByFolderName(folderName: String): ImapSyncState?
}
