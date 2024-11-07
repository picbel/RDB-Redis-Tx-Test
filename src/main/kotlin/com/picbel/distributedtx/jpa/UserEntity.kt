package com.picbel.distributedtx.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,
    @Column(name = "name", columnDefinition = "VARCHAR2(15)")
    val name: String
) {
}
