package com.bistroops.waiting.model;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.member.model.MemberRepository;
import com.bistroops.member.model.MemberVO;
import com.bistroops.seattype.model.SeatTypeRepository;
import com.bistroops.seattype.model.SeatTypeVO;

@Service
@Transactional(readOnly = true)
public class WaitingService {

    public static final String WAITING = "候位中";
    public static final String NOTIFIED = "已通知";
    public static final String SEATED = "已入座";
    public static final String CANCELLED = "取消";
    public static final String MISSED = "過號";

    private static final List<String> ACTIVE = List.of(WAITING, NOTIFIED);

    private final WaitingRepository repository;
    private final MemberRepository memberRepository;
    private final SeatTypeRepository seatTypeRepository;

    public WaitingService(
            WaitingRepository repository,
            MemberRepository memberRepository,
            SeatTypeRepository seatTypeRepository) {

        this.repository = repository;
        this.memberRepository = memberRepository;
        this.seatTypeRepository = seatTypeRepository;
    }

    public List<WaitingVO> getAll() {
        return repository.findAllByOrderByWaitingNoAsc();
    }

    public WaitingVO getOneWaiting(Long waitingNo) {
        checkId(waitingNo);
        return repository.findById(waitingNo)
                .orElseThrow(() -> new IllegalArgumentException("候位紀錄不存在"));
    }

    public List<WaitingVO> getActiveBySeatType(Integer seatTypeNo) {
        checkSeatType(seatTypeNo);

        return repository
                .findBySeatType_SeatTypeNoAndWaitingStatusInOrderByWaitingNoAsc(
                        seatTypeNo, ACTIVE);
    }

    public List<WaitingVO> getByMember(Integer memNo) {
        checkMemberId(memNo);
        return repository.findByMember_MemNoOrderByWaitingNoDesc(memNo);
    }

    public long countActiveBySeatType(Integer seatTypeNo) {
        checkSeatType(seatTypeNo);

        return repository.countBySeatType_SeatTypeNoAndWaitingStatusIn(
                seatTypeNo, ACTIVE);
    }

    // 回傳同桌型、編號較早且尚未結束的組數；不估計等待分鐘數。
    public long countAhead(Long waitingNo) {
        WaitingVO waiting = getOneWaiting(waitingNo);
        requireActive(waiting);

        return repository
                .countBySeatType_SeatTypeNoAndWaitingNoLessThanAndWaitingStatusIn(
                        waiting.getSeatType().getSeatTypeNo(),
                        waitingNo,
                        ACTIVE);
    }

    // memNo 可為 null（非會員）；有值時必須由 Controller 的登入資訊取得。
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WaitingVO addWaiting(Integer memNo, Integer seatTypeNo,
            String waitingTel, String waitingName, String waitingComment) {

        checkSeatType(seatTypeNo);
        String tel = trim(waitingTel);
        String name = trim(waitingName);
        String comment = trim(waitingComment);

        if (!tel.matches("09[0-9]{8}")) {
            throw new IllegalArgumentException("請輸入09開頭的10碼手機號碼");
        }
        if (name.isBlank() || length(name) > 30) {
            throw new IllegalArgumentException("姓名不可空白且最多30個字元");
        }
        if (length(comment) > 20) {
            throw new IllegalArgumentException("備註最多20個字元");
        }

        repository.lockSeatType(seatTypeNo)
                .orElseThrow(() -> new IllegalArgumentException("桌型不存在"));

        SeatTypeVO seatType = seatTypeRepository.findById(seatTypeNo)
                .orElseThrow(() -> new IllegalArgumentException("桌型不存在"));
        
        if (repository
                .existsBySeatType_SeatTypeNoAndWaitingTelAndWaitingStatusIn(
                        seatTypeNo, tel, ACTIVE)) {

            throw new IllegalArgumentException(
                    "此手機在所選桌型已有有效候位");
        }

        MemberVO member = null;
        if (memNo != null) {
            checkMemberId(memNo);
            member = memberRepository.findById(memNo)
                    .orElseThrow(() -> new IllegalArgumentException("會員不存在"));
        }

        WaitingVO waiting = new WaitingVO();
        waiting.setSeatType(seatType);
        waiting.setMember(member);
        waiting.setWaitingTel(tel);
        waiting.setWaitingName(name);
        waiting.setWaitingComment(comment);
        waiting.setWaitingStatus(WAITING);
        waiting.setWaitingNotifyTime(null);
        return repository.save(waiting);
    }

    // 以下 staff 方法必須由具有員工權限的 Controller 呼叫。
    @Transactional
    public WaitingVO notifyWaiting(Long waitingNo) {
        WaitingVO waiting = lockWaiting(waitingNo);
        if (!WAITING.equals(waiting.getWaitingStatus())) {
            throw new IllegalArgumentException("只有候位中的紀錄可以標記通知");
        }
        waiting.setWaitingStatus(NOTIFIED);
        waiting.setWaitingNotifyTime(LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
        return waiting;
    }

    // 只更新候位紀錄；不代表已分配實際桌號或建立點餐訂單。
    @Transactional
    public WaitingVO seatWaiting(Long waitingNo) {
        WaitingVO waiting = lockWaiting(waitingNo);
        requireActive(waiting);
        waiting.setWaitingStatus(SEATED);
        return waiting;
    }

    @Transactional
    public WaitingVO missWaiting(Long waitingNo) {
        WaitingVO waiting = lockWaiting(waitingNo);
        if (!NOTIFIED.equals(waiting.getWaitingStatus())) {
            throw new IllegalArgumentException("只有已通知的紀錄可以標記過號");
        }
        waiting.setWaitingStatus(MISSED);
        return waiting;
    }

    @Transactional
    public WaitingVO cancelByStaff(Long waitingNo) {
        WaitingVO waiting = lockWaiting(waitingNo);
        requireActive(waiting);
        waiting.setWaitingStatus(CANCELLED);
        return waiting;
    }

    @Transactional
    public WaitingVO cancelByMember(Long waitingNo, Integer memNo) {
        checkMemberId(memNo);
        WaitingVO waiting = lockWaiting(waitingNo);
        if (waiting.getMember() == null
                || !memNo.equals(waiting.getMember().getMemNo())) {
            throw new IllegalArgumentException("你無法操作這筆候位");
        }
        requireActive(waiting);
        waiting.setWaitingStatus(CANCELLED);
        return waiting;
    }

    private WaitingVO lockWaiting(Long waitingNo) {
        checkId(waitingNo);
        return repository.findByIdForUpdate(waitingNo)
                .orElseThrow(() -> new IllegalArgumentException("候位紀錄不存在"));
    }

    private void requireActive(WaitingVO waiting) {
        if (!ACTIVE.contains(waiting.getWaitingStatus())) {
            throw new IllegalArgumentException("這筆候位已結束，無法再次操作");
        }
    }

    private void checkSeatType(Integer seatTypeNo) {
        if (seatTypeNo == null || !Set.of(2, 4, 6).contains(seatTypeNo)) {
            throw new IllegalArgumentException("請選擇2、4或6人桌");
        }
    }

    private void checkId(Long waitingNo) {
        if (waitingNo == null || waitingNo <= 0) {
            throw new IllegalArgumentException("候位編號必須大於零");
        }
    }

    private void checkMemberId(Integer memNo) {
        if (memNo == null || memNo <= 0) {
            throw new IllegalArgumentException("請先登入有效會員");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private int length(String value) {
        return value.codePointCount(0, value.length());
    }
}
