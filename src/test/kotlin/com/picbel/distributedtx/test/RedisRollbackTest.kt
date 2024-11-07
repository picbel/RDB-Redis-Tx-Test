package com.picbel.distributedtx.test

import com.picbel.distributedtx.DistributedTxApplication
import com.picbel.distributedtx.jpa.User
import com.picbel.distributedtx.jpa.UserRepo
import com.picbel.distributedtx.jpa.UserEntity
import com.picbel.distributedtx.redis.Session
import com.picbel.distributedtx.redis.SessionDao
import com.picbel.distributedtx.redis.SessionRTxDao
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate


@SpringBootTest(
    classes = [
        DistributedTxApplication::class
    ],
)
@EnableAutoConfiguration
class RedisRollbackTest {

    @Autowired
    private lateinit var sessionDao: SessionDao

    @Autowired
    private lateinit var sessionRtxDao: SessionRTxDao

    @Autowired
    private lateinit var userRepo: UserRepo

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setup() {
        transactionTemplate = TransactionTemplate(transactionManager)
    }

    @Test
    fun `Redis save test`() {
        // given
        val userId = 1L
        val (user, session) = createUserAndSession(userId)
        sessionDao.saveSession(session)
        userRepo.save(user)

        // when
        val savedSession = sessionDao.getSession(session.sessionId)
        val savedUser = userRepo.findById(userId)

        // then
        savedUser shouldBe user
        savedSession shouldBe session
    }

    @Test
    fun `rdb error rollback test`() {
        // given
        val userId = 2L
        val (user, session) = createUserAndSession(userId)
        // when
        try {
            transactionTemplate.execute {
                sessionDao.saveSession(session)
                userRepo.save(user, isThrow = true)
            }
        } catch (_: Exception) {
            println("예외 발생")
        }
        // then userRepo.save()에서 예외가 발생하여 롤백되었으므로 세션과 유저 정보가 저장되지 않아야 한다.
        val savedSession = sessionDao.getSession(session.sessionId)
        val savedUser = userRepo.findById(userId)
        assertSoftly {
            savedSession shouldBe null
            savedUser shouldBe null
        }
    }

    @Test
    fun `rdb error rollback test redisson tx test-commit`() {
        // given
        val userId = 3L
        val (user, session) = createUserAndSession(userId)
        // when
        try {
            transactionTemplate.execute {
                sessionRtxDao.saveSession(session, isThrow = false, isCommit = true)
                userRepo.save(user, isThrow = true)
            }
        } catch (_: Exception) {
            println("예외 발생")
        }
        // then userRepo.save()에서 예외가 발생하여 롤백되었으므로 세션과 유저 정보가 저장되지 않아야 한다.
        val savedSession = sessionDao.getSession(session.sessionId)
        val savedUser = userRepo.findById(userId)
        assertSoftly {
            savedSession shouldBe null
            savedUser shouldBe null
        }
    }

    // 애는 커밋이 안되었으니 저장이 안된게 맞겠죠...?
    @Test
    fun `rdb error rollback test redisson tx test-not commit`() {
        // given
        val userId = 4L
        val (user, session) = createUserAndSession(userId)
        // when
        try {
            transactionTemplate.execute {
                val saveSession = sessionRtxDao.saveSession(session, isThrow = false, isCommit = false)
                userRepo.save(user, isThrow = true)
                saveSession.second.commit()
            }
        } catch (_: Exception) {
            println("예외 발생")
        }
        // then userRepo.save()에서 예외가 발생하여 롤백되었으므로 세션과 유저 정보가 저장되지 않아야 한다.
        val savedSession = sessionDao.getSession(session.sessionId)
        val savedUser = userRepo.findById(userId)
        assertSoftly {
            savedSession shouldBe null
            savedUser shouldBe null
        }
    }

    // 애는 커밋이 안되었으니 저장이 안된게 맞겠죠...?
    @Test
    fun `redis error rollback test redisson tx test-not commit`() {
        // given
        val userId = 4L
        val (user, session) = createUserAndSession(userId)
        // when
        try {
            transactionTemplate.execute {
                userRepo.save(user, isThrow = false)
                sessionRtxDao.saveSession(session, isThrow = true, isCommit = true)
            }
        } catch (_: Exception) {
            println("예외 발생")
        }
        // then  sessionRtxDao.saveSession에서 예외가 발생하여 롤백되었으므로 세션과 유저 정보가 저장되지 않아야 한다.
        val savedSession = sessionDao.getSession(session.sessionId)
        val savedUser = userRepo.findById(userId)
        assertSoftly {
            savedSession shouldBe null
            savedUser shouldBe null
        }
    }

    private fun createUserAndSession(userId: Long): Pair<User, Session> {
        val user = User(userId, "user-$userId")
        val sessionId = "session-$userId"
        val session = Session(sessionId, userId, System.currentTimeMillis())
        return Pair(user, session)
    }
}
