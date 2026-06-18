package de.thm.mni.backend.attachment

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AttachmentRepository : CrudRepository<Attachment, UUID>
