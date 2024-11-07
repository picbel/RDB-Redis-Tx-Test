package com.picbel.distributedtx.redis

import org.redisson.api.RedissonClient
import org.redisson.api.RMapCache
import org.redisson.api.RTransaction
import org.redisson.api.TransactionOptions
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Repository
class SessionDao(
    private val redissonClient: RedissonClient
) {

    private val sessionMap: RMapCache<String, Session> = redissonClient.getMapCache("sessionCache")

    /**
     * 세션을 Redis에 저장합니다.
     * @param session 세션 정보
     */
    @Transactional
    fun saveSession(session: Session) {
        sessionMap[session.sessionId] = session
    }

    /**
     * 세션을 Redis에서 조회합니다.
     * @param sessionId 세션 ID
     * @return 세션 정보 또는 null
     */
    @Transactional
    fun getSession(sessionId: String): Session? {
        return sessionMap[sessionId]
    }

    @Transactional
    fun deleteSession(sessionId: String) {
        sessionMap.remove(sessionId)
    }

    fun clear() {
        sessionMap.clear()
    }
}

@Repository
class SessionRTxDao(
    private val redissonClient: RedissonClient
) {

    private val sessionMap = redissonClient.getMapCache<String, Session>("sessionCache")

    /**
     * RTransaction을 외부로 리턴하여 커밋 또는 롤백 처리를 테스트하기 쉽게 한다
     */
    fun saveSession(
        session: Session,
        isThrow : Boolean = false,
        isCommit : Boolean = false
    ) : Pair<Session, RTransaction> {
        val tx = executeInTransaction { transaction ->
            if (isThrow) {
                println("트랜잭션 테스트를 위한 의도적인 예외 발생")
                throw RuntimeException("트랜잭션 테스트를 위한 의도적인 예외 발생")
            }
            val transactionSessionMap = transaction.getMapCache<String, Session>("sessionCache")
            transactionSessionMap[session.sessionId] = session
            if (isCommit) {
                transaction.commit()
                println("트랜잭션이 성공적으로 커밋되었습니다.")
            }
        }
        return Pair(session, tx)
    }

    fun getSession(sessionId: String): Session? {
        return sessionMap[sessionId]
    }

    /**
     * 트랜잭션을 실행하고 커밋 또는 롤백 처리하는 헬퍼 메서드
     */
    private fun executeInTransaction(action: (RTransaction) -> Unit) : RTransaction{
        val transactionOptions = TransactionOptions.defaults()
            .timeout(5, TimeUnit.SECONDS)
            .retryAttempts(3)
            .retryInterval(2, TimeUnit.SECONDS)

        val transaction = redissonClient.createTransaction(transactionOptions)
        try {
            action(transaction)
//            transaction.commit() // 테스트를 위해 주석 처리

        } catch (e: Exception) {
            transaction.rollback()
            println("트랜잭션이 롤백되었습니다: ${e.message}")
            throw e  // 예외를 다시 던져 호출자가 처리할 수 있도록 함
        }
        return transaction
    }
}