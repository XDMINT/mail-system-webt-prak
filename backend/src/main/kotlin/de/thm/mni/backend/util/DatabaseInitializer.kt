package de.thm.mni.backend.util

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecord
import de.thm.mni.backend.mail_record.MailRecordRepository
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserRepository
import de.thm.mni.backend.util.dto.SeedData
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.core.type.TypeReference
import kotlin.collections.forEach


@Component
class DatabaseInitializer(
    private val userRepository: UserRepository,
    private val mailRepository: MailRepository,
    private val mailRecordRepository: MailRecordRepository,
): CommandLineRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        try {
            val resource = ClassPathResource("data.json")

            val objectMapper = ObjectMapper()
            val jsonData: SeedData = objectMapper.readValue(
                resource.inputStream,
                object : TypeReference<SeedData>() {}
            )
            val usersDto = jsonData.users
            val mailsDto = jsonData.mails

            usersDto.forEach { dto ->
                if (!userRepository.existsUserByEmail(dto.email)) {
                    userRepository.save(
                        User(
                            firstName = dto.firstName,
                            lastName = dto.lastName,
                            email = dto.email,
                            identityProviderSubject = dto.identityProviderSubject,
                            externalContact = dto.externalContact,
                        )
                    )
                }
            }

            mailsDto.forEach { dto ->
                val sender = userRepository.findByEmail(dto.senderEmail)
                if (sender == null) {
                    logger.warn("Skipping seed mail because sender {} does not exist", dto.senderEmail)
                    return@forEach
                }

                if (mailRepository.existsBySenderAndSubjectAndContent(sender, dto.subject, dto.content)) {
                    return@forEach
                }

                val mail = Mail(
                    sender = sender,
                    subject = dto.subject,
                    content = dto.content,
                    attachments = mutableListOf()
                )
                if(dto.status == MailStatus.SENT) {
                    mail.status = MailStatus.SENT
                }
                mail.source = dto.source
                mail.externalMessageId = dto.externalMessageId
                mail.externalSenderEmail = dto.externalSenderEmail
                val createdMail = mailRepository.save(mail)
                this.createMailRecords(createdMail, dto.toEmails, dto.ccEmails, dto.bccEmails)
            }

        } catch (e: Exception) {
            logger.warn("Failed to initialize seed data", e)
        }

    }

    private fun createMailRecords(
        mail: Mail,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>)
    {
        to.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.TO
        ))}

        cc.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.CC
        ))}

        bcc.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.BCC
        ))}

    }
}
