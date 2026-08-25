package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.entity.FuturesimHouseholdMember;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// 가구원(본인 제외 나머지 가구원) 개개인 정보. DB 테이블 없이 세션에만 보관한다 —
// 최종 결과를 저장하는 기능이 생기면 그때 다른 값들과 함께 결과 테이블에 커밋하면 된다.
@Service
public class HouseholdMemberService {

    public static final String DRAFT_SESSION_KEY = "futuresimHouseholdMembersDraft";

    @SuppressWarnings("unchecked")
    public List<FuturesimHouseholdMember> getDraftMembers(HttpSession session) {
        List<FuturesimHouseholdMember> draft = (List<FuturesimHouseholdMember>) session.getAttribute(DRAFT_SESSION_KEY);
        return draft != null ? draft : List.of();
    }

    public void addDraftMember(HttpSession session, String memberName, String relationship, int age, BigDecimal annualIncome) {
        FuturesimHouseholdMember member = new FuturesimHouseholdMember();
        member.setMemberName(memberName);
        member.setRelationship(relationship);
        member.setAge(age);
        member.setAnnualIncome(annualIncome);

        List<FuturesimHouseholdMember> draft = new ArrayList<>(getDraftMembers(session));
        draft.add(member);
        session.setAttribute(DRAFT_SESSION_KEY, draft);
    }

    public void removeDraftMember(HttpSession session, int index) {
        List<FuturesimHouseholdMember> draft = new ArrayList<>(getDraftMembers(session));
        if (index >= 0 && index < draft.size()) {
            draft.remove(index);
        }
        session.setAttribute(DRAFT_SESSION_KEY, draft);
    }
}
