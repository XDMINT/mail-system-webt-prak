package de.thm.mni.backend.smtp

import de.thm.mni.backend.mail.Mail

interface OutboundMailGateway {
    fun send(mail: Mail): Boolean
}
