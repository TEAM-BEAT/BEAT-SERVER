package com.beat.infra.persistence.member.repository

import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.infra.persistence.member.mapper.MemberPersistenceMapper
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
    private val memberPersistenceMapper: MemberPersistenceMapper,
) : MemberRepository {
    override fun findById(id: Long?): Optional<Member> =
        memberJpaRepository.findById(requireNotNull(id) { "The given id must not be null" })
            .map(memberPersistenceMapper::toDomain)

    override fun save(member: Member): Member {
        val entity = memberPersistenceMapper.toEntity(member)
        try {
            return memberPersistenceMapper.toDomain(memberJpaRepository.saveAndFlush(entity))
        } catch (exception: DataIntegrityViolationException) {
            if (hasConstraint(exception, "uk_member_social_identity")) {
                throw DuplicateSocialIdentityException(exception)
            }
            throw exception
        }
    }

    private fun hasConstraint(exception: Throwable, expectedConstraintName: String): Boolean {
        val expectedIdentifier = normalizeConstraintName(expectedConstraintName) ?: return false
        return generateSequence(exception) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .any { expectedIdentifier.equals(normalizeConstraintName(it.constraintName), ignoreCase = true) }
    }

    override fun findBySocialIdentity(socialIdentity: SocialIdentity): Optional<Member> =
        memberJpaRepository.findBySocialTypeAndSocialId(
            socialIdentity.socialId,
            socialIdentity.socialType,
        ).map(memberPersistenceMapper::toDomain)

    override fun count(): Long = memberJpaRepository.count()

    companion object {
        @JvmStatic
        fun normalizeConstraintName(constraintName: String?): String? {
            if (constraintName == null) {
                return null
            }

            val unqualified = constraintName.trim().substringAfterLast('.')
            return unqualified.trim('`', '\'', '"').trim().ifEmpty { null }
        }
    }
}
