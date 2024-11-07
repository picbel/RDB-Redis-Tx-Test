package com.picbel.distributedtx.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional


interface UserRepo {
    fun save(
        user: User,
        isThrow: Boolean = false
    ): User

    fun findById(id: Long): User?
}

@Repository
private class UserRepoImpl(
    private val userJpaDao: UserJpaDao
) : UserRepo {
    @Transactional
    override fun save(user: User, isThrow: Boolean): User {
        val userEntity = UserEntity(user.id, user.name)
        return userJpaDao.save(userEntity)
            .also { if (isThrow) throw RuntimeException("트랜잭션 테스트를 위한 의도적인 예외 발생") }
            .toUser()
    }

    @Transactional
    override fun findById(id: Long): User? {
        return userJpaDao.findById(id).map { it.toUser() }.orElse(null)
    }

    private fun UserEntity.toUser() = User(id, name)
}

@Repository
interface UserJpaDao : JpaRepository<UserEntity, Long>
