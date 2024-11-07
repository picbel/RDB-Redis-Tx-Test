package com.picbel.distributedtx.test

import com.picbel.distributedtx.DistributedTxApplication
import com.picbel.distributedtx.jpa.User
import com.picbel.distributedtx.jpa.UserRepo
import com.picbel.distributedtx.jpa.UserEntity
import com.picbel.distributedtx.redis.Session
import com.picbel.distributedtx.redis.SessionDao
import com.picbel.distributedtx.redis.SessionRTxDao
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
        val user = User(userId, "user-1")
        val sessionId = "session-1"
        val session = Session(sessionId, userId, System.currentTimeMillis())
        sessionDao.saveSession(session)
        userRepo.save(user)

        // when
        val savedSession = sessionDao.getSession(sessionId)
        val savedUser = userRepo.findById(userId)

        // then
        savedUser shouldBe user
        savedSession shouldBe session
    }

    @Test
    fun `rdb error`() {


    }
}
