package de.thm.mni.backend.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID


@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id : UUID? = null

    @Column(name = "first_name", nullable = false)
    var firstName: String = ""

    @Column(name = "last_name", nullable = false)
    var lastName: String = ""

    @Column(name = "email", nullable = false, unique = true)
    var email: String = ""

    @Column(name = "identity_provider_subject", unique = true)
    var identityProviderSubject: String? = null

    @Column(name = "external_contact", nullable = false)
    var externalContact: Boolean = false

    constructor()
    constructor(
        firstName: String,
        lastName: String,
        email: String,
        externalContact: Boolean = false,
        identityProviderSubject: String? = null,
    ) {
        this.firstName = firstName
        this.lastName = lastName
        this.email = email
        this.externalContact = externalContact
        this.identityProviderSubject = identityProviderSubject
    }


}
