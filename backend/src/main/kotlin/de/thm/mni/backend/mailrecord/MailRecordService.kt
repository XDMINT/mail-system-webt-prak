package de.thm.mni.backend.mailrecord

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mailrecord.dto.CreateMailRecord
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class MailRecordService(private val repository: MailRecordRepository) {

    @Transactional
    fun createMailRecord(mailRecord: CreateMailRecord): MailRecord {
        val mailRecordEntity = MailRecord(
            mail = mailRecord.mail,
            user = mailRecord.receiver,
            type = mailRecord.mailType
        )
        return repository.save(mailRecordEntity)
    }

    @Transactional
    fun deleteMailRecord(id: MailRecordId) {
        repository.deleteById(id)
    }

    fun getMailRecordByMailId(mailId: UUID): List<MailRecord> {
        return repository.findMailRecordByMailId(mailId)
    }

    fun getAllIncomingMailsForUser(userId: UUID): List<Mail> {
        return repository.findAllByUserId(userId)
            .filter { it.type in RECIPIENT_TYPES }
            .map { it.mail!! }
            .filter { mail -> mail.status == MailStatus.SENT }
    }

    fun getIncomingMailsForUser(userId: UUID, pageable: Pageable): Page<Mail> {
        return repository.findIncomingMailsForUser(
            userId = userId,
            recipientTypes = RECIPIENT_TYPES,
            status = MailStatus.SENT,
            externalSource = MailSource.EXTERN,
            pageable = pageable
        )
    }

    private companion object {
        val RECIPIENT_TYPES = listOf(MailType.TO, MailType.CC, MailType.BCC)
    }

}
