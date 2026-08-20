package de.thm.mni.backend.mailrecord

import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MailRecordRepository: CrudRepository<MailRecord, MailRecordId> {
    fun findMailRecordByMailId(mailId: UUID): MutableList<MailRecord>
    fun findAllByUserId(userId: UUID): MutableList<MailRecord>
    @Query(
        value = """
            select distinct m
            from Mail m
            left join MailRecord mr on mr.mail = m
            where m.status = :status
              and (m.source = :externalSource
                or (mr.user.id = :userId and mr.type in :recipientTypes))
            order by coalesce(m.sentAt, m.createdAt) desc
        """,
        countQuery = """
            select count(distinct m)
            from Mail m
            left join MailRecord mr on mr.mail = m
            where m.status = :status
              and (m.source = :externalSource
                or (mr.user.id = :userId and mr.type in :recipientTypes))
        """
    )
    fun findIncomingMailsForUser(
        @Param("userId") userId: UUID,
        @Param("recipientTypes") recipientTypes: Collection<MailType>,
        @Param("status") status: MailStatus,
        @Param("externalSource") externalSource: MailSource,
        pageable: Pageable,
    ): Page<Mail>
}
