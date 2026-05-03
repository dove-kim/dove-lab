package com.dove.member.application.service;

import com.dove.member.domain.entity.Member;
import com.dove.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandService {

    private final MemberRepository memberRepository;

    public Member save(Member member) {
        return memberRepository.save(member);
    }
}
