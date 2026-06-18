package de.thm.mni.backend.mail_record

import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MailRecordRepository: CrudRepository<MailRecord, UUID> {
    fun findMailRecordByMailId(mailId: UUID): MutableList<MailRecord>
    fun findAllByUserId(userId: UUID): MutableList<MailRecord>
    @Query(
        value = """
            select mr.mail
            from MailRecord mr
            where mr.user.id = :userId
              and mr.type <> :replyToType
              and mr.mail.status = :status
            order by coalesce(mr.mail.sentAt, mr.mail.createdAt) desc
        """,
        countQuery = """
            select count(mr.mail)
            from MailRecord mr
            where mr.user.id = :userId
              and mr.type <> :replyToType
              and mr.mail.status = :status
        """
    )
    fun findIncomingMailsForUser(
        @Param("userId") userId: UUID,
        @Param("replyToType") replyToType: MailType,
        @Param("status") status: MailStatus,
        pageable: Pageable,
    ): Page<Mail>

    fun deleteById(id: MailRecordId)
}
