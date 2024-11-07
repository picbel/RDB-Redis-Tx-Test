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
import org.junit.jupiter.api.assertAll
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
    /**
     * 실행시키실때 docker 이용해서 redis 서버를 띄워주세요
     * localhost 9900 포트로 띄워주세요
     */

    @Autowired
    private lateinit var sessionDao: SessionDao

    @Autowired
    private lateinit var sessionRtxDao: SessionRTxDao

    @Autowired
    private lateinit var userRepo: UserRepo

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var transactionTemplate: TransactionTemplate

    /**
     * user는 rdb에 저장하고 session은 redis에 저장합니다.
     */
    @BeforeEach
    fun setup() {
        transactionTemplate = TransactionTemplate(transactionManager)
        sessionDao.clear()
    }

    @Test
    fun `유저와 세션을 rdb와 redis에 저장합니다`() {
        // given
        val userId = 1L
        val (user, session) = createUserAndSession(userId)
        sessionDao.saveSession(session)
        userRepo.save(user)

        // when
        val savedUser = userRepo.findById(userId)
        val savedSession = sessionDao.getSession(session.sessionId)

        // then
        savedUser shouldBe user
        savedSession shouldBe session
    }

    @Test
    fun `스프링 트랜잭션을 이용하여 redis저장후 rdb함수에서 에러가 발생하여 전부 저장되지 않습니다`() {
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
        assertAll(
            { assert(savedSession == null) { "세션 정보가 저장되어있습니다." } },
            { assert(savedUser == null) { "유저 정보가 저장되어있습니다." } }
        )
    }

    @Test
    fun `R트랜잭션을 이용하여 redis저장후 rdb함수에서 에러가 발생하여 전부 저장되지 않습니다`() {
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
        assertAll(
            { assert(savedSession == null) { "세션 정보가 저장되어있습니다." } },
            { assert(savedUser == null) { "유저 정보가 저장되어있습니다." } }
        )
    }

    // 애는 커밋이 안되었으니 저장이 안된게 맞겠죠...?
    @Test
    fun `R트랜잭션을 이용하여 redis저장후 커밋하지않고 rdb함수에서 에러가 발생하여 전부 저장되지 않습니다`() {
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
        assertAll(
            { assert(savedSession == null) { "세션 정보가 저장되어있습니다." } },
            { assert(savedUser == null) { "유저 정보가 저장되어있습니다." } }
        )
    }

    @Test
    fun `rdb함수에서 저장후 redis에서 에러가 발생하여 전부 저장되지 않습니다`() {
        // given
        val userId = 5L
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
        assertAll(
            { assert(savedSession == null) { "세션 정보가 저장되어있습니다." } },
            { assert(savedUser == null) { "유저 정보가 저장되어있습니다." } }
        )
    }

    private fun createUserAndSession(userId: Long): Pair<User, Session> {
        val user = User(userId, "user-$userId")
        val sessionId = "session-$userId"
        val session = Session(sessionId, userId, System.currentTimeMillis())
        return Pair(user, session)
    }
}
