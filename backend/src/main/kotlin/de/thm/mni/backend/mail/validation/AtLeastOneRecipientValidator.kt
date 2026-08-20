package de.thm.mni.backend.mail.validation

import de.thm.mni.backend.mail.dto.RecipientMailRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class AtLeastOneRecipientValidator : ConstraintValidator<AtLeastOneRecipient, RecipientMailRequest> {
    override fun isValid(value: RecipientMailRequest?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return value.toIds.isNotEmpty() || value.ccIds.isNotEmpty() || value.bccIds.isNotEmpty()
    }
}
