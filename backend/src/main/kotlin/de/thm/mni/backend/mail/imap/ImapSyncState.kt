package de.thm.mni.backend.mail.imap

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "imap_sync_states")
class ImapSyncState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "folder_name", nullable = false, unique = true)
    var folderName: String = ""

    @Column(name = "initial_import_completed", nullable = false)
    var initialImportCompleted: Boolean = false

    @Column(name = "last_sync_at")
    var lastSyncAt: LocalDateTime? = null

    constructor()

    constructor(folderName: String) {
        this.folderName = folderName
    }
}
